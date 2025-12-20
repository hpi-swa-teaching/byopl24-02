# --engine.SpecializationStatistics

## 1. Command Execution

### Basic Command
```bash
js --experimental-options --engine.SpecializationStatistics test.js
```

### Full Command Syntax
```bash
<language-launcher> --experimental-options --engine.SpecializationStatistics [script-file]

# With custom build (mx build system)
mx build -c -A-Atruffle.dsl.GenerateSpecializationStatistics=true
<language-launcher> --experimental-options --engine.SpecializationStatistics [script-file]
```

### Execution Context

* **JVM Mode**: Yes - Requires `--experimental-options` flag
* **Native Image Mode**: Yes - If compiled with the statistics generation flag
* **Prerequisites**: 
  - Truffle DSL nodes must be compiled with `-Atruffle.dsl.GenerateSpecializationStatistics=true` annotation processor flag
  - Language implementation must use `@Specialization` annotations
  - Must rebuild interpreter/language with the special compilation flag enabled
  - IDE auto-recompilation should be disabled during testing

## 2. Command Options & Parameters

| Option                              | Type    | Default | Description                                                                                          |
| ----------------------------------- | ------- | ------- | ---------------------------------------------------------------------------------------------------- |
| `--engine.SpecializationStatistics` | Boolean | false   | Enables specialization statistics for nodes generated with Truffle DSL and prints the result on exit |
| `--experimental-options`            | Boolean | false   | Required prefix flag to enable experimental engine options                                           |

### Key Options

**--engine.SpecializationStatistics**: This is the primary flag that activates the specialization statistics collection and reporting mechanism. When enabled, the tool generates a comprehensive histogram showing node class usage, specialization activation patterns, dynamic type combinations, and polymorphism information. The output is printed to the console when the program terminates.

**Compilation Flag: -Atruffle.dsl.GenerateSpecializationStatistics=true**: This Java compiler annotation processor argument is mandatory for the tool to function. It instructs the Truffle DSL annotation processor to generate additional instrumentation code in each node class that tracks specialization activations, type combinations, and execution counts. Without this build-time flag, running with `--engine.SpecializationStatistics` produces no data and displays the warning: "No specialization statistics data was collected."

### Advanced Options

**@SpecializationStatistics.AlwaysEnabled Annotation**: Language developers can annotate specific node classes with `@SpecializationStatistics.AlwaysEnabled` to enable statistics collection for those nodes without requiring the global compilation flag. This provides fine-grained control over which nodes generate statistics, reducing overhead when only specific nodes need analysis.

**Programmatic API**: The `com.oracle.truffle.api.dsl.SpecializationStatistics` Java API allows developers to programmatically create statistics instances, enter/leave collection scopes on specific threads, check if data was collected, and print histograms to custom streams. This enables integration with custom profiling frameworks and test harnesses.

## 3. Functional Description

The `--engine.SpecializationStatistics` tool serves as a diagnostic instrument for analyzing Truffle DSL specialization usage patterns during interpreter execution. Its primary purpose is to provide language implementers with detailed insights into how their DSL-generated nodes behave at runtime, revealing specialization activation frequencies, type combinations, and polymorphism patterns that directly impact performance.

The tool operates through compile-time instrumentation rather than runtime sampling or dynamic tracing. When nodes are compiled with the special annotation processor flag, the Truffle DSL code generator embeds tracking logic directly into each generated specialization method. This instrumentation creates `NodeStatistics` objects that record every specialization activation, capturing the specific type combinations that trigger each specialization path. Each time a node executes, the embedded instrumentation calls methods like `acceptExecute(int specializationIndex, Class<?>... types)` to register the event.

The mechanism works by augmenting the standard Truffle DSL node generation process. Normally, the DSL annotation processor generates optimized node classes from `@Specialization` annotations, creating fast-path guards and specialization selection logic. With statistics generation enabled, additional code is woven into these nodes to maintain execution counters, track active specialization combinations per node instance, and record the dynamic types that flow through each specialization. This data accumulates throughout program execution and is aggregated into a comprehensive histogram when the program terminates or when explicitly requested via the API.

Language developers should use this tool during the development and optimization phases when they need to understand specialization effectiveness, identify unexpected polymorphism, or optimize specialization ordering. It is particularly valuable when diagnosing performance issues related to megamorphic behavior, detecting over-specialized code with many unused specializations, or validating that type guards are triggering as expected. The tool excels at revealing discrepancies between intended specialization design and actual runtime behavior, making it indispensable for fine-tuning DSL-based language implementations.

## 4. Output Format & Structure

### Output Type
- ☑ Console/Terminal output (printed on program exit)
- ☐ File output (can be redirected)
- ☐ Graphical output
- ☐ Binary output

### Output Location
**Default Location**: Standard output (stdout) when program terminates

**Custom Location**: Can be redirected using shell redirection (`> output.txt`) or programmatically via the Java API using `printHistogram(PrintWriter)` or `printHistogram(PrintStream)`

### Output Format Example

```
-----------------------------------------------------------------------------------------------------------------------------------------------------------------------
| Name                                Instances        Executions       Executions per instance
-----------------------------------------------------------------------------------------------------------------------------------------------------------------------
| JSWriteCurrentFrameSlotNodeGen      8 (17%)          18 (12%)         Min= 1 Avg= 2.25 Max= 5 MaxNode= test.js~5-7:76-128
|   doBoolean <boolean>               1 (13%)          1 (6%)           Min= 1 Avg= 1.00 Max= 1 MaxNode= test.js~4:52-71
|   doInt <int>                       1 (13%)          1 (6%)           Min= 1 Avg= 1.00 Max= 1 MaxNode= test.js~5-7:76-128
|   doSafeIntegerInt                  0 (0%)           0 (0%)           Min= 0 Avg= 0.00 Max= 0 MaxNode= -
|   doSafeInteger                     0 (0%)           0 (0%)           Min= 0 Avg= 0.00 Max= 0 MaxNode= -
|   doLong                            0 (0%)           0 (0%)           Min= 0 Avg= 0.00 Max= 0 MaxNode= -
|   doDouble                          0 (0%)           0 (0%)           Min= 0 Avg= 0.00 Max= 0 MaxNode= -
|   doObject                          7 (88%)          16 (89%)         Min= 1 Avg= 2.29 Max= 5 MaxNode= test.js~5-7:76-128
|     <DynamicObjectBasic>            6 (86%)          12 (75%)         Min= 1 Avg= 2.00 Max= 5 MaxNode= test.js~5-7:76-128
|     <IteratorRecord>                1 (14%)          1 (6%)           Min= 1 Avg= 1.00 Max= 1 MaxNode= test.js~1-8:16-130
|     <String>                        2 (29%)          2 (13%)          Min= 1 Avg= 1.00 Max= 1 MaxNode= test.js~5-7:76-128
|     <Integer>                       1 (14%)          1 (6%)           Min= 1 Avg= 1.00 Max= 1 MaxNode= test.js~6:105-123
| --------------------------------------------------------------------------------------------------------------------------------------------------------------------
| [doBoolean]                         1 (13%)          1 (6%)           Min= 1 Avg= 1.00 Max= 1 MaxNode= test.js~4:52-71
| [doInt, doObject]                   1 (13%)          4 (22%)          Min= 4 Avg= 4.00 Max= 4 MaxNode= test.js~5-7:76-128
|   doInt                             1 (100%)         1 (25%)          Min= 1 Avg= 1.00 Max= 1 MaxNode= test.js~5-7:76-128
|   doObject                          1 (100%)         3 (75%)          Min= 3 Avg= 3.00 Max= 3 MaxNode= test.js~5-7:76-128
| [doObject]                          6 (75%)          13 (72%)         Min= 1 Avg= 2.17 Max= 5 MaxNode= test.js~5-7:76-128
-----------------------------------------------------------------------------------------------------------------------------------------------------------------------
```

**Annotations**:
- The output contains **two table sections**: The first table groups by individual specializations and their dynamic type combinations; the second table groups by combinations of specializations active on the same node instances (showing polymorphic patterns).
- **Hierarchy**: Node class name → Specialization methods → Dynamic type combinations (3-level nesting)
- **Ordering**: Histograms ordered by sum of executions per node class, with most frequently used node classes printed last
- **Source locations**: Format is `file~lines:character-range` (e.g., `test.js~5-7:76-128`)

### Output Fields/Metrics

| Field                       | Description                                                  | Unit                    | Interpretation                                                                                                     |
| --------------------------- | ------------------------------------------------------------ | ----------------------- | ------------------------------------------------------------------------------------------------------------------ |
| Name                        | Node class name, specialization method name, or dynamic type | String                  | Identifies the code element being measured                                                                         |
| Instances                   | Number of node instances created                             | Count (with percentage) | Higher values indicate frequently instantiated nodes; percentage shows proportion relative to parent or total      |
| Executions                  | Total number of times the node/specialization was executed   | Count (with percentage) | Shows hot execution paths; percentage indicates relative frequency                                                 |
| Min                         | Minimum executions per instance                              | Count                   | Reveals cold/unused instances                                                                                      |
| Avg                         | Average executions per instance                              | Count (decimal)         | Indicates typical usage pattern                                                                                    |
| Max                         | Maximum executions per instance                              | Count                   | Identifies hottest single instance                                                                                 |
| MaxNode                     | Source location of most-executed instance                    | File location           | Direct pointer to hot code for optimization                                                                        |
| Type combinations           | Dynamic runtime types in angle brackets                      | Class name              | Shows actual types flowing through polymorphic specializations (e.g., `<DynamicObjectBasic>`, `<String>`, `<int>`) |
| Specialization combinations | Comma-separated list in brackets                             | List                    | Shows which specializations coexist on polymorphic node instances (e.g., `[doInt, doObject]`)                      |

## 5. Insights & Analysis

Language developers turn to `--engine.SpecializationStatistics` when they encounter puzzling performance characteristics in their Truffle-based interpreters or when they need to validate that their specialization design aligns with actual runtime behavior. The tool answers critical questions about specialization effectiveness: Are my carefully crafted specializations actually being used? Is my code exhibiting unexpected polymorphism? Are certain specializations consuming memory without providing value? These questions become particularly pressing when benchmarks show performance gaps compared to expectations or when profiling reveals interpreter overhead that seems disproportionate to the workload.

### Insight 1: Detecting unused or rarely-used specializations

When examining the output histogram, developers may notice specializations with zero or very low instance counts despite being written to handle supposedly common cases. For example, seeing `doSafeIntegerInt: 0 (0%), doSafeInteger: 0 (0%), doLong: 0 (0%), doDouble: 0 (0%)` while `doObject` shows 88% of instances indicates that the specialized fast-paths for numeric types are completely unused. This symptom reveals that the actual runtime type distribution differs dramatically from the developer's assumptions during node design.

The root cause typically stems from overeager specialization or incorrect type guard design. Perhaps the language implementation's object model always boxes primitive values into objects before they reach certain nodes, making the primitive specializations unreachable. Alternatively, the specialization ordering might be wrong, with a too-permissive guard earlier in the chain capturing all cases before narrower specializations can activate. Another possibility is that the test workload simply doesn't exercise those code paths, making this a testing gap rather than a design flaw.

To resolve this issue, developers should first verify whether the unused specializations represent genuine dead code or merely inadequate test coverage by running diverse workloads. If consistently unused across realistic programs, consider removing these specializations entirely to reduce code size and node complexity. This consolidation decreases memory footprint per node instance and simplifies the specialization chain. If the specializations should be reachable, examine type guard logic and insertion points—perhaps primitives need to be kept unboxed further upstream, or the specialization needs to be moved to a different node in the AST. When uncertain, use `--engine.TraceCompilationPolymorphism` alongside SpecializationStatistics to see if the Graal compiler encounters these specializations during compilation, providing additional evidence about their actual utility.

### Insight 2: Identifying polymorphic hotspots requiring optimization

The specialization combination section of the output (entries like `[doInt, doObject]`) reveals nodes with multiple active specializations, indicating polymorphic behavior. When a single node instance shows combinations like `[doInt, doObject]` with high execution counts, it signals that this specific node alternates between handling integers and objects. Seeing many node instances with such combinations, especially in performance-critical functions, indicates systematic polymorphism that degrades performance by preventing effective optimization.

Polymorphism at the interpreter level typically arises from programming patterns where variables change types across iterations, from library functions accepting heterogeneous inputs, or from framework code that must handle multiple type categories. In JavaScript, for instance, array operations might encounter both integer indices and string property names, forcing index nodes into polymorphic states. The performance impact is substantial: polymorphic nodes execute slower guard checks, prevent effective inlining during compilation, and can trigger deoptimization when new type combinations appear.

The resolution strategy depends on whether the polymorphism is avoidable or fundamental to the algorithm. For avoidable cases, consider creating separate code paths at a higher level to segregate types before they reach the polymorphic node—for example, using separate loops for integer-indexed and string-keyed operations. For unavoidable polymorphism, the tool helps identify which specific type combinations are common. If `[doInt, doObject]` appears frequently with the object type being specifically `<String>` in most cases, consider adding a specialized `doIntOrString` method that handles both cases efficiently without full polymorphic overhead. This middle-ground specialization acknowledges the polymorphism while optimizing for the common pattern. Additionally, review specialization ordering: ensure the most frequent combinations appear first in the DSL declaration, as guard checking proceeds sequentially and early matches execute faster.

### Insight 3: Discovering unexpected type combinations requiring specialized fast paths

Within individual specializations, particularly generic ones like `doObject`, the tool reveals the actual dynamic type distribution through indented type entries such as `<DynamicObjectBasic> 6 (86%), <String> 2 (29%), <Integer> 1 (14%)`. When a single type dominates overwhelmingly—for instance, if `<DynamicObjectBasic>` appears in 86% of `doObject` executions—this indicates an opportunity for targeted specialization that the current design misses.

The underlying cause is that the generic `doObject` specialization, while correct, treats all object types uniformly despite the runtime reality that one type dominates. This generic handling might involve expensive type checks, virtual calls, or conservative assumptions about object properties. The heavily-used specific type likely has characteristics that could enable faster specialized handling: perhaps `DynamicObjectBasic` instances have predictable layouts allowing direct field access, or they support certain operations that the generic object path must check for dynamically.

To capitalize on this insight, introduce a new specialization specifically for the dominant type. For the example above, add a `doDynamicObjectBasic(DynamicObjectBasic obj)` specialization with a type guard checking for that specific class. Place this new specialization before the generic `doObject` in the DSL declaration so it intercepts the common case. Within this specialized method, leverage whatever optimizations are valid for this specific type—direct field access, eliminated type checks, inlined operations, or cached values specific to the type's invariants. This pattern of progressive specialization refinement, guided by empirical type distribution data from SpecializationStatistics, represents one of the most effective optimization strategies in Truffle DSL development. After implementing the new specialization, rerun with statistics enabled to verify that instances migrate to the new fast path and that the generic fallback now handles a smaller, more diverse set of less-common types.

### Insight 4: Optimizing specialization ordering based on execution frequency

The tool's output directly reveals which specializations execute most frequently through the execution count and percentage columns. When examining a node class, comparing these frequencies against the actual order of `@Specialization` annotations in the source code often reveals misalignment. For instance, if the output shows `doObject: 16 (89%)` while `doInt: 1 (6%)` and `doBoolean: 1 (6%)`, but the source code declares specializations in the order `@Specialization doBoolean`, `@Specialization doInt`, `@Specialization doObject`, there's a clear optimization opportunity.

Truffle DSL generates guard-checking code that evaluates specializations in declaration order, attempting to match each specialization's type guards sequentially until one succeeds. When rarely-used specializations appear early in this sequence, every execution must check their guards first before falling through to the common case. These unnecessary guard evaluations accumulate overhead, particularly in interpreter mode where these checks aren't optimized away. While the compiler eventually optimizes hot paths, interpreter performance matters significantly during warmup and for code that never reaches the compilation threshold.

The solution is straightforward but impactful: reorder the `@Specialization` declarations in the source code to match the runtime frequency distribution revealed by the tool. Place the most frequently executed specialization first—in this example, moving `doObject` to the first position. This ensures that the common case succeeds on the first guard check, eliminating unnecessary guard evaluations. For specializations with similar frequencies, consider execution environment factors: if certain types are more common in startup code versus steady-state, prioritize startup patterns for better warmup performance. After reordering, rebuild with statistics enabled and verify that the change doesn't introduce unexpected polymorphism (which could happen if guard specificity changes affect which specialization activates first). The performance improvement from this simple reordering can be substantial in interpreter mode, potentially improving warmup time by 5-15% depending on guard complexity and node frequency.

### Insight 5: Investigating unexpected specialization instantiations

Occasionally, the statistics reveal specializations being instantiated in source locations that seem incorrect or unexpected based on the developer's understanding of their code. The `MaxNode` field points directly to these locations with precise source coordinates. For example, seeing that an `Uncached` variant of a node appears frequently in the profile—identifiable by the name pattern—indicates that cached specialization limits are being exceeded or that the caching strategy is ineffective.

The presence of uncached nodes typically stems from specialization explosion: when each unique type combination or cached parameter value creates a new specialization instance, the cache can fill up rapidly. Truffle DSL limits cache sizes for memory efficiency (typically around 3 specializations per node instance), and once exceeded, the node transitions to an uncached fallback implementation. Uncached nodes are dramatically slower because they must perform full type checking and dispatch on every execution without the benefit of optimized monomorphic paths. Seeing unexpected specializations in particular source locations might also indicate that data flows differently through the AST than anticipated—perhaps a node shared across multiple contexts receives more diverse types than expected due to AST sharing or unexpected code paths.

To address uncached node appearances, first examine the `@Cached` parameters in the affected specialization to understand what's being cached and whether the cache limit needs adjustment. Consider using the `@Shared` annotation to share cached values across multiple specializations, reducing per-specialization cache pressure. The `@Exclusive` annotation can prevent sharing when different specializations genuinely need different cached values. If the issue is fundamental cache overflow from too many type combinations, consider whether the node design is too fine-grained—perhaps moving caching to a different level or using a different specialization strategy would be more appropriate. For unexpected specializations in particular locations, use the source coordinates to navigate to that exact AST construction point and trace back how nodes are created and shared. This might reveal that nodes are being reused inappropriately or that the AST construction logic needs to create separate node instances for different contexts to avoid problematic specialization mixing.

### Correlation with Other Tools

The insights from `--engine.SpecializationStatistics` become more powerful when combined with complementary Truffle profiling tools. `--engine.TraceCompilationPolymorphism` shows which nodes remain polymorphic after compilation, helping distinguish between interpreter-level polymorphism (which SpecializationStatistics reveals) and compiler-level polymorphism (which affects peak performance). If SpecializationStatistics shows polymorphism but TraceCompilationPolymorphism doesn't, the compiler successfully optimized it away, suggesting the interpreter-level polymorphism might be acceptable. Conversely, if both tools show the same nodes as polymorphic, optimization is clearly needed.

`--engine.NodeExpansionStatistics` provides the compilation-time complement to SpecializationStatistics' interpreter-time view. While SpecializationStatistics shows all specializations attempted during interpretation, NodeExpansionStatistics reveals which specializations appear in compiled code with their IR node counts, showing code size impact post-optimization. Comparing these outputs identifies specializations that exist in the interpreter but never survive to compiled code, potentially indicating optimization opportunities or specializations that could be simplified.

Finally, `--cpusampler` and `--cputracer` help validate that fixing issues revealed by SpecializationStatistics actually improves performance. After reordering specializations or adding targeted fast paths based on SpecializationStatistics insights, use CPU profiling to measure the actual performance impact. The specialization statistics provide the diagnostic insight explaining why something is slow; CPU profiling confirms whether the optimization resolved the actual performance bottleneck or whether the slowness stemmed from a different cause entirely.

## 6. Additional Notes

The overhead characteristics of `--engine.SpecializationStatistics` make it strictly a development-time tool rather than a production diagnostic option. Official documentation explicitly warns that enabling this flag and the required compiler option has "major implications on the performance and footprint of the interpreter" and should never be used in production environments. While exact overhead figures aren't published in official sources, the instrumentation-based approach inherently adds tracking code to every specialization execution, creating execution counters and type recording overhead on every node invocation. This contrasts with sampling-based profilers like `--cpusampler` which have much lower overhead by only periodically checking execution state rather than instrumenting every operation.

The tool's fundamental limitation is its requirement for special compilation, which represents both a strength and a weakness. The compile-time instrumentation approach ensures perfect accuracy with deterministic, repeatable results showing exact execution counts rather than statistical approximations. However, this means developers cannot simply enable the flag on an existing build—they must rebuild their entire language implementation with the special annotation processor flag. This rebuild requirement complicates workflows, particularly in IDEs where automatic background compilation might override the special flag, causing statistics collection to mysteriously stop working. Additionally, the generated instrumentation code increases the code size of every DSL node class, impacting memory footprint even before considering the runtime data structures that accumulate statistics during execution.

Best practices for using this tool center on maintaining separate build configurations and using focused test cases to generate actionable insights. Rather than running with statistics enabled on large, complex applications that generate overwhelming output, developers should create targeted microbenchmarks or focused test scenarios that exercise specific language features or operations under investigation. When using build systems like `mx`, maintain separate build targets for statistics-enabled builds: `mx build --targets=DEBUG -c -A-Atruffle.dsl.GenerateSpecializationStatistics=true`. This separation ensures that normal development builds remain fast while statistics builds are explicitly opt-in. During statistics collection, disable IDE auto-compilation to prevent the IDE from rebuilding sources without the special flag, which would cause statistics collection to silently fail.

The iterative workflow for specialization optimization should treat SpecializationStatistics as one tool in a larger optimization cycle. Run statistics on a representative workload to identify one specific issue—unexpected polymorphism, unused specializations, or suboptimal ordering. Focus on fixing that single issue, then rebuild with statistics enabled and measure again to verify the fix. This focused iteration prevents the overwhelming confusion that comes from trying to optimize everything simultaneously and makes it easier to attribute performance changes to specific code modifications. Cross-referencing statistics output with other tools like `--engine.TraceCompilationPolymorphism` and `--cpusampler` provides validation: the statistics explain what the interpreter is doing, while other tools confirm whether those patterns affect actual performance or whether the compiler optimizes away the concerns.

Common pitfalls beyond forgetting the rebuild step include misinterpreting the nested percentage displays, where percentages at different indentation levels are relative to their parent rather than absolute. Developers sometimes mistake interpreter-level statistics for compiled-code behavior, assuming that high execution counts in statistics directly translate to performance bottlenecks when in fact the Graal compiler might completely optimize away those operations. The tool shows pre-optimization behavior, which is valuable for understanding the interpreter but doesn't necessarily predict peak performance. Another pitfall is over-indexing on the output from synthetic microbenchmarks that don't represent realistic workloads—specialization patterns in real applications often differ significantly from carefully constructed test cases. Finally, developers sometimes attempt to eliminate all polymorphism revealed by the tool, but some polymorphism is inherent to the problem domain and attempts to eliminate it through increasingly complex specialization designs can make code less maintainable without actually improving performance.

## 7. Resources

### Official Documentation

- GraalVM Specialization Histogram Guide: https://www.graalvm.org/latest/graalvm-as-a-platform/language-implementation-framework/SpecializationHistogram/
- GraalVM Options Reference (22.1): https://www.graalvm.org/22.1/graalvm-as-a-platform/language-implementation-framework/Options/
- GraalVM Options Reference (JDK 24): https://www.graalvm.org/jdk24/graalvm-as-a-platform/language-implementation-framework/Options/
- GraalVM Optimizing Guide: https://www.graalvm.org/latest/graalvm-as-a-platform/language-implementation-framework/Optimizing/
- Oracle GraalVM JDK 17 Options: https://docs.oracle.com/en/graalvm/jdk/17/docs/graalvm-as-a-platform/language-implementation-framework/Options/
- Oracle GraalVM Enterprise 22 Specialization Histogram: https://docs.oracle.com/en/graalvm/enterprise/22/docs/graalvm-as-a-platform/language-implementation-framework/SpecializationHistogram/
- GraalVM Specialization Testing: https://www.graalvm.org/latest/graalvm-as-a-platform/language-implementation-framework/SpecializationTesting/

### GitHub Repository

- Specialization Histogram Documentation: https://github.com/oracle/graal/blob/master/truffle/docs/SpecializationHistogram.md
- Options Documentation: https://github.com/oracle/graal/blob/master/truffle/docs/Options.md
- Optimizing Documentation: https://github.com/oracle/graal/blob/master/truffle/docs/Optimizing.md
- Reporting Polymorphism: https://github.com/oracle/graal/blob/master/truffle/docs/splitting/ReportingPolymorphism.md

### API Documentation

- SpecializationStatistics Javadoc: https://www.graalvm.org/truffle/javadoc/com/oracle/truffle/api/dsl/SpecializationStatistics.html
- Truffle DSL Package Summary: https://www.graalvm.org/truffle/javadoc/com/oracle/truffle/api/dsl/package-summary.html
- Truffle Javadoc Index: https://www.graalvm.org/truffle/javadoc/index-all.html

### Academic Resources

- Würthinger, T., Wimmer, C., Humer, C., et al. "Truffle DSL: A DSL for Building Self-Optimizing AST Interpreters." ResearchGate Publication 333510523. https://www.researchgate.net/publication/333510523_Truffle_DSL_A_DSL_for_Building_Self-Optimizing_AST_Interpreters

### Tutorials

- Truffle DSL Tutorial (End of Line Blog): https://www.endoflineblog.com/graal-truffle-tutorial-part-3-specializations-with-truffle-dsl-typesystem
