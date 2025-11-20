# Resolutions

This guide provides solutions to common issues identified by trace-inlining.

## Common Issues and Fixes

### Pattern 1: Budget Exhaustion (Critical Functions Hit Cutoff)

```
[engine] inline start bigFunction |IR Nodes 27149 |Truffle Callees 14 |...
[engine] Inlined helper1 |call diff -3.00 |IR Nodes 2100 |...
[engine] Inlined helper2 |call diff -2.50 |IR Nodes 3500 |...
[engine] Cutoff criticalHelper |IR Nodes 0 |...        ❌ Problem!
[engine] Cutoff anotherHelper |IR Nodes 0 |...
```

**Problem**: Critical functions reaching Cutoff state early

**Symptoms**:
- Important hot path functions showing "Cutoff" with 0 IR nodes
- Cutoff appears early in call tree traversal
- Exploration budget exhausted too soon

**Root Cause**:
- Default exploration budget (12,000 nodes) exhausted
- Too many potential callees consuming budget
- Complex compilation units

**Resolution**:
```bash
# Increase exploration budget by 50%
./lox --experimental-options \
  --engine.TraceInlining \
  --engine.InliningExpansionBudget=18000 \
  program.lox
```
- Increase conservatively (25-50% increments)
- Use profiling to confirm cutoff functions are actually hot
- Verify improvement with benchmarks

**Alternative**: Refactor code structure
- Split large functions into smaller pieces
- Reduce call fan-out
- Mark cold paths with `@TruffleBoundary`

### Pattern 2: Large Functions Blocking Inlining

```
[engine] Expanded hugeFunction |call diff 1.50 |IR Nodes 15000 |...  ❌ Too large!
```

**Problem**: Functions marked "Expanded" with high IR node counts

**Symptoms**:
- Hot path functions marked "Expanded"
- IR node counts approaching or exceeding 12,000
- Often positive call diff values

**Root Cause**:
- Function's compiled size exceeds available inlining budget
- Function contains many calls that wouldn't inline
- High specialization diversity generates many combinations

**Resolution**:
1. **Refactor into smaller functions**:
   ```lox
   // Before: One large function
   fun processData(data) {
     // 100 lines of code
   }

   // After: Split into focused pieces
   fun processData(data) {
     var validated = validateData(data);
     var transformed = transformData(validated);
     return formatOutput(transformed);
   }
   ```

2. **Isolate cold paths**: Mark error handling and rare features
3. **Review specialization**: Sometimes fewer, more general specializations help
4. **Consider accepting Expanded**: Not every function should inline

### Pattern 3: Compilation Bailout (Critical!)

```
[engine] BailedOut problematicFunction |...  🚨 Critical issue!
```

**Problem**: Partial evaluation failures (rare but serious)

**Symptoms**: BailedOut state for any function

**Root Cause**:
- Unbounded loops without compilation-final loop counts
- Recursive structures without termination guarantees
- Excessive complexity exceeding compiler limits
- Implementation patterns incompatible with partial evaluation

**Resolution**:
```bash
# Enable detailed compiler logging
./lox --experimental-options \
  --engine.TraceInlining \
  --vm.Djdk.graal.Log=:3 \
  --vm.Djdk.graal.MethodFilter=problematicFunction \
  program.lox
```
- Investigate immediately with detailed logging
- Use IGV to examine where partial evaluation fails
- Mark problematic loops with boundaries
- Restructure recursion to enable inlining limits

### Pattern 4: Good Inlining (Target Pattern!)

```
[engine] inline start optimizedFunction |IR Nodes 8500 |Truffle Callees 5 |...
[engine] Inlined helper1 |call diff -5.00 |IR Nodes 1200 |...  ✅ Great!
[engine] Inlined helper2 |call diff -3.00 |IR Nodes 2100 |...  ✅ Great!
[engine] Removed getter |call diff -1.00 |IR Nodes 0 |...     ✅ Perfect!
[engine] Inlined helper3 |call diff -2.50 |IR Nodes 1800 |...  ✅ Great!
[engine] inline done optimizedFunction |IR Nodes 8500 |...
```

**Characteristics of good inlining**:
- ✅ Most important calls show "Inlined" or "Removed"
- ✅ Negative call diff values (eliminating calls)
- ✅ Reasonable IR node counts (well under 12,000 budget)
- ✅ No Cutoff for hot paths
- ✅ Some Removed calls (optimized away completely)

## Best Practices for Fixes

The skill follows these analysis best practices:

1. **Start with Simple Programs**
   - Understand output format on small examples first
   - Then analyze complex production code

2. **Always Combine with TraceCompilation**
   - Provides context: when/what tier compiled
   - Helps interpret inlining decisions

3. **Focus on Hot Paths First**
   - Use profiling (`--cpusampler`) to identify hot functions
   - Don't try to optimize every function's inlining

4. **Use CompileOnly for Debugging**
   - Reduce output volume dramatically
   - Focus on specific problematic functions

5. **Redirect Output to Files**
   ```bash
   ./lox --experimental-options \
     --engine.TraceInlining \
     program.lox > inlining.txt 2>&1
   ```
   - Enable offline analysis with text processing tools
   - Compare across runs

6. **Monitor IR Node Counts**
   - Functions approaching 12,000 nodes risk budget exhaustion
   - Track relative to budget limits

7. **Check Call Diff Values**
   - Negative = eliminates calls (good)
   - Positive = adds calls (investigate if hot path)

8. **Increase Budgets Conservatively**
   - 25-50% increments only
   - Always measure actual performance impact
   - Validate with benchmarks

## Related Documentation

- [Common Patterns](./patterns.md) - Problem identification
- [Analysis Guide](./analysis.md) - Understanding the output
- [Main Workflow](../SKILL.md) - Overall optimization process
