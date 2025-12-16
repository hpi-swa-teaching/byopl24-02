---
name: analyze-compiler-graph
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
- **Required**: Having benchmark baseline data for comparison
- **Required**: Profiling data showing specific hot functions
- **Required**: bgv2json or Seafoam installed
  - `which bgv2json || echo "Install: gem install bgv2json"`
  - `which seafoam || echo "Install: gem install seafoam"`
- **Recommended**: Performance warning output
- **Recommended**: Compilation trace output

## Fermi Verification: The Sanity Gate (MANDATORY)

**Principle:**  
The Tool Output is the highest authority for *data*, but your Fermi Estimate is the highest authority for *pipeline integrity*.

**The Logic:**
- **Small Deviation:** Tool works correctly. Update your mental model.
- **Massive Deviation (>1 Order of Magnitude):** Tool is likely **malfunctioning** (silent failure, misconfiguration, or wrong target).

**Protocol:**

### Step 1: Pre-Calculation
- In a scratchpad, estimate the expected output magnitude (e.g., "This acts on an array of 10k items, so I expect at least 10k nodes").
- *Key:* You must write this down *before* generating the tool command.

### Step 2: Smoke Test (The Probe)
- Run on trivial input first to prove the tool *can* work.

### Step 3: Execute & Validate
- Run the actual command.
- **Credibility Threshold Check:** Compare Output vs. Estimate.
  - **Scenario A (Within 1 Order of Magnitude):** **ACCEPT.** The tool is the authority. Proceed with this result.
  - **Scenario B (>1 Order of Magnitude Divergence OR Unexpected Zero):** **REJECT & DIAGNOSE.**
    - **STOP.** Do not use this result for the next step.
    - **Hypothesis:** The tool failed silently, the path is wrong, or permissions are denied.
    - **Action:** Run a *Debug Command* (e.g., `ls -l target_file` to check size, or `echo $?` to check exit code) to prove the tool is healthy.
    - *Only* after the tool's health is proven via a secondary check may you accept the divergent result.

## How the Skill Works

In all examples, `<launcher>` refers to your programming language launcher script.

### 1. Dump Compiler Graphs

#### Basic Dump (Truffle Compilations)
```bash
EXTRA_JAVA_ARGS="-Djdk.graal.Dump=Truffle:1 \
  -Djdk.graal.PrintGraph=File \
  -Djdk.graal.DumpPath=compiler_graphs" \
  <launcher> <program> [script args]
```
- Dumps all Truffle compilations
- Level 1: Basic graphs (After parsing, After TruffleTier)
- Output: `compiler_graphs/` directory with BGV files

#### Focused Dump (Specific Method)
```bash
EXTRA_JAVA_ARGS="-Djdk.graal.Dump=Truffle:1 \
  -Djdk.graal.MethodFilter="*hotFunction*" \
  -Djdk.graal.DumpPath=compiler_graphs" \
  <launcher> --experimental-options \
  --engine.CompileOnly="*hotFunction*" \
  <program> [script args]
```
- Level 1: Basic graphs (After parsing, After TruffleTier)
- After TruffleTier is the phase showing Truffle-specific optimizations
- `--engine.CompileOnly` ensures only this function is compiled, reducing output
- Dramatically reduces output
- Focus on known problem method
- Essential for manageable analysis

#### With Source Positions
```bash
EXTRA_JAVA_ARGS="-Djdk.graal.Dump=Truffle:1 \
  -Djdk.graal.TrackNodeSourcePosition=true \
  -Djdk.graal.DumpPath=compiler_graphs" \
  <launcher> --experimental-options \
  --engine.NodeSourcePositions \
  <program> [script args]
```
- Enables source location tracking
- Required for `seafoam source` command
- Shows inlining call stacks

#### Level 2: Detailed Phases
```bash
EXTRA_JAVA_ARGS="-Djdk.graal.Dump=Truffle:2 \
  -Djdk.graal.MethodFilter="*hotFunction*" \
  -Djdk.graal.DumpPath=compiler_graphs" \
  <launcher> <program> [script args]
```
- Shows all optimization phases
- Level 2: Full detail
- Use only for investigating specific phase failures
- 5-10x more output than level 1

#### Side Note: Dump Level Argument

The numbers after the colon (`:1`, `:2`, `:3`, etc.) control the **verbosity** of graph dumps - essentially how many compilation phases are dumped.

The dump levels control how many compiler phases are captured. A typical usage for low-level IR visualization is `:3`, which provides detailed dumps suitable for the C1 Visualizer. While the documentation doesn't specify exact phase counts for each level, the pattern is:

- **`:1`** - Fewer phases (basic dumps)
- **`:2`** - More phases (intermediate detail)  
- **`:3`** - Most phases (comprehensive dumps for low-level debugging)

Using `Truffle:1` with `PrintGraph=Network` shows Truffle ASTs, guest-language call graphs, and Graal graphs as they leave the Truffle phase. `Truffle:2` dumps Graal graphs between each compiler phase, providing more granular detail during the Truffle compilation pipeline.

### 2. Use seafoam to analyze BGV

- Call `seafoam help` for full command list
- Analyze the BGV files dumped in output directory
- Continue with step 3 if you need more control to analyze JSON directly

### 3. Convert BGV to JSON

```bash
bgv2json compiler_graphs/*.bgv > graphs.json
```
- Fast, efficient conversion
- Produces JSON Lines format (one graph per line)
- Works with compressed files (.bgv.gz)

### 4. Understanding the JSON Structure

The converted JSON has a specific structure that's important to understand for effective analysis.

#### File Format
- **JSON Lines format**: Each line is a complete JSON object representing one compilation phase/graph
- Multiple graphs per BGV file (one for each compilation phase)

#### Top-Level Structure
Each graph object has these keys:
```json
{
  "name": ["function_name", "phase_name"],
  "props": { /* graph metadata */ },
  "nodes": [ /* IR nodes */ ],
  "edges": [ /* data/control flow */ ],
  "blocks": [ /* basic blocks */ ]
}
```

#### Graph Metadata
```json
{
  "name": ["TruffleIR.Tier1.root_sieve()", "After TruffleTier"],
  "props": {
    "compilationIdentifier": "TruffleHotSpotCompilation-2793[root sieve]",
    "graph": "StructuredGraph:696145{...}",
    "scope": "TruffleCompilerThread-31.Truffle.TruffleFinal"
  }
}
```
- `name[0]`: Function/method name
- `name[1]`: Compilation phase (e.g., "After TruffleTier", "After PartialEscape")
- `props.compilationIdentifier`: Unique ID for this compilation

#### Node Structure
Each node represents a compiler IR operation:
```json
{
  "id": 5,
  "props": {
    "label": "AddNode",              // Node type (for filtering)
    "category": "arithmetic",         // Category: floating, arithmetic, state, etc.
    "stamp": "i32",                   // Type information
    "nodeToBlock": "B0",              // Basic block ID
    "node_class": {
      "node_class": "jdk.graal.compiler.nodes.calc.AddNode"
    }
  }
}
```

**Key properties:**
- `id`: Unique identifier (used in edges)
- `props.label`: Human-readable node type (e.g., "AddNode", "UnboxNode")
- `props.category`: Node category (arithmetic, floating, state, control)
- `props.stamp`: Type/value information
- `props.node_class.node_class`: Fully qualified Java class name

#### Edge Structure
Edges connect nodes to show data and control flow:
```json
{
  "from": 5,
  "to": 1,
  "props": {
    "direct": true,
    "name": "x",               // Input name (e.g., "x", "y" for binary ops)
    "type": "Value",           // Edge type: Value, State, Association
    "index": 0
  }
}
```

#### Common Node Types

**Arithmetic Operations:**
- `AddNode`, `SubNode`, `MulNode`, `DivNode`
- `IntegerLessThanNode`, `IntegerEqualsNode`

**Constants & Parameters:**
- `ConstantNode` - Compile-time constants
- `ParameterNode` - Method parameters

**Conversions (Performance Critical):**
- `BoxNode` / `BoxNode$AllocatingBoxNode` - Boxing primitives ⚠️
- `UnboxNode` - Unboxing objects ⚠️

**Memory Operations:**
- `LoadFieldNode`, `StoreFieldNode` - Object field access
- `LoadIndexedNode`, `StoreIndexedNode` - Array access

**Allocations (Escape Analysis):**
- `TruffleNew` - Object allocation ⚠️
- `CommitAllocationNode`, `NewInstanceNode` - Failed escape analysis ⚠️
- `AllocatedObjectNode` - Allocation tracking

**Call Operations:**
- `OptimizedDirectCallNode` - Specialized direct call ✅
- `OptimizedIndirectCallNode` - Dynamic call ⚠️
- `InvokeNode`, `InvokeWithExceptionNode` - Method calls ⚠️

**Control Flow:**
- `IfNode` - Conditional branch
- `LoopBeginNode`, `LoopEndNode` - Loop structure
- `MergeNode`, `BeginNode`, `EndNode` - Control merge points
- `ReturnNode` - Method return

### 6. Analyze with jq

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

#### Additional: Generate SVG Visualization

```bash
# Render the "After TruffleTier" phase to an image
seafoam file.bgv.gz:2 render > graph.svg
```

### 5. Common Problem Patterns

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
```<your-language>
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

### 6. Key Graph Phases to Check

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
<launcher> --cpusampler --cpusampler.ShowTiers=true <program> [script args]
```
**Identify**: Function consuming most time

### Step 2: Dump Graphs for Hot Function Only
```bash
# Clean previous dumps to ensure data isolation
rm -rf compiler_graphs/

EXTRA_JAVA_ARGS="-Djdk.graal.Dump=Truffle:1 \
  -Djdk.graal.MethodFilter="*hotFunction*" \
  -Djdk.graal.DumpPath=compiler_graphs" \
  <launcher> --experimental-options \
  --engine.CompileOnly="*hotFunction*" \
  <program> [script args]
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
-Djdk.graal.Dump=Truffle:1 -Djdk.graal.MethodFilter="*hotFunction*"
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
  <launcher> --experimental-options \
  --compiler.TracePerformanceWarnings=all \
  --engine.TraceCompilation \
  <program> [script args] 2>&1 | tee combined.log
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
- Seafoam: https://github.com/Shopify/seafoam
- BGV format: https://github.com/Shopify/seafoam/blob/main/docs/bgv.md
- GraalVM Debugging: https://github.com/oracle/graal/blob/master/compiler/docs/Debugging.md
- Use Graal Truffle Docs skill

## Implementation Notes

This skill:
- Uses environment variable: `EXTRA_JAVA_ARGS`
- Dumps to: `compiler_graphs/` directory (default)
- Requires: bgv2json or Seafoam for analysis
- Generates: Potentially gigabytes of data without filtering
- Use only after: Profiling, warnings, and compilation traces
- Focus on: "After TruffleTier" phase for Truffle work
- Emphasizes: **Most complex diagnostic - use last, after simpler tools**
- Combined with other performance analysis skills for full picture

## Related Skills

- Use Graal Truffle Docs skill to understand Truffle APIs and options
- Use CPU Sampler Analyze skill for initial profiling to identify hot functions
- Use Performance Warnings Analyze skill to find optimization barriers
- Use Compilation Trace Analyze skill to see inlining and compilation decisions
- Use CPU Tracer Analyze skill for execution frequency insights
- Use Memory Tracer Analyze skill for allocation profiling
- Use Trace Inlining Analyze skill for inlining decision analysis
- Use Trace Transfer to Interpreter Analyze skill for deoptimization insights
- Use Benchmark Baseline skill for creating performance baselines with different benchmarks
