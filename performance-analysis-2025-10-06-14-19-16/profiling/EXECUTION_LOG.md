# Profiling Execution Log

**Date:** 2025-10-06
**Working Directory:** `/Users/antonykamp/Projects/hpi-ma/byopl24-02`

## Phase 1: Compilation Statistics

### Queens Benchmark
```bash
./lox --experimental-options \
  --engine.CompilationStatistics \
  --engine.CompilationStatisticDetails \
  harness.lox queens 8 10 \
  > performance-analysis-2025-10-06-14-19-16/profiling/compilation_stats/queens_stats.txt 2>&1
```
**Status:** ✓ Success
**Output File:** `compilation_stats/queens_stats.txt` (38K)
**Key Metrics:**
- 51 compilations (49 successful, 2 interrupted)
- 0 invalidations
- Compilation accuracy: 1.000000

---

### Sieve Benchmark
```bash
./lox --experimental-options \
  --engine.CompilationStatistics \
  --engine.CompilationStatisticDetails \
  harness.lox sieve 5000 10 \
  > performance-analysis-2025-10-06-14-19-16/profiling/compilation_stats/sieve_stats.txt 2>&1
```
**Status:** ✓ Success
**Output File:** `compilation_stats/sieve_stats.txt` (241K)
**Key Metrics:**
- 18 compilations (18 successful)
- 0 invalidations
- High variability in runtime (180ms → 0.5ms range)

---

### Towers Benchmark
```bash
./lox --experimental-options \
  --engine.CompilationStatistics \
  --engine.CompilationStatisticDetails \
  harness.lox towers 13 10 \
  > performance-analysis-2025-10-06-14-19-16/profiling/compilation_stats/towers_stats.txt 2>&1
```
**Status:** ✓ Success
**Output File:** `compilation_stats/towers_stats.txt` (42K)
**Key Metrics:**
- 129 compilations (124 successful)
- 1 temporary bailout (CancellationBailoutException)
- 1 permanent bailout (ValuePhi not reduced to constant)

---

### Permute Benchmark
```bash
./lox --experimental-options \
  --engine.CompilationStatistics \
  --engine.CompilationStatisticDetails \
  harness.lox permute 6 10 \
  > performance-analysis-2025-10-06-14-19-16/profiling/compilation_stats/permute_stats.txt 2>&1
```
**Status:** ✓ Success
**Output File:** `compilation_stats/permute_stats.txt` (37K)
**Key Metrics:**
- 8 compilations (5 successful)
- 1 temporary bailout (CancellationBailoutException)
- 117x warmup improvement (117ms → 2.6ms)

---

### List Benchmark
```bash
./lox --experimental-options \
  --engine.CompilationStatistics \
  --engine.CompilationStatisticDetails \
  harness.lox list 18 10 \
  > performance-analysis-2025-10-06-14-19-16/profiling/compilation_stats/list_stats.txt 2>&1
```
**Status:** ✓ Success
**Output File:** `compilation_stats/list_stats.txt` (38K)
**Key Metrics:**
- 12 compilations (9 successful)
- 3 permanent bailouts (ValuePhi not reduced to constant)

---

## Phase 2: CPU Sampling with Tier Information

### Permute Benchmark
```bash
./lox --experimental-options \
  --cpusampler \
  --cpusampler.ShowTiers=true \
  harness.lox permute 6 10 \
  > performance-analysis-2025-10-06-14-19-16/profiling/cpu_sampler/permute_tiers.txt 2>&1
```
**Status:** ✓ Success
**Output File:** `cpu_sampler/permute_tiers.txt` (3.7K)
**Sampling:** 18 samples (period: 10ms, missed: 8)
**Hotspot:**
- `swap`: 55.6% total time (50% T0, 20% T1, 30% T2)
- `permute`: 77.8% total time (85.7% T0, 14.3% T1)

---

### Queens Benchmark
```bash
./lox --experimental-options \
  --cpusampler \
  --cpusampler.ShowTiers=true \
  harness.lox queens 8 10 \
  > performance-analysis-2025-10-06-14-19-16/profiling/cpu_sampler/queens_tiers.txt 2>&1
```
**Status:** ✓ Success
**Output File:** `cpu_sampler/queens_tiers.txt` (3.6K)
**Sampling:** 75 samples (period: 10ms, missed: 7)
**Hotspot:**
- `getRowColumn`: 90.7% total time (100% T0) ⚠️ **CRITICAL ISSUE**
- `queens`: 93.3% total time (15.7% T0, 84.3% T1)
- `placeQueen`: 90.7% total time (10.3% T0, 89.7% T1)

---

### Sieve Benchmark
```bash
./lox --experimental-options \
  --cpusampler \
  --cpusampler.ShowTiers=true \
  harness.lox sieve 5000 10 \
  > performance-analysis-2025-10-06-14-19-16/profiling/cpu_sampler/sieve_tiers.txt 2>&1
```
**Status:** ✓ Success
**Output File:** `cpu_sampler/sieve_tiers.txt` (204K)
**Note:** Large output suggests extensive sampling data and complex execution profile

---

## Phase 3: Flamegraph Generation

### Sieve Benchmark
```bash
./lox --experimental-options \
  --cpusampler \
  --cpusampler.Output=flamegraph \
  --cpusampler.OutputFile=performance-analysis-2025-10-06-14-19-16/profiling/flamegraphs/sieve_flamegraph.svg \
  harness.lox sieve 5000 10
```
**Status:** ✓ Success
**Output File:** `flamegraphs/sieve_flamegraph.svg` (64K)
**Purpose:** Analyze deoptimization patterns in array-based prime sieve

---

### Towers Benchmark
```bash
./lox --experimental-options \
  --cpusampler \
  --cpusampler.Output=flamegraph \
  --cpusampler.OutputFile=performance-analysis-2025-10-06-14-19-16/profiling/flamegraphs/towers_flamegraph.svg \
  harness.lox towers 13 10
```
**Status:** ✓ Success
**Output File:** `flamegraphs/towers_flamegraph.svg` (69K)
**Purpose:** Profile recursion patterns in Towers of Hanoi

---

### List Benchmark
```bash
./lox --experimental-options \
  --cpusampler \
  --cpusampler.Output=flamegraph \
  --cpusampler.OutputFile=performance-analysis-2025-10-06-14-19-16/profiling/flamegraphs/list_flamegraph.svg \
  harness.lox list 18 10
```
**Status:** ✓ Success
**Output File:** `flamegraphs/list_flamegraph.svg` (68K)
**Purpose:** Analyze linked list operation performance

---

## Summary

**Total Profiling Runs:** 11
**Phase 1 (Compilation Stats):** 5 benchmarks ✓
**Phase 2 (CPU Sampler + Tiers):** 3 benchmarks ✓
**Phase 3 (Flamegraphs):** 3 benchmarks ✓

**All tasks completed successfully.**

### File Structure
```
performance-analysis-2025-10-06-14-19-16/profiling/
├── compilation_stats/
│   ├── queens_stats.txt (38K)
│   ├── sieve_stats.txt (241K)
│   ├── towers_stats.txt (42K)
│   ├── permute_stats.txt (37K)
│   └── list_stats.txt (38K)
├── cpu_sampler/
│   ├── queens_tiers.txt (3.6K)
│   ├── sieve_tiers.txt (204K)
│   └── permute_tiers.txt (3.7K)
├── flamegraphs/
│   ├── sieve_flamegraph.svg (64K)
│   ├── towers_flamegraph.svg (69K)
│   └── list_flamegraph.svg (68K)
├── PROFILING_SUMMARY.md
└── EXECUTION_LOG.md (this file)
```

**Total Size:** ~750KB of profiling data

---

*Generated by cpu-sampler-agent on 2025-10-06*
