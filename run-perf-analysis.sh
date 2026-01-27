#!/bin/bash

# Autonomous Claude Code Performance Analysis Script
# Runs multiple analysis cycles, each with iterations building on each other

# Note: NOT using set -e to allow graceful error handling and iteration skipping

# =============================================================================
# Configuration
# =============================================================================

PREFIX="${1:?Usage: $0 <prefix> <prompt-file> [baseline-branch]}"
PROMPT_FILE="${2:?Usage: $0 <prefix> <prompt-file> [baseline-branch]}"
BASELINE_BRANCH="${3:-main}"

ITERATIONS_PER_RUN=10
TOTAL_RUNS=5
TIMEOUT_SECONDS=$((2 * 60 * 60))  # 2 hours in seconds
TIMEOUT_WARNING_THRESHOLD=60       # Warn when less than this many seconds remain
STARTUP_DELAY=1                    # Seconds to wait after starting Claude
TERMINATION_GRACE_PERIOD=5         # Seconds to wait before force kill
CLEANUP_GRACE_PERIOD=2             # Seconds to wait in cleanup before force kill

PLUGIN_DIR="../cc-truffle-performance-plugin"
CLAUDE_FLAGS="--dangerously-skip-permissions"

# Benchmark configuration
declare -A BENCHMARKS=(
    ["sieve"]="10 10000"
    ["towers"]="10 300"
    ["list"]="10 100"
    ["permute"]="10 10000"
    ["queens"]="10 3000"
)
BENCHMARK_ORDER=("sieve" "towers" "list" "permute" "queens")

# =============================================================================
# Helper Functions
# =============================================================================

# Terminates a process and all its children
# Args: $1 = PID, $2 = grace_period (seconds)
terminate_process() {
    local pid=$1
    local grace_period=${2:-$TERMINATION_GRACE_PERIOD}

    if [[ -z "$pid" ]] || ! kill -0 "$pid" 2>/dev/null; then
        return 0
    fi

    echo "Terminating process $pid and children..."
    kill -TERM "$pid" 2>/dev/null || true
    pkill -P "$pid" 2>/dev/null || true
    sleep "$grace_period"
    kill -KILL "$pid" 2>/dev/null || true
    pkill -9 -P "$pid" 2>/dev/null || true
}

# Commits uncommitted changes with a message
# Args: $1 = commit message
commit_if_needed() {
    local message=$1

    if [[ -n $(git status --porcelain) ]]; then
        echo "Committing changes: $message"
        git add -A
        git commit -m "$message" || true
    fi
}

# Runs all benchmarks and saves results
# Args: $1 = output_file, $2 = run_number
run_benchmarks() {
    local output_file=$1
    local run_num=$2

    echo "--- Running benchmarks for run $run_num ---"

    if ! ./mvnw package -q 2>&1; then
        echo "Warning: Build failed, skipping benchmarks for run $run_num"
        echo "Build failed - benchmarks skipped" > "$output_file"
        return 1
    fi

    {
        echo "=== Benchmark Results for ${PREFIX}-run-${run_num} ==="
        echo "Date: $(date)"
        echo "Final branch: ${PREFIX}-run-${run_num}-iteration-${ITERATIONS_PER_RUN}"
        echo ""

        for benchmark in "${BENCHMARK_ORDER[@]}"; do
            echo "--- $benchmark ---"
            # shellcheck disable=SC2086
            ./lox harness.lox "$benchmark" ${BENCHMARKS[$benchmark]} || echo "$benchmark benchmark failed"
            echo ""
        done
    } | tee "$output_file"

    echo "Benchmark results saved to: $output_file"
}

# Builds the iteration-specific prompt
# Args: $1 = iteration, $2 = base_prompt
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
Use skills if available.
EOF
}

# Runs Claude with timeout management
# Args: $1 = prompt, $2 = timeout_seconds
# Returns: 0 on success, 1 on error, 124 on timeout
run_claude_with_timeout() {
    local prompt=$1
    local timeout=$2

    # Start Claude in background
    claude $CLAUDE_FLAGS --plugin-dir="$PLUGIN_DIR" -p "$prompt" &
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
            return 124
        fi
        sleep 1
    done

    # Process completed, check exit code
    local exit_code=0
    wait "$CLAUDE_PID" || exit_code=$?
    CLAUDE_PID=""
    return $exit_code
}

# =============================================================================
# Cleanup Handler
# =============================================================================

cleanup() {
    echo ""
    echo "Caught interrupt signal - cleaning up..."

    terminate_process "$CLAUDE_PID" "$CLEANUP_GRACE_PERIOD"
    pkill -P $$ 2>/dev/null || true

    echo "Cleanup complete"
    exit 130
}

trap cleanup SIGINT SIGTERM

# =============================================================================
# Validation
# =============================================================================

if [[ ! -f "$PROMPT_FILE" ]]; then
    echo "Error: Prompt file '$PROMPT_FILE' not found"
    exit 1
fi

PROMPT=$(cat "$PROMPT_FILE")

if [[ -n $(git status --porcelain) ]]; then
    echo "Error: Working directory has uncommitted changes. Please commit or stash them first."
    exit 1
fi

if ! git rev-parse --verify "$BASELINE_BRANCH" >/dev/null 2>&1; then
    echo "Error: Baseline branch '$BASELINE_BRANCH' does not exist"
    exit 1
fi

# =============================================================================
# Main Execution
# =============================================================================

echo "=== Claude Code Autonomous Performance Analysis ==="
echo "Prefix: $PREFIX"
echo "Baseline: $BASELINE_BRANCH"
echo "Runs: $TOTAL_RUNS"
echo "Iterations per run: $ITERATIONS_PER_RUN"
echo "Timeout per run: $((TIMEOUT_SECONDS / 3600))h"
echo "=================================================="
echo ""

for run in $(seq 1 $TOTAL_RUNS); do
    echo ""
    echo "########## STARTING RUN $run of $TOTAL_RUNS ##########"
    echo "Time: $(date)"
    echo ""

    # Reset to baseline
    if ! git checkout "$BASELINE_BRANCH" 2>/dev/null; then
        echo "Error: Could not checkout baseline branch $BASELINE_BRANCH"
        echo "Skipping run $run"
        continue
    fi
    git reset --hard "$BASELINE_BRANCH"

    # Initialize run timer
    RUN_START=$(date +%s)

    for iteration in $(seq 1 $ITERATIONS_PER_RUN); do
        BRANCH_NAME="${PREFIX}-run-${run}-iteration-${iteration}"

        echo ""
        echo "--- Run $run, Iteration $iteration ---"
        echo "Branch: $BRANCH_NAME"
        echo "Started: $(date)"
        echo ""

        # Create branch
        git checkout -b "$BRANCH_NAME"

        # Calculate remaining time
        ELAPSED=$(($(date +%s) - RUN_START))
        REMAINING=$((TIMEOUT_SECONDS - ELAPSED))

        if [[ $REMAINING -le $TIMEOUT_WARNING_THRESHOLD ]]; then
            echo "Warning: Less than ${TIMEOUT_WARNING_THRESHOLD}s remaining. Skipping remaining iterations."
            break
        fi

        # Build prompt and run Claude
        ITERATION_PROMPT=$(build_iteration_prompt "$iteration" "$PROMPT")

        if run_claude_with_timeout "$ITERATION_PROMPT" "$REMAINING"; then
            # Success
            commit_if_needed "Iteration $iteration: Uncommitted changes cleanup"
        else
            EXIT_CODE=$?

            if [[ $EXIT_CODE -eq 124 ]]; then
                # Timeout
                commit_if_needed "Iteration $iteration (timeout): Partial changes"
                break
            else
                # Error
                echo "Error: Claude exited with code $EXIT_CODE"
                echo "Skipping iteration $iteration and continuing to next iteration..."

                git checkout "$BASELINE_BRANCH" 2>/dev/null || git checkout main
                git branch -D "$BRANCH_NAME" 2>/dev/null || true

                echo "Iteration $iteration skipped due to error at $(date)"
                echo ""
                continue
            fi
        fi

        echo ""
        echo "Completed iteration $iteration at $(date)"
        echo "Branch $BRANCH_NAME is ready"
    done

    echo ""
    echo "########## COMPLETED RUN $run ##########"
    echo ""

    # Run benchmarks
    BENCHMARK_OUTPUT="benchmark-results-${PREFIX}-run-${run}.txt"
    run_benchmarks "$BENCHMARK_OUTPUT" "$run"
    echo ""
done

# Return to baseline
git checkout "$BASELINE_BRANCH"

echo ""
echo "=== Analysis Complete ==="
echo "Created branches:"
git branch --list "${PREFIX}-*"
echo ""
echo "To compare results, use:"
echo "  git diff ${BASELINE_BRANCH}..${PREFIX}-run-1-iteration-${ITERATIONS_PER_RUN}"
echo "  git log --oneline ${PREFIX}-run-1-iteration-1..${PREFIX}-run-1-iteration-${ITERATIONS_PER_RUN}"
