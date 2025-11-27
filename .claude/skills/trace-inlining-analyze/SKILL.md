---
name: Run and Analyze Inlining Tracer
description: Shows inlining decisions during compilation with call tree and reasons for inline/don't-inline. Use to verify critical calls are inlined, understand why inlining failed (too large, recursive, boundary, budget exhaustion), and optimize method sizes. Good inlining = better compilation and performance.
---

# Skill: Run and Analyze Inlining Tracer

This skill runs the Trace Inlining tool on your language implementation and provides detailed analysis of inlining decisions to help identify optimization opportunities and understand why functions do or don't inline.

## What This Skill Does

1. **Runs Inlining Tracer**: Executes the program with inlining tracing enabled
2. **Analyzes Inlining Decisions**: Interprets the trace output to identify:
   - Which calls are successfully inlined
   - Why calls fail to inline (budget exhaustion, size limits, recursion)
   - Budget consumption patterns
   - Call tree structure and optimization opportunities
3. **Provides Actionable Recommendations**: Suggests specific changes to improve inlining effectiveness

## Why Inlining Matters

**Inlining is critical for peak performance** because:
- **Eliminates call overhead**: 5-20 nanoseconds per call (branch misprediction, pipeline stalls)
- **Enables cross-function optimization**: constant propagation, dead code elimination, specialization
- **Typically 2-10x performance difference**: between well-inlined and poorly-inlined code

Poor inlining = slow code, even if compilation succeeds!

## When to Use This Skill

- Investigate why performance is poor despite successful compilation
- Verify critical hot paths are inlining their callees
- Debug compilation unit size issues
- Tune inlining budgets for optimal performance
- Understand why a specific function isn't inlining
- Validate that optimizations enable better inlining

## Prerequisites

Before running this skill, you should know:
- **Required**: Having benchmark baseline data for comparison
- The path to the program you want to analyze
- Whether you have specific functions to focus on (reduces output volume)
- Ideally, profiling results showing hot functions

## Fermi Verification (MANDATORY)

**Context:** Tools often fail due to environment issues, permissions, or misconfiguration. To avoid hallucinating results, misinterpreting output, or wasting resources, you must follow this 3-step verification loop.

**Mitigation Strategy**: Fermi Verification Before accepting the tool's output, you must:

1. Estimate: Look at the complexity of the code. Perform a "Fermi Calculation" to estimate the expected order of magnitude of allocations (e.g., "This acts on an array of 10k items, so I expect at least 10k allocations").
2. Probe: Run the tool on a trivial input (e.g., a minimal program) to ensure it produces output quickly and correctly.
3. Execute: Run the tool on the real target.
4. Compare: If the tool output deviates from your Fermi Estimate by more than one order of magnitude (or is zero):
   - **STOP - The tool is broken or misconfigured**
   - **Do NOT rationalize unexpected results** (e.g., "maybe optimizations eliminated everything")
   - Verify tool prerequisites are met before proceeding
   - Invalid tool output = invalid analysis

## How the Skill Works

The skill follows this workflow:

### 1. Initial Setup
- Confirms the program path and any arguments
- Optionally sets up function filtering with `--engine.CompileOnly`
- Determines if detailed tracing is needed

### 2. Run Inlining Tracer

⚠️ WARNING: Output Volume Tracing generates massive text. Always redirect to file and inspect slices of the data.

#### Basic Inlining Trace (Recommended)
```bash
<launcher> --experimental-options --engine.TraceInlining <program> [script args]
```
- Shows inlining decisions for all compilations
- Includes depth-first call tree traversal
- Shows inlined/expanded/cutoff states

#### Detailed Inlining Trace (Verbose)
```bash
<launcher> --experimental-options \
  --engine.TraceInlining \
  --engine.TraceInliningDetails \
  <program> [script args]
```
- Shows entire explored call tree (including rejected candidates)
- Much more output volume
- Use for deep analysis of specific compilations

#### Focused Trace (Recommended for Large Programs)
```bash
<launcher> --experimental-options \
  --engine.TraceInlining \
  --engine.CompileOnly=functionName \
  <program> [script args]
```
- Restricts compilation tracing to specific function
- Dramatically reduces output volume
- Ideal for investigating specific issues

#### Redirect Output to File
```bash
<launcher> --experimental-options \
  --engine.TraceInlining \
  <program> [script args] > inlining.txt 2>&1
```
- Enables offline analysis with text processing tools
- Compare across runs

### 3. Understand Call States

Every call in the trace shows one of six states:

#### ✅ **Inlined** - Success!
```
[engine] Inlined EdgeSeparation |call diff -3.00 |IR Nodes 4097 |...
```
- **Meaning**: Call successfully incorporated into parent compilation unit
- **Benefits**: No call overhead, enables cross-function optimization
- **Look for**: Negative call diff (eliminates calls) and reasonable IR node count

#### ⚠️ **Expanded** - Too Expensive
```
[engine] Expanded EdgeSeparation |call diff 1.00 |IR Nodes 8500 |...
```
- **Meaning**: Call was evaluated but deemed too expensive to inline
- **Reasons**: IR size excessive, would exceed inlining budget, or positive call diff
- **Action**: Consider refactoring if this is a hot function

#### ❌ **Cutoff** - Budget Exhausted
```
[engine] Cutoff FindMaxSeparation |IR Nodes 0 |...
```
- **Meaning**: Exploration budget exhausted before evaluating this call
- **Zero IR nodes**: Function wasn't even partially evaluated
- **Action**: If hot function, increase `--engine.InliningExpansionBudget`

#### ✨ **Removed** - Optimized Away (Best!)
```
[engine] Removed getterFunction |call diff -1.00 |IR Nodes 0 |...
```
- **Meaning**: Partial evaluation completely eliminated the call
- **Best outcome**: No overhead, no code size increase
- **Common for**: Simple getters, constant functions

#### 🔀 **Indirect** - Cannot Inline
```
[engine] Indirect callback |...
```
- **Meaning**: Indirect call (through variable/callback)
- **Cannot inline**: Without speculative optimization
- **Action**: Usually acceptable, but consider making direct if hot

#### 🚨 **BailedOut** - Compilation Failed
```
[engine] BailedOut problematicFunction |...
```
- **Meaning**: Partial evaluation failed for this target
- **Serious problem**: Function cannot be compiled at all
- **Action**: Investigate immediately with detailed compiler logging

### 4. Analyze Output Fields

Each trace line contains these key metrics:

```
[engine] Inlined FindMaxSeparation |call diff -8.99 |Recursion Depth 0 |IR Nodes 4617 |Frequency 1.00 |Truffle Callees 7 |Depth 1
```

**Field Interpretation**:

- **State**: `Inlined` - Successfully inlined
- **Function Name**: `FindMaxSeparation` - Target being inlined
- **call diff**: `-8.99` - Negative = good! Reduces net calls by ~9
  - Negative: Inlining eliminates more calls than it adds (excellent)
  - Zero: Neutral impact
  - Positive: Inlining adds calls (usually bad, but sometimes acceptable)
- **Recursion Depth**: `0` - Not recursive (higher = deeper recursion)
- **IR Nodes**: `4617` - Graal IR node count after partial evaluation
  - Default budget: 12,000 nodes
  - Approaching budget = risk of exhaustion
- **Frequency**: `1.00` - Executed once per parent (higher for loops)
- **Truffle Callees**: `7` - This function has 7 outgoing calls
  - Indicates potential for further inlining
- **Depth**: `1` - Inlining depth (0 = root, higher = nested)

### 5. Identify Common Patterns

#### Pattern 1: Budget Exhaustion (Critical Functions Hit Cutoff)
```
[engine] inline start bigFunction |IR Nodes 27149 |Truffle Callees 14 |...
[engine] Inlined helper1 |call diff -3.00 |IR Nodes 2100 |...
[engine] Inlined helper2 |call diff -2.50 |IR Nodes 3500 |...
[engine] Cutoff criticalHelper |IR Nodes 0 |...        ❌ Problem!
[engine] Cutoff anotherHelper |IR Nodes 0 |...
```

**Problem**: Critical functions reaching Cutoff state early

**Symptoms**:
- Important hot path functions showing "Cutoff" with 0 IR nodes
- Cutoff appears early in call tree traversal
- Exploration budget exhausted too soon

**Root Cause**:
- Default exploration budget (12,000 nodes) exhausted
- Too many potential callees consuming budget
- Complex compilation units

**Resolution**:
```bash
# Increase exploration budget by 50%
<launcher> --experimental-options \
  --engine.TraceInlining \
  --engine.InliningExpansionBudget=18000 \
  <program> [script args]
```
- Increase conservatively (25-50% increments)
- Use profiling to confirm cutoff functions are actually hot
- Verify improvement with benchmarks

**Alternative**: Refactor code structure
- Split large functions into smaller pieces
- Reduce call fan-out
- Mark cold paths with `@TruffleBoundary`

#### Pattern 2: Large Functions Blocking Inlining
```
[engine] Expanded hugeFunction |call diff 1.50 |IR Nodes 15000 |...  ❌ Too large!
```

**Problem**: Functions marked "Expanded" with high IR node counts

**Symptoms**:
- Hot path functions marked "Expanded"
- IR node counts approaching or exceeding 12,000
- Often positive call diff values

**Root Cause**:
- Function's compiled size exceeds available inlining budget
- Function contains many calls that wouldn't inline
- High specialization diversity generates many combinations

**Resolution**:
1. **Refactor into smaller functions**:
   ```<your-language>
   // Before: One large function
   fun processData(data) {
     // 100 lines of code
   }

   // After: Split into focused pieces
   fun processData(data) {
     var validated = validateData(data);
     var transformed = transformData(validated);
     return formatOutput(transformed);
   }
   ```

2. **Isolate cold paths**: Mark error handling and rare features
3. **Review specialization**: Sometimes fewer, more general specializations help
4. **Consider accepting Expanded**: Not every function should inline

#### Pattern 3: Compilation Bailout (Critical!)
```
[engine] BailedOut problematicFunction |...  🚨 Critical issue!
```

**Problem**: Partial evaluation failures (rare but serious)

**Symptoms**: BailedOut state for any function

**Root Cause**:
- Unbounded loops without compilation-final loop counts
- Recursive structures without termination guarantees
- Excessive complexity exceeding compiler limits
- Implementation patterns incompatible with partial evaluation

**Resolution**:
```bash
# Enable detailed compiler logging
<launcher> --experimental-options \
  --engine.TraceInlining \
  --vm.Djdk.graal.Log=:3 \
  --vm.Djdk.graal.MethodFilter=problematicFunction \
  <program> [script args]
```
- Investigate immediately with detailed logging
- Use IGV to examine where partial evaluation fails
- Mark problematic loops with boundaries
- Restructure recursion to enable inlining limits

#### Pattern 4: Good Inlining (Target Pattern!)
```
[engine] inline start optimizedFunction |IR Nodes 8500 |Truffle Callees 5 |...
[engine] Inlined helper1 |call diff -5.00 |IR Nodes 1200 |...  ✅ Great!
[engine] Inlined helper2 |call diff -3.00 |IR Nodes 2100 |...  ✅ Great!
[engine] Removed getter |call diff -1.00 |IR Nodes 0 |...     ✅ Perfect!
[engine] Inlined helper3 |call diff -2.50 |IR Nodes 1800 |...  ✅ Great!
[engine] inline done optimizedFunction |IR Nodes 8500 |...
```

**Characteristics of good inlining**:
- ✅ Most important calls show "Inlined" or "Removed"
- ✅ Negative call diff values (eliminating calls)
- ✅ Reasonable IR node counts (well under 12,000 budget)
- ✅ No Cutoff for hot paths
- ✅ Some Removed calls (optimized away completely)

### 6. Analyze Root Compilation Summary

The root function shows overall metrics:

```
[engine] inline start CollidePolygons |Explore/inline ratio 1.07 |IR Nodes 27149 |Truffle Callees 14 |Depth 0
...
[engine] inline done CollidePolygons |Explore/inline ratio 1.07 |IR Nodes 27149 |...
```

**Key metrics**:
- **Explore/inline ratio**: 1.07 means exploration budget consumed 7% more than inlining budget
  - Close to 1.0 = balanced budget usage (good)
  - Much higher = exploration dominated (may need more inlining budget)
  - Much lower = inlining dominated (may need more exploration budget)
- **IR Nodes**: Total compilation unit size (27,149 nodes)
  - Default budget: 12,000 nodes per function
  - This is cumulative after inlining
- **Truffle Callees**: Number of potential calls to analyze

### 7. Budget Tuning

The skill can help tune two critical budgets:

#### Exploration Budget (Default: 12,000)
```bash
--engine.InliningExpansionBudget=<N>
```
- Controls how much partial evaluation to perform
- Exhaustion → Cutoff states
- Increase if critical functions hit Cutoff
- IF State is Cutoff (nodes = 0) AND Function is Hot → Action: Increase InliningExpansionBudget.

#### Inlining Budget (Default: 12,000)
```bash
--engine.InliningInliningBudget=<N>
```
- Controls total compilation unit size after inlining
- Exhaustion → Expanded states
- Increase if hot functions marked Expanded
- IF State is Expanded (nodes > 0) AND Function is Hot → Action: Increase InliningInliningBudget OR Refactor to reduce size.

**Best practices for tuning**:
- ✅ Increase conservatively (25-50% increments)
- ✅ Verify hot functions with profiling first
- ✅ Measure actual performance impact with benchmarks
- ❌ Don't increase arbitrarily (causes compilation time explosion)
- ❌ Don't tune without profiling data

### 8. Recursion Handling

```bash
--engine.InliningRecursionDepth=<N>  # Default: 2
```

Recursive functions show Recursion Depth > 0:
```
[engine] Inlined factorial |Recursion Depth 1 |IR Nodes 500 |...
[engine] Inlined factorial |Recursion Depth 2 |IR Nodes 500 |...
```

**Resolution**:
- Increase recursion depth cautiously: `--engine.InliningRecursionDepth=4`
- Risk: Code explosion with deeper unrolling
- Consider refactoring recursive → iterative (loops optimize better)

## Correlation with Other Tools

### Trace Inlining + Trace Compilation
```bash
<launcher> --experimental-options \
  --engine.TraceInlining \
  --engine.TraceCompilation \
  <program> [script args]
```
**Why combine**:
- TraceCompilation: Overall compilation success, timing, IR size
- TraceInlining: Detailed inlining decisions that produced that result
- Correlate "Inlined X Y" counts in compilation with inlining states

### Trace Inlining + Performance Warnings
```bash
<launcher> --experimental-options \
  --engine.TraceInlining \
  --engine.TracePerformanceWarnings=call \
  <program> [script args]
```
**Why combine**:
- Performance warnings: Flags virtual calls remaining in compiled code
- TraceInlining: Shows why calls didn't inline (budget, size, indirect)
- Essential combo for achieving peak performance

### Trace Inlining + CPU Sampler
```bash
# Step 1: Profile to find hot functions
<launcher> --cpusampler --cpusampler.Delay=2000 \
  --cpusampler.ShowTiers=true \
  <program> [script args] > profile.txt

# Step 2: Analyze inlining for hot functions
<launcher> --experimental-options \
  --engine.TraceInlining \
  --engine.CompileOnly=hotFunction \
  <program> [script args] > inlining.txt
```
**Why combine**:
- CPU Sampler: Identifies hot functions consuming time
- TraceInlining: Shows if hot functions inline properly
- Focus optimization on actual bottlenecks

### Trace Inlining + IGV (Visual Analysis)
```bash
<launcher> --experimental-options \
  --engine.TraceInlining \
  --vm.Djdk.graal.Dump=Truffle:1 \
  --vm.Djdk.graal.PrintGraph=Network \
  <program> [script args]
```
**Why combine**:
- TraceInlining: Text-based decision log
- IGV: Visual call tree and IR graphs
- Visual graphs reveal patterns hard to spot in text

## Advanced Options

### Disable Inlining (Diagnostic)
```bash
<launcher> --experimental-options --engine.Inlining=false <program> [script args]
```
- Measures inlining's performance impact
- Compare with/without inlining
- Typically 2-10x slower without inlining

### Filter to Specific Function
```bash
<launcher> --experimental-options \
  --engine.TraceInlining \
  --engine.CompileOnly=specificFunction \
  <program> [script args]
```
- Drastically reduces output volume
- Focus on investigating specific issues

### Detailed Call Tree
```bash
<launcher> --experimental-options \
  --engine.TraceInlining \
  --engine.TraceInliningDetails \
  <program> [script args]
```
- Shows entire explored call tree (including rejected candidates)
- Much more output - use for deep analysis only

## Best Practices

The skill follows these analysis best practices:

1. **Start with Simple Programs**
   - Understand output format on small examples first
   - Then analyze complex production code

2. **Always Combine with TraceCompilation**
   - Provides context: when/what tier compiled
   - Helps interpret inlining decisions

3. **Focus on Hot Paths First**
   - Use profiling (`--cpusampler`) to identify hot functions
   - Don't try to optimize every function's inlining

4. **Use CompileOnly for Debugging**
   - Reduce output volume dramatically
   - Focus on specific problematic functions

5. **Redirect Output to Files**
   ```bash
   <launcher> --experimental-options \
     --engine.TraceInlining \
     <program> [script args] > inlining.txt 2>&1
   ```
   - Enable offline analysis with text processing tools
   - Compare across runs

6. **Monitor IR Node Counts**
   - Functions approaching 12,000 nodes risk budget exhaustion
   - Track relative to budget limits

7. **Check Call Diff Values**
   - Negative = eliminates calls (good)
   - Positive = adds calls (investigate if hot path)

8. **Increase Budgets Conservatively**
   - 25-50% increments only
   - Always measure actual performance impact
   - Validate with benchmarks

## Common Pitfalls to Avoid

The skill warns about these mistakes:

- ❌ **Misinterpreting Cutoff as failure**: Often normal for cold paths
  - Not every call should inline - compiler makes deliberate trade-offs

- ❌ **Increasing budgets without measurement**: Causes compilation time explosion
  - Always validate with actual performance benchmarks
  - Increase conservatively (25-50% increments)

- ❌ **Thinking positive call diff is always bad**: Can be acceptable trade-offs
  - If exposed calls subsequently inline or enable other optimizations

- ❌ **Not correlating with profiling**: Optimize for real performance, not theory
  - Use `--cpusampler` to identify actually hot functions

- ❌ **Forgetting multi-tier compilation**: Analyze both tier-1 and tier-2 traces
  - Functions compile multiple times at different optimization levels

- ❌ **Overwhelming output volume**: Use filters and CompileOnly
  - Redirect to files for analysis

## Typical Workflow

The skill typically follows this analysis workflow:

### Step 1: Profile to Find Hot Functions
```bash
<launcher> --cpusampler --cpusampler.Delay=2000 \
  --cpusampler.ShowTiers=true \
  <program> [script args]
```
**Identify**: Top 3-5 functions consuming most time

### Step 2: Trace Inlining for Hot Functions
```bash
<launcher> --experimental-options \
  --engine.TraceInlining \
  --engine.CompileOnly=hotFunction \
  <program> [script args] > inlining.txt
```
**Analyze**: Check if hot functions inline their callees

### Step 3: Identify Inlining Issues
**Look for**:
- Cutoff states on important calls
- Expanded states on hot functions
- BailedOut states (critical issues)
- High IR node counts approaching budget

### Step 4: Address Issues

**If Budget Exhaustion**:
```bash
<launcher> --experimental-options \
  --engine.TraceInlining \
  --engine.InliningExpansionBudget=18000 \
  <program> [script args]
```

**If Functions Too Large**: Refactor into smaller pieces

**If Bailouts**: Investigate with detailed logging

### Step 5: Verify Improvements
```bash
# Re-run profiling
<launcher> --cpusampler --cpusampler.Delay=2000 \
  --cpusampler.ShowTiers=true \
  <program> [script args]

# Compare inlining trace
<launcher> --experimental-options --engine.TraceInlining <program> [script args]
```
**Check**: Did performance improve? Are more calls inlined?

## Example Analysis

### Example Output
```
[engine] inline start sieveFunction |IR Nodes 8500 |Truffle Callees 3 |Depth 0
[engine] Inlined isPrime |call diff -5.00 |IR Nodes 2100 |Depth 1
[engine]   Inlined modulo |call diff -1.00 |IR Nodes 150 |Depth 2
[engine]   Removed getLimit |call diff -1.00 |IR Nodes 0 |Depth 2
[engine] Inlined nextCandidate |call diff -2.00 |IR Nodes 800 |Depth 1
[engine] Expanded hugeHelper |call diff 1.00 |IR Nodes 12500 |Depth 1
[engine] inline done sieveFunction |IR Nodes 8500 |...
```

### Skill Analysis

1. **Root function**: `sieveFunction` - 8,500 IR nodes (under budget ✅)

2. **Successful inlining**:
   - ✅ `isPrime` inlined, eliminates 5 calls
   - ✅ `modulo` nested inline (depth 2)
   - ✅ `getLimit` optimized away completely (Removed)
   - ✅ `nextCandidate` inlined, eliminates 2 calls

3. **Problem identified**:
   - ❌ `hugeHelper` marked Expanded (12,500 nodes - too large!)
   - Positive call diff (would add 1 call)

4. **Recommendations**:
   - If `hugeHelper` is on hot path: Refactor into smaller functions
   - If `hugeHelper` is cold path: Acceptable as-is (non-critical)
   - Use profiling to determine if this matters for performance

5. **Overall assessment**: Good inlining! Most calls successfully inlined with negative call diffs. Only issue is `hugeHelper`, which needs profiling to determine importance.

## Reference Documentation

For detailed information, see:
- `/Users/antonykamp/Projects/hpi-ma/byopl24-02/docs/commands/trace-inlining.md` - Complete Trace Inlining documentation
- Official GraalVM docs: https://www.graalvm.org/latest/graalvm-as-a-platform/language-implementation-framework/Inlining/
- Optimization guide: https://www.graalvm.org/latest/graalvm-as-a-platform/language-implementation-framework/Optimizing/

## Implementation Notes

This skill:
- Uses your language's launcher: `<launcher>`
- Requires `--experimental-options` flag
- Defaults to basic tracing (not detailed)
- Uses `--engine.CompileOnly` for focused analysis
- Provides line-number references for investigation
- Suggests budget tuning with conservative increments
- Combines with profiling for data-driven optimization
- Saves output to files for offline analysis
