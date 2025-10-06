# Queens Benchmark Results

## Execution Details
- **Command**: `./lox harness.lox queens 10 1`
- **Date**: 2025-10-06
- **Iterations**: 10
- **Inner Iterations**: 1

## Raw Output
```
load benchmark: queens.lox
Starting queens benchmark ...
queens: innerIterations=1 runtime: 113909us
queens: innerIterations=1 runtime: 26044us
queens: innerIterations=1 runtime: 24975us
queens: innerIterations=1 runtime: 22466us
queens: innerIterations=1 runtime: 32735us
queens: innerIterations=1 runtime: 24157us
queens: innerIterations=1 runtime: 23795us
queens: innerIterations=1 runtime: 21603us
queens: innerIterations=1 runtime: 22407us
queens: innerIterations=1 runtime: 21734us
queens: iterations=10 average: 33382.5us total: 333825us

Total Runtime: 333825us
```

## Performance Metrics
- **Average Runtime**: 33,382.5 microseconds (33.38 ms)
- **Total Runtime**: 333,825 microseconds (333.83 ms)
- **First Iteration**: 113,909 microseconds (113.91 ms) - includes warmup
- **Subsequent Iterations**: 21,603 - 32,735 microseconds
- **Best Time**: 21,603 microseconds (21.60 ms)
- **Worst Time (excluding first)**: 32,735 microseconds (32.74 ms)

## Observations
- First iteration shows significant warmup overhead (5.27x slower than best time)
- After warmup, performance stabilizes in the 21-33ms range
- Average performance of ~25ms for warmed-up iterations
- No errors or warnings encountered
- Benchmark completed successfully
