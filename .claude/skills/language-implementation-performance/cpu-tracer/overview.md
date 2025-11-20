# Overview

# Tool Name: --cputracer


## 3. Functional Description

### Primary Purpose

The CPUTracer tool serves as a deterministic, execution count-based profiler for Truffle language implementations, designed to provide exact execution statistics for code elements at various granularities during development and performance analysis phases. Unlike time-based profilers, it counts every execution of traced elements to reveal control flow patterns, algorithm behavior, and optimization effectiveness.

### What the Tool Does

CPUTracer instruments the abstract syntax tree of your program to count how many times specific code elements execute. It tracks execution at three configurable granularities: root level for function and method entries, call level for individual call sites within functions, and statement level for every statement execution. The tool distinguishes between interpreted and compiled executions, providing insight into which code has been optimized by the Graal compiler. At program completion, it outputs either a histogram table showing execution counts, percentages, and source locations, or a JSON structure containing the same data in machine-readable format. The tracer supports sophisticated filtering to focus analysis on specific files, languages, or functions, making it practical to use even on large codebases.

### How It Works

CPUTracer is built on the Truffle Instrumentation Framework and operates through AST node instrumentation rather than statistical sampling. When enabled, the tool attaches ExecutionEventNode listeners to filtered source sections in the program's abstract syntax tree. These instrumentation nodes are inserted at locations corresponding to the requested granularity—roots, calls, or statements. As the program executes, each instrumentation node increments counters every time its associated code element executes, maintaining separate counts for interpreted versus compiled execution modes. This instrumentation happens at the Truffle level, allowing the tool to work identically across all Truffle languages without language-specific implementations. The deterministic nature means every single execution is counted rather than sampled, providing exact execution counts but with corresponding overhead. Upon program completion, the accumulated counters are formatted according to the output specification and written to stdout or the specified output file.

### When to Use

Language developers should use CPUTracer during the development phase when they need to understand exactly how many times different code paths execute, when analyzing algorithmic complexity to verify expected execution patterns, when investigating why certain code isn't being compiled by examining the interpreted versus compiled execution split, when validating that optimizations like inlining and specialization are working by checking that hot paths show high compiled percentages, and when debugging unexpected behavior by tracing actual execution flow through the program. Unlike time-based profilers that might miss fast operations or be skewed by I/O waits, CPUTracer reveals the true execution frequency of each operation, making it invaluable for understanding control flow patterns, loop iteration counts, recursion depths, and branch execution frequencies. It's particularly useful when combined with compilation tracing to correlate execution counts with compilation decisions—if a function executes thousands of times but remains interpreted, that indicates a compilation problem worth investigating.

## 6. Additional Notes

### Overhead

The performance overhead of CPUTracer is moderate to high because it instruments every execution of traced elements rather than sampling periodically. The overhead varies significantly based on tracing granularity: using TraceRoots only (the default) provides function-level data with lower overhead suitable for longer executions; adding TraceCalls increases overhead moderately as it instruments every call site within functions; enabling TraceStatements creates the highest overhead because it instruments every statement. This instrumentation overhead makes CPUTracer inappropriate for production profiling where the lower-overhead CPUSampler is preferred, but acceptable for development and testing where exact execution counts are more valuable than runtime performance. The memory impact involves storing counters for all traced elements, which grows with the number of unique source locations traced. Mitigation strategies include using filters aggressively to limit traced code to relevant subsystems, starting with TraceRoots only and adding finer granularity only for targeted analysis, and using shorter representative workloads that execute enough to reveal patterns without unnecessary prolonged execution.

### Limitations

Several important constraints should be understood when using CPUTracer. The tool provides no timing information—it counts executions but doesn't measure how long each execution takes, making it complementary rather than a replacement for time-based profilers. The instrumentation overhead itself affects program behavior, potentially changing execution patterns especially in timing-sensitive code, though execution counts themselves remain accurate. The default histogram output is a flat list without hierarchical call graph structure, though JSON output provides more structural information. Memory usage grows with the number of traced elements, potentially becoming significant on very large codebases without filtering. The tool profiles only guest language code executed through the Truffle framework and doesn't capture host JVM code execution or native code outside Truffle's view. For asynchronous operations or concurrent execution patterns, the execution counts may not fully capture the logical flow of the program. Finally, the tool is language-agnostic at the Truffle level but requires that the language implementation properly exposes source sections and execution semantics to the instrumentation framework.

### Best Practices

Effective use of CPUTracer requires following several key practices to manage overhead while gathering useful data. Start analysis with TraceRoots only to get function-level execution counts and identify hotspots before drilling into finer-grained detail. Use filters extensively to focus tracing on relevant code—use FilterFile to trace specific source files, FilterRootName to trace specific function patterns, and FilterLanguage to focus on one language in polyglot applications. Apply finer granularity progressively: once TraceRoots identifies hot functions, add TraceCalls or TraceStatements with appropriate filters to analyze only those hot functions rather than the entire codebase. Save output to files using `--cputracer.OutputFile` for later analysis and comparison across runs. Use short representative workloads that execute enough iterations to reach steady state and trigger compilation but don't run unnecessarily long given the overhead. Compare interpreted versus compiled percentages to validate optimization effectiveness, aiming for compiled percentages above 95% on hot paths. Combine CPUTracer with CPUSampler to understand both execution frequency and time consumption. Use JSON output format for automated analysis, scripting, or integration with custom tooling. When investigating specific issues, disable parallel compilation with `--engine.BackgroundCompilation=false` to make behavior more deterministic and easier to correlate with traced execution patterns.

### Common Pitfalls

Developers frequently encounter several avoidable mistakes when using CPUTracer. A frequent mistake is confusing it with CPUSampler and expecting timing information when CPUTracer only provides counts. Many developers enable TraceStatements without filters on large codebases, creating overwhelming overhead and massive output that's impractical to analyze—always filter aggressively when using statement-level tracing. There's often confusion about the interpreted versus compiled percentages: the percentage shown is relative to that element's total executions, not relative to all executions, so a 5% interpreted percentage means 5% of that function's executions were interpreted, not 5% of the program's total execution time. Another pitfall is misunderstanding that multiple entries with the same function name represent different specializations or source locations, not duplicate data. Some developers expect real-time output but the trace only appears at program completion, which can be surprising for long-running programs. Attempting to parse histogram output programmatically without using the JSON format leads to fragile parsing code that breaks across versions. Finally, not accounting for the instrumentation overhead when drawing performance conclusions—the observed runtime with CPUTracer enabled doesn't represent actual performance, only the execution counts are meaningful.

### Related Tools

CPUTracer is part of a comprehensive profiling ecosystem and works best in combination with related tools. The most important related tool is CPUSampler (`--cpusampler`), a time-based sampling profiler that provides wall-clock time measurements and flame graphs—use CPUSampler for timing analysis and CPUTracer for execution frequency analysis together. MemTracer (`--experimental-options --memtracer`) profiles memory allocation patterns and can be correlated with CPUTracer execution counts to identify allocation hotspots. Compilation tracing tools including `--engine.TraceCompilation` for per-compilation events, `--engine.CompilationStatistics` for aggregate compilation metrics, and `--engine.TraceInlining` for inlining decisions help explain the interpreted versus compiled split observed in CPUTracer. The Ideal Graph Visualizer (IGV) tool accessed via `--vm.Dgraal.Dump` visualizes compiler intermediate representations for methods identified as problematic in CPUTracer output. These tools can be combined in a single execution: for example, running with both `--cpusampler`, `--cputracer`, and `--experimental-options --memtracer` provides comprehensive profiling data across time, execution counts, and allocations simultaneously, allowing correlation analysis to identify true performance bottlenecks.

## 7. Resources

### Official GraalVM Documentation

- GraalVM Profiling Tools Guide: https://www.graalvm.org/latest/tools/profiling/
- Oracle GraalVM Profiling Documentation: https://docs.oracle.com/en/graalvm/jdk/20/docs/tools/profiling/
- GraalVM JDK 24 Profiling Tools: https://www.graalvm.org/jdk24/tools/profiling/
- Truffle Profiling Guide: https://www.graalvm.org/latest/graalvm-as-a-platform/language-implementation-framework/Profiling/

### API Documentation

- CPUTracer JavaDoc: https://www.graalvm.org/tools/javadoc/com/oracle/truffle/tools/profiler/CPUTracer.html
- CPUTracer.Payload JavaDoc: https://www.graalvm.org/tools/javadoc/com/oracle/truffle/tools/profiler/CPUTracer.Payload.html
- Profiler Package Overview: https://www.graalvm.org/tools/javadoc/com/oracle/truffle/tools/profiler/package-summary.html
- Truffle Instrumentation API: https://www.graalvm.org/truffle/javadoc/com/oracle/truffle/api/instrumentation/package-summary.html

### GitHub Repository

- Oracle/Graal Main Repository: https://github.com/oracle/graal
- Truffle Framework Source: https://github.com/oracle/graal/tree/master/truffle

### Additional Resources

- GraalVM Official Website: https://www.graalvm.org/
- GraalVM Downloads: https://www.graalvm.org/downloads/
- GraalVM Release Notes: https://www.graalvm.org/release-notes/JDK_25/
- Truffle Language Implementation Framework: https://www.graalvm.org/latest/graalvm-as-a-platform/language-implementation-framework/