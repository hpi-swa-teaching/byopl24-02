# Performance Hypotheses and Analysis Plan

**Generated:** 2025-10-06
**Based on:** Baseline benchmark results (10 runs per benchmark)

## Executive Summary

Analysis of baseline benchmarks reveals extreme warmup effects (2-117x) and deoptimization events. This document outlines hypotheses about performance bottlenecks and recommends profiling strategies to validate them.

## Baseline Performance Summary

| Benchmark | Size | Mean (us) | Post-Warmup (us) | Warmup Factor | Variance |
|-----------|------|-----------|------------------|---------------|----------|
| queens    | 8    | 56,591    | ~17,000          | 18.8x         | 10%      |
| sieve     | 5000 | 274,234   | ~246,000         | 2.4x          | 12%      |
| towers    | 13   | 214,120   | ~142,000         | 4.5x          | 8%       |
| permute   | 6    | 13,625    | ~1,600           | 117x          | 8%       |
| list      | 18   | 132,870   | ~100,000         | 3.3x          | 5%       |

## Performance Hypotheses

### Hypothesis 1: Delayed Compilation Threshold Causes Extreme Warmup in Permute

**Description:** The permute benchmark shows a 117x warmup penalty (120ms first run vs 1ms stable). This extreme effect suggests the JIT compiler is not triggering compilation early enough, forcing the first iteration to run in interpreter mode or low-tier compilation.

**Evidence:**
- Permute first run: 120,692 us
- Permute runs 2-10: 1,028-3,048 us (avg ~1,600 us)
- Dramatic drop after first iteration suggests tier transition
- Pattern indicates interpreter → compiled transition rather than gradual optimization

**Expected Root Causes:**
1. Compilation threshold too high for short-running code
2. Method call frequency not reaching threshold in single iteration
3. Possible OSR (On-Stack Replacement) not triggering for loops
4. Small code size may delay profiling data collection

**Validation Strategy:**
- Use `--engine.TraceCompilation` to observe when compilation occurs
- Check compilation tier transitions (interpreted → tier 1 → tier 2)
- Verify if compilation happens during run 1 or between runs

---

### Hypothesis 2: Multi-Phase Warmup Indicates Tiered Compilation Progression

**Description:** Queens, towers, and list show two-phase warmup patterns (run 1 very slow, run 2 moderately slow, run 3+ stable), suggesting progression through GraalVM's compilation tiers.

**Evidence:**
- Queens: Run 1 (287ms) → Run 2 (27ms) → Runs 3+ (15-19ms)
- Towers: Run 1 (611ms) → Run 2 (401ms) → Runs 3+ (135-146ms)
- List: Run 1 (331ms) → Run 2 (193ms) → Runs 3+ (99-103ms)

**Expected Root Causes:**
1. Run 1: Interpreted or minimal profiling
2. Run 2: Tier 1 compilation (low optimization)
3. Run 3+: Tier 2 compilation (full optimization with inlining)
4. Profiling overhead in early runs collecting data for optimization

**Validation Strategy:**
- Use `--engine.TraceCompilationDetails` to see queuing and tier progression
- Compare time spent in different compilation tiers
- Identify when tier 1 vs tier 2 compilation occurs

---

### Hypothesis 3: Deoptimization Event in Sieve Run 6 Due to Type Profiling Change

**Description:** Sieve shows an outlier at run 6 (346ms vs 240-255ms typical), suggesting a deoptimization event where optimized code was invalidated and recompiled.

**Evidence:**
- Runs 2-5: 203-247ms (relatively stable)
- Run 6: 346ms (40% slower)
- Runs 7-10: 242-255ms (back to normal)
- Pattern suggests: optimize → deopt → reoptimize

**Expected Root Causes:**
1. Type speculation failure (e.g., integer overflow to double)
2. Array bounds check assumption violation
3. Loop unrolling or vectorization assumption invalidated
4. Hidden class/shape change in object property access

**Validation Strategy:**
- Use `--engine.TraceDeoptimization` to catch deoptimization reasons
- Use `--engine.TracePerformanceWarnings` to identify unstable operations
- CPU sampler with tier tracking to see tier transitions during run 6

---

### Hypothesis 4: Missing Inlining Opportunities Limit Peak Performance

**Description:** After warmup, benchmarks may still have performance headroom if critical functions are not being inlined. The stabilized performance might be suboptimal due to virtual call overhead.

**Evidence:**
- Post-warmup performance is stable but unknown if optimal
- Truffle DSL specialization may not cover all type combinations
- Function call overhead in tight loops (especially permute, queens)

**Expected Root Causes:**
1. Polymorphic call sites preventing inlining
2. Method size exceeding inlining threshold
3. Recursive calls not optimized
4. Insufficient call frequency for inlining decisions

**Validation Strategy:**
- Use `--engine.TraceInlining` to see inlining decisions
- CPU sampler to identify hot methods with high call counts
- Check for monomorphic vs polymorphic call sites
- Compare self-time vs total-time in profiling data

---

### Hypothesis 5: Bytecode Interpreter Overhead Dominates First Run Performance

**Description:** The universal warmup penalty across all benchmarks suggests the Truffle bytecode interpreter has significant overhead before compilation, with profiling instrumentation adding additional cost.

**Evidence:**
- ALL benchmarks show first-run penalties (no exceptions)
- Warmup factor correlates inversely with benchmark complexity
  - Simple permute: 117x (heavy interpreter overhead)
  - Complex sieve: 2.4x (more computation, less overhead ratio)

**Expected Root Causes:**
1. Bytecode dispatch overhead in interpreter
2. Profiling instrumentation collecting type/branch data
3. Frame materialization for closures/variables
4. Lack of specialized nodes before profiling feedback

**Validation Strategy:**
- CPU sampler on first run to see interpreter time
- Use `--cpusampler.ShowTiers=true` to separate interpreted vs compiled
- Compare tier 0 (interpreter) vs tier 1 vs tier 2 time distribution
- Identify which operations are slow in interpreter mode

---

## Profiling Recommendations

### Priority 1: Understand Compilation Behavior (All Benchmarks)

**Goal:** Map the compilation timeline and tier transitions for each benchmark

**Tools:**
1. **Compilation Tracing** (all benchmarks)
   - Command: `--engine.TraceCompilation --engine.TraceCompilationDetails`
   - What to look for:
     - When does first compilation occur (during run 1 or after)?
     - Which tier (1 or 2)?
     - Compilation queue behavior
     - Time spent in Truffle tier vs Graal tier

2. **Compilation Statistics** (all benchmarks)
   - Command: `--engine.CompilationStatistics`
   - What to look for:
     - Total compilations, invalidations, dequeues
     - AST node statistics (complexity indicators)
     - Graal node counts (optimization effectiveness)
     - Compilation success/failure rates

**Expected Insights:**
- Validate hypothesis 1 (delayed compilation in permute)
- Validate hypothesis 2 (tiered compilation progression)
- Identify compilation thresholds and triggers

---

### Priority 2: Identify Hot Paths and Optimization Quality

**Goal:** Find performance bottlenecks and verify optimization effectiveness

**Tools:**
1. **CPU Sampler with Tier Information** (focus: permute, queens, sieve)
   - Command: `--cpusampler --cpusampler.ShowTiers=true --cpusampler.Output=histogram`
   - What to look for:
     - Which functions are hot (self-time)
     - Time spent in interpreter vs tier 1 vs tier 2
     - Call tree depth and frequency
     - Ratio of compiled vs interpreted execution

2. **Flamegraph Analysis** (focus: sieve, towers, list)
   - Command: `--cpusampler --cpusampler.Output=flamegraph --cpusampler.OutputFile=<benchmark>_flamegraph.svg`
   - What to look for:
     - Call stack visualization
     - Wide stacks (hot paths)
     - Narrow stacks (rare paths)
     - Compiler-inserted code (e.g., type checks, guards)

**Expected Insights:**
- Validate hypothesis 4 (inlining opportunities)
- Validate hypothesis 5 (interpreter overhead)
- Identify actual hot methods vs expected hot methods

---

### Priority 3: Investigate Deoptimization and Instability

**Goal:** Understand the sieve run 6 outlier and any unstable optimizations

**Tools:**
1. **Deoptimization Tracing** (focus: sieve)
   - Command: `--engine.TraceDeoptimization`
   - What to look for:
     - When deoptimizations occur
     - Reason for deoptimization
     - Source location of unstable code

2. **Performance Warnings** (all benchmarks)
   - Command: `--engine.TracePerformanceWarnings=all`
   - What to look for:
     - Virtual calls not inlined
     - Type checks that cannot be eliminated
     - Store operations with non-constant locations

**Expected Insights:**
- Validate hypothesis 3 (deoptimization in sieve)
- Identify type instabilities
- Find optimization barriers

---

### Priority 4: Deep Dive into Optimization Decisions

**Goal:** Understand why certain optimizations succeed or fail

**Tools:**
1. **Inlining Tracing** (focus: permute, queens - fastest benchmarks)
   - Command: `--engine.TraceInlining`
   - What to look for:
     - Which functions get inlined
     - Inlining depth achieved
     - Reasons for NOT inlining (size, polymorphism, recursion)

2. **Method Expansion Statistics** (all benchmarks)
   - Command: `--engine.MethodExpansionStatistics=truffleTier`
   - What to look for:
     - IR node counts per method
     - Graal node explosion (code size growth)
     - Expensive operations (allocation, division, etc.)

**Expected Insights:**
- Optimization quality assessment
- Code bloat identification
- Specialization effectiveness

---

## Profiling Execution Plan

### Phase 1: Compilation Baseline (Est. 10 minutes)

**Purpose:** Understand when and how compilation happens

**Benchmarks:** All 5 (queens, sieve, towers, permute, list)

**Tools:**
- `--engine.TraceCompilation`
- `--engine.CompilationStatistics`

**Output Files:**
- `{benchmark}_compilation_trace.txt`
- `{benchmark}_compilation_stats.txt`

**Validation:**
- Hypothesis 1 (permute delayed compilation)
- Hypothesis 2 (tiered compilation)

---

### Phase 2: Hot Path Identification (Est. 15 minutes)

**Purpose:** Find where time is actually spent

**Benchmarks:** permute, queens, sieve (most interesting patterns)

**Tools:**
- `--cpusampler --cpusampler.ShowTiers=true --cpusampler.Output=histogram`

**Output Files:**
- `{benchmark}_cpu_histogram.txt`

**Validation:**
- Hypothesis 4 (inlining opportunities)
- Hypothesis 5 (interpreter overhead)

---

### Phase 3: Visual Analysis (Est. 10 minutes)

**Purpose:** Create flamegraphs for visual bottleneck identification

**Benchmarks:** sieve, towers, list (longer-running, more complex)

**Tools:**
- `--cpusampler --cpusampler.Output=flamegraph --cpusampler.OutputFile={benchmark}_flamegraph.svg`

**Output Files:**
- `{benchmark}_flamegraph.svg`

**Validation:**
- Call stack depth
- Hot method identification
- Compilation tier distribution

---

### Phase 4: Stability Analysis (Est. 10 minutes)

**Purpose:** Understand deoptimization and instability

**Benchmarks:** sieve (has known outlier), all others for comparison

**Tools:**
- `--engine.TraceDeoptimization`
- `--engine.TracePerformanceWarnings=all`

**Output Files:**
- `{benchmark}_deopt_trace.txt`
- `{benchmark}_perf_warnings.txt`

**Validation:**
- Hypothesis 3 (sieve deoptimization)
- Type stability issues
- Optimization barriers

---

### Phase 5: Optimization Deep Dive (Est. 15 minutes)

**Purpose:** Understand optimization decisions in detail

**Benchmarks:** permute (extreme warmup), sieve (deopt issues)

**Tools:**
- `--engine.TraceInlining`
- `--engine.MethodExpansionStatistics=truffleTier`

**Output Files:**
- `{benchmark}_inlining_trace.txt`
- `{benchmark}_expansion_stats.txt`

**Validation:**
- Inlining effectiveness
- Code expansion quality
- Specialization coverage

---

## Expected Outcomes

### For Implementation Team

1. **Identified Bottlenecks:** Specific functions/operations causing slowdowns
2. **Optimization Opportunities:** Concrete code changes to improve performance
3. **Compilation Tuning:** Threshold adjustments for better warmup
4. **Type Stability Fixes:** Locations requiring type hint or code refactoring

### For Performance Validation

1. **Baseline Understanding:** Know what "good" performance looks like
2. **Regression Detection:** Ability to detect performance degradation
3. **Optimization Verification:** Confirm improvements work as expected

### For Future Development

1. **Best Practices:** Coding patterns that optimize well
2. **Anti-Patterns:** Coding patterns that prevent optimization
3. **Benchmarking Strategy:** How to measure performance reliably

---

## Success Criteria

### Hypothesis Validation
- [ ] Confirm or reject each of the 5 hypotheses
- [ ] Provide evidence (profiling data) for conclusions
- [ ] Identify unexpected findings

### Actionable Insights
- [ ] List 3-5 concrete optimization opportunities
- [ ] Prioritize by expected impact
- [ ] Provide implementation guidance

### Data Quality
- [ ] Reproducible results across runs
- [ ] Clear visualization of bottlenecks
- [ ] Quantified impact of each issue

---

## Next Steps

After completing the profiling phases:

1. **Analysis Report:** Synthesize findings into performance analysis document
2. **Implementation Plan:** Create prioritized optimization tasks
3. **Optimization Execution:** Implement highest-impact improvements
4. **Validation:** Re-run benchmarks to measure improvement
5. **Iteration:** Repeat for remaining bottlenecks

## Notes

- All profiling should use same benchmark parameters as baseline (size, iterations)
- Save all raw profiling output for later reference
- Document any unexpected behavior or anomalies
- Consider running profiling multiple times for consistency
