#!/bin/bash

# Helper functions for perf-analysis

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
