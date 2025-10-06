# List Benchmark Results

## Execution Details
- **Command**: `./lox harness.lox list 10 1`
- **Date**: 2025-10-06
- **Iterations**: 10
- **Inner Iterations**: 1

## Raw Output
```
load benchmark: list.lox
Starting list benchmark ...
list: innerIterations=1 runtime: 105929us
list: innerIterations=1 runtime: 21873us
list: innerIterations=1 runtime: 19661us
list: innerIterations=1 runtime: 17821us
list: innerIterations=1 runtime: 17508us
list: innerIterations=1 runtime: 20868us
list: innerIterations=1 runtime: 13197us
list: innerIterations=1 runtime: 21039us
list: innerIterations=1 runtime: 18131us
list: innerIterations=1 runtime: 16914us
list: iterations=10 average: 27294.1us total: 272941us

Total Runtime: 272941us
```

## Performance Metrics
- **Average Runtime**: 27,294.1 microseconds (27.29 ms)
- **Total Runtime**: 272,941 microseconds (272.94 ms)
- **First Iteration**: 105,929 microseconds (105.93 ms) - includes warmup
- **Subsequent Iterations**: 13,197 - 21,873 microseconds
- **Best Time**: 13,197 microseconds (13.20 ms)
- **Worst Time (excluding first)**: 21,873 microseconds (21.87 ms)

## Observations
- First iteration shows typical warmup overhead (8x slower than best time)
- After warmup, performance stabilizes in the 13-22ms range
- More consistent performance compared to towers benchmark
- Average warmed-up performance around 18-19ms
- Similar performance profile to queens benchmark
- No errors or warnings encountered
- Benchmark completed successfully
