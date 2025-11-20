# Overview

# Tool Name: --memtracer


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
