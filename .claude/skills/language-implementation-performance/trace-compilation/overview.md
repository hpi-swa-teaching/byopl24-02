# Overview

# Tool: --engine.TraceCompilation


## 3. Functional Description

### Primary Purpose

The `--engine.TraceCompilation` tool is designed for Truffle language implementers to observe and debug the Truffle compilation process, providing visibility into when methods are compiled, which compilation tier (first-tier vs last-tier in multi-tier compilation), compilation performance metrics, deoptimization events, and call target invalidations.

### What the Tool Does

The tool traces compilation events for **Truffle CallTargets**—the fundamental compiled units in the Truffle framework where each CallTarget represents a guest-language method or function that has reached the compilation threshold. At compilation completion, the tool prints structured output to stdout prefixed with `[engine]`, displaying the target name, compilation tier, timing breakdown (time in Truffle tier vs Graal compiler), node counts, inlining statistics, code size, and source location. Beyond successful compilations, it tracks deoptimization events (when compiled code returns to the interpreter) and invalidation events (when compiled code is thrown away due to assumption violations). When combined with `--engine.TraceCompilationDetails`, it additionally logs queue events showing when compilation tasks are queued, started, unqueued, and completed, along with queue size, load metrics, and prioritization information.

### How It Works

The tool integrates directly with the **Truffle compilation infrastructure** and **Graal compiler**. The technical mechanism operates as follows: guest code initially executes in the interpreter while call and loop counts are tracked per CallTarget. When a target reaches the compilation threshold (default: 400 invocations for first tier, 10000 for last tier), a compilation task is queued. Compiler threads process the queue, performing **partial evaluation** to specialize the Truffle AST based on runtime profiling, making **inlining decisions** for guest-language calls, executing **node specialization** based on observed types, and integrating with the **Graal compiler** for backend optimizations including standard compiler passes and machine code generation. TraceCompilation hooks into these stages to emit diagnostic output at key events—primarily at compilation completion but also at queue manipulation points when TraceCompilationDetails is enabled.

GraalVM uses **multi-tier compilation** by default (`--engine.MultiTier=true`). First tier (Tier 1) provides fast compilation with basic optimizations at a lower compilation threshold, getting code compiled quickly. Last tier (Tier 2) applies full optimizations including aggressive partial evaluation, producing higher quality code for hot methods. This tiered approach balances startup performance (get code compiled quickly) with peak performance (optimize truly hot code heavily).

### When to Use

Language developers should use TraceCompilation during development for multiple critical scenarios. First, **understanding compilation behavior** by observing which methods are being compiled and when, establishing whether the compilation system responds appropriately to application hotspots. Second, **debugging performance issues** to identify methods that should be compiled but aren't, investigating why certain code paths remain interpreted despite high execution frequency. Third, **detecting deoptimizations** to find code that repeatedly deoptimizes—a critical performance killer where compiled code keeps reverting to interpreter mode. Fourth, **analyzing compilation time** to understand compilation overhead, identify methods with unexpectedly long compilation times, and observe compilation queue behavior under load. Fifth, **validating optimizations** to verify that expected inlining occurs, that node specialization proceeds correctly, and that partial evaluation produces expected results. Sixth, during **development and testing** phases of language implementation to ensure proper compilation infrastructure integration. Finally, for **performance tuning** by correlating TraceCompilation output with profiling data from tools like CPUSampler to optimize hot paths effectively.

## 6. Additional Notes

### Overhead

The runtime overhead of TraceCompilation is **low** when compilation is infrequent during steady-state execution, becoming **moderate** during warmup phases with many concurrent compilations. The tool introduces no measurable impact on compilation decisions or quality; overhead stems primarily from string formatting for output generation and I/O operations for writing to stdout. With large applications generating hundreds or thousands of compilation events during warmup, output volume becomes the primary concern rather than runtime performance impact. The tool is **not typically used in production** because of output volume rather than performance impact, though brief production use for specific investigation is feasible when combined with `--engine.CompileOnly` to limit output.

### Limitations

TraceCompilation provides **no built-in method filtering** beyond the `--engine.CompileOnly` option, making it challenging to focus on specific methods without external filtering of stdout. The **output format is plain text** rather than structured machine-readable format like JSON, requiring custom parsing for automated analysis. **Deoptimization details are minimal**—the tool shows that deoptimization occurred but not why; use `--engine.TraceTransferToInterpreter` or `--vm.XX:+TraceDeoptimization` for stack traces. There's **no historical data or built-in storage**; output is real-time only without built-in analysis or aggregation. **Native Image considerations** include requiring special build flags like `-H:+IncludeNodeSourcePositions` for full diagnostic information, which increases image size. Finally, **output format evolution** across GraalVM versions means that field presence and formatting may differ between major releases, requiring version-specific parsing logic for automated tools.

### Best Practices

For development workflows, start with basic `--engine.TraceCompilation` to obtain an overview of compilation behavior before adding more verbose options. Add `--engine.TraceCompilationDetails` specifically when investigating compilation queue issues, as the additional verbosity provides little value unless queue behavior is under investigation. Use `--engine.CompileOnly=<methodname>` to focus on specific methods of interest, dramatically reducing output volume and making patterns easier to identify. Combine with `--engine.BackgroundCompilation=false` during testing to produce deterministic output where compilation completes before execution continues, simplifying correlation between events. Always pair with profiling tools to correlate performance issues with compilation behavior—understanding *what* is slow (from profiler) and *why* it's slow (from compilation trace) together provides complete diagnostic information.

For debugging deoptimizations specifically, use `--engine.TraceCompilation` to identify deoptimization frequency and which methods are affected. Add the native image build flag `-H:+IncludeNodeSourcePositions` during image creation to enable detailed diagnostics. Enable `--engine.TraceTransferToInterpreter` for stack traces showing exactly where deoptimization occurs. Use `--engine.TraceAssumptions` to understand which assumptions are being invalidated. Finally, examine with `--vm.Djdk.graal.Dump=Truffle:1` in IGV for graph-level understanding of what the compiler attempted before deoptimization occurred.

For performance analysis in general, the recommended combination brings multiple tools together: `<launcher> --experimental-options --engine.TraceCompilation --engine.TraceCompilationDetails --cpusampler --cpusampler.Delay=5000 <script>` provides compilation events, queue metrics, and performance profiling simultaneously. For testing language implementation during development, force compilation decisions to be deterministic: `<launcher> --experimental-options --engine.TraceCompilation --engine.CompileImmediately --engine.BackgroundCompilation=false --engine.CompileOnly=<target-method> <script>` ensures the target method compiles immediately and synchronously, making test output reproducible.

### Common Pitfalls

**Overwhelming output volume** represents the most common pitfall—large applications generate massive trace output during warmup, making it difficult to identify relevant information. Filter with `--engine.CompileOnly` or use shell tools to grep for specific methods. **Timing confusion** causes misinterpretation when developers confuse compilation time (shown in Time field) with execution time; these are entirely different metrics. **Deoptimization interpretation errors** occur when developers react to single deoptimization events, but single deoptimizations may not indicate problems—repeated deoptimizations of the same method are the critical issue. **Version compatibility** problems arise when parsing output programmatically across GraalVM versions; the format evolves and fields may appear or disappear between major releases. **Native Image timing peculiarities** mean timestamps in native images may use different epoch references than HotSpot, complicating absolute time comparisons. Finally, **queue load metric misunderstanding** causes confusion about what values indicate problems; load greater than 1.0 indicates queue saturation triggering dynamic threshold increases, directly impacting what gets compiled.

### Related Tools and Options

**Compilation control options** adjust compiler behavior for testing: `--engine.CompilerThreads=<N>` controls compilation parallelism, `--engine.MultiTier=true|false` enables or disables multi-tier compilation, `--engine.OSR=true|false` controls on-stack-replacement compilation for long-running loops, and `--engine.Inlining=true|false` globally controls inlining.

**Additional tracing options** provide complementary diagnostics: `--engine.TraceSplitting` prints call target splitting decisions used for monomorphization, `--engine.TracePerformanceWarnings` prints potential performance problems detected by the runtime, `--engine.SpecializationStatistics` prints DSL specialization statistics (requires language rebuild with statistics enabled), and `--engine.CompilationStatistics` prints summary statistics at end of execution.

**Compiler configuration options** adjust thresholds and policies: `--engine.FirstTierCompilationThreshold=<N>` sets Tier 1 threshold (default: 400), `--engine.LastTierCompilationThreshold=<N>` sets Tier 2 threshold (default: 10000), and `--engine.Mode=latency|throughput` optimizes for latency (faster compilation, lower thresholds) versus throughput (better code, higher thresholds).

**Graph dumping options** enable visualization: `--vm.Djdk.graal.Dump=Truffle:1` dumps compilation graphs to IGV (Ideal Graph Visualizer), `--vm.Djdk.graal.PrintGraph=Network` sends dumps to IGV over network connection, and `--vm.Djdk.graal.Dump=:3` dumps for C1 Visualizer showing LIR and assembly levels.

### Integration with Build Systems

For Maven projects, pass options through configuration:

```xml
<configuration>
  <jvmArgs>
    <arg>--experimental-options</arg>
    <arg>--engine.TraceCompilation</arg>
  </jvmArgs>
</configuration>
```

For Gradle projects, specify in jvmArgs:

```groovy
jvmArgs = [
  '--experimental-options',
  '--engine.TraceCompilation'
]
```

### Native Image Specific Considerations

Native Image requires special build-time configuration for full diagnostic capabilities. To enable deoptimization stack traces in native images, build with `native-image -H:+IncludeNodeSourcePositions <other-options>`. Note that this flag increases image size as it includes source position metadata in the compiled image. Compilation IDs (CompId field) remain consistent across HotSpot and Native Image modes, enabling cross-mode comparison. Auxiliary Engine Caching can pre-compile common call targets during image build, reducing the number of runtime compilations visible in TraceCompilation output and improving warmup performance. Native Image uses a different timestamp mechanism than HotSpot, so absolute timestamp values may differ in interpretation but relative timing remains meaningful.

## 7. Resources

### Official GraalVM Documentation

1. **Optimizing Truffle Interpreters (Primary Source)**
   - https://www.graalvm.org/latest/graalvm-as-a-platform/language-implementation-framework/Optimizing/
   - Complete documentation of TraceCompilation including output format, field descriptions, and usage examples

2. **Optimizing Truffle Interpreters (Oracle Documentation - JDK 17)**
   - https://docs.oracle.com/en/graalvm/jdk/17/docs/graalvm-as-a-platform/language-implementation-framework/Optimizing/
   - Oracle's official documentation with additional enterprise context

3. **Truffle Options Documentation**
   - https://www.graalvm.org/latest/graalvm-as-a-platform/language-implementation-framework/Options/
   - Complete list of engine options with types, defaults, and descriptions

4. **Truffle Options (Oracle Documentation - JDK 17)**
   - https://docs.oracle.com/en/graalvm/jdk/17/docs/graalvm-as-a-platform/language-implementation-framework/Options/
   - Oracle's official options reference

5. **Optimizing Guide (GraalVM 22.0)**
   - https://www.graalvm.org/22.0/graalvm-as-a-platform/language-implementation-framework/Optimizing/
   - Historical version showing output format evolution

6. **Optimizing Guide (GraalVM 22.1)**
   - https://www.graalvm.org/22.1/graalvm-as-a-platform/language-implementation-framework/Optimizing/
   - Additional version for format comparison

7. **Truffle Language Implementation Framework**
   - https://www.graalvm.org/latest/graalvm-as-a-platform/language-implementation-framework/
   - Overview of Truffle architecture and development context

8. **Auxiliary Engine Caching**
   - https://www.graalvm.org/latest/graalvm-as-a-platform/language-implementation-framework/AuxiliaryEngineCachingEnterprise/
   - TraceCompilation usage in context of engine caching for improved warmup

9. **Truffle AOT Overview**
   - https://www.graalvm.org/latest/graalvm-as-a-platform/language-implementation-framework/AOTOverview/
   - Context for compilation in ahead-of-time scenarios

10. **Truffle Approach to the Compilation Queue**
    - https://www.graalvm.org/latest/graalvm-as-a-platform/language-implementation-framework/TraversingCompilationQueue/
    - Understanding queue metrics in TraceCompilationDetails output

11. **GraalVM 24 Documentation**
    - https://www.graalvm.org/jdk24/graalvm-as-a-platform/language-implementation-framework/Optimizing/
    - Latest JDK 24 based documentation version

### GitHub oracle/graal Repository

12. **Optimizing.md (Master Branch)**
    - https://github.com/oracle/graal/blob/master/truffle/docs/Optimizing.md
    - Source markdown for official documentation, canonical reference

13. **SDK CHANGELOG.md**
    - https://github.com/oracle/graal/blob/master/sdk/CHANGELOG.md
    - Version history including CompId field addition (issue GR-61448)

14. **oracle/graal Repository Main**
    - https://github.com/oracle/graal
    - Repository structure, build instructions, and source code

### Oracle Enterprise Documentation

15. **Options (GraalVM Enterprise 20)**
    - https://docs.oracle.com/en/graalvm/enterprise/20/docs/graalvm-as-a-platform/language-implementation-framework/Options/
    - Enterprise edition options documentation with additional features

16. **Optimizing (GraalVM Enterprise 21)**
    - https://docs.oracle.com/en/graalvm/enterprise/21/docs/graalvm-as-a-platform/language-implementation-framework/Optimizing/
    - Enterprise edition with TraceCompilationDetails enhancements
