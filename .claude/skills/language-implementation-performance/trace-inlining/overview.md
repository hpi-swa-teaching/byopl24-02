# Overview

# Tool Name: --engine.TraceInlining


## 3. Functional Description
### Primary Purpose

In Truffle, inlining occurs during **partial evaluation**—the first phase of JIT compilation where the Truffle Abstract Syntax Tree (AST) transforms into Graal intermediate representation (IR). Understanding this tool requires grasping what makes Truffle's inlining unique compared to traditional JIT compilers. Traditional compilers inline method calls after generating IR, making decisions based on method bytecode size and heuristics. Truffle performs inlining during partial evaluation itself, making decisions based on the actual IR that results from specializing the interpreter for specific guest code patterns.

The **primary purpose** of `--engine.TraceInlining` is tracing these language-agnostic inlining decisions. Every time Truffle compiles a guest language function, it builds an internal call tree representing all potential callees reachable from the root. For each call in this tree, the tool must decide: inline this call into the parent compilation unit, or leave it as a call boundary? The trace output documents every such decision with detailed rationale: how large is the callee's IR, how does inlining affect call counts, was the decision forced by annotations, did budget exhaustion prevent exploration?
### What the Tool Does

The **technical mechanism** proceeds through several phases. First, during **call tree construction**, the engine analyzes the root function's AST, identifying all direct calls to other Truffle functions. Each call becomes a node in the call tree. Second, **partial evaluation before decisions** distinguishes this approach: the compiler partially evaluates each candidate call target to determine its actual compiled size in Graal nodes, not AST nodes. This is crucial because AST size poorly predicts compiled code size—specialization and constant folding can collapse large ASTs into tiny IR, while some small ASTs expand into complex control flow. Third, **budget-based decision making** uses two budgets to control the process. The exploration budget limits how many candidates to partially evaluate, while the inlining budget limits total compilation unit size .

### How it Works

Each call in the tree transitions through one of six **call states** that appear in the output. **Inlined** means the call was successfully incorporated into the parent compilation unit—the callee's IR is directly embedded, eliminating call overhead and enabling cross-function optimization. **Cutoff** indicates the exploration budget exhausted before evaluating this candidate; it wasn't analyzed at all, so its size is unknown (shown as 0 IR nodes). **Expanded** means the call was partially evaluated but deemed too expensive to inline—either its IR size is excessive, or the inlining budget would be exceeded. **Removed** is the best outcome: partial evaluation proved the call can be completely eliminated through optimization (e.g., a getter that just returns a constant field). **Indirect** indicates an indirect call (through a variable or callback) that cannot be inlined without speculative optimization. **BailedOut** is rare and concerning—partial evaluation failed for this target, usually indicating a fundamental issue in the language implementation that prevents compilation.
### When to Use

Language developers need inlining visibility because **inlining decisions directly determine peak performance**. Call overhead is expensive in modern CPUs—typically 5-20 nanoseconds per call due to branch misprediction and pipeline stalls. For tight inner loops calling functions thousands of times per millisecond, this overhead dominates. Beyond eliminating call overhead, inlining enables crucial cross-function optimizations: constant propagation across call boundaries, dead code elimination of unused parameters, and specialization of callees based on caller context. When critical functions fail to inline due to size limits or budget exhaustion, performance suffers significantly—often 2-5x slower for computation-intensive code.

The tool serves multiple **use cases during development phases**. During **performance debugging**, when profiling reveals unexpectedly poor performance despite compilation, TraceInlining shows whether hot path functions are actually inlining their callees or leaving expensive call boundaries. During **language implementation tuning**, developers adjust code structure to achieve better inlining—breaking large functions into smaller pieces, moving cold paths behind boundaries, or restructuring recursion patterns. During **benchmarking**, the tool validates that performance-critical code paths inline as expected, ensuring benchmark results reflect real optimization rather than artificial patterns. For **regression testing**, comparing inlining traces across versions detects unintended changes in inlining behavior that might degrade performance. For **teaching and learning**, the detailed traces help developers understand Truffle's compilation model and how their language implementation decisions affect optimization.

---

## 6. Additional Notes
### Overhead

The performance overhead of TraceInlining is minimal, affecting only compilation time, not runtime performance. Enabling the trace adds approximately 1% to compilation time for the string formatting and output generation. Since compilation occurs asynchronously in background threads by default, this overhead is typically invisible to application execution. The trace has **zero runtime performance impact**—inlining decisions are identical whether tracing is enabled or disabled. Memory overhead is negligible, limited to buffering output strings.

Production use is generally not recommended despite low overhead. The tool generates substantial output volume—potentially thousands of lines for large applications or long-running services compiling many functions. Log management systems can struggle with this volume, and the output provides no operational value in production. The appropriate usage model is development and testing environments where performance diagnosis occurs, then disabling for production deployment. If production issues require inlining analysis, enable the tool temporarily in controlled debugging scenarios with output redirected to files for offline analysis.
### Limitations

The tool has several limitations worth noting. Output volume can be overwhelming for large applications—a single compilation might generate hundreds of trace lines, and applications might perform thousands of compilations. No built-in filtering mechanism exists to focus on specific functions; developers must use external tools like grep or redirect output and analyze offline. Output appears only when compilations complete, not progressively, so long-running compilations appear silent until finishing. No aggregation across multiple compilations of the same function occurs—each compilation produces independent traces, even though multi-tier compilation means functions compile multiple times. 

### Best Practice 

Best practices for effective use of TraceInlining include starting with simple, small test programs to understand the output format before analyzing complex production code. Always combine with `--engine.TraceCompilation` to provide context—knowing when and at what tier a function compiled helps interpret its inlining decisions. Focus analysis on hot path functions identified through profiling (`--cpusampler`) rather than trying to optimize every function's inlining. Use `--engine.CompileOnly=functionName` to restrict compilation to specific functions during debugging, dramatically reducing output volume. Redirect output to files (`js --engine.TraceInlining=true script.js > trace.txt 2>&1`) for offline analysis with text processing tools. Monitor IR node counts relative to budgets—functions approaching 12,000 nodes risk exhausting inlining budget. Check call diff values to understand whether inlining eliminates or adds call overhead.
### Common mistakes

Common mistakes include misinterpreting Cutoff as failure when it's often normal and acceptable for unimportant functions. Not every call should inline—the compiler makes deliberate trade-offs to focus budget on the most important calls. Another pitfall is increasing budgets indiscriminately without measuring actual performance impact. Larger budgets cause longer compilation times, higher memory usage, and potential compilation failures from excessive complexity. Always increase conservatively (25-50% increments) and validate with benchmarks. Developers sometimes misinterpret positive call diff values as always indicating problems, but they can represent acceptable trade-offs if the exposed calls subsequently inline or enable other optimizations. Not correlating inlining traces with actual performance measurements is a critical mistake—inlining decisions should optimize for real performance, not theoretical metrics. Finally, forgetting multi-tier compilation means analyzing only first-tier traces without examining second-tier compilations that apply full optimization.
### Related Tools

Related tools provide complementary inlining analysis capabilities. The `--engine.TraceInliningDetails` flag expands the trace to show the entire explored call tree, including all candidates considered for inlining, not just those actually inlined or expanded. This reveals the complete decision space but generates much more output. The `--engine.TraceMethodExpansion=truffleTier` and `--engine.TraceNodeExpansion=truffleTier` options trace host Java method and Truffle node expansion during partial evaluation, showing where IR nodes come from and revealing code bloat sources. These are more detailed than TraceInlining, useful for understanding why specific functions generate so many IR nodes. IGV (Ideal Graph Visualizer) provides visual call graphs and IR graphs, offering intuitive understanding of inlining decisions and their results. The `--engine.CompilationStatistics` option gives aggregate inlining metrics across all compilations, quantifying overall inlining success rates and budget consumption patterns.

---

## 7. Resources

All information derives from official Oracle/GraalVM sources, verified as authoritative and current:

1. **Truffle Approach to Function Inlining** - https://www.graalvm.org/latest/graalvm-as-a-platform/language-implementation-framework/Inlining/ - Primary source documenting language-agnostic inlining mechanism, call tree states, field explanations, and example output interpretation.

2. **Optimizing Truffle Interpreters** - https://www.graalvm.org/latest/graalvm-as-a-platform/language-implementation-framework/Optimizing/ - Comprehensive optimization guide covering TraceInlining in context with other tools, best practices, and debugging workflows.

3. **Truffle Options - GraalVM JDK 24** - https://www.graalvm.org/jdk24/graalvm-as-a-platform/language-implementation-framework/Options/ - Complete option reference with parameter types, defaults, and descriptions for all engine options.

4. **Truffle Options - GraalVM Latest** - https://www.graalvm.org/latest/graalvm-as-a-platform/language-implementation-framework/Options/ - Current version documentation for the most recent GraalVM release.

5. **Truffle Options - GraalVM 22.1** - https://www.graalvm.org/22.1/graalvm-as-a-platform/language-implementation-framework/Options/ - Historical reference showing tool evolution and compatibility across versions.

6. **Oracle Enterprise Documentation - Inlining** - https://docs.oracle.com/en/graalvm/enterprise/20/docs/graalvm-as-a-platform/language-implementation-framework/Inlining/ - Enterprise version documentation with additional technical details.

7. **Oracle JDK 17 Documentation - Options** - https://docs.oracle.com/en/graalvm/jdk/17/docs/graalvm-as-a-platform/language-implementation-framework/Options/ - LTS version option documentation for long-term support deployments.

8. **Oracle Graal GitHub Repository - Truffle Options** - https://github.com/oracle/graal/blob/master/truffle/docs/Options.md - Development documentation showing latest option changes and updates.

9. **Oracle Graal GitHub Repository - Host Compilation** - https://github.com/oracle/graal/blob/master/truffle/docs/HostCompilation.md - Context on host versus guest inlining distinctions.

10. **Oracle Graal GitHub Repository - DSL Node Object Inlining** - https://github.com/oracle/graal/blob/master/truffle/docs/DSLNodeObjectInlining.md - Complementary documentation on node-level inlining optimization (distinct from call inlining).

11. **Polyglot Programming Reference** - https://www.graalvm.org/latest/reference-manual/polyglot-programming/ - Guide to setting engine options via Polyglot API for embedded use cases.

12. **Host Optimization for Interpreter Code** - https://docs.oracle.com/en/graalvm/jdk/21/docs/graalvm-as-a-platform/language-implementation-framework/HostOptimization/ - Context on host versus guest compilation boundaries and their impact on inlining.

13. **Engine.Builder Javadoc** - https://www.graalvm.org/truffle/javadoc/org/graalvm/polyglot/Engine.Builder.html - API documentation for programmatic option configuration.
