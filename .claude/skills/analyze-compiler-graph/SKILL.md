---
name: Dump and Analyze Compiler Graphs
description: Dumps and analyzes Graal IR compiler graphs showing optimization decisions. Use BGV format with bgv2json/seafoam to inspect escape analysis (allocation elimination), boxing removal, inlining decisions, and call node types. Reveals what compiler actually optimized vs what you intended. Best for deep-dive investigation after basic profiling identifies issues.
---

# Skill: Dump and Analyze Compiler Graphs

This skill dumps Graal IR compiler graphs and analyzes them to understand optimization decisions at the deepest level.

## What This Skill Does

1. **Dumps Compiler Graphs**: Generates BGV files showing IR at different optimization phases
2. **Analyzes with Seafoam/bgv2json**: Converts BGV to JSON for programmatic analysis
3. **Identifies Optimization Issues**: Reveals:
   - Failed escape analysis (allocations remaining)
   - Indirect calls preventing inlining
   - Boxing/unboxing overhead
   - Missing arithmetic specializations
   - Deoptimization instability

## Critical Understanding: Use After Basic Profiling!

**This is a DEEP diagnostic tool - use only after**:
1. ✅ CPU profiling identified hot methods
2. ✅ Performance warnings revealed optimization barriers
3. ✅ Compilation/inlining traces showed issues

**Why last**:
- Most complex diagnostic
- Requires compiler expertise to interpret
- Generates massive output without filtering
- Usually other tools identify the issue faster

**Use when**:
- Other tools show problems but don't reveal root cause
- Need to understand WHY optimization fails
- Investigating allocation elimination issues
- Debugging polymorphism and specialization

## Prerequisites

Before using this skill:
- **Required**: Profiling data showing specific hot functions
- **Required**: bgv2json or Seafoam installed
- **Recommended**: Performance warning output
- **Recommended**: Compilation trace output

## Reliability Protocol (MANDATORY)

**Context:** Tools often fail due to environment issues, permissions, or misconfiguration. To avoid hallucinating results, misinterpreting output, or wasting resources, you must follow this 3-step verification loop.

### Step 1: Pre-Execution Baseline

Before executing the primary task, establish a mental baseline:
* **Complexity Estimate:** asking yourself what you expect from a run with a trivial input (i.e. "If I run this on trivial input, how fast should it be?").
* **Failure Mode Prediction:** "If this tool is broken, will it hang, crash, or return empty text?"
* **Sanity Check:** If the tool takes 100x longer than your estimate, **STOP**. It is likely misconfigured or waiting on input.

### Step 2: The Probe (Dry Run)
Never run a complex or heavy command blind. Execute a **Probe** first:
* **The Test:** Run the exact command structure on a trivial target (e.g., `print "test";`, `SELECT 1`, or a dummy file).
* **Constraint:** If the Probe hangs, errors, or produces empty output, **STOP**. Do not proceed to the main task.

### Step 3: Output Audit (Verification)

Do not assume success based on exit codes.
* **Physical Check:** verify the output artifact exists and has a file size > 0 bytes.
* **Content Scan:** Read the first 5 lines/bytes of the output to ensure it is not an error message written to stdout (e.g., "Error: Command not found" saved inside `output.json`). Verify it's in the range of expected content and metrics. If it's to far off, **STOP**.

## How the Skill Works

### 1. Dump Compiler Graphs

#### Basic Dump (Truffle Compilations)
```bash
EXTRA_JAVA_ARGS="-Djdk.graal.Dump=Truffle:1 \
  -Djdk.graal.PrintGraph=File \
  -Djdk.graal.DumpPath=compiler_graphs" \
  ./lox program.lox
```
- Dumps all Truffle compilations
- Level 1: Basic graphs (After parsing, After TruffleTier)
- Output: `compiler_graphs/` directory with BGV files

#### Focused Dump (Specific Method)
```bash
EXTRA_JAVA_ARGS="-Djdk.graal.Dump=Truffle:1 \
  -Djdk.graal.MethodFilter=*hotFunction* \
  -Djdk.graal.DumpPath=compiler_graphs" \
  ./lox --experimental-options \
  --engine.CompileOnly=hotFunction \
  program.lox
```
- Dramatically reduces output
- Focus on known problem method
- Essential for manageable analysis

#### With Source Positions
```bash
EXTRA_JAVA_ARGS="-Djdk.graal.Dump=Truffle:1 \
  -Djdk.graal.TrackNodeSourcePosition=true \
  -Djdk.graal.DumpPath=compiler_graphs" \
  ./lox --experimental-options \
  --engine.NodeSourcePositions \
  program.lox
```
- Enables source location tracking
- Required for `seafoam source` command
- Shows inlining call stacks

#### Level 2: Detailed Phases
```bash
EXTRA_JAVA_ARGS="-Djdk.graal.Dump=Truffle:2 \
  -Djdk.graal.MethodFilter=*hotFunction* \
  -Djdk.graal.DumpPath=compiler_graphs" \
  ./lox program.lox
```
- Shows all optimization phases
- Use only for investigating specific phase failures
- 5-10x more output than level 1

### 2. Convert BGV to JSON

#### Using bgv2json (Recommended)
```bash
bgv2json compiler_graphs/*.bgv > graphs.json
```
- Fast, efficient conversion
- Produces JSON Lines format (one graph per line)
- Works with compressed files (.bgv.gz)

#### Using Seafoam
```bash
# List graphs in file
seafoam compiler_graphs/file.bgv.gz list

# Convert specific graph to JSON
seafoam --json file.bgv.gz:0 props > graph.json
```

### 3. Analyze with jq

#### Query 1: Find Indirect Calls (Performance Problem!)
```bash
# Should show OptimizedDirectCallNode, NOT OptimizedIndirectCallNode
cat graphs.json | jq '.nodes[] | select(.props.label | contains("Call")) | .props.label' | sort | uniq -c

# Or with seafoam
seafoam --json file.bgv.gz:2 describe | \
  jq '.node_counts | to_entries | .[] | select(.key | contains("Call"))'
```

**Good**:
```json
{"key": "OptimizedDirectCallNode", "value": 5}
```

**Bad** (needs caching!):
```json
{"key": "OptimizedIndirectCallNode", "value": 10}
{"key": "InvokeNode", "value": 8}
```

#### Query 2: Find Failed Escape Analysis (Allocations Remaining)
```bash
# After PartialEscape phase, should have ZERO allocations
cat graphs.json | jq 'select(.name | contains("After PartialEscape")) | .nodes[] | select(.props.label | test("Alloc|New")) | .props.label' | sort | uniq -c

# Or with seafoam
seafoam --json file.bgv.gz:3 describe | \
  jq '.node_counts | to_entries | .[] | select(.key | test("Alloc|New"))'
```

**Good**: Empty output (all allocations eliminated)

**Bad** (escape analysis failed!):
```json
{"key": "CommitAllocationNode", "value": 2}
{"key": "NewInstanceNode", "value": 1}
```

#### Query 3: Find Boxing Operations (Specialization Issue!)
```bash
cat graphs.json | jq '.nodes[] | select(.props.label | test("Box|Unbox")) | .props.label' | sort | uniq -c

# Or with seafoam
seafoam --json file.bgv.gz:2 describe | \
  jq '.node_counts | to_entries | .[] | select(.key | test("Box|Unbox"))'
```

**Good**: Empty output (primitives stay unboxed)

**Bad** (missing primitive specializations!):
```json
{"key": "BoxNode", "value": 4}
{"key": "UnboxNode", "value": 4}
```

#### Query 4: Check Graph Characteristics
```bash
seafoam --json file.bgv.gz:2 describe | \
  jq '{
    node_count,
    loops,
    branches,
    deopts,
    calls,
    linear
  }'
```

**Interpretation**:
- `deopts: false` = Good (stable compilation)
- `deopts: true` = Bad (deoptimization instability)
- `calls` count = High means many unspecialized calls
- `linear: true` = Optimal (no branches, straight-line code)

#### Query 5: Count Node Types
```bash
seafoam --json file.bgv.gz:2 describe | jq '.node_counts' | jq 'to_entries | sort_by(.value) | reverse | .[0:10]'
```

**Look for**:
- High `InvokeNode` count = Unspecialized method calls
- `AddNode`, `MulNode` = Good (arithmetic specialized)
- `ConstantNode` high = Good (constant propagation working)

### 4. Common Problem Patterns

#### Problem 1: Indirect Calls (Critical!)
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

#### Problem 2: Failed Escape Analysis
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

#### Problem 3: Boxing/Unboxing
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

#### Problem 4: Deoptimization Nodes
```
DeoptimizeNode or UnreachedNode found in hot path
```

**Root Cause**: Unstable type assumptions

**Fix**: Add proper guards and type specializations

**Correlation**: Use with `--engine.TraceTransferToInterpreter` to find exact location

### 5. Key Graph Phases to Check

#### "After parsing"
- Shows initial IR from AST
- Lots of nodes, not optimized yet

#### "After TruffleTier"
- **Most important for language developers!**
- Shows Truffle-specific optimizations
- Should see:
  - ✅ Constant nodes (from partial evaluation)
  - ✅ Direct calls (not indirect)
  - ✅ Arithmetic nodes (not InvokeNodes)
  - ❌ VirtualFrame references (should be eliminated)

#### "After PartialEscape"
- Check for allocation elimination
- Should have ZERO allocation nodes if escape analysis worked

#### "After TruffleTier" vs "Final"
- Compare to see what generic Graal optimizations did
- Node count should decrease significantly

## Typical Workflow

### Step 1: Profile to Identify Hot Function
```bash
./lox --cpusampler --cpusampler.ShowTiers=true program.lox
```
**Identify**: Function consuming most time

### Step 2: Dump Graphs for Hot Function Only
```bash
EXTRA_JAVA_ARGS="-Djdk.graal.Dump=Truffle:1 \
  -Djdk.graal.MethodFilter=*hotFunction* \
  -Djdk.graal.DumpPath=compiler_graphs" \
  ./lox --experimental-options \
  --engine.CompileOnly=hotFunction \
  program.lox
```

### Step 3: Convert to JSON
```bash
bgv2json compiler_graphs/*.bgv > graphs.json
```

### Step 4: Find "After TruffleTier" Graph
```bash
cat graphs.json | jq 'select(.name | contains("After TruffleTier"))'  | head -1 > truffle-tier.json
```

### Step 5: Check for Common Issues
```bash
# Indirect calls?
cat truffle-tier.json | jq '.nodes[] | select(.props.label | contains("IndirectCall"))'

# Allocations?
cat truffle-tier.json | jq '.nodes[] | select(.props.label | test("Alloc|New"))'

# Boxing?
cat truffle-tier.json | jq '.nodes[] | select(.props.label | test("Box|Unbox"))'

# InvokeNodes (unspecialized)?
cat truffle-tier.json | jq '.nodes[] | select(.props.label == "InvokeNode") | .props'
```

### Step 6: Fix Issues

### Step 7: Verify Fixes
```bash
# Re-dump and re-analyze
# Should see problems eliminated
```

## Best Practices

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

## Common Pitfalls

- ❌ **Dumping without profiling first**: Wasted effort on wrong methods
- ❌ **Not using MethodFilter**: Gigabytes of unusable output
- ❌ **Analyzing wrong phase**: Focus on "After TruffleTier" first
- ❌ **Ignoring source positions**: Use --engine.NodeSourcePositions
- ❌ **Comparing without controlled conditions**: Different warmup = incomparable
- ❌ **Over-interpreting**: Graphs show results, not decision process

## Key Node Types to Recognize

### Good Patterns (Optimized)
- `OptimizedDirectCallNode` - Specialized calls
- `AddNode`, `MulNode`, `SubNode` - Primitive arithmetic
- `ConstantNode` - Constants from partial evaluation
- `ParameterNode` → disappears after inlining
- Few `GuardNode` - Stable assumptions

### Bad Patterns (Need Fixes)
- `OptimizedIndirectCallNode` - Need caching
- `InvokeNode` - Unspecialized method calls
- `CommitAllocationNode`, `NewInstanceNode` - Escape analysis failed
- `BoxNode`, `UnboxNode` - Missing primitive specializations
- `DeoptimizeNode` in hot paths - Unstable assumptions
- Many `GuardNode` - Over-speculation or instability

## Success Criteria

**After optimization**:
- ✅ Zero `OptimizedIndirectCallNode` (all direct)
- ✅ Zero allocation nodes after PartialEscape
- ✅ Zero `BoxNode`/`UnboxNode` (primitives stay unboxed)
- ✅ Low `InvokeNode` count (arithmetic specialized)
- ✅ Zero `DeoptimizeNode` in hot paths
- ✅ High `ConstantNode` count (good partial evaluation)
- ✅ Simple, linear graphs (few branches)

## Reference Documentation

For detailed information, see:
- `/Users/antonykamp/Projects/hpi-ma/byopl24-02/docs/commands/analyze-compiler-graph.md` - Complete documentation with node reference
- Seafoam: https://github.com/Shopify/seafoam
- BGV format: https://github.com/Shopify/seafoam/blob/main/docs/bgv.md
- GraalVM Debugging: https://github.com/oracle/graal/blob/master/compiler/docs/Debugging.md

## Implementation Notes

This skill:
- Uses environment variable: `EXTRA_JAVA_ARGS`
- Dumps to: `compiler_graphs/` directory (default)
- Requires: bgv2json or Seafoam for analysis
- Generates: Potentially gigabytes of data without filtering
- Use only after: Profiling, warnings, and compilation traces
- Focus on: "After TruffleTier" phase for Truffle work
- Emphasizes: **Most complex diagnostic - use last, after simpler tools**
