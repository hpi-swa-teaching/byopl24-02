---
name: analyze-compiler-graph
description: Analyzes Graal IR compiler graphs to understand optimization decisions. Dumps compiler graphs from benchmarks using -Djdk.graal.Dump, analyzes them directly with seafoam for quick insights, converts to JSON with bgv2json for deep analysis with jq, and identifies performance issues like failed escape analysis, boxing overhead, indirect calls, and polymorphism.
---

# Skill: Analyze Compiler Graphs

This skill performs deep IR-level analysis of Graal compiler graphs to understand what optimizations the compiler applied (or failed to apply) and identify performance issues invisible to other profiling tools.

## What This Skill Does

1. **Generates Compiler Graphs**: Runs benchmarks with `-Djdk.graal.Dump=Truffle:1` to generate BGV files
2. **Analyzes with Seafoam**: Uses `seafoam` for quick analysis and graph summaries
3. **Converts to JSON**: Uses `bgv2json` to convert BGV files to JSON for detailed querying
4. **Queries with jq**: Runs systematic jq queries to find:
   - Indirect calls (OptimizedIndirectCallNode)
   - Boxing/unboxing overhead (BoxNode, UnboxNode)
   - Failed escape analysis (allocation nodes after PartialEscape phase)
   - Type instability (excessive guards, deoptimization nodes)
   - Polymorphism (InstanceOf, Checkcast nodes)
5. **Generates Analysis Report**: Documents findings with specific IR-level evidence

## When to Use This Skill

Use this skill as a **LAST RESORT** after simpler tools (trace-performance-warnings, trace-inlining, cpu-sampler) show problems but don't reveal root causes:

- **After trace-performance-warnings** shows optimization barriers but you need to see WHAT the compiler actually did
- **When allocations should be eliminated** but escape analysis appears to fail
- **When boxing overhead is suspected** but you need to see BoxNode/UnboxNode in the IR
- **When indirect calls are suspected** and you need to verify OptimizedIndirectCallNode presence
- **Understanding why inlining worked** but performance is still poor (what did the inlined code become?)
- **Deep investigation** when other tools indicate problems but the root cause is unclear

**Important**: This is the most complex diagnostic tool. Always use simpler tools first:
1. `cpu-sampler` - Find where time is spent
2. `trace-performance-warnings` - Find optimization barriers
3. `trace-inlining` - Understand inlining decisions
4. **Only then** → Analyze compiler graphs to see what the compiler actually did

## Prerequisites

### Required Tools
- **GraalVM** with Truffle (already installed if running Truffle language)
- **seafoam**: Ruby gem for BGV analysis
  ```bash
  brew install graphviz  # macOS
  gem install seafoam
  ```
- **bgv2json**: Ruby gem for BGV to JSON conversion
  ```bash
  gem install bgv2json
  ```

### Required Files
- Benchmark programs 
- Sufficient disk space (100MB-1GB per benchmark depending on complexity)

## How This Skill Works

### Phase 1: Generate Compiler Graphs

**Objective**: Run benchmark with Graal graph dumping enabled to generate BGV files

**Process**:

1. **Create output directory**
   ```bash
   mkdir -p compiler_graphs/[benchmark-name]
   ```

2. **Run benchmark with compiler graph dumping**
   ```bash
   EXTRA_JAVA_ARGS="-Djdk.graal.Dump=Truffle:1 \
                    -Djdk.graal.PrintGraph=File \
                    -Djdk.graal.DumpPath=compiler_graphs/[benchmark-name]" \
   <language-launcher> <program>
   ```

3. **Verify BGV files generated**
   ```bash
   ls -lh compiler_graphs/[benchmark-name]/*.bgv
   ```

**Key Options**:
- `-Djdk.graal.Dump=Truffle:1`: Dump Truffle compilation graphs (level 1 verbosity)
- `-Djdk.graal.PrintGraph=File`: Write to filesystem (not network/IGV)
- `-Djdk.graal.DumpPath=<path>`: Custom output directory

**Output**: BGV files named like:
```
TruffleHotSpotCompilation-2745[root_getRowColumn].bgv
TruffleHotSpotCompilation-2801[root_placeQueen].bgv
```

**Example**:
```bash
# Run queens benchmark with graph dumping
mkdir -p compiler_graphs/queens
EXTRA_JAVA_ARGS="-Djdk.graal.Dump=Truffle:1 \
                 -Djdk.graal.PrintGraph=File \
                 -Djdk.graal.DumpPath=compiler_graphs/queens" \
<language-launcher> <program>

# Verify output
ls -lh compiler_graphs/queens/
# Output:
# -rw-r--r--  732K TruffleHotSpotCompilation-2745[root_getRowColumn].bgv
# -rw-r--r--  1.2M TruffleHotSpotCompilation-2801[root_placeQueen].bgv
# -rw-r--r--  1.3M TruffleHotSpotCompilation-3227[root_queens].bgv
```

---

### Phase 2: Analyze with Seafoam (Quick Analysis)

**Objective**: Get high-level graph statistics and verify optimization quality without deep diving

**Process**:

1. **List graphs in BGV file**
   ```bash
   seafoam "[bgv-file]" list
   ```

   Shows all compilation phases:
   - `After PE Tier` - After partial evaluation
   - `After TruffleTier` - After Truffle-specific optimizations (MOST IMPORTANT)
   - `After high tier` - After high-level Graal optimizations
   - `After low tier` - After code generation

2. **Describe specific graph** (focus on "After TruffleTier")
   ```bash
   seafoam --json "[bgv-file]:3" describe
   ```

   Returns:
   ```json
   {
     "node_count": 426,
     "branches": true,
     "calls": true,
     "deopts": false,
     "loops": true,
     "linear": false,
     "node_counts": {
       "FixedGuardNode": 59,
       "LoadFieldNode": 46,
       "ConstantNode": 33,
       ...
     }
   }
   ```

3. **Identify optimization issues**
   - **InvokeWithExceptionNode** count: Should be minimal (indicates method calls remaining)
   - **OptimizedIndirectCallNode**: BAD - should be OptimizedDirectCallNode
   - **BoxNode/UnboxNode**: BAD - indicates boxing overhead
   - **CommitAllocationNode** after TruffleTier: BAD - escape analysis failed
   - **DeoptimizeNode** count: Should be low (indicates speculation failures)
   - **FixedGuardNode** count: High counts indicate type instability

**Example**:
```bash
# Analyze the "After TruffleTier" graph (usually index 3)
seafoam --json "compiler_graphs/queens/TruffleHotSpotCompilation-2801[root_placeQueen].bgv:3" describe

# Output:
{
  "node_count": 426,
  "branches": true,
  "calls": true,
  "deopts": false,
  "loops": true,
  "node_counts": {
    "FixedGuardNode": 59,        # Many guards - possible type instability
    "LoadFieldNode": 46,          # Field accesses
    "InvokeWithExceptionNode": 9, # 9 method calls remaining - optimization opportunity
    "UnboxNode": 8,               # Boxing overhead present
    "BoxNode$PureBoxNode": 4      # Boxing overhead present
  }
}

# Interpretation:
# ⚠️ 9 InvokeNodes - calls not fully inlined/specialized
# ⚠️ Boxing overhead - 8 UnboxNode + 4 BoxNode
# ⚠️ 59 FixedGuardNode - possible type instability
# ✅ No deopts - compilation is stable
```

---

### Phase 3: Convert to JSON with bgv2json

**Objective**: Convert BGV files to JSON format for detailed querying with jq

**Process**:

1. **Convert BGV to JSON**
   ```bash
   bgv2json "[bgv-file]" > [output.json]
   ```

   Generates JSON Lines format (one JSON object per graph, one per line)

2. **Verify JSON structure**
   ```bash
   cat [output.json] | jq -s 'length'  # Count graphs
   cat [output.json] | jq -r '.name'   # List graph names
   ```

**JSON Structure**:
Each line is a graph with:
```json
{
  "name": ["TruffleIR.Tier1.root_placeQueen()", "After TruffleTier"],
  "props": {...},
  "nodes": [
    {
      "id": 0,
      "props": {
        "node_class": {
          "node_class": "jdk.graal.compiler.nodes.StartNode",
          "name_template": "Start",
          ...
        },
        ...
      }
    },
    ...
  ],
  "edges": [
    {"from": 0, "to": 11, "props": {...}},
    ...
  ]
}
```

**Example**:
```bash
# Convert queens placeQueen function to JSON
bgv2json "compiler_graphs/queens/TruffleHotSpotCompilation-2801[root_placeQueen].bgv" \
  > compiler_graphs/queens/placeQueen.json

# Verify conversion
cat compiler_graphs/queens/placeQueen.json | jq -s 'length'
# Output: 9 (9 graphs in the file)

# List graph names
cat compiler_graphs/queens/placeQueen.json | jq -r '.name'
# Output:
# ["TruffleIR.Tier1.root_placeQueen()","After PE Tier"]
# ["TruffleIR.Tier1.root_placeQueen()","Call Tree","Before Inline"]
# ["TruffleIR.Tier1.root_placeQueen()","Call Tree","After Inline"]
# ["TruffleIR.Tier1.root_placeQueen()","After TruffleTier"]
# ...
```

---

### Phase 4: Query with jq (Deep Analysis)

**Objective**: Run systematic queries to identify specific performance issues in the IR

**Process**: Query the JSON using jq to find problematic patterns

#### Query 1: Graph Statistics
```bash
# Basic statistics for "After TruffleTier" graph (index 3)
cat [json-file] | jq -s '.[3] | {
  graph_name: .name,
  total_nodes: (.nodes | length),
  total_edges: (.edges | length)
}'
```

**Example Output**:
```json
{
  "graph_name": ["TruffleIR.Tier1.root_getRowColumn()", "After TruffleTier"],
  "total_nodes": 608,
  "total_edges": 1434
}
```

#### Query 2: Node Type Distribution (Top 15)
```bash
# Find most common node types
cat [json-file] | jq -s '.[3] |
  .nodes |
  group_by(.props.node_class.node_class) |
  map({
    node_class: .[0].props.node_class.node_class,
    count: length
  }) |
  sort_by(.count) |
  reverse |
  .[0:15]'
```

**Example Output**:
```json
[
  {"node_class": "jdk.graal.compiler.nodes.FrameState", "count": 102},
  {"node_class": "jdk.graal.compiler.nodes.ConstantNode", "count": 50},
  {"node_class": "jdk.graal.compiler.nodes.FixedGuardNode", "count": 37},
  {"node_class": "jdk.graal.compiler.nodes.java.LoadFieldNode", "count": 23},
  ...
]
```

**Interpretation**:
- **High ConstantNode count**: Good - constant folding working
- **High FixedGuardNode count**: May indicate type instability
- **High FrameState count**: Normal for complex functions

#### Query 3: Find Call Nodes (Optimization Opportunities)
```bash
# Find all call-related nodes
cat [json-file] | jq -s '.[3] |
  .nodes |
  group_by(.props.node_class.node_class) |
  map({
    node_class: .[0].props.node_class.node_class,
    count: length
  }) |
  map(select(.node_class | test("Call"))) |
  sort_by(.count) |
  reverse'
```

**Example Output**:
```json
[
  {"node_class": "jdk.graal.compiler.nodes.java.MethodCallTargetNode", "count": 10}
]
```

**Interpretation**:
- **InvokeNode / InvokeWithExceptionNode**: Method calls remaining (BAD - should be eliminated)
- **OptimizedIndirectCallNode**: Indirect calls preventing optimization (BAD)
- **OptimizedDirectCallNode**: Direct calls, acceptable if minimal (ACCEPTABLE)
- **MethodCallTargetNode**: Call metadata, count indicates call overhead

**Problem**: 10 MethodCallTargetNode means 10 method calls remain in compiled code
**Fix**: Add CallTarget caching with `@Cached`, improve specialization

#### Query 4: Find Boxing Overhead
```bash
# Find Box/Unbox nodes
cat [json-file] | jq -s '.[3] |
  .nodes |
  group_by(.props.node_class.node_class) |
  map({
    node_class: .[0].props.node_class.node_class,
    count: length
  }) |
  map(select(.node_class | test("Box|Unbox"))) |
  sort_by(.count) |
  reverse'
```

**Example Output**:
```json
[
  {"node_class": "jdk.graal.compiler.nodes.extended.UnboxNode", "count": 18},
  {"node_class": "jdk.graal.compiler.nodes.extended.BoxNode$PureBoxNode", "count": 4}
]
```

**Interpretation**:
- **UnboxNode / BoxNode**: Boxing/unboxing operations waste allocations (BAD)
- **High counts**: Missing primitive specializations or boxing elimination config

**Problem**: 18 UnboxNode + 4 BoxNode = significant boxing overhead
**Fix**: Add primitive specializations (`int`, `long`, `double`), enable boxing elimination in bytecode config

#### Query 5: Find Allocation Nodes (Failed Escape Analysis)
```bash
# Find allocation-related nodes
cat [json-file] | jq -s '.[3] |
  .nodes |
  group_by(.props.node_class.node_class) |
  map({
    node_class: .[0].props.node_class.node_class,
    count: length
  }) |
  map(select(.node_class | test("Alloc|New|Commit"))) |
  sort_by(.count) |
  reverse'
```

**Example Output**:
```json
[]
```

**Interpretation**:
- **Empty result after "After TruffleTier"**: GOOD - escape analysis successful, allocations eliminated
- **CommitAllocationNode present**: BAD - allocations survived optimization
- **NewInstanceNode present**: BAD - object allocations remaining
- **NewArrayNode present**: BAD - array allocations remaining

**Problem**: Allocations present means escape analysis failed
**Fix**: Refactor to keep object lifetimes local, ensure inlining succeeds, avoid storing in fields

#### Query 6: Find Deoptimization Nodes
```bash
# Count deoptimization nodes
cat [json-file] | jq -s '.[3] |
  .nodes |
  map(select(.props.node_class.node_class | test("Deopt"))) |
  length'
```

**Example Output**:
```
10
```

**Interpretation**:
- **0 DeoptimizeNode**: IDEAL - no unconditional deoptimizations
- **1-5 DeoptimizeNode**: ACCEPTABLE - uncommon paths
- **>10 DeoptimizeNode**: CONCERNING - many deoptimization points

**Problem**: Many DeoptimizeNode suggests unstable speculation or excessive guards
**Fix**: Improve type stability, check for polymorphic call sites

#### Query 7: Find Loop Structures
```bash
# Find loop-related nodes
cat [json-file] | jq -s '.[3] |
  .nodes |
  map(select(.props.node_class.node_class | test("Loop"))) |
  group_by(.props.node_class.node_class) |
  map({
    node_class: .[0].props.node_class.node_class,
    count: length
  })'
```

**Example Output**:
```json
[
  {"node_class": "jdk.graal.compiler.nodes.LoopBeginNode", "count": 1},
  {"node_class": "jdk.graal.compiler.nodes.LoopEndNode", "count": 1}
]
```

**Interpretation**:
- **LoopBeginNode count**: Number of loops
- **LoopEndNode count**: Should equal LoopBeginNode (one back-edge per loop ideal)
- **Multiple LoopEndNode per LoopBeginNode**: Complex control flow, multiple back-edges

#### Query 8: Type Check Operations (Polymorphism Indicators)
```bash
# Find type check nodes
cat [json-file] | jq -s '.[3] |
  .nodes |
  group_by(.props.node_class.node_class) |
  map({
    node_class: .[0].props.node_class.node_class,
    count: length
  }) |
  map(select(.node_class | test("InstanceOf|Checkcast"))) |
  sort_by(.count) |
  reverse'
```

**Example Output**:
```json
[
  {"node_class": "jdk.graal.compiler.nodes.java.InstanceOfNode", "count": 25}
]
```

**Interpretation**:
- **High InstanceOfNode count**: Indicates polymorphism or type checks
- **High CheckcastNode count**: Type casting operations, potential type pollution

**Problem**: 25 InstanceOfNode suggests polymorphic code paths
**Fix**: Add more specific @Specialization guards, use DSL to split execution by type

---

### Phase 5: Analyze and Report Findings

**Objective**: Interpret query results and generate actionable recommendations

**Analysis Checklist**:

1. **Call Overhead**
   - [ ] Count InvokeNode / InvokeWithExceptionNode
   - [ ] Count OptimizedIndirectCallNode (should be 0)
   - [ ] Identify which functions are called (not inlined)
   - [ ] Recommendation: Add CallTarget caching, improve specialization

2. **Boxing Overhead**
   - [ ] Count BoxNode / UnboxNode
   - [ ] Identify which operations cause boxing
   - [ ] Recommendation: Add primitive specializations, enable boxing elimination

3. **Allocation Overhead**
   - [ ] Check for CommitAllocationNode / NewInstanceNode after TruffleTier
   - [ ] Identify which allocations survived
   - [ ] Recommendation: Improve escape analysis, keep objects local

4. **Type Stability**
   - [ ] Count FixedGuardNode (high counts indicate instability)
   - [ ] Count DeoptimizeNode (should be minimal)
   - [ ] Count InstanceOfNode (indicates polymorphism)
   - [ ] Recommendation: Improve type specialization, reduce polymorphism

5. **Loop Optimization**
   - [ ] Identify loop structures
   - [ ] Check for loop-invariant hoisting opportunities
   - [ ] Recommendation: Manual hoisting if compiler didn't optimize

**Report Template**:
```markdown
# Compiler Graph Analysis Report

**Benchmark**: [name]
**Function Analyzed**: [function-name]
**Graph Phase**: After TruffleTier
**Total Nodes**: [count]
**Total Edges**: [count]

## Findings

### 1. Call Overhead
- **MethodCallTargetNode**: [count]
- **InvokeWithExceptionNode**: [count]
- **Status**: ⚠️ [count] method calls remaining in compiled code
- **Recommendation**: Add CallTarget caching for [specific calls]

### 2. Boxing Overhead
- **UnboxNode**: [count]
- **BoxNode**: [count]
- **Status**: ⚠️ Boxing overhead present
- **Recommendation**: Add primitive specializations for arithmetic operations

### 3. Escape Analysis
- **CommitAllocationNode**: [count]
- **Status**: ✅ Escape analysis successful (all allocations eliminated)

### 4. Type Stability
- **FixedGuardNode**: [count]
- **DeoptimizeNode**: [count]
- **InstanceOfNode**: [count]
- **Status**: ⚠️ High guard count suggests type instability
- **Recommendation**: Add more specific type guards, reduce polymorphism

## Summary

**Critical Issues**: [count]
**Moderate Issues**: [count]
**Expected Impact**: [estimate] if fixed

**Top Priority**: [most impactful fix]
```

---

## Example Usage Scenario

**User**: "Analyze compiler graphs for the queens benchmark to understand why performance is slow"

**Skill Actions**:

### Step 1: Generate Compiler Graphs
```bash
# Create output directory
mkdir -p compiler_graphs/queens

# Run benchmark with graph dumping
EXTRA_JAVA_ARGS="-Djdk.graal.Dump=Truffle:1 \
                 -Djdk.graal.PrintGraph=File \
                 -Djdk.graal.DumpPath=compiler_graphs/queens" \
<language-launcher> <program>

# Verify output
ls -lh compiler_graphs/queens/
# Generated:
# TruffleHotSpotCompilation-2745[root_getRowColumn].bgv  (732K)
# TruffleHotSpotCompilation-2801[root_placeQueen].bgv   (1.2M)
# TruffleHotSpotCompilation-3227[root_queens].bgv       (1.3M)
```

### Step 2: Quick Analysis with Seafoam
```bash
# List graphs in placeQueen function
seafoam "compiler_graphs/queens/TruffleHotSpotCompilation-2801[root_placeQueen].bgv" list
# Shows 9 graphs (different compilation phases)

# Analyze "After TruffleTier" phase (index 3)
seafoam --json "compiler_graphs/queens/TruffleHotSpotCompilation-2801[root_placeQueen].bgv:3" describe

# Result:
{
  "node_count": 426,
  "calls": true,
  "deopts": false,
  "loops": true,
  "node_counts": {
    "InvokeWithExceptionNode": 9,    # ⚠️ Calls remaining
    "UnboxNode": 8,                   # ⚠️ Boxing overhead
    "BoxNode$PureBoxNode": 4,         # ⚠️ Boxing overhead
    "FixedGuardNode": 59              # ⚠️ Many guards
  }
}

# Initial findings:
# ⚠️ 9 method calls not eliminated
# ⚠️ Boxing overhead (12 box/unbox operations)
# ⚠️ High guard count (potential type instability)
```

### Step 3: Convert to JSON for Deep Analysis
```bash
# Convert to JSON
bgv2json "compiler_graphs/queens/TruffleHotSpotCompilation-2801[root_placeQueen].bgv" \
  > compiler_graphs/queens/placeQueen.json

# Verify
cat compiler_graphs/queens/placeQueen.json | jq -s '.[3] | .name'
# ["TruffleIR.Tier1.root_placeQueen()","After TruffleTier"]
```

### Step 4: Query for Specific Issues
```bash
# Query 1: Find call overhead
cat compiler_graphs/queens/placeQueen.json | jq -s '.[3] |
  .nodes |
  group_by(.props.node_class.node_class) |
  map({node_class: .[0].props.node_class.node_class, count: length}) |
  map(select(.node_class | test("Call")))'

# Result: 9 MethodCallTargetNode
# Interpretation: 9 method calls not optimized away

# Query 2: Find boxing overhead
cat compiler_graphs/queens/placeQueen.json | jq -s '.[3] |
  .nodes |
  group_by(.props.node_class.node_class) |
  map({node_class: .[0].props.node_class.node_class, count: length}) |
  map(select(.node_class | test("Box|Unbox")))'

# Result: 8 UnboxNode, 4 BoxNode$PureBoxNode
# Interpretation: Significant boxing overhead for primitive operations

# Query 3: Check escape analysis
cat compiler_graphs/queens/placeQueen.json | jq -s '.[3] |
  .nodes |
  group_by(.props.node_class.node_class) |
  map({node_class: .[0].props.node_class.node_class, count: length}) |
  map(select(.node_class | test("Alloc|New|Commit")))'

# Result: []
# Interpretation: ✅ Escape analysis successful - all allocations eliminated
```

### Step 5: Generate Report
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
- **Impact**: Prevents full optimization, adds overhead
- **Recommendation**:
  - Add CallTarget caching with @Cached for recursive calls
  - Ensure helper methods inline properly
  - Check trace-inlining to see which calls didn't inline

### 2. Boxing Overhead ⚠️ MODERATE
- **UnboxNode**: 8
- **BoxNode$PureBoxNode**: 4
- **Status**: ⚠️ 12 boxing operations present
- **Impact**: Allocation overhead, prevents scalar replacement
- **Recommendation**:
  - Add primitive specializations for arithmetic (`int`, `long`)
  - Enable boxing elimination in @GenerateBytecode config
  - Use @Specialization with primitive parameter types

### 3. Escape Analysis ✅ SUCCESS
- **CommitAllocationNode**: 0
- **Status**: ✅ All allocations successfully eliminated
- **Impact**: Optimal - no allocation overhead

### 4. Type Stability ⚠️ MODERATE
- **FixedGuardNode**: 59
- **DeoptimizeNode**: 0
- **InstanceOfNode**: 15
- **Status**: ⚠️ Many guards, but no deoptimizations
- **Impact**: Guard overhead acceptable, no deopt instability
- **Recommendation**: Guards are working correctly, not a priority

## Summary

**Critical Issues**: 1 (call overhead)
**Moderate Issues**: 1 (boxing overhead)
**Expected Impact**: 3-5x speedup if both issues fixed

**Top Priority**: Add CallTarget caching to eliminate 9 remaining method calls

**Next Steps**:
1. Implement CallTarget caching for recursive placeQueen calls
2. Add primitive specializations to eliminate boxing
3. Re-run benchmark and verify improvements
```

---

## Tool Selection Guide

**When to Use Compiler Graph Analysis**:
- **AFTER** trace-performance-warnings shows issues
- **AFTER** trace-inlining shows successful inlining but performance still poor
- **AFTER** cpu-sampler shows high compiled time but slow execution
- **When** you need to verify WHAT the compiler actually did
- **When** you suspect escape analysis, boxing, or indirect call issues

**When NOT to Use**:
- **BEFORE** simpler tools (cpu-sampler, trace-performance-warnings, trace-inlining)
- **When** the issue is obvious from traces or warnings
- **When** you're just starting performance investigation

**Workflow**:
```
1. cpu-sampler → Identify hot functions
2. trace-performance-warnings → Find optimization barriers
3. trace-inlining → Verify inlining decisions
4. analyze-compiler-graph → Understand WHAT the compiler did (if still unclear)
```

---

## Common Patterns and Interpretations

### Good Patterns (Optimized Code)

✅ **High ConstantNode count**: Constant folding working
✅ **Zero allocation nodes after TruffleTier**: Escape analysis successful
✅ **Low InvokeNode count**: Calls inlined or specialized
✅ **Zero BoxNode/UnboxNode**: No boxing overhead
✅ **Low DeoptimizeNode count**: Stable speculation

### Bad Patterns (Optimization Failures)

❌ **High InvokeNode/InvokeWithExceptionNode count**: Calls not optimized
❌ **OptimizedIndirectCallNode present**: Indirect calls preventing optimization
❌ **High BoxNode/UnboxNode count**: Boxing overhead
❌ **CommitAllocationNode after TruffleTier**: Escape analysis failed
❌ **High FixedGuardNode with high DeoptimizeNode**: Type instability
❌ **High InstanceOfNode count**: Polymorphic code

---

## Best Practices

### 1. Always Focus on "After TruffleTier" Graph
- This shows Truffle-specific optimization results
- Earlier phases are less relevant for Truffle languages
- Later phases show generic Graal optimizations

### 2. Start with Seafoam for Quick Overview
- Use `seafoam --json [file]:3 describe` for instant statistics
- Identify major issues before deep diving
- Saves time compared to full JSON analysis

### 3. Systematic Query Workflow
1. Graph statistics (nodes, edges)
2. Node type distribution (top 15)
3. Call nodes (optimization opportunities)
4. Boxing nodes (primitive specialization opportunities)
5. Allocation nodes (escape analysis effectiveness)
6. Deoptimization nodes (stability check)
7. Type check nodes (polymorphism indicators)

### 4. Correlate with Other Tools
- **trace-performance-warnings** → Identifies issues
- **Compiler graphs** → Confirms issues in IR
- **Example**: Warning says "virtual call" → Graph shows OptimizedIndirectCallNode

### 5. Compare Before/After Optimization
- Generate graphs before fix
- Apply fix (e.g., add CallTarget caching)
- Generate graphs after fix
- Compare node counts to verify improvement

---

## Common Pitfalls

❌ **Analyzing wrong graph phase**: Focus on "After TruffleTier" (usually index 3)
❌ **Not using seafoam first**: JSON queries are slow; seafoam gives quick overview
❌ **Analyzing cold code**: Only analyze hot functions identified by cpu-sampler
❌ **Ignoring other tools**: Graphs alone don't explain WHY; use trace-performance-warnings
❌ **Over-interpreting small counts**: A few guards/invokes may be acceptable
❌ **Comparing graphs from different runs**: Different compilation IDs, not comparable

---

## Reference Commands

### Generate Compiler Graphs
```bash
EXTRA_JAVA_ARGS="-Djdk.graal.Dump=Truffle:1 \
                 -Djdk.graal.PrintGraph=File \
                 -Djdk.graal.DumpPath=compiler_graphs/[name]" \
<language-launcher> <program>
```

### Seafoam Quick Analysis
```bash
# List graphs
seafoam "[file].bgv" list

# Describe specific graph
seafoam --json "[file].bgv:3" describe
```

### Convert to JSON
```bash
bgv2json "[file].bgv" > output.json
```

### Essential jq Queries
```bash
# Count graphs
cat [json] | jq -s 'length'

# List graph names
cat [json] | jq -r '.name'

# Graph statistics
cat [json] | jq -s '.[3] | {total_nodes: (.nodes|length), total_edges: (.edges|length)}'

# Top 15 node types
cat [json] | jq -s '.[3] | .nodes | group_by(.props.node_class.node_class) | map({node_class: .[0].props.node_class.node_class, count: length}) | sort_by(.count) | reverse | .[0:15]'

# Find calls
cat [json] | jq -s '.[3] | .nodes | group_by(.props.node_class.node_class) | map({node_class: .[0].props.node_class.node_class, count: length}) | map(select(.node_class | test("Call")))'

# Find boxing
cat [json] | jq -s '.[3] | .nodes | group_by(.props.node_class.node_class) | map({node_class: .[0].props.node_class.node_class, count: length}) | map(select(.node_class | test("Box|Unbox")))'

# Find allocations
cat [json] | jq -s '.[3] | .nodes | group_by(.props.node_class.node_class) | map({node_class: .[0].props.node_class.node_class, count: length}) | map(select(.node_class | test("Alloc|New|Commit")))'

# Count deopts
cat [json] | jq -s '.[3] | .nodes | map(select(.props.node_class.node_class | test("Deopt"))) | length'
```

---

## Related Documentation

- **Dump Compiler Graph.md**: Complete reference for `-Djdk.graal.Dump` option
- **Seafoam**: https://github.com/Shopify/seafoam
- **BGV Format**: https://github.com/Shopify/seafoam/blob/main/docs/bgv.md
- **Ideal Graph Visualizer (IGV)**: Traditional GUI tool for BGV visualization
- **GraalVM Compiler**: https://www.graalvm.org/latest/reference-manual/compiler/

---

## Success Criteria

**Good Analysis**:
- ✅ Compiler graphs generated for hot functions
- ✅ Seafoam analysis shows optimization quality
- ✅ JSON queries identify specific issues
- ✅ Findings correlate with trace-performance-warnings
- ✅ Actionable recommendations generated

**Excellent Analysis**:
- ✅ Before/after comparison shows optimization improvements
- ✅ All major optimization barriers identified in IR
- ✅ Specific node IDs referenced in recommendations
- ✅ Verification that fixes eliminated problematic nodes
- ✅ Documentation of expected IR patterns for future reference
