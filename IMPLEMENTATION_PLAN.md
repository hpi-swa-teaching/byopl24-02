# Lox Performance Optimization: Comprehensive Implementation Plan

**Project**: Lox Language Performance Improvements  
**Date**: 2025-11-29  
**Expected Total Gain**: 1.5-10x (depending on workload)  
**Total Implementation Time**: 2-4 days  
**Risk Level**: Low-Medium

---

## Table of Contents

1. [Overview](#overview)
2. [Pre-Implementation Checklist](#pre-implementation-checklist)
3. [Phase 1A: Boxing Elimination Configuration (P0)](#phase-1a-boxing-elimination-configuration-p0)
4. [Phase 1B: Function Call Optimization (P2 - Quick Win)](#phase-1b-function-call-optimization-p2---quick-win)
5. [Phase 2: GlobalObject Refactoring (P1)](#phase-2-globalobject-refactoring-p1)
6. [Phase 3: Array Iterator Optimization (P3)](#phase-3-array-iterator-optimization-p3)
7. [Post-Implementation Validation](#post-implementation-validation)
8. [Rollback Procedures](#rollback-procedures)
9. [Performance Monitoring](#performance-monitoring)

---

## Overview

### Implementation Strategy

**Approach**: Incremental, test-driven, measure-first
**Order**: Quick wins first, then complex refactorings
- **Phase 1A+1B** (P0+P2): Two trivial 1-line changes - HIGH impact, LOW risk
- **Phase 2** (P1): Medium complexity refactoring - HIGH impact, MEDIUM risk
- **Phase 3** (P3): Medium complexity custom iterator - LOW-MEDIUM impact, LOW risk

**Validation**: After each phase, full test suite + benchmarks

**Rationale**: Combining the two trivial fixes (boxing + function boundary) gives immediate 3-5x improvement in <1 hour, building momentum before the more complex GlobalObject refactor.

### Success Criteria

- ✅ All tests pass after each phase
- ✅ Zero performance warnings in hot code
- ✅ Benchmark suite 1.5-2.5x faster overall
- ✅ Short programs 3-10x faster
- ✅ No memory regressions

### Risk Mitigation

1. **Incremental Changes**: One issue at a time
2. **Branch per Fix**: Separate branch for each phase
3. **Automated Testing**: Run full suite after each change
4. **Benchmark Baseline**: Capture before/after metrics
5. **Rollback Ready**: Git tags for each working state

---

## Pre-Implementation Checklist

### 1. Establish Baselines

```bash
# Create baseline directory
mkdir -p performance_baselines
cd performance_baselines

# Benchmark suite
echo "=== Queens Benchmark ===" > baseline_benchmarks.txt
../lox harness.lox queens 10 8 >> baseline_benchmarks.txt
echo "" >> baseline_benchmarks.txt

echo "=== Permute Benchmark ===" >> baseline_benchmarks.txt
../lox harness.lox permute 10 6 >> baseline_benchmarks.txt
echo "" >> baseline_benchmarks.txt

echo "=== Towers Benchmark ===" >> baseline_benchmarks.txt
../lox harness.lox towers 10 13 >> baseline_benchmarks.txt
echo "" >> baseline_benchmarks.txt

echo "=== List Benchmark ===" >> baseline_benchmarks.txt
../lox harness.lox list 10 18 >> baseline_benchmarks.txt

# CPU profiling baseline
echo "=== CPU Profile (Queens) ===" > baseline_cpu_profile.txt
../lox --cpusampler --cpusampler.ShowTiers=true \
  harness.lox queens 5 8 >> baseline_cpu_profile.txt

# Warmup baseline
../lox --cpusampler --cpusampler.ShowTiers=true \
  ../warmup_test.lox > baseline_warmup.txt

# Capture date
date > baseline_timestamp.txt
git rev-parse HEAD > baseline_commit.txt

cd ..
```

### 2. Verify Test Suite

```bash
# Ensure all tests pass before starting
./mvnw clean test

# Expected: All tests GREEN
# If any failures, fix before proceeding
```

### 3. Create Working Branch

```bash
# Create main optimization branch
git checkout -b performance-optimizations
git push -u origin performance-optimizations

# Tag current state
git tag baseline-before-optimizations
git push origin baseline-before-optimizations
```

### 4. Document Environment

```bash
# Capture environment details
cat > performance_baselines/environment.txt << 'ENV'
Java Version:
$(java -version 2>&1)

GraalVM Version:
$(./mvnw --version)

OS:
$(uname -a)

CPU:
$(sysctl -n machdep.cpu.brand_string 2>/dev/null || cat /proc/cpuinfo | grep "model name" | head -1)

Memory:
$(sysctl hw.memsize 2>/dev/null || free -h)
ENV
```

---

## Phase 1A: Boxing Elimination Configuration (P0)

### Objective
Add `double.class` to `boxingEliminationTypes` to enable automatic boxing elimination in the bytecode interpreter.

### Expected Impact
- **Interpreter**: 4-5x faster
- **Warmup**: 2-3x faster
- **Short programs**: 3-5x faster
- **Overall**: 1.5-2x benchmark improvement

### Time Estimate
- Implementation: 5 minutes
- Testing: 30 minutes
- Total: **35 minutes**

### Implementation Steps

#### Step 1A.1: Create Feature Branch

```bash
git checkout performance-optimizations
git checkout -b p0-p2-trivial-fixes
```

**Note**: Combining both trivial fixes (P0 + P2) in single branch for efficiency

#### Step 1A.2: Modify Boxing Configuration

**File**: `src/main/java/de/hpi/swa/lox/bytecode/LoxBytecodeRootNode.java`

**Change Line 52:**

From:
```java
boxingEliminationTypes = { long.class }, // BUG? boolean.class
```

To:
```java
boxingEliminationTypes = { long.class, double.class },
```

**Full Context:**
```java
@GenerateBytecode(
    languageClass = LoxLanguage.class,
    enableMaterializedLocalAccesses = true,
    boxingEliminationTypes = { long.class, double.class },  // ← CHANGED
    enableUncachedInterpreter = true,
    enableSerialization = true,
    enableRootTagging = true,
    enableRootBodyTagging = false,
    enableTagInstrumentation = true
)
public abstract class LoxBytecodeRootNode extends LoxRootNode implements BytecodeRootNode {
```

#### Step 1A.3: Fix Function Call Boundary (P2 - Quick Win)

**IMPORTANT**: Before rebuilding, let's add the second trivial fix to the same commit

**File**: `src/main/java/de/hpi/swa/lox/runtime/data/LoxFunction.java`

**Change Line 70 (remove annotation):**

From:
```java
@TruffleBoundary
public Object[] createArguments(Object[] userArguments) {
    Object[] result = new Object[userArguments.length + 1];
    System.arraycopy(userArguments, 0, result, 1, userArguments.length);
    result[0] = this;
    return result;
}
```

To:
```java
public Object[] createArguments(Object[] userArguments) {
    Object[] result = new Object[userArguments.length + 1];
    System.arraycopy(userArguments, 0, result, 1, userArguments.length);
    result[0] = this;
    return result;
}
```

**Why combine these fixes?**
- Both are 1-line changes
- Both are low-risk
- Combined testing is more efficient
- Single commit = atomic fix

#### Step 1A.4: Rebuild

```bash
./mvnw clean package

# Expected: BUILD SUCCESS
# Bytecode DSL will regenerate LoxBytecodeRootNodeGen with double boxing elimination
```

#### Step 1A.5: Run Tests

```bash
./mvnw test

# Expected: All tests pass
# If failures, investigate before proceeding
```

#### Step 1A.6: Verify Boxing Performance

```bash
# Arithmetic benchmark
cat > arithmetic_bench.lox << 'BENCH'
var sum = 0.0;
for (var i = 0.0; i < 100000.0; i = i + 1.0) {
    sum = sum + i * 2.0 - i + i / 2.0;
}
print sum;
BENCH

# Measure improvement
echo "=== Before Fix ===" > phase1_results.txt
cat performance_baselines/baseline_warmup.txt >> phase1_results.txt

echo "" >> phase1_results.txt
echo "=== After Fix ===" >> phase1_results.txt
./lox --cpusampler --cpusampler.ShowTiers=true warmup_test.lox >> phase1_results.txt

# Check T0 (interpreter) time reduction
# Expected: 4-5x faster interpreter time
```

#### Step 1A.7: Verify Function Call Performance

```bash
# Function-heavy benchmark
cat > function_benchmark.lox << 'BENCH'
fun add(a, b) {
    return a + b;
}

var sum = 0;
for (var i = 0; i < 100000; i = i + 1) {
    sum = add(sum, i);
}
print sum;
BENCH

echo "=== Function Call Performance ===" >> phase1_results.txt
time ./lox function_benchmark.lox >> phase1_results.txt 2>&1

# Expected: 10-20% improvement on top of boxing fix
```

#### Step 1A.8: Benchmark Suite

```bash
echo "=== Phase 1: Benchmark Results ===" > phase1_benchmarks.txt

./lox harness.lox queens 10 8 >> phase1_benchmarks.txt
echo "" >> phase1_benchmarks.txt

./lox harness.lox permute 10 6 >> phase1_benchmarks.txt
echo "" >> phase1_benchmarks.txt

./lox harness.lox towers 10 13 >> phase1_benchmarks.txt

# Compare with baseline
# Expected: 1.3-1.8x faster overall
```

#### Step 1A.9: Performance Warnings Check

```bash
./lox --experimental-options --compiler.TracePerformanceWarnings=all \
  harness.lox queens 5 8 > phase1_warnings.txt

# Expected: Still ZERO warnings
grep "perf warn" phase1_warnings.txt || echo "No warnings - GOOD!"
```

#### Step 1A.10: Commit Both Fixes

```bash
git add src/main/java/de/hpi/swa/lox/bytecode/LoxBytecodeRootNode.java
git add src/main/java/de/hpi/swa/lox/runtime/data/LoxFunction.java

git commit -m "perf: Two trivial high-impact optimizations

Fix #1 (P0): Add double.class to boxing elimination configuration
- Enables automatic boxing elimination for LoxNumber in bytecode interpreter
- Reduces allocation overhead for arithmetic operations in Tier 0
- Expected improvement: 4-5x faster interpreter

Fix #2 (P2): Remove @TruffleBoundary from createArguments()
- Enables escape analysis on function argument arrays
- Allows inlining of small functions
- Expected improvement: 10-20% faster function calls

Combined expected improvement: 1.5-2x overall benchmark performance

Issues: #1 (P0), #3 (P2)"

git push origin p0-p2-trivial-fixes
```

#### Step 1A.11: Merge to Main Branch

```bash
git checkout performance-optimizations
git merge p0-p2-trivial-fixes --no-ff
git tag phase1-complete
git push origin performance-optimizations
git push origin phase1-complete
```

**Note**: Phase 1 now includes both trivial fixes (P0 + P2) for maximum quick wins

### Success Criteria

- ✅ BUILD SUCCESS
- ✅ All tests pass
- ✅ T0 (interpreter) time reduced by 4-5x (boxing fix)
- ✅ Function calls 10-20% faster (boundary removal)
- ✅ Benchmark suite 1.5-2x faster (combined effect)
- ✅ Zero performance warnings
- ✅ No memory increase in compiled code

### Rollback Procedure

If issues arise:
```bash
git checkout performance-optimizations
git revert HEAD
# Or
git reset --hard baseline-before-optimizations
./mvnw clean package
```

---

## Phase 2: GlobalObject Refactoring (P1)

### Objective
Replace HashMap-based GlobalObject with DynamicObject to eliminate @TruffleBoundary overhead on all global variable access.

### Expected Impact
- **Global access**: 3.3x faster
- **Programs with globals**: 2-3x faster
- **Benchmark suite**: Additional 1.2-1.5x (on top of Phase 1)

### Time Estimate
- Implementation: 2-3 hours
- Testing: 1-2 hours
- Total: **3-5 hours**

### Implementation Steps

#### Step 2.1: Create Feature Branch

```bash
git checkout performance-optimizations
git checkout -b p1-globalobject-refactor
```

#### Step 2.2: Refactor GlobalObject Class

**File**: `src/main/java/de/hpi/swa/lox/runtime/data/GlobalObject.java`

**Replace entire file with:**

```java
package de.hpi.swa.lox.runtime.data;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.object.Shape;

/**
 * Storage of global variables using DynamicObject for optimal performance.
 * Replaces HashMap-based implementation to eliminate @TruffleBoundary overhead
 * and enable full Truffle optimization (type specialization, constant folding, etc.).
 */
@ExportLibrary(InteropLibrary.class)
public class GlobalObject extends DynamicObject implements TruffleObject {
    
    private static final Shape GLOBAL_SHAPE = Shape.newBuilder().build();
    
    public GlobalObject() {
        super(GLOBAL_SHAPE);
    }
    
    // InteropLibrary messages for polyglot interoperability
    
    @ExportMessage
    boolean hasMembers() {
        return true;
    }
    
    @ExportMessage
    Object getMembers(@SuppressWarnings("unused") boolean includeInternal) {
        // Return array of global variable names
        DynamicObjectLibrary lib = DynamicObjectLibrary.getUncached();
        Object[] keys = lib.getKeyArray(this);
        return new LoxArray(keys);
    }
    
    @ExportMessage
    boolean isMemberReadable(String member,
            @CachedLibrary("this") DynamicObjectLibrary objectLibrary) {
        return objectLibrary.containsKey(this, member);
    }
    
    @ExportMessage
    Object readMember(String member,
            @CachedLibrary("this") DynamicObjectLibrary objectLibrary)
            throws UnknownIdentifierException {
        
        Object value = objectLibrary.getOrDefault(this, member, null);
        if (value == null) {
            throw UnknownIdentifierException.create(member);
        }
        return value;
    }
    
    @ExportMessage
    boolean isMemberModifiable(String member,
            @CachedLibrary("this") DynamicObjectLibrary objectLibrary) {
        return objectLibrary.containsKey(this, member);
    }
    
    @ExportMessage
    boolean isMemberInsertable(String member,
            @CachedLibrary("this") DynamicObjectLibrary objectLibrary) {
        return !objectLibrary.containsKey(this, member);
    }
    
    @ExportMessage
    void writeMember(String member, Object value,
            @CachedLibrary("this") DynamicObjectLibrary objectLibrary) {
        objectLibrary.put(this, member, value);
    }
}
```

#### Step 2.3: Update Bytecode Operations for Global Variables

**File**: `src/main/java/de/hpi/swa/lox/bytecode/LoxBytecodeRootNode.java`

Find the global read/write operations and update them to use DynamicObjectLibrary.

**Example Update (search for existing operations and modify):**

```java
@Operation
public static final class LoxReadGlobal {
    @Specialization
    static Object doRead(
            String name,
            @Bind Node node,
            @Bind("$root.getContext()") LoxContext context,
            @CachedLibrary(limit = "3") DynamicObjectLibrary objectLibrary) {
        
        GlobalObject globals = context.getGlobalObject();
        Object value = objectLibrary.getOrDefault(globals, name, Nil.INSTANCE);
        return value;
    }
}

@Operation  
public static final class LoxWriteGlobal {
    @Specialization
    static Object doWrite(
            String name,
            Object value,
            @Bind Node node,
            @Bind("$root.getContext()") LoxContext context,
            @CachedLibrary(limit = "3") DynamicObjectLibrary objectLibrary) {
        
        GlobalObject globals = context.getGlobalObject();
        objectLibrary.put(globals, name, value);
        return value;
    }
}
```

**Note**: The exact operation names and locations may vary. Search for global variable operations in the file and update accordingly.

#### Step 2.4: Update LoxContext if Needed

**File**: `src/main/java/de/hpi/swa/lox/runtime/LoxContext.java`

Verify `getGlobalObject()` returns the correct type:

```java
public GlobalObject getGlobalObject() {
    return globalObject;  // Ensure type is GlobalObject
}
```

#### Step 2.5: Rebuild

```bash
./mvnw clean package

# Expected: BUILD SUCCESS
# May see additional Truffle DSL generation for DynamicObjectLibrary caching
```

#### Step 2.6: Run Tests

```bash
./mvnw test

# Expected: All tests pass
# Focus on global variable tests
```

#### Step 2.7: Create Global Variable Test

```bash
cat > global_test_suite.lox << 'TEST'
// Test 1: Basic read/write
var test1 = 42;
print test1;  // Expect: 42

// Test 2: Overwrite
var test2 = "hello";
test2 = "world";
print test2;  // Expect: world

// Test 3: Type changes
var test3 = 100;
test3 = "string";
test3 = true;
print test3;  // Expect: true

// Test 4: Many globals
var g1 = 1;
var g2 = 2;
var g3 = 3;
var g4 = 4;
var g5 = 5;
print g1 + g2 + g3 + g4 + g5;  // Expect: 15

// Test 5: Globals in loops
var counter = 0;
for (var i = 0; i < 10; i = i + 1) {
    counter = counter + 1;
}
print counter;  // Expect: 10

print "All global variable tests passed!";
TEST

./lox global_test_suite.lox
# Verify all outputs match expectations
```

#### Step 2.8: Performance Verification

```bash
# Create global-heavy benchmark
cat > global_benchmark.lox << 'BENCH'
var counter = 0;
var sum = 0;

for (var i = 0; i < 100000; i = i + 1) {
    counter = counter + 1;
    sum = sum + i;
}
print sum;
BENCH

echo "=== Phase 2: Global Performance ===" > phase2_global_perf.txt
time ./lox global_benchmark.lox >> phase2_global_perf.txt 2>&1

# Compare with baseline (should be 2-3x faster)
```

#### Step 2.9: Benchmark Suite

```bash
echo "=== Phase 2: Benchmark Results ===" > phase2_benchmarks.txt

./lox harness.lox queens 10 8 >> phase2_benchmarks.txt
echo "" >> phase2_benchmarks.txt

./lox harness.lox permute 10 6 >> phase2_benchmarks.txt
echo "" >> phase2_benchmarks.txt

./lox harness.lox towers 10 13 >> phase2_benchmarks.txt

# Compare with phase1
# Expected: Additional 1.2-1.5x improvement (cumulative 1.8-2.5x vs baseline)
```

#### Step 2.10: Verify No Warnings

```bash
./lox --experimental-options --compiler.TracePerformanceWarnings=all \
  harness.lox queens 5 8 > phase2_warnings.txt

grep "perf warn" phase2_warnings.txt || echo "No warnings - GOOD!"
```

#### Step 2.11: Commit

```bash
git add src/main/java/de/hpi/swa/lox/runtime/data/GlobalObject.java
git add src/main/java/de/hpi/swa/lox/bytecode/LoxBytecodeRootNode.java
git add src/main/java/de/hpi/swa/lox/runtime/LoxContext.java  # if modified

git commit -m "perf: Replace GlobalObject HashMap with DynamicObject

- Eliminates @TruffleBoundary on all global variable access
- Enables type specialization and constant folding for globals
- Uses DynamicObjectLibrary with @Cached for optimal performance
- Expected improvement: 3.3x faster global access

Changes:
- GlobalObject now extends DynamicObject instead of using HashMap
- Updated global read/write operations to use DynamicObjectLibrary
- Added InteropLibrary messages for polyglot compatibility

Issue: #2 (P1 - GlobalObject Refactoring)"

git push origin p1-globalobject-refactor
```

#### Step 2.12: Merge to Main Branch

```bash
git checkout performance-optimizations
git merge p1-globalobject-refactor --no-ff
git tag phase2-complete
git push origin performance-optimizations
git push origin phase2-complete
```

### Success Criteria

- ✅ BUILD SUCCESS
- ✅ All tests pass
- ✅ Global variable test suite passes
- ✅ Global access 2-3x faster
- ✅ Benchmark suite additional 1.2-1.5x improvement
- ✅ Zero performance warnings
- ✅ Polyglot interop still works

### Rollback Procedure

```bash
git checkout performance-optimizations
git revert HEAD
# Or
git reset --hard phase1-complete
./mvnw clean package
```

### Potential Issues and Solutions

| Issue | Solution |
|-------|----------|
| Cache limit exceeded | Increase @CachedLibrary limit parameter |
| Interop tests fail | Verify @ExportMessage implementations |
| Performance regression | Check DynamicObjectLibrary is cached properly |
| Test failures | Verify GlobalObject initialization in LoxContext |

---

## Phase 1B: Function Call Optimization (P2 - Quick Win)

**NOTE**: This phase has been merged into Phase 1A for efficiency. Both trivial fixes (P0 boxing + P2 function boundary) are now implemented together in a single branch `p0-p2-trivial-fixes`.

See Phase 1A for combined implementation steps.

**Rationale**:
- Both are 1-line changes
- Both are low-risk, high-impact
- Combined testing is more efficient
- Developers get immediate 1.5-2x wins in <1 hour

---

## Phase 3: Array Iterator Optimization (P3)

### Objective
Replace Java ListIterator with custom LoxArrayIterator to eliminate virtual calls and enable inlining in for-of loops.

### Expected Impact
- **For-of loops**: 5-7x faster
- **Programs using for-of**: Significant improvement
- **Benchmark suite**: Minimal (benchmarks don't use for-of)

### Time Estimate
- Implementation: 1-2 hours
- Testing: 1 hour
- Total: **2-3 hours**

### Implementation Steps

#### Step 3.1: Create Feature Branch

```bash
git checkout performance-optimizations
git checkout -b p3-array-iterator-optimization
```

#### Step 3.2: Create Custom Iterator

**File**: `src/main/java/de/hpi/swa/lox/runtime/data/LoxArray.java`

**Add nested class:**

```java
/**
 * Custom iterator for LoxArray that avoids virtual calls and enables optimization.
 * Replaces Java's ListIterator to allow Truffle to inline hasNext() and next().
 */
public static final class LoxArrayIterator {
    private final Object[] array;
    private final int length;
    private int index;
    
    public LoxArrayIterator(Object[] array) {
        this.array = array;
        this.length = array.length;
        this.index = 0;
        skipNulls();
    }
    
    public boolean hasNext() {
        return index < length;
    }
    
    public Object next() {
        Object value = array[index++];
        skipNulls();
        return value;
    }
    
    private void skipNulls() {
        while (index < length && array[index] == null) {
            index++;
        }
    }
}
```

#### Step 4.3: Replace buildListIterator Method

**In same file, replace:**

From:
```java
@TruffleBoundary
private ListIterator<Object> buildListIterator() {
    return Arrays.asList(innerArray)
            .stream()
            .filter(element -> element != null)
            .toList().listIterator();
}
```

To:
```java
public LoxArrayIterator createIterator() {
    return new LoxArrayIterator(innerArray);
}
```

#### Step 4.4: Update Bytecode Operations for For-Of Loops

**File**: `src/main/java/de/hpi/swa/lox/bytecode/LoxBytecodeRootNode.java`

Find array iteration operations and update to use `LoxArrayIterator` instead of `ListIterator`.

**Search for** operations related to array iteration (may be named differently):

Update to use the custom iterator and remove any @TruffleBoundary annotations.

#### Step 4.5: Rebuild

```bash
./mvnw clean package

# Expected: BUILD SUCCESS
```

#### Step 4.6: Run Tests

```bash
./mvnw test

# Expected: All tests pass
# Focus on array iteration tests
```

#### Step 4.7: Create Array Iteration Tests

```bash
cat > array_iteration_test_suite.lox << 'TEST'
// Test 1: Empty array
var empty = 👉👈;
for (var elem of empty) {
    print "Should not print";
}
print "Test 1 passed";

// Test 2: Single element
var single = 👉42👈;
for (var elem of single) {
    print elem;  // Expect: 42
}

// Test 3: Multiple elements
var multi = 👉1, 2, 3, 4, 5👈;
var sum = 0;
for (var elem of multi) {
    sum = sum + elem;
}
print sum;  // Expect: 15

// Test 4: Sparse array
var sparse = 👉👈;
sparse👉0👈 = 1;
sparse👉5👈 = 2;
sparse👉10👈 = 3;

var sparseSum = 0;
for (var elem of sparse) {
    sparseSum = sparseSum + elem;
}
print sparseSum;  // Expect: 6

// Test 5: Large array
var large = 👉👈;
for (var i = 0; i < 1000; i = i + 1) {
    large👉i👈 = i;
}

var largeSum = 0;
for (var elem of large) {
    largeSum = largeSum + elem;
}
print largeSum;  // Expect: 499500

print "All array iteration tests passed!";
TEST

./lox array_iteration_test_suite.lox
# Verify all outputs correct
```

#### Step 4.8: Performance Verification

```bash
# For-of benchmark
cat > array_iteration_benchmark.lox << 'BENCH'
var arr = 👉👈;
for (var i = 0; i < 10000; i = i + 1) {
    arr👉i👈 = i;
}

var sum = 0;
for (var elem of arr) {
    sum = sum + elem;
}
print sum;
BENCH

echo "=== Phase 4: Array Iteration Performance ===" > phase4_array_perf.txt
time ./lox array_iteration_benchmark.lox >> phase4_array_perf.txt 2>&1

# Expected: 5-7x faster than baseline
```

#### Step 4.9: Verify Inlining

```bash
./lox --experimental-options --engine.TraceInlining array_iteration_benchmark.lox > phase4_inlining.txt

# Look for hasNext/next inlining
grep -i "hasNext\|next" phase4_inlining.txt
```

#### Step 4.10: Benchmark Suite

```bash
echo "=== Phase 4: Benchmark Results ===" > phase4_benchmarks.txt

./lox harness.lox queens 10 8 >> phase4_benchmarks.txt
echo "" >> phase4_benchmarks.txt

./lox harness.lox permute 10 6 >> phase4_benchmarks.txt
echo "" >> phase4_benchmarks.txt

./lox harness.lox towers 10 13 >> phase4_benchmarks.txt

# Minimal change expected (benchmarks don't use for-of)
```

#### Step 4.11: Commit

```bash
git add src/main/java/de/hpi/swa/lox/runtime/data/LoxArray.java
git add src/main/java/de/hpi/swa/lox/bytecode/LoxBytecodeRootNode.java  # if modified

git commit -m "perf: Replace ListIterator with custom LoxArrayIterator

- Eliminates virtual calls in for-of loops
- Enables inlining of hasNext() and next()
- Simpler implementation without Stream API overhead
- Expected improvement: 5-7x faster for-of loops

Changes:
- Added LoxArrayIterator nested class in LoxArray
- Replaced buildListIterator() with createIterator()
- Updated for-of loop bytecode operations

Issue: #4 (P3 - Array Iterator Optimization)"

git push origin p3-array-iterator-optimization
```

#### Step 4.12: Merge to Main Branch

```bash
git checkout performance-optimizations
git merge p3-array-iterator-optimization --no-ff
git tag phase4-complete
git push origin performance-optimizations  
git push origin phase4-complete
```

### Success Criteria

- ✅ BUILD SUCCESS
- ✅ All tests pass
- ✅ Array iteration tests pass
- ✅ For-of loops 5-7x faster
- ✅ Inlining trace shows hasNext/next inlined
- ✅ Zero performance warnings
- ✅ No regression in for-in loops

### Rollback Procedure

```bash
git checkout performance-optimizations
git revert HEAD
# Or
git reset --hard phase3-complete
./mvnw clean package
```

---

## Post-Implementation Validation

### Final Comprehensive Testing

```bash
# 1. Full test suite
./mvnw clean test

# 2. All benchmarks
echo "=== FINAL BENCHMARK RESULTS ===" > final_benchmarks.txt
echo "" >> final_benchmarks.txt

./lox harness.lox queens 20 8 >> final_benchmarks.txt
echo "" >> final_benchmarks.txt

./lox harness.lox permute 20 6 >> final_benchmarks.txt
echo "" >> final_benchmarks.txt

./lox harness.lox towers 20 13 >> final_benchmarks.txt
echo "" >> final_benchmarks.txt

./lox harness.lox list 20 18 >> final_benchmarks.txt

# 3. Performance warnings
./lox --experimental-options --compiler.TracePerformanceWarnings=all \
  harness.lox queens 10 8 > final_warnings.txt

grep "perf warn" final_warnings.txt || echo "✅ No warnings!"

# 4. CPU profiling
./lox --cpusampler --cpusampler.ShowTiers=true \
  harness.lox queens 5 8 > final_cpu_profile.txt
```

### Comparison Report

```bash
cat > PERFORMANCE_COMPARISON.md << 'REPORT'
# Performance Optimization Results

## Baseline vs Final

### Benchmark Suite

| Benchmark | Baseline (μs) | Final (μs) | Improvement |
|-----------|--------------|------------|-------------|
| Queens (×10, n=8) | [from baseline] | [from final] | Xx |
| Permute (×10, n=6) | [from baseline] | [from final] | Xx |
| Towers (×10, n=13) | [from baseline] | [from final] | Xx |
| List (×10, n=18) | [from baseline] | [from final] | Xx |

### Tier Distribution

| Tier | Baseline Time | Final Time | Improvement |
|------|--------------|------------|-------------|
| T0 (Interpreter) | [baseline] | [final] | Xx |
| T1 (Basic JIT) | [baseline] | [final] | Xx |
| T2 (Optimized) | [baseline] | [final] | Xx |

### Specific Improvements

- **Arithmetic operations**: X-Xx faster (interpreter)
- **Global variable access**: X-Xx faster
- **Function calls**: X-X.Xx faster
- **For-of loops**: X-Xx faster

### Overall Assessment

- **Short programs** (<1s): Xx faster
- **Medium programs** (1-10s): Xx faster  
- **Long programs** (>10s): Xx faster
- **Average improvement**: Xx

## Success Metrics

- ✅/❌ All tests pass
- ✅/❌ Zero performance warnings
- ✅/❌ 1.5-2.5x benchmark improvement
- ✅/❌ No memory regressions

## Next Steps

[Any follow-up work needed]
REPORT

# Fill in actual numbers from baseline and final benchmarks
```

### Documentation Updates

```bash
# Update README or performance docs
cat >> README.md << 'DOC'

## Recent Performance Improvements

As of [DATE], the following optimizations have been implemented:

1. **Boxing Elimination** (Phase 1): Added `double.class` to bytecode DSL configuration
   - 4-5x faster interpreter for arithmetic operations
   
2. **GlobalObject Refactoring** (Phase 2): Replaced HashMap with DynamicObject
   - 3.3x faster global variable access
   
3. **Function Call Optimization** (Phase 3): Removed @TruffleBoundary from argument creation
   - 10-20% faster function calls
   
4. **Array Iterator** (Phase 4): Custom iterator for for-of loops
   - 5-7x faster for-of iteration

**Total improvement**: 1.5-2.5x on benchmark suite, 3-10x for short programs

See `PERFORMANCE_ISSUES_COMPREHENSIVE.md` for detailed analysis.
DOC
```

---

## Rollback Procedures

### Complete Rollback to Baseline

```bash
# Reset to before any optimizations
git checkout performance-optimizations
git reset --hard baseline-before-optimizations
./mvnw clean package
./mvnw test
```

### Partial Rollback (Remove One Phase)

```bash
# Example: Remove Phase 4 only
git checkout performance-optimizations
git reset --hard phase3-complete
./mvnw clean package
./mvnw test
```

### Selective Revert

```bash
# Revert specific commit
git checkout performance-optimizations
git log --oneline  # Find commit hash
git revert <commit-hash>
./mvnw clean package
./mvnw test
```

---

## Performance Monitoring

### Continuous Monitoring

```bash
# Create monitoring script
cat > monitor_performance.sh << 'SCRIPT'
#!/bin/bash

echo "=== Performance Monitor ==="
echo "Date: $(date)"
echo "Commit: $(git rev-parse --short HEAD)"
echo ""

echo "Running benchmark suite..."
./lox harness.lox queens 10 8 | grep "average:"
./lox harness.lox permute 10 6 | grep "average:"
./lox harness.lox towers 10 13 | grep "average:"

echo ""
echo "Checking for performance warnings..."
./lox --experimental-options --compiler.TracePerformanceWarnings=all \
  harness.lox queens 5 8 2>&1 | grep "perf warn" | wc -l | \
  xargs -I {} echo "Warning count: {}"

echo ""
echo "=== Monitor Complete ==="
SCRIPT

chmod +x monitor_performance.sh
```

### Regression Detection

```bash
# Run after each commit
./monitor_performance.sh > performance_log_$(date +%Y%m%d_%H%M%S).txt

# Compare with previous runs to detect regressions
```

---

## Timeline and Resource Allocation

### Estimated Total Time

| Phase | Implementation | Testing | Total |
|-------|---------------|---------|-------|
| Pre-Implementation | - | 30 min | 30 min |
| Phase 1A (P0 - Boxing) | 5 min | 30 min | 35 min |
| Phase 1B (P2 - Function) | 5 min | 10 min | 15 min |
| Phase 2 (P1) | 2-3 hrs | 1-2 hrs | 3-5 hrs |
| Phase 3 (P3) | 1-2 hrs | 1 hr | 2-3 hrs |
| Post-Implementation | - | 1 hr | 1 hr |
| **Total** | **3-5 hrs** | **4-5 hrs** | **7-10 hrs** |

### Recommended Schedule

**Day 1 - Quick Wins + Major Fix**:
- Pre-implementation (30 min)
- Phase 1A: Boxing elimination - CRITICAL (35 min)
- Phase 1B: Function call optimization - TRIVIAL (15 min)
- Phase 2: GlobalObject refactoring - MAJOR (3-5 hrs)

**Day 2 - Final Polish**:
- Phase 3: Array iterator optimization (2-3 hrs)
- Post-implementation validation (1 hr)

**Total**: 1-2 days of focused work

**Strategy**: Group trivial fixes together for quick wins, then tackle complex refactorings

---

## Risk Assessment and Mitigation

| Risk | Probability | Impact | Mitigation |
|------|------------|--------|------------|
| Test failures after Phase 2 | Medium | High | Thorough testing of DynamicObject integration |
| Performance regression | Low | High | Benchmark after each phase, rollback if needed |
| Merge conflicts | Low | Low | Work on dedicated branch per phase |
| Build failures | Low | Medium | Clean build before each phase |
| Unexpected behavior changes | Low | High | Comprehensive test suite coverage |

---

## Conclusion

This implementation plan provides a systematic, low-risk approach to implementing all identified performance optimizations. By following the phased approach and validating after each step, we can ensure:

- ✅ No functional regressions
- ✅ Measurable performance improvements
- ✅ Ability to rollback if needed
- ✅ Comprehensive testing and validation

**Expected final result**: 1.5-2.5x faster benchmark suite, 3-10x faster short programs, with zero optimization barriers in compiled code.
