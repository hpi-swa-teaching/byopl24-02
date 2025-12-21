# Compiler Graph Queries Reference

Complete reference for jq queries to analyze compiler graphs in JSON format.

All queries below assume the JSON file was generated with `bgv2json` and target the "After TruffleTier" graph (usually at index 3).

---

## Query 1: Graph Statistics

**Purpose**: Get basic graph information

```bash
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

**Interpretation**:
- Total nodes indicates compilation complexity
- High node count doesn't always mean problems
- Use as baseline for understanding graph size

---

## Query 2: Node Type Distribution (Top 15)

**Purpose**: Find most common node types to identify patterns

```bash
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
  {"node_class": "jdk.graal.compiler.nodes.java.LoadFieldNode", "count": 23}
]
```

**Interpretation**:
- **High ConstantNode count**: GOOD - constant folding working
- **High FixedGuardNode count**: May indicate type instability
- **High FrameState count**: Normal for complex functions
- **High LoadFieldNode**: Field access overhead (may be acceptable)

---

## Query 3: Find Call Nodes (Optimization Opportunities)

**Purpose**: Identify method calls that weren't eliminated

```bash
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
- **InvokeNode / InvokeWithExceptionNode**: BAD - method calls remaining
- **OptimizedIndirectCallNode**: BAD - indirect calls preventing optimization
- **OptimizedDirectCallNode**: ACCEPTABLE if minimal
- **MethodCallTargetNode**: Count indicates call overhead

**Problem**: 10 MethodCallTargetNode = 10 method calls in compiled code

**Fix**: Add CallTarget caching with `@Cached`, improve specialization

---

## Query 4: Find Boxing Overhead

**Purpose**: Identify boxing/unboxing operations

```bash
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
- **UnboxNode / BoxNode**: BAD - boxing/unboxing wastes allocations
- **High counts**: Missing primitive specializations

**Problem**: 18 UnboxNode + 4 BoxNode = significant boxing overhead

**Fix**:
- Add primitive specializations (`int`, `long`, `double`)
- Enable boxing elimination in bytecode config:
  ```java
  @GenerateBytecode(boxingEliminationTypes = {long.class})
  ```

---

## Query 5: Find Allocation Nodes (Failed Escape Analysis)

**Purpose**: Check if escape analysis successfully eliminated allocations

```bash
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

**Example Output (Good)**:
```json
[]
```

**Example Output (Bad)**:
```json
[
  {"node_class": "jdk.graal.compiler.nodes.virtual.CommitAllocationNode", "count": 5},
  {"node_class": "jdk.graal.compiler.nodes.java.NewInstanceNode", "count": 2}
]
```

**Interpretation**:
- **Empty result after "After TruffleTier"**: GOOD - escape analysis successful
- **CommitAllocationNode present**: BAD - allocations survived optimization
- **NewInstanceNode present**: BAD - object allocations remaining
- **NewArrayNode present**: BAD - array allocations remaining

**Problem**: Allocations present = escape analysis failed

**Fix**:
- Refactor to keep object lifetimes local
- Ensure inlining succeeds (check trace-inlining)
- Avoid storing in fields unless necessary

---

## Query 6: Find Deoptimization Nodes

**Purpose**: Check compilation stability

```bash
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
- **1-5 DeoptimizeNode**: ACCEPTABLE - uncommon paths guarded
- **>10 DeoptimizeNode**: CONCERNING - many deoptimization points

**Problem**: Many DeoptimizeNode suggests unstable speculation or excessive guards

**Fix**:
- Improve type stability
- Check for polymorphic call sites
- Use trace-transfer-to-interpreter to see if deoptimizations actually occur

---

## Query 7: Find Loop Structures

**Purpose**: Understand loop organization and complexity

```bash
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
- **LoopBeginNode count**: Number of loops in the function
- **LoopEndNode count**: Should equal LoopBeginNode (one back-edge per loop ideal)
- **Multiple LoopEndNode per LoopBeginNode**: Complex control flow, multiple back-edges

**Use Case**: Understanding loop structure helps identify opportunities for loop-invariant hoisting

---

## Query 8: Type Check Operations (Polymorphism Indicators)

**Purpose**: Identify polymorphism and type checking overhead

```bash
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

**Fix**:
- Add more specific `@Specialization` guards
- Use DSL to split execution by type
- Consider separate specializations for each type

---

## Query 9: Find Guard Nodes (Type Stability Indicator)

**Purpose**: Measure type stability through guard count

```bash
cat [json-file] | jq -s '.[3] |
  .nodes |
  group_by(.props.node_class.node_class) |
  map({
    node_class: .[0].props.node_class.node_class,
    count: length
  }) |
  map(select(.node_class | test("Guard"))) |
  sort_by(.count) |
  reverse'
```

**Example Output**:
```json
[
  {"node_class": "jdk.graal.compiler.nodes.FixedGuardNode", "count": 59}
]
```

**Interpretation**:
- **Low FixedGuardNode count (<20)**: GOOD - stable types
- **Medium count (20-50)**: ACCEPTABLE - normal guards
- **High count (>50)**: CONCERNING - type instability

**Note**: Guards are necessary for correctness but high counts combined with DeoptimizeNode indicate problems

---

## Query 10: Find Invoke Nodes (Direct Count)

**Purpose**: Directly count method invocations remaining in compiled code

```bash
cat [json-file] | jq -s '.[3] |
  .nodes |
  group_by(.props.node_class.node_class) |
  map({
    node_class: .[0].props.node_class.node_class,
    count: length
  }) |
  map(select(.node_class | test("Invoke"))) |
  sort_by(.count) |
  reverse'
```

**Example Output**:
```json
[
  {"node_class": "jdk.graal.compiler.nodes.InvokeWithExceptionNode", "count": 9}
]
```

**Interpretation**:
- **0 InvokeNode**: IDEAL - all calls inlined/specialized
- **1-3 InvokeNode**: ACCEPTABLE - minimal call overhead
- **>5 InvokeNode**: BAD - significant call overhead

**Problem**: Each InvokeNode represents a method call that couldn't be eliminated

**Fix**: Check trace-inlining to understand why calls weren't inlined

---

## Query Workflow Template

Use this systematic workflow when analyzing a new graph:

```bash
# Step 1: Verify JSON and identify "After TruffleTier" index
cat [json] | jq -s 'length'
cat [json] | jq -r '.name' | grep -n "After TruffleTier"

# Step 2: Basic statistics
cat [json] | jq -s '.[3] | {total_nodes: (.nodes|length), total_edges: (.edges|length)}'

# Step 3: Top node types
cat [json] | jq -s '.[3] | .nodes | group_by(.props.node_class.node_class) | map({node_class: .[0].props.node_class.node_class, count: length}) | sort_by(.count) | reverse | .[0:15]'

# Step 4: Critical checks (run all in parallel)
cat [json] | jq -s '.[3] | .nodes | group_by(.props.node_class.node_class) | map({node_class: .[0].props.node_class.node_class, count: length}) | map(select(.node_class | test("Call")))'

cat [json] | jq -s '.[3] | .nodes | group_by(.props.node_class.node_class) | map({node_class: .[0].props.node_class.node_class, count: length}) | map(select(.node_class | test("Box|Unbox")))'

cat [json] | jq -s '.[3] | .nodes | group_by(.props.node_class.node_class) | map({node_class: .[0].props.node_class.node_class, count: length}) | map(select(.node_class | test("Alloc|New|Commit")))'

cat [json] | jq -s '.[3] | .nodes | map(select(.props.node_class.node_class | test("Deopt"))) | length'

# Step 5: Additional analysis as needed
# (guards, loops, type checks based on Step 3 results)
```

---

## Custom Query Tips

### Filter by Specific Node Class
```bash
cat [json] | jq -s '.[3] | .nodes | map(select(.props.node_class.node_class == "jdk.graal.compiler.nodes.InvokeWithExceptionNode"))'
```

### Count Nodes Matching Pattern
```bash
cat [json] | jq -s '.[3] | .nodes | map(select(.props.node_class.node_class | test("YourPattern"))) | length'
```

### Extract Node IDs for Specific Type
```bash
cat [json] | jq -s '.[3] | .nodes | map(select(.props.node_class.node_class | test("BoxNode"))) | map(.id)'
```

### Group by Multiple Criteria
```bash
cat [json] | jq -s '.[3] | .nodes | group_by(.props.node_class.node_class) | map({type: .[0].props.node_class.node_class, count: length, ids: map(.id)})'
```

---

## Performance Tips

1. **Use `jq -s` for slurping**: Converts JSON Lines to array for indexing
2. **Cache JSON files**: Don't re-convert BGV for each query
3. **Use index 3 for "After TruffleTier"**: Verify with `jq -r '.name'` first
4. **Combine queries when possible**: Multiple `map(select(...))` in one pipeline
5. **Save query results**: Pipe to files for comparison across optimization iterations
