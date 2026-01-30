#!/bin/bash

# Claude invocation with error detection

# Exit codes:
#   0   = success
#   1   = generic error
#   2   = rate limit / "No messages returned" error
#   124 = timeout

# Global variable to store the session ID from the last run
CLAUDE_SESSION_ID=""

# Builds the iteration-specific prompt
# Args: $1 = iteration, $2 = base_prompt
# Uses globals: $ITERATIONS_PER_RUN, $run
build_iteration_prompt() {
    local iteration=$1
    local base_prompt=$2
    local total=$ITERATIONS_PER_RUN

    cat <<EOF
$base_prompt

This is iteration $iteration of $total in run $run.
$(if [[ $iteration -gt 1 ]]; then echo "Build upon the improvements from the previous iteration."; fi)
$(if [[ $iteration -eq $total ]]; then echo "This is the final iteration of this run."; fi)

When done with your changes for this iteration, commit them with a descriptive message summarizing what you improved.
EOF
}

# Runs Claude with timeout management and error detection
# Args: $1 = prompt, $2 = timeout_seconds, $3 = session_id (optional - if provided, uses --continue)
# Returns: 0 on success, 1 on generic error, 2 on rate limit error, 124 on timeout
# Sets global CLAUDE_SESSION_ID with the session ID from this run
run_claude_with_timeout() {
    local prompt=$1
    local timeout=$2
    local session_id=${3:-}
    local output_file
    output_file=$(mktemp)

    # Build Claude command
    local claude_cmd
    if [[ -n "$session_id" ]]; then
        echo "Continuing Claude session: $session_id"
        claude_cmd="claude --continue $session_id -p \"Please continue\""
    else
        claude_cmd="claude $CLAUDE_FLAGS --plugin-dir $PLUGIN_DIR -p \"$prompt\""
    fi

    # Start Claude in background, capturing output while still displaying it
    eval "$claude_cmd" 2>&1 | tee "$output_file" &
    CLAUDE_PID=$!
    sleep "$STARTUP_DELAY"

    # Extract session ID from output (Claude prints it early in the session)
    # Wait a bit for the session ID to appear in the output
    sleep 2
    if [[ -z "$session_id" ]]; then
        # Try to extract session ID from output
        # Claude typically outputs session info in various formats, try common patterns
        CLAUDE_SESSION_ID=$(grep -oE "session[_-]?id[:\s]+[a-zA-Z0-9_-]+" "$output_file" | head -1 | awk '{print $NF}')
        if [[ -z "$CLAUDE_SESSION_ID" ]]; then
            # Try alternative pattern
            CLAUDE_SESSION_ID=$(grep -oE "[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}" "$output_file" | head -1)
        fi
    else
        # Continuing existing session
        CLAUDE_SESSION_ID=$session_id
    fi

    # Wait for Claude with timeout
    local wait_start
    wait_start=$(date +%s)
    while kill -0 "$CLAUDE_PID" 2>/dev/null; do
        local wait_elapsed
        wait_elapsed=$(($(date +%s) - wait_start))

        if [[ $wait_elapsed -ge $timeout ]]; then
            echo "Timeout reached (${timeout}s elapsed)"
            terminate_process "$CLAUDE_PID" "$TERMINATION_GRACE_PERIOD"
            CLAUDE_PID=""
            rm -f "$output_file"
            return 124
        fi
        sleep 1
    done

    # Process completed, check exit code
    local exit_code=0
    wait "$CLAUDE_PID" || exit_code=$?
    CLAUDE_PID=""

    # Check for rate limit / API errors in output
    # Common error patterns:
    # - "No messages returned"
    # - "promise rejected"
    # - "rate limit"
    # - "429" (HTTP status code for too many requests)
    # - "quota exceeded"
    # - "API error"
    if grep -qiE "No messages returned|promise rejected|rate limit|429|quota exceeded|too many requests|overloaded_error" "$output_file" 2>/dev/null; then
        echo ""
        echo "Detected rate limit or API error in Claude output."
        rm -f "$output_file"
        return 2
    fi

    # If Claude exited with error and output is suspiciously short, likely an API/auth error
    if [[ $exit_code -ne 0 ]]; then
        local output_size
        output_size=$(wc -l < "$output_file" 2>/dev/null || echo "0")
        if [[ $output_size -lt 5 ]]; then
            # Very short output with error exit suggests API/rate limit issue
            echo ""
            echo "Warning: Claude exited with error $exit_code and minimal output (possible rate limit)."
            if grep -qiE "error|failed|limit" "$output_file" 2>/dev/null; then
                echo "Error detected in output, treating as rate limit."
                rm -f "$output_file"
                return 2
            fi
        fi
    fi

    rm -f "$output_file"
    return $exit_code
}
