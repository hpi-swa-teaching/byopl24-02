# Resolutions

This guide provides solutions to common issues identified by memory-tracer.

## Common Issues and Fixes

### Pattern 1: Allocation Hotspot (Primary Target)

```
Name                | Self Count    | Total Count   | Location
--------------------------------------------------------------------------------
createFilter        | 5000  95.0%   | 5000  95.0%   | sieve.lox~42:512-530
checkPrime          | 150   2.9%    | 5150  97.9%   | sieve.lox~18:203-220
:program            | 7     0.1%    | 5257  100.0%  | sieve.lox~1-50:0-1000
```

**Problem**: Single function dominates allocations (95%!)

**Symptoms**:
- High self count percentage (>80%)
- Self count ≈ Total count (allocations direct, not in callees)
- Function appears in hot path (via CPU profiling)

**Root Cause**: Per-iteration allocation in hot loop
```lox
// Example problematic pattern
for (var i = 0; i < 1000; i = i + 1) {
  var filter = createFilter(i);  // Creates new object each iteration!
  process(filter);
}
```

**Common Causes**:
- Creating new objects in loops instead of reusing
- Functional style with immutable objects per operation
- String concatenation creating intermediate strings
- Boxing/unboxing primitives repeatedly
- Collection operations creating intermediate collections

**Resolution**:

1. **Object Pooling** (if objects can be reused):
   ```lox
   // Create pool of reusable objects
   var pool = createFilterPool(10);
   for (var i = 0; i < 1000; i = i + 1) {
     var filter = pool.acquire();
     filter.reset(i);
     process(filter);
     pool.release(filter);
   }
   ```

2. **Mutable Data Structures** (if safe):
   ```lox
   // Reuse single object
   var filter = createFilter(0);
   for (var i = 0; i < 1000; i = i + 1) {
     filter.update(i);  // Modify existing instead of creating new
     process(filter);
   }
   ```

3. **Pre-size Collections**:
   ```lox
   // ❌ BAD: Growing collection
   var result = 👉👈;
   for (var i = 0; i < 1000; i = i + 1) {
     // Repeated resizing and copying
   }

   // ✅ GOOD: Pre-sized
   var result = createArrayWithSize(1000);
   ```

**Verification**:
```bash
# Before optimization
./lox --experimental-options --memtracer program.lox > before.txt
grep "createFilter" before.txt

# After optimization
./lox --experimental-options --memtracer program.lox > after.txt
grep "createFilter" after.txt

# Should see dramatically reduced count
```

### Pattern 2: Unexpected Allocations

```
Name                | Self Count    | Total Count   | Location
--------------------------------------------------------------------------------
simpleHelper        | 1000  19.0%   | 1000  19.0%   | code.lox~67:890-910
```

**Problem**: Function thought to be allocation-free is allocating!

**Symptoms**:
- "Lightweight" helper function appears in output
- Allocations in function designed to be allocation-free
- Surprise at seeing function in allocation profile

**Root Cause**: Subtle language semantics causing hidden allocations

**Common Hidden Allocations in Lox**:
```lox
// Boxing operations
fun calculate(n) {
  return n + 1;  // Might box if n type varies
}

// String operations
fun formatNumber(n) {
  return "Value: " + String(n);  // Creates intermediate strings
}

// Array operations
fun getElement(arr, i) {
  return arr👉i👈;  // Might allocate for bounds checking wrapper
}
```

**Resolution**:

1. **Use TraceStatements to pinpoint**:
   ```bash
   ./lox --experimental-options \
     --memtracer \
     --memtracer.TraceStatements \
     --memtracer.FilterRootName=*simpleHelper* \
     program.lox
   ```

2. **Examine each statement** for hidden allocations

3. **Consider alternatives**:
   - Avoid boxing by using consistent types
   - Use string builders for concatenation
   - Cache results of allocation-heavy operations

### Pattern 3: Type Distribution Issues

```
Type Histogram Mode:

Type                | Count        | Percentage
------------------------------------------------
Array               | 3000  60.0%  | Most allocations
String              | 1500  30.0%  | Second most
Number              | 400   8.0%   | Boxed numbers
CustomClass         | 100   2.0%   | User objects
```

**Problem**: Excessive intermediate collections or boxed types

**Symptoms** (typehistogram mode):
- One type dominates (>60%)
- Unexpected types present (boxed primitives when unboxed expected)
- Temporary collection types (intermediate arrays)

**Common Issues**:
- **Array-heavy**: Excessive intermediate array allocations
- **String-heavy**: String concatenation in loops
- **Boxed primitives**: Type instability causing boxing

**Resolution by Type**:

1. **Excessive Arrays**:
   ```lox
   // ❌ BAD: Intermediate arrays
   fun process(data) {
     var filtered = filterArray(data);    // Allocates
     var mapped = mapArray(filtered);     // Allocates
     var reduced = reduceArray(mapped);   // Allocates
     return reduced;
   }

   // ✅ GOOD: Single-pass or streaming
   fun process(data) {
  # ... (additional code omitted for brevity)
   ```

2. **Excessive Strings**:
   ```lox
   // ❌ BAD: Repeated concatenation
   var result = "";
   for (var i = 0; i < 100; i = i + 1) {
     result = result + String(i) + ",";  // Many allocations
   }

   // ✅ GOOD: Build array then join
   var parts = 👉👈;
   for (var i = 0; i < 100; i = i + 1) {
     // Add to array
   }
   // Join once at end
   ```

3. **Boxed Primitives**:
   - Ensure consistent types in hot paths
   - Avoid mixing integers and doubles
   - Use specialized operations for primitives

### Pattern 4: Correlated with CPU Hotspots

**Most powerful analysis: Combine with CPU profiler!**

```bash
# Step 1: CPU profiling
./lox --cpusampler --cpusampler.Delay=2000 \
  --cpusampler.ShowTiers=true \
  program.lox > cpu.txt

# Step 2: Memory profiling
./lox --experimental-options --memtracer \
  program.lox > memory.txt

# Step 3: Compare outputs
```

**Look for functions appearing in BOTH profiles**:

**High CPU + High Allocations = Critical Target!**
```
CPU Profile:
  processData    1500ms  75.0%   <-- High CPU time!

Memory Profile:
  processData    4500    90.0%   <-- High allocations!

→ This function is creating memory pressure
→ GC overhead likely contributing to CPU time
→ PRIMARY OPTIMIZATION TARGET
```

**High CPU + Low Allocations = Computational**:
```
CPU Profile:
  calculate      1200ms  60.0%   <-- High CPU time

Memory Profile:
  calculate      10      0.2%    <-- Few allocations

→ CPU-bound, not memory-bound
→ Focus on algorithm optimization, not allocation reduction
```

**Low CPU + High Allocations = May Not Matter**:
```
CPU Profile:
  initialize     50ms    2.5%    <-- Low CPU time

Memory Profile:
  initialize     2000    40.0%   <-- Many allocations!

→ Runs infrequently (startup?)
→ Allocations may not impact performance
→ Low priority unless causing specific issues
```

**Resolution Strategy**:
1. Prioritize functions with **both high CPU and high allocations**
2. These likely have GC overhead contributing to CPU time
3. Reducing allocations here has compounding benefit

## Best Practices for Fixes

The skill follows these profiling best practices:

### 1. Start with High-Level Overview
```bash
./lox --experimental-options --memtracer program.lox
```
- Default histogram with root-level granularity
- Identify top allocation sources
- Get overall picture before drilling down

### 2. Use Filters for Focused Analysis
```bash
# After identifying hotspot
./lox --experimental-options \
  --memtracer \
  --memtracer.TraceStatements \
  --memtracer.FilterRootName=*hotspot* \
  program.lox
```
- Reduces overhead dramatically
- Makes output manageable
- Pinpoints exact allocation statements

### 3. Combine with CPU Profiling
```bash
# Run both profiles
./lox --cpusampler --cpusampler.Delay=2000 program.lox > cpu.txt
./lox --experimental-options --memtracer program.lox > mem.txt

# Compare outputs
```
- Identify high CPU + high allocation functions
- These are critical optimization targets
- Allocation reduction here has compounding benefit

### 4. Progressive Investigation
1. **High-level**: Histogram mode, root-level
2. **Type analysis**: Typehistogram to understand object types
3. **Detailed**: TraceStatements on specific hotspots
4. **Context**: TraceCalls to understand calling patterns

### 5. Iterative Optimization
```bash
# Profile → Optimize → Verify cycle
./lox --experimental-options --memtracer program.lox > before.txt
# Make optimization
./lox --experimental-options --memtracer program.lox > after.txt
# Compare counts
diff before.txt after.txt
```

### 6. Verify Actual Performance Impact
```bash
# Memory profiling shows improvement, but does performance?
time ./lox program.lox  # Before
# Optimize
time ./lox program.lox  # After - should be faster
```
- Don't optimize based solely on allocation counts
- Verify actual performance improvement
- Some allocations might not matter (eliminated by compiler)

## Related Documentation

- [Common Patterns](./patterns.md) - Problem identification
- [Analysis Guide](./analysis.md) - Understanding the output
- [Main Workflow](../SKILL.md) - Overall optimization process
