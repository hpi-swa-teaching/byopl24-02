# Resolutions

This guide provides solutions to common issues identified by trace-performance-warnings.

## Common Issues and Fixes

### Problem 1: Virtual Call to Profile Objects

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

### Problem 2: Polymorphic Type Checks

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

### Problem 3: Non-Constant Frame Slots

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

### Problem 4: Compilation Bailout

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

## Best Practices for Fixes

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

## Related Documentation

- [Common Patterns](./patterns.md) - Problem identification
- [Analysis Guide](./analysis.md) - Understanding the output
- [Main Workflow](../SKILL.md) - Overall optimization process
