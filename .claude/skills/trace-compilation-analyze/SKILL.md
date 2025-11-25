---
name: Run and Analyze Compilation Tracer
description: Logs every compilation event with timing, tier (T1/T2), success/failure, and invalidation reasons. Use to verify hot code is compiling, diagnose compilation failures/bailouts, track recompilation cycles, and understand tiered compilation behavior. Shows WHEN compilation happens. Essential for understanding compilation lifecycle.
---

# Skill: Run and Analyze Compilation Tracer

This skill runs the Trace Compilation tool on a Lox program and provides detailed analysis of compilation events to help verify optimization effectiveness and diagnose compilation issues.

## What This Skill Does

1. **Runs Compilation Tracer**: Executes the Lox program with compilation tracing enabled
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
- The path to the Lox program to analyze
- Ideally, CPU profiling results showing hot functions
- Whether you want basic or detailed queue analysis

## Reliability Protocol (MANDATORY)

**Context:** Tools often fail due to environment issues, permissions, or misconfiguration. To avoid hallucinating results, misinterpreting output, or wasting resources, you must follow this 3-step verification loop.

### Step 1: Pre-Execution Baseline

Before executing the primary task, establish a mental baseline:
* **Complexity Estimate:** asking yourself what you expect from a run with a trivial input (i.e. "If I run this on trivial input, how fast should it be?").
* **Failure Mode Prediction:** "If this tool is broken, will it hang, crash, or return empty text?"
* **Sanity Check:** If the tool takes 100x longer than your estimate, **STOP**. It is likely misconfigured or waiting on input.

### Step 2: The Probe (Dry Run)
Never run a complex or heavy command blind. Execute a **Probe** first:
* **The Test:** Run the exact command structure on a trivial target (e.g., `print "test";`, `SELECT 1`, or a dummy file).
* **Constraint:** If the Probe hangs, errors, or produces empty output, **STOP**. Do not proceed to the main task.

### Step 3: Output Audit (Verification)

Do not assume success based on exit codes.
* **Physical Check:** verify the output artifact exists and has a file size > 0 bytes.
* **Content Scan:** Read the first 5 lines/bytes of the output to ensure it is not an error message written to stdout (e.g., "Error: Command not found" saved inside `output.json`). Verify it's in the range of expected content and metrics. If it's to far off, **STOP**.

## How the Skill Works

The skill follows this workflow:

### 1. Initial Setup
- Confirms the Lox program path and arguments
- Determines whether detailed queue analysis needed
- Optionally sets up function filtering

### 2. Run Compilation Tracer

⚠️ WARNING: Output Volume Tracing generates massive text. Always redirect to file and inspect slices of the data.

#### Basic Compilation Trace (Recommended)
```bash
./lox --experimental-options \
  --engine.TraceCompilation \
  program.lox [script args]
```
- Shows compilation completion events
- Includes deoptimizations and invalidations
- Moderate output volume

#### Detailed Queue Analysis
```bash
./lox --experimental-options \
  --engine.TraceCompilation \
  --engine.TraceCompilationDetails \
  program.lox [script args]
```
- Adds queue events (queued, start, unqueued)
- Shows queue size, load, and timing
- HIGH output volume - use for queue debugging only

#### Focused on Specific Function
```bash
./lox --experimental-options \
  --engine.TraceCompilation \
  --engine.CompileOnly=functionName \
  program.lox [script args]
```
- Restricts compilation to specific method
- Dramatically reduces output volume
- Ideal for investigating specific issues

#### Synchronous Compilation (Debugging)
```bash
./lox --experimental-options \
  --engine.TraceCompilation \
  --engine.BackgroundCompilation=false \
  program.lox [script args]
```
- Disables background compilation
- Makes output deterministic
- Simplifies correlation between events

#### Redirect Output
```bash
./lox --experimental-options \
  --engine.TraceCompilation \
  program.lox [script args] > trace.log

# Later analysis
tail -n 50 trace.log
grep "opt deopt" trace.log | tail -n 20  
```
- Saves output to file for later analysis
- Enables searching and filtering

### 3. Understand Output Events

#### Compilation Success (opt done)
```
[engine] opt done id=244 innerLoop |Tier 1|Time 268( 220+47 )ms|AST 17|Inlined 0Y 2N|IR 238/ 437|CodeSize 1874|Src sieve.lox:42
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
- **Src sieve.lox:42**: Source location

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
[engine] opt queued id=237 processData |Tier 1|Count/Thres 25/ 25|Queue: Size 1 Change +1 Load 0.06|Src code.lox:42
```
- **Count/Thres 25/ 25**: Reached threshold (25 invocations)
- **Queue: Size 1**: One compilation in queue
- **Change +1**: Queue size increased by 1
- **Load 0.06**: Queue load (1.0 = normal, >1.0 = overloaded)

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
```lox
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
   ./lox --experimental-options \
     --engine.TraceCompilation \
     --engine.TraceTransferToInterpreter \
     program.lox [script args] 2>&1 | tee trace.log
   ```

2. **Find exact deoptimization location** in stack traces

3. **Fix type stability**:
   ```lox
   // Ensure consistent types in hot paths
   // Don't mix Number() calls with literals
   // Avoid type instability
   ```

4. **Use assumption tracing**:
   ```bash
   ./lox --experimental-options \
     --engine.TraceCompilation \
     --engine.TraceAssumptions \
     program.lox [script args]
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
   ./lox --experimental-options \
     --engine.Compilation=true \
     --engine.TraceCompilation \
     program.lox [script args]
   ```

2. **Check profiler tier information**:
   ```bash
   ./lox --cpusampler \
     --cpusampler.ShowTiers=true \
     --cpusampler.Delay=5000 \
     program.lox [script args]
   ```

3. **Lower thresholds for testing**:
   ```bash
   ./lox --experimental-options \
     --engine.FirstTierCompilationThreshold=100 \
     --engine.TraceCompilation \
     program.lox [script args]
   ```

4. **Verify method executes enough times**:
   ```bash
   ./lox --cputracer program.lox [script args]
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
   ./lox --experimental-options \
     --engine.TraceCompilation \
     --engine.TraceInlining \
     --engine.CompileOnly=hugeFunction \
     program.lox [script args]
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
   ./lox --experimental-options \
     --engine.TraceCompilation \
     --engine.TraceInlining \
     --engine.CompileOnly=caller \
     program.lox [script args]
   ```

2. **Check for polymorphism**:
   ```bash
   ./lox --experimental-options \
     --engine.TraceCompilationPolymorphism \
     program.lox [script args]
   ```

3. **Consider call target splitting**:
   ```bash
   ./lox --experimental-options \
     --engine.TraceSplitting \
     program.lox [script args]
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
   ./lox --experimental-options \
     --engine.TraceCompilation \
     --engine.TraceCompilationDetails \
     program.lox [script args] | grep "Queue: Load"
   ```

2. **If Load consistently >1.0**: Queue saturated
   ```bash
   # Increase compiler threads
   ./lox --experimental-options \
     --engine.CompilerThreads=4 \
     --engine.TraceCompilation \
     program.lox [script args]
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
   ./lox --experimental-options \
     --engine.CompilerThreads=4 \
     --engine.TraceCompilation \
     --engine.TraceCompilationDetails \
     program.lox [script args]
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
./lox --cpusampler \
  --cpusampler.ShowTiers=true \
  --cpusampler.Delay=5000 \
  program.lox [script args] > cpu.txt

# Step 2: Compilation Trace
./lox --experimental-options \
  --engine.TraceCompilation \
  program.lox [script args] > compilation.txt

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
./lox --experimental-options \
  --engine.TraceCompilation \
  --engine.TraceTransferToInterpreter \
  program.lox [script args] 2>&1 | tee full-trace.log
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
./lox --experimental-options \
  --engine.TraceCompilation \
  --engine.TraceInlining \
  --engine.CompileOnly=specificFunction \
  program.lox [script args]
```

**Why combine**:
- TraceCompilation: Shows aggregate inlining stats ("5Y 12N")
- TraceInlining: Shows which specific calls inlined/failed and why
- Together: Understand inlining effectiveness

#### Trace Compilation + Trace Assumptions
```bash
./lox --experimental-options \
  --engine.TraceCompilation \
  --engine.TraceAssumptions \
  program.lox [script args] 2>&1
```

**Why combine**:
- TraceCompilation: Shows invalidation events
- TraceAssumptions: Shows which assumptions violated
- Together: Understand why invalidations occur

## Best Practices

The skill follows these analysis best practices:

### 1. Start with Basic Trace
```bash
./lox --experimental-options \
  --engine.TraceCompilation \
  program.lox [script args]
```
- Get overview before adding verbosity
- Identify major patterns first

### 2. Add Details Only When Needed
```bash
# Only add TraceCompilationDetails for queue analysis
./lox --experimental-options \
  --engine.TraceCompilation \
  --engine.TraceCompilationDetails \
  program.lox [script args]
```
- High output volume
- Only needed for queue debugging

### 3. Use CompileOnly for Focus
```bash
./lox --experimental-options \
  --engine.TraceCompilation \
  --engine.CompileOnly=problemFunction \
  program.lox [script args]
```
- Dramatically reduces output
- Makes patterns easier to see

### 4. Always Correlate with Profiling
```bash
# Never analyze compilation in isolation
./lox --cpusampler --cpusampler.ShowTiers=true program.lox [script args]
./lox --experimental-options --engine.TraceCompilation program.lox [script args]
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
./lox --cpusampler \
  --cpusampler.ShowTiers=true \
  --cpusampler.Delay=5000 \
  program.lox [script args] > cpu.txt
```
**Identify**: Hot functions and tier distribution

### Step 2: Basic Compilation Trace
```bash
./lox --experimental-options \
  --engine.TraceCompilation \
  program.lox [script args] > compilation.txt
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
./lox --experimental-options \
  --engine.TraceCompilation \
  --engine.TraceTransferToInterpreter \
  program.lox [script args] 2>&1
```

**If poor inlining**:
```bash
./lox --experimental-options \
  --engine.TraceCompilation \
  --engine.TraceInlining \
  --engine.CompileOnly=problemFunction \
  program.lox [script args]
```

**If queue issues**:
```bash
./lox --experimental-options \
  --engine.TraceCompilation \
  --engine.TraceCompilationDetails \
  program.lox [script args] | grep "Queue: Load"
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
- `/Users/antonykamp/Projects/hpi-ma/byopl24-02/docs/commands/trace-compilation.md` - Complete documentation
- Official GraalVM docs: https://www.graalvm.org/latest/graalvm-as-a-platform/language-implementation-framework/Optimizing/
- Truffle options: https://www.graalvm.org/latest/graalvm-as-a-platform/language-implementation-framework/Options/

## Implementation Notes

This skill:
- Uses the Lox launcher: `./lox`
- Requires `--experimental-options` flag
- Outputs to stdout with `[engine]` prefix
- Low overhead, but high output volume
- Essential for understanding compilation lifecycle
- Should always be correlated with CPU profiling
- Focus on deoptimization loops (most critical issue)
- Emphasizes: **Verify hot code compiles and stays compiled**
