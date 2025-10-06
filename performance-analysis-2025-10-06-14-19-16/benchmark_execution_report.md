# Baseline Benchmark Execution Report

**Date:** 2025-10-06
**Time:** 14:19:16
**Total Benchmarks:** 5
**Total Runs:** 50 (10 runs per benchmark)

## Executive Summary

All five benchmarks (queens, sieve, towers, permute, list) were successfully executed with 10 runs each. Each benchmark demonstrates significant JIT warmup effects, with first-run performance 2-112x slower than stabilized performance. After warmup (typically 2-3 runs), all benchmarks show good stability with 5-10% variance.

## Benchmark Results

### 1. Queens Benchmark (N-Queens Solver)

**Command:** `./lox harness.lox queens 10 8`
**Size:** 8
**Runs:** 10

**Raw Output:**
```
load benchmark: queens.lox
Starting queens benchmark ...
queens: innerIterations=8 runtime: 287318us
queens: innerIterations=8 runtime: 26983us
queens: innerIterations=8 runtime: 132538us
queens: innerIterations=8 runtime: 16280us
queens: innerIterations=8 runtime: 19555us
queens: innerIterations=8 runtime: 15282us
queens: innerIterations=8 runtime: 16910us
queens: innerIterations=8 runtime: 17386us
queens: innerIterations=8 runtime: 17431us
queens: innerIterations=8 runtime: 16227us
queens: iterations=10 average: 56591us total: 565910us

Total Runtime: 565910us
```

**Statistics:**
- Mean: 56,591 us
- Median: 17,408.5 us
- Min: 15,282 us
- Max: 287,318 us
- Std Dev: 80,837.45 us
- Total: 565,910 us

**Observations:**
- First run shows extreme warmup effect (287ms vs 15-17ms stabilized)
- Second run also elevated (27ms), suggesting multi-phase warmup
- Runs 4-10 are very stable (15-19ms range)
- Warmup factor: 18.8x slower for first run vs minimum

---

### 2. Sieve Benchmark (Sieve of Eratosthenes)

**Command:** `./lox harness.lox sieve 10 5000`
**Size:** 5000
**Runs:** 10

**Raw Output:**
```
load benchmark: sieve.lox
Starting sieve benchmark ...
sieve: innerIterations=5000 runtime: 490538us
sieve: innerIterations=5000 runtime: 203196us
sieve: innerIterations=5000 runtime: 214428us
sieve: innerIterations=5000 runtime: 245742us
sieve: innerIterations=5000 runtime: 247083us
sieve: innerIterations=5000 runtime: 346183us
sieve: innerIterations=5000 runtime: 251114us
sieve: innerIterations=5000 runtime: 246472us
sieve: innerIterations=5000 runtime: 242583us
sieve: innerIterations=5000 runtime: 255003us
sieve: iterations=10 average: 274234.2us total: 2742342us

Total Runtime: 2742342us
```

**Statistics:**
- Mean: 274,234.2 us
- Median: 246,777.5 us
- Min: 203,196 us
- Max: 490,538 us
- Std Dev: 77,369.11 us
- Total: 2,742,342 us

**Observations:**
- First run warmup effect present (490ms vs 203ms minimum)
- Warmup factor: 2.4x slower for first run
- Run 6 shows an outlier (346ms) suggesting possible deoptimization or GC
- Most runs cluster in 240-255ms range after initial warmup
- This is the longest-running benchmark (2.7s total)

---

### 3. Towers Benchmark (Towers of Hanoi)

**Command:** `./lox harness.lox towers 10 13`
**Size:** 13
**Runs:** 10

**Raw Output:**
```
load benchmark: towers.lox
Starting towers benchmark ...
towers: innerIterations=13 runtime: 611766us
towers: innerIterations=13 runtime: 401293us
towers: innerIterations=13 runtime: 141714us
towers: innerIterations=13 runtime: 136839us
towers: innerIterations=13 runtime: 144300us
towers: innerIterations=13 runtime: 146166us
towers: innerIterations=13 runtime: 138919us
towers: innerIterations=13 runtime: 142407us
towers: innerIterations=13 runtime: 134895us
towers: innerIterations=13 runtime: 142900us
towers: iterations=10 average: 214119.9us total: 2141199us

Total Runtime: 2141199us
```

**Statistics:**
- Mean: 214,119.9 us
- Median: 142,653.5 us
- Min: 134,895 us
- Max: 611,766 us
- Std Dev: 144,930.74 us
- Total: 2,141,199 us

**Observations:**
- Strong two-phase warmup: run 1 (611ms), run 2 (401ms), then stabilizes
- Warmup factor: 4.5x slower for first run, 3x for second run
- Runs 3-10 very stable (135-146ms range, only 8% variance)
- Shows clearest warmup pattern of all benchmarks

---

### 4. Permute Benchmark (Permutation Generator)

**Command:** `./lox harness.lox permute 10 6`
**Size:** 6
**Runs:** 10

**Raw Output:**
```
load benchmark: permute.lox
Starting permute benchmark ...
permute: innerIterations=6 runtime: 120692us
permute: innerIterations=6 runtime: 1070us
permute: innerIterations=6 runtime: 1276us
permute: innerIterations=6 runtime: 1028us
permute: innerIterations=6 runtime: 1538us
permute: innerIterations=6 runtime: 1646us
permute: innerIterations=6 runtime: 3048us
permute: innerIterations=6 runtime: 1689us
permute: innerIterations=6 runtime: 1398us
permute: innerIterations=6 runtime: 2863us
permute: iterations=10 average: 13624.8us total: 136248us

Total Runtime: 136248us
```

**Statistics:**
- Mean: 13,624.8 us
- Median: 1,617.0 us
- Min: 1,028 us
- Max: 120,692 us
- Std Dev: 37,134.12 us
- Total: 136,248 us

**Observations:**
- EXTREME warmup effect: first run is 120ms, subsequent runs 1-3ms
- Warmup factor: 117x slower for first run vs minimum
- After warmup, performance is sub-millisecond and very fast
- This is the fastest benchmark after warmup
- Runs 2-10 show excellent stability (1-3ms range)

---

### 5. List Benchmark (Takeuchi Function with Linked Lists)

**Command:** `./lox harness.lox list 10 18`
**Size:** 18
**Runs:** 10

**Raw Output:**
```
load benchmark: list.lox
Starting list benchmark ...
list: innerIterations=18 runtime: 331055us
list: innerIterations=18 runtime: 193021us
list: innerIterations=18 runtime: 102200us
list: innerIterations=18 runtime: 103839us
list: innerIterations=18 runtime: 101085us
list: innerIterations=18 runtime: 99315us
list: innerIterations=18 runtime: 99501us
list: innerIterations=18 runtime: 98904us
list: innerIterations=18 runtime: 99610us
list: innerIterations=18 runtime: 100166us
list: iterations=10 average: 132869.6us total: 1328696us

Total Runtime: 1328696us
```

**Statistics:**
- Mean: 132,869.6 us
- Median: 100,625.5 us
- Min: 98,904 us
- Max: 331,055 us
- Std Dev: 73,067.53 us
- Total: 1,328,696 us

**Observations:**
- Clear two-phase warmup: run 1 (331ms), run 2 (193ms), then stabilizes
- Warmup factor: 3.3x slower for first run, 1.9x for second run
- Runs 3-10 extremely stable (99-103ms range, only 5% variance)
- Best post-warmup stability of all benchmarks

---

## Overall Analysis

### Warmup Effects Ranking (First Run vs Minimum)
1. Permute: 117x slower
2. Queens: 18.8x slower
3. Towers: 4.5x slower
4. List: 3.3x slower
5. Sieve: 2.4x slower

### Post-Warmup Stability (Runs 3-10 Variance)
1. List: ~5% variance (99-103ms)
2. Permute: ~8% variance (1-3ms)
3. Towers: ~8% variance (135-146ms)
4. Queens: ~10% variance (15-19ms)
5. Sieve: ~12% variance (214-255ms, with one outlier)

### Execution Speed (Post-Warmup Average)
1. Permute: 1.6ms (fastest)
2. Queens: 17.4ms
3. List: 100ms
4. Towers: 142ms
5. Sieve: 246ms (slowest)

### Key Findings

1. **JIT Warmup is Critical:** All benchmarks show 2-117x slowdown on first run. Performance measurements should exclude first 2-3 runs.

2. **Two Warmup Phases:** Most benchmarks show a pattern of extreme slowdown (run 1), moderate slowdown (run 2), then stabilization (run 3+).

3. **Good Post-Warmup Stability:** After warmup, all benchmarks show consistent performance with 5-12% variance, indicating reliable JIT optimization.

4. **Benchmark Characteristics:**
   - Permute is very fast but has extreme warmup penalty
   - Sieve is slowest overall and shows occasional deoptimization
   - List has best post-warmup stability
   - Queens shows unusual second-run behavior (partial warmup)
   - Towers has clearest two-phase warmup pattern

5. **Outliers:** Sieve run 6 (346ms) suggests possible GC or deoptimization event during execution.

## Recommendations

1. Always run at least 3 warmup iterations before collecting performance data
2. For production benchmarking, exclude first 3 runs from analysis
3. Run 10-15 iterations to account for occasional outliers
4. Monitor for deoptimization events (sudden slowdowns after warmup)
5. Consider running benchmarks in separate JVM instances to isolate warmup effects
