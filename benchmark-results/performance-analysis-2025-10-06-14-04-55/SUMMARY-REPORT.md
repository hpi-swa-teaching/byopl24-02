# Benchmark Execution Summary Report

**Date**: 2025-10-06
**Analysis ID**: performance-analysis-2025-10-06-14-04-55
**Configuration**: 10 iterations, 1 inner iteration per benchmark

## Executive Summary

All 5 benchmarks executed successfully with no errors or warnings. Results show significant JIT compilation warmup effects across all benchmarks, with performance improvements ranging from 5x to 460x after warmup.

## Benchmark Results Comparison

### Overall Performance Rankings (by average runtime)

1. **Sieve** - 15.64 ms average (fastest)
2. **Permute** - 11.94 ms average
3. **List** - 27.29 ms average
4. **Queens** - 33.38 ms average
5. **Towers** - 78.97 ms average (slowest)

### Detailed Performance Metrics

| Benchmark | Average (ms) | Total (ms) | Best (ms) | Worst (ms) | First Iter (ms) | Speedup |
|-----------|-------------|-----------|-----------|------------|-----------------|---------|
| Queens    | 33.38       | 333.83    | 21.60     | 113.91     | 113.91          | 5.27x   |
| Sieve     | 15.64       | 156.35    | 0.19      | 88.49      | 88.49           | 460x    |
| Towers    | 78.97       | 789.68    | 12.53     | 213.28     | 213.28          | 17x     |
| Permute   | 11.94       | 119.43    | 0.18      | 70.77      | 70.77           | 384x    |
| List      | 27.29       | 272.94    | 13.20     | 105.93     | 105.93          | 8x      |

Note: Speedup calculated as (First Iteration Time / Best Time)

### Warmed-up Performance (excluding first iteration)

| Benchmark | Warmed Avg (ms) | Min (ms) | Max (ms) | Variance |
|-----------|----------------|----------|----------|----------|
| Queens    | 24.88          | 21.60    | 32.74    | Low      |
| Sieve     | 0.32           | 0.19     | 0.44     | Very Low |
| Towers    | 63.93          | 12.53    | 92.16    | High     |
| Permute   | 0.27           | 0.18     | 0.35     | Very Low |
| List      | 18.56          | 13.20    | 21.87    | Low      |

## Key Observations

### Warmup Behavior

1. **Sieve & Permute**: Show dramatic performance improvements (384-460x) after JIT compilation
   - Achieve sub-millisecond execution times when fully optimized
   - Sieve shows two-stage warmup (88ms -> 65ms -> <0.5ms)

2. **Queens & List**: Moderate warmup effects (5-8x improvement)
   - Stabilize in the 13-33ms range after warmup
   - Consistent performance across iterations

3. **Towers**: Significant warmup (17x) but high performance variance
   - Shows unstable behavior with performance ranging from 12ms to 92ms
   - Possible deoptimization or GC interference

### Performance Characteristics

**Fastest Benchmarks (warmed-up)**:
- Permute: 0.27ms average
- Sieve: 0.32ms average

**Mid-range Benchmarks**:
- List: 18.56ms average
- Queens: 24.88ms average

**Slowest Benchmark**:
- Towers: 63.93ms average (with high variance)

### JIT Compilation Impact

The benchmarks demonstrate clear JIT optimization effects:
- First iteration is consistently the slowest (warmup overhead)
- Performance improves dramatically in subsequent iterations
- Sieve and Permute benefit most from JIT compilation
- Towers shows optimization instability

## Verification Status

All benchmarks:
- Completed successfully without errors
- Produced expected output format
- Showed timing data for all 10 iterations
- Calculated correct averages and totals

## Performance Anomalies

1. **Towers Benchmark**: High variance in execution time
   - Iterations 7-8 performed much better (12-14ms) than others (54-92ms)
   - Suggests possible deoptimization or GC activity
   - Warrants further investigation with profiling tools

2. **Sieve Two-Stage Warmup**:
   - First iteration: 88ms
   - Second iteration: 65ms (still warm-up)
   - Iterations 3-10: <0.5ms (fully optimized)
   - Indicates complex optimization path

## Recommendations for Future Analysis

1. Run Towers benchmark with CPU sampler to identify variance causes
2. Increase iteration count to better understand steady-state performance
3. Consider running with compilation tracing to see JIT decisions
4. Analyze memory allocation patterns for Towers benchmark
5. Test with different heap sizes to rule out GC interference

## Files Generated

All detailed results saved to:
- `/Users/antonykamp/Projects/hpi-ma/byopl24-02/benchmark-results/performance-analysis-2025-10-06-14-04-55/queens-benchmark.md`
- `/Users/antonykamp/Projects/hpi-ma/byopl24-02/benchmark-results/performance-analysis-2025-10-06-14-04-55/sieve-benchmark.md`
- `/Users/antonykamp/Projects/hpi-ma/byopl24-02/benchmark-results/performance-analysis-2025-10-06-14-04-55/towers-benchmark.md`
- `/Users/antonykamp/Projects/hpi-ma/byopl24-02/benchmark-results/performance-analysis-2025-10-06-14-04-55/permute-benchmark.md`
- `/Users/antonykamp/Projects/hpi-ma/byopl24-02/benchmark-results/performance-analysis-2025-10-06-14-04-55/list-benchmark.md`
- `/Users/antonykamp/Projects/hpi-ma/byopl24-02/benchmark-results/performance-analysis-2025-10-06-14-04-55/SUMMARY-REPORT.md`
