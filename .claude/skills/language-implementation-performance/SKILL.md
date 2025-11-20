---
name: Analyze and Improve Language Implementation Performance
description: Comprehensive hierarchical workflow for diagnosing and optimizing Truffle language implementation performance. Covers profiling, compilation analysis, optimization barriers, and deep IR analysis. Guides through systematic performance investigation from high-level profiling to low-level compiler graphs.
---

# Skill: Analyze and Improve Language Implementation Performance

This is a comprehensive workflow guide for optimizing Truffle-based language implementations (like Lox). It provides a systematic approach to performance analysis, from initial profiling through deep compiler diagnostics.

## Overview

Performance optimization for Truffle languages requires a layered approach, using different tools at different stages. This skill guides you through the complete workflow.

### The Performance Optimization Hierarchy

```
1. IDENTIFY HOTSPOTS        → CPU Sampler, CPU Tracer
2. CHECK COMPILATION        → Trace Compilation
3. FIND BARRIERS (START!)   → Trace Performance Warnings
4. CHECK INLINING           → Trace Inlining
5. CHECK STABILITY          → Trace Transfer to Interpreter
6. CHECK ALLOCATIONS        → Memory Tracer
7. DEEP DIVE (LAST!)        → Compiler Graphs
```

### When to Use This Skill

Use this comprehensive workflow when:
- Starting performance optimization work on a Truffle language
- Investigating why compiled code is slow
- Building new language features requiring peak performance
- Debugging deoptimization or compilation issues
- Need systematic approach to find optimization barriers

## Quick Start: The Recommended Workflow

### Phase 1: Identify What's Hot (ALWAYS START HERE!)

**Goal**: Find which functions consume the most time

**Tools**:
- [CPU Sampler](./cpu-sampler/overview.md) - Time-based sampling
- [CPU Tracer](./cpu-tracer/overview.md) - Execution frequency counts

**Workflow**:
1. Run [CPU Sampler with delay](./cpu-sampler/usage.md#basic-profiling) to skip warmup
2. Identify top 3-5 functions by [self-time](./cpu-sampler/analysis.md#interpreting-output)
3. Check [tier distribution](./cpu-sampler/analysis.md#tier-information) (T0/T1/T2)
4. Optionally correlate with [CPU Tracer](./cpu-tracer/usage.md#function-level) for frequency

**Output**: List of hot functions to optimize

**Next**: If hot functions show high T0 (interpreter) time → Go to Phase 2

---

### Phase 2: Verify Compilation (IF T0 TIME IS HIGH)

**Goal**: Ensure hot code is being compiled

**Tools**:
- [Trace Compilation](./trace-compilation/overview.md)

**Workflow**:
1. Run [compilation trace](./trace-compilation/usage.md#basic-trace)
2. [Check for opt done events](./trace-compilation/analysis.md#compilation-success) for hot functions
3. [Verify tier progression](./trace-compilation/analysis.md#tier-progression) (T1 → T2)
4. [Look for deoptimization cycles](./trace-compilation/analysis.md#deoptimization-loop)

**Output**: Confirmation that hot code compiles or identification of compilation failures

**Next**: If compiling but still slow → Go to Phase 3

---

### Phase 3: Find Optimization Barriers (⭐ START HERE FOR OPTIMIZATION!)

**Goal**: Identify WHY optimization fails

**Tools**:
- [Trace Performance Warnings](./trace-performance-warnings/overview.md) ⭐ USE FIRST!

**Workflow**:
1. Enable [all warnings](./trace-performance-warnings/usage.md#all-warnings) for hot functions
2. [Count warnings per function](./trace-performance-warnings/analysis.md#counting-warnings)
3. [Identify warning types](./trace-performance-warnings/analysis.md#warning-types):
   - `call` → [Virtual calls](./trace-performance-warnings/patterns.md#virtual-calls) not inlining
   - `instanceof` → [Type checks](./trace-performance-warnings/patterns.md#type-checks) not resolving
   - `store` → [Frame slots](./trace-performance-warnings/patterns.md#store-locations) not constant
4. [Fix identified barriers](./trace-performance-warnings/resolutions.md)

**Output**: List of specific optimization barriers to fix

**Next**: After fixing warnings → Verify with Phase 4

---

### Phase 4: Verify Inlining (AFTER FIXING WARNINGS)

**Goal**: Confirm that fixes enabled successful inlining

**Tools**:
- [Trace Inlining](./trace-inlining/overview.md)

**Workflow**:
1. Run [inlining trace](./trace-inlining/usage.md#basic-trace) for hot functions
2. [Check call states](./trace-inlining/analysis.md#call-states):
   - ✅ Inlined / Removed (good)
   - ❌ Cutoff / Expanded (problems)
3. [Analyze call diff values](./trace-inlining/analysis.md#call-diff) (negative = good)
4. [Check IR node counts](./trace-inlining/analysis.md#ir-nodes) vs budget

**Output**: Confirmation of successful inlining or budget issues

**Next**: If seeing deoptimizations → Go to Phase 5

---

### Phase 5: Check Compilation Stability (IF DEOPTS OCCUR)

**Goal**: Eliminate deoptimization cycles

**Tools**:
- [Trace Transfer to Interpreter](./trace-transfer-to-interpreter/overview.md)

**Workflow**:
1. Run [transfer trace](./trace-transfer-to-interpreter/usage.md#basic-trace) with compilation trace
2. [Count transfers per location](./trace-transfer-to-interpreter/analysis.md#counting-transfers)
3. [Identify deoptimization loops](./trace-transfer-to-interpreter/patterns.md#deoptimization-loop) (critical!)
4. [Fix type stability issues](./trace-transfer-to-interpreter/resolutions.md#type-stability)

**Output**: Zero steady-state transfers (goal)

**Next**: If seeing GC overhead → Go to Phase 6

---

### Phase 6: Check Allocation Pressure (IF GC OVERHEAD)

**Goal**: Identify unnecessary allocations

**Tools**:
- [Memory Tracer](./memory-tracer/overview.md)

**Workflow**:
1. [Correlate with CPU Sampler](./memory-tracer/workflow.md#correlation) (high CPU + high allocations = critical)
2. Run [memory trace](./memory-tracer/usage.md#basic-trace)
3. [Identify allocation hotspots](./memory-tracer/analysis.md#hotspots) (single function >80%)
4. [Check type distribution](./memory-tracer/usage.md#type-histogram) for boxing issues
5. [Reduce allocations](./memory-tracer/resolutions.md) in hot paths

**Output**: Reduced allocation counts in hot functions

**Next**: If still unclear why slow → Go to Phase 7

---

### Phase 7: Deep IR Analysis (LAST RESORT - COMPLEX!)

**Goal**: Understand exact compiler optimizations at IR level

**Tools**:
- [Compiler Graphs](./compiler-graphs/overview.md)

**Workflow**:
1. [Dump graphs](./compiler-graphs/usage.md#basic-dump) for specific hot function only (use MethodFilter!)
2. [Convert to JSON](./compiler-graphs/usage.md#conversion) with bgv2json
3. [Check "After TruffleTier" phase](./compiler-graphs/analysis.md#key-phases)
4. [Find IR issues](./compiler-graphs/patterns.md):
   - [Indirect calls](./compiler-graphs/patterns.md#indirect-calls)
   - [Failed escape analysis](./compiler-graphs/patterns.md#escape-analysis)
   - [Boxing operations](./compiler-graphs/patterns.md#boxing)
5. [Fix at language implementation level](./compiler-graphs/resolutions.md)

**Output**: Understanding of exact IR-level optimization failures

**Next**: Implement fixes and return to Phase 1 to verify

---

## Complete Workflow Example

### Scenario: Slow Recursive Fibonacci

#### Step 1: Profile
```bash
./lox --cpusampler --cpusampler.ShowTiers=true --cpusampler.Delay=2000 fib.lox
```
**Result**: `fib` function shows 95% self-time, 30% T0 (interpreter)

**Analysis**: Hot function with high interpreter time → compilation issue

---

#### Step 2: Check Compilation
```bash
./lox --experimental-options --engine.TraceCompilation fib.lox
```
**Result**: See "opt done" for `fib` at T1 and T2

**Analysis**: Function compiles, but performance still poor → optimization barriers

---

#### Step 3: Find Barriers ⭐
```bash
./lox --compiler.TracePerformanceWarnings=all fib.lox
```
**Result**:
```
[engine] perf warn fib |Partial evaluation could not inline the virtual runtime call Virtual to OptimizedCallNode
```

**Analysis**: Virtual call preventing inlining → need to cache CallTarget

---

#### Step 4: Verify Inlining
```bash
./lox --experimental-options --engine.TraceInlining --engine.CompileOnly=fib fib.lox
```
**Result**: Shows "Cutoff" for recursive calls

**Analysis**: Recursion depth limit or budget exhaustion

---

#### Step 5: Fix Implementation
```java
// Before: Dynamic call
public Object execute(VirtualFrame frame) {
    return callTarget.call(args);  // Not cached!
}

// After: Cached call
@Specialization(guards = "function == cachedFunction")
public Object executeCached(
    @Cached("function") Function cachedFunction,
    @Cached("cachedFunction.getCallTarget()") CallTarget callTarget) {
    return callTarget.call(args);
}
```

---

#### Step 6: Verify Fix
```bash
./lox --cpusampler --cpusampler.ShowTiers=true --cpusampler.Delay=2000 fib.lox
```
**Result**: `fib` now shows 98% T2 (fully optimized), 10x faster

**Success**: Optimization complete!

---

## Tool Selection Guide

### "I need to find what's slow"
→ Start with [CPU Sampler](./cpu-sampler/overview.md)
- Shows WHERE time is spent
- Easy to understand output
- Low overhead

### "I know what's slow, need to know WHY it won't optimize"
→ Use [Trace Performance Warnings](./trace-performance-warnings/overview.md) ⭐
- Most targeted diagnostic
- Shows exact optimization barriers
- Actionable fixes

### "Code compiles but stays in interpreter mode"
→ Use [Trace Transfer to Interpreter](./trace-transfer-to-interpreter/overview.md)
- Shows deoptimization events
- Identifies type instability
- Critical for stable compilation

### "Functions aren't inlining"
→ Use [Trace Inlining](./trace-inlining/overview.md)
- Shows inlining decisions
- Explains why inlining fails
- Helps tune budgets

### "Too much GC overhead"
→ Use [Memory Tracer](./memory-tracer/overview.md)
- Shows allocation patterns
- Identifies allocation hotspots
- Correlate with CPU profiling

### "Need to see exact compiler optimizations"
→ Use [Compiler Graphs](./compiler-graphs/overview.md) (LAST!)
- Most complex tool
- Requires compiler expertise
- Use only when other tools insufficient

---

## Common Performance Patterns

### Pattern 1: High T0 Time Despite Compilation
**Symptoms**: CPU Sampler shows >20% T0 time, TraceCompilation shows "opt done"

**Diagnosis Workflow**:
1. [Check for deoptimizations](./trace-transfer-to-interpreter/workflow.md) (likely cause)
2. If deopts found → [Fix type stability](./trace-transfer-to-interpreter/resolutions.md#type-stability)
3. If no deopts → [Check compilation actually succeeded](./trace-compilation/analysis.md#verification)

**See**: [trace-transfer-to-interpreter/patterns.md#deoptimization-loop](./trace-transfer-to-interpreter/patterns.md#deoptimization-loop)

---

### Pattern 2: Functions Compile But Stay Slow
**Symptoms**: TraceCompilation shows success, performance still poor

**Diagnosis Workflow**:
1. [Check performance warnings](./trace-performance-warnings/workflow.md) (start here!)
2. Common issues:
   - [Virtual calls not inlining](./trace-performance-warnings/patterns.md#virtual-calls)
   - [Type checks not resolving](./trace-performance-warnings/patterns.md#type-checks)
   - [Non-constant frame slots](./trace-performance-warnings/patterns.md#store-locations)
3. After fixes → [Verify with TraceInlining](./trace-inlining/workflow.md)

**See**: [trace-performance-warnings/patterns.md](./trace-performance-warnings/patterns.md)

---

### Pattern 3: Excessive GC Overhead
**Symptoms**: CPU Sampler shows high time in GC, program slow

**Diagnosis Workflow**:
1. [Profile allocations](./memory-tracer/workflow.md#basic-workflow)
2. [Correlate with CPU time](./memory-tracer/workflow.md#correlation)
3. Focus on functions with high CPU + high allocations
4. [Check escape analysis with graphs](./compiler-graphs/patterns.md#escape-analysis) (advanced)

**See**: [memory-tracer/patterns.md#allocation-hotspot](./memory-tracer/patterns.md#allocation-hotspot)

---

### Pattern 4: Deoptimization Loops
**Symptoms**: Code repeatedly compiles, deoptimizes, recompiles

**Diagnosis Workflow**:
1. [Trace transfers](./trace-transfer-to-interpreter/usage.md#with-compilation)
2. [Count transfers per location](./trace-transfer-to-interpreter/analysis.md#counting-transfers)
3. High counts (10+) at same location = catastrophic
4. [Fix type mixing](./trace-transfer-to-interpreter/resolutions.md#type-stability)

**See**: [trace-transfer-to-interpreter/patterns.md#deoptimization-loop](./trace-transfer-to-interpreter/patterns.md#deoptimization-loop)

---

## Best Practices

### 1. Always Start with Profiling
❌ Don't jump to deep diagnostics
✅ Profile first, then investigate specific hot functions

### 2. Use Performance Warnings Early
❌ Don't start with compiler graphs
✅ Performance warnings identify barriers faster

### 3. Focus on Hot Paths Only
❌ Don't optimize cold code
✅ Use profiling to focus on top 3-5 functions

### 4. Use Filters Aggressively
❌ Don't dump everything
✅ Use `--engine.CompileOnly=hotFunction` and MethodFilter

### 5. Verify After Each Fix
❌ Don't make multiple changes without measuring
✅ Profile → Fix → Profile → Verify improvement

### 6. Correlate Tools
❌ Don't use tools in isolation
✅ Combine CPU Sampler + Warnings + Inlining for complete picture

---

## Troubleshooting Guide

### Issue: "I don't know where to start"
**Solution**: Always start with [CPU Sampler](./cpu-sampler/workflow.md#getting-started)

### Issue: "Too much output, can't find the problem"
**Solution**: Use filters:
- `--engine.CompileOnly=functionName`
- `-Djdk.graal.MethodFilter=*functionName*`

### Issue: "Fixed warnings but still slow"
**Solution**:
1. [Verify compilation succeeded](./trace-compilation/workflow.md#verification)
2. [Check for deoptimizations](./trace-transfer-to-interpreter/workflow.md)
3. [Profile again](./cpu-sampler/workflow.md) to find remaining issues

### Issue: "Graph dumps are gigabytes"
**Solution**: [Use MethodFilter aggressively](./compiler-graphs/usage.md#focused-dump)

### Issue: "Can't understand compiler graphs"
**Solution**:
1. Start with [simpler tools](./trace-performance-warnings/overview.md) first
2. Check [key patterns](./compiler-graphs/patterns.md) documentation
3. Use [automated queries](./compiler-graphs/analysis.md#jq-queries)

---

## Tool Reference Quick Links

### Profiling Tools
- [CPU Sampler Overview](./cpu-sampler/overview.md) - Time-based sampling
- [CPU Tracer Overview](./cpu-tracer/overview.md) - Execution frequency counts

### Compilation Tools
- [Trace Compilation Overview](./trace-compilation/overview.md) - Compilation lifecycle
- [Trace Inlining Overview](./trace-inlining/overview.md) - Inlining decisions
- [Trace Transfer to Interpreter Overview](./trace-transfer-to-interpreter/overview.md) - Deoptimizations

### Optimization Tools
- [Trace Performance Warnings Overview](./trace-performance-warnings/overview.md) ⭐ - Optimization barriers
- [Memory Tracer Overview](./memory-tracer/overview.md) - Allocation patterns

### Deep Diagnostics
- [Compiler Graphs Overview](./compiler-graphs/overview.md) - IR analysis

---

## Success Criteria

After optimization, you should see:
- ✅ Hot functions show >95% T2 time (CPU Sampler)
- ✅ Zero performance warnings in hot paths
- ✅ Successful inlining of critical calls (TraceInlining)
- ✅ Zero steady-state deoptimizations (TraceTransferToInterpreter)
- ✅ Low allocation counts in hot loops (Memory Tracer)
- ✅ Clean compiler graphs with direct calls and eliminated allocations

---

## Additional Resources

- Main documentation: `/Users/antonykamp/Projects/hpi-ma/byopl24-02/docs/commands/`
- GraalVM optimization guide: https://www.graalvm.org/latest/graalvm-as-a-platform/language-implementation-framework/Optimizing/
- Truffle documentation: https://github.com/oracle/graal/tree/master/truffle/docs

---

## Notes

This hierarchical skill provides:
- Systematic workflow from profiling to deep analysis
- Clear decision points for tool selection
- Concrete examples and patterns
- Links to detailed documentation for each tool

Each tool's subfolder contains:
- `overview.md` - What the tool does, when to use it
- `usage.md` - Command syntax and options
- `analysis.md` - How to interpret output
- `patterns.md` - Common problem patterns
- `resolutions.md` - How to fix identified issues
- `workflow.md` - Step-by-step usage guide
