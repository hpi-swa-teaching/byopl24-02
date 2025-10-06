# Performance Hypotheses and Verification Plans

## Baseline Benchmark Results Summary

| Benchmark | Avg Time (ms) | Speedup | Observations |
|-----------|---------------|---------|--------------|
| Sieve | 15.64 | 460x | Excellent warmup, stable performance |
| Permute | 11.94 | 384x | Excellent warmup, stable performance |
| List | 27.29 | 8x | Poor speedup compared to others |
| Queens | 33.38 | 5.27x | Stable but limited speedup |
| Towers | 78.97 | 17x | High variance (12-92ms), unstable after warmup |

## Performance Hypotheses

### Hypothesis 1: Towers benchmark experiences deoptimization cycles
**Severity: HIGH**

**Evidence:**
- Towers shows 17x speedup after warmup, but maintains high variance (12-92ms range)
- This suggests the JIT compiler is optimizing and then deoptimizing repeatedly
- Normal warmed-up code should show stable, low variance performance

**Root Cause Candidates:**
1. Type instability: The benchmark may use variables with changing types, causing speculative optimizations to fail
2. Polymorphic call sites: Method calls may dispatch to different implementations, preventing monomorphization
3. Transfer to interpreter triggers: Certain code paths may force deoptimization back to interpreter mode
4. Allocation pressure: Excessive object creation may trigger GC during critical sections

**Verification Plan:**
1. Run Towers with trace compilation to identify deoptimization events
   - Look for repeated compilation/deoptimization cycles
   - Identify which methods are being recompiled
   - Check for "TransferToInterpreter" events
2. Run Towers with CPU sampler showing tier information
   - Analyze time spent in T0 (interpreter), T1 (tier 1), and T2 (tier 2)
   - High T0 percentage indicates frequent deoptimization
3. Examine compilation statistics to see:
   - Number of compilations per method
   - Invalidation reasons
   - Bailout frequencies

### Hypothesis 2: List benchmark has poor compilation effectiveness
**Severity: HIGH**

**Evidence:**
- List shows only 8x speedup, significantly worse than Sieve (460x) and Permute (384x)
- This suggests the code is either:
  - Not getting compiled at all (remains interpreted)
  - Getting compiled but with poor optimization quality
  - Spending most time in operations that cannot be optimized well

**Root Cause Candidates:**
1. Array operations not optimizing well in the Lox implementation
2. Iterator-based loops (for-of/for-in) may have optimization barriers
3. Boundary checks or dynamic dispatch preventing vectorization
4. Poor specialization of array access patterns

**Verification Plan:**
1. Run List with CPU sampler showing tier information
   - Check if code reaches T2 (fully optimized tier)
   - Identify which functions remain in T0 or T1
2. Run List with trace compilation to see:
   - Which methods get compiled
   - Any compilation failures or bailouts
   - Optimization decisions for array operations
3. Compare compilation statistics between List and Sieve:
   - Number of compiled methods
   - Optimization levels achieved
   - Time spent in compilation

### Hypothesis 3: Queens benchmark has optimization limitations
**Severity: MEDIUM**

**Evidence:**
- Queens shows stable performance but only 5.27x speedup
- Better than List but far worse than Sieve/Permute
- Suggests a fundamental limitation in how the algorithm optimizes

**Root Cause Candidates:**
1. Complex control flow preventing aggressive inlining
2. Array access patterns that cannot be bounds-check eliminated
3. Recursive nature may hit inlining depth limits
4. Class-based implementation (if using objects) may prevent scalar replacement

**Verification Plan:**
1. Run Queens with compilation statistics to analyze:
   - Inlining decisions
   - Method compilation depth
   - Escape analysis results
2. Run Queens with CPU sampler to identify hot methods
   - Check tier distribution
   - Identify bottleneck functions
3. Compare with trace compilation to see optimization opportunities missed

### Hypothesis 4: Sieve and Permute demonstrate optimal performance characteristics
**Severity: LOW (Reference baseline)**

**Evidence:**
- Both show 300-400x speedup with stable variance
- This represents what the Lox implementation CAN achieve when everything works well
- These serve as the performance ceiling for comparison

**Root Cause (Why they succeed):**
1. Simple, predictable control flow
2. Monomorphic types throughout execution
3. Effective loop optimization and vectorization
4. Minimal allocation pressure

**Verification Plan:**
1. Profile with CPU sampler to confirm T2 dominance
2. Use as reference baseline for comparing other benchmarks
3. Document optimization patterns that succeed

## Recommended Profiling Strategy

### Phase 1: Identify Compilation Issues (Priority: HIGH)
Run trace compilation on problematic benchmarks to understand what the JIT compiler is doing:

1. **Towers** (trace compilation):
   - Goal: Identify deoptimization cycles
   - Expected insights: Repeated compilation events, specific deoptimization reasons
   - Command: `run_benchmark_with_trace_compilation` for Towers

2. **List** (trace compilation):
   - Goal: Understand why code isn't optimizing
   - Expected insights: Compilation failures, bailouts, optimization barriers
   - Command: `run_benchmark_with_trace_compilation` for List

3. **Queens** (trace compilation):
   - Goal: Identify missed optimization opportunities
   - Expected insights: Inlining decisions, loop optimization limits
   - Command: `run_benchmark_with_trace_compilation` for Queens

### Phase 2: Profile Execution Tiers (Priority: HIGH)
Run CPU sampler with tier information to understand execution distribution:

1. **Towers** (CPU sampler with tiers):
   - Goal: Quantify time in interpreter vs compiled code
   - Expected insights: High T0 percentage confirms deoptimization hypothesis
   - Command: `run_benchmark_with_cpu_sampler(tiers='0,1,2')` for Towers

2. **List** (CPU sampler with tiers):
   - Goal: Check if code reaches full optimization
   - Expected insights: T2 percentage shows compilation effectiveness
   - Command: `run_benchmark_with_cpu_sampler(tiers='0,1,2')` for List

3. **Queens** (CPU sampler with tiers):
   - Goal: Identify hot methods and their optimization state
   - Expected insights: Which functions are bottlenecks and their compilation tier
   - Command: `run_benchmark_with_cpu_sampler(tiers='0,1,2')` for Queens

### Phase 3: Detailed Compilation Metrics (Priority: MEDIUM)
Run compilation statistics for deeper insights:

1. **All problematic benchmarks** (Towers, List, Queens):
   - Goal: Quantitative metrics on compilation behavior
   - Expected insights: Number of compilations, invalidations, compilation time
   - Command: `run_benchmark_with_compilation_statistics` for each

### Phase 4: Flamegraph Analysis (Priority: LOW)
Generate flamegraphs for visual hotspot identification:

1. **List and Queens** only (if previous phases don't provide clear direction):
   - Goal: Visual representation of where time is spent
   - Expected insights: Call stack hot paths
   - Command: `run_benchmark_with_cpu_sampler(flamegraph=True)` for each

## Next Steps

After collecting the profiling data:
1. Update this document with findings from each profiling run
2. Validate or invalidate each hypothesis
3. Prioritize root causes based on impact
4. Recommend specific code changes to address identified issues
