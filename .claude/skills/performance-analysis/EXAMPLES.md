# Performance Analysis Examples

Complete usage scenarios and report templates.

---

## Example Usage Scenario

**User**: "Analyze the performance of my Lox implementation. Focus on minor implementation issues and architectural patterns. I've run benchmarks and have BENCHMARK_BASELINE.md."

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

### Phase 2: Compare & Generate Theories

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

```
Analyzing implementation code systematically...

Step 1: Language configuration
→ Reading LoxLanguage.java bytecode configuration
→ Checking optimization flags and settings
→ Found: Boxing configuration looks good

Step 2: ALL operations/nodes
→ Reading LoxBytecodeRootNode.java
→ Checking all @Operation definitions
→ Found 20 operations total
→ Analyzing each for anti-patterns...
→ Found: Missing primitive specializations in 3 operations

Step 3: Runtime data structures
→ Reading LoxFunction.java, LoxClass.java, GlobalObject.java
→ Checking for @TruffleBoundary and allocation patterns
→ Found: CallTarget.call() without @Cached in function calls
→ Found: ConditionProfile not cached

Step 4: Node implementations
→ Reading src/main/java/de/hpi/swa/lox/nodes/*.java
→ Found: CallNode.java:42 missing caching
→ Found: AddNode.java only has Object specialization

Generated Theories for queens (2.6x slower, recursive algorithm):

Priority 1: "Hot functions have low compilation effectiveness"
- Tools: cpu-sampler (tier distribution), cpu-tracer (compiled %), trace-compilation (why not compiling)
- Verification: Quick, foundational profiling
- Evidence if true: High T0 % in sampler, low compiled % in tracer
- Impact: Critical (blocks all optimization)

Priority 2: "CallTarget not cached in CallNode"
- Source: Code analysis found CallNode.java:42
- Tools: trace-performance-warnings (virtual calls), cpu-sampler (impact on hot path)
- Evidence if true: Virtual call warnings at CallNode, high time in CallNode
- Impact: High (recursive benchmark = many calls)

Priority 3: "ConditionProfile not cached causing virtual calls"
- Source: Code analysis of control flow nodes
- Tools: trace-performance-warnings (virtual calls), trace-inlining (inlining failures)
- Evidence if true: ConditionProfile.profile() warnings, inline cutoffs
- Impact: High (every conditional statement affected)

Priority 4: "Inlining budget exhausted for recursive calls"
- Tools: trace-inlining (cutoff states), cpu-tracer (depth analysis)
- Verification: Medium complexity
- Evidence if true: Cutoff states for queens function
- Impact: Medium (would limit optimization depth)
```

### Phase 3: Verify Theories

**Theory 1**: "Hot functions have low compilation effectiveness"

```
Tool 1/3: cpu-sampler

Step 3.1: Load Documentation
→ Reading .claude/skills/performance-analysis/CPUSampler.md
→ Retrieved: Command syntax, tier meanings, targets

Step 3.2: Fermi Verification
→ Pre-calculation: Expect 1-5 functions in output
→ Smoke test: ./lox --cpusampler trivial.lox → ✓ Shows 1 function
→ Execute: ./lox --cpusampler --cpusampler.ShowTiers=true --cpusampler.Delay=2000 queens.lox
→ Output: 3 functions shown
→ Validation: 3 is within 1 order of magnitude of 1-5 ✓
→ Credibility: ACCEPT

Step 3.3: Analyze Results
→ Parsing output...
→ queens: 95.2% T0, 1.7% T2
→ Target: <10% T0, >80% T2
→ Gap: 10x worse than target
→ Evidence: ✅ FOUND (critical compilation issue)
→ Saved to tool-outputs/cpu-sampler-queens.txt

Tool 2/3: cpu-tracer

Step 3.1: Load Documentation
→ Reading .claude/skills/performance-analysis/CPUTracer.md
→ Retrieved: Shows interpreted vs compiled execution counts

Step 3.2: Fermi Verification
→ Pre-calculation: Expect queens function executed 100-10000 times (recursive)
→ Smoke test: ./lox --cputracer trivial.lox → ✓ Shows execution counts
→ Execute: ./lox --cputracer queens.lox
→ Output: queens executed 5,432 times total
→ Validation: 5,432 is within expected range ✓
→ Credibility: ACCEPT

Step 3.3: Analyze Results
→ Parsing output...
→ queens: 5,200 interpreted (95.7%), 232 compiled (4.3%)
→ Target: >95% compiled for hot functions
→ Evidence: ✅ FOUND (confirms low compilation)
→ Saved to tool-outputs/cpu-tracer-queens.txt

Tool 3/3: trace-compilation

Step 3.1: Load Documentation
→ Reading .claude/skills/performance-analysis/Trace Compilation.md
→ Retrieved: Shows when/if compilation happens

Step 3.2: Fermi Verification
→ Pre-calculation: Expect 1-3 compilation events for hot functions
→ Smoke test: ./lox --engine.TraceCompilation trivial.lox 2>&1 | grep "opt done" → ✓ Works
→ Execute: ./lox --engine.TraceCompilation queens.lox 2>&1 | grep queens
→ Output: 2 compilation events shown
→ Validation: 2 is within expected range ✓
→ Credibility: ACCEPT

Step 3.3: Analyze Results
→ Parsing output...
→ [T1] opt done queens (tier 1, basic optimization)
→ [T2] opt bailout queens (tier 2 compilation FAILED)
→ Evidence: ✅ FOUND (T2 compilation failing, explains low T2%)
→ Saved to tool-outputs/trace-compilation-queens.txt

Step 3.4: Combine Evidence
→ All 3 tools confirm theory
→ cpu-sampler: 95.2% T0 (symptom)
→ cpu-tracer: 95.7% interpreted execution (confirms symptom)
→ trace-compilation: T2 bailout (root cause!)
→ Verdict: ✅ VERIFIED

Step 3.5: Characterize Issue
→ Root cause: T2 compilation failing (bailout)
→ Issue requires deeper investigation: Use trace-performance-warnings to identify cause
→ Generated follow-up theory: "Optimization barriers causing T2 bailout"
```

**Theory 2**: "CallTarget not cached in CallNode"

```
Tool 1/2: trace-performance-warnings

Step 3.1: Load Documentation
→ Reading .claude/skills/performance-analysis/Trace Performance Warnings.md
→ Retrieved: Shows virtual calls preventing optimization

Step 3.2: Fermi Verification
→ Pre-calculation: CallNode used frequently in recursive benchmark, expect 1-10 warnings
→ Smoke test: ./lox --engine.TracePerformanceWarnings=all trivial.lox 2>&1 | grep "perf warn" → ✓ Works
→ Execute: ./lox --engine.TracePerformanceWarnings=all queens.lox 2>&1 | grep "perf warn"
→ Output: 5 warning groups shown
→ Validation: 5 is within expected range ✓
→ Credibility: ACCEPT

Step 3.3: Analyze Results
→ Parsing output...
→ Warning: "Virtual to HotSpotMethod<CallTarget.call(Object[])>" at CallNode.java:42
→ Stack trace confirms exact location
→ Evidence: ✅ FOUND (exact match to theory)
→ Saved to tool-outputs/trace-performance-warnings-queens.txt

Tool 2/2: cpu-sampler

Step 3.1: Already loaded from Theory 1

Step 3.2: Fermi Verification (already done)

Step 3.3: Re-analyze Results for CallNode
→ Parsing previous output...
→ CallNode not directly visible (inlined into queens)
→ But queens shows 88% total time with virtual calls
→ Evidence: ✅ SUPPORTING (high impact on hot path)

Step 3.4: Combine Evidence
→ trace-performance-warnings: Direct evidence of virtual call at CallNode.java:42
→ cpu-sampler: Shows high time in affected function
→ Verdict: ✅ VERIFIED

Step 3.5: Characterize Issue
→ Issue: CallTarget.call() not cached, causing virtual calls at CallNode.java:42
→ Location: CallNode.java:42
→ Impact: Virtual calls in hot path (queens 88% total time), blocking T2 compilation
```

**Theory 3**: "ConditionProfile not cached causing virtual calls"

```
Tool 1/2: trace-performance-warnings

Step 3.1: Already loaded from Theory 2

Step 3.2: Fermi Verification
→ Pre-calculation: Conditionals used frequently, expect 1-10 warnings
→ Smoke test: Already done for Theory 2
→ Execute: Use same output from Theory 2
→ Output: 5 warning groups (already validated)
→ Credibility: ACCEPT

Step 3.3: Analyze Results
→ Parsing output...
→ Warning: "Virtual to HotSpotMethod<ConditionProfile.profile(boolean)>" at IfNode.java:28
→ Evidence: ✅ FOUND
→ Saved to tool-outputs/trace-performance-warnings-queens.txt (same file)

Tool 2/2: trace-inlining

Step 3.1: Load Documentation
→ Reading .claude/skills/performance-analysis/Trace Inlining.md
→ Retrieved: Shows inlining decisions and failures

Step 3.2: Fermi Verification
→ Pre-calculation: Expect 1-5 inlining events for queens function
→ Smoke test: ./lox --engine.TraceInlining trivial.lox 2>&1 | grep "inline" → ✓ Works
→ Execute: ./lox --engine.TraceInlining queens.lox 2>&1 | grep queens
→ Output: 0 lines shown (unexpected!)
→ Validation: REJECTED (expected 1-5, got 0 - divergence too large)

Step 3.2b: Diagnosis
→ Run: ./lox queens.lox → ✓ Runs successfully
→ Run: ./lox --engine.TraceInlining queens.lox 2>&1 | head -20
→ Output shows: "inline start :program" but no queens events
→ Root cause: queens never compiles to T2 (from Theory 1), so no inlining trace
→ This is inconclusive due to prerequisite issue
→ Credibility: ⚠️ INCONCLUSIVE (tool can't provide data due to compilation failure)

Step 3.4: Combine Evidence
→ trace-performance-warnings: Direct evidence of ConditionProfile virtual call
→ trace-inlining: Inconclusive (no T2 compilation yet)
→ Verdict: ⚠️ PARTIALLY VERIFIED (1/2 tools confirm, 1 inconclusive)

Step 3.5: Characterize Issue
→ Issue: ConditionProfile.profile() not cached, causing virtual calls
→ Impact: Virtual calls in conditional statements throughout codebase
→ Note: trace-inlining inconclusive until T2 compilation works (Theory 1 & 2 block this)
```

**Theory 4**: "Inlining budget exhausted for recursive calls"

```
Tool 1/2: trace-inlining

Step 3.1: Already loaded from Theory 3

Step 3.2: Fermi Verification & Diagnosis
→ Already determined inconclusive due to no T2 compilation
→ Cannot verify this theory without successful compilation
→ Credibility: ⚠️ INCONCLUSIVE

Tool 2/2: cpu-tracer

Step 3.1: Already loaded from Theory 1

Step 3.2: Use existing results

Step 3.3: Analyze for recursion depth
→ cpu-tracer doesn't show call depth
→ Cannot determine if budget exhaustion is the issue
→ Evidence: ⚠️ INCONCLUSIVE (wrong tool for this metric)

Step 3.4: Combine Evidence
→ trace-inlining: Inconclusive (no T2 compilation)
→ cpu-tracer: Inconclusive (doesn't show depth)
→ Verdict: ⚠️ INCONCLUSIVE (need T2 compilation first)

Step 3.5: Note for Follow-up
→ Re-test this theory after Theory 1, 2, 3 are investigated/resolved
→ Alternative: Could use compiler graph analysis if available
→ Status: Defer until T2 compilation issue resolved
```

### Phase 4: Generate Report

```
Compiling findings...

Writing PERFORMANCE_ANALYSIS_REPORT.md:
✓ Executive summary
✓ Benchmark results
✓ Baseline comparison
✓ 4 theories with verdicts (2 verified, 1 partially verified, 1 inconclusive)
✓ Multi-tool evidence sections
✓ Inconclusive theories section with follow-up plan
✓ 2 prioritized issues by severity
✓ Tool outputs appendix

Saving tool outputs:
✓ tool-outputs/cpu-sampler-queens.txt
✓ tool-outputs/cpu-tracer-queens.txt
✓ tool-outputs/trace-compilation-queens.txt
✓ tool-outputs/trace-performance-warnings-queens.txt
✓ tool-outputs/trace-inlining-queens.txt (inconclusive - documented)
```

### Final Summary

```
Performance Analysis Complete!

Executive Summary:
- Analyzed 1 benchmark (queens - 2.6x slower than expected)
- 4 theories tested: 2 verified, 1 partially verified, 1 inconclusive
- Critical issue found: T2 compilation failing (95.2% T0 vs <10% target)
- Root cause identified: Virtual calls from uncached CallTarget and ConditionProfile
- Impact: 10x worse than target compilation tier distribution

Priority 1 Issues (Critical):
1. CallTarget.call() not cached → virtual calls at CallNode.java:42
2. ConditionProfile.profile() not cached → virtual calls in conditional statements

Inconclusive Theories:
- Theory 4: Cannot test until T2 compilation issue resolved (defer)

Report saved to: PERFORMANCE_ANALYSIS_REPORT.md
Tool outputs in: tool-outputs/
```

---

## Example Report Template

```markdown
# Performance Analysis Report

**Generated**: 2024-01-15 14:30:00
**Language**: Lox (Truffle/GraalVM)
**Benchmarks Analyzed**: 1 (queens)
**Theories Tested**: 2 verified, 1 partially verified, 1 inconclusive / 4 total
**Tools Executed**: cpu-sampler, cpu-tracer, trace-compilation, trace-performance-warnings, trace-inlining

## Executive Summary

Analysis identified critical T2 compilation failure in the queens benchmark (2.6x slower than Lua baseline). Root cause: uncached CallTarget and ConditionProfile causing virtual calls that prevent full optimization. Multi-tool verification (cpu-sampler, cpu-tracer, trace-compilation, trace-performance-warnings) confirmed 95.2% interpreter execution with T2 bailouts. Two theories fully verified with multiple tools, one partially verified, and one inconclusive pending T2 compilation resolution.

- **Critical Issues**: 2 verified (blocking T2 compilation)
- **Partially Verified Issues**: 1 (ConditionProfile caching)
- **Inconclusive Theories**: 1 (deferred until T2 compilation issue resolved)
- **Impact**: 10x worse than target tier distribution (95.2% T0 vs <10% target)

**Top Finding**: Uncached CallTarget and ConditionProfile cause virtual calls, preventing T2 compilation → 95% interpreter execution (target: <10%)

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
- **Tools Used**: cpu-sampler, cpu-tracer, trace-compilation (3/3 tools)
- **Priority**: P1 (Critical)

**Evidence from Multiple Tools**:

**Tool 1: cpu-sampler** (tier distribution)
```
Name     || Total Time    || T0     | T1    | T2
queens   || 1850ms 88.0%  || 95.2% | 3.1%  | 1.7%
```
- queens function: 95.2% interpreter time (T0)
- Target: <10% T0, >80% T2
- Gap: 10x worse than target

**Tool 2: cpu-tracer** (execution counts)
```
queens: 5,200 interpreted (95.7%), 232 compiled (4.3%)
```
- Target: >95% compiled for hot functions
- Confirms low compilation effectiveness

**Tool 3: trace-compilation** (compilation events)
```
[T1] opt done queens (tier 1, basic optimization)
[T2] opt bailout queens (tier 2 compilation FAILED)
```
- Root cause identified: T2 compilation failing

**Conclusion**: Critical compilation issue - T2 bailout prevents optimization

**Saved Outputs**:
- tool-outputs/cpu-sampler-queens.txt
- tool-outputs/cpu-tracer-queens.txt
- tool-outputs/trace-compilation-queens.txt

---

### Theory 2: "CallTarget not cached in CallNode"
- **Status**: ✅ VERIFIED
- **Tools Used**: trace-performance-warnings, cpu-sampler (2/2 tools)
- **Priority**: P1 (Critical)

**Evidence from Multiple Tools**:

**Tool 1: trace-performance-warnings** (virtual calls)
```
[engine] perf warn queens |Partial evaluation could not inline the virtual runtime call Virtual to HotSpotMethod<CallTarget.call(Object[])>

Approximated stack trace:
  at CallNode.java:42
```
- Direct evidence of virtual call at exact location predicted by theory
- Prevents optimization and likely causes T2 bailout from Theory 1

**Tool 2: cpu-sampler** (impact assessment)
```
queens || 1850ms 88.0%  (88% of total execution time)
```
- Virtual call occurs in function consuming 88% of runtime
- High impact on hot path

**Conclusion**: Missing CallTarget caching causes virtual calls in hot path, preventing T2 compilation

**Saved Outputs**:
- tool-outputs/trace-performance-warnings-queens.txt
- tool-outputs/cpu-sampler-queens.txt (from Theory 1)

---

### Theory 3: "ConditionProfile not cached causing virtual calls"
- **Status**: ⚠️ PARTIALLY VERIFIED
- **Tools Used**: trace-performance-warnings (verified), trace-inlining (inconclusive) - 1/2 tools
- **Priority**: P1 (Critical)

**Evidence from Multiple Tools**:

**Tool 1: trace-performance-warnings** (virtual calls) - ✅ VERIFIED
```
[engine] perf warn queens |Partial evaluation could not inline the virtual runtime call Virtual to HotSpotMethod<ConditionProfile.profile(boolean)>

Approximated stack trace:
  at IfNode.java:28
```
- Direct evidence of ConditionProfile virtual call
- Confirms theory prediction

**Tool 2: trace-inlining** (inlining failures) - ⚠️ INCONCLUSIVE
- Expected: Inlining cutoff or failure messages
- Actual: No output for queens function
- Diagnosis: queens never reaches T2 compilation (from Theory 1)
- Conclusion: Cannot verify inlining impact until compilation succeeds

**Conclusion**: Theory confirmed by trace-performance-warnings; second tool inconclusive due to prerequisite compilation failure

**Follow-up Required**: Re-test trace-inlining after fixing Theory 1 & 2

**Saved Outputs**:
- tool-outputs/trace-performance-warnings-queens.txt (same as Theory 2)
- tool-outputs/trace-inlining-queens.txt (inconclusive - documented)

---

### Theory 4: "Inlining budget exhausted for recursive calls"
- **Status**: ⚠️ INCONCLUSIVE
- **Tools Used**: trace-inlining (inconclusive), cpu-tracer (inconclusive) - 0/2 tools verified
- **Priority**: P2 (Medium)

**Evidence from Multiple Tools**:

**Tool 1: trace-inlining** - ⚠️ INCONCLUSIVE
- Expected: Cutoff states or budget messages
- Actual: No output for queens function
- Diagnosis: Requires T2 compilation which is currently failing
- Conclusion: Cannot verify without successful compilation

**Tool 2: cpu-tracer** - ⚠️ INCONCLUSIVE
- Expected: Call depth metrics
- Actual: Tool doesn't show call depth/recursion metrics
- Conclusion: Wrong tool for measuring this aspect

**Alternative Approaches Considered**:
- Compiler graph analysis (requires bgv2json/seafoam setup)
- Manual code inspection (not sufficient for verification per methodology)

**Conclusion**: Cannot verify theory with available tools until compilation works

**Follow-up Required**:
1. Fix Theory 1, 2, 3 to enable T2 compilation
2. Re-run trace-inlining to check for budget exhaustion
3. Consider compiler graph analysis if still inconclusive

**Saved Outputs**:
- tool-outputs/trace-inlining-queens.txt (inconclusive - documented)
- tool-outputs/cpu-tracer-queens.txt (from Theory 1)

---

## Inconclusive Theories Summary

**Theory 4** could not be fully verified due to prerequisite issues:
- **Blocker**: T2 compilation must succeed first
- **Follow-up**: Re-test after Theory 1, 2, 3 issues resolved
- **Alternative Tools**: Compiler graph analysis with bgv2json if trace tools remain inconclusive

---

## Prioritized Issues Summary

### Priority 1: Critical Issues (Blocking Optimization)

**Issue 1: Uncached CallTarget.call() causes virtual calls**

**Location**: CallNode.java:42

**Evidence**: trace-performance-warnings shows virtual call warning at CallNode

**Impact**: Virtual calls in hot path (queens 88% total time), blocks T2 compilation

**Root Cause**: CallTarget retrieved from LoxFunction not cached as compilation constant

**Category**: Implementation issue

---

**Issue 2: Uncached ConditionProfile causes virtual calls**

**Evidence**: trace-performance-warnings shows ConditionProfile.profile() virtual calls

**Impact**: Virtual calls in conditional statements throughout codebase

**Root Cause**: ConditionProfile not stored as compilation constant

**Category**: Implementation issue

**Status**: Partially verified (1/2 tools confirm, trace-inlining inconclusive until T2 works)

---

### Example Code Showing Issue (for reference):

**Problematic Pattern - CallTarget not cached:**
```java
@Specialization
public Object call(VirtualFrame frame, Object function) {
    CallTarget target = ((LoxFunction) function).getCallTarget();
    return target.call(args);  // ← Virtual call warning at this location
}
```
- CallTarget retrieved from function is not a compilation constant
- Results in virtual call that blocks T2 compilation

**Problematic Pattern - ConditionProfile not cached:**
```java
private ConditionProfile profile = ConditionProfile.createBinaryProfile();

public Object execute(VirtualFrame frame) {
    if (profile.profile(condition)) { ... }  // ← Virtual call to profile.profile()
}
```
- Profile stored in instance field, not as @Cached compilation constant
- Results in virtual calls on every conditional check

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

**Theory Verification Results**: 4 theories tested
- ✅ **2 Verified**: Theory 1 (3/3 tools), Theory 2 (2/2 tools)
- ⚠️ **1 Partially Verified**: Theory 3 (1/2 tools verified, 1 inconclusive)
- ⚠️ **1 Inconclusive**: Theory 4 (0/2 tools - blocked by compilation failure)

**Multi-Tool Verification Summary**:
- **Theory 1**: cpu-sampler + cpu-tracer + trace-compilation → All confirm T2 bailout
- **Theory 2**: trace-performance-warnings + cpu-sampler → Both confirm CallTarget virtual calls
- **Theory 3**: trace-performance-warnings (✅) + trace-inlining (⚠️ blocked by T2 failure)
- **Theory 4**: All tools inconclusive due to prerequisite issues

**Critical Findings**:
1. **Root Cause**: T2 compilation failing (bailout)
2. **Trigger**: Virtual calls from missing @Cached on CallTarget and ConditionProfile
3. **Impact**: 95.2% interpreter execution (target: <10%), 1.7% T2 (target: >80%)
4. **Severity**: 10x worse than target metrics

**Expected Improvement**: 5-10x speedup after implementing Priority 1 fixes

**Inconclusive Theories**:
- Theory 4 deferred until compilation succeeds (cannot test inlining without T2)

**Next Steps**:
1. Implement Priority 1 fixes (CallTarget + ConditionProfile caching)
2. Re-run benchmarks to measure improvement
3. Re-test inconclusive theories (Theory 3 trace-inlining, Theory 4)
4. If still slow after fixes: Investigate remaining issues with fresh analysis
```

---

## Tool Output Examples

### cpu-sampler Output

```
----------------------------------------------------------------------------------------------
Sampling Histogram. Recorded 412 samples with period 10ms.
  Self Time: Time spent on the top of the stack.
  Total Time: Time spent somewhere on the stack.
  T0: Percent of time spent in interpreter.
  T1: Percent of time spent in code compiled by tier 1 compiler.
  T2: Percent of time spent in code compiled by tier 2 compiler.
----------------------------------------------------------------------------------------------
Thread[main,5,main]
Name         || Total Time      || Self Time       || T0     | T1    | T2    || Location
----------------------------------------------------------------------------------------------
queens       || 1850ms  88.0%   || 1850ms  88.0%   || 95.2% | 3.1%  | 1.7%  || queens.lox~15-45
hasConflict  ||  250ms  11.9%   ||  250ms  11.9%   || 92.8% | 4.2%  | 3.0%  || queens.lox~8-14
:program     ||   12ms   0.6%   ||   12ms   0.6%   ||100.0% | 0.0%  | 0.0%  || queens.lox~1-50
----------------------------------------------------------------------------------------------
```

### trace-performance-warnings Output

```
[engine] perf warn queens |Partial evaluation could not inline the virtual runtime call Virtual to HotSpotMethod<CallTarget.call(Object[])> (167|MethodCallTarget).

Approximated stack trace for [167 | MethodCallTarget]:
  at de.hpi.swa.lox.nodes.CallNode.execute(CallNode.java:42)
  at de.hpi.swa.lox.bytecode.LoxBytecodeRootNode.execute(LoxBytecodeRootNode.java:156)

[engine] perf warn queens |Partial evaluation could not inline the virtual runtime call Virtual to HotSpotMethod<ConditionProfile.profile(boolean)> (234|MethodCallTarget).

Approximated stack trace for [234 | MethodCallTarget]:
  at de.hpi.swa.lox.nodes.IfNode.execute(IfNode.java:28)
```

### trace-inlining Output

```
[engine] inline start queens |IR Nodes 4500 |Truffle Callees 2 |Depth 0
[engine] Inlined hasConflict |call diff -1.00 |IR Nodes 800 |Depth 1
[engine] Inlined placeQueen |call diff -1.00 |IR Nodes 600 |Depth 1
[engine] inline done queens |IR Nodes 4500 |graal 3500 |CodeSize 2800
```
