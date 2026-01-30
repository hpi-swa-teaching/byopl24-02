#!/bin/bash

# Autonomous Claude Code Performance Analysis Script
# Runs multiple analysis cycles, each with iterations building on each other

# Note: NOT using set -e to allow graceful error handling and iteration skipping

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# =============================================================================
# Source helper scripts
# =============================================================================

source "$SCRIPT_DIR/perf-analysis/helpers.sh"
source "$SCRIPT_DIR/perf-analysis/benchmarks.sh"
source "$SCRIPT_DIR/perf-analysis/claude-runner.sh"
source "$SCRIPT_DIR/perf-analysis/state.sh"

# =============================================================================
# Argument Parsing
# =============================================================================

CONTINUE_MODE=false

# Parse optional --continue flag
if [[ "${1:-}" == "--continue" ]]; then
    CONTINUE_MODE=true
    shift
fi

PREFIX="${1:?Usage: $0 [--continue] <prefix> <prompt-file> [baseline-branch]}"
PROMPT_FILE="${2:?Usage: $0 [--continue] <prefix> <prompt-file> [baseline-branch]}"
BASELINE_BRANCH="${3:-main}"

# =============================================================================
# Configuration
# =============================================================================

ITERATIONS_PER_RUN=10
TOTAL_RUNS=5
TIMEOUT_SECONDS=$((2 * 60 * 60))  # 2 hours in seconds
TIMEOUT_WARNING_THRESHOLD=60       # Warn when less than this many seconds remain
STARTUP_DELAY=1                    # Seconds to wait after starting Claude
TERMINATION_GRACE_PERIOD=5         # Seconds to wait before force kill
CLEANUP_GRACE_PERIOD=2             # Seconds to wait in cleanup before force kill

PLUGIN_DIR="../cc-truffle-performance-plugin"
CLAUDE_FLAGS="--dangerously-skip-permissions"

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

# =============================================================================
# Handle --continue mode
# =============================================================================

START_RUN=1
START_ITERATION=1
RESUME_SESSION_ID=""

if [[ "$CONTINUE_MODE" == true ]]; then
    if ! load_state "$PREFIX"; then
        echo "Cannot continue: no saved state for prefix '$PREFIX'"
        exit 1
    fi

    BASELINE_BRANCH="$SAVED_BASELINE_BRANCH"
    START_RUN=$SAVED_RUN
    # Resume the saved iteration (don't increment - we're continuing the same iteration)
    START_ITERATION=$SAVED_ITERATION
    RESUME_SESSION_ID="$SAVED_SESSION_ID"

    # If iteration is 0, it means we need to start iteration 1
    if [[ $START_ITERATION -eq 0 ]]; then
        START_ITERATION=1
    fi

    # If we finished all iterations in the saved run, move to next run
    if [[ $START_ITERATION -gt $ITERATIONS_PER_RUN ]]; then
        START_RUN=$((START_RUN + 1))
        START_ITERATION=1
    fi

    # Check if there's anything left to do
    if [[ $START_RUN -gt $TOTAL_RUNS ]]; then
        echo "All runs already completed. Nothing to continue."
        clear_state "$PREFIX"
        exit 0
    fi

    # Checkout the last successful iteration branch
    LAST_BRANCH="${PREFIX}-run-${SAVED_RUN}-iteration-${SAVED_ITERATION}"
    if [[ $SAVED_ITERATION -gt 0 ]] && git rev-parse --verify "$LAST_BRANCH" >/dev/null 2>&1; then
        echo "Resuming from branch: $LAST_BRANCH"
        git checkout "$LAST_BRANCH"
    else
        echo "Resuming from baseline: $BASELINE_BRANCH"
        git checkout "$BASELINE_BRANCH"
    fi

    echo "Continuing from run $START_RUN, iteration $START_ITERATION"
    if [[ -n "$RESUME_SESSION_ID" ]]; then
        echo "Will resume Claude session: $RESUME_SESSION_ID"
    fi
    echo ""
else
    # Fresh run: validate clean working directory and baseline branch
    if [[ -n $(git status --porcelain) ]]; then
        echo "Error: Working directory has uncommitted changes. Please commit or stash them first."
        exit 1
    fi

    if ! git rev-parse --verify "$BASELINE_BRANCH" >/dev/null 2>&1; then
        echo "Error: Baseline branch '$BASELINE_BRANCH' does not exist"
        exit 1
    fi
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
if [[ "$CONTINUE_MODE" == true ]]; then
    echo "Mode: CONTINUE (from run $START_RUN, iteration $START_ITERATION)"
fi
echo "=================================================="
echo ""

for run in $(seq $START_RUN $TOTAL_RUNS); do
    echo ""
    echo "########## STARTING RUN $run of $TOTAL_RUNS ##########"
    echo "Time: $(date)"
    echo ""

    # Determine starting iteration for this run
    local_start_iteration=1
    if [[ $run -eq $START_RUN ]]; then
        local_start_iteration=$START_ITERATION
    fi

    # Reset to baseline at start of a new run (iteration 1)
    if [[ $local_start_iteration -eq 1 ]]; then
        if ! git checkout "$BASELINE_BRANCH" 2>/dev/null; then
            echo "Error: Could not checkout baseline branch $BASELINE_BRANCH"
            echo "Skipping run $run"
            continue
        fi
        git reset --hard "$BASELINE_BRANCH"
    fi

    # Initialize run timer and failure tracking
    RUN_START=$(date +%s)
    CONSECUTIVE_FAILURES=0
    MAX_CONSECUTIVE_FAILURES=2

    for iteration in $(seq $local_start_iteration $ITERATIONS_PER_RUN); do
        BRANCH_NAME="${PREFIX}-run-${run}-iteration-${iteration}"

        echo ""
        echo "--- Run $run, Iteration $iteration ---"
        echo "Branch: $BRANCH_NAME"
        echo "Started: $(date)"
        echo ""

        # Create or checkout branch
        if git rev-parse --verify "$BRANCH_NAME" >/dev/null 2>&1; then
            # Branch exists (resuming after rate limit) - just check it out
            echo "Resuming existing branch: $BRANCH_NAME"
            git checkout "$BRANCH_NAME"
        else
            # New branch - create it
            git checkout -b "$BRANCH_NAME"
        fi

        # Calculate remaining time
        ELAPSED=$(($(date +%s) - RUN_START))
        REMAINING=$((TIMEOUT_SECONDS - ELAPSED))

        if [[ $REMAINING -le $TIMEOUT_WARNING_THRESHOLD ]]; then
            echo "Warning: Less than ${TIMEOUT_WARNING_THRESHOLD}s remaining. Skipping remaining iterations."
            break
        fi

        # Build prompt and run Claude
        ITERATION_PROMPT=$(build_iteration_prompt "$iteration" "$PROMPT")

        # Use saved session ID only on first iteration when continuing
        if [[ -n "$RESUME_SESSION_ID" ]]; then
            run_claude_with_timeout "$ITERATION_PROMPT" "$REMAINING" "$RESUME_SESSION_ID"
            RESUME_SESSION_ID=""  # Clear after first use
        else
            run_claude_with_timeout "$ITERATION_PROMPT" "$REMAINING"
        fi
        EXIT_CODE=$?

        if [[ $EXIT_CODE -eq 0 ]]; then
            # Success - reset failure counter
            CONSECUTIVE_FAILURES=0
            commit_if_needed "Iteration $iteration: Uncommitted changes cleanup"
        elif [[ $EXIT_CODE -eq 2 ]]; then
            # Rate limit / API error - save state and stop
            echo ""
            echo "Rate limit or API error detected."
            echo "Keeping current iteration state for resuming..."

            # Commit any uncommitted changes to preserve state
            commit_if_needed "Iteration $iteration: Work in progress (rate limit hit)"

            # Save state for CURRENT iteration (not previous) so we can resume it
            save_state "$PREFIX" "$run" "$iteration" "$BASELINE_BRANCH" "$CLAUDE_SESSION_ID"

            echo ""
            echo "Rate limit reached. State saved for resuming iteration $iteration."
            echo ""
            if [[ -n "$CLAUDE_SESSION_ID" ]]; then
                echo "To resume the Claude session directly:"
                echo "  claude --continue $CLAUDE_SESSION_ID"
                echo ""
            fi
            echo "To continue the full script (will resume iteration $iteration):"
            echo "  $0 --continue $PREFIX $PROMPT_FILE"
            exit 3
        elif [[ $EXIT_CODE -eq 124 ]]; then
            # Timeout
            commit_if_needed "Iteration $iteration (timeout): Partial changes"
            break
        else
            # Generic error
            CONSECUTIVE_FAILURES=$((CONSECUTIVE_FAILURES + 1))

            echo "Error: Claude exited with code $EXIT_CODE"
            echo "Consecutive failures: $CONSECUTIVE_FAILURES of $MAX_CONSECUTIVE_FAILURES"

            # Check if we should stop due to too many consecutive failures
            if [[ $CONSECUTIVE_FAILURES -ge $MAX_CONSECUTIVE_FAILURES ]]; then
                # Too many failures - likely a persistent issue (rate limit, etc.)
                # Keep the state for resuming
                echo "Too many consecutive failures detected."
                echo "Keeping current iteration state for resuming..."

                # Commit any uncommitted changes to preserve state
                commit_if_needed "Iteration $iteration: Work in progress (multiple failures)"

                # Save state for current iteration
                save_state "$PREFIX" "$run" "$iteration" "$BASELINE_BRANCH" "$CLAUDE_SESSION_ID"

                echo ""
                echo "=========================================="
                echo "ERROR: Too many consecutive failures ($CONSECUTIVE_FAILURES)."
                echo "This likely indicates a persistent issue (e.g., rate limit, API error)."
                echo "Stopping to prevent infinite retry loop."
                echo "=========================================="
                echo ""
                if [[ -n "$CLAUDE_SESSION_ID" ]]; then
                    echo "To resume the Claude session directly:"
                    echo "  claude --continue $CLAUDE_SESSION_ID"
                    echo ""
                fi
                echo "To continue the script later (will resume iteration $iteration):"
                echo "  $0 --continue $PREFIX $PROMPT_FILE"
                exit 4
            fi

            # Single failure - revert and try next iteration
            git checkout -- . 2>/dev/null || true
            git clean -fd 2>/dev/null || true

            if [[ $iteration -gt 1 ]]; then
                PREV_BRANCH="${PREFIX}-run-${run}-iteration-$((iteration - 1))"
                git checkout "$PREV_BRANCH" 2>/dev/null || git checkout "$BASELINE_BRANCH"
            else
                git checkout "$BASELINE_BRANCH" 2>/dev/null || git checkout main
            fi
            git branch -D "$BRANCH_NAME" 2>/dev/null || true

            echo "Iteration $iteration skipped due to error at $(date)"
            echo ""
            continue
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

# All runs completed successfully - clear state
clear_state "$PREFIX"

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
