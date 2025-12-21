# Performance Analysis Examples

Complete usage scenarios and report templates.

---

## Example Usage Scenario

**User**: "Analyze the performance of my Lox implementation. I've run benchmarks and have BENCHMARK_BASELINE.md."

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

### Phase 3: Verify Theories

**Theory 1**: "Hot functions have low compilation effectiveness"

```
Step 3a: Load Documentation
→ Reading .claude/skills/performance-analysis/CPUSampler.md
→ Retrieved: Command syntax, tier meanings, targets

Step 3b: Fermi Verification
→ Pre-calculation: Expect 1-5 functions in output
→ Smoke test: ./lox --cpusampler trivial.lox → ✓ Works
→ Execute: ./lox --cpusampler --cpusampler.ShowTiers=true --cpusampler.Delay=2000 queens.lox
→ Output: 3 functions shown
→ Validation: 3 is within 1 order of magnitude of 1-5 ✓
→ Credibility: ACCEPT

Step 3c: Analyze Results
→ Parsing output...
→ queens: 95.2% T0, 1.7% T2
→ Target: <10% T0, >80% T2
→ Gap: 10x worse than target
→ Verdict: ✅ VERIFIED

Step 3d: Record Findings
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

### Phase 4: Generate Report

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

### Final Summary

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

## Example Report Template

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
