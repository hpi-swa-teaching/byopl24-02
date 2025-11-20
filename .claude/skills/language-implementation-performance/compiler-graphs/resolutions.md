# Resolutions

This guide provides solutions to common issues identified by compiler-graphs.

## Common Issues and Fixes

### Problem 1: Indirect Calls (Critical!)

```
OptimizedIndirectCallNode found in graph
```

**Root Cause**: No caching of CallTarget

**Fix** (Language Implementation):
```java
// ❌ BAD: Dynamic lookup every time
public Object execute(VirtualFrame frame) {
    CallTarget target = lookupFunction(name);
    return target.call(args);
}

// ✅ GOOD: Cache with @Cached
@Specialization(guards = "function == cachedFunction")
public Object executeCached(VirtualFrame frame,
        @Cached("function") Function cachedFunction,
        @Cached("cachedFunction.getCallTarget()") CallTarget callTarget) {
    return callTarget.call(args);
}
```

**Verification**: Re-dump and check for OptimizedDirectCallNode

### Problem 2: Failed Escape Analysis

```
CommitAllocationNode or NewInstanceNode found after PartialEscape
```

**Root Cause**: Object escapes compilation unit

**Common Causes**:
- Object stored in field visible to other threads
- Object passed to method that doesn't inline
- Object stored in escaping data structure
- Identity operations (synchronization, ==)

**Fix**: Keep object lifetime strictly local
```lox
// ❌ BAD: Object escapes
var temp = Point(x, y);
this.lastPoint = temp;  // Escapes!

// ✅ GOOD: Object stays local
fun calculate(x, y) {
    var temp = Point(x, y);  // Local only
    var result = temp.distance();
    return result;  // Only result escapes
}
```

**Verification**: After PartialEscape phase should show zero allocation nodes

### Problem 3: Boxing/Unboxing

```
BoxNode and UnboxNode found
```

**Root Cause**: Missing primitive specializations

**Fix** (Language Implementation):
```java
// ❌ BAD: Generic Object parameters
@Specialization
Object add(Object left, Object right) { ... }

// ✅ GOOD: Primitive specializations
@Specialization
int add(int left, int right) { return left + right; }

@Specialization
long add(long left, long right) { return left + right; }

@Specialization
double add(double left, double right) { return left + right; }
```

**Verification**: Box/Unbox nodes should disappear

### Problem 4: Deoptimization Nodes

```
DeoptimizeNode or UnreachedNode found in hot path
```

**Root Cause**: Unstable type assumptions

**Fix**: Add proper guards and type specializations

**Correlation**: Use with `--engine.TraceTransferToInterpreter` to find exact location

## Best Practices for Fixes

### 1. Always Use MethodFilter
```bash
# ❌ BAD: Overwhelming output
-Djdk.graal.Dump=Truffle:1

# ✅ GOOD: Focused on problem
-Djdk.graal.Dump=Truffle:1 -Djdk.graal.MethodFilter=*hotFunction*
```

### 2. Start with Level 1
- Level 1: Basic phases (usually sufficient)
- Level 2: All phases (only when needed)
- Level 3+: Rarely useful for Truffle development

### 3. Compress BGV Files
```bash
# BGV files are huge, compress them
gzip compiler_graphs/*.bgv

# bgv2json and seafoam read .bgv.gz natively
```

### 4. Focus on "After TruffleTier"
- Most relevant for language developers
- Shows language-specific optimization effectiveness
- Later phases are generic Graal (less actionable)

### 5. Correlate with Other Tools
```bash
# Run together
EXTRA_JAVA_ARGS="-Djdk.graal.Dump=Truffle:1 ..." \
  ./lox --experimental-options \
  --compiler.TracePerformanceWarnings=all \
  --engine.TraceCompilation \
  program.lox 2>&1 | tee combined.log
```

## Related Documentation

- [Common Patterns](./patterns.md) - Problem identification
- [Analysis Guide](./analysis.md) - Understanding the output
- [Main Workflow](../SKILL.md) - Overall optimization process
