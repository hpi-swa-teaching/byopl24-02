# Sieve Benchmark Results

## Execution Details
- **Command**: `./lox harness.lox sieve 10 1`
- **Date**: 2025-10-06
- **Iterations**: 10
- **Inner Iterations**: 1

## Raw Output
```
load benchmark: sieve.lox
Starting sieve benchmark ...
sieve: innerIterations=1 runtime: 88491us
sieve: innerIterations=1 runtime: 65462us
sieve: innerIterations=1 runtime: 370us
sieve: innerIterations=1 runtime: 286us
sieve: innerIterations=1 runtime: 194us
sieve: innerIterations=1 runtime: 221us
sieve: innerIterations=1 runtime: 290us
sieve: innerIterations=1 runtime: 406us
sieve: innerIterations=1 runtime: 440us
sieve: innerIterations=1 runtime: 192us
sieve: iterations=10 average: 15635.2us total: 156352us

Total Runtime: 156352us
```

## Performance Metrics
- **Average Runtime**: 15,635.2 microseconds (15.64 ms)
- **Total Runtime**: 156,352 microseconds (156.35 ms)
- **First Iteration**: 88,491 microseconds (88.49 ms) - includes warmup
- **Second Iteration**: 65,462 microseconds (65.46 ms) - still warming up
- **Subsequent Iterations**: 192 - 440 microseconds
- **Best Time**: 192 microseconds (0.19 ms)
- **Worst Time (excluding warmup)**: 440 microseconds (0.44 ms)

## Observations
- Dramatic performance improvement after warmup (460x faster than first iteration)
- Two-stage warmup visible: first at 88ms, second at 65ms, then drops to sub-millisecond
- Warmed-up performance is extremely fast: 200-400 microseconds
- This benchmark shows the most significant JIT compilation benefit
- No errors or warnings encountered
- Benchmark completed successfully
