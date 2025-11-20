# Resolutions

This guide provides solutions to common issues identified by trace-compilation.

## Common Issues and Fixes

### Pattern 1: Deoptimization Loop (CRITICAL!)

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
     program.lox 2>&1 | tee trace.log
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
     program.lox
   ```

**Verification**:
```bash
# Count deopt events
grep "opt deopt" trace.log | sort | uniq -c

# Goal: Zero repeated deoptimizations for same function
```

### Pattern 2: Hot Methods Never Compiling

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
     program.lox
   ```

2. **Check profiler tier information**:
   ```bash
   ./lox --cpusampler \
     --cpusampler.ShowTiers=true \
     --cpusampler.Delay=5000 \
     program.lox
   ```

3. **Lower thresholds for testing**:
   ```bash
   ./lox --experimental-options \
     --engine.FirstTierCompilationThreshold=100 \
     --engine.TraceCompilation \
     program.lox
   ```

4. **Verify method executes enough times**:
   ```bash
   ./lox --cputracer program.lox
   # Check execution counts
   ```

**Verification**: After fixes, should see "opt done" events and reduced T0 time

### Pattern 3: Excessive Compilation Times

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
     program.lox
   ```

2. **Check if size limits hit**:
   - Look for compilation failures
   - Check IR node counts approaching limits

3. **Consider refactoring**:
   - Split large functions into smaller pieces
   - Reduce inlining budget if appropriate
   - Simplify complex control flow

**Verification**: Reduced Time values while maintaining performance

### Pattern 4: Poor Inlining Effectiveness

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
     program.lox
   ```

2. **Check for polymorphism**:
   ```bash
   ./lox --experimental-options \
     --engine.TraceCompilationPolymorphism \
     program.lox
   ```

3. **Consider call target splitting**:
   ```bash
   ./lox --experimental-options \
     --engine.TraceSplitting \
     program.lox
   ```

**Note**: Not all failed inlines are problems
- Some calls shouldn't inline (budget trade-offs)
- Focus on whether performance is actually poor

### Pattern 5: Tier 1 Without Tier 2 Progression

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
     program.lox | grep "Queue: Load"
   ```

2. **If Load consistently >1.0**: Queue saturated
   ```bash
   # Increase compiler threads
   ./lox --experimental-options \
     --engine.CompilerThreads=4 \
     --engine.TraceCompilation \
     program.lox
   ```

3. **Check if T2 is actually needed**:
   - If method becomes cold after T1, T2 not needed
   - This may be correct behavior

**Verification**: Important methods reach T2, queue load <1.0

### Pattern 6: Queue Overload (with TraceCompilationDetails)

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
     program.lox
   ```

2. **Address long compilation times** (see Pattern 3)

3. **Check for saturation duration**:
   - Temporary during warmup: Acceptable
   - Persistent throughout execution: Problem

**Verification**: Queue Load stays near or below 1.0

## Best Practices for Fixes

The skill follows these analysis best practices:

### 1. Start with Basic Trace
```bash
./lox --experimental-options \
  --engine.TraceCompilation \
  program.lox
```
- Get overview before adding verbosity
- Identify major patterns first

### 2. Add Details Only When Needed
```bash
# Only add TraceCompilationDetails for queue analysis
./lox --experimental-options \
  --engine.TraceCompilation \
  --engine.TraceCompilationDetails \
  program.lox
```
- High output volume
- Only needed for queue debugging

### 3. Use CompileOnly for Focus
```bash
./lox --experimental-options \
  --engine.TraceCompilation \
  --engine.CompileOnly=problemFunction \
  program.lox
```
- Dramatically reduces output
- Makes patterns easier to see

### 4. Always Correlate with Profiling
```bash
# Never analyze compilation in isolation
./lox --cpusampler --cpusampler.ShowTiers=true program.lox
./lox --experimental-options --engine.TraceCompilation program.lox
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

## Related Documentation

- [Common Patterns](./patterns.md) - Problem identification
- [Analysis Guide](./analysis.md) - Understanding the output
- [Main Workflow](../SKILL.md) - Overall optimization process
