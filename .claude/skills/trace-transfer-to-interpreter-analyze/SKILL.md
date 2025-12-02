---
name: Run and Analyze Transfer To Interpreter Tracer
description: Traces deoptimization events where execution falls back from compiled code to interpreter. Use to detect deoptimization loops (same location repeatedly), unstable assumptions causing recompilation, and type instability. Zero transfers = stable compilation (ideal). Many transfers = severe performance problem. Critical for diagnosing compilation instability.
---

# Skill: Run and Analyze Transfer To Interpreter Tracer

This skill runs the Transfer to Interpreter tracing tool on your language implementation and provides detailed analysis of deoptimization events to help identify and eliminate compilation instability.

## What This Skill Does

1. **Runs Deoptimization Tracer**: Executes the program with transfer-to-interpreter tracing enabled
2. **Analyzes Deoptimization Patterns**: Interprets the trace output to identify:
   - Deoptimization loops (same location repeatedly)
   - Unstable type assumptions
   - Object shape instabilities
   - Premature compilation issues
3. **Provides Critical Recommendations**: Suggests specific fixes to eliminate deoptimizations

## Why Deoptimizations Are Catastrophic

**Deoptimizations are extraordinarily expensive** because:
- **Single transfer cost**: Hundreds of nanoseconds (vs. ~5ns for normal call)
- **Repeated deoptimization cycles**: 10-100x slower than pure interpreter!
- **Compilation wasted work**: Compile → deopt → recompile → deopt = disaster
- **GraalVM 25+ bailout**: After threshold violations, permanently gives up on optimization

**Goal: Zero transfers in steady-state execution!**

## When to Use This Skill

- Investigate poor performance despite successful compilation
- Profile shows methods spending excessive time in interpreted mode
- Validate optimizations didn't introduce deoptimization cycles
- Debug why compiled code keeps falling back to interpreter
- Verify benchmarks have properly warmed up before measurement

## Prerequisites

Before running this skill, you should know:
- **Required**: Having benchmark baseline data for comparison
- The path to the program to analyze
- Whether you want to focus on specific functions (reduces output)
- Approximate warmup time for the program

## Fermi Verification: The Sanity Gate (MANDATORY)

**Principle:**  
The Tool Output is the highest authority for *data*, but your Fermi Estimate is the highest authority for *pipeline integrity*.

**The Logic:**
- **Small Deviation:** Tool works correctly. Update your mental model.
- **Massive Deviation (>1 Order of Magnitude):** Tool is likely **malfunctioning** (silent failure, misconfiguration, or wrong target).

**Protocol:**

### Step 1: Pre-Calculation
- In a scratchpad, estimate the expected output magnitude (e.g., "For a program with 3 hot functions, I expect 0-50 transfers during warmup (as the system profiles types and specializes nodes), then zero transfers in steady-state").
- *Key:* You must write this down *before* generating the tool command.

### Step 2: Smoke Test (The Probe)
- Run on trivial input first to prove the tool *can* work.

### Step 3: Execute & Validate
- Run the actual command.
- **Credibility Threshold Check:** Compare Output vs. Estimate.
  - **Scenario A (Within 1 Order of Magnitude):** **ACCEPT.** The tool is the authority. Proceed with this result.
  - **Scenario B (>1 Order of Magnitude Divergence OR Unexpected Zero):** **REJECT & DIAGNOSE.**
    - **STOP.** Do not use this result for the next step.
    - **Hypothesis:** The tool failed silently, the path is wrong, or permissions are denied.
    - **Action:** Run a *Debug Command* (e.g., `ls -l target_file` to check size, or `echo $?` to check exit code) to prove the tool is healthy.
    - *Only* after the tool's health is proven via a secondary check may you accept the divergent result.

## How the Skill Works

In all examples, `<launcher>` refers to your programming language launcher script.

The skill follows this workflow:

### 1. Initial Setup
- Confirms the program path and arguments
- Optionally sets up function filtering with `--engine.CompileOnly`
- Determines output redirection strategy

### 2. Run Transfer Tracer

#### Basic Trace (Recommended)
```bash
<launcher> --experimental-options \
  --engine.TraceTransferToInterpreter \
  <program> [script args]
```
- Traces all deoptimization events
- Outputs to stderr by default
- Shows guest language stack traces

#### Focused Trace (For Specific Functions)
```bash
<launcher> --experimental-options \
  --engine.TraceTransferToInterpreter \
  --engine.CompileOnly=functionName \
  <program> [script args]
```
- Restricts compilation to specific function
- Dramatically reduces output volume
- Ideal for debugging specific issues

#### Combined with Compilation Trace (Highly Recommended)
```bash
<launcher> --experimental-options \
  --engine.TraceTransferToInterpreter \
  --engine.TraceCompilation \
  <program> [script args] 2>&1 | tee transfers.log
```
- Shows both compilation events and transfers
- Essential for understanding deoptimization cycles
- Redirects stderr+stdout to file and console

### 3. Understand the Output

#### Transfer Event Structure
```
[engine] transferToInterpreter at
problemFunction(<source>:42:123-456)
callerFunction(<source>:18:78-90)
topLevelFunction(<source>:5:10-50)
```

**Key information**:
- **Location**: `problemFunction(<source>:42:123-456)` - Exact source location
- **Stack trace**: Shows call chain leading to transfer
- **Timing**: Appears immediately when transfer occurs

### 4. Identify Critical Patterns

#### Pattern 1: Deoptimization Loop (CRITICAL!)
```
[engine] transferToInterpreter at MyNode.execute(<source>:42)
[engine] transferToInterpreter at MyNode.execute(<source>:42)
[engine] transferToInterpreter at MyNode.execute(<source>:42)
[engine] transferToInterpreter at MyNode.execute(<source>:42)
...dozens or hundreds of times...
```

**🚨 This is catastrophic for performance!**

**Problem**: Same location appearing repeatedly

**Symptoms**:
- Identical stack trace appearing dozens/hundreds of times
- Often same line number in same function
- Occurs during supposedly "hot" execution

**Root Cause**: Unstable type assumptions or polymorphic behavior
- Node compiles with type assumptions (e.g., "this is always an integer")
- Encounters value violating assumption (e.g., receives a double)
- Deoptimizes and invalidates
- Recompiles with new assumptions
- Cycle repeats if code continues seeing different types

**Why It's Catastrophic**:
- Often **worse than never compiling at all**
- Wastes compilation time repeatedly
- Execution constantly switching between compiled/interpreted
- Performance can degrade 10-100x

**Resolution**:
1. **Implement proper type specializations** (if writing Truffle implementation):
   ```java
   @Specialization
   int doIntegers(int a, int b) { return a + b; }

   @Specialization
   double doDoubles(double a, double b) { return a + b; }

   @Specialization
   Object doGeneric(Object a, Object b) { /* fallback */ }
   ```

2. **For guest language code**: Avoid mixing types in hot paths
   ```<your-language>
   // ❌ BAD: Mixing types in hot loop
   for (var i = 0; i < 1000; i = i + 1) {
     var x = someFunction(i);  // Returns int sometimes, double other times
     process(x);
   }

   // ✅ GOOD: Consistent types
   for (var i = 0; i < 1000; i = i + 1) {
     var x = someFunction(i);  // Always returns same type
     process(x);
   }
   ```

3. **Tune compilation thresholds** (allow more profiling):
   ```bash
   <launcher> --experimental-options \
     --engine.FirstTierCompilationThreshold=1000 \
     --engine.TraceTransferToInterpreter \
     <program> [script args]
   ```

**Verification**:
```bash
# Before fix: Count transfers
<launcher> --experimental-options \
  --engine.TraceTransferToInterpreter \
  <program> [script args] 2>&1 | grep -c "transferToInterpreter"

# After fix: Should be zero or very few
```

#### Pattern 2: Property Access Deoptimizations
```
[engine] transferToInterpreter at
getProperty(<source>:123)
...
PropertyCacheNode.deoptimize(...)
PropertyGetNode.getValueOrDefault(...)
```

**Problem**: Unstable object shapes causing cache invalidations

**Symptoms**:
- Transfers in property access code
- Stack traces showing cache-related nodes
- Occurs when accessing object properties

**Root Cause**: Objects changing shape after compilation
- Objects add/remove properties dynamically
- Property types change (int → double)
- Compiler caches property locations assuming stable shapes
- Shape changes invalidate these assumptions

**Common Causes**:
```<your-language>
// ❌ BAD: Adding properties dynamically
class Point {
  init(x, y) {
    self.x = x;
    self.y = y;
  }
}

var p = Point(1, 2);
// Later... adding new property dynamically
p.z = 3;  // Shape change! Deoptimizes!
```

**Resolution**:
```<your-language>
// ✅ GOOD: All properties in constructor
class Point {
  init(x, y, z) {
    self.x = x;
    self.y = y;
    self.z = z;  // Define all properties upfront
  }
}

var p = Point(1, 2, 0);
// Later... just modify existing property
p.z = 3;  // No shape change, stays optimized
```

**Verification Tools**:
```bash
# Combine with assumption tracing
<launcher> --experimental-options \
  --engine.TraceTransferToInterpreter \
  --engine.TraceAssumptions \
  <program> [script args] 2>&1 | tee analysis.log
```

#### Pattern 3: Warmup Transfers (Normal, But Monitor)
```
[First 5 seconds of execution]
[engine] transferToInterpreter at function1(...)
[engine] transferToInterpreter at function2(...)
[engine] transferToInterpreter at function3(...)
...many transfers...

[After 10+ seconds of execution]
[No more transfers]  ✅ Good!
```

**Problem**: Many transfers during warmup, then stabilizes

**Symptoms**:
- Hundreds of transfers in first seconds
- Gradually decreasing over time
- Eventually stops or becomes rare

**Root Cause**: Normal profiling and specialization process
- System profiles types during interpreter execution
- Compiles with initial assumptions
- Specializes nodes based on observed types
- Eventually stabilizes when all types seen

**When It's Acceptable**:
- ✅ Transfers decrease over time
- ✅ Stops after reasonable warmup (10-20 seconds)
- ✅ Doesn't resume during steady-state execution

**When It's Problematic**:
- ❌ Continues indefinitely (deoptimization loop)
- ❌ Takes very long to stabilize (>60 seconds)
- ❌ Resumes after appearing to stabilize

**Resolution for Excessive Warmup**:
```bash
# Increase first-tier threshold for more profiling
<launcher> --experimental-options \
  --engine.FirstTierCompilationThreshold=800 \
  --engine.TraceTransferToInterpreter \
  <program> [script args]
```

**Verification**:
```bash
# Skip warmup in profiling
<launcher> --cpusampler --cpusampler.Delay=10000 \
  --cpusampler.ShowTiers=true \
  <program> [script args]
```

#### Pattern 4: Rare Path Transfers (Acceptable)
```
[During entire execution]
[engine] transferToInterpreter at errorHandler(<source>:250)
[engine] transferToInterpreter at validateInput(<source>:89)
```

**Problem**: Occasional transfers from uncommon paths

**Symptoms**:
- Infrequent transfers (single digits per run)
- From error handling or validation code
- Don't repeat at same location

**Root Cause**: Not actually a problem!
- Truffle uses deoptimization for uncommon paths
- Better than complicating compiled code
- Error handling, edge case validation appropriate for interpreter

**When It's Acceptable**:
- ✅ Transfers are infrequent (single digits)
- ✅ From genuinely uncommon paths (error handling, validation)
- ✅ Don't impact overall performance

**When It's Problematic**:
- ❌ "Rare" paths executing frequently (design issue)
- ❌ Many different rare paths (code structure issue)

**Resolution**:
- If truly rare: No action needed
- If actually frequent: Reconsider code design

**Verification**:
```bash
# Check if these paths are actually hot
<launcher> --cpusampler <program> [script args]
```

### 5. Correlation with Compilation Trace

**Essential combination** - always use together!

```bash
<launcher> --experimental-options \
  --engine.TraceTransferToInterpreter \
  --engine.TraceCompilation \
  <program> [script args] 2>&1 | tee full-trace.log
```

**Look for this pattern (deoptimization cycle)**:
```
[engine] opt done    myFunction              <-- Compiled!
[engine] transferToInterpreter at myFunction <-- Deoptimized!
[engine] opt deopt   myFunction              <-- Invalidated!
[engine] opt done    myFunction              <-- Recompiled!
[engine] transferToInterpreter at myFunction <-- Deoptimized again!
[engine] opt deopt   myFunction              <-- Invalidated again!
...repeats...                                 <-- 🚨 DISASTER!
```

**What to look for**:
- **Stable compilation** (good):
  ```
  [engine] opt done    myFunction
  [No transfers for this function]
  ```

- **Deoptimization cycle** (catastrophic):
  ```
  [engine] opt done    myFunction
  [engine] transferToInterpreter at myFunction
  [engine] opt deopt   myFunction
  [Repeats...]
  ```

### 6. Analysis Metrics

The skill tracks these key metrics:

#### Total Transfer Count
```bash
# Count all transfers
grep -c "transferToInterpreter" transfers.log
```
- **0 transfers**: Perfect! ✅
- **1-10 transfers**: Acceptable (likely warmup or rare paths)
- **10-100 transfers**: Investigate - may have issues
- **100+ transfers**: Critical problem! 🚨

#### Unique Transfer Locations
```bash
# Count unique locations
grep "transferToInterpreter at" transfers.log | sort -u | wc -l
```
- **Few unique locations**: Likely deoptimization loop
- **Many unique locations**: Widespread instability or warmup

#### Transfers Over Time
- **Early execution**: High count acceptable (warmup)
- **Steady-state** (after 10+ seconds): Should be zero

## Best Practices

The skill follows these analysis best practices:

### 1. Always Combine with TraceCompilation
```bash
<launcher> --experimental-options \
  --engine.TraceTransferToInterpreter \
  --engine.TraceCompilation \
  <program> [script args] 2>&1 | tee analysis.log
```
**Why**: See compilation lifecycle with deoptimization events

### 2. Filter to Specific Functions for Debugging
```bash
<launcher> --experimental-options \
  --engine.TraceTransferToInterpreter \
  --engine.CompileOnly=problematicFunction \
  <program> [script args] 2>&1
```
**Why**: Reduce output volume for focused analysis

### 3. Redirect Output to File
```bash
<launcher> --experimental-options \
  --engine.TraceTransferToInterpreter \
  <program> [script args] 2>transfers.log
```
**Why**: Analyze offline, compare across runs, count patterns

### 4. Distinguish Warmup from Steady-State
```bash
# Run long enough to see pattern
<launcher> --experimental-options \
  --engine.TraceTransferToInterpreter \
  long-running-<program> 2>&1 | \
  awk '{print NR, $0}' > numbered-output.log
```
**Why**: Only steady-state transfers are real problems

### 5. Count Transfer Frequency
```bash
# Count transfers per location
grep "transferToInterpreter at" transfers.log | \
  sort | uniq -c | sort -rn
```
**Why**: Identify deoptimization loops (high counts at same location)

### 6. Use Deterministic Compilation for Debugging
```bash
<launcher> --experimental-options \
  --engine.TraceTransferToInterpreter \
  --engine.CompileImmediately \
  --engine.BackgroundCompilation=false \
  <program> [script args] 2>&1
```
**Why**: Makes behavior deterministic, avoids race conditions

## Correlation with Other Tools

### Transfer Tracer + Compilation Trace (Essential!)
```bash
<launcher> --experimental-options \
  --engine.TraceTransferToInterpreter \
  --engine.TraceCompilation \
  <program> [script args] 2>&1 | tee full-trace.log
```
**Why**:
- TraceCompilation: When/what compiled, invalidations
- TraceTransferToInterpreter: Where/why deoptimizations
- Together: Complete deoptimization cycle picture

### Transfer Tracer + Assumption Trace
```bash
<launcher> --experimental-options \
  --engine.TraceTransferToInterpreter \
  --engine.TraceAssumptions \
  <program> [script args] 2>&1 | tee assumptions.log
```
**Why**:
- TraceAssumptions: Which specific assumptions invalidating
- TraceTransferToInterpreter: Where deoptimizations occur
- Together: Understand assumption instability

### Transfer Tracer + CPU Sampler
```bash
# Step 1: Identify deoptimization issues
<launcher> --experimental-options \
  --engine.TraceTransferToInterpreter \
  <program> [script args] 2>transfers.log

# Step 2: Profile to see impact
<launcher> --cpusampler --cpusampler.ShowTiers=true \
  --cpusampler.Delay=10000 \
  <program> [script args]
```
**Why**:
- CPU Sampler: Shows time in interpreted vs compiled
- Transfer Tracer: Shows why code is interpreted
- Together: Quantify deoptimization performance impact

### Transfer Tracer + Compilation Statistics
```bash
<launcher> --experimental-options \
  --engine.TraceTransferToInterpreter \
  --engine.CompilationStatistics \
  <program> [script args] 2>&1
```
**Why**:
- CompilationStatistics: Aggregate invalidation counts/rates
- TraceTransferToInterpreter: Individual deoptimization events
- Together: Quantify overall deoptimization problem

## Advanced Options

### Increase Stack Trace Depth
```bash
<launcher> --experimental-options \
  --engine.TraceTransferToInterpreter \
  --engine.TraceStackTraceLimit=50 \
  <program> [script args] 2>&1
```
- Default: 20 frames
- Increase for deep call chains
- More frames = more output

### Immediate Compilation (Testing)
```bash
<launcher> --experimental-options \
  --engine.TraceTransferToInterpreter \
  --engine.CompileImmediately \
  <program> [script args] 2>&1
```
- Compiles as soon as methods run
- Useful for triggering issues quickly
- Makes behavior deterministic

### Synchronous Compilation (Debugging)
```bash
<launcher> --experimental-options \
  --engine.TraceTransferToInterpreter \
  --engine.BackgroundCompilation=false \
  <program> [script args] 2>&1
```
- Disables background compilation
- Makes timing deterministic
- Simplifies debugging

## Common Pitfalls to Avoid

The skill warns about these mistakes:

- ❌ **Panicking at warmup transfers**:
  - Many transfers during first 10-20 seconds is normal
  - Only steady-state transfers are problems

- ❌ **Not correlating with compilation trace**:
  - Missing crucial context of compile/deopt cycles
  - Can't understand full picture without compilation events

- ❌ **Ignoring deoptimization loops**:
  - Same location repeatedly = catastrophic problem
  - Must fix immediately (often most impactful optimization)

- ❌ **Treating all transfers as equal**:
  - Rare path transfers (error handling) are acceptable
  - Hot path transfers are critical problems
  - Use profiling to distinguish

- ❌ **Forgetting experimental-options flag**:
  - Tool silently doesn't work without it
  - Always include `--experimental-options`

- ❌ **Not saving output for analysis**:
  - Output can be overwhelming in console
  - Always redirect to file: `2>transfers.log`

## Typical Workflow

The skill typically follows this analysis workflow:

### Step 1: Initial Trace
```bash
<launcher> --experimental-options \
  --engine.TraceTransferToInterpreter \
  --engine.TraceCompilation \
  <program> [script args] 2>&1 | tee initial-trace.log
```

### Step 2: Count and Categorize Transfers
```bash
# Total transfers
grep -c "transferToInterpreter" initial-trace.log

# Transfers per location
grep "transferToInterpreter at" initial-trace.log | \
  sort | uniq -c | sort -rn > transfer-counts.txt

# Look for high counts (deoptimization loops)
head -20 transfer-counts.txt
```

### Step 3: Identify Deoptimization Loops
```bash
# Locations with 10+ transfers
awk '$1 >= 10' transfer-counts.txt
```
**If found**: Critical problem! Fix immediately.

### Step 4: Analyze Compilation Cycles
```bash
# Extract compilation and deopt events for specific function
grep "myFunction" initial-trace.log | \
  grep -E "opt done|opt deopt|transferToInterpreter"
```
**Look for**: Compile → transfer → deopt → recompile pattern

### Step 5: Fix Issues
- **Deoptimization loops**: Fix type stability, add specializations
- **Property access**: Stabilize object shapes
- **Excessive warmup**: Increase compilation thresholds

### Step 6: Verify Fixes
```bash
# Re-run trace
<launcher> --experimental-options \
  --engine.TraceTransferToInterpreter \
  <program> [script args] 2>&1 | tee fixed-trace.log

# Count transfers
grep -c "transferToInterpreter" fixed-trace.log

# Goal: Zero or single-digit count
```

### Step 7: Measure Performance Impact
```bash
# Before fix
time <launcher> <program> [script args]

# After fix (should be much faster)
time <launcher> <program> [script args]
```

## Success Criteria

**Ideal outcome**:
- ✅ Zero transfers in steady-state execution (after warmup)
- ✅ Only single-digit transfers during warmup
- ✅ No deoptimization loops
- ✅ All transfers from rare/error paths (if any)

**Acceptable outcome**:
- ✅ <10 transfers total
- ✅ No repeated transfers at same location
- ✅ All transfers during warmup or from rare paths
- ✅ CPU Sampler shows >95% compiled time for hot functions

**Critical problems**:
- 🚨 Same location appearing repeatedly (deoptimization loop)
- 🚨 100+ transfers
- 🚨 Transfers continuing indefinitely in steady-state
- 🚨 CPU Sampler shows high interpreter time

## Example Analysis

### Example Output
```
[engine] opt done    sieveFunction  <tier1>
[engine] opt done    isPrimeHelper  <tier1>
[engine] transferToInterpreter at isPrimeHelper(<source>:42)
[engine] transferToInterpreter at isPrimeHelper(<source>:42)
[engine] transferToInterpreter at isPrimeHelper(<source>:42)
[engine] opt deopt   isPrimeHelper
[engine] opt done    isPrimeHelper  <tier1>
[engine] transferToInterpreter at isPrimeHelper(<source>:42)
```

### Skill Analysis

1. **Pattern identified**: Deoptimization loop! 🚨
   - `isPrimeHelper` at line 42 repeatedly deoptimizing
   - Compile → transfer → deopt → recompile cycle

2. **Root cause**: Type instability
   - Line 42 likely receives mixed types (int/double)
   - Compilation assumes one type, encounters another

3. **Performance impact**: Catastrophic
   - Function likely hot (in prime checking)
   - Repeated deopt worse than no compilation

4. **Recommended fix**:
   ```<your-language>
   // Check line 42 - likely something like:
   fun isPrimeHelper(n, divisor) {
     if (divisor * divisor > n) return true;  // Line 42
     ...
   }

   // Ensure n and divisor are always same type
   // Don't mix Number(n) calls with integer literals
   ```

5. **Verification**:
   - Fix type consistency
   - Re-run trace
   - Confirm zero transfers at line 42

## Reference Documentation

For detailed information, see:
- Official GraalVM docs: https://www.graalvm.org/latest/graalvm-as-a-platform/language-implementation-framework/Optimizing/
- CompilerDirectives API: https://www.graalvm.org/truffle/javadoc/com/oracle/truffle/api/CompilerDirectives.html
- Use Graal Truffle Docs skill

## Implementation Notes

This skill:
- Uses your language's launcher: `<launcher>`
- Requires `--experimental-options` flag
- Outputs to stderr (redirect with `2>`)
- Combines with TraceCompilation for full picture
- Focuses on deoptimization loops (critical issues)
- Distinguishes warmup vs steady-state transfers
- Provides line-number references for investigation
- Emphasizes goal: **zero steady-state transfers**
- Combined with other performance analysis skills for full picture

## Related Skills

- Use Graal Truffle Docs skill to understand Truffle APIs and options
- Use CPU Sampler Analyze skill for initial profiling to identify hot functions
- Use Performance Warnings Analyze skill to find optimization barriers
- Use Compilation Trace Analyze skill to see inlining and compilation decisions
- Use CPU Tracer Analyze skill for execution frequency insights
- Use Memory Tracer Analyze skill for allocation profiling
- Use Trace Inlining Analyze skill for inlining decision analysis
- Use Benchmark Baseline skill for creating performance baselines with different benchmarks
