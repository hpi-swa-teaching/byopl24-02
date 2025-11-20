# Analysis


## 4. Output Format & Structure

### Output characteristics

- **Output type**: Text-based console output to standard output (stdout)
- **Output location**: Console by default; redirectable using standard shell redirection
- **Format prefix**: All lines prefixed with `[engine]` tag
- **Timing**: Output appears when each compilation completes
- **Structure**: Depth-first traversal of the call tree with indentation indicating nesting depth

### Example output with detailed annotations

```
[engine] inline start M.CollidePolygons |call diff 0.00 |Recursion Depth 0 |Explore/inline ratio 1.07 |IR Nodes 27149 |Frequency 1.00 |Truffle Callees 14 |Forced false |Depth 0

[engine] Inlined M.FindMaxSeparation <opt> |call diff -8.99 |Recursion Depth 0 |Explore/inline ratio NaN |IR Nodes 4617 |Frequency 1.00 |Truffle Callees 7 |Forced false |Depth 1

[engine] Inlined parseInt <opt> |call diff -1.00 |Recursion Depth 0 |Explore/inline ratio NaN |IR Nodes 111 |Frequency 1.00 |Truffle Callees 0 |Forced true |Depth 2

[engine] Inlined M.EdgeSeparation |call diff -3.00 |Recursion Depth 0 |Explore/inline ratio NaN |IR Nodes 4097 |Frequency 1.00 |Truffle Callees 2 |Forced false |Depth 2

[engine] Expanded M.EdgeSeparation |call diff 1.00 |Recursion Depth 0 |Explore/inline ratio NaN |IR Nodes 4097 |Frequency 1.00 |Truffle Callees 2 |Forced false |Depth 2

[engine] Cutoff M.FindMaxSeparation <opt> |call diff 1.00 |Recursion Depth 0 |Explore/inline ratio NaN |IR Nodes 0 |Frequency 1.00 |Truffle Callees 7 |Forced false |Depth 1

[engine] Cutoff M.FindIncidentEdge <opt> |call diff 1.00 |Recursion Depth 0 |Explore/inline ratio NaN |IR Nodes 0 |Frequency 1.00 |Truffle Callees 19 |Forced false |Depth 1

[engine] inline done M.CollidePolygons |call diff 0.00 |Recursion Depth 0 |Explore/inline ratio 1.07 |IR Nodes 27149 |Frequency 1.00 |Truffle Callees 14 |Forced false |Depth 0
```

**Line-by-line interpretation:**
1. **inline start**: Beginning of compilation for root function `CollidePolygons` with 27,149 IR nodes, 14 Truffle callees, exploration/inlining ratio of 1.07
2. **Inlined**: Successfully inlined `FindMaxSeparation`, reducing net calls by 8.99 (excellent outcome)
3. **Inlined**: Successfully inlined `parseInt` at depth 2 (nested inside FindMaxSeparation), forced inlining
4. **Inlined**: Successfully inlined first `EdgeSeparation` call, reducing calls by 3
5. **Expanded**: Second `EdgeSeparation` call evaluated but NOT inlined (would increase calls by 1)
6. **Cutoff**: Exploration budget exhausted, remaining calls not evaluated (0 IR nodes)
7. **Cutoff**: Another unexplored call due to budget exhaustion
8. **inline done**: Compilation completed for this root

### Output fields and metrics table

| Field Name               | Description                            | Unit    | Interpretation                                                     |
| ------------------------ | -------------------------------------- | ------- | ------------------------------------------------------------------ |
| **State**                | Inlining decision outcome              | Enum    | Inlined/Expanded/Cutoff/Removed/Indirect/BailedOut                 |
| **Function Name**        | Target being compiled/inlined          | String  | May include `<opt>` (optimized), `<split-ID>` (split target)       |
| **call diff**            | Net change in call count from inlining | Float   | Negative = fewer calls (good), positive = more calls (usually bad) |
| **Recursion Depth**      | Recursive call nesting level           | Integer | 0 = not recursive, higher = deeper recursion                       |
| **Explore/inline ratio** | Exploration vs inlining budget ratio   | Float   | Only for root; NaN for nested calls                                |
| **IR Nodes**             | Graal IR node count after PE           | Integer | 0 for Cutoff; primary size metric                                  |
| **Frequency**            | Estimated execution frequency          | Float   | 1.0 = once per parent; higher for loops                            |
| **Truffle Callees**      | Number of outgoing calls               | Integer | Indicates potential for further inlining                           |
| **Forced**               | Whether inlining was mandatory         | Boolean | true = annotation/language forced, false = heuristic decision      |
| **Depth**                | Inlining depth in call tree            | Integer | 0 = root, higher = deeper nesting                                  |

### Output interpretation guidelines

**Successful inlining pattern:**
```
[engine] Inlined FunctionName |call diff -5.00 |... |IR Nodes 2500 |...
```
Negative call diff indicates net call elimination—inlining this function removed more calls than it added (by eliminating the call itself and potentially enabling optimization of nested calls). Reasonable IR node count under budget (default 12,000) means no risk of compilation unit explosion.

**Budget exhaustion pattern:**
```
[engine] Cutoff FunctionName |... |IR Nodes 0 |...
```
Zero IR nodes signifies the function wasn't partially evaluated due to exploration budget exhaustion. If this function represents a hot path, consider increasing `--engine.InliningExpansionBudget`. However, cutoff is normal and acceptable for cold paths.

**Too expensive pattern:**
```
[engine] Expanded FunctionName |call diff 1.50 |... |IR Nodes 8500 |...
```
Positive call diff combined with expanded state means inlining this function would increase net call overhead (perhaps it contains many calls to other functions that wouldn't inline). The compiler evaluated it but decided against inlining. This may be correct—inlining isn't always beneficial if it prevents better inlining opportunities elsewhere.

**Optimal elimination pattern:**
```
[engine] Removed FunctionName |call diff -1.00 |... |IR Nodes 0 |...
```
The call was completely optimized away during partial evaluation. This is the best possible outcome—no call overhead, no code size increase. Typically occurs with simple getters, constant functions, or calls that constant folding can eliminate.

---
