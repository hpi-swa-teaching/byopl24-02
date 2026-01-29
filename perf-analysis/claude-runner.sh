#!/bin/bash

# Claude invocation with error detection

# Exit codes:
#   0   = success
#   1   = generic error
#   2   = rate limit / "No messages returned" error
#   124 = timeout

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
# Args: $1 = prompt, $2 = timeout_seconds
# Returns: 0 on success, 1 on generic error, 2 on rate limit error, 124 on timeout
run_claude_with_timeout() {
    local prompt=$1
    local timeout=$2
    local output_file
    output_file=$(mktemp)

    # Start Claude in background, capturing output while still displaying it
    claude $CLAUDE_FLAGS --plugin-dir $PLUGIN_DIR -p "$prompt" 2>&1 | tee "$output_file" &
    CLAUDE_PID=$!
    sleep "$STARTUP_DELAY"

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
    if grep -qiE "No messages returned|promise rejected" "$output_file" 2>/dev/null; then
        echo ""
        echo "Detected rate limit or API error in Claude output."
        rm -f "$output_file"
        return 2
    fi

    rm -f "$output_file"
    return $exit_code
}
