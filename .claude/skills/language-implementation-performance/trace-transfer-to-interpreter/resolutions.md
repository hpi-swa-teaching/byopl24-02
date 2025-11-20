# Resolutions

This guide provides solutions to common issues identified by trace-transfer-to-interpreter.

## Common Issues and Fixes

### Pattern 1: Deoptimization Loop (CRITICAL!)

```
[engine] transferToInterpreter at MyNode.execute(code.lox:42)
[engine] transferToInterpreter at MyNode.execute(code.lox:42)
[engine] transferToInterpreter at MyNode.execute(code.lox:42)
[engine] transferToInterpreter at MyNode.execute(code.lox:42)
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

2. **For Lox user code**: Avoid mixing types in hot paths
   ```lox
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
   ./lox --experimental-options \
     --engine.FirstTierCompilationThreshold=1000 \
     --engine.TraceTransferToInterpreter \
     program.lox
   ```

**Verification**:
```bash
# Before fix: Count transfers
./lox --experimental-options \
  --engine.TraceTransferToInterpreter \
  program.lox 2>&1 | grep -c "transferToInterpreter"

# After fix: Should be zero or very few
```

### Pattern 2: Property Access Deoptimizations

```
[engine] transferToInterpreter at
getProperty(code.lox:123)
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
```lox
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
```lox
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
./lox --experimental-options \
  --engine.TraceTransferToInterpreter \
  --engine.TraceAssumptions \
  program.lox 2>&1 | tee analysis.log
```

### Pattern 3: Warmup Transfers (Normal, But Monitor)

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
./lox --experimental-options \
  --engine.FirstTierCompilationThreshold=800 \
  --engine.TraceTransferToInterpreter \
  program.lox
```

**Verification**:
```bash
# Skip warmup in profiling
./lox --cpusampler --cpusampler.Delay=10000 \
  --cpusampler.ShowTiers=true \
  program.lox
```

### Pattern 4: Rare Path Transfers (Acceptable)

```
[During entire execution]
[engine] transferToInterpreter at errorHandler(code.lox:250)
[engine] transferToInterpreter at validateInput(code.lox:89)
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
./lox --cpusampler program.lox
```

## Best Practices for Fixes

The skill follows these analysis best practices:

### 1. Always Combine with TraceCompilation
```bash
./lox --experimental-options \
  --engine.TraceTransferToInterpreter \
  --engine.TraceCompilation \
  program.lox 2>&1 | tee analysis.log
```
**Why**: See compilation lifecycle with deoptimization events

### 2. Filter to Specific Functions for Debugging
```bash
./lox --experimental-options \
  --engine.TraceTransferToInterpreter \
  --engine.CompileOnly=problematicFunction \
  program.lox 2>&1
```
**Why**: Reduce output volume for focused analysis

### 3. Redirect Output to File
```bash
./lox --experimental-options \
  --engine.TraceTransferToInterpreter \
  program.lox 2>transfers.log
```
**Why**: Analyze offline, compare across runs, count patterns

### 4. Distinguish Warmup from Steady-State
```bash
# Run long enough to see pattern
./lox --experimental-options \
  --engine.TraceTransferToInterpreter \
  long-running-program.lox 2>&1 | \
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
./lox --experimental-options \
  --engine.TraceTransferToInterpreter \
  --engine.CompileImmediately \
  --engine.BackgroundCompilation=false \
  program.lox 2>&1
```
**Why**: Makes behavior deterministic, avoids race conditions

## Related Documentation

- [Common Patterns](./patterns.md) - Problem identification
- [Analysis Guide](./analysis.md) - Understanding the output
- [Main Workflow](../SKILL.md) - Overall optimization process
