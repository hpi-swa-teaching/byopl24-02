# Compiler Graph Analysis - Complete Example

This document walks through a complete compiler graph analysis for the queens benchmark, demonstrating all phases from graph generation through final report.

---

## Scenario

**User Request**: "Analyze compiler graphs for the queens benchmark to understand why performance is slow"

**Context**:
- cpu-sampler identified `placeQueen` as a hot function
- trace-performance-warnings didn't show critical issues
- trace-inlining shows successful inlining
- Performance is still slower than expected

---

## Step 1: Generate Compiler Graphs

```bash
# Create output directory
mkdir -p compiler_graphs/queens

# Run benchmark with graph dumping
EXTRA_JAVA_ARGS="-Djdk.graal.Dump=Truffle:1 \
                 -Djdk.graal.PrintGraph=File \
                 -Djdk.graal.DumpPath=compiler_graphs/queens" \
./lox benchmarks/queens.lox

# Verify output
ls -lh compiler_graphs/queens/
```

**Output**:
```
-rw-r--r--  732K TruffleHotSpotCompilation-2745[root_getRowColumn].bgv
-rw-r--r--  1.2M TruffleHotSpotCompilation-2801[root_placeQueen].bgv
-rw-r--r--  1.3M TruffleHotSpotCompilation-3227[root_queens].bgv
```

**Analysis**: Three functions compiled - focus on `placeQueen` (the hot function identified by cpu-sampler)

---

## Step 2: Quick Analysis with Seafoam

```bash
# List graphs in placeQueen function
seafoam "compiler_graphs/queens/TruffleHotSpotCompilation-2801[root_placeQueen].bgv" list
```

**Output**:
```
0: ["TruffleIR.Tier1.root_placeQueen()","After PE Tier"]
1: ["TruffleIR.Tier1.root_placeQueen()","Call Tree","Before Inline"]
2: ["TruffleIR.Tier1.root_placeQueen()","Call Tree","After Inline"]
3: ["TruffleIR.Tier1.root_placeQueen()","After TruffleTier"]  ← TARGET
4: ["Graal.Tier1.root_placeQueen()","After high tier"]
5: ["Graal.Tier1.root_placeQueen()","After mid tier"]
6: ["Graal.Tier1.root_placeQueen()","After low tier"]
```

**Analysis**: Focus on index 3 ("After TruffleTier")

```bash
# Analyze "After TruffleTier" phase
seafoam --json "compiler_graphs/queens/TruffleHotSpotCompilation-2801[root_placeQueen].bgv:3" describe
```

**Output**:
```json
{
  "node_count": 426,
  "branches": true,
  "calls": true,
  "deopts": false,
  "loops": true,
  "linear": false,
  "node_counts": {
    "FrameState": 102,
    "ConstantNode": 50,
    "FixedGuardNode": 59,
    "LoadFieldNode": 46,
    "InvokeWithExceptionNode": 9,
    "UnboxNode": 8,
    "BoxNode$PureBoxNode": 4,
    "MethodCallTargetNode": 9
  }
}
```

**Initial Findings**:
- ⚠️ **9 InvokeWithExceptionNode** - method calls not eliminated
- ⚠️ **12 boxing operations** (8 UnboxNode + 4 BoxNode)
- ⚠️ **59 FixedGuardNode** - many guards (potential type instability)
- ✅ **No deopts** - compilation is stable
- ✅ **50 ConstantNode** - constant folding working

**Conclusion**: Call overhead and boxing are the primary issues

---

## Step 3: Convert to JSON for Deep Analysis

```bash
# Convert to JSON
bgv2json "compiler_graphs/queens/TruffleHotSpotCompilation-2801[root_placeQueen].bgv" \
  > compiler_graphs/queens/placeQueen.json

# Verify conversion
cat compiler_graphs/queens/placeQueen.json | jq -s 'length'
```

**Output**: `7` (7 graphs in the file)

```bash
# Verify "After TruffleTier" is at index 3
cat compiler_graphs/queens/placeQueen.json | jq -s '.[3] | .name'
```

**Output**: `["TruffleIR.Tier1.root_placeQueen()","After TruffleTier"]`

---

## Step 4: Query for Specific Issues

### Query 1: Call Overhead

```bash
cat compiler_graphs/queens/placeQueen.json | jq -s '.[3] |
  .nodes |
  group_by(.props.node_class.node_class) |
  map({node_class: .[0].props.node_class.node_class, count: length}) |
  map(select(.node_class | test("Call")))'
```

**Output**:
```json
[
  {"node_class": "jdk.graal.compiler.nodes.java.MethodCallTargetNode", "count": 9}
]
```

**Interpretation**: 9 method calls remaining in compiled code - significant optimization opportunity

---

### Query 2: Boxing Overhead

```bash
cat compiler_graphs/queens/placeQueen.json | jq -s '.[3] |
  .nodes |
  group_by(.props.node_class.node_class) |
  map({node_class: .[0].props.node_class.node_class, count: length}) |
  map(select(.node_class | test("Box|Unbox")))'
```

**Output**:
```json
[
  {"node_class": "jdk.graal.compiler.nodes.extended.UnboxNode", "count": 8},
  {"node_class": "jdk.graal.compiler.nodes.extended.BoxNode$PureBoxNode", "count": 4}
]
```

**Interpretation**: Significant boxing overhead - primitive values are being boxed/unboxed repeatedly

---

### Query 3: Escape Analysis Check

```bash
cat compiler_graphs/queens/placeQueen.json | jq -s '.[3] |
  .nodes |
  group_by(.props.node_class.node_class) |
  map({node_class: .[0].props.node_class.node_class, count: length}) |
  map(select(.node_class | test("Alloc|New|Commit")))'
```

**Output**:
```json
[]
```

**Interpretation**: ✅ Escape analysis successful - all allocations eliminated

---

### Query 4: Type Stability

```bash
# Count guards
cat compiler_graphs/queens/placeQueen.json | jq -s '.[3] |
  .nodes |
  group_by(.props.node_class.node_class) |
  map({node_class: .[0].props.node_class.node_class, count: length}) |
  map(select(.node_class | test("Guard")))'
```

**Output**:
```json
[
  {"node_class": "jdk.graal.compiler.nodes.FixedGuardNode", "count": 59}
]
```

```bash
# Count deopts
cat compiler_graphs/queens/placeQueen.json | jq -s '.[3] |
  .nodes |
  map(select(.props.node_class.node_class | test("Deopt"))) |
  length'
```

**Output**: `0`

```bash
# Count type checks
cat compiler_graphs/queens/placeQueen.json | jq -s '.[3] |
  .nodes |
  group_by(.props.node_class.node_class) |
  map({node_class: .[0].props.node_class.node_class, count: length}) |
  map(select(.node_class | test("InstanceOf")))'
```

**Output**:
```json
[
  {"node_class": "jdk.graal.compiler.nodes.java.InstanceOfNode", "count": 15}
]
```

**Interpretation**:
- Many guards but no deopts = guards are working correctly
- 15 InstanceOfNode indicates some polymorphism but not excessive
- Not a critical issue

---

## Step 5: Generate Report

```markdown
# Compiler Graph Analysis Report

**Benchmark**: queens
**Function Analyzed**: root_placeQueen
**Graph Phase**: After TruffleTier
**Total Nodes**: 426
**Total Edges**: 1253

## Findings

### 1. Call Overhead ⚠️ CRITICAL
- **MethodCallTargetNode**: 9
- **InvokeWithExceptionNode**: 9
- **Status**: ⚠️ 9 method calls remaining in compiled code
- **Impact**: Prevents full optimization, adds call overhead on every invocation
- **Root Cause**: Recursive calls to `placeQueen` not being cached
- **Recommendation**:
  - Add CallTarget caching with `@Cached` for recursive calls
  - Ensure helper methods are marked for inlining
  - Verify with trace-inlining that all expected calls inline

### 2. Boxing Overhead ⚠️ MODERATE
- **UnboxNode**: 8
- **BoxNode$PureBoxNode**: 4
- **Status**: ⚠️ 12 boxing operations present
- **Impact**: Allocation overhead, prevents scalar replacement
- **Root Cause**: Arithmetic operations using boxed types instead of primitives
- **Recommendation**:
  - Add `@Specialization` with primitive parameter types (`int`, `long`)
  - Enable boxing elimination in `@GenerateBytecode` config:
    ```java
    @GenerateBytecode(boxingEliminationTypes = {long.class})
    ```
  - Use primitive return types where possible

### 3. Escape Analysis ✅ SUCCESS
- **CommitAllocationNode**: 0
- **NewInstanceNode**: 0
- **NewArrayNode**: 0
- **Status**: ✅ All allocations successfully eliminated
- **Impact**: Optimal - no allocation overhead
- **Note**: This is excellent - keep current allocation patterns

### 4. Type Stability ⚠️ ACCEPTABLE
- **FixedGuardNode**: 59
- **DeoptimizeNode**: 0
- **InstanceOfNode**: 15
- **Status**: ⚠️ Many guards, but no actual deoptimizations
- **Impact**: Guard overhead acceptable, no deopt instability
- **Note**: Guards are working correctly - not a priority for optimization

## Summary

**Critical Issues**: 1 (call overhead - 9 remaining invocations)
**Moderate Issues**: 1 (boxing overhead - 12 box/unbox operations)
**Minor Issues**: 0
**Expected Impact**: 3-5x speedup if both critical and moderate issues fixed

**Top Priority**: Eliminate 9 remaining method calls through CallTarget caching

**Estimated Benefit Breakdown**:
- CallTarget caching: ~2-3x improvement
- Boxing elimination: ~1.5-2x improvement
- Combined: ~3-5x total improvement

## Next Steps

1. **Immediate**: Implement CallTarget caching for `placeQueen` recursive calls
   ```java
   @Specialization
   void placeQueen(int col, @Cached("create(...)") CallTarget recursiveCall) {
     // Use recursiveCall instead of direct invoke
   }
   ```

2. **High Priority**: Add primitive specializations to eliminate boxing
   ```java
   @Specialization
   long add(long a, long b) { return a + b; }

   @Specialization
   long multiply(long a, long b) { return a * b; }
   ```

3. **Verification**: Re-run compiler graph analysis after fixes
   - Verify InvokeWithExceptionNode count drops to 0
   - Verify BoxNode/UnboxNode count drops to 0
   - Compare performance with original baseline

4. **Measurement**: Run cpu-sampler to measure actual performance improvement
```

---

## Verification After Fixes

After implementing CallTarget caching and primitive specializations, re-run the analysis:

```bash
# Clean old graphs
rm -rf compiler_graphs/queens

# Generate new graphs
EXTRA_JAVA_ARGS="-Djdk.graal.Dump=Truffle:1 \
                 -Djdk.graal.PrintGraph=File \
                 -Djdk.graal.DumpPath=compiler_graphs/queens" \
./lox benchmarks/queens.lox

# Quick check with seafoam
seafoam --json "compiler_graphs/queens/TruffleHotSpotCompilation-XXXX[root_placeQueen].bgv:3" describe
```

**Expected Result**:
```json
{
  "node_count": 320,  // Reduced from 426
  "calls": false,     // Changed from true
  "node_counts": {
    "ConstantNode": 65,              // Increased (more constant folding)
    "FixedGuardNode": 45,            // Reduced from 59
    "LoadFieldNode": 38,             // Reduced from 46
    "InvokeWithExceptionNode": 0,    // ✅ Eliminated (was 9)
    "UnboxNode": 0,                  // ✅ Eliminated (was 8)
    "BoxNode$PureBoxNode": 0         // ✅ Eliminated (was 4)
  }
}
```

**Success Criteria**:
- ✅ InvokeWithExceptionNode = 0
- ✅ BoxNode/UnboxNode = 0
- ✅ Total node count reduced by ~20-30%
- ✅ Performance improvement matches estimate (3-5x)

---

## Key Takeaways

1. **Seafoam first**: Quickly identified call and boxing issues without full JSON analysis
2. **Targeted queries**: Used specific jq queries only for critical issues
3. **Correlation**: Findings aligned with cpu-sampler (hot function) and trace-inlining results
4. **Actionable**: Report provided specific code changes, not just descriptions
5. **Verifiable**: Clear success criteria for post-fix verification

This analysis took ~15 minutes and identified concrete optimization opportunities worth 3-5x performance improvement.
