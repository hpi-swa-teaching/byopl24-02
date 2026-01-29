#!/bin/bash

# Benchmark configuration and running logic

# Benchmark configuration
declare -A BENCHMARKS=(
    ["sieve"]="10 10000"
    ["towers"]="10 300"
    ["list"]="10 100"
    ["permute"]="10 10000"
    ["queens"]="10 3000"
)
BENCHMARK_ORDER=("sieve" "towers" "list" "permute" "queens")

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
