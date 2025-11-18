---
name: Memory Tracer
description: Experimental allocation profiler tracking memory allocations at guest-language level. Shows allocation sites, object types, and memory pressure patterns. Use to: identify allocation hotspots, find unnecessary object creation, and understand memory behavior. High allocations in hot loops = optimization opportunity if escape analysis isn't eliminating them.
---

# Tool Name: --memtracer

## 1. Command Execution

### Basic Command
```bash
<language-launcher> --experimental-options --memtracer <script>
```

### Full Command Syntax
```bash
<language-launcher> --experimental-options --memtracer [--memtracer.<option>=<value>] <script-file>

# Examples with different output modes
js --experimental-options --memtracer --memtracer.Output=histogram primes.js 
js --experimental-options --memtracer --memtracer.Output=typehistogram primes.js 
js --experimental-options --memtracer --memtracer.Output=calltree primes.js 

# With tracing options
js --experimental-options --memtracer --memtracer.TraceStatements --memtracer.TraceCalls primes.js

# With filters
js --experimental-options --memtracer --memtracer.FilterRootName="Math.*" --memtracer.FilterFile="*mycode*" script.js
```

### Execution Context

* **JVM Mode**: Yes - Full support with all Truffle-based languages
* **Native Image Mode**: Yes - Available when Truffle languages are embedded (requires Truffle framework)
* **Prerequisites**:
  - GraalVM installation with Truffle framework
  - `--experimental-options` flag (mandatory, tool is experimental)
  - Language implementation must use Truffle AllocationReporter API
  - Not suitable for production (development/debugging only)

## 2. Command Options & Parameters

| Option                        | Type          | Default   | Description                                                                      |
| ----------------------------- | ------------- | --------- | -------------------------------------------------------------------------------- |
| `--experimental-options`      | Boolean       | false     | Required flag to enable experimental tools (mandatory prefix)                    |
| `--memtracer`                 | Boolean       | false     | Enable the Memory Tracer tool                                                    |
| `--memtracer.Output`          | String (enum) | histogram | Output format: `histogram`, `typehistogram`, or `calltree`                       |
| `--memtracer.StackLimit`      | Integer       | 10000     | Maximum number of stack elements to record (range: 1 to ∞)                       |
| `--memtracer.TraceCalls`      | Boolean       | false     | Capture function/method call sites in the trace                                  |
| `--memtracer.TraceRoots`      | Boolean       | true      | Capture allocations at root function level                                       |
| `--memtracer.TraceStatements` | Boolean       | false     | Capture allocations at statement-level granularity                               |
| `--memtracer.TraceInternal`   | Boolean       | false     | Include internal/framework allocations in trace                                  |
| `--memtracer.FilterFile`      | String        | none      | Wildcard filter for source file paths (e.g., `*program*.sl`)                     |
| `--memtracer.FilterLanguage`  | String        | none      | Only profile languages with specified ID (e.g., `js`)                            |
| `--memtracer.FilterMimeType`  | String        | none      | Only profile languages with specified MIME type (e.g., `application/javascript`) |
| `--memtracer.FilterRootName`  | String        | none      | Wildcard filter for program roots/function names (e.g., `Math.*`)                |

### Key Options

**--memtracer.Output**: This option controls the aggregation and presentation format of allocation data. The default `histogram` mode groups allocations by source location (function and line), showing where in the code allocations occur. The `typehistogram` mode groups by the type or class of allocated objects, revealing what kinds of objects are being created most frequently. The `calltree` mode presents allocations in a hierarchical call tree structure, similar to CPU profiler output, showing the allocation patterns within the calling context.

**--memtracer.TraceStatements vs --memtracer.TraceRoots**: These options control granularity of allocation attribution. `TraceRoots` (enabled by default) attributes allocations to root-level functions, providing a high-level view suitable for initial investigation. `TraceStatements` increases granularity to individual statements within functions, enabling precise identification of allocation sources at the cost of more detailed (and potentially overwhelming) output. These options can be combined: enabling both provides full detail while maintaining the high-level summary.

**--memtracer.TraceCalls**: Enabling this option instructs the tool to record not just the immediate location of allocations but also the call sites—which functions called the allocating function. This creates a richer call hierarchy in the output, particularly valuable with `calltree` output mode, as it shows allocation patterns in their full calling context. The additional call site tracking increases overhead but provides crucial information for understanding why certain functions allocate frequently.

### Advanced Options

**Filter Options (FilterFile, FilterLanguage, FilterMimeType, FilterRootName)**: These options enable focused profiling on specific code regions, essential when analyzing large applications where full allocation traces become overwhelming. Filters use wildcard expressions for file names and root names, allowing patterns like `*core*` to match any file containing "core" in its name. Multiple filters can be combined—for instance, filtering by both language and root name to profile only JavaScript functions whose names match a pattern. Effective filtering dramatically reduces overhead and output volume, making the tool practical for larger codebases.

**--memtracer.StackLimit**: This option caps the number of stack frames recorded per allocation event. The default of 10,000 is sufficient for most programs, but deeply recursive algorithms or pathological call stacks might hit this limit. Increasing the limit captures more complete stack information at the cost of higher memory usage for the profiling data structures. Conversely, decreasing the limit on very allocation-heavy workloads can reduce profiler memory overhead, though at the risk of truncating important call stack information.

**--memtracer.TraceInternal**: This advanced option includes allocations from internal framework code and runtime libraries rather than exclusively guest language code. While normally disabled to focus on user code, enabling it helps language implementers understand memory behavior of the language runtime itself, including allocations in built-in libraries, runtime support code, and Truffle framework infrastructure. This option is primarily valuable for language implementers debugging the language implementation rather than application developers optimizing their code.

## 3. Functional Description
### Primary Purpose

The `--memtracer` tool provides memory allocation profiling at the guest language level for Truffle-based language implementations. Its primary purpose is to help developers understand allocation patterns in their source code by tracking where allocations occur during program execution, independently of whether those allocations survive compiler optimization. This tool serves both language implementers who need to understand their runtime's allocation behavior and application developers who want to optimize memory-intensive algorithms.
### What the Tool Does

What the tool does operationally is instrument the Truffle framework's AllocationReporter API to intercept and record allocation events as they occur during program execution. When a Truffle language implementation creates objects—whether JavaScript objects, Python lists, Ruby strings, or any other guest language construct—the runtime reports these allocations to the AllocationReporter. The memtracer registers as an allocation listener, receiving callbacks for each allocation event that passes its configured filters. For each event, it captures the source location, optional call stack information (if TraceCalls is enabled), and accumulates counts. At program termination, the tool aggregates this accumulated data into the requested output format and prints the results to the console.
### How it works

The mechanism operates through instrumentation rather than sampling or tracing compilation artifacts. Truffle language implementations are structured to report allocations through a standardized API, calling methods like `onEnter` before allocation and `onReturnValue` after allocation completes. The memtracer hooks into this reporting infrastructure using Truffle's instrumentation framework, which provides event filtering, source location attribution, and efficient callback dispatch. This instrumentation approach captures logical allocations at the source code level—the allocations that appear to happen from the guest language perspective—rather than actual JVM heap allocations. This distinction is crucial: the Graal compiler applies aggressive optimization including escape analysis and scalar replacement that can eliminate many allocations entirely, so an allocation visible in memtracer output might never actually occur in optimized compiled code.

The guest-language level perspective means the tool operates above the compilation and optimization layers. It shows what the source code appears to allocate based on language semantics, not what the runtime actually allocates in memory. For example, a JavaScript function that creates a new object on every iteration will show that allocation in memtracer even if the compiler determines the object doesn't escape and optimizes it into scalar variables or eliminates it entirely. This pre-optimization view is valuable for understanding algorithmic allocation patterns and comparing implementation approaches, but it shouldn't be confused with actual memory consumption or heap profiling.
### When to use

Language developers should use memtracer during the development and optimization phases when they need to understand allocation behavior of their code or language implementation. It excels at answering questions like "which functions allocate the most objects?" or "what types of objects does this algorithm create?" or "where are unexpected allocations coming from?" The tool is particularly valuable when combined with CPU profilers: high CPU time in a function combined with high allocation counts suggests memory pressure might be causing GC overhead. It's also useful for comparing alternative algorithm implementations to see which approach generates fewer intermediate objects, or for validating that optimizations intended to reduce allocations actually do so at the source level. However, developers must remember that memtracer shows pre-optimization allocations, so its output doesn't directly predict memory usage or GC pressure—the compiler might eliminate many of the allocations it reports.

## 4. Output Format & Structure

### Output Type
- ☑ Console/Terminal output (printed on program exit)
- ☐ File output (can be redirected via shell)
- ☐ Graphical output
- ☐ Binary output

### Output Location
**Default Location**: Standard output (stdout) when program terminates

**Custom Location**: Use shell redirection to save output: `js script.js --experimental-options --memtracer > allocations.txt`

### Output Format Examples

**Histogram Mode (Default):**
```
Location Histogram with Allocation Counts.
Recorded a total of 5007 allocations.

Total Count: Number of allocations during the execution of this element.
Self Count: Number of allocations in this element alone (excluding sub calls).

Name                | Self Count    | Total Count   | Location
--------------------------------------------------------------------------------
next                | 5000  99.9%   | 5000  99.9%   | primes.js~31-37:537-737
:program            | 6     0.1%    | 5007  100.0%  | primes.js~1-46:0-982
Primes              | 1     0.0%    | 1     0.0%    | primes.js~25-38:424-739
```

**Annotations for Histogram:**
- Header explains total allocations recorded
- Each row represents a function or code location
- Self Count: allocations directly in this element (excluding called functions)
- Total Count: allocations including all sub-calls
- Percentages show proportion of total allocations
- Location format: `file~line-range:character-range`
- Ordered by Total Count descending (most allocations at top)

**Typehistogram Mode:** (format shows allocation counts grouped by object type/class rather than location; specific output format not documented in official sources but analogous to histogram with type names instead of function names)

**Calltree Mode:** (format shows hierarchical call tree with allocation counts at each level, similar to CPU profiler calltree; specific output format not documented in official sources but follows standard call tree conventions with indentation showing call hierarchy)

### Output Fields/Metrics

| Field       | Description                                                          | Unit                       | Interpretation                                                                                      |
| ----------- | -------------------------------------------------------------------- | -------------------------- | --------------------------------------------------------------------------------------------------- |
| Name        | Function name, method name, or code element identifier               | String                     | Identifies where allocations occur; `:program` represents top-level code                            |
| Self Count  | Number of allocations directly in this element, excluding sub-calls  | Count (with percentage)    | Shows allocations attributable exclusively to this function's own code                              |
| Total Count | Number of allocations including all functions called by this element | Count (with percentage)    | Shows total allocation impact including all downstream effects                                      |
| Location    | Source file and position                                             | Format: `file~lines:chars` | Direct pointer to source code location; lines and character ranges enable precise navigation        |
| Type/Class  | Object type or class name (typehistogram mode)                       | String                     | Shows what kinds of objects are created (e.g., Array, String, custom classes)                       |
| Percentage  | Proportion relative to total allocations                             | Percentage                 | Enables quick identification of allocation hotspots; high percentages indicate optimization targets |

**Note on Size Information**: The AllocationReporter API includes size parameters, but many language implementations report `SIZE_UNKNOWN` rather than actual byte counts. Therefore, memtracer primarily focuses on allocation counts rather than memory sizes in bytes. The tool tracks how many objects are allocated rather than how much memory they consume.

## 5. Insights & Analysis

Developers reach for `--memtracer` when they suspect that memory allocation patterns are impacting performance or when they need to understand the allocation characteristics of their algorithms. The tool addresses questions that arise during optimization work: Why is garbage collection consuming so much time? Which functions create the most objects? Are there unexpected allocations in supposedly allocation-free hot paths? What types of objects dominate the allocation profile? These questions become pressing when profilers show significant GC overhead, when memory usage seems disproportionate to problem size, or when comparing alternative implementations to select the most memory-efficient approach.

### Insight 1: Identifying allocation-intensive functions that drive GC pressure

When examining histogram output, the Self Count and Total Count columns reveal functions responsible for most allocations. Seeing a function like `next` with `5000 (99.9%)` in the Self Count column immediately identifies it as the dominant allocation source. If this function executes frequently in the main computation loop, it's directly responsible for allocation pressure that triggers garbage collection. The nearly identical Self and Total counts indicate the allocations happen directly in this function rather than in functions it calls, making it a prime optimization target.

The root cause of allocation-intensive functions typically lies in algorithmic choices or implementation patterns that create intermediate objects. In an example, each iteration creates a new object rather than reusing existing objects or using mutable data structures. This pattern of per-iteration allocation is common in functional programming styles and in implementations that prioritize code clarity over memory efficiency. Other common causes include string concatenation in loops (creating intermediate string objects), repeated conversion between data representations (boxing/unboxing primitives), or collection operations that create intermediate collections at each step (map, filter, reduce chains in languages with immutable collections).

To resolve allocation pressure from dominant functions, first verify that the allocations are necessary for correctness. If the algorithm fundamentally requires creating objects, consider alternative approaches: object pooling to reuse instances, mutable data structures instead of creating new immutable ones, or in-place modifications instead of copy-on-write. For the primes example, implementing a fixed-size pool of DivisibleByFilter objects that get recycled would eliminate most allocations. When allocations are necessary, ensure they're being performed efficiently—for instance, pre-sizing collections instead of allowing repeated growth, or using specialized collection types. After implementing changes, re-run memtracer to verify that allocation counts decrease and use `--cpusampler` to confirm that GC time improves. Remember that memtracer shows pre-optimization allocations, so some of the allocations it reports might already be eliminated by the compiler in hot code paths—verify actual performance impact rather than optimizing based solely on allocation counts.

### Insight 2: Discovering unexpected allocations in performance-critical code

Sometimes memtracer reveals allocations in functions that were designed to be allocation-free or where allocations weren't expected. For example, finding that a supposedly lightweight helper function appears in the output with significant allocation counts, or discovering that a function thought to operate entirely on primitives is actually creating boxed objects. The location information points directly to the source code responsible, enabling investigation of why allocations are occurring.

Unexpected allocations often arise from subtle language semantics or implementation details that aren't obvious from the source code. In dynamically typed languages, operations that appear to work with primitives might trigger boxing when values flow to contexts requiring objects. Operator overloading or implicit conversions can introduce allocations. Built-in functions that seem lightweight might allocate internal data structures. In JavaScript, for instance, accessing array elements with non-integer indices creates property maps; in Python, list comprehensions create new lists even when generator expressions would suffice. These allocations happen according to language specifications but might not be apparent to developers familiar with other languages or expecting different semantics.

The resolution depends on understanding the specific source of unexpected allocations. Use memtracer with `--memtracer.TraceStatements` to pinpoint exactly which statements allocate, then examine those statements for implicit allocations. Consult language documentation to understand when operations allocate—for instance, learning which Python operations return views versus copies, or which JavaScript operations trigger object creation. Consider alternative APIs or language constructs that avoid allocation: using iterators instead of materializing collections, explicit type annotations to prevent boxing, or specialized APIs for allocation-sensitive contexts. For language implementers, unexpected allocations might indicate opportunities to improve the implementation, such as using more aggressive caching or supporting allocation-free fast paths for common cases. When working in performance-critical code, establishing allocation budgets and using memtracer in continuous integration to catch allocation regressions can prevent unexpected allocations from creeping in during development.

### Insight 3: Understanding object type distribution for targeted optimization

The `typehistogram` output mode groups allocations by object type or class, revealing which kinds of objects dominate the allocation profile. Seeing that 80% of allocations are of a particular class or that unexpected types appear frequently in the profile guides optimization efforts toward the most impactful types. For instance, discovering that String objects dominate allocations might suggest excessive string operations, or finding that temporary collection objects represent most allocations might indicate opportunities for lazy evaluation.

Type-based allocation patterns typically reflect the fundamental data structures and operations in the algorithm. Array-heavy algorithms naturally allocate many arrays; tree traversal code allocates tree nodes; string processing creates strings. However, sometimes the type distribution reveals inefficiencies: excessive intermediate collections from chained operations, defensive copying that creates unnecessary duplicates, or boxed primitives where unboxed values would suffice. In statically typed languages, seeing object types where primitive arrays could work indicates missed optimization opportunities. In dynamically typed languages, the presence of wrapper objects or boxed values might indicate that type specialization isn't occurring effectively.

To act on type distribution insights, first categorize allocations as necessary versus eliminable. Necessary allocations—the primary data structures the algorithm manipulates—might be optimizable in degree (allocating smaller structures, reusing instances) but not eliminable entirely. Focus optimization efforts on eliminable intermediate allocations: temporary collections that could be avoided with streaming operations, intermediate strings that could use string builders, or short-lived objects that could be replaced with stack allocation or escape analysis-friendly patterns. For types representing state or computation results, consider whether mutable versions could replace immutable ones, or whether pooling strategies apply. Language-specific optimizations matter: in Java, using primitive collections avoids boxing; in JavaScript, using typed arrays avoids generic Array overhead. After optimization, verify with both memtracer (to confirm allocation reduction) and CPU profiling (to ensure the changes actually improve performance rather than merely shifting bottlenecks).

### Insight 4: Correlating allocations with execution hotspots for comprehensive optimization

The most powerful use of memtracer comes from combining it with CPU profiling tools like `--cpusampler`. When a function appears as both a CPU hotspot (consuming significant execution time) and an allocation hotspot (creating many objects), it indicates that memory pressure might be contributing to the performance problem through garbage collection overhead. Conversely, if a function allocates heavily but doesn't appear in CPU profiles, the allocations might not matter—perhaps they occur infrequently, or the compiler eliminates them in hot code.

Memory allocation and CPU time correlate most strongly when allocation pressure triggers frequent garbage collection. GC pauses interrupt execution, and even concurrent collectors impose overhead through barriers and background work. Functions that combine high allocation rates with high execution frequency create sustained memory pressure that keeps the garbage collector active. This correlation is particularly strong in young generation collectors where frequent short-lived object allocation directly triggers minor GC events. The relationship is more nuanced for long-lived objects or when escape analysis eliminates allocations in compiled code.

To effectively use this correlation, profile both CPU and memory simultaneously by enabling both `--cpusampler` and `--memtracer` on the same run. Compare the outputs to identify functions appearing prominently in both profiles—these represent the highest-value optimization targets. For functions with high CPU time but low allocation counts, focus on computational optimization rather than memory optimization. For functions with high allocation counts but low CPU time, investigate whether the allocations cause problems: check if they're in infrequently called code (less important), whether they're long-lived (might contribute to heap pressure without causing frequent GC), or whether they're in startup code (might affect warmup but not steady-state performance). The combination of both profiles provides a complete performance picture: allocation patterns explain memory behavior, CPU profiles show actual time costs, and together they reveal whether optimizing allocations will yield meaningful performance improvements or merely make code more complex without practical benefit.

## 6. Additional Notes

### Overhead

Performance overhead from memtracer stems from its instrumentation-based approach but specific quantitative measurements are not published in official documentation. The tool intercepts every allocation event through the AllocationReporter API, adding callback overhead to each allocation. Enabling `--memtracer.TraceCalls` increases overhead further by capturing and storing call stacks. Enabling `--memtracer.TraceStatements` adds even more granularity and thus more tracking overhead. What is clear is that the overhead is substantial enough that the tool is positioned exclusively for development use, not production deployment or realistic performance benchmarking. The exact overhead varies based on allocation frequency in the profiled program, enabled tracing options, and filter configuration.
### Limitation

The most significant limitation is the tool's focus on allocation counts rather than memory sizes. The AllocationReporter API includes size parameters, but many language implementations report SIZE_UNKNOWN because accurately determining object sizes at allocation time is difficult in dynamic languages with growing data structures. Consequently, memtracer tells you how many objects are allocated but not how much memory they consume. This limitation means the tool is most useful for understanding algorithmic allocation patterns—how many objects, what types, where in code—rather than for memory budgeting or heap size estimation. Developers needing actual memory consumption data should use heap dumps or memory analyzers instead.

Another important limitation is that memtracer shows pre-optimization allocations. The tool captures logical allocations that appear to occur based on source code semantics, not actual runtime allocations after compiler optimization. The Graal compiler applies aggressive optimization including escape analysis that eliminates allocations for objects that don't escape their creation scope, and scalar replacement that converts object allocations into local variables. An allocation appearing in memtracer output might never actually occur in compiled code during steady-state execution. This means memtracer is valuable for understanding interpreter behavior and comparing algorithm allocation patterns, but it doesn't directly predict actual heap usage or garbage collection frequency in long-running optimized code.
### Best Practice

Best practices for using memtracer center on focused profiling with appropriate filters and incremental investigation. Rather than profiling entire large applications, use filter options to focus on specific modules, files, or functions under investigation. Start with the default histogram mode and TraceRoots to get a high-level overview, then progressively add TraceStatements or TraceCalls for more detail in specific areas of interest. Combine memtracer with CPU profiling to understand the relationship between allocation patterns and actual performance impact. Use the tool iteratively: profile to identify an issue, make a targeted change, profile again to verify improvement. This focused workflow prevents overwhelming output and makes it practical to use the tool effectively despite its overhead.
### Common Pitfall

Common pitfalls include treating allocation counts as direct predictors of memory usage, which ignores both object sizes and compiler optimizations. Developers sometimes optimize based solely on memtracer output without verifying that changes actually improve performance, potentially making code more complex without meaningful benefit. Another pitfall is forgetting the `--experimental-options` flag, which causes memtracer to fail silently or with unclear errors. Developers also sometimes use memtracer in production or performance benchmark environments where its overhead skews results, defeating the purpose of measurement. Understanding that memtracer shows the pre-optimization view helps avoid misinterpretation: it reveals what the source code does, not what the optimized runtime does, making it a guide for algorithm design rather than a precise predictor of memory behavior.
### Related Tools

Related tools in the GraalVM ecosystem each serve specific profiling needs. The CPU Sampler (`--cpusampler`) provides low-overhead time-based profiling suitable even for production-like workloads, using sampling to minimize overhead while identifying performance hotspots. The CPU Tracer (`--cputracer`) offers precise execution count profiling similar to memtracer's instrumentation approach, tracking how many times each statement or call executes rather than allocations. Both complement memtracer by providing the CPU perspective while memtracer provides the memory allocation perspective. Heap dump tools offer point-in-time snapshots of actual heap contents, showing what survived optimization and collection, contrasting with memtracer's view of logical allocations. The unique value of memtracer lies in its guest-language level allocation perspective combined with source code attribution, making it irreplaceable for understanding allocation patterns in Truffle language implementations despite its limitations and experimental status.

## 7. Resources

### Official GraalVM Documentation

- GraalVM Profiling Tools Guide: https://www.graalvm.org/latest/tools/profiling/
- Oracle GraalVM JDK 20 Profiling: https://docs.oracle.com/en/graalvm/jdk/20/docs/tools/profiling/
- Oracle GraalVM Enterprise 19 Profiling: https://docs.oracle.com/en/graalvm/enterprise/19/guide/tools/profiling.html
- Oracle GraalVM Enterprise 20 Profiling: https://docs.oracle.com/en/graalvm/enterprise/20/docs/tools/profiling/
- GraalVM 22.0 Profiling: https://www.graalvm.org/22.0/tools/profiling/

### Language-Specific Documentation

- GraalJS FAQ (includes memtracer usage): https://www.graalvm.org/latest/reference-manual/js/FAQ/
- GraalPy Tooling Guide: https://www.graalvm.org/latest/reference-manual/python/Tooling/
- TruffleRuby Options Reference: https://www.graalvm.org/jdk20/reference-manual/ruby/Options/
- Espresso Demos (memtracer examples): https://www.graalvm.org/jdk24/reference-manual/espresso/demos/

### Truffle Framework Documentation

- Truffle Language Implementation Framework: https://www.graalvm.org/latest/graalvm-as-a-platform/language-implementation-framework/
- AllocationReporter JavaDoc: https://www.graalvm.org/truffle/javadoc/com/oracle/truffle/api/instrumentation/AllocationReporter.html

### GitHub Repository

- Oracle Graal Repository (Truffle directory): https://github.com/oracle/graal/tree/master/truffle
- Truffle Languages List: https://github.com/oracle/graal/blob/master/truffle/docs/Languages.md
- GraalJS Issue #139 (memtracer discussion): https://github.com/oracle/graaljs/issues/139

### Related Technical Articles

- Native Memory Tracking in GraalVM Native Image: https://developers.redhat.com/articles/2024/05/21/native-memory-tracking-graalvm-native-image
