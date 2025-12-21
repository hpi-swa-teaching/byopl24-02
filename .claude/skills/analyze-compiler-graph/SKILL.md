---
name: analyze-compiler-graph
description: Deep IR-level analysis of compiler optimization decisions. Identifies what optimizations succeeded or failed (escape analysis, inlining, specialization). Use as last resort after simpler profiling tools when root cause remains unclear.
---

# Skill: Analyze Compiler Graphs

## Contents
- [What This Skill Does](#what-this-skill-does)
- [When to Use This Skill](#when-to-use-this-skill)
- [Prerequisites](#prerequisites)
- [How This Skill Works](#how-this-skill-works)
- [Tool Selection Guide](#tool-selection-guide)
- [Best Practices](#best-practices)
- [Reference Commands](#reference-commands)

---

## What This Skill Does

Performs deep IR-level analysis of Graal compiler graphs to understand optimization decisions:

1. **Generates Compiler Graphs**: Runs benchmarks with `-Djdk.graal.Dump=Truffle:1`
2. **Analyzes with Seafoam**: Quick graph summaries and statistics
3. **Converts to JSON**: Uses `bgv2json` for detailed querying
4. **Queries with jq**: Systematic analysis to find optimization failures
5. **Generates Report**: Documents findings with IR-level evidence

**Key Insights Provided**:
- Indirect calls vs direct calls (OptimizedIndirectCallNode)
- Boxing/unboxing overhead (BoxNode, UnboxNode)
- Failed escape analysis (allocation nodes after PartialEscape phase)
- Type instability (guards, deoptimization nodes)
- Polymorphism indicators (InstanceOf, Checkcast)

---

## When to Use This Skill

Use as **LAST RESORT** after simpler tools show problems but don't reveal root causes:

- **After trace-performance-warnings** shows optimization barriers but you need to see WHAT the compiler actually did
- **When allocations should be eliminated** but escape analysis appears to fail
- **When boxing overhead is suspected** but you need IR-level confirmation
- **When indirect calls are suspected** and you need to verify their presence
- **Deep investigation** when other tools indicate problems but root cause is unclear

**Important**: This is the most complex diagnostic tool. Always use simpler tools first:
1. `cpu-sampler` - Find where time is spent
2. `trace-performance-warnings` - Find optimization barriers
3. `trace-inlining` - Understand inlining decisions
4. **Only then** → Analyze compiler graphs

---

## Prerequisites

### Required Tools
- **GraalVM** with Truffle (already installed if running Truffle language)
- **seafoam**: Ruby gem for BGV analysis
  ```bash
  gem install seafoam
  ```
- **bgv2json**: Ruby gem for BGV to JSON conversion
  ```bash
  gem install bgv2json
  ```
- **jq**: JSON query tool
  ```bash
  brew install jq  # macOS
  ```

### Verify Installation

Run this script to verify all required tools are available:

```bash
#!/bin/bash
echo "Verifying compiler graph analysis tools..."

check_tool() {
  if command -v "$1" &> /dev/null; then
    echo "✅ $1 is installed"
    return 0
  else
    echo "❌ $1 is NOT installed"
    return 1
  fi
}

check_tool "seafoam"
check_tool "bgv2json"
check_tool "jq"

echo ""
echo "Done. Install missing tools before proceeding."
```

### Required Files
- Benchmark programs to analyze
- Sufficient disk space (100MB-1GB per benchmark)

---

## How This Skill Works

### Phase 1: Generate Compiler Graphs

**Objective**: Run benchmark with Graal graph dumping enabled

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
- `-Djdk.graal.Dump=Truffle:1`: Dump Truffle compilation graphs
- `-Djdk.graal.PrintGraph=File`: Write to filesystem (not network/IGV)
- `-Djdk.graal.DumpPath=<path>`: Custom output directory

**Output**: BGV files named like:
```
TruffleHotSpotCompilation-2745[root_getRowColumn].bgv
TruffleHotSpotCompilation-2801[root_placeQueen].bgv
```

---

### Phase 2: Analyze with Seafoam (Quick Analysis)

**Objective**: Get high-level graph statistics without deep diving

**Process**:

1. **List graphs in BGV file**
   ```bash
   seafoam "[bgv-file]" list
   ```

   Key phases to look for:
   - `After PE Tier` - After partial evaluation
   - `After TruffleTier` - **MOST IMPORTANT** - After Truffle optimizations
   - `After high tier` - After high-level Graal optimizations

2. **Describe specific graph** (focus on "After TruffleTier")
   ```bash
   seafoam --json "[bgv-file]:3" describe
   ```

   Returns node counts like:
   ```json
   {
     "node_count": 426,
     "calls": true,
     "deopts": false,
     "loops": true,
     "node_counts": {
       "FixedGuardNode": 59,
       "InvokeWithExceptionNode": 9,
       "UnboxNode": 8,
       "BoxNode$PureBoxNode": 4
     }
   }
   ```

3. **Identify optimization issues**
   - **InvokeWithExceptionNode** count: Should be minimal (indicates calls remaining)
   - **OptimizedIndirectCallNode**: BAD - should be OptimizedDirectCallNode
   - **BoxNode/UnboxNode**: BAD - boxing overhead
   - **CommitAllocationNode** after TruffleTier: BAD - escape analysis failed
   - **DeoptimizeNode** count: Should be low
   - **FixedGuardNode** count: High values indicate type instability

---

### Phase 3: Convert to JSON with bgv2json

**Objective**: Convert BGV files to JSON for detailed querying

**Process**:

1. **Convert BGV to JSON**
   ```bash
   bgv2json "[bgv-file]" > [output.json]
   ```

2. **Verify JSON structure**
   ```bash
   cat [output.json] | jq -s 'length'  # Count graphs
   cat [output.json] | jq -r '.name'   # List graph names
   ```

**Output**: JSON Lines format (one JSON object per graph per line)

**JSON Structure**:
```json
{
  "name": ["TruffleIR.Tier1.root_placeQueen()", "After TruffleTier"],
  "nodes": [
    {
      "id": 0,
      "props": {
        "node_class": {
          "node_class": "jdk.graal.compiler.nodes.StartNode",
          "name_template": "Start"
        }
      }
    }
  ],
  "edges": [{"from": 0, "to": 11, "props": {}}]
}
```

---

### Phase 4: Query with jq (Deep Analysis)

**Objective**: Run systematic queries to identify specific performance issues

For detailed jq queries and examples, see **[QUERIES.md](QUERIES.md)**

**Query Categories**:
1. Graph statistics (nodes, edges)
2. Node type distribution
3. Call nodes (optimization opportunities)
4. Boxing overhead (BoxNode/UnboxNode)
5. Allocation nodes (escape analysis)
6. Deoptimization nodes (stability)
7. Loop structures
8. Type checks (polymorphism)

---

### Phase 5: Analyze and Report Findings

**Objective**: Interpret query results and generate actionable recommendations

Copy this checklist to track analysis progress:

```
Analysis Progress:
- [ ] 1. Assess call overhead (InvokeNode counts)
- [ ] 2. Check boxing overhead (BoxNode/UnboxNode)
- [ ] 3. Verify escape analysis (allocation nodes)
- [ ] 4. Evaluate type stability (guards, deopts)
- [ ] 5. Generate report with findings
```

**Analysis Checklist**:

1. **Call Overhead**
   - Count InvokeNode / InvokeWithExceptionNode
   - Count OptimizedIndirectCallNode (should be 0)
   - Identify which functions are called (not inlined)
   - Recommendation: Add CallTarget caching, improve specialization

2. **Boxing Overhead**
   - Count BoxNode / UnboxNode
   - Identify which operations cause boxing
   - Recommendation: Add primitive specializations, enable boxing elimination

3. **Allocation Overhead**
   - Check for CommitAllocationNode / NewInstanceNode after TruffleTier
   - Identify which allocations survived
   - Recommendation: Improve escape analysis, keep objects local

4. **Type Stability**
   - Count FixedGuardNode (high counts indicate instability)
   - Count DeoptimizeNode (should be minimal)
   - Count InstanceOfNode (indicates polymorphism)
   - Recommendation: Improve type specialization, reduce polymorphism

5. **Loop Optimization**
   - Identify loop structures
   - Check for loop-invariant hoisting opportunities
   - Recommendation: Manual hoisting if compiler didn't optimize

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
- **Status**: ⚠️/✅
- **Recommendation**: [specific actions]

### 2. Boxing Overhead
- **UnboxNode**: [count]
- **BoxNode**: [count]
- **Status**: ⚠️/✅
- **Recommendation**: [specific actions]

### 3. Escape Analysis
- **CommitAllocationNode**: [count]
- **Status**: ⚠️/✅

### 4. Type Stability
- **FixedGuardNode**: [count]
- **DeoptimizeNode**: [count]
- **InstanceOfNode**: [count]
- **Status**: ⚠️/✅
- **Recommendation**: [specific actions]

## Summary

**Critical Issues**: [count]
**Moderate Issues**: [count]
**Expected Impact**: [estimate]

**Top Priority**: [most impactful fix]
```

For complete usage examples, see **[EXAMPLES.md](EXAMPLES.md)**

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

**Recommended Workflow**:
```
1. cpu-sampler → Identify hot functions
2. trace-performance-warnings → Find optimization barriers
3. trace-inlining → Verify inlining decisions
4. analyze-compiler-graph → Understand WHAT the compiler did (if still unclear)
```

---

## Best Practices

### 1. Always Focus on "After TruffleTier" Graph
- Shows Truffle-specific optimization results
- Usually at index 3 when listing graphs
- Most relevant for Truffle languages

### 2. Start with Seafoam for Quick Overview
- Use `seafoam --json [file]:3 describe` for instant statistics
- Identify major issues before deep diving
- Saves time compared to full JSON analysis

### 3. Systematic Query Workflow
1. Graph statistics (nodes, edges)
2. Node type distribution (top 15)
3. Call nodes → Boxing nodes → Allocation nodes
4. Deoptimization nodes → Type check nodes

### 4. Correlate with Other Tools
- **trace-performance-warnings** → Identifies issues
- **Compiler graphs** → Confirms issues in IR
- Example: Warning says "virtual call" → Graph shows OptimizedIndirectCallNode

### 5. Compare Before/After Optimization
- Generate graphs before fix
- Apply fix (e.g., add CallTarget caching)
- Generate graphs after fix
- Compare node counts to verify improvement

### 6. Common Pitfalls to Avoid
- ❌ Analyzing wrong graph phase (always use "After TruffleTier")
- ❌ Not using seafoam first (JSON queries are slower)
- ❌ Analyzing cold code (only analyze hot functions from cpu-sampler)
- ❌ Ignoring other tools (graphs alone don't explain WHY)
- ❌ Over-interpreting small counts (a few guards/invokes may be acceptable)

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

# Describe specific graph (usually index 3 for "After TruffleTier")
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
```

For complete query reference, see **[QUERIES.md](QUERIES.md)**

---

## Related Files

- **[QUERIES.md](QUERIES.md)**: Complete jq query reference with examples
- **[EXAMPLES.md](EXAMPLES.md)**: Full usage scenario walkthrough

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
