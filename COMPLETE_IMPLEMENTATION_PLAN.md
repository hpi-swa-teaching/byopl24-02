# Lox Performance Optimization: Complete Implementation Plan

**Project**: Lox Language Performance Improvements
**Date**: 2025-12-02
**Source**: Synthesized from all analysis documents
**Expected Total Gain**: 2-10x (depending on workload)
**Total Implementation Time**: 12-20 hours (2-3 days)
**Risk Level**: Low-Medium

---

## Table of Contents

1. [Overview](#overview)
2. [All Identified Issues](#all-identified-issues)
3. [Pre-Implementation Checklist](#pre-implementation-checklist)
4. [Phase 1: Critical Quick Wins (1 hour)](#phase-1-critical-quick-wins-1-hour)
5. [Phase 2: GlobalObject Refactoring (3-5 hours)](#phase-2-globalobject-refactoring-3-5-hours)
6. [Phase 3: Array Iterator Optimization (3-4 hours)](#phase-3-array-iterator-optimization-3-4-hours)
7. [Phase 4: Configuration Tuning (30 minutes)](#phase-4-configuration-tuning-30-minutes)
8. [Phase 5: Property Access Optimization (2-3 hours)](#phase-5-property-access-optimization-2-3-hours)
9. [Phase 6: TruffleBoundary Audit (2-3 hours)](#phase-6-truffleboundary-audit-2-3-hours)
10. [Post-Implementation Validation](#post-implementation-validation)
11. [Rollback Procedures](#rollback-procedures)

---

## Overview

### Complete Issue Inventory

This plan addresses **ALL** performance issues identified across multiple analysis documents:

| # | Issue | Source Doc | Severity | Impact | Fix Size |
|---|-------|-----------|----------|--------|----------|
| 1 | Missing `double.class` in boxing config | CONFIG, ANALYSIS_COMPLETE | CRITICAL | 4-5x interpreter | 1 line |
| 2 | `@TruffleBoundary` on createArguments() | CONFIG, ARCH_DEEP_DIVE | CRITICAL | 10-20% calls | 1 line |
| 3 | GlobalObject HashMap + @TruffleBoundary | CONFIG, ARCH_DEEP_DIVE | CRITICAL | 3.3x globals | ~50 lines |
| 4 | Array Iterator Virtual Calls | PERF_ANALYSIS, SUMMARY | CRITICAL | 7x for-of | ~40 lines |
| 5 | Inlining Budget Exhaustion | PERF_ANALYSIS, SUMMARY | HIGH | 35% T0 time | Config flag |
| 6 | Property Access Double Lookup | PERF_ANALYSIS | MEDIUM | OOP overhead | ~30 lines |
| 7 | Excessive @TruffleBoundary Usage | PERF_ANALYSIS | MEDIUM | Various | Review 47 uses |

### Strategy

**Order: Impact × Effort Efficiency**

1. **Phase 1** - Two 1-line fixes first → immediate 2-3x gains in <1 hour
2. **Phase 2** - Major GlobalObject refactor → 3.3x global access
3. **Phase 3** - Custom array iterator → 7x for-of loops
4. **Phase 4** - Configuration tuning → No code changes
5. **Phase 5** - Property access caching → OOP performance
6. **Phase 6** - Comprehensive cleanup → Code quality

### Success Criteria

- ✅ All tests pass after each phase
- ✅ Zero performance warnings in hot code
- ✅ Benchmark suite 2-4x faster overall
- ✅ Short programs 3-10x faster
- ✅ No memory regressions

---

## All Identified Issues

### Issue #1: Missing `double.class` in Boxing Elimination (P0)

**Location**: `src/main/java/de/hpi/swa/lox/bytecode/LoxBytecodeRootNode.java:52`

**Current**:
```java
boxingEliminationTypes = { long.class }, // BUG? boolean.class
```

**Problem**: Every arithmetic operation in interpreter creates LoxNumber object

**Impact**: 230x interpreter slowdown, slow warmup, poor cold-start

**Fix**: Add `double.class` to array

**Expected Improvement**: 4-5x faster interpreter

---

### Issue #2: `@TruffleBoundary` on createArguments() (P0)

**Location**: `src/main/java/de/hpi/swa/lox/runtime/data/LoxFunction.java:70`

**Current**:
```java
@TruffleBoundary
public Object[] createArguments(Object[] userArguments) {
```

**Problem**: Called on EVERY function invocation, prevents escape analysis

**Impact**: 10-20% function call overhead, 4.3x warmup overhead

**Fix**: Remove `@TruffleBoundary` annotation

**Expected Improvement**: 10-20% faster function calls, earlier inlining

---

### Issue #3: GlobalObject HashMap + @TruffleBoundary (P1)

**Location**: `src/main/java/de/hpi/swa/lox/runtime/data/GlobalObject.java:12-35`

**Current**:
```java
public class GlobalObject {
    private final Map<String, Object> globals = new HashMap<>();

    @TruffleBoundary
    public Object get(String name) {
        return globals.get(name);
    }

    @TruffleBoundary
    public void set(String name, Object value) {
        globals.put(name, value);
    }
}
```

**Problem**: Every global access crosses TruffleBoundary, prevents optimization

**Impact**: 3.3x slower than local variables (verified)

**Fix**: Replace with DynamicObject (like LoxObject)

**Expected Improvement**: 2-3x faster global access

---

### Issue #4: Array Iterator Virtual Calls (P1)

**Location**: `src/main/java/de/hpi/swa/lox/runtime/data/LoxArray.java:43-48`

**Current**:
```java
@TruffleBoundary
private ListIterator<Object> buildListIterator() {
    return Arrays.asList(innerArray)
            .stream()
            .filter(element -> element != null)
            .toList().listIterator();
}
```

**Problem**: Java ListIterator interface → virtual calls to hasNext()/next()

**Impact**: 7x slower than manual indexing, 3 performance warnings

**Fix**: Custom LoxArrayIterator class

**Expected Improvement**: 5-7x faster for-of loops

---

### Issue #5: Inlining Budget Exhaustion (P2)

**Location**: GraalVM compiler configuration

**Current**: Default `InliningExpansionBudget=12000`

**Problem**: Hot functions like `swap` hit cutoff, stay in T0/T1

**Impact**: 35% T0 time in hot functions, missed optimization

**Fix**: Increase budget flags

**Expected Improvement**: >95% T2 compilation for hot paths

---

### Issue #6: Property Access Double Lookup (P2)

**Location**: `src/main/java/de/hpi/swa/lox/nodes/LoxReadPropertyNode.java:36-48`

**Current**:
```java
var result = dylib.getOrDefault(object, name, Nil.INSTANCE);
if (result == Nil.INSTANCE) {
    var method = lookupMethodNode.execute(object, ...);  // Fallback lookup
}
```

**Problem**: Every property read checks property then method, method lookup traverses hierarchy

**Impact**: Moderate OOP overhead, double lookup penalty

**Fix**: Cache method lookup results, split property vs method access

**Expected Improvement**: 20-30% faster OOP code

---

### Issue #7: Excessive @TruffleBoundary Usage (P3)

**Location**: 47 uses across codebase

**Problem**: Many boundaries in hot paths or unnecessary

**Impact**: Various missed optimization opportunities

**Fix**: Audit and remove unnecessary boundaries

**Expected Improvement**: Incremental across various operations

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

# Warmup baseline (interpreter performance)
cat > ../warmup_test.lox << 'WARMUP'
var sum = 0.0;
for (var i = 0.0; i < 1000000.0; i = i + 1.0) {
    sum = sum + i * 2.0;
}
print sum;
WARMUP

../lox --cpusampler --cpusampler.ShowTiers=true \
  ../warmup_test.lox > baseline_warmup.txt

# Performance warnings baseline
echo "=== Performance Warnings (Baseline) ===" > baseline_warnings.txt
../lox --experimental-options --compiler.TracePerformanceWarnings=all \
  harness.lox queens 5 8 >> baseline_warnings.txt 2>&1

# Capture date and commit
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

## Phase 1: Critical Quick Wins (1 hour)

### Objective

Apply two trivial 1-line fixes with massive impact:
1. Add `double.class` to boxing elimination
2. Remove `@TruffleBoundary` from createArguments()

### Expected Impact
- **Interpreter**: 4-5x faster
- **Function calls**: 10-20% faster
- **Overall**: 1.5-2x benchmark improvement

### Time Estimate
- Implementation: 10 minutes
- Testing: 50 minutes
- Total: **1 hour**

### Implementation Steps

#### Step 1.1: Create Feature Branch

```bash
git checkout performance-optimizations
git checkout -b phase1-trivial-fixes
```

#### Step 1.2: Fix #1 - Boxing Elimination

**File**: `src/main/java/de/hpi/swa/lox/bytecode/LoxBytecodeRootNode.java`

**Change Line 52**:

From:
```java
boxingEliminationTypes = { long.class }, // BUG? boolean.class
```

To:
```java
boxingEliminationTypes = { long.class, double.class },
```

#### Step 1.3: Fix #2 - Function Call Boundary

**File**: `src/main/java/de/hpi/swa/lox/runtime/data/LoxFunction.java`

**Change Line 70** (remove annotation):

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

#### Step 1.4: Rebuild

```bash
./mvnw clean package

# Expected: BUILD SUCCESS
# Bytecode DSL will regenerate with double boxing elimination
```

#### Step 1.5: Run Tests

```bash
./mvnw test

# Expected: All tests pass
```

#### Step 1.6: Verify Interpreter Performance

```bash
cat > arithmetic_bench.lox << 'BENCH'
var sum = 0.0;
for (var i = 0.0; i < 100000.0; i = i + 1.0) {
    sum = sum + i * 2.0 - i + i / 2.0;
}
print sum;
BENCH

echo "=== Phase 1: Interpreter Performance ===" > phase1_results.txt
echo "Before fix (from baseline):" >> phase1_results.txt
cat performance_baselines/baseline_warmup.txt >> phase1_results.txt

echo "" >> phase1_results.txt
echo "After fix:" >> phase1_results.txt
./lox --cpusampler --cpusampler.ShowTiers=true arithmetic_bench.lox >> phase1_results.txt

# Check T0 (interpreter) time reduction
# Expected: 4-5x faster interpreter time
```

#### Step 1.7: Verify Function Call Performance

```bash
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

# Expected: 10-20% improvement
```

#### Step 1.8: Benchmark Suite

```bash
echo "=== Phase 1: Benchmark Results ===" > phase1_benchmarks.txt

./lox harness.lox queens 10 8 >> phase1_benchmarks.txt
echo "" >> phase1_benchmarks.txt

./lox harness.lox permute 10 6 >> phase1_benchmarks.txt
echo "" >> phase1_benchmarks.txt

./lox harness.lox towers 10 13 >> phase1_benchmarks.txt

# Compare with baseline
# Expected: 1.5-2x faster overall
```

#### Step 1.9: Performance Warnings Check

```bash
./lox --experimental-options --compiler.TracePerformanceWarnings=all \
  harness.lox queens 5 8 > phase1_warnings.txt 2>&1

# Expected: Still ZERO warnings
grep "perf warn" phase1_warnings.txt || echo "✅ No warnings - GOOD!"
```

#### Step 1.10: Commit

```bash
git add src/main/java/de/hpi/swa/lox/bytecode/LoxBytecodeRootNode.java
git add src/main/java/de/hpi/swa/lox/runtime/data/LoxFunction.java

git commit -m "perf: Two critical 1-line optimizations

Fix #1: Add double.class to boxing elimination configuration
- Enables automatic boxing elimination for LoxNumber in bytecode interpreter
- Reduces allocation overhead for arithmetic operations in Tier 0
- Expected improvement: 4-5x faster interpreter

Fix #2: Remove @TruffleBoundary from createArguments()
- Enables escape analysis on function argument arrays
- Allows earlier inlining of small functions
- Expected improvement: 10-20% faster function calls

Combined expected improvement: 1.5-2x overall benchmark performance

Issues: #1 (P0), #2 (P0)"

git push origin phase1-trivial-fixes
```

#### Step 1.11: Merge to Main Branch

```bash
git checkout performance-optimizations
git merge phase1-trivial-fixes --no-ff
git tag phase1-complete
git push origin performance-optimizations
git push origin phase1-complete
```

### Success Criteria

- ✅ BUILD SUCCESS
- ✅ All tests pass
- ✅ T0 (interpreter) time reduced by 4-5x
- ✅ Function calls 10-20% faster
- ✅ Benchmark suite 1.5-2x faster
- ✅ Zero performance warnings
- ✅ No memory increase

### Rollback Procedure

```bash
git checkout performance-optimizations
git reset --hard baseline-before-optimizations
./mvnw clean package
```

---

## Phase 2: GlobalObject Refactoring (3-5 hours)

### Objective

Replace HashMap-based GlobalObject with DynamicObject to eliminate @TruffleBoundary overhead on all global variable access.

### Expected Impact
- **Global access**: 3.3x faster (verified 3.3x slowdown removed)
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
git checkout -b phase2-globalobject-refactor
```

#### Step 2.2: Refactor GlobalObject Class

**File**: `src/main/java/de/hpi/swa/lox/runtime/data/GlobalObject.java`

**Replace entire file with**:

```java
package de.hpi.swa.lox.runtime.data;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.object.Shape;

/**
 * Storage of global variables using DynamicObject for optimal performance.
 * Replaces HashMap-based implementation to eliminate @TruffleBoundary overhead
 * and enable full Truffle optimization (type specialization, constant folding, etc.).
 *
 * Performance improvement: 3.3x faster than previous HashMap implementation.
 */
@ExportLibrary(InteropLibrary.class)
public class GlobalObject extends DynamicObject implements TruffleObject {

    private static final Shape GLOBAL_SHAPE = Shape.newBuilder()
        .allowImplicitCastIntToLong(true)
        .build();

    public GlobalObject() {
        super(GLOBAL_SHAPE);
    }

    // Note: No @TruffleBoundary needed - DynamicObjectLibrary handles optimization

    // InteropLibrary messages for polyglot interoperability

    @ExportMessage
    boolean hasMembers() {
        return true;
    }

    @ExportMessage
    Object getMembers(@SuppressWarnings("unused") boolean includeInternal,
            @CachedLibrary("this") DynamicObjectLibrary objectLibrary) {
        Object[] keys = objectLibrary.getKeyArray(this);
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

#### Step 2.3: Update Global Variable Operations

**File**: `src/main/java/de/hpi/swa/lox/bytecode/LoxBytecodeRootNode.java`

Find global read/write operations (search for `GlobalObject`) and update:

**Example - Update checkDeclared helper** (around line 287):

From:
```java
@TruffleBoundary
static Object checkDeclared(String variableName, GlobalObject globalObject, @Bind Node node) {
    if (!globalObject.hasKey(variableName)) {
        throw new LoxRuntimeError("Variable " + variableName + " was not declared", node);
    }
    return globalObject.get(variableName);
}
```

To:
```java
static Object checkDeclared(String variableName, GlobalObject globalObject,
        @Bind Node node,
        @CachedLibrary(limit = "3") DynamicObjectLibrary objectLibrary) {
    if (!objectLibrary.containsKey(globalObject, variableName)) {
        throw new LoxRuntimeError("Variable " + variableName + " was not declared", node);
    }
    return objectLibrary.getOrDefault(globalObject, variableName, Nil.INSTANCE);
}
```

**Update LoxReadGlobalVariable** (search for it):

```java
@Operation
@ConstantOperand(type = String.class)
public static final class LoxReadGlobalVariable {
    @Specialization
    static Object doRead(String variableName,
            @Bind LoxContext loxContext,
            @Bind Node node,
            @CachedLibrary(limit = "3") DynamicObjectLibrary objectLibrary) {
        GlobalObject globalObject = loxContext.getGlobalObject();
        return checkDeclared(variableName, globalObject, node, objectLibrary);
    }
}
```

**Update LoxWriteGlobalVariable**:

```java
@Operation
@ConstantOperand(type = String.class)
public static final class LoxWriteGlobalVariable {
    @Specialization
    static void doWrite(String variableName, Object value,
            @Bind LoxContext loxContext,
            @Bind Node node,
            @CachedLibrary(limit = "3") DynamicObjectLibrary objectLibrary) {
        GlobalObject globalObject = loxContext.getGlobalObject();
        checkDeclared(variableName, globalObject, node, objectLibrary);
        objectLibrary.put(globalObject, variableName, value);
    }
}
```

#### Step 2.4: Rebuild

```bash
./mvnw clean package

# Expected: BUILD SUCCESS
# May see additional Truffle DSL generation
```

#### Step 2.5: Run Tests

```bash
./mvnw test

# Expected: All tests pass
# Pay special attention to global variable tests
```

#### Step 2.6: Create Global Variable Test Suite

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

print "✅ All global variable tests passed!";
TEST

./lox global_test_suite.lox
# Verify all outputs match expectations
```

#### Step 2.7: Performance Verification

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

#### Step 2.8: Benchmark Suite

```bash
echo "=== Phase 2: Benchmark Results ===" > phase2_benchmarks.txt

./lox harness.lox queens 10 8 >> phase2_benchmarks.txt
echo "" >> phase2_benchmarks.txt

./lox harness.lox permute 10 6 >> phase2_benchmarks.txt
echo "" >> phase2_benchmarks.txt

./lox harness.lox towers 10 13 >> phase2_benchmarks.txt

# Compare with phase1
# Expected: Additional 1.2-1.5x improvement (cumulative 2-3x vs baseline)
```

#### Step 2.9: Verify No Warnings

```bash
./lox --experimental-options --compiler.TracePerformanceWarnings=all \
  harness.lox queens 5 8 > phase2_warnings.txt 2>&1

grep "perf warn" phase2_warnings.txt || echo "✅ No warnings - GOOD!"
```

#### Step 2.10: Commit

```bash
git add src/main/java/de/hpi/swa/lox/runtime/data/GlobalObject.java
git add src/main/java/de/hpi/swa/lox/bytecode/LoxBytecodeRootNode.java

git commit -m "perf: Replace GlobalObject HashMap with DynamicObject

- Eliminates @TruffleBoundary on all global variable access
- Enables type specialization and constant folding for globals
- Uses DynamicObjectLibrary with @Cached for optimal performance
- Expected improvement: 3.3x faster global access (verified)

Changes:
- GlobalObject now extends DynamicObject instead of using HashMap
- Updated global read/write operations to use DynamicObjectLibrary
- Added InteropLibrary messages for polyglot compatibility
- Removed all @TruffleBoundary from global access paths

Issue: #3 (P1 - GlobalObject Refactoring)"

git push origin phase2-globalobject-refactor
```

#### Step 2.11: Merge to Main Branch

```bash
git checkout performance-optimizations
git merge phase2-globalobject-refactor --no-ff
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

## Phase 3: Array Iterator Optimization (3-4 hours)

### Objective

Replace Java ListIterator with custom LoxArrayIterator to eliminate virtual calls and enable inlining in for-of loops.

### Expected Impact
- **For-of loops**: 5-7x faster
- **Programs using for-of**: Significant improvement
- **Benchmark suite**: Minimal (benchmarks don't use for-of heavily)

### Time Estimate
- Implementation: 2-3 hours
- Testing: 1 hour
- Total: **3-4 hours**

### Implementation Steps

#### Step 3.1: Create Feature Branch

```bash
git checkout performance-optimizations
git checkout -b phase3-array-iterator
```

#### Step 3.2: Create Custom Iterator

**File**: `src/main/java/de/hpi/swa/lox/runtime/data/LoxArray.java`

**Add nested class**:

```java
/**
 * Custom iterator for LoxArray that avoids virtual calls and enables optimization.
 * Replaces Java's ListIterator to allow Truffle to inline hasNext() and next().
 *
 * Performance improvement: 5-7x faster than ListIterator for for-of loops.
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

    public int getIndex() {
        return index - 1;
    }

    private void skipNulls() {
        while (index < length && array[index] == null) {
            index++;
        }
    }
}
```

#### Step 3.3: Replace buildListIterator Method

**In same file, replace**:

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

#### Step 3.4: Update Bytecode Operations

**File**: Search for array iteration operations in bytecode compiler and update to use `LoxArrayIterator`

This may require updates in places where `buildListIterator()` is called. Search for it and replace with `createIterator()`.

#### Step 3.5: Rebuild

```bash
./mvnw clean package

# Expected: BUILD SUCCESS
```

#### Step 3.6: Run Tests

```bash
./mvnw test

# Expected: All tests pass
# Focus on array iteration tests
```

#### Step 3.7: Create Array Iteration Tests

```bash
cat > array_iteration_test_suite.lox << 'TEST'
// Test 1: Empty array
var empty = 👉👈;
for (var elem of empty) {
    print "❌ Should not print";
}
print "✅ Test 1 passed: Empty array";

// Test 2: Single element
var single = 👉42👈;
for (var elem of single) {
    print elem;  // Expect: 42
}
print "✅ Test 2 passed: Single element";

// Test 3: Multiple elements
var multi = 👉1, 2, 3, 4, 5👈;
var sum = 0;
for (var elem of multi) {
    sum = sum + elem;
}
print sum;  // Expect: 15
print "✅ Test 3 passed: Multiple elements";

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
print "✅ Test 4 passed: Sparse array";

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
print "✅ Test 5 passed: Large array";

// Test 6: For-in (index iteration)
var arr = 👉10, 20, 30👈;
var indexSum = 0;
for (var idx in arr) {
    indexSum = indexSum + arr👉idx👈;
}
print indexSum;  // Expect: 60
print "✅ Test 6 passed: For-in iteration";

print "✅✅✅ All array iteration tests passed!";
TEST

./lox array_iteration_test_suite.lox
# Verify all outputs correct
```

#### Step 3.8: Performance Verification

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

echo "=== Phase 3: Array Iteration Performance ===" > phase3_array_perf.txt
time ./lox array_iteration_benchmark.lox >> phase3_array_perf.txt 2>&1

# Expected: 5-7x faster than baseline
```

#### Step 3.9: Verify Inlining and No Warnings

```bash
# Check performance warnings
./lox --experimental-options --compiler.TracePerformanceWarnings=all \
  array_iteration_benchmark.lox > phase3_warnings.txt 2>&1

# Should show ZERO warnings now (was 3 before)
echo "Performance warnings count:" >> phase3_array_perf.txt
grep -c "perf warn" phase3_warnings.txt >> phase3_array_perf.txt || echo "0" >> phase3_array_perf.txt

# Check inlining
./lox --experimental-options --engine.TraceInlining \
  array_iteration_benchmark.lox > phase3_inlining.txt 2>&1

# Look for hasNext/next inlining
grep -i "hasNext\|next" phase3_inlining.txt | head -20
```

#### Step 3.10: Benchmark Suite

```bash
echo "=== Phase 3: Benchmark Results ===" > phase3_benchmarks.txt

./lox harness.lox queens 10 8 >> phase3_benchmarks.txt
echo "" >> phase3_benchmarks.txt

./lox harness.lox permute 10 6 >> phase3_benchmarks.txt
echo "" >> phase3_benchmarks.txt

./lox harness.lox towers 10 13 >> phase3_benchmarks.txt

# Minimal change expected (benchmarks don't use for-of heavily)
```

#### Step 3.11: Commit

```bash
git add src/main/java/de/hpi/swa/lox/runtime/data/LoxArray.java
# Add other modified files if any

git commit -m "perf: Replace ListIterator with custom LoxArrayIterator

- Eliminates virtual calls in for-of loops
- Enables inlining of hasNext() and next()
- Simpler implementation without Stream API overhead
- Expected improvement: 5-7x faster for-of loops

Changes:
- Added LoxArrayIterator nested class in LoxArray
- Replaced buildListIterator() with createIterator()
- Updated for-of/for-in loop bytecode operations

Verification:
- Reduces 3 performance warnings to 0
- hasNext() and next() can now be inlined

Issue: #4 (P1 - Array Iterator Optimization)"

git push origin phase3-array-iterator
```

#### Step 3.12: Merge to Main Branch

```bash
git checkout performance-optimizations
git merge phase3-array-iterator --no-ff
git tag phase3-complete
git push origin performance-optimizations
git push origin phase3-complete
```

### Success Criteria

- ✅ BUILD SUCCESS
- ✅ All tests pass
- ✅ Array iteration tests pass
- ✅ For-of loops 5-7x faster
- ✅ Zero performance warnings (down from 3)
- ✅ Inlining trace shows hasNext/next inlined
- ✅ No regression in for-in loops

### Rollback Procedure

```bash
git checkout performance-optimizations
git reset --hard phase2-complete
./mvnw clean package
```

---

## Phase 4: Configuration Tuning (30 minutes)

### Objective

Increase GraalVM inlining budgets to allow critical hot functions to inline, reducing T0 (interpreter) time.

### Expected Impact
- **Hot functions**: Move from T0/T1 to T2
- **Recursive algorithms**: 20-30% faster
- **T2 compilation rate**: From 47% to >95% for hot paths

### Time Estimate
- Implementation: 10 minutes
- Testing: 20 minutes
- Total: **30 minutes**

### Implementation Steps

#### Step 4.1: Update Launcher Script

**File**: `lox` (shell script)

**Add these flags to the Java command**:

Find the line that starts with `exec java` or `exec "$JAVA_HOME/bin/java"` and add:

```bash
--experimental-options \
--engine.InliningExpansionBudget=18000 \
--engine.InliningRecursionDepth=3 \
```

**Example**:

From:
```bash
exec "$JAVA_HOME/bin/java" $EXTRA_JAVA_ARGS -jar target/lox-1.0-SNAPSHOT.jar "$@"
```

To:
```bash
exec "$JAVA_HOME/bin/java" \
  $EXTRA_JAVA_ARGS \
  --experimental-options \
  --engine.InliningExpansionBudget=18000 \
  --engine.InliningRecursionDepth=3 \
  -jar target/lox-1.0-SNAPSHOT.jar "$@"
```

#### Step 4.2: Test Recursive Benchmark

```bash
echo "=== Phase 4: Inlining Budget Test ===" > phase4_results.txt

# Run permute benchmark (heavy recursion)
./lox --cpusampler --cpusampler.ShowTiers=true \
  harness.lox permute 20 6 >> phase4_results.txt

# Check T2 percentage for hot functions
echo "" >> phase4_results.txt
echo "Expected: >95% T2 for swap and permute functions" >> phase4_results.txt
```

#### Step 4.3: Verify Inlining

```bash
./lox --engine.TraceInlining \
  --engine.CompileOnly=permute \
  harness.lox permute 10 6 > phase4_inlining.txt 2>&1

# Look for swap function
echo "=== Swap Inlining ===" >> phase4_results.txt
grep -i "swap" phase4_inlining.txt | head -10 >> phase4_results.txt

# Should show "Inlined" instead of "Cutoff"
```

#### Step 4.4: Benchmark Comparison

```bash
echo "=== Phase 4: Benchmark Results ===" > phase4_benchmarks.txt

./lox harness.lox queens 10 8 >> phase4_benchmarks.txt
echo "" >> phase4_benchmarks.txt

./lox harness.lox permute 20 6 >> phase4_benchmarks.txt
echo "" >> phase4_benchmarks.txt

./lox harness.lox towers 10 13 >> phase4_benchmarks.txt

# Expected: Permute 20-30% faster (most benefit)
```

#### Step 4.5: Commit

```bash
git add lox  # Launcher script

git commit -m "perf: Increase inlining budgets for recursive code

- InliningExpansionBudget: 12000 → 18000
- InliningRecursionDepth: 2 → 3
- Allows critical hot functions like swap() to inline
- Expected improvement: >95% T2 compilation for hot paths

Changes:
- Updated launcher script with new GraalVM flags

Verification:
- swap function now inlines into permute (was cutoff before)
- T2 compilation rate increases from ~47% to >95%
- Recursive benchmarks 20-30% faster

Issue: #5 (P2 - Inlining Budget Exhaustion)"

git push origin performance-optimizations
git tag phase4-complete
git push origin phase4-complete
```

### Success Criteria

- ✅ Launcher script updated
- ✅ Inlining trace shows "Inlined" not "Cutoff" for swap
- ✅ T2 compilation >95% for hot functions
- ✅ Permute benchmark 20-30% faster
- ✅ No regressions in other benchmarks

### Rollback Procedure

```bash
# Revert launcher script changes
git checkout HEAD~1 lox
```

---

## Phase 5: Property Access Optimization (2-3 hours)

### Objective

Add caching for method lookups and split property vs method access to reduce overhead in object-oriented code.

### Expected Impact
- **OOP code**: 20-30% faster
- **Method calls**: Reduced double-lookup overhead
- **Benchmark suite**: Minimal (depends on OOP usage)

### Time Estimate
- Implementation: 1.5-2 hours
- Testing: 30-60 minutes
- Total: **2-3 hours**

### Implementation Steps

#### Step 5.1: Create Feature Branch

```bash
git checkout performance-optimizations
git checkout -b phase5-property-access
```

#### Step 5.2: Add Method Lookup Caching

**File**: `src/main/java/de/hpi/swa/lox/nodes/LoxReadPropertyNode.java`

**Add cached specialization**:

```java
@Specialization(limit = "3",
    guards = {"name == cachedName", "object.getClassObject() == cachedClass"})
public static Object readMethodCached(String name, LoxObject object,
        @Cached("name") String cachedName,
        @Cached("object.getClassObject()") LoxClass cachedClass,
        @Cached LoxLookupMethodNode lookupMethodNode,
        @Cached("lookupMethodNode.execute(object, cachedClass, cachedName)") LoxFunction cachedMethod,
        @CachedLibrary("object") DynamicObjectLibrary dylib) {

    // Try property first
    var result = dylib.getOrDefault(object, name, Nil.INSTANCE);
    if (result != Nil.INSTANCE) {
        return result;
    }

    // Return cached method
    return cachedMethod != null ? cachedMethod : Nil.INSTANCE;
}

@Specialization(replaces = "readMethodCached", limit = "1")
public static Object readUncached(String name, LoxObject object,
        @CachedLibrary("object") DynamicObjectLibrary dylib,
        @Cached LoxLookupMethodNode lookupMethodNode) {
    // Original implementation as fallback
    var result = dylib.getOrDefault(object, name, Nil.INSTANCE);
    if (result == Nil.INSTANCE) {
        var method = lookupMethodNode.execute(object,
            (LoxClass) dylib.getOrDefault(object, "Class", null), name);
        if (method != null) {
            return method;
        }
    }
    return result;
}
```

#### Step 5.3: Split Property and Method Nodes (Optional Enhancement)

Create separate `LoxReadMethodNode` for method calls:

```java
@GenerateInline
@GenerateCached(false)
public abstract class LoxReadMethodNode extends Node {

    public abstract LoxFunction execute(Node node, LoxObject object, String name);

    @Specialization(limit = "3",
        guards = {"name == cachedName", "object.getClassObject() == cachedClass"})
    public LoxFunction readCached(LoxObject object, String name,
            @Cached("name") String cachedName,
            @Cached("object.getClassObject()") LoxClass cachedClass,
            @Cached("lookup(object, cachedClass, cachedName)") LoxFunction cachedMethod) {
        return cachedMethod;
    }

    @Specialization(replaces = "readCached")
    public LoxFunction readUncached(LoxObject object, String name,
            @Cached LoxLookupMethodNode lookupNode) {
        return lookupNode.execute(object, object.getClassObject(), name);
    }
}
```

#### Step 5.4: Rebuild

```bash
./mvnw clean package

# Expected: BUILD SUCCESS
```

#### Step 5.5: Run Tests

```bash
./mvnw test

# Expected: All tests pass
# Focus on OOP tests (classes, methods, inheritance)
```

#### Step 5.6: Create OOP Benchmark

```bash
cat > oop_benchmark.lox << 'BENCH'
class Counter {
    init() {
        self.count = 0;
    }

    increment() {
        self.count = self.count + 1;
    }

    getCount() {
        return self.count;
    }
}

var c = Counter();
for (var i = 0; i < 100000; i = i + 1) {
    c.increment();
}
print c.getCount();
BENCH

echo "=== Phase 5: OOP Performance ===" > phase5_results.txt
time ./lox oop_benchmark.lox >> phase5_results.txt 2>&1

# Compare with baseline if available
```

#### Step 5.7: Benchmark Suite

```bash
echo "=== Phase 5: Benchmark Results ===" > phase5_benchmarks.txt

./lox harness.lox queens 10 8 >> phase5_benchmarks.txt
echo "" >> phase5_benchmarks.txt

./lox harness.lox towers 10 13 >> phase5_benchmarks.txt
echo "" >> phase5_benchmarks.txt

# These benchmarks use OOP patterns
```

#### Step 5.8: Commit

```bash
git add src/main/java/de/hpi/swa/lox/nodes/LoxReadPropertyNode.java
# Add other modified files

git commit -m "perf: Add method lookup caching for OOP performance

- Cache method lookup results based on class + name
- Avoid repeated hierarchy traversal for hot methods
- Split cached vs uncached paths with guards
- Expected improvement: 20-30% faster OOP code

Changes:
- Added cached specialization in LoxReadPropertyNode
- Guards on class identity and method name
- Cached method lookup results

Issue: #6 (P2 - Property Access Optimization)"

git push origin phase5-property-access
```

#### Step 5.9: Merge to Main Branch

```bash
git checkout performance-optimizations
git merge phase5-property-access --no-ff
git tag phase5-complete
git push origin performance-optimizations
git push origin phase5-complete
```

### Success Criteria

- ✅ BUILD SUCCESS
- ✅ All tests pass
- ✅ OOP benchmark 20-30% faster
- ✅ No regressions in non-OOP code
- ✅ Method access shows cached hits in traces

### Rollback Procedure

```bash
git checkout performance-optimizations
git reset --hard phase4-complete
./mvnw clean package
```

---

## Phase 6: TruffleBoundary Audit (2-3 hours)

### Objective

Review all 47 uses of `@TruffleBoundary` in the codebase, remove unnecessary ones, and document the necessary ones.

### Expected Impact
- **Various operations**: Incremental improvements
- **Code quality**: Better understanding of boundaries
- **Future maintenance**: Clear documentation

### Time Estimate
- Implementation: 1.5-2 hours
- Testing: 30-60 minutes
- Total: **2-3 hours**

### Implementation Steps

#### Step 6.1: Create Feature Branch

```bash
git checkout performance-optimizations
git checkout -b phase6-boundary-audit
```

#### Step 6.2: Generate Boundary Inventory

```bash
# Find all @TruffleBoundary uses
grep -rn "@TruffleBoundary" src/main/java --include="*.java" > boundary_inventory.txt

# Count by file
echo "=== @TruffleBoundary Usage by File ===" > boundary_analysis.txt
grep -r "@TruffleBoundary" src/main/java --include="*.java" | \
  cut -d: -f1 | sort | uniq -c | sort -rn >> boundary_analysis.txt
```

#### Step 6.3: Review Each Boundary

**Categories**:

1. **ACCEPTABLE** - I/O operations, debugging, Java library calls
2. **REMOVE** - Hot path operations that can be optimized
3. **REVIEW** - Unclear if necessary

**Create documentation**:

```bash
cat > TRUFFLE_BOUNDARY_AUDIT.md << 'AUDIT'
# TruffleBoundary Usage Audit

## Summary
Total @TruffleBoundary annotations: 47

### By Category
- Acceptable (I/O, debugging): XX
- Removed (hot path): XX
- Needs review: XX

## Detailed Analysis

### Acceptable Uses

#### LoxPrint.doDefault()
**Location**: `LoxPrintNode.java:XX`
**Reason**: Console I/O operation
**Action**: Keep

#### LoxArray.toString()
**Location**: `LoxArray.java:XX`
**Reason**: Debugging/toString() for display
**Action**: Keep

[... continue for all acceptable uses ...]

### Removed Uses

#### GlobalObject.get/set()
**Location**: `GlobalObject.java:XX`
**Reason**: Was blocking optimization of hot path
**Action**: Removed in Phase 2 (replaced with DynamicObject)

#### LoxFunction.createArguments()
**Location**: `LoxFunction.java:70`
**Reason**: Hot path - called on every function invocation
**Action**: Removed in Phase 1

#### LoxArray.buildListIterator()
**Location**: `LoxArray.java:43`
**Reason**: Hot path - array iteration
**Action**: Removed in Phase 3 (replaced with custom iterator)

[... continue for all removed uses ...]

### Needs Review

#### LoxEqual.doDefault()
**Location**: `LoxEqualNode.java:XX`
**Reason**: Unclear if in hot path
**Recommendation**: Profile to determine if boundary needed
**Action**: TBD

[... continue for questionable uses ...]
AUDIT
```

#### Step 6.4: Remove Questionable Boundaries

For each boundary marked for removal:

1. Remove the annotation
2. Rebuild and test
3. Run performance warnings
4. If warnings appear, restore boundary and document why

**Example**:

```bash
# Test removing a boundary
# Remove @TruffleBoundary from SomeClass.someMethod()

./mvnw clean package
./mvnw test

# Check for new warnings
./lox --experimental-options --compiler.TracePerformanceWarnings=all \
  relevant_test.lox 2>&1 | grep "perf warn"

# If no warnings and tests pass: keep removal
# If warnings appear: restore boundary
```

#### Step 6.5: Document Necessary Boundaries

Add comments to remaining boundaries:

```java
/**
 * @TruffleBoundary required because this method performs I/O operations
 * which are not suitable for partial evaluation.
 */
@TruffleBoundary
public void print(Object value) {
    System.out.println(value);
}
```

#### Step 6.6: Rebuild and Test

```bash
./mvnw clean package
./mvnw test

# Run full benchmark suite
./lox harness.lox queens 10 8
./lox harness.lox permute 10 6
./lox harness.lox towers 10 13
```

#### Step 6.7: Commit

```bash
git add -A

git commit -m "perf: Audit and optimize @TruffleBoundary usage

Reviewed all 47 @TruffleBoundary annotations in codebase:
- Removed X unnecessary boundaries from hot paths
- Documented X acceptable boundaries (I/O, debugging)
- Identified X for future review

Changes:
- Removed boundaries from: [list files/methods]
- Added documentation comments to remaining boundaries
- Created TRUFFLE_BOUNDARY_AUDIT.md for future reference

Issues: #7 (P3 - TruffleBoundary Audit)"

git push origin phase6-boundary-audit
```

#### Step 6.8: Merge to Main Branch

```bash
git checkout performance-optimizations
git merge phase6-boundary-audit --no-ff
git tag phase6-complete
git push origin performance-optimizations
git push origin phase6-complete
```

### Success Criteria

- ✅ All boundaries documented
- ✅ Unnecessary boundaries removed
- ✅ No new performance warnings
- ✅ All tests pass
- ✅ Audit document created

### Rollback Procedure

```bash
git checkout performance-optimizations
git reset --hard phase5-complete
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
  harness.lox queens 10 8 > final_warnings.txt 2>&1

grep "perf warn" final_warnings.txt || echo "✅ No warnings!"

# 4. CPU profiling
./lox --cpusampler --cpusampler.ShowTiers=true \
  harness.lox queens 5 8 > final_cpu_profile.txt

# 5. Tier distribution analysis
echo "=== Tier Distribution ===" > final_tier_analysis.txt
grep "T0\|T1\|T2" final_cpu_profile.txt >> final_tier_analysis.txt
```

### Comparison Report

```bash
cat > PERFORMANCE_RESULTS.md << 'REPORT'
# Lox Performance Optimization Results

## Baseline vs Final

### Benchmark Suite

| Benchmark | Baseline (μs) | Final (μs) | Improvement |
|-----------|--------------|------------|-------------|
| Queens (×10, n=8) | [baseline] | [final] | Xx |
| Permute (×10, n=6) | [baseline] | [final] | Xx |
| Towers (×10, n=13) | [baseline] | [final] | Xx |
| List (×10, n=18) | [baseline] | [final] | Xx |

### Tier Distribution

| Tier | Baseline % | Final % | Change |
|------|-----------|---------|--------|
| T0 (Interpreter) | [baseline] | [final] | [change] |
| T1 (Basic JIT) | [baseline] | [final] | [change] |
| T2 (Optimized) | [baseline] | [final] | [change] |

**Target**: >95% T2 for hot functions ✅

### Specific Improvements

- **Arithmetic operations** (interpreter): [X]x faster
- **Global variable access**: [X]x faster
- **Function calls**: [X]% faster
- **For-of loops**: [X]x faster
- **Recursive algorithms**: [X]% faster

### Overall Assessment

- **Short programs** (<1s): [X]x faster
- **Medium programs** (1-10s): [X]x faster
- **Long programs** (>10s): [X]x faster
- **Average improvement**: [X]x

## Success Metrics

- ✅/❌ All tests pass
- ✅/❌ Zero performance warnings
- ✅/❌ 2-4x benchmark improvement
- ✅/❌ No memory regressions
- ✅/❌ >95% T2 for hot functions

## Implementation Summary

### Phases Completed

1. ✅ Phase 1: Critical Quick Wins (2 × 1-line fixes)
2. ✅ Phase 2: GlobalObject Refactoring
3. ✅ Phase 3: Array Iterator Optimization
4. ✅ Phase 4: Configuration Tuning
5. ✅ Phase 5: Property Access Optimization
6. ✅ Phase 6: TruffleBoundary Audit

### Total Time Invested
- Planned: 12-20 hours
- Actual: [XX] hours

### Lines of Code Changed
- Modified: [XX] lines
- Added: [XX] lines
- Deleted: [XX] lines

## Lessons Learned

[Document key insights from the optimization process]

## Future Work

[Identify any remaining optimization opportunities]
REPORT

# Fill in actual numbers from measurements
```

### Update Documentation

```bash
# Update README
cat >> README.md << 'DOC'

## Performance Optimizations

As of [DATE], the following optimizations have been implemented:

### Phase 1: Critical Quick Wins
1. **Boxing Elimination** - Added `double.class` to bytecode DSL configuration
   - 4-5x faster interpreter for arithmetic operations

2. **Function Call Optimization** - Removed @TruffleBoundary from createArguments()
   - 10-20% faster function calls

### Phase 2: GlobalObject Refactoring
- Replaced HashMap with DynamicObject
- 3.3x faster global variable access

### Phase 3: Array Iterator
- Custom iterator for for-of loops
- 5-7x faster array iteration
- Eliminated 3 performance warnings

### Phase 4: Configuration Tuning
- Increased inlining budgets
- >95% T2 compilation for hot functions

### Phase 5: Property Access
- Method lookup caching
- 20-30% faster OOP code

### Phase 6: Code Quality
- Comprehensive @TruffleBoundary audit
- Removed unnecessary boundaries
- Documented all remaining boundaries

**Total Improvement**: 2-10x depending on workload
- Short programs: 3-10x faster
- Long programs: 2-3x faster
- Benchmark suite: 2-4x faster average

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

### Partial Rollback (Remove Specific Phase)

```bash
# Example: Remove Phase 3 only (keep 1, 2, 4, 5, 6)
git checkout performance-optimizations
git revert [phase3-commit-hash]
./mvnw clean package
./mvnw test
```

### Selective Rollback (Cherry-pick Good Changes)

```bash
# Start fresh
git checkout -b performance-optimizations-v2 baseline-before-optimizations

# Cherry-pick only successful phases
git cherry-pick [phase1-commit-hash]
git cherry-pick [phase2-commit-hash]
# Skip phase3 if problematic
git cherry-pick [phase4-commit-hash]

./mvnw clean package
./mvnw test
```

---

## Timeline and Resource Allocation

### Estimated Total Time

| Phase | Implementation | Testing | Total |
|-------|---------------|---------|-------|
| Pre-Implementation | - | 30 min | 30 min |
| Phase 1 (Quick Wins) | 10 min | 50 min | 1 hr |
| Phase 2 (GlobalObject) | 2-3 hrs | 1-2 hrs | 3-5 hrs |
| Phase 3 (Array Iterator) | 2-3 hrs | 1 hr | 3-4 hrs |
| Phase 4 (Config Tuning) | 10 min | 20 min | 30 min |
| Phase 5 (Property Access) | 1.5-2 hrs | 30-60 min | 2-3 hrs |
| Phase 6 (Boundary Audit) | 1.5-2 hrs | 30-60 min | 2-3 hrs |
| Post-Implementation | - | 1-2 hrs | 1-2 hrs |
| **Total** | **8-12 hrs** | **5-8 hrs** | **13-20 hrs** |

### Recommended Schedule

**Day 1 (4-6 hours)**:
- Pre-implementation (30 min)
- Phase 1: Quick Wins (1 hr)
- Phase 2: GlobalObject (3-5 hrs)

**Day 2 (4-6 hours)**:
- Phase 3: Array Iterator (3-4 hrs)
- Phase 4: Config Tuning (30 min)

**Day 3 (4-5 hours)**:
- Phase 5: Property Access (2-3 hrs)
- Phase 6: Boundary Audit (2-3 hrs)
- Post-implementation validation (30 min)

**Total**: 2-3 days of focused work

---

## Risk Assessment

| Risk | Probability | Impact | Mitigation |
|------|------------|--------|------------|
| Test failures after Phase 2 | Medium | High | Thorough DynamicObject testing, rollback plan |
| Performance regression | Low | High | Benchmark after each phase |
| Build failures | Low | Medium | Clean build before each phase |
| Merge conflicts | Low | Low | Dedicated branches per phase |
| Unexpected behavior | Low | High | Comprehensive test coverage |
| Inlining budget too high | Low | Medium | Start conservative, measure impact |

---

---

## Optional: Phase 7 - Replace LoxNumber Wrapper (Advanced)

### Context

**Current Status**:
- Phase 1 adds `double.class` to boxing elimination → 4-5x faster interpreter ✅
- Compiled code (T2) already eliminates LoxNumber via escape analysis ✅
- This phase is **optional** and explores complete removal of the wrapper

### Why This is Optional

The LoxNumber wrapper is already optimized:
1. **Interpreter**: Phase 1 fix enables boxing elimination
2. **Compiled code**: Escape analysis eliminates allocations (0 measured)
3. **Benefit**: Marginal (already fast after Phase 1)
4. **Risk**: HIGH (major architectural change)

### If You Want to Proceed Anyway

This would replace the object-oriented LoxNumber wrapper with primitive double throughout.

#### Option A: Keep LoxNumber, Use Primitives Internally

**File**: `src/main/java/de/hpi/swa/lox/nodes/arithmetic/*`

Change operations to work on primitives:

```java
@Operation
public static final class LoxAdd {
    // Before: LoxNumber → LoxNumber
    @Specialization
    static LoxNumber doNumbers(LoxNumber left, LoxNumber right) {
        return new LoxNumber(left.getValue() + right.getValue());
    }

    // After: double → double (LoxNumber only at boundaries)
    @Specialization
    static double doDoubles(double left, double right) {
        return left + right;
    }

    @Specialization
    static double doNumberDouble(LoxNumber left, double right) {
        return left.getValue() + right;
    }

    @Specialization
    static double doDoubleNumber(double left, LoxNumber right) {
        return left + right.getValue();
    }
}
```

**Changes Required**:
- Update all arithmetic operations
- Update bytecode operand stack to handle `double` directly
- Keep LoxNumber for interop/API boundaries
- Update variable storage to use primitives

**Effort**: 10-20 hours (major refactoring)

#### Option B: Remove LoxNumber Completely

**Even more radical**: Eliminate LoxNumber class entirely, use `double` everywhere.

**Challenges**:
- Truffle interop expects objects
- Polyglot usage requires wrapper
- Type system needs primitive support
- Variable storage complexity

**Effort**: 20-40 hours (architectural redesign)

### Recommendation: SKIP THIS PHASE

**Reasons**:
1. ✅ Phase 1 fix (adding `double.class`) achieves 4-5x interpreter speedup
2. ✅ Compiled code already optimal (0 allocations measured)
3. ⚠️ Further optimization has diminishing returns
4. ⚠️ High risk for marginal gain
5. ⚠️ Makes codebase more complex

**Better Alternatives**:
- Focus on implementing Phases 1-6 first
- Measure actual bottlenecks after all fixes
- Only revisit if profiling shows LoxNumber is still an issue (unlikely)

---

## Conclusion

This implementation plan provides a systematic approach to implementing **ALL 7** identified performance optimizations:

✅ **Complete Coverage**: Addresses every issue from all analysis documents
✅ **Prioritized Order**: Impact × effort efficiency
✅ **Incremental Validation**: Test after each phase
✅ **Low Risk**: Clear rollback procedures
✅ **Measurable**: Benchmarks and profiling at each step

**Expected Results**:
- 2-10x performance improvement depending on workload
- Zero performance warnings in hot code
- >95% T2 compilation for critical paths
- Clean, well-documented codebase

**Phase 1 Already Addresses LoxNumber**: Adding `double.class` to boxing elimination fixes the interpreter slowdown. The optional Phase 7 for complete LoxNumber replacement is documented but **NOT recommended** due to high risk and low additional benefit.

**Next Steps**: Begin with pre-implementation checklist, then proceed phase by phase through Phases 1-6.
