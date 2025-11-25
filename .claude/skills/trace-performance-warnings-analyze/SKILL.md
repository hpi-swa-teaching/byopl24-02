---
name: Run and Analyze Performance Warning Tracer
description: Detects optimization barriers during compilation. Use FIRST when optimizing - identifies virtual calls, non-constant stores, unresolved type checks, and TruffleBoundary issues that prevent peak performance. Reports exact source locations with stack traces. Essential for eliminating compilation warnings that block full optimization.
---

# Skill: Run and Analyze Performance Warning Tracer

This skill runs the Performance Warning tracer on a Lox program and provides detailed analysis of optimization barriers to help achieve peak compiled performance.

## What This Skill Does

1. **Runs Performance Warning Tracer**: Executes the Lox program with warning detection enabled
2. **Analyzes Optimization Barriers**: Interprets warnings to identify:
   - Virtual calls that can't be inlined
   - Type checks that can't be resolved
   - Non-constant store locations
   - Frame merge incompatibilities
   - Unnecessarily complex trivial operations
   - Compilation bailouts
3. **Provides Fix Recommendations**: Suggests specific code changes to eliminate barriers

## Critical Understanding: Use This FIRST!

**Performance warnings identify WHY optimization fails**

**Workflow order**:
1. ✅ **First**: Enable performance warnings → See WHAT blocks optimization
2. ✅ **Second**: Use TraceInlining → Verify fixes work
3. ✅ **Third**: Use CPU Sampler → Measure actual performance impact

**Why first**:
- Identifies exact optimization barriers
- Provides actionable source locations
- Pinpoints specific AST nodes causing problems
- More targeted than generic profiling

## Six Warning Types

### 1. Virtual Runtime Calls (`call`)
```
[engine] perf warn myFunction |Partial evaluation could not inline the virtual runtime call Virtual to HotSpotMethod<SomeClass.method()>
```
**Problem**: Method call can't be statically resolved
**Impact**: Call overhead remains in compiled code
**Common causes**: Polymorphism, interface calls, non-constant references

### 2. Unresolved Type Checks (`instanceof`)
```
[engine] perf warn myFunction |Partial evaluation could not resolve virtual instanceof to an exact type
```
**Problem**: Type remains polymorphic
**Impact**: Runtime type checks required
**Common causes**: Multiple types through same path, insufficient guards

### 3. Non-Constant Store Locations (`store`)
```
[engine] perf warn myFunction |Store location argument is not a partial evaluation constant
```
**Problem**: Frame slot or property location computed at runtime
**Impact**: Dynamic lookup required, can't optimize to direct memory access
**Common causes**: Computed frame slots, variable property names

### 4. Frame Merge Issues (`frame_merge`)
```
[engine] perf warn myFunction |Frame merge with incompatible frame states
```
**Problem**: Control flow paths have incompatible frame layouts
**Impact**: Additional overhead merging frame states
**Common causes**: Complex control flow, frame descriptor inconsistencies

### 5. Trivial Code Complexity (`trivial`)
```
[engine] perf warn myFunction |Unnecessarily complex trivial operation
```
**Problem**: Simple operation implemented in complex way
**Impact**: Missed optimization opportunities
**Common causes**: Over-abstraction, unnecessary indirection

### 6. Compilation Bailouts (`bailout`)
```
[engine] perf warn myFunction |Compilation bailed out
```
**Problem**: Compiler gave up on compiling this method
**Impact**: Method stays in interpreter mode
**Common causes**: Excessive complexity, unsupported patterns

## When to Use This Skill

- **Starting optimization work** (use FIRST!)
- Investigating slow compiled performance
- Developing new language features
- Code review for performance regressions
- Teaching Truffle best practices
- Understanding why compiled code is slow
- Validating optimization fixes

## Prerequisites

Before running this skill, you should know:
- The path to the Lox program to analyze
- Ideally, profiling results showing hot functions
- Whether to focus on specific methods

## How the Skill Works

The skill follows this workflow:

### 1. Initial Setup
- Confirms the Lox program path and arguments
- Determines whether to enable all warnings or specific types
- Optionally sets up method filtering

### 2. Run Performance Warning Tracer

#### All Warnings (Recommended for Initial Analysis)
```bash
./lox --compiler.TracePerformanceWarnings=all program.lox
```
- Enables all warning types
- Comprehensive coverage
- Best for initial investigation

#### Specific Warning Types
```bash
./lox --compiler.TracePerformanceWarnings=call,instanceof,store program.lox
```
- Focus on specific issues
- Reduces output volume
- Use after identifying problem categories

#### With Compilation Trace (Recommended!)
```bash
./lox --experimental-options \
  --compiler.TracePerformanceWarnings=all \
  --engine.TraceCompilation \
  program.lox
```
- Correlate warnings with compilation events
- See which compilations trigger warnings
- Essential for complete picture

#### Focused on Specific Method
```bash
./lox --experimental-options \
  --compiler.TracePerformanceWarnings=all \
  --engine.CompileOnly=problematicFunction \
  program.lox
```
- Dramatically reduces output
- Focus on known problem areas
- Ideal for targeted fixes

### 3. Understand Output Format

#### Warning Structure
```
[engine] perf warn myFunction |Partial evaluation could not inline the virtual runtime call Virtual to HotSpotMethod<ProfileNode.profile(Object)> (167|MethodCallTarget).

Approximated stack trace for [167 | MethodCallTarget]:
  at MyLanguageNode.execute(MyLanguageNode.java:42)
  at CallerNode.execute(CallerNode.java:18)
  at RootNode.execute(RootNode.java:5)
```

**Field Breakdown**:
- **[engine] perf warn**: Warning identifier
- **myFunction**: Compilation unit (root method being compiled)
- **Message**: Describes what optimization failed
- **(167|MethodCallTarget)**: Graal IR node ID (searchable in IGV)
- **Stack trace**: Path through Truffle AST nodes
- **File:line**: Source locations (when available)

### 4. Identify and Fix Common Problems

#### Problem 1: Virtual Call to Profile Objects
```
[engine] perf warn hotFunction |Partial evaluation could not inline the virtual runtime call Virtual to HotSpotMethod<ConditionProfile.profile(boolean)>
```

**Root Cause**: Profile object not a compilation constant

**Example Issue**:
```java
// In Truffle language implementation:
class MyNode extends Node {
    private ConditionProfile profile;  // ❌ Not final, not constant!

    @Override
    public Object execute(VirtualFrame frame) {
        if (profile.profile(condition)) {
            // ...
        }
    }
}
```

**Resolution**:
```java
class MyNode extends Node {
    // ✅ Cache profile as compilation constant
    @Override
    public Object execute(VirtualFrame frame,
            @Cached("createBinaryProfile()") ConditionProfile profile) {
        if (profile.profile(condition)) {
            // ...
        }
    }
}
```

**For Lox Users**: This is typically a language implementation issue, not user code

**Verification**:
```bash
./lox --experimental-options \
  --compiler.TracePerformanceWarnings=call \
  --engine.TraceInlining \
  program.lox
```
Should see profile method inlined after fix

#### Problem 2: Polymorphic Type Checks
```
[engine] perf warn calculate |Partial evaluation could not resolve virtual instanceof to an exact type
```

**Root Cause**: Multiple types flow through same code path

**Example Issue (Lox code)**:
```lox
fun calculate(value) {
  // Sometimes receives int, sometimes double, sometimes string
  // Compiler can't resolve to exact type
  if (isNumber(value)) {
    return value + 1;
  }
  return 0;
}
```

**Impact**: Runtime type checks remain in compiled code

**Resolution (Language Implementation)**:
```java
// Add type-specific specializations
@Specialization
int doInt(int value) { return value + 1; }

@Specialization
double doDouble(double value) { return value + 1; }

@Specialization(guards = "isString(value)")
int doString(Object value) { return 0; }
```

**For Lox Users**: Avoid mixing types in hot paths
```lox
// ❌ BAD: Mixing types
for (var i = 0; i < 1000; i = i + 1) {
  calculate(i);        // int
  calculate(i + 0.5);  // double
  calculate("test");   // string
}

// ✅ GOOD: Consistent types
for (var i = 0; i < 1000; i = i + 1) {
  calculate(i);  // Always int
}
```

#### Problem 3: Non-Constant Frame Slots
```
[engine] perf warn accessVar |Store location argument is not a partial evaluation constant
```

**Root Cause**: Frame slot computed at runtime

**Example Issue (Language Implementation)**:
```java
// ❌ BAD: Dynamic frame slot lookup
public Object execute(VirtualFrame frame) {
    String varName = computeVariableName();  // Runtime computation
    FrameSlot slot = frame.getFrameDescriptor().findFrameSlot(varName);
    return frame.getValue(slot);
}
```

**Resolution**:
```java
// ✅ GOOD: Constant frame slot
public class ReadLocalNode extends Node {
    private final FrameSlot slot;  // Compilation constant!

    public ReadLocalNode(FrameSlot slot) {
        this.slot = slot;  // Set during parsing
    }

    public Object execute(VirtualFrame frame) {
        return frame.getValue(slot);  // Direct access
    }
}
```

**For Lox Users**: This is language implementation issue

#### Problem 4: Compilation Bailout
```
[engine] perf warn complexFunction |Compilation bailed out
```

**Root Cause**: Method too complex for compiler

**Common Causes**:
- Excessive inlining (compilation unit too large)
- Unbounded loops without compilation-final loop counts
- Recursive structures without termination guarantees
- Exceeding compiler complexity limits

**Resolution**:
1. **Check compilation trace**:
   ```bash
   ./lox --experimental-options \
     --engine.TraceCompilation \
     --compiler.TracePerformanceWarnings=bailout \
     program.lox
   ```

2. **Use TraceInlining** to see if inlining is excessive

3. **Consider refactoring** large functions into smaller pieces

4. **Mark boundaries** for unavoidable complexity:
   ```java
   @TruffleBoundary
   public void complexOperation() {
       // Complex but infrequent operation
   }
   ```

### 5. Workflow Integration

#### Step 1: Profile to Find Hot Functions
```bash
./lox --cpusampler \
  --cpusampler.ShowTiers=true \
  --cpusampler.Delay=5000 \
  program.lox > cpu.txt
```

#### Step 2: Check for Warnings in Hot Functions
```bash
./lox --compiler.TracePerformanceWarnings=all \
  program.lox 2>&1 | tee warnings.txt

# Check if hot functions have warnings
grep "hotFunction" warnings.txt
```

#### Step 3: Analyze Warnings
```bash
# Count warnings per function
grep "perf warn" warnings.txt | \
  sed 's/.*perf warn \([^ ]*\).*/\1/' | \
  sort | uniq -c | sort -rn
```

#### Step 4: Focus on Problematic Functions
```bash
./lox --experimental-options \
  --compiler.TracePerformanceWarnings=all \
  --engine.CompileOnly=problematicFunction \
  program.lox
```

#### Step 5: Fix Issues (see Problem patterns above)

#### Step 6: Verify Fixes
```bash
# Re-run warnings check
./lox --compiler.TracePerformanceWarnings=all \
  program.lox 2>&1 | grep "problematicFunction"

# Should see fewer or no warnings

# Verify with inlining trace
./lox --experimental-options \
  --engine.TraceInlining \
  --engine.CompileOnly=problematicFunction \
  program.lox
```

#### Step 7: Measure Performance Impact
```bash
# Benchmark before and after
time ./lox program.lox  # Before fixes
# Apply fixes
time ./lox program.lox  # After fixes - should be faster
```

## Best Practices

The skill follows these analysis best practices:

### 1. Use Performance Warnings First
```bash
# Start optimization workflow here
./lox --compiler.TracePerformanceWarnings=all program.lox
```
- Most targeted diagnostic
- Identifies exact problems
- More actionable than general profiling

### 2. Combine with Profiling
```bash
# Identify hot + warnings
./lox --cpusampler program.lox > cpu.txt
./lox --compiler.TracePerformanceWarnings=all program.lox > warn.txt

# Find hot functions with warnings
```
- Focus on hot paths only
- Warnings in cold code don't matter

### 3. Use CompileOnly to Reduce Noise
```bash
./lox --experimental-options \
  --compiler.TracePerformanceWarnings=all \
  --engine.CompileOnly=hotFunction \
  program.lox
```
- Dramatically reduces output
- Focus on known problems

### 4. Correlate with Compilation Trace
```bash
./lox --experimental-options \
  --compiler.TracePerformanceWarnings=all \
  --engine.TraceCompilation \
  program.lox 2>&1 | tee full-trace.log
```
- See if warnings cause compilation failures
- Understand compilation context

### 5. Verify Fixes with TraceInlining
```bash
# After fixing call warnings
./lox --experimental-options \
  --compiler.TracePerformanceWarnings=call \
  --engine.TraceInlining \
  program.lox
```
- Confirm methods now inline
- Validate optimization effectiveness

### 6. Don't Over-Optimize
- Not all warnings need fixing
- Focus on hot paths only
- Some patterns legitimately can't optimize (I/O, etc.)
- Measure performance impact

## Common Pitfalls to Avoid

The skill warns about these mistakes:

- ❌ **Over-using @TruffleBoundary**:
  - Prevents inlining of beneficial methods
  - Use only for truly non-optimizable code (I/O, etc.)

- ❌ **Fixing cold code warnings**:
  - Code that never compiles doesn't matter
  - Focus on hot paths only

- ❌ **Not measuring performance**:
  - Fixing warnings doesn't guarantee speedup
  - Always benchmark before/after

- ❌ **Misinterpreting approximated stack traces**:
  - Stack traces are approximations
  - Use IGV for precise node identification

- ❌ **Combining too many diagnostics**:
  - Overwhelming output obscures relevant info
  - Add options progressively

- ❌ **Assuming all warnings must be fixed**:
  - Some language features inherently trigger warnings
  - Document known acceptable warnings

## Typical Workflow

The skill typically follows this workflow:

### Step 1: Baseline Profiling
```bash
./lox --cpusampler --cpusampler.ShowTiers=true \
  --cpusampler.Delay=5000 \
  program.lox > cpu.txt
```
**Identify**: Top 3-5 hot functions

### Step 2: Check for Warnings
```bash
./lox --compiler.TracePerformanceWarnings=all \
  program.lox 2>&1 | tee warnings.txt
```
**Look for**: Warnings in hot functions

### Step 3: Count Warnings per Function
```bash
grep "perf warn" warnings.txt | \
  sed 's/.*perf warn \([^ ]*\).*/\1/' | \
  sort | uniq -c | sort -rn
```
**Identify**: Functions with most warnings

### Step 4: Analyze Specific Warnings
```bash
grep "perf warn hotFunction" warnings.txt
```
**Understand**: What optimization is blocked

### Step 5: Implement Fixes
- Virtual calls → Cache profiles, add specializations
- Type checks → Add type-specific specializations
- Store locations → Use compilation-constant frame slots
- Bailouts → Simplify, add boundaries

### Step 6: Verify Fixes
```bash
# No more warnings for this function
./lox --compiler.TracePerformanceWarnings=all \
  --engine.CompileOnly=hotFunction \
  program.lox

# Methods now inline
./lox --experimental-options \
  --engine.TraceInlining \
  --engine.CompileOnly=hotFunction \
  program.lox
```

### Step 7: Measure Impact
```bash
# Benchmark
time ./lox program.lox  # Should be faster
```

## Success Criteria

**Good optimization**:
- ✅ Zero warnings in hot paths
- ✅ Critical methods show successful inlining
- ✅ Measurable performance improvement
- ✅ No compilation bailouts for hot code

**Acceptable**:
- ✅ Warnings in cold/rare paths (don't matter)
- ✅ Warnings for legitimate I/O operations
- ✅ Documented known acceptable warnings

**Problems**:
- 🚨 Warnings in hot functions (high CPU time)
- 🚨 Compilation bailouts for hot code
- 🚨 Many warnings of same type
- 🚨 Warnings prevent inlining of critical calls

## Reference Documentation

For detailed information, see:
- `/Users/antonykamp/Projects/hpi-ma/byopl24-02/docs/commands/trace-performance-warnings.md` - Complete documentation
- Official GraalVM docs: https://www.graalvm.org/latest/graalvm-as-a-platform/language-implementation-framework/Optimizing/
- Truffle options: https://www.graalvm.org/latest/graalvm-as-a-platform/language-implementation-framework/Options/

## Implementation Notes

This skill:
- Uses the Lox launcher: `./lox`
- Flag: `--compiler.TracePerformanceWarnings=all`
- May require `--experimental-options` in some versions
- Outputs to stdout with `[engine] perf warn` prefix
- Minimal overhead (only during compilation)
- Should be used FIRST in optimization workflow
- Focus on hot paths identified by profiling
- Always measure performance impact of fixes
- Emphasizes: **Eliminate optimization barriers in hot code**
