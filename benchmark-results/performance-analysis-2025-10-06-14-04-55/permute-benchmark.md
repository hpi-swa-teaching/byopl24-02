# Permute Benchmark Results

## Execution Details
- **Command**: `./lox harness.lox permute 10 1`
- **Date**: 2025-10-06
- **Iterations**: 10
- **Inner Iterations**: 1

## Raw Output
```
load benchmark: permute.lox
Starting permute benchmark ...
permute: innerIterations=1 runtime: 70773us
permute: innerIterations=1 runtime: 21851us
permute: innerIterations=1 runtime: 19447us
permute: innerIterations=1 runtime: 5751us
permute: innerIterations=1 runtime: 191us
permute: innerIterations=1 runtime: 184us
permute: innerIterations=1 runtime: 344us
permute: innerIterations=1 runtime: 351us
permute: innerIterations=1 runtime: 343us
permute: innerIterations=1 runtime: 194us
permute: iterations=10 average: 11942.9us total: 119429us

Total Runtime: 119429us
```

## Performance Metrics
- **Average Runtime**: 11,942.9 microseconds (11.94 ms)
- **Total Runtime**: 119,429 microseconds (119.43 ms)
- **First Iteration**: 70,773 microseconds (70.77 ms) - includes warmup
- **Subsequent Iterations**: 184 - 21,851 microseconds
- **Best Time**: 184 microseconds (0.18 ms)
- **Worst Time (excluding first)**: 21,851 microseconds (21.85 ms)

## Observations
- Gradual warmup over first 4 iterations (70ms -> 21ms -> 19ms -> 5ms)
- After full warmup, achieves excellent performance: 184-351 microseconds
- 384x speedup from first iteration to best time
- Second fastest benchmark after warmup (similar to sieve)
- Shows clear JIT optimization progression
- No errors or warnings encountered
- Benchmark completed successfully
