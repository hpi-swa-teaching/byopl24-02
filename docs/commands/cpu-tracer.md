---
name: CPU Tracer
description: Counts exact execution frequencies (not time) at function/statement level. Shows interpreted vs compiled execution split. Use to: verify functions are compiling (aim for >95% compiled), understand control flow patterns, validate algorithmic complexity, and quantify how often code paths execute. Complements CPUSampler by showing frequency rather than duration.
---
# Tool Name: --cputracer

## 1. Command Execution

### Basic Command
```bash
<language-launcher> --cputracer <script>
```

### Full Command Syntax
```bash
# Complete syntax with all available options
<language-launcher> <script> --cputracer \
  --cputracer.TraceStatements \
  --cputracer.TraceCalls \
  --cputracer.TraceRoots=true|false \
  --cputracer.FilterRootName=<pattern> \
  --cputracer.FilterFile=<pattern> \
  --cputracer.FilterLanguage=<languageId> \
  --cputracer.FilterMimeType=<mime-type> \
  --cputracer.Output=histogram|json \
  --cputracer.OutputFile=<path>

# Examples
js primes.js --cputracer
js app.js --cputracer --cputracer.TraceStatements --cputracer.FilterRootName=*parse*
js script.js --cputracer --cputracer.Output=json --cputracer.OutputFile=trace.json
```

### Execution Context

- **JVM Mode Compatibility**: Yes
  - **Requirements**: GraalVM with Truffle-based language runtime
  - **Access**: Standard command-line flag or Polyglot API
  
- **Native Image Mode Compatibility**: Yes
  - **Requirements**: Language compiled to native executable with Truffle support
  - **Functionality**: Full feature parity with JVM mode
  
- **Prerequisites**:
  - GraalVM installation (Community or Enterprise Edition)
  - Truffle-based language (GraalJS, GraalPy, TruffleRuby, FastR, GraalWasm)
  - No additional configuration files needed
  - Built-in Truffle instrument, no separate installation required

## 2. Command Options & Parameters

### Key Options

| Option                        | Type                  | Default   | Description                                                                |
| ----------------------------- | --------------------- | --------- | -------------------------------------------------------------------------- |
| `--cputracer`                 | Boolean flag          | false     | Enable the CPU tracer instrument                                           |
| `--cputracer.TraceRoots`      | Boolean               | true      | Trace function/method entries (root executions)                            |
| `--cputracer.TraceCalls`      | Boolean               | false     | Trace call sites within functions                                          |
| `--cputracer.TraceStatements` | Boolean               | false     | Trace individual statements (most detailed granularity)                    |
| `--cputracer.Output`          | Enum (histogram/json) | histogram | Output format: histogram (human-readable table) or json (machine-readable) |
| `--cputracer.OutputFile`      | String (path)         | stdout    | Redirect output from stdout to specified file path                         |

### Advanced Options

| Option                       | Type                      | Default   | Description                                                                  |
| ---------------------------- | ------------------------- | --------- | ---------------------------------------------------------------------------- |
| `--cputracer.FilterFile`     | String (wildcard pattern) | no filter | Filter by source file path (e.g., `program.sl`, `*.js`, `app/*`)             |
| `--cputracer.FilterLanguage` | String (languageId)       | no filter | Filter by language ID (e.g., `js`, `python`, `ruby`, `R`)                    |
| `--cputracer.FilterMimeType` | String (mime-type)        | no filter | Filter by MIME type (e.g., `application/javascript`, `text/x-python`)        |
| `--cputracer.FilterRootName` | String (wildcard pattern) | no filter | Filter by function/method name pattern (e.g., `Math.*`, `*accept`, `parse*`) |

**Advanced Option Details**:

- **Wildcard Patterns**: All filter options support `*` wildcard for pattern matching. For example, `Math.*` matches all functions starting with "Math.", `*parse*` matches functions containing "parse", and `*.js` matches all JavaScript files.

- **Granularity Hierarchy**: TraceRoots provides function-level data, TraceCalls adds call-site information within functions, and TraceStatements provides statement-level detail. These can be combined, with overhead increasing at each level.

- **Filter Combination**: Multiple filters can be applied simultaneously and work as logical AND—for example, combining FilterLanguage=js with FilterFile=*.js and FilterRootName=Math.* traces only JavaScript Math functions in .js files.

## 3. Functional Description

### Primary Purpose

The CPUTracer tool serves as a deterministic, execution count-based profiler for Truffle language implementations, designed to provide exact execution statistics for code elements at various granularities during development and performance analysis phases. Unlike time-based profilers, it counts every execution of traced elements to reveal control flow patterns, algorithm behavior, and optimization effectiveness.

### What the Tool Does

CPUTracer instruments the abstract syntax tree of your program to count how many times specific code elements execute. It tracks execution at three configurable granularities: root level for function and method entries, call level for individual call sites within functions, and statement level for every statement execution. The tool distinguishes between interpreted and compiled executions, providing insight into which code has been optimized by the Graal compiler. At program completion, it outputs either a histogram table showing execution counts, percentages, and source locations, or a JSON structure containing the same data in machine-readable format. The tracer supports sophisticated filtering to focus analysis on specific files, languages, or functions, making it practical to use even on large codebases.

### How It Works

CPUTracer is built on the Truffle Instrumentation Framework and operates through AST node instrumentation rather than statistical sampling. When enabled, the tool attaches ExecutionEventNode listeners to filtered source sections in the program's abstract syntax tree. These instrumentation nodes are inserted at locations corresponding to the requested granularity—roots, calls, or statements. As the program executes, each instrumentation node increments counters every time its associated code element executes, maintaining separate counts for interpreted versus compiled execution modes. This instrumentation happens at the Truffle level, allowing the tool to work identically across all Truffle languages without language-specific implementations. The deterministic nature means every single execution is counted rather than sampled, providing exact execution counts but with corresponding overhead. Upon program completion, the accumulated counters are formatted according to the output specification and written to stdout or the specified output file.

### When to Use

Language developers should use CPUTracer during the development phase when they need to understand exactly how many times different code paths execute, when analyzing algorithmic complexity to verify expected execution patterns, when investigating why certain code isn't being compiled by examining the interpreted versus compiled execution split, when validating that optimizations like inlining and specialization are working by checking that hot paths show high compiled percentages, and when debugging unexpected behavior by tracing actual execution flow through the program. Unlike time-based profilers that might miss fast operations or be skewed by I/O waits, CPUTracer reveals the true execution frequency of each operation, making it invaluable for understanding control flow patterns, loop iteration counts, recursion depths, and branch execution frequencies. It's particularly useful when combined with compilation tracing to correlate execution counts with compilation decisions—if a function executes thousands of times but remains interpreted, that indicates a compilation problem worth investigating.

## 4. Output Format & Structure

### Output Type
- ☑ Console/Terminal (stdout by default)
- ☑ File output (text format via --cputracer.OutputFile)
- ☑ File output (JSON format via --cputracer.Output=json)
- ☐ Graphical
- ☐ Binary

### Output Location

- **Default**: stdout (console)
- **Custom specification**: Use `--cputracer.OutputFile=<path>` to specify file
- **Timing**: Output appears at program completion

### Output Format Example

**Histogram Format (Default)**:

```
-----------------------------------------------------------------------------------------
Tracing Histogram. Counted a total of 468336895 element executions.

Total Count: Number of times the element was executed and percentage of total executions.
Interpreted Count: Number of times the element was interpreted and percentage of total executions of this element.
Compiled Count: Number of times the compiled element was executed and percentage of total executions of this element.
-----------------------------------------------------------------------------------------

Name       | Total Count      | Interpreted Count | Compiled Count    | Location
-----------------------------------------------------------------------------------------
accept     | 234117338 50.0%  | 365660 0.2%      | 233751678 99.8%  | primes.js~15:245-258
accept     | 117053670 25.0%  | 182582 0.2%      | 116871088 99.8%  | primes.js~16-18:275-348
accept     | 117005061 25.0%  | 181001 0.2%      | 116824060 99.8%  | primes.js~19:362-381
accept     | 53608 0.0%       | 1829 3.4%        | 51779 96.6%      | primes.js~14:211-227
accept     | 53608 0.0%       | 1829 3.4%        | 51779 96.6%      | primes.js~13-22:191-419
accept     | 48609 0.0%       | 1581 3.3%        | 47028 96.7%      | primes.js~17:322-334
accept     | 4999 0.0%        | 248 5.0%         | 4751 95.0%       | primes.js~21:402-413
accept     | 1 0.0%           | 1 100.0%         | 0 0.0%           | primes.js~2-4:25-61
accept     | 1 0.0%           | 1 100.0%         | 0 0.0%           | primes.js~3:45-55
-----------------------------------------------------------------------------------------
```

**JSON Format** (via `--cputracer.Output=json`):
- Machine-readable JSON structure with similar data
- Includes hierarchical call graph information
- Parseable by automated analysis tools or jq
- Exact schema varies by version but maintains execution count and location data

### Output Fields/Metrics

| Field                 | Description                          | Unit                   | Interpretation                      |
| --------------------- | ------------------------------------ | ---------------------- | ----------------------------------- |
| **Name**              | Function/method/statement identifier | string                 | Code element being traced           |
| **Total Count**       | Absolute execution count             | integer count          | Higher = hotter code path           |
| **Total Count %**     | Percentage of all traced executions  | percentage (0-100%)    | Relative importance of this element |
| **Interpreted Count** | Executions in interpreted mode       | integer count          | Executions before compilation       |
| **Interpreted %**     | Interpreted as % of element's total  | percentage (0-100%)    | Lower is better for hot code        |
| **Compiled Count**    | Executions in compiled mode          | integer count          | Executions after Graal compilation  |
| **Compiled %**        | Compiled as % of element's total     | percentage (0-100%)    | Higher is better for hot code       |
| **Location**          | Source location                      | file~line(s):offset(s) | Where in source code                |

**Location Format Details**:
- Single line: `file.js~15:245-258` (file, line 15, characters 245-258)
- Line range: `file.js~16-18:275-348` (file, lines 16-18, characters 275-348)
- Multiple splits: Same function may appear multiple times with different locations due to specialization

## 5. Insights & Analysis

### When should you use it?

Language developers should reach for CPUTracer when they need deterministic execution counts rather than time-based sampling, when they want to understand exact control flow patterns and algorithmic complexity in practice, when investigating why certain hot code remains interpreted rather than compiled, when validating that optimizations like partial evaluation and specialization are achieving the expected execution patterns, and when debugging unexpected behavior by seeing exactly which code paths execute and how often. This tool answers questions like "How many times does this loop actually iterate?", "Which branches in this conditional are actually taken?", "Is this hot function being compiled or stuck in interpreted mode?", and "What's the actual execution pattern of this recursive algorithm?" It solves the fundamental problem of execution visibility—while time-based profilers tell you what's slow, CPUTracer tells you what's frequent, which is often more actionable for optimization since many small fast operations can collectively dominate performance if they execute millions of times.

### Insight: Compilation Effectiveness Analysis

When examining the traced output, compiled percentages reveal how effectively the Graal compiler is optimizing your code. For genuinely hot code paths—those with high total counts above 10,000 executions—you should expect to see compiled percentages above 95%, indicating that after initial warmup and profiling in interpreted mode, the compiler successfully generated optimized machine code that handles almost all subsequent executions. If instead you observe high total counts combined with high interpreted percentages exceeding 10-20%, this symptom indicates a compilation barrier where hot code is failing to compile. The root cause is typically one of several issues: the function contains operations that prevent compilation such as certain reflective operations or non-trivial boundaries, the compilation was triggered but subsequently deoptimized due to violated assumptions causing it to fall back to interpreted mode repeatedly, the execution count hasn't yet reached the compilation threshold which defaults to triggering after thousands of invocations, or there's a compilation bailout occurring silently. To resolve this, first use `--engine.TraceCompilation` alongside CPUTracer to see if compilation is attempted and what the outcome is, then examine any bailout messages or deoptimization reasons, review the function for Truffle boundary issues that would prevent compilation, check for unstable types or object shapes that could cause repeated invalidation, and potentially adjust compilation thresholds or enable compilation exceptions to fail fast during development. Verification involves re-running CPUTracer after fixes and confirming that the compiled percentage increases above 95% for the hot paths.

### Insight: Execution Hotspot Identification

The total count and percentage fields directly identify your hottest execution paths, which should be your primary optimization targets. When sorted by total count, the top entries consuming 80% or more of total executions represent your critical path—even small performance improvements to these functions or statements yield significant overall gains. If you observe a few functions dominating with total counts in the millions while most code executes only hundreds of times, this concentrated execution pattern is ideal because optimization efforts can focus narrowly. Conversely, if execution is evenly distributed across hundreds of functions without clear hotspots, this diffuse pattern makes optimization harder and suggests the problem may be architectural rather than localized to specific functions. The resolution for hotspot scenarios involves focusing optimization effort on the highest-count elements, using statement-level tracing (`--cputracer.TraceStatements`) to understand which specific operations within hot functions execute most, considering whether hot code can be simplified or specialized further, and verifying that these hot functions are successfully compiled as described previously. For diffuse execution patterns, consider higher-level architectural changes rather than micro-optimizations, profile with time-based tools like CPUSampler to find slow operations that might not execute frequently but still matter, and use filtering to focus analysis on specific subsystems.

### Insight: Algorithmic Complexity Validation

CPUTracer excels at validating the actual runtime complexity of algorithms by revealing exact execution counts that can be compared against input sizes. When you trace statement-level executions within loops and recursive functions, you can measure how execution counts scale with input. For example, if you expect O(n) behavior, execution counts should scale linearly with input size; if you see quadratic or exponential scaling instead, that reveals an algorithmic problem. The symptom of unexpected complexity appears as execution counts that grow much faster than expected when you increase input size—for instance, doubling input size causing execution counts to quadruple suggests O(n²) rather than expected O(n). The root cause is typically hidden nested loops, recursive algorithms with poor base cases, or inefficient search and traversal patterns. Resolution requires examining the high-count statements to identify unexpected loops, analyzing the actual control flow revealed by the tracer to find where iterations exceed expectations, refactoring algorithms to reduce iteration counts, and adding early termination conditions or caching to avoid redundant executions. Verification involves running CPUTracer with varying input sizes and confirming that execution count growth matches the intended complexity class.

### Insight: Specialization and Splitting Analysis

When a single function name appears multiple times in the output with different locations, this reveals that the Truffle framework has created multiple specialized versions of the function through splitting or specialization, which is generally positive for performance. Each version handles specific type combinations or execution contexts, allowing more aggressive optimization. High compiled percentages across all specializations indicate successful optimization of each variant. However, if you observe dozens of specializations with low execution counts (below 1000), this suggests over-splitting where the framework is creating too many variants without sufficient execution to justify the compilation cost, leading to code cache pollution. The resolution involves reviewing DSL specialization annotations to potentially combine related cases, adjusting splitting limits using `--engine.Splitting=false` during diagnosis, and ensuring that common cases are properly handled by dominant specializations. The goal is having a small number of heavily-executed specializations rather than many lightly-executed ones.

### Correlation with Other Tools

CPUTracer provides execution count data that becomes most powerful when combined with complementary profiling tools. The most important correlation is with CPUSampler—the time-based sampling profiler—where you should analyze the ratio of execution time to execution count. Functions with high CPUTracer counts but low CPUSampler time are executing frequently but individually fast, indicating they're well-optimized; conversely, functions with low CPUTracer counts but high CPUSampler time are infrequent but slow operations that may need optimization despite low execution frequency. Use `--cpusampler` and `--cputracer` together to get this complete picture. Correlation with `--engine.TraceCompilation` reveals the relationship between compilation events and execution patterns—a function showing thousands of interpreted executions in CPUTracer should have corresponding compilation traces, and absence of compilation despite high counts indicates a problem. MemTracer (`--experimental-options --memtracer`) can be correlated to find allocation sites that execute frequently according to CPUTracer and also allocate heavily, pinpointing memory pressure hotspots. Finally, Compilation Statistics (`--engine.CompilationStatistics`) provides aggregate compilation metrics that explain the compiled versus interpreted split you observe in CPUTracer output—high invalidation counts in compilation statistics explain why CPUTracer shows high interpreted percentages on hot code.

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