# CPU Profiling Results Summary

**Date:** 2025-10-06
**Agent:** cpu-sampler-agent
**Analysis ID:** performance-analysis-2025-10-06-14-19-16

## Overview

This document summarizes the results from comprehensive CPU profiling of 5 Lox benchmarks using GraalVM's compilation statistics, CPU sampler with tier information, and flamegraph generation.

## Profiling Phases Executed

### Phase 1: Compilation Statistics (All Benchmarks)
- **Command:** `--engine.CompilationStatistics --engine.CompilationStatisticDetails`
- **Benchmarks:** queens, sieve, towers, permute, list
- **Purpose:** Understand JIT compilation behavior, inlining decisions, and node distributions

### Phase 2: CPU Sampling with Tier Information
- **Command:** `--cpusampler --cpusampler.ShowTiers=true`
- **Benchmarks:** permute, queens, sieve
- **Purpose:** Analyze execution time across compilation tiers (T0=interpreted, T1/T2=compiled)

### Phase 3: Flamegraph Generation
- **Command:** `--cpusampler --cpusampler.Output=flamegraph --cpusampler.OutputFile=...`
- **Benchmarks:** sieve, towers, list
- **Purpose:** Visualize call stacks and identify performance hotspots

## Output Files

### Compilation Statistics
Location: `/Users/antonykamp/Projects/hpi-ma/byopl24-02/performance-analysis-2025-10-06-14-19-16/profiling/compilation_stats/`

- `queens_stats.txt` (38K)
- `sieve_stats.txt` (241K)
- `towers_stats.txt` (42K)
- `permute_stats.txt` (37K)
- `list_stats.txt` (38K)

### CPU Sampler Tier Information
Location: `/Users/antonykamp/Projects/hpi-ma/byopl24-02/performance-analysis-2025-10-06-14-19-16/profiling/cpu_sampler/`

- `queens_tiers.txt` (3.6K)
- `sieve_tiers.txt` (204K)
- `permute_tiers.txt` (3.7K)

### Flamegraphs
Location: `/Users/antonykamp/Projects/hpi-ma/byopl24-02/performance-analysis-2025-10-06-14-19-16/profiling/flamegraphs/`

- `sieve_flamegraph.svg` (64K)
- `towers_flamegraph.svg` (69K)
- `list_flamegraph.svg` (68K)

## Key Findings

### 1. Queens Benchmark (size=8, runs=10)

**Compilation Statistics:**
```
Compilations:           51 total (49 successful, 2 interrupted)
Compilation Accuracy:   1.000000
Invalidations:          0
Splits:                 0
```

**Performance:**
- Runtime progression: 321ms → 114ms → 29ms → 21ms → 18-23ms (steady state)
- Total runtime: 565ms (average: 70.7ms per iteration)
- Strong warmup effect: 17.9x improvement from first to steady state

**Tier Distribution (CPU Sampler):**
- `getRowColumn`: 90.7% total time, 100% in T0 (interpreted) - **HOTSPOT**
- `queens`: 93.3% total time, 15.7% T0, 84.3% T1
- `placeQueen`: 90.7% total time, 10.3% T0, 89.7% T1

**Key Issue:** `getRowColumn` spending 100% time in interpreted mode despite being the hottest function. This suggests it may not be compiling properly or is being deoptimized.

**Inlining:**
- 47 inlined calls (avg 0.92 per compilation)
- 10 dispatched calls (avg 0.20 per compilation)
- Max 24 inlined calls in `queens` function

### 2. Sieve Benchmark (size=5000, runs=10)

**Compilation Statistics:**
```
Compilations:           18 total (18 successful)
Compilation Accuracy:   1.000000
Invalidations:          0
Bailouts:               0
```

**Performance:**
- Runtime shows high variability with periodic spikes
- Initial run: 180ms → drops to 0.5-2.6ms range
- Periodic spikes up to 7.4ms observed during execution
- Suggests potential deoptimization or GC interference

**Deoptimization Evidence:**
```
DeoptimizeNode:         count=9, avg=6.33-19.11, max=20-80
DynamicDeoptimizeNode:  count=9, avg=8.22, max=25
```

**CPU Sampler:** Output file size (204K) indicates extensive sampling data, suggesting complex execution profile

### 3. Permute Benchmark (size=6, runs=10)

**Compilation Statistics:**
```
Compilations:           8 total (5 successful, 1 temporary bailout)
Temporary Bailouts:     1 (CancellationBailoutException)
Invalidations:          0
```

**Performance:**
- First run: 117ms (cold start)
- Subsequent runs: 2.6-8.8ms
- 117x warmup improvement (most dramatic of all benchmarks)
- Total runtime: 140ms (average: 23.4ms)

**Tier Distribution (CPU Sampler):**
- `swap`: 55.6% total time - 50% T0, 20% T1, 30% T2
- `permute`: 77.8% total time - 85.7% T0, 14.3% T1, 0% T2

**Key Observation:** Mixed tier execution on hottest function suggests compilation is happening but not optimally. The `swap` function shows good progression through tiers (T0→T1→T2), but `permute` is mostly interpreted.

### 4. Towers Benchmark (size=13, runs=10)

**Compilation Statistics:**
```
Compilations:           129 total (124 successful)
Temporary Bailouts:     1 (CancellationBailoutException)
Permanent Bailouts:     1 (ValuePhi not reduced to constant)
```

**Permanent Bailout Details:**
```
Partial evaluation did not reduce value to a constant:
  ValuePhi(8, i32) at LoopBegin
```

**Key Issue:** The permanent bailout indicates a loop variable that cannot be optimized away during partial evaluation. This is typical for deeply recursive algorithms where the compiler cannot unroll or eliminate loops.

**Flamegraph:** Generated to analyze recursion patterns in Towers of Hanoi algorithm.

### 5. List Benchmark (size=18, runs=10)

**Compilation Statistics:**
```
Compilations:           12 total (9 successful)
Permanent Bailouts:     3 (ValuePhi not reduced to constant)
Invalidations:          0
```

**Permanent Bailout Details:**
```
Partial evaluation did not reduce value to a constant:
  - ValuePhi(8, i32) at LoopBegin (2 instances)
```

**Key Issue:** Similar to towers, multiple permanent bailouts suggest loop-intensive code with dynamic iteration counts that cannot be optimized during partial evaluation. This is expected for linked list traversal.

**Flamegraph:** Generated to analyze linked list operation patterns.

## Anomalies and Issues

### 1. getRowColumn in Queens (Critical)
- **Issue:** 90.7% of execution time in interpreted mode (T0)
- **Impact:** Major performance bottleneck
- **Hypothesis:** Function may be too small for compilation threshold, or contains patterns that prevent compilation
- **Recommendation:** Investigate inlining opportunities or compilation thresholds

### 2. Sieve Runtime Variability
- **Issue:** Periodic runtime spikes (0.5ms → 7.4ms)
- **Evidence:** DeoptimizeNode and DynamicDeoptimizeNode present
- **Hypothesis:** Type instability or speculative optimization failures
- **Recommendation:** Analyze deoptimization traces to identify unstable assumptions

### 3. Permute Extreme Warmup (117x)
- **Issue:** Dramatic first-run penalty (117ms vs 2.6ms)
- **Evidence:** Temporary compilation bailout
- **Impact:** Cold-start performance severely degraded
- **Recommendation:** Investigate compilation bailout cause and potential ahead-of-time warming

### 4. Permanent Bailouts in Towers and List
- **Issue:** Partial evaluation failures for loop values
- **Impact:** Limits optimization potential for recursive/iterative code
- **Evidence:** 1 bailout in towers, 3 in list
- **Note:** May be inherent to algorithm structure (expected behavior)

## Execution Commands

All profiling runs used the following patterns:

### Compilation Statistics
```bash
./lox --experimental-options \
  --engine.CompilationStatistics \
  --engine.CompilationStatisticDetails \
  harness.lox <benchmark> <size> <runs>
```

### CPU Sampler with Tiers
```bash
./lox --experimental-options \
  --cpusampler \
  --cpusampler.ShowTiers=true \
  harness.lox <benchmark> <size> <runs>
```

### Flamegraph Generation
```bash
./lox --experimental-options \
  --cpusampler \
  --cpusampler.Output=flamegraph \
  --cpusampler.OutputFile=<output.svg> \
  harness.lox <benchmark> <size> <runs>
```

## Next Steps

Based on these profiling results, the following analysis tasks are recommended:

1. **Investigate queens.getRowColumn compilation failure** - Why is the hottest function not compiling?
2. **Analyze sieve deoptimization patterns** - What assumptions are being invalidated?
3. **Profile permute cold-start behavior** - Why such extreme warmup penalty?
4. **Examine flamegraphs** - Identify call stack patterns in sieve, towers, and list
5. **Review permanent bailouts** - Are these optimizable or inherent limitations?

## Summary Statistics

| Benchmark | Compilations | Success | Bailouts | Invalidations | Warmup Factor |
|-----------|-------------|---------|----------|---------------|---------------|
| queens    | 51          | 49      | 0        | 0             | 17.9x         |
| sieve     | 18          | 18      | 0        | 0             | 360x          |
| towers    | 129         | 124     | 1 temp, 1 perm | 0       | N/A           |
| permute   | 8           | 5       | 1 temp   | 0             | 117x          |
| list      | 12          | 9       | 3 perm   | 0             | N/A           |

**Total Profiling Outputs:** 11 files (5 compilation stats, 3 CPU sampler, 3 flamegraphs)

---

*Generated by cpu-sampler-agent on 2025-10-06*
