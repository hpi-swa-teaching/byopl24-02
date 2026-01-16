#!/bin/bash

# Autonomous Claude Code Performance Analysis Script
# Runs 5 complete analysis cycles, each with 5 iterations building on each other
# Total: 25 branches created

set -e

# --- Configuration ---
PREFIX="${1:?Usage: $0 <prefix> <prompt-file> [baseline-branch]}"
PROMPT_FILE="${2:?Usage: $0 <prefix> <prompt-file> [baseline-branch]}"
BASELINE_BRANCH="${3:-main}"
ITERATIONS_PER_RUN=10
TOTAL_RUNS=5
TIMEOUT_PER_RUN="2h"

# --- Validation ---
if [[ ! -f "$PROMPT_FILE" ]]; then
    echo "Error: Prompt file '$PROMPT_FILE' not found"
    exit 1
fi

PROMPT=$(cat "$PROMPT_FILE")

# Check for uncommitted changes
if [[ -n $(git status --porcelain) ]]; then
    echo "Error: Working directory has uncommitted changes. Please commit or stash them first."
    exit 1
fi

# Check baseline branch exists
if ! git rev-parse --verify "$BASELINE_BRANCH" >/dev/null 2>&1; then
    echo "Error: Baseline branch '$BASELINE_BRANCH' does not exist"
    exit 1
fi

echo "=== Claude Code Autonomous Performance Analysis ==="
echo "Prefix: $PREFIX"
echo "Baseline: $BASELINE_BRANCH"
echo "Runs: $TOTAL_RUNS"
echo "Iterations per run: $ITERATIONS_PER_RUN"
echo "Timeout per run: $TIMEOUT_PER_RUN"
echo "=================================================="
echo ""

# --- Main Loop ---
for run in $(seq 1 $TOTAL_RUNS); do
    echo ""
    echo "########## STARTING RUN $run of $TOTAL_RUNS ##########"
    echo "Time: $(date)"
    echo ""

    # Reset to baseline at the start of each run
    git checkout "$BASELINE_BRANCH"
    git reset --hard "$BASELINE_BRANCH"

    for iteration in $(seq 1 $ITERATIONS_PER_RUN); do
        BRANCH_NAME="${PREFIX}-run-${run}-iteration-${iteration}"

        echo ""
        echo "--- Run $run, Iteration $iteration ---"
        echo "Branch: $BRANCH_NAME"
        echo "Started: $(date)"
        echo ""

        # Create new branch from current state
        git checkout -b "$BRANCH_NAME"

        # Calculate remaining time for this run
        # (For first iteration, full timeout; subsequent iterations get remaining time)
        # Using timeout for the entire run, trusting Claude to manage iterations

        if [[ $iteration -eq 1 ]]; then
            # First iteration of run - start timer and run with full timeout
            RUN_START=$(date +%s)
        fi

        # Calculate elapsed time in this run
        ELAPSED=$(($(date +%s) - RUN_START))
        TIMEOUT_SECONDS=$((2 * 60 * 60))  # 2 hours in seconds
        REMAINING=$((TIMEOUT_SECONDS - ELAPSED))

        if [[ $REMAINING -le 60 ]]; then
            echo "Warning: Less than 1 minute remaining for this run. Skipping remaining iterations."
            break
        fi

        # Build iteration-specific prompt
        ITERATION_PROMPT="$PROMPT

This is iteration $iteration of $ITERATIONS_PER_RUN in run $run.
You have approximately $((REMAINING / 60)) minutes remaining for this run.
$(if [[ $iteration -gt 1 ]]; then echo "Build upon the improvements from the previous iteration."; fi)
$(if [[ $iteration -eq $ITERATIONS_PER_RUN ]]; then echo "This is the final iteration of this run."; fi)

Document your findings and reuse information from previous iterations.
When done with your changes for this iteration, commit them with a descriptive message summarizing what you improved.
Use skills if available.

Do NOT stop to ask me questions. Make reasonable decisions on your own 
and continue until the task is complete. If you encounter ambiguity, 
use your best judgment and document your choices. Only stop if you 
hit a truly unrecoverable error."

        # Run Claude Code with timeout
        # --yes: auto-accept all prompts
        # --dangerously-skip-permissions: skip all permission prompts (alternative to --yes)
        # --plugin-dir=../cc-truffle-performance-plugin use the performance plugin
        timeout "${REMAINING}s" claude --dangerously-skip-permissions --plugin-dir=../cc-truffle-performance-plugin -p "$ITERATION_PROMPT" || {
            EXIT_CODE=$?
            if [[ $EXIT_CODE -eq 124 ]]; then
                echo "Timeout reached for run $run"
                # Commit any uncommitted changes before moving on
                if [[ -n $(git status --porcelain) ]]; then
                    git add -A
                    git commit -m "Iteration $iteration (timeout): Partial changes

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>" || true
                fi
                break
            else
                echo "Claude exited with code $EXIT_CODE"
            fi
        }

        # Ensure all changes are committed
        if [[ -n $(git status --porcelain) ]]; then
            echo "Committing any remaining uncommitted changes..."
            git add -A
            git commit -m "Iteration $iteration: Uncommitted changes cleanup

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>" || true
        fi

        echo ""
        echo "Completed iteration $iteration at $(date)"
        echo "Branch $BRANCH_NAME is ready"

    done

    echo ""
    echo "########## COMPLETED RUN $run ##########"
    echo ""

    # --- Run Benchmarks ---
    echo "--- Running benchmarks for run $run ---"
    BENCHMARK_OUTPUT="benchmark-results-${PREFIX}-run-${run}.txt"

    # Rebuild to ensure latest changes are compiled
    ./mvnw package -q || echo "Warning: Build failed, running benchmarks anyway"

    {
        echo "=== Benchmark Results for ${PREFIX}-run-${run} ==="
        echo "Date: $(date)"
        echo "Final branch: ${PREFIX}-run-${run}-iteration-${ITERATIONS_PER_RUN}"
        echo ""

        echo "--- sieve ---"
        ./lox harness.lox sieve 10 10000
        echo ""

        echo "--- towers ---"
        ./lox harness.lox towers 10 300
        echo ""

        echo "--- list ---"
        ./lox harness.lox list 10 100
        echo ""

        echo "--- permute ---"
        ./lox harness.lox permute 10 10000
        echo ""

        echo "--- queens ---"
        ./lox harness.lox queens 10 3000
        echo ""
    } | tee "$BENCHMARK_OUTPUT"

    echo ""
    echo "Benchmark results saved to: $BENCHMARK_OUTPUT"
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
echo "  git diff ${BASELINE_BRANCH}..${PREFIX}-run-1-iteration-5"
echo "  git log --oneline ${PREFIX}-run-1-iteration-1..${PREFIX}-run-1-iteration-5"
