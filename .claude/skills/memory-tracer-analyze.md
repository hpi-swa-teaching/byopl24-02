---
name: Run and Analyze Memory Tracer
description: Experimental allocation profiler tracking memory allocations at guest-language level. Shows allocation sites, object types, and memory pressure patterns. Use to identify allocation hotspots, find unnecessary object creation, and understand memory behavior. High allocations in hot loops = optimization opportunity if escape analysis isn't eliminating them.
---

# Skill: Run and Analyze Memory Tracer

This skill runs the Memory Tracer profiling tool on a Lox program and provides detailed analysis of allocation patterns to help identify memory optimization opportunities.

## What This Skill Does

1. **Runs Memory Tracer**: Executes the Lox program with allocation profiling enabled
2. **Analyzes Allocation Patterns**: Interprets the output to identify:
   - Allocation hotspots (functions creating most objects)
   - Object type distribution
   - Unexpected allocations in critical code
   - Memory pressure patterns
3. **Provides Optimization Recommendations**: Suggests specific changes to reduce allocations

## Important Understanding: Pre-Optimization View

**Critical concept**: Memory Tracer shows **logical allocations** at source code level, NOT actual runtime allocations!

- Shows what the source code **appears** to allocate
- **Before** compiler optimizations (escape analysis, scalar replacement)
- Many shown allocations might be **eliminated by compiler** in compiled code
- Useful for understanding **algorithmic patterns**, not predicting actual heap usage

**Use for**:
- ✅ Comparing alternative algorithm implementations
- ✅ Understanding allocation patterns at source level
- ✅ Finding obviously unnecessary object creation
- ✅ Guiding algorithm design

**Don't use for**:
- ❌ Predicting actual memory consumption
- ❌ Estimating heap size needs
- ❌ Measuring actual GC pressure (use heap dumps instead)

## When to Use This Skill

- Suspect memory allocation patterns impacting performance
- Profiling shows significant GC overhead
- Want to understand which functions create most objects
- Comparing alternative implementations for memory efficiency
- Looking for unexpected allocations in hot paths
- Combined with CPU profiler to correlate allocation with time

## Prerequisites

Before running this skill, you should know:
- The path to the Lox program to analyze
- Whether you want to focus on specific functions (reduces overhead)
- Ideally, CPU profiling results showing hot functions

## How the Skill Works

The skill follows this workflow:

### 1. Initial Setup
- Confirms the Lox program path and arguments
- Determines appropriate output format and granularity
- Optionally sets up filters for focused profiling

### 2. Run Memory Tracer

#### Basic Allocation Profile (Recommended)
```bash
./lox --experimental-options --memtracer program.lox
```
- Default histogram output
- Shows allocations by source location
- Root-level granularity (function-level)

#### Type Histogram (What Object Types)
```bash
./lox --experimental-options \
  --memtracer \
  --memtracer.Output=typehistogram \
  program.lox
```
- Groups allocations by object type/class
- Shows what kinds of objects created most
- Reveals type-specific optimization opportunities

#### Call Tree (Hierarchical View)
```bash
./lox --experimental-options \
  --memtracer \
  --memtracer.Output=calltree \
  program.lox
```
- Hierarchical allocation call tree
- Shows allocation patterns in calling context
- Similar to CPU profiler call tree

#### Statement-Level Detail (High Overhead!)
```bash
./lox --experimental-options \
  --memtracer \
  --memtracer.TraceStatements \
  --memtracer.FilterRootName=*hotFunction* \
  program.lox
```
- Attributes allocations to individual statements
- **HIGH OVERHEAD** - always use with filters!
- Pinpoints exact allocation sources

#### With Call Sites (More Context)
```bash
./lox --experimental-options \
  --memtracer \
  --memtracer.TraceCalls \
  program.lox
```
- Includes call site information
- Shows which functions called allocating function
- Richer calling context, higher overhead

### 3. Understand Output Formats

#### Histogram Mode (Default)
```
Location Histogram with Allocation Counts.
Recorded a total of 5007 allocations.

Total Count: Number of allocations during the execution of this element.
Self Count: Number of allocations in this element alone (excluding sub calls).

Name                | Self Count    | Total Count   | Location
--------------------------------------------------------------------------------
next                | 5000  99.9%   | 5000  99.9%   | primes.lox~31-37:537-737
:program            | 6     0.1%    | 5007  100.0%  | primes.lox~1-46:0-982
Primes              | 1     0.0%    | 1     0.0%    | primes.lox~25-38:424-739
```

**Field Interpretation**:

- **Name**: Function or code element identifier
  - `:program` = top-level code
  - Regular names = function names

- **Self Count**: Allocations directly in this element (excluding called functions)
  - Shows allocations attributable exclusively to this function's own code
  - High self count = direct allocation source

- **Total Count**: Allocations including all functions called by this element
  - Shows total allocation impact including downstream effects
  - Difference from self count = allocations in callees

- **Percentage**: Proportion relative to total allocations
  - High percentages = optimization targets

- **Location**: Source file and position
  - Format: `file~line-range:character-range`
  - Enables precise navigation to source

### 4. Identify Common Patterns

#### Pattern 1: Allocation Hotspot (Primary Target)
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

#### Pattern 2: Unexpected Allocations
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

#### Pattern 3: Type Distribution Issues
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
     var result = 0;
     for (var i = 0; i < data.length; i = i + 1) {
       if (filter(data👉i👈)) {
         result = result + map(data👉i👈);
       }
     }
     return result;
   }
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

#### Pattern 4: Correlated with CPU Hotspots
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

### 5. Understanding Limitations

#### Counts vs. Sizes
**Important**: Memory Tracer counts allocations, NOT memory size!

```
Function creates 1000 small objects (10 bytes each) = 1000 count
Function creates 10 large objects (1KB each)       = 10 count

Memory Tracer shows: First function "worse" (1000 vs 10)
Reality: Second function uses more memory (10KB vs 10KB)
```

**Implication**:
- Tool best for understanding frequency patterns
- NOT for memory budgeting or heap size estimation
- Use heap dumps for actual memory consumption

#### Pre-Optimization View
**Critical**: Shows allocations BEFORE compiler optimizations!

```lox
fun hotFunction(n) {
  var temp = Point(n, n);  // Appears in memtracer output
  return temp.x + temp.y;  // Object might not escape
}
```

**Reality**:
- Escape analysis might eliminate `temp` allocation entirely
- Scalar replacement might convert object into local variables
- Compiled code might have ZERO actual allocations
- But memtracer still shows the allocation

**Use appropriately**:
- ✅ Compare algorithm alternatives
- ✅ Understand source-level patterns
- ✅ Find obviously unnecessary allocations
- ❌ Don't assume shown allocations actually happen in compiled code

### 6. Advanced Options

#### Filter by Function Name
```bash
./lox --experimental-options \
  --memtracer \
  --memtracer.FilterRootName=*process* \
  program.lox
```
- Focus on functions matching pattern
- Reduces overhead and output volume

#### Filter by File
```bash
./lox --experimental-options \
  --memtracer \
  --memtracer.FilterFile=*benchmark* \
  program.lox
```
- Only trace allocations in specific files

#### Include Internal Allocations
```bash
./lox --experimental-options \
  --memtracer \
  --memtracer.TraceInternal \
  program.lox
```
- Shows framework/runtime allocations
- Primarily for language implementers
- Very verbose output

#### Combined Options
```bash
./lox --experimental-options \
  --memtracer \
  --memtracer.TraceStatements \
  --memtracer.TraceCalls \
  --memtracer.FilterRootName=*hotFunction* \
  --memtracer.Output=calltree \
  program.lox
```
- Statement-level detail
- Call site information
- Filtered to specific function
- Call tree output format

## Best Practices

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

## Common Pitfalls to Avoid

The skill warns about these mistakes:

- ❌ **Treating counts as memory size**:
  - Many small allocations ≠ more memory than few large ones
  - Use heap dumps for actual memory consumption

- ❌ **Assuming shown allocations actually happen**:
  - Pre-optimization view
  - Compiler might eliminate allocations
  - Verify actual performance impact

- ❌ **Forgetting --experimental-options flag**:
  - Tool won't work without it
  - Always include experimental-options

- ❌ **Using in production or benchmarks**:
  - Substantial overhead from instrumentation
  - Development/debugging only
  - Skews performance measurements

- ❌ **Optimizing without CPU profiling correlation**:
  - Might optimize allocations that don't matter
  - Could make code more complex without benefit
  - Always correlate with actual performance

- ❌ **Not using filters with TraceStatements**:
  - Overwhelming overhead without filters
  - Impractical output volume
  - Always filter aggressively

## Typical Workflow

The skill typically follows this analysis workflow:

### Step 1: CPU Profile First
```bash
./lox --cpusampler --cpusampler.Delay=2000 \
  --cpusampler.ShowTiers=true \
  program.lox > cpu.txt
```
**Identify**: Top 3-5 functions consuming most time

### Step 2: Memory Profile
```bash
./lox --experimental-options --memtracer \
  program.lox > memory.txt
```
**Identify**: Functions creating most allocations

### Step 3: Correlate Profiles
```bash
# Find functions in both profiles
grep "hotFunction" cpu.txt
grep "hotFunction" memory.txt
```
**Target**: Functions with high CPU and high allocations

### Step 4: Type Analysis (Optional)
```bash
./lox --experimental-options \
  --memtracer \
  --memtracer.Output=typehistogram \
  program.lox
```
**Understand**: What kinds of objects being created

### Step 5: Statement-Level Detail
```bash
./lox --experimental-options \
  --memtracer \
  --memtracer.TraceStatements \
  --memtracer.FilterRootName=*hotFunction* \
  program.lox
```
**Pinpoint**: Exact statements causing allocations

### Step 6: Optimize
- Reduce unnecessary allocations
- Reuse objects where possible
- Avoid intermediate collections
- Fix type stability issues

### Step 7: Verify
```bash
# Re-run memory profile
./lox --experimental-options --memtracer \
  program.lox > after-memory.txt

# Compare counts
diff memory.txt after-memory.txt

# Measure actual performance
time ./lox program.lox
```
**Confirm**: Allocations reduced and performance improved

## Success Criteria

**Good outcomes**:
- ✅ Identified top allocation hotspots
- ✅ Correlated with CPU profiling results
- ✅ Found specific optimization opportunities
- ✅ Reduced allocation counts in hot paths
- ✅ Verified actual performance improvement

**Red flags**:
- 🚨 Single function >80% of allocations
- 🚨 High allocations in supposedly allocation-free code
- 🚨 Allocations correlated with high CPU time
- 🚨 Many intermediate object allocations in loops

## Reference Documentation

For detailed information, see:
- `/Users/antonykamp/Projects/hpi-ma/byopl24-02/docs/commands/memory-tracer.md` - Complete documentation
- Official GraalVM docs: https://www.graalvm.org/latest/tools/profiling/
- AllocationReporter API: https://www.graalvm.org/truffle/javadoc/com/oracle/truffle/api/instrumentation/AllocationReporter.html

## Implementation Notes

This skill:
- Uses the Lox launcher: `./lox`
- Requires `--experimental-options` flag (mandatory)
- Tool is experimental - for development only
- Shows pre-optimization allocations
- Counts allocations, not memory size
- Should be correlated with CPU profiling
- Output appears at program termination
- Substantial overhead - not for production
- Emphasizes: **Verify actual performance impact, not just allocation counts**
