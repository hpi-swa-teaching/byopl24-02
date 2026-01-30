#!/bin/bash

# State tracking for --continue support

# Returns the state file path for the given prefix
# Args: $1 = prefix
state_file_path() {
    local prefix=$1
    echo "perf-analysis/.state-${prefix}"
}

# Saves current progress to state file
# Args: $1 = prefix, $2 = run, $3 = iteration, $4 = baseline_branch, $5 = session_id (optional)
save_state() {
    local prefix=$1
    local run=$2
    local iteration=$3
    local baseline_branch=$4
    local session_id=${5:-}
    local state_file
    state_file=$(state_file_path "$prefix")

    cat > "$state_file" <<EOF
RUN=$run
ITERATION=$iteration
BASELINE_BRANCH=$baseline_branch
SESSION_ID=$session_id
EOF
    echo "State saved: run=$run, iteration=$iteration${session_id:+, session=$session_id}"
}

# Loads state from file. Sets SAVED_RUN, SAVED_ITERATION, SAVED_BASELINE_BRANCH, SAVED_SESSION_ID.
# Args: $1 = prefix
# Returns: 0 on success, 1 if no state file found
load_state() {
    local prefix=$1
    local state_file
    state_file=$(state_file_path "$prefix")

    if [[ ! -f "$state_file" ]]; then
        echo "Error: No saved state found for prefix '$prefix'"
        echo "State file not found: $state_file"
        return 1
    fi

    # shellcheck disable=SC1090
    source "$state_file"
    SAVED_RUN=$RUN
    SAVED_ITERATION=$ITERATION
    SAVED_BASELINE_BRANCH=$BASELINE_BRANCH
    SAVED_SESSION_ID=${SESSION_ID:-}

    echo "Loaded state: run=$SAVED_RUN, iteration=$SAVED_ITERATION, baseline=$SAVED_BASELINE_BRANCH${SAVED_SESSION_ID:+, session=$SAVED_SESSION_ID}"
    return 0
}

# Removes the state file for a prefix
# Args: $1 = prefix
clear_state() {
    local prefix=$1
    local state_file
    state_file=$(state_file_path "$prefix")

    rm -f "$state_file"
    echo "State cleared for prefix '$prefix'"
}
