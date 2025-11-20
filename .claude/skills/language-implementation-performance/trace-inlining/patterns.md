# Common Patterns


## 5. Insights & Analysis
### When should you use it?

Language developers should use TraceInlining when investigating why performance doesn't meet expectations despite successful compilation, when debugging compilation failures related to code size, when tuning inlining budgets for optimal performance, or when validating that critical hot paths are being optimized as expected. The tool answers fundamental questions: "Why isn't this function inlining its callees?" and "Is my compilation unit too large?" and "Which calls are consuming my inlining budget?"

### Actionable Insights
#### Insight: Identifying budget exhaustion bottlenecks

**Performance Issue**: Critical functions reaching Cutoff state  
**Symptom in Output**: Important hot path functions showing state "Cutoff" with 0 IR nodes, particularly when appearing early in the call tree traversal. The exploration budget exhausts before evaluating these candidates.

**Root Cause**: The exploration budget (default 12,000 Graal nodes) controls how much partial evaluation work the compiler performs to explore candidates. Complex compilation units with many potential callees can exhaust this budget, leaving later candidates unexplored. The compiler prioritizes candidates in heuristic order—typically by call frequency and inline annotations—so cutoff usually affects less important calls. However, if heuristics misjudge importance or the root function is exceptionally complex, critical calls may hit cutoff.

**Resolution**: Incrementally increase the exploration budget using `--engine.InliningExpansionBudget=18000` (50% increase) and observe whether cutoff moves to less critical functions. Avoid increasing arbitrarily—excessive budgets cause compilation time explosion and potential compiler failures. Profile using `--cpusampler` to confirm cutoff functions are actually hot before tuning budgets. Alternatively, refactor code structure: split large functions into smaller pieces, reduce call fan-out, or use `@TruffleBoundary` to mark cold paths that shouldn't consume budget.

**Verification Tools**: Use `--engine.TraceCompilation` to monitor compilation time increases from budget changes. Use `--engine.CompilationStatistics` to see aggregate metrics on inlining budget consumption. Verify performance improvements with actual benchmarks—budget tuning should be data-driven, not speculative.

#### Insight: Diagnosing Expanded calls blocking optimization

**Performance Issue**: Large functions preventing inlining  
**Symptom in Output**: Hot path functions marked "Expanded" with high IR node counts (approaching or exceeding 12,000), often with positive call diff values. These functions were evaluated but not inlined.

**Root Cause**: The function's compiled size exceeds available inlining budget, or inlining it would increase net call overhead because it contains many calls that wouldn't themselves inline. Large functions emerge from several patterns: directly large implementations (many operations), high specialization diversity (DSL generates many specialization combinations), or deep call chains that partially evaluate into complex control flow. Positive call diff occurs when the function body contains more calls than inlining would eliminate.

**Resolution**: The most effective solution is architectural—refactor large functions into smaller, more focused pieces. Identify cold paths (error handling, debugging, rare features) and isolate them behind `@TruffleBoundary` annotations to prevent their partial evaluation from consuming budget. Consider whether specialization is too granular—sometimes fewer, more general specializations produce smaller code. For functions that must remain large, evaluate whether forced inlining (`@TruffleBoundary(allowInlining = true)`) is appropriate, but use cautiously as this can cause compilation failures. In some cases, accepting the Expanded state is correct—not every function should inline if it prevents better optimization of smaller, hotter calls.
#### Insight: Recognizing BailedOut compilation failures

**Performance Issue**: Partial evaluation failures  
**Symptom in Output**: BailedOut state appearing for any function, which is rare and indicates serious problems. The function cannot be compiled at all.

**Root Cause**: Partial evaluation encountered conditions preventing compilation: unbounded loops without compilation-final loop counts, recursive structures without termination guarantees, excessive complexity exceeding compiler limits, or implementation patterns incompatible with partial evaluation assumptions. This represents a fundamental defect in the language implementation preventing optimization of affected code.

**Resolution**: BailedOut requires immediate investigation as it indicates broken optimization. Enable detailed compiler logging with `--vm.Djdk.graal.Log=:3` and `--vm.Djdk.graal.MethodFilter=BailedOutMethod` to capture bailout reasons. Use IGV to examine where partial evaluation fails. Common fixes include marking problematic loops with `@TruffleBoundary`, restructuring recursion to enable inlining limits, simplifying control flow, or using `@ExplodeLoop` with appropriate `@CompilationFinal` annotations for bounded loops. Contact the Truffle development team if bailout reasons seem unreasonable—occasional compiler bugs do occur.

**Verification Tools**: Use `--engine.CompilationStatistics` with `--engine.CompilationStatisticsDetails` to see bailout counts and categories. Use `--engine.TracePerformanceWarnings=all` to check for related warnings that might explain the bailout.
#### Insight: Optimizing recursive function inlining

**Performance Issue**: Recursive calls not unrolling  
**Symptom in Output**: Recursive functions showing Recursion Depth > 0 but many recursive calls remaining as Invoke nodes rather than inlining.

**Root Cause**: The recursion depth limit (default 2) prevents deeper inlining of recursive calls. This protects against code explosion but limits optimization of recursive algorithms. Some recursive patterns benefit from deeper unrolling—tail recursion, simple recursive arithmetic, or tree traversals with predictable depth.

**Resolution**: Increase `--engine.InliningRecursionDepth=4` cautiously and measure impact. Higher values can dramatically improve performance for recursive code but risk compilation unit explosion. Consider refactoring recursive algorithms into iterative equivalents if possible, as loops optimize more reliably than recursion in Truffle. For must-be-recursive code, ensure the recursion terminates predictably and uses compilation-final constants for depth limits where possible.

**Verification Tools**: Use `--engine.TraceCompilation` to monitor compilation unit sizes as recursion depth increases. Use IGV to visualize unrolled recursive calls and verify the resulting IR is reasonable.

### Correlation with complementary tools

TraceInlining achieves maximum value when combined with other analysis tools. **With --engine.TraceCompilation**, you see both macro and micro perspectives: TraceCompilation shows overall compilation success, tier, timing, and total IR size, while TraceInlining shows the detailed inlining decisions that produced that result. The "Inlined X Y" counts in TraceCompilation output directly correspond to the sum of Inlined states in TraceInlining. When investigating compilation time issues, TraceCompilation identifies slow compilations while TraceInlining reveals whether excessive inlining exploration caused the slowdown.

**With --engine.TracePerformanceWarnings=call**, you identify gaps between what should inline and what actually did. Performance warnings flag virtual calls remaining in compiled code—calls that should have specialized and inlined but didn't. Cross-referencing warnings with TraceInlining reveals whether budget limits, size constraints, or indirect calls prevented optimization. This combination is essential for achieving peak performance.

**With IGV (Ideal Graph Visualizer)** using `--vm.Djdk.graal.Dump=Truffle:1 --vm.Djdk.graal.PrintGraph=Network`, you gain visual representation of the call tree and IR graphs. TraceInlining provides the text-based decision log, while IGV shows the resulting compiled code structure. Visual graphs reveal patterns difficult to spot in text traces: excessive control flow merging, unexplained IR bloat, or missed optimization opportunities. The tools complement each other—text for quick analysis, visual for deep investigation.

**With --engine.TraceSplitting**, you understand the interaction between polymorphism and inlining. Call target splitting (monomorphization) duplicates compilation profiles for polymorphic call sites, enabling better type specialization. TraceInlining shows inlining decisions for split targets (marked with `<split-ID>` in function names), while TraceSplitting shows why splitting occurred. Together they reveal whether splitting successfully enabled better inlining or just created duplicate compilation units without benefit.

---
