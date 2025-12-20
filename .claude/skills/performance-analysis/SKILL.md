---
name: performance-analysis
description: Comprehensive performance analysis for Truffle language implementations. Loads benchmarks and baseline data, builds performance theories, verifies them using appropriate tools (with mandatory documentation file loading, Fermi verification, and smoke tests), and generates a detailed analysis report with recommendations. You MUST load this skill when you want to perform language performance analysis. YOU MUST run the benchmark baseline skill first.
---

# Skill: Comprehensive Performance Analysis

This skill performs systematic performance analysis of your Truffle language implementation by loading existing benchmark results and baseline data, automatically generating performance theories based on gaps and patterns, systematically verifying each theory with appropriate tools, and producing a comprehensive report with actionable recommendations.

## What This Skill Does

1. **Loads Benchmark Data**: Reads existing benchmark execution results
2. **Loads Baseline Data**: Reads BENCHMARK_BASELINE.md (from benchmark-baseline skill)
3. **Analyzes Performance**: Compares actual vs expected performance, identifies gaps
4. **Generates Theories**: Automatically builds testable performance theories based on:
   - Performance gaps vs baseline
   - Known anti-patterns
   - Code analysis for implementation flaws
   - Execution characteristics
5. **Verifies Theories**: For each theory systematically:
   - Selects appropriate diagnostic tool
   - **Loads tool documentation file** (MANDATORY - from skill directory)
   - Performs Fermi verification
   - Runs smoke test
   - Executes tool and analyzes results
   - Marks theory as verified or falsified
6. **Produces Report**: Generates `PERFORMANCE_ANALYSIS_REPORT.md` containing:
   - All theories with verdicts (verified/falsified)
   - Performance metrics and comparisons
   - Actionable fix recommendations
   - Tool outputs and evidence

## When to Use This Skill

Use this skill after running benchmarks when you need comprehensive analysis:

- **After benchmark-baseline skill** to understand why performance differs from expectations
- **Debugging slow performance** with no clear cause
- **Before optimization work** to identify bottlenecks systematically
- **After implementing features** to understand performance impact
- **Preparing for optimization** with data-driven insights

**Important**: This skill assumes benchmarks have been executed and baseline exists. Run benchmarks first, then use this skill for analysis.

## Prerequisites

Before running this skill:

- **Required**: Benchmarks have been executed (timing data available)
- **Required**: `BENCHMARK_BASELINE.md` exists (from benchmark-baseline skill or manual creation)
- **Recommended**: Tools installed for deep analysis:
  - `bgv2json` or `seafoam` for compiler graph analysis
  - Standard GraalVM tools (included with GraalVM)
- **Required**: Tool documentation files in `.claude/skills/performance-analysis/` directory

## Theory Generation Process

The skill automatically generates performance theories based on:

### 1. Performance vs Baseline
- **Much slower than comparable languages** (>2x difference)
  - Theory: Compilation effectiveness issues
  - Theory: Missing optimizations
  - Theory: Algorithm inefficiencies

- **Slower than expected range** (within comparable but below best case)
  - Theory: Partial optimization success
  - Theory: Some barriers remaining

### 2. Known Anti-Patterns
- **Low compiled percentage** (<95% compiled time) if profiling data available
  - Theory: Deoptimization loops
  - Theory: Compilation barriers
  - Theory: Type instability

- **Many indirect calls** if profiling data available
  - Theory: Missing CallTarget caching
  - Theory: Polymorphic call sites

- **High allocation rates** if memory data available
  - Theory: Failed escape analysis
  - Theory: Excessive object creation

- **Poor inlining** if traces available
  - Theory: Budget exhaustion
  - Theory: Functions too large
  - Theory: Recursive patterns

### 3. Benchmark-Specific Patterns
- **Recursive algorithms slow** (queens, towers)
  - Theory: Inlining issues with recursion
  - Theory: CallTarget caching missing

- **Allocation-heavy benchmarks slow** (storage, trees)
  - Theory: Escape analysis failures
  - Theory: GC pressure

- **Arithmetic-heavy benchmarks slow** (sieve, permute)
  - Theory: Missing primitive specializations
  - Theory: Boxing overhead

### 4. Code Analysis for Common Implementation Flaws

Examines implementation code for common anti-patterns:

- **Missing @Cached for CallTarget/Profiles**
  - Search node classes for uncached call targets or profile objects
  - Theory: Virtual calls preventing optimization

- **Missing primitive specializations**
  - Check operator nodes for Object-only specializations
  - Theory: Boxing overhead in arithmetic

- **Dynamic frame slot lookups**
  - Search for runtime FrameSlot resolution by name
  - Theory: Non-constant store locations

- **Missing boxing elimination config** (Bytecode DSL)
  - Check @GenerateBytecode for boxingEliminationTypes
  - Theory: Unnecessary boxing of primitives

**Process**: Use Glob/Grep to find node files, scan for patterns, generate theories with specific file locations for verification.

## Tool Selection Guide

The skill automatically selects tools based on theory type. Here's when each tool is used:

### For Compilation Issues
- **trace-performance-warnings** ← First tool to use
  - When: Need to identify WHY compilation/optimization fails
  - Detects: Virtual calls, type checks, non-constant stores
  - Output: Exact source locations of optimization barriers
  - Use when: Theory suggests optimization barriers

- **trace-compilation** ← Understand compilation lifecycle
  - When: Need to see WHEN and IF compilation happens
  - Detects: Bailouts, invalidations, recompilation cycles
  - Output: Compilation events with timing and success/failure
  - Use when: Theory suggests compilation not happening

### For Call and Inlining Issues
- **trace-inlining** ← Verify inlining decisions
  - When: Calls aren't inlining or budget issues suspected
  - Detects: Cutoff states, expanded functions, budget exhaustion
  - Output: Call tree with inline/don't-inline reasons
  - Use when: Theory suggests inlining problems

### For Execution Patterns
- **cpu-sampler** ← Identify where time is spent
  - When: Need to find hot functions (use FIRST for profiling)
  - Detects: Time-consuming functions, tier distribution
  - Output: Histogram with self/total time per function
  - Use when: Need initial profiling to identify hot code

- **cpu-tracer** ← Count execution frequencies
  - When: Need to verify compilation effectiveness or understand control flow
  - Detects: Low compiled %, execution hotspots, algorithmic complexity
  - Output: Execution counts with interpreted/compiled split
  - Use when: Theory suggests low compilation % or frequency issues

### For Memory Issues
- **memory-tracer** ← Find allocation hotspots
  - When: High memory usage or allocation rates suspected
  - Detects: Allocation sites, object types, memory pressure
  - Output: Allocation histogram by location and type
  - Use when: Theory suggests memory/allocation issues

### For Deoptimization
- **trace-transfer-to-interpreter** ← Detect deoptimization
  - When: Suspect unstable compilation or deoptimization loops
  - Detects: Deoptimization events, unstable assumptions
  - Output: Transfer events with reasons and locations
  - Use when: Theory suggests type instability or deopt loops

### For Deep Investigation
- **analyze-compiler-graph skill** ← Understand optimization decisions (Complete skill with seafoam/bgv2json/jq)
  - When: Other tools show problems but not root cause
  - Detects: Failed escape analysis, boxing, indirect calls in IR
  - Output: Compiler IR graphs with automated analysis via seafoam and jq queries
  - Workflow: Dumps BGV files → Analyzes with seafoam → Converts to JSON → Queries with jq
  - Note: Use LAST after other tools (most complex but most detailed)
  - Use when: Need to see WHAT the compiler actually did in the IR

## How the Skill Works

The skill follows a systematic 4-phase workflow:

### Phase 1: Load Benchmark and Baseline Data

**Objective**: Gather all existing performance data and expectations

**Process**:

1. **Load benchmark results**
   - Read `BENCHMARK_BASELINE.md` for benchmark list
   - Request user to provide timing data or locate result files
   - Parse execution times for each benchmark
   - Identify successful vs failed benchmarks

2. **Load baseline comparison data**
   - Read `BENCHMARK_BASELINE.md`:
     - Language characteristics
     - Comparable languages
     - Expected performance ranges per benchmark
     - Reference implementation times
   - Extract performance expectations

3. **Initial comparison**
   - Calculate performance ratios (actual / expected)
   - Identify significantly slower benchmarks (>2x slower than best case)
   - Identify unexpectedly slow benchmarks (>expected range)
   - Identify unexpectedly fast benchmarks (<expected range)

**Output**:
- Benchmark execution times loaded
- Baseline expectations loaded
- Performance gap analysis
- List of problem benchmarks

**Example**:
```
Loaded Results:
- queens: 2.1s (actual)
- Expected range: 0.8s (Lua best) to 3.5s (Python worst)
- Ratio: 2.6x slower than best case
- Status: ⚠️ INVESTIGATE (slower than expected)

- bounce: 1.2s (actual)
- Expected range: 0.5s to 2.0s
- Status: ✅ OK (within expected range)
```

### Phase 2: Generate Performance Theories

**Objective**: Generate testable performance theories based on gaps and patterns

**Process**:

1. **Analyze performance gaps**
   - For each benchmark significantly slower than baseline:
     - Identify gap magnitude (2x, 3x, 10x slower)
     - Consider benchmark type (recursive, allocation-heavy, arithmetic)
     - Generate specific theories

2. **Apply pattern matching**
   - **Recursive benchmarks** (queens, towers) → Inlining/caching theories
   - **Allocation benchmarks** (storage, trees) → Escape analysis theories
   - **Arithmetic benchmarks** (sieve, permute) → Specialization theories
   - **All slow benchmarks** → Compilation effectiveness theories

3. **Analyze implementation code for common flaws**
   - Search node implementations for anti-patterns:
     - Missing @Cached annotations (CallTarget, profiles)
     - Missing primitive specializations (Object-only)
     - Dynamic frame slot lookups
     - Missing boxing elimination config
   - Generate theories with specific file locations
   - Example: "CallTarget not cached in CallNode.java:42"

4. **Prioritize theories**
   - **Priority 1**: High-impact, quick-to-verify (e.g., cpu-sampler)
   - **Priority 2**: Medium complexity (e.g., trace-performance-warnings)
   - **Priority 3**: Deep investigation (e.g., compiler graphs)
   - Code-based theories often P2 (specific locations known)

5. **Select verification tools**
   - Match theory type to appropriate tool
   - Consider verification workflow:
     - Quick profiling first (cpu-sampler)
     - Specific diagnostics next (trace-performance-warnings)
     - Deep analysis last (compiler graphs)

**Generated Theory Types**:

**Compilation Theories**:
- "Functions are not being compiled (stuck in interpreter)"
  - Tool: cpu-tracer
  - Evidence if true: <95% compiled time for hot functions

- "Compilation has optimization barriers preventing peak performance"
  - Tool: trace-performance-warnings
  - Evidence if true: Virtual call warnings, non-constant stores

- "Compilation is failing (bailouts)"
  - Tool: trace-compilation
  - Evidence if true: Bailout messages in compilation log

**Call/Inlining Theories**:
- "CallTarget not cached (indirect calls preventing optimization)"
  - Tool: trace-performance-warnings
  - Evidence if true: OptimizedIndirectCallNode warnings

- "Inlining budget exhausted for hot functions"
  - Tool: trace-inlining
  - Evidence if true: Cutoff states on critical functions

- "Functions too large to inline"
  - Tool: trace-inlining
  - Evidence if true: Expanded states with high IR node counts

**Type/Specialization Theories**:
- "Type instability causing deoptimization"
  - Tool: trace-transfer-to-interpreter
  - Evidence if true: Repeated deoptimization events

- "Missing primitive specializations (boxing overhead)"
  - Tool: analyze-compiler-graph
  - Evidence if true: BoxNode/UnboxNode in IR

- "Polymorphic call sites preventing optimization"
  - Tool: trace-performance-warnings
  - Evidence if true: Unresolved instanceof warnings

**Memory Theories**:
- "Escape analysis failing (allocations remaining in compiled code)"
  - Tool: analyze-compiler-graph
  - Evidence if true: CommitAllocationNode after PartialEscape phase

- "Excessive object allocation in hot paths"
  - Tool: memory-tracer
  - Evidence if true: High allocation counts in benchmark functions

**Algorithm Theories**:
- "Algorithmic complexity worse than expected"
  - Tool: cpu-tracer (statement level)
  - Evidence if true: Disproportionate execution counts in nested loops

**Implementation Theories** (from code analysis):
- "CallTarget not cached in [specific node]"
  - Source: Identified from code inspection
  - Tool: trace-performance-warnings
  - Evidence if true: Virtual call warnings at specific location

- "Missing primitive specializations in [operator node]"
  - Source: Identified from code inspection
  - Tool: analyze-compiler-graph
  - Evidence if true: BoxNode/UnboxNode in IR for arithmetic

- "Frame slots looked up dynamically in [variable node]"
  - Source: Identified from code inspection
  - Tool: trace-performance-warnings
  - Evidence if true: Non-constant store location warnings

**Output**:
- Prioritized list of theories (3-8 theories typical)
- Selected verification tool for each
- Expected evidence description
- Theory rationale

**Example**:
```
Generated Theories for queens benchmark (2.1s, 2.6x slower than Lua):

Priority 1: "Hot functions have low compilation effectiveness"
- Tool: cpu-sampler
- Evidence if true: High T0 (interpreter) time percentage
- Rationale: Fundamental performance issue if code isn't compiling

Priority 2: "Optimization barriers prevent peak performance"
- Tool: trace-performance-warnings
- Evidence if true: Virtual call warnings, type check failures
- Rationale: Recursive algorithms need good inlining and caching

Priority 3: "Inlining budget exhausted for recursive calls"
- Tool: trace-inlining
- Evidence if true: Cutoff state for queens function
- Rationale: Deep recursion can exhaust inlining budget
```

### Phase 3: Verify Theories Systematically

**Objective**: Verify or falsify each theory using appropriate tools with rigorous methodology

For each theory in priority order, execute this sub-workflow:

#### Step 3.1: Load Tool Documentation (MANDATORY)

**Process**:
- **MUST** read the tool's documentation file before running the tool
- Documentation files located in `.claude/skills/performance-analysis/`
- Extract:
  - Tool command syntax and required options
  - Output format and field meanings
  - Interpretation guidelines (what's good vs bad)
  - Best practices for the tool
  - Expected evidence patterns for verification

**Tool Documentation Files**:
- `./CPUSampler.md` - For cpu-sampler
- `./CPUTracer.md` - For cpu-tracer
- `./Trace Performance Warnings.md` - For trace-performance-warnings
- `./Trace Inlining.md` - For trace-inlining
- `./Trace Compilation.md` - For trace-compilation
- `./Trace Transfer To Interpreter.md` - For trace-transfer-to-interpreter
- `./MemoryTracer.md` - For memory-tracer
- `./Dump Compiler Graph.md` - For compiler graph analysis

**Example**:
```
Theory: "Hot functions have low compilation effectiveness"
Tool: cpu-sampler

Loading documentation:
→ Reading .claude/skills/performance-analysis/CPUSampler.md

Retrieved Information:
- Command: <launcher> --cpusampler --cpusampler.ShowTiers=true --cpusampler.Delay=2000 <program>
- Tiers: T0=interpreter, T1=basic compiled, T2=fully compiled
- Target: >80% T2 for hot functions, <10% T0
- ShowTiers=true REQUIRED to see tier breakdown
- Evidence if verified: Hot function shows >30% T0 (target <10%)
```

#### Step 3.2: Fermi Verification (MANDATORY)

**Process** (CRITICAL - prevents silent tool failures):

**Sub-step 2.1: Pre-Calculation**
- Estimate expected output magnitude BEFORE running tool
- Write down estimate explicitly
- Base estimate on:
  - Benchmark characteristics (known loop counts, recursion depth)
  - Language patterns
  - Theory prediction
- Be conservative: order of magnitude is sufficient

**Sub-step 2.2: Smoke Test (The Probe)**
- Run tool on trivial input first
- Verify tool produces expected output format
- Confirm tool is functional and accessible
- Example: Run cpu-sampler on "print 1;" program

**Sub-step 2.3: Execute & Validate**
- Run actual tool command on benchmark
- Compare output magnitude vs pre-calculated estimate
- **Credibility Threshold Check**:
  - **Scenario A (Within 1 Order of Magnitude)**: **ACCEPT** result
    - Tool is working correctly
    - Proceed with analysis
  - **Scenario B (>1 Order of Magnitude Divergence OR Unexpected Zero)**: **REJECT & DIAGNOSE**
    - **STOP** - Do NOT use this result
    - **Hypothesis**: Tool failed silently, wrong path, or permissions issue
    - **Action**: Run debug commands:
      - `ls -l <benchmark-file>` (check file exists and size)
      - `echo $?` (check exit code)
      - Re-run with verbose flags
      - Check tool installation
    - **Only** after proving tool health may you accept divergent result

**Example**:
```
Theory: "Hot functions have low compilation effectiveness"
Tool: cpu-sampler
Benchmark: queens.lox

Sub-step 2.1: Pre-Calculation
- queens.lox has recursive function
- Recursive calls likely hot
- Expected: 1-5 hot functions in output
- Estimated execution time: 2-3 seconds (known from benchmark run)
- **Pre-calculation**: Expect 1-5 functions in histogram

Sub-step 2.2: Smoke Test
- Command: ./lox --cpusampler --cpusampler.Delay=1000 trivial.lox
- Expected: Histogram with at least 1 function
- Result: Shows histogram with 2 functions ✓
- **Smoke test PASSED**: Tool works

Sub-step 2.3: Execute & Validate
- Command: ./lox --cpusampler --cpusampler.ShowTiers=true --cpusampler.Delay=2000 queens.lox
- Output: Histogram with 3 functions
- **Validation**: 3 functions is within 1 order of magnitude of estimate (1-5) ✓
- **Credibility**: ACCEPT result, proceed with analysis

Alternative failure scenario:
- Output: 0 functions (unexpected zero)
- **Validation**: REJECTED (divergence too large)
- **Diagnosis**:
  - Run: ls -l queens.lox → File exists, 523 bytes ✓
  - Run: ./lox queens.lox → Runs successfully ✓
  - Run: ./lox --cpusampler queens.lox → 0 functions still
  - **Root cause**: --cpusampler.Delay=2000 exceeds runtime (2.1s)
  - **Fix**: Reduce delay to 500ms
  - Rerun: Now shows 3 functions ✓
  - **Credibility**: ACCEPT after fix
```

#### Step 3.3: Run Tool and Analyze Results

**Process**:

1. **Execute tool command**
   - Use appropriate options based on documentation
   - Redirect output to file for analysis: `command > output.txt 2>&1`
   - Capture both stdout and stderr

2. **Parse output**
   - Extract relevant metrics
   - Identify key findings
   - Look for expected evidence (from theory description)

3. **Evaluate theory**
   - **✅ Verified**: Evidence found as predicted
     - Extract specific data points
     - Note magnitude of issue (how far from target)
   - **❌ Falsified**: Evidence not found or contradicts prediction
     - Document why theory was wrong
     - Note what was found instead
   - **⚠️ Inconclusive**: Insufficient data (rare)
     - Note why inconclusive
     - Suggest additional investigation

4. **Generate recommendations** (if verified)
   - Identify specific fix based on evidence
   - Provide code examples where applicable
   - Estimate impact (if possible)
   - Reference similar implementations or documentation

5. **Record findings**
   - Save tool output to file
   - Record theory verdict
   - Extract actionable insights
   - Note any follow-up theories generated

**Output**:
- Theory verdict (verified/falsified/inconclusive)
- Evidence excerpt from tool output
- Recommendation (if verified)
- Saved tool output file

**Example**:
```
Theory: "Hot functions have low compilation effectiveness"
Tool: cpu-sampler
Command: ./lox --cpusampler --cpusampler.ShowTiers=true --cpusampler.Delay=2000 queens.lox

Output (saved to cpu-sampler-queens.txt):
-----------------------------------------------------------
Sampling Histogram. Recorded 412 samples with period 10ms.
  Self Time: Time spent in function (excluding callees)
  Total Time: Time in function including callees

Name     || Total Time    || Self Time     || T0     | T1    | T2
queens   || 1850ms 88.0%  || 1850ms 88.0%  || 95.2% | 3.1%  | 1.7%
hasConflict || 250ms 11.9% || 250ms 11.9%  || 92.8% | 4.2%  | 3.0%
-----------------------------------------------------------

Parsing:
- Hot function: queens (88% of total time)
- Tier distribution: 95.2% T0 (interpreter), only 1.7% T2 (compiled)
- Target: >80% T2, <10% T0
- Gap: 95.2% T0 vs <10% target → 10x worse than target

Evaluation:
- Evidence found: ✅ YES
  - queens function shows 95.2% interpreter time (T0)
  - Only 1.7% fully compiled (T2)
  - Target is <10% T0, >80% T2
- Verdict: ✅ VERIFIED
  - Theory is correct: low compilation effectiveness
  - Severity: Critical (10x worse than target)

Recommendation:
**Problem**: queens function executing 95.2% in interpreter (target: <10%)
**Root Cause**: Functions likely not compiling or deoptimizing constantly
**Next Steps**:
  1. Run trace-compilation to see if compilation is happening
  2. If compiling: Check for deoptimization with trace-transfer-to-interpreter
  3. If not compiling: Check for compilation barriers with trace-performance-warnings
**Expected Impact**: Fixing this should yield 5-10x speedup (based on T0→T2 improvement)

**Generated Follow-up Theory**: "Compilation is failing or bailouts occurring"
- Tool: trace-compilation
- Priority: High (explains current finding)
```

### Phase 4: Generate Comprehensive Report

**Objective**: Produce detailed analysis document with all findings and recommendations

**Process**:

1. **Compile all data**
   - Benchmark execution times
   - Baseline comparisons and expectations
   - All theories with verdicts and evidence
   - Tool outputs (excerpts and full outputs)
   - Recommendations prioritized by impact

2. **Generate report structure**
   - Executive summary (high-level findings)
   - Benchmark results section
   - Performance comparison section
   - Theory verification section (each theory)
   - Prioritized recommendations section
   - Appendix with full tool outputs

3. **Write recommendations**
   - For each verified theory:
     - Explain the problem clearly
     - Provide specific fix (code examples if applicable)
     - Reference implementation examples or documentation
     - Estimate performance impact
     - Note dependencies between fixes

4. **Prioritize recommendations**
   - **Priority 1**: High impact, must-fix issues
   - **Priority 2**: Medium impact, should-fix issues
   - **Priority 3**: Low impact, nice-to-have improvements

5. **Save report**
   - Write to `PERFORMANCE_ANALYSIS_REPORT.md`
   - Include date, configuration, tool versions
   - Format for readability (tables, code blocks)
   - Include file references for tool outputs

**Report Structure**:

```markdown
# Performance Analysis Report

**Generated**: [DATE and TIME]
**Language**: [DETECTED NAME]
**Benchmarks Analyzed**: [COUNT]
**Theories Tested**: [COUNT verified] / [COUNT total]

## Executive Summary

[3-5 sentences summarizing key findings]

- **Critical Issues**: [COUNT] (blocking optimization)
- **Moderate Issues**: [COUNT] (limiting performance)
- **Expected Improvement**: [ESTIMATE] after fixes

**Top Finding**: [Most impactful verified theory]

---

## Benchmark Results

| Benchmark | Actual Time | Expected Range | Ratio vs Best | Status |
|-----------|-------------|----------------|---------------|--------|
| queens    | 2.1s        | 0.8s - 3.5s    | 2.6x slower   | ⚠️ Investigate |
| bounce    | 1.2s        | 0.5s - 2.0s    | 2.4x slower   | ✅ OK |
| towers    | 0.5s        | 0.3s - 1.0s    | 1.7x slower   | ✅ OK |

**Summary**: 1/3 benchmarks significantly slower than expected (queens)

---

## Baseline Comparison

### Language Characteristics
- **Type System**: Dynamic typing
- **Execution Model**: Bytecode VM with JIT compilation (Truffle/Graal)
- **Platform**: JVM-based (GraalVM Truffle)
- **Paradigm**: Object-oriented, imperative

### Comparable Languages
1. **Lua** - Similar execution model (bytecode VM + JIT, dynamic)
2. **Python 3** - Similar abstraction level and paradigm

### Performance Comparison (queens benchmark)
- **Your implementation**: 2.1s
- **Lua** (best case): 0.8s
- **Python 3** (worst case): 3.5s
- **Your ratio**: 2.6x slower than Lua, 1.7x faster than Python
- **Assessment**: ⚠️ Slower than expected (should approach Lua)

---

## Theory Verification Results

### Theory 1: "Hot functions have low compilation effectiveness"
- **Status**: ✅ VERIFIED
- **Tool**: cpu-sampler
- **Priority**: P1 (Critical)

**Evidence**:
```
Name     || Total Time    || T0     | T1    | T2
queens   || 1850ms 88.0%  || 95.2% | 3.1%  | 1.7%
```
- queens function: 95.2% interpreter time (T0)
- Target: <10% T0, >80% T2
- Gap: 10x worse than target

**Conclusion**: Critical compilation issue preventing optimization

---

### Theory 2: "Optimization barriers prevent peak performance"
- **Status**: ✅ VERIFIED
- **Tool**: trace-performance-warnings
- **Priority**: P1 (Critical)

**Evidence**:
```
[engine] perf warn queens |Partial evaluation could not inline the virtual runtime call Virtual to HotSpotMethod<CallTarget.call(Object[])>
[engine] perf warn queens |Partial evaluation could not inline the virtual runtime call Virtual to HotSpotMethod<ConditionProfile.profile(boolean)>
```
- 3 virtual call warnings found
- Prevents full inlining and optimization
- CallTarget and ConditionProfile not cached

**Conclusion**: Missing caching of compilation-final objects

---

### Theory 3: "Inlining budget exhausted for recursive calls"
- **Status**: ❌ FALSIFIED
- **Tool**: trace-inlining
- **Priority**: P2 (Medium)

**Evidence**:
```
[engine] inline start queens |IR Nodes 4500 |Truffle Callees 2 |Depth 0
[engine] Inlined hasConflict |call diff -1.00 |IR Nodes 800 |Depth 1
[engine] inline done queens |IR Nodes 4500 |...
```
- All calls successfully inlined (no Cutoff states)
- IR nodes well under budget (4500 < 12000)
- No budget exhaustion

**Conclusion**: Inlining working correctly; issue is elsewhere (theories 1 & 2)

---

## Recommendations

### Priority 1: Fix Missing Caching (Critical Impact)

**Problem**: CallTarget and ConditionProfile not cached, causing virtual calls

**Evidence**: trace-performance-warnings shows 3 virtual call warnings

**Fix**: Cache compilation-final objects using @Cached

**Implementation**:
```java
// Current (bad):
@Specialization
public Object call(VirtualFrame frame, Object function) {
    CallTarget target = ((LoxFunction) function).getCallTarget();
    return target.call(args);  // Virtual call warning!
}

// Fixed (good):
@Specialization(guards = "function == cachedFunction", limit = "3")
public Object callCached(VirtualFrame frame, LoxFunction function,
        @Cached("function") LoxFunction cachedFunction,
        @Cached("cachedFunction.getCallTarget()") CallTarget callTarget) {
    return callTarget.call(args);  // Optimized direct call!
}

@Specialization(replaces = "callCached")
public Object callUncached(VirtualFrame frame, LoxFunction function) {
    return function.getCallTarget().call(args);
}
```

Similarly for ConditionProfile:
```java
// Current (bad):
private ConditionProfile profile = ConditionProfile.createBinaryProfile();

// Fixed (good):
@Specialization
public Object execute(VirtualFrame frame,
        @Cached("createBinaryProfile()") ConditionProfile profile) {
    if (profile.profile(condition)) { ... }
}
```

**Expected Impact**: 5-10x speedup (T0 95% → T2 >80%)

**Dependencies**: None (standalone fix)

---

### Priority 2: Investigate Compilation Behavior (High Impact)

**Problem**: Only 1.7% T2 compiled time suggests compilation issues

**Evidence**: cpu-sampler shows 95.2% T0, 1.7% T2

**Next Steps**:
1. Run trace-compilation to verify compilation is happening:
   ```bash
   ./lox --engine.TraceCompilation queens.lox 2>&1 | grep queens
   ```
2. Check for bailouts or deoptimization
3. If compiling but deoptimizing: Use trace-transfer-to-interpreter
4. If not compiling: Check compilation thresholds or barriers

**Expected Impact**: After Priority 1 fix, should see T2 >80%

**Dependencies**: Should be investigated after Priority 1 fix

---

## Summary

**Verified Theories**: 2/3
- ✅ Low compilation effectiveness (cpu-sampler)
- ✅ Optimization barriers (trace-performance-warnings)
- ❌ Inlining budget exhaustion (trace-inlining)

**Critical Findings**:
1. Missing caching causing virtual calls and preventing optimization
2. Only 1.7% fully compiled execution (target >80%)

**Expected Improvement**: 5-10x speedup after implementing Priority 1 fix

**Next Steps**:
1. Implement CallTarget and ConditionProfile caching (Priority 1)
2. Re-run benchmarks to measure improvement
3. Investigate remaining compilation issues if still slow (Priority 2)
```

**File Naming Convention**:
- Report: `PERFORMANCE_ANALYSIS_REPORT.md`
- Tool outputs: `tool-outputs/[tool-name]-[benchmark].txt`

---

## Example Usage Scenario

**User**: "Analyze the performance of my Lox implementation. I've run benchmarks and have BENCHMARK_BASELINE.md."

**Skill Actions**:

### Phase 1: Load Data

```
Reading BENCHMARK_BASELINE.md...

Language Characteristics:
- Type System: Dynamic typing
- Execution Model: Bytecode VM with JIT (Truffle)
- Comparable Languages: Lua, Python 3

Benchmark Expectations:
- queens: 0.8s (Lua) to 3.5s (Python)
- bounce: 0.5s to 2.0s
- towers: 0.3s to 1.0s

Requesting timing data...
User provides:
- queens: 2.1s
- bounce: 1.2s
- towers: 0.5s
```

### Phase 2: Compare

```
Comparison Results:

queens: 2.1s
- Expected: 0.8s - 3.5s
- Ratio: 2.6x slower than best (Lua)
- Status: ⚠️ INVESTIGATE (significantly slower)

bounce: 1.2s
- Expected: 0.5s - 2.0s
- Ratio: 2.4x slower than best
- Status: ✅ OK (within range, minor gap)

towers: 0.5s
- Expected: 0.3s - 1.0s
- Ratio: 1.7x slower than best
- Status: ✅ OK (within range)

Problem Benchmarks: queens (primary focus)
```

### Phase 3: Generate Theories

```
Analyzing implementation code...
→ Searching for node files: **/*Node.java
→ Found: CallNode.java, AddNode.java, ReadVariableNode.java
→ Checking for anti-patterns...
→ Found: CallTarget.call() without @Cached in CallNode.java:42
→ Found: Only Object specialization in AddNode.java

Theories for queens (2.6x slower, recursive algorithm):

Priority 1: "Hot functions have low compilation effectiveness"
- Tool: cpu-sampler
- Verification: Quick, initial profiling
- Evidence if true: High T0 % (interpreter time)

Priority 2: "CallTarget not cached in CallNode"
- Source: Code analysis found CallNode.java:42
- Tool: trace-performance-warnings
- Evidence if true: Virtual call warnings at CallNode execution

Priority 3: "Optimization barriers prevent peak performance"
- Tool: trace-performance-warnings
- Verification: Targeted diagnostic
- Evidence if true: Virtual call warnings

Priority 4: "Inlining budget exhausted for recursive calls"
- Tool: trace-inlining
- Verification: Medium complexity
- Evidence if true: Cutoff states
```

### Phase 4: Verify Theories

**Theory 1**: "Hot functions have low compilation effectiveness"

```
Step 4a: Load Documentation
→ Reading .claude/skills/performance-analysis/CPUSampler.md
→ Retrieved: Command syntax, tier meanings, targets

Step 4b: Fermi Verification
→ Pre-calculation: Expect 1-5 functions in output
→ Smoke test: ./lox --cpusampler trivial.lox → ✓ Works
→ Execute: ./lox --cpusampler --cpusampler.ShowTiers=true --cpusampler.Delay=2000 queens.lox
→ Output: 3 functions shown
→ Validation: 3 is within 1 order of magnitude of 1-5 ✓
→ Credibility: ACCEPT

Step 4c: Analyze Results
→ Parsing output...
→ queens: 95.2% T0, 1.7% T2
→ Target: <10% T0, >80% T2
→ Gap: 10x worse than target
→ Verdict: ✅ VERIFIED

Step 4d: Record Findings
→ Saved output to tool-outputs/cpu-sampler-queens.txt
→ Recommendation: Investigate compilation issues
→ Generated follow-up theory: "Compilation failing"
```

**Theory 2**: "Optimization barriers prevent peak performance"

```
[Similar process...]
→ Verdict: ✅ VERIFIED
→ Evidence: 3 virtual call warnings
→ Recommendation: Cache CallTarget and ConditionProfile
```

**Theory 3**: "Inlining budget exhausted"

```
[Similar process...]
→ Verdict: ❌ FALSIFIED
→ Evidence: All calls inlined successfully, no Cutoff states
→ Conclusion: Inlining works fine; issue is elsewhere
```

### Phase 5: Generate Report

```
Compiling findings...

Writing PERFORMANCE_ANALYSIS_REPORT.md:
✓ Executive summary
✓ Benchmark results
✓ Baseline comparison
✓ 3 theories with verdicts
✓ 2 prioritized recommendations
✓ Tool outputs appendix

Saving tool outputs:
✓ tool-outputs/cpu-sampler-queens.txt
✓ tool-outputs/trace-performance-warnings-queens.txt
✓ tool-outputs/trace-inlining-queens.txt
```

### Phase 6: Present Findings

```
Performance Analysis Complete!

Executive Summary:
- Analyzed 3 benchmarks
- 2/3 theories verified
- 1 critical issue found: Missing caching causing 95.2% interpreter time
- Expected improvement: 5-10x speedup after fix

Top Recommendation:
Implement CallTarget and ConditionProfile caching to eliminate virtual calls

Report saved to: PERFORMANCE_ANALYSIS_REPORT.md
Tool outputs in: tool-outputs/
```

---

## Best Practices

The skill follows these analysis best practices:

### 1. Progressive Refinement
- Load existing data (don't rerun benchmarks)
- Start with quick profiling (cpu-sampler)
- Use targeted tools for specific theories
- Deep-dive only when needed (compiler graphs last)

### 2. Theory Prioritization
- High-impact theories first (compilation effectiveness)
- Quick-to-verify theories early (build confidence)
- Complex theories later (after simpler ones tested)
- Stop if theory explains issue (avoid over-analysis)

### 3. Tool Selection
- **Always** start with cpu-sampler for initial profiling
- Use trace-performance-warnings for optimization barriers
- Use compiler graphs LAST (most complex)
- Match tool to theory type automatically

### 4. Fermi Verification (MANDATORY)
- Never skip pre-calculation step
- Always run smoke test first
- Reject results that deviate >1 order of magnitude
- Diagnose tool health before accepting divergent results
- Prevents silent tool failures and wrong conclusions

### 5. Evidence-Based Conclusions
- Verify theories with actual data (not assumptions)
- Don't assume root causes without evidence
- Multiple tools can provide corroborating evidence
- Mark as falsified when evidence contradicts theory

### 6. Actionable Recommendations
- Specific code changes (not vague suggestions)
- Include code examples for fixes
- Reference documentation or similar implementations
- Estimate impact where possible (based on gaps)
- Prioritize by impact and implementation effort

### 7. Separation of Concerns
- Assume benchmarks already executed (don't rerun)
- Reuse baseline data from BENCHMARK_BASELINE.md
- Focus on analysis and diagnosis
- Generate comprehensive report for future reference

---

## Common Pitfalls to Avoid

The skill warns about these mistakes:

- ❌ **Rerunning benchmarks**: Wasteful; reuse existing timing data
- ❌ **Skipping Fermi verification**: Silent tool failures lead to wrong conclusions
- ❌ **Running all tools blindly**: Overwhelming data, no clear insights
- ❌ **Assuming root causes**: Verify theories with evidence, don't guess
- ❌ **Single tool verification**: Corroborate findings when possible
- ❌ **Over-optimizing cold paths**: Focus on problem benchmarks only
- ❌ **Vague recommendations**: "Make it faster" isn't actionable
- ❌ **Ignoring falsified theories**: Learn from what's NOT the problem
- ❌ **Not saving tool outputs**: Needed for detailed investigation later

---

## Success Criteria

**Good analysis**:
- ✅ Benchmark data and baseline loaded successfully
- ✅ >3 theories generated and verified/falsified
- ✅ Fermi verification passed for all tool runs
- ✅ Specific recommendations with fix examples (for verified theories)
- ✅ Report saved to `PERFORMANCE_ANALYSIS_REPORT.md`

**Excellent analysis**:
- ✅ Multiple tools corroborate findings
- ✅ Verified theories lead to actionable fixes with code examples
- ✅ Performance improvement estimates provided (based on gaps)
- ✅ Root causes identified (not just symptoms)
- ✅ Tool outputs saved for future reference
- ✅ Follow-up theories generated from findings

---

## Integration with Other Skills

### Prerequisite Skills
- **benchmark-baseline**: Should be run FIRST to create BENCHMARK_BASELINE.md
  - Establishes language characteristics
  - Identifies comparable languages
  - Sets performance expectations
  - Creates benchmarks to execute

### Tools Used (with Documentation Files)
This skill uses GraalVM profiling and diagnostic tools:
- **cpu-sampler**: Time-based profiling (./CPUSampler.md)
- **cpu-tracer**: Execution frequency analysis (./CPUTracer.md)
- **trace-performance-warnings**: Optimization barriers (./Trace Performance Warnings.md)
- **trace-inlining**: Inlining decisions (./Trace Inlining.md)
- **trace-compilation**: Compilation lifecycle (./Trace Compilation.md)
- **trace-transfer-to-interpreter**: Deoptimization (./Trace Transfer To Interpreter.md)
- **memory-tracer**: Allocation profiling (./MemoryTracer.md)
- **compiler-graph**: Deep IR analysis (./Dump Compiler Graph.md)

All tool documentation files are located in `.claude/skills/performance-analysis/` and MUST be loaded before running each tool.

### Related Skill
- **analyze-compiler-graph**: Dedicated skill for comprehensive compiler graph analysis using seafoam, bgv2json, and jq. Use this skill when theories require deep IR-level investigation (e.g., verifying escape analysis, finding boxing overhead, confirming indirect calls).
- **graal-truffle-docs**: For doing web searches for understanding GraalVM Truffle optimization techniques.

### When NOT to Use This Skill

Use individual tools directly when:
- **You already know the specific problem** → Run the specific tool directly
- **Quick investigation needed** → Use cpu-sampler for initial profiling
- **Following up on known issue** → Run appropriate tool
- **Learning how a tool works** → Experiment with simple examples
- **Benchmarks not run yet** → Run benchmark-baseline skill first

---

## Workflow Integration

Typical performance optimization workflow:

```
Step 1: benchmark-baseline skill
→ Creates benchmarks
→ Fetches baseline data
→ Produces BENCHMARK_BASELINE.md

Step 2: Run benchmarks
→ Execute each benchmark
→ Record timing data

Step 3: performance-analysis skill ← THIS SKILL
→ Loads benchmark results
→ Loads BENCHMARK_BASELINE.md
→ Generates and verifies theories
→ Produces PERFORMANCE_ANALYSIS_REPORT.md

Step 4: Implement fixes
→ Follow recommendations from report
→ Make code changes

Step 5: Re-run benchmarks
→ Measure improvement

Step 6: (Optional) Re-run performance-analysis
→ Verify issues resolved
→ Find remaining opportunities
```

---

## Implementation Notes

This skill:
- **Assumes benchmarks executed** (doesn't rerun them)
- **Loads BENCHMARK_BASELINE.md** (from benchmark-baseline skill)
- **Generates theories automatically** based on gaps, patterns, and code analysis
- **Loads tool documentation files** (MANDATORY before each tool execution)
- **Enforces Fermi verification** (MANDATORY for all tool executions)
- **Saves all tool outputs** to `tool-outputs/` directory
- **Produces final report**: `PERFORMANCE_ANALYSIS_REPORT.md`
- **Duration**: 10-25 minutes depending on theory count and tool complexity
- **Intermediate saves**: Theory status and findings saved incrementally
- **Documentation files**: Located in `.claude/skills/performance-analysis/`

---

## Reference Documentation

For detailed information:
- **Tool documentation files**: In `.claude/skills/performance-analysis/` directory
- **benchmark-baseline skill**: For baseline establishment workflow
- **Benchmarks Game**: https://benchmarksgame-team.pages.debian.net/benchmarksgame/
- **AreWeFastYet**: https://github.com/smarr/are-we-fast-yet
- **GraalVM Profiling**: https://www.graalvm.org/latest/tools/profiling/
- **Truffle Optimization**: https://www.graalvm.org/latest/graalvm-as-a-platform/language-implementation-framework/Optimizing/

---

## Quick Reference: Help Flags

All GraalVM performance tools and options provide built-in help. Use these flags to discover available options and their usage:

### Tool-Specific Help

Get detailed options for each profiling tool:

```bash
<language-launcher> --help:cpusampler      # CPU Sampler options
<language-launcher> --help:cputracer       # CPU Tracer options
<language-launcher> --help:memtracer       # Memory Tracer options
<language-launcher> --help:heapmonitor     # Heap Allocation Monitor options
<language-launcher> --help:dap             # Debug Adapter Protocol options
<language-launcher> --help:inspect         # Chrome Inspector options
```

**Key CPU Sampler Options** (from `--help:cpusampler`):
- `--cpusampler.ShowTiers=true|false|0,1,2` - Show compilation tier information (T0=interpreter, T1/T2=compiled)
- `--cpusampler.Delay=<ms>` - Delay sampling start (avoid warmup noise)
- `--cpusampler.Period=<ms>` - Sampling interval (default: 10ms)
- `--cpusampler.Output=histogram|calltree|json|flamegraph` - Output format
- `--cpusampler.FilterRootName=<filter>` - Filter by function name pattern

**Key CPU Tracer Options** (from `--help:cputracer`):
- `--cputracer.TraceCalls` - Count call invocations
- `--cputracer.TraceStatements` - Count statement executions
- `--cputracer.Output=histogram|json` - Output format

**Key Memory Tracer Options** (from `--help:memtracer`):
- `--memtracer.Output=typehistogram|histogram|calltree` - Output format
- `--memtracer.TraceCalls` - Track allocations per call
- `--memtracer.TraceStatements` - Track allocations per statement

### Engine Options

Get Truffle engine configuration options:

```bash
<language-launcher> --help:engine          # User and expert engine options
<language-launcher> --help:engine:internal # Internal engine options (includes trace flags)
```

**Important Engine Trace Options** (from `--help:engine:internal`):
- `--engine.TraceCompilation` - Print compilation events
- `--engine.TraceCompilationDetails` - Print compilation queuing details
- `--engine.TraceCompilationPolymorphism` - Print polymorphic/generic nodes after compilation
- `--engine.TraceTransferToInterpreter` - Print deoptimization events (stack traces on deopt)
- `--engine.TraceAssumptions` - Print stack traces on assumption invalidation
- `--engine.TraceSplitting` - Print splitting decisions
- `--engine.TraceStackTraceLimit=[1, inf)` - Number of stack frames to print (default: 20)

**Compilation Thresholds** (from `--help:engine`):
- `--engine.FirstTierCompilationThreshold=[1, inf)` - First-tier compilation threshold (default: 400)
- `--engine.LastTierCompilationThreshold=[1, inf)` - Last-tier compilation threshold (default: 10000)
- `--engine.MinInvokeThreshold=[1, inf)` - Minimum invocations before compilation (default: 3)
- `--engine.MultiTier=true|false` - Enable multi-tier compilation (default: true)
- `--engine.OSR=true|false` - Enable on-stack replacement for loops (default: true)

### Compiler Options

Get Truffle compiler configuration and optimization options:

```bash
<language-launcher> --help:compiler        # Basic compiler options
<language-launcher> --help:compiler:internal # Internal compiler options (includes trace flags)
```

**Important Compiler Trace Options** (from `--help:compiler:internal`):
- `--compiler.TraceInlining` - Print inlining decisions
- `--compiler.TraceInliningDetails` - Print detailed inlining call tree
- `--compiler.TracePerformanceWarnings` - Print optimization barriers (virtual calls, type checks, etc.)
- `--compiler.TraceMethodExpansion` - Print expanded Java method tree with statistics
- `--compiler.TraceNodeExpansion` - Print expanded Truffle node tree with statistics

**Inlining Control** (from `--help:compiler`):
- `--compiler.Inlining=true|false` - Enable/disable automatic inlining (default: true)
- `--compiler.InliningExpansionBudget=[1, inf)` - Expansion budget for inlining (default: 12000)
- `--compiler.InliningInliningBudget=[1, inf)` - Inlining budget (default: 12000)
- `--compiler.InliningRecursionDepth=[0, inf)` - Max recursion depth for inlining (default: 2)

### General Help

```bash
<language-launcher> --help                 # Basic usage and available tools
<language-launcher> --help:all             # All options (103+ options)
<language-launcher> --help:vm              # Host VM options
<language-launcher> --version:graalvm      # GraalVM version information
```

### VM Options for Compiler Graph Dumping

Pass options to the Graal compiler via `--vm.D` prefix:

```bash
# Dump compiler graphs (see "Dump Compiler Graph.md" for details)
<language-launcher> --vm.Djdk.graal.Dump=Truffle:1
<language-launcher> --vm.Djdk.graal.PrintGraph=File
<language-launcher> --vm.Djdk.graal.DumpPath=<path>

# Method filtering
<language-launcher> --vm.Djdk.graal.MethodFilter=<pattern>

# Source position tracking (for correlating IR with source code)
<language-launcher> --vm.Djdk.graal.TrackNodeSourcePosition=true
```

**Note**: For Truffle languages, use `--engine.NodeSourcePositions` instead of the low-level VM option.

### Usage Examples

**Example 1: Get all CPU Sampler options**
```bash
./lox --help:cpusampler
```

**Example 2: Discover trace compilation options**
```bash
./lox --help:engine:internal | grep Trace
```

**Example 3: Find inlining-related options**
```bash
./lox --help:compiler:internal | grep -i inlin
```

**Example 4: See all available tools**
```bash
./lox --help
# Look under "Tools:" section
```

### Quick Tool Selection

Use these help flags to quickly find the right tool for your analysis:

| Analysis Goal | Help Flag | Key Options to Check |
|---------------|-----------|---------------------|
| Profile time distribution | `--help:cpusampler` | `ShowTiers`, `Delay`, `Period` |
| Count execution frequency | `--help:cputracer` | `TraceCalls`, `TraceStatements` |
| Track allocations | `--help:memtracer` | `Output`, `TraceCalls` |
| Debug compilation | `--help:engine:internal` | `TraceCompilation*` flags |
| Debug inlining | `--help:compiler:internal` | `TraceInlining*` flags |
| Find optimization barriers | `--help:compiler:internal` | `TracePerformanceWarnings` |
| Debug deoptimization | `--help:engine:internal` | `TraceTransferToInterpreter` |

---

## Related Skills

### Prerequisite
- **benchmark-baseline**: Establish baselines FIRST (creates BENCHMARK_BASELINE.md)

### Deep Investigation
- **analyze-compiler-graph**: Comprehensive compiler graph analysis skill using seafoam, bgv2json, and jq. Invoked automatically when theories require IR-level verification (escape analysis, boxing, indirect calls). Can also be used standalone for deep compiler optimization investigation.

### Documentation Search
- **graal-truffle-docs**: Skill for searching GraalVM Truffle documentation online for understanding optimization techniques.

### Alternatives
- Use GraalVM tools directly for targeted investigation
- Use benchmark-baseline for baseline establishment only
- Use analyze-compiler-graph for deep compiler IR analysis

### Tool Documentation
All tool documentation is included in `.claude/skills/performance-analysis/`:
- ./CPUSampler.md
- ./CPUTracer.md
- ./Trace Performance Warnings.md
- ./Trace Inlining.md
- ./Trace Compilation.md
- ./Trace Transfer To Interpreter.md
- ./MemoryTracer.md
- ./Dump Compiler Graph.md