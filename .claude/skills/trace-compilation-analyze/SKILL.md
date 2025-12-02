---
name: Run and Analyze Compilation Tracer
description: Logs every compilation event with timing, tier (T1/T2), success/failure, and invalidation reasons. Use to verify hot code is compiling, diagnose compilation failures/bailouts, track recompilation cycles, and understand tiered compilation behavior. Shows WHEN compilation happens. Essential for understanding compilation lifecycle.
---

# Skill: Run and Analyze Compilation Tracer

This skill runs the Trace Compilation tool on your language implementation and provides detailed analysis of compilation events to help verify optimization effectiveness and diagnose compilation issues.

## What This Skill Does

1. **Runs Compilation Tracer**: Executes the program with compilation tracing enabled
2. **Analyzes Compilation Events**: Interprets the output to identify:
   - Which methods are being compiled and when
   - Compilation tiers (T1 fast vs T2 optimized)
   - Deoptimization and invalidation cycles
   - Compilation timing and queue behavior
   - Inlining effectiveness
3. **Provides Diagnostic Recommendations**: Suggests fixes for compilation issues

## Understanding Tiered Compilation

**GraalVM uses multi-tier compilation** by default:

- **Tier 1** (T1): Fast compilation with basic optimizations
  - Threshold: 400 invocations (default)
  - Purpose: Get code compiled quickly
  - Trade-off: Faster warmup, lower code quality

- **Tier 2** (T2): Full optimization with aggressive partial evaluation
  - Threshold: 10,000 invocations (default)
  - Purpose: Optimize truly hot code heavily
  - Trade-off: Longer compilation, higher code quality

**Strategy**: Balance startup (T1) vs peak performance (T2)

## When to Use This Skill

- Verify hot code is actually being compiled
- Diagnose why performance is poor despite code being "hot"
- Track deoptimization and invalidation cycles
- Understand compilation timing and queue behavior
- Validate that expected optimizations occur
- Investigate compilation failures or bailouts
- Correlate compilation with profiling data

## Prerequisites

Before running this skill, you should know:
- **Required**: Having benchmark baseline data for comparison
- The path to the program to analyze
- Ideally, CPU profiling results showing hot functions
- Whether you want basic or detailed queue analysis

## Fermi Verification: The Sanity Gate (MANDATORY)

**Principle:**  
The Tool Output is the highest authority for *data*, but your Fermi Estimate is the highest authority for *pipeline integrity*.

**The Logic:**
- **Small Deviation:** Tool works correctly. Update your mental model.
- **Massive Deviation (>1 Order of Magnitude):** Tool is likely **malfunctioning** (silent failure, misconfiguration, or wrong target).

**Protocol:**

### Step 1: Pre-Calculation
- In a scratchpad, estimate the expected output magnitude (e.g., "This has 5 hot functions, I expect 5-15 compilation events (including T1 and T2)").
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
- Determines whether detailed queue analysis needed
- Optionally sets up function filtering

### 2. Run Compilation Tracer

⚠️ WARNING: Output Volume Tracing generates massive text. Always redirect to file and inspect slices of the data.

#### Basic Compilation Trace (Recommended)
```bash
<launcher> --experimental-options \
  --engine.TraceCompilation \
  <program> [script args]
```
- Shows compilation completion events
- Includes deoptimizations and invalidations
- Moderate output volume

#### Detailed Queue Analysis
```bash
<launcher> --experimental-options \
  --engine.TraceCompilation \
  --engine.TraceCompilationDetails \
  <program> [script args]
```
- Adds queue events (queued, start, unqueued)
- Shows queue size, load, and timing
- HIGH output volume - use for queue debugging only

#### Focused on Specific Function
```bash
<launcher> --experimental-options \
  --engine.TraceCompilation \
  --engine.CompileOnly="*functionName*" \
  <program> [script args]
```
- Restricts compilation to specific method
- Dramatically reduces output volume
- Pattern matching
- Ideal for investigating specific issues

#### Synchronous Compilation (Debugging)
```bash
<launcher> --experimental-options \
  --engine.TraceCompilation \
  --engine.BackgroundCompilation=false \
  <program> [script args]
```
- Disables background compilation
- Makes output deterministic
- Simplifies correlation between events

#### Redirect Output
```bash
<launcher> --experimental-options \
  --engine.TraceCompilation \
  <program> [script args] > trace.log

# Later analysis
tail -n 50 trace.log
grep "opt deopt" trace.log | tail -n 20  
```
- Saves output to file for later analysis
- Enables searching and filtering

### 3. Understand Output Events

#### Compilation Success (opt done)
```
[engine] opt done id=244 innerLoop |Tier 1|Time 268( 220+47 )ms|AST 17|Inlined 0Y 2N|IR 238/ 437|CodeSize 1874|Src <source>:42
```

**Field Breakdown**:

- **id=244**: Unique identifier for this call target
- **innerLoop**: Function name being compiled
- **Tier 1**: Compilation tier (T1=fast, T2=optimized)
- **Time 268( 220+47 )ms**:
  - Total: 268ms
  - Truffle tier (partial evaluation): 220ms
  - Graal compiler: 47ms
- **AST 17**: 17 non-trivial Truffle nodes (method complexity)
- **Inlined 0Y 2N**:
  - 0 calls inlined successfully (Y = Yes)
  - 2 calls remained as calls (N = No)
- **IR 238/ 437**:
  - 238 nodes after partial evaluation
  - 437 nodes after full compilation
- **CodeSize 1874**: 1,874 bytes of machine code generated
- **Src <source>:42**: Source location

#### Deoptimization Event (opt deopt)
```
[engine] opt deopt innerLoop
```

**Meaning**: Compiled code fell back to interpreter
- **Single occurrence**: May be normal (rare path executed)
- **Repeated occurrence**: CRITICAL PROBLEM (deoptimization loop)

#### Invalidation Event (opt inv.)
```
[engine] opt inv. innerLoop
```

**Meaning**: Compiled code discarded due to assumption violation
- Triggers recompilation with updated assumptions
- Repeated invalidations = type instability

#### Queue Events (with TraceCompilationDetails)
```
[engine] opt queued id=237 processData |Tier 1|Count/Thres 25/ 25|Queue: Size 1 Change +1 Load 0.06|Src <source>:42
```
- **Count/Thres 25/ 25**: Reached threshold (25 invocations)
- **Queue: Size 1**: One compilation in queue
- **Change +1**: Queue size increased by 1
- **Load 0.06**: Queue load (1.0 = normal, >1.0 = overloaded)
- Load = queue growth rate; 1.0 = steady state, >1.0 = queue growing faster than processing

### 4. Identify Critical Patterns

#### Pattern 1: Deoptimization Loop (CRITICAL!)
```
[engine] opt done id=100 hotFunction |Tier 1|...
[engine] opt deopt hotFunction
[engine] opt inv. hotFunction
[engine] opt done id=100 hotFunction |Tier 1|...
[engine] opt deopt hotFunction
[engine] opt deopt hotFunction
[engine] opt deopt hotFunction
```

**🚨 This is catastrophic for performance!**

**Problem**: Compile → deopt → recompile → deopt cycle

**Symptoms**:
- Same function repeatedly in "opt deopt" events
- Pattern: opt done → opt deopt → opt inv → repeat
- May happen dozens/hundreds of times

**Root Cause**: Unstable type assumptions
```<your-language>
fun hotFunction(value) {
  // Compiles assuming value is always int
  // Then encounters double → deoptimizes
  // Recompiles assuming double
  // Then encounters int again → deoptimizes
  return value + 1;
}
```

**Resolution**:
1. **Enable deoptimization tracing**:
   ```bash
   <launcher> --experimental-options \
     --engine.TraceCompilation \
     --engine.TraceTransferToInterpreter \
     <program> [script args] 2>&1 | tee trace.log
   ```

2. **Find exact deoptimization location** in stack traces

3. **Fix type stability**:
   ```<your-language>
   // Ensure consistent types in hot paths
   // Don't mix Number() calls with literals
   // Avoid type instability
   ```

4. **Use assumption tracing**:
   ```bash
   <launcher> --experimental-options \
     --engine.TraceCompilation \
     --engine.TraceAssumptions \
     <program> [script args]
   ```

**Verification**:
```bash
# Count deopt events
grep "opt deopt" trace.log | sort | uniq -c

# Goal: Zero repeated deoptimizations for same function
```

#### Pattern 2: Hot Methods Never Compiling
**Correlation with CPU profiler essential!**

```bash
# CPU Profiler shows:
hotFunction    1500ms  75.0%  T0: 95%  <-- High interpreter time!

# But TraceCompilation shows:
[No compilation events for hotFunction]
```

**Problem**: Method consuming time but not compiling

**Symptoms**:
- CPU profiler shows high T0 (interpreter) percentage
- Method appears hot in profiler
- No "opt done" events in compilation trace

**Root Cause**: Not reaching compilation threshold

**Common Causes**:
- Insufficient invocations (<400 for T1, <10000 for T2)
- Compilation disabled entirely
- Method only executed during warmup

**Resolution**:

1. **Verify compilation enabled**:
   ```bash
   # Should be enabled by default
   <launcher> --experimental-options \
     --engine.Compilation=true \
     --engine.TraceCompilation \
     <program> [script args]
   ```

2. **Check profiler tier information**:
   ```bash
   <launcher> --cpusampler \
     --cpusampler.ShowTiers=true \
     --cpusampler.Delay=5000 \
     <program> [script args]
   ```

3. **Lower thresholds for testing**:
   ```bash
   <launcher> --experimental-options \
     --engine.FirstTierCompilationThreshold=100 \
     --engine.TraceCompilation \
     <program> [script args]
   ```

4. **Verify method executes enough times**:
   ```bash
   <launcher> --cputracer <program> [script args]
   # Check execution counts
   ```

**Verification**: After fixes, should see "opt done" events and reduced T0 time

#### Pattern 3: Excessive Compilation Times
```
[engine] opt done id=300 hugeFunction |Tier 2|Time 5280( 4200+1080 )ms|AST 450|Inlined 25Y 5N|IR 8500/12000|...
```

**Problem**: Compilation taking >1000ms (blocks warmup)

**Symptoms**:
- Time field shows very high values (>500ms concerning, >1000ms critical)
- Large AST counts (>100)
- Large IR node counts (>5000)
- Many successful inlines (high Y count)

**Root Cause**: Compilation complexity
- Large/complex methods
- Excessive inlining pulling many callees
- Complex control flow

**Impact**:
- Slow warmup
- Memory pressure
- May hit compiler size limits

**Resolution**:

1. **Analyze inlining decisions**:
   ```bash
   <launcher> --experimental-options \
     --engine.TraceCompilation \
     --engine.TraceInlining \
     --engine.CompileOnly=hugeFunction \
     <program> [script args]
   ```

2. **Check if size limits hit**:
   - Look for compilation failures
   - Check IR node counts approaching limits

3. **Consider refactoring**:
   - Split large functions into smaller pieces
   - Reduce inlining budget if appropriate
   - Simplify complex control flow

**Verification**: Reduced Time values while maintaining performance

#### Pattern 4: Poor Inlining Effectiveness
```
[engine] opt done id=200 caller |Tier 2|Time 120ms|AST 50|Inlined 2Y 15N|IR 500/ 800|...
```

**Problem**: Many failed inlines (2 success, 15 failures)

**Symptoms**:
- High N count relative to Y count
- "2Y 15N" = 2 inlined, 15 remained as calls

**Root Cause**:
- Inlining budget exhaustion (method too large)
- Polymorphic call sites (multiple possible targets)
- Recursive calls exceeding depth limits

**Resolution**:

1. **Detailed inlining analysis**:
   ```bash
   <launcher> --experimental-options \
     --engine.TraceCompilation \
     --engine.TraceInlining \
     --engine.CompileOnly=caller \
     <program> [script args]
   ```

2. **Check for polymorphism**:
   ```bash
   <launcher> --experimental-options \
     --engine.TraceCompilationPolymorphism \
     <program> [script args]
   ```

3. **Consider call target splitting**:
   ```bash
   <launcher> --experimental-options \
     --engine.TraceSplitting \
     <program> [script args]
   ```

**Note**: Not all failed inlines are problems
- Some calls shouldn't inline (budget trade-offs)
- Focus on whether performance is actually poor

#### Pattern 5: Tier 1 Without Tier 2 Progression
```
[engine] opt done id=150 warmFunction |Tier 1|...
[Many Tier 1 compilations for various functions]
[No Tier 2 compilations for warmFunction]
```

**Problem**: Method reaches T1 but not T2

**Symptoms**:
- Multiple Tier 1 compilations
- Method never shows Tier 2
- Method becomes "warm" but not "hot"

**Root Cause**:
- Invocations exceed T1 threshold (400) but not T2 (10000)
- Method becomes cold after T1 compilation
- Compilation queue overloaded preventing T2 work

**Resolution**:

1. **Check queue state**:
   ```bash
   <launcher> --experimental-options \
     --engine.TraceCompilation \
     --engine.TraceCompilationDetails \
     <program> [script args] | grep "Queue: Load"
   ```

2. **If Load consistently >1.0**: Queue saturated
   ```bash
   # Increase compiler threads
   <launcher> --experimental-options \
     --engine.CompilerThreads=4 \
     --engine.TraceCompilation \
     <program> [script args]
   ```

3. **Check if T2 is actually needed**:
   - If method becomes cold after T1, T2 not needed
   - This may be correct behavior

**Verification**: Important methods reach T2, queue load <1.0

#### Pattern 6: Queue Overload (with TraceCompilationDetails)
```
[engine] opt queued ... |Queue: Size 50 Load 2.5|...
[engine] opt queued ... |Queue: Size 75 Load 3.1|...
[engine] opt queued ... |Queue: Size 100 Load 4.2|...
```

**Problem**: Queue Load consistently >1.0

**Symptoms**:
- Queue Size growing
- Load metric >1.0 (indicates saturation)
- Dynamic threshold increases kicking in

**Root Cause**:
- Too many methods reaching threshold simultaneously
- Insufficient compiler threads
- Individual compilations taking too long

**Impact**:
- Delayed compilation of hot methods
- Increased warmup time
- Important methods may never compile

**Resolution**:

1. **Increase compiler threads**:
   ```bash
   <launcher> --experimental-options \
     --engine.CompilerThreads=4 \
     --engine.TraceCompilation \
     --engine.TraceCompilationDetails \
     <program> [script args]
   ```

2. **Address long compilation times** (see Pattern 3)

3. **Check for saturation duration**:
   - Temporary during warmup: Acceptable
   - Persistent throughout execution: Problem

**Verification**: Queue Load stays near or below 1.0

### 5. Correlation with Other Tools

#### Trace Compilation + CPU Sampler (ESSENTIAL!)
```bash
# Step 1: CPU Profile
<launcher> --cpusampler \
  --cpusampler.ShowTiers=true \
  --cpusampler.Delay=5000 \
  <program> [script args] > cpu.txt

# Step 2: Compilation Trace
<launcher> --experimental-options \
  --engine.TraceCompilation \
  <program> [script args] > compilation.txt

# Step 3: Correlate
# Hot functions in CPU profile should have "opt done" events
# High T0 time + no compilation = problem!
```

**Workflow**:
1. Profiler identifies hot methods
2. TraceCompilation verifies they compiled
3. If hot but high T0: Compilation issue
4. If hot and low T0: Successfully optimized

#### Trace Compilation + Transfer to Interpreter
```bash
<launcher> --experimental-options \
  --engine.TraceCompilation \
  --engine.TraceTransferToInterpreter \
  <program> [script args] 2>&1 | tee full-trace.log
```

**Why combine**:
- TraceCompilation: Shows compile/deopt cycles
- TraceTransferToInterpreter: Shows WHERE deoptimizations occur
- Together: Complete picture of deoptimization problem

**Look for**:
```
[engine] opt done hotFunction
[engine] transferToInterpreter at hotFunction:42
[engine] opt deopt hotFunction
[engine] opt inv. hotFunction
```
= Deoptimization loop with exact location!

#### Trace Compilation + Trace Inlining
```bash
<launcher> --experimental-options \
  --engine.TraceCompilation \
  --engine.TraceInlining \
  --engine.CompileOnly=specificFunction \
  <program> [script args]
```

**Why combine**:
- TraceCompilation: Shows aggregate inlining stats ("5Y 12N")
- TraceInlining: Shows which specific calls inlined/failed and why
- Together: Understand inlining effectiveness

#### Trace Compilation + Trace Assumptions
```bash
<launcher> --experimental-options \
  --engine.TraceCompilation \
  --engine.TraceAssumptions \
  <program> [script args] 2>&1
```

**Why combine**:
- TraceCompilation: Shows invalidation events
- TraceAssumptions: Shows which assumptions violated
- Together: Understand why invalidations occur

## Best Practices

The skill follows these analysis best practices:

### 1. Start with Basic Trace
```bash
<launcher> --experimental-options \
  --engine.TraceCompilation \
  <program> [script args]
```
- Get overview before adding verbosity
- Identify major patterns first

### 2. Add Details Only When Needed
```bash
# Only add TraceCompilationDetails for queue analysis
<launcher> --experimental-options \
  --engine.TraceCompilation \
  --engine.TraceCompilationDetails \
  <program> [script args]
```
- High output volume
- Only needed for queue debugging

### 3. Use CompileOnly for Focus
```bash
<launcher> --experimental-options \
  --engine.TraceCompilation \
  --engine.CompileOnly=problemFunction \
  <program> [script args]
```
- Dramatically reduces output
- Makes patterns easier to see

### 4. Always Correlate with Profiling
```bash
# Never analyze compilation in isolation
<launcher> --cpusampler --cpusampler.ShowTiers=true <program> [script args]
<launcher> --experimental-options --engine.TraceCompilation <program> [script args]
```
- Profiler shows what's hot
- Compilation trace shows what's optimized
- Together: Complete picture

### 5. Count Deoptimizations
```bash
# After trace
grep "opt deopt" trace.log | sort | uniq -c | sort -rn
```
- High counts for same function = critical problem
- Single occurrences often acceptable

### 6. Check Tier Progression
```bash
# Look for T1 → T2 progression for hot functions
grep "opt done.*Tier" trace.log | grep functionName
```
- Hot functions should reach T2 eventually
- T1 only may indicate issues

## Common Pitfalls to Avoid

The skill warns about these mistakes:

- ❌ **Overwhelming output volume**:
  - Large apps generate massive output
  - Use --engine.CompileOnly to filter

- ❌ **Confusing compilation time with execution time**:
  - Time field = compilation duration
  - Not the same as execution performance

- ❌ **Reacting to single deoptimizations**:
  - Single deopt may be normal (rare path)
  - Repeated deopts are the problem

- ❌ **Not correlating with profiling**:
  - Must understand what's hot first
  - Compilation trace alone incomplete

- ❌ **Forgetting --experimental-options**:
  - Tool won't work without it
  - Always include this flag

- ❌ **Misunderstanding queue load**:
  - Load >1.0 = saturation (bad)
  - Load <1.0 = normal (good)

## Typical Workflow

The skill typically follows this analysis workflow:

### Step 1: CPU Profile First
```bash
<launcher> --cpusampler \
  --cpusampler.ShowTiers=true \
  --cpusampler.Delay=5000 \
  <program> [script args] > cpu.txt
```
**Identify**: Hot functions and tier distribution

### Step 2: Basic Compilation Trace
```bash
<launcher> --experimental-options \
  --engine.TraceCompilation \
  <program> [script args] > compilation.txt
```
**Verify**: Hot functions have "opt done" events

### Step 3: Check for Deoptimizations
```bash
grep "opt deopt" compilation.txt | sort | uniq -c | sort -rn
```
**Look for**: Repeated deoptimizations (same function multiple times)

### Step 4: Investigate Issues
**If deoptimization loop**:
```bash
<launcher> --experimental-options \
  --engine.TraceCompilation \
  --engine.TraceTransferToInterpreter \
  <program> [script args] 2>&1
```

**If poor inlining**:
```bash
<launcher> --experimental-options \
  --engine.TraceCompilation \
  --engine.TraceInlining \
  --engine.CompileOnly=problemFunction \
  <program> [script args]
```

**If queue issues**:
```bash
<launcher> --experimental-options \
  --engine.TraceCompilation \
  --engine.TraceCompilationDetails \
  <program> [script args] | grep "Queue: Load"
```

### Step 5: Verify Fixes
```bash
# Re-run traces after fixes
# Confirm:
# - No repeated deoptimizations
# - Hot functions compile
# - Tier progression to T2
# - Reasonable compilation times
```

## Success Criteria

**Good compilation behavior**:
- ✅ Hot functions show "opt done" events
- ✅ Important functions reach Tier 2
- ✅ Zero or rare deoptimizations
- ✅ No repeated "opt deopt" for same function
- ✅ Reasonable compilation times (<500ms)
- ✅ Good inlining statistics (more Y than N for hot paths)
- ✅ Queue load <1.0

**Critical problems**:
- 🚨 Deoptimization loops (repeated opt deopt)
- 🚨 Hot functions never compiling
- 🚨 Excessive compilation times (>1000ms)
- 🚨 Queue load >2.0 sustained
- 🚨 All Tier 1, no Tier 2 for hot code

## Reference Documentation

For detailed information, see:
- Official GraalVM docs: https://www.graalvm.org/latest/graalvm-as-a-platform/language-implementation-framework/Optimizing/
- Truffle options: https://www.graalvm.org/latest/graalvm-as-a-platform/language-implementation-framework/Options/
- Use Graal Truffle Docs skill

## Implementation Notes

This skill:
- Uses your language's launcher: `<launcher>`
- Requires `--experimental-options` flag
- Outputs to stdout with `[engine]` prefix
- Low overhead, but high output volume
- Essential for understanding compilation lifecycle
- Should always be correlated with CPU profiling
- Focus on deoptimization loops (most critical issue)
- Emphasizes: **Verify hot code compiles and stays compiled**
- Combined with other performance analysis skills for full picture

## Related Skills

- Use Graal Truffle Docs skill to understand Truffle APIs and options
- Use CPU Sampler Analyze skill for initial profiling to identify hot functions
- Use Performance Warnings Analyze skill to find optimization barriers
- Use CPU Tracer Analyze skill for execution frequency insights
- Use Memory Tracer Analyze skill for allocation profiling
- Use Trace Inlining Analyze skill for inlining decision analysis
- Use Trace Transfer to Interpreter Analyze skill for deoptimization insights
- Use Benchmark Baseline skill for creating performance baselines with different benchmarks
