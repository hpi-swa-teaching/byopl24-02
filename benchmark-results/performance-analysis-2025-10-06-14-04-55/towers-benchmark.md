# Towers Benchmark Results

## Execution Details
- **Command**: `./lox harness.lox towers 10 1`
- **Date**: 2025-10-06
- **Iterations**: 10
- **Inner Iterations**: 1

## Raw Output
```
load benchmark: towers.lox
Starting towers benchmark ...
towers: innerIterations=1 runtime: 213282us
towers: innerIterations=1 runtime: 65188us
towers: innerIterations=1 runtime: 91061us
towers: innerIterations=1 runtime: 54075us
towers: innerIterations=1 runtime: 89918us
towers: innerIterations=1 runtime: 92158us
towers: innerIterations=1 runtime: 12525us
towers: innerIterations=1 runtime: 14293us
towers: innerIterations=1 runtime: 77541us
towers: innerIterations=1 runtime: 79638us
towers: iterations=10 average: 78967.9us total: 789679us

Total Runtime: 789679us
```

## Performance Metrics
- **Average Runtime**: 78,967.9 microseconds (78.97 ms)
- **Total Runtime**: 789,679 microseconds (789.68 ms)
- **First Iteration**: 213,282 microseconds (213.28 ms) - includes warmup
- **Subsequent Iterations**: 12,525 - 92,158 microseconds
- **Best Time**: 12,525 microseconds (12.53 ms)
- **Worst Time (excluding first)**: 92,158 microseconds (92.16 ms)

## Observations
- First iteration shows warmup overhead (17x slower than best time)
- High variance in performance after warmup (12.5ms - 92ms range)
- Performance appears unstable with two distinct performance regimes visible
- Iterations 7-8 show much better performance (12-14ms) compared to others (54-92ms)
- Possible deoptimization or GC interference causing variance
- This is the slowest benchmark overall with highest total runtime
- No errors or warnings encountered
- Benchmark completed successfully
