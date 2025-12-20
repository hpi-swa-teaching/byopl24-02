# Tool: --engine.TraceCompilation

## 1. Command Execution

### Basic Command
```bash
<language-launcher> --experimental-options --engine.TraceCompilation <script>
```

```bash
<language-launcher> --experimental-options --engine.TraceCompilationDetails <script>
```
### Full Command Syntax
```bash
<language-launcher> --experimental-options --engine.TraceCompilation \
  [--engine.CompileOnly=<name-list>] \
  [--engine.CompileImmediately] \
  [--engine.BackgroundCompilation=<boolean>] \
  [--engine.FirstTierCompilationThreshold=<N>] \
  [--engine.LastTierCompilationThreshold=<N>] \
  [--jvm|--native] \
  <script>

# Example usage
js --experimental-options --engine.TraceCompilation script.js
js --experimental-options --engine.TraceCompilation --engine.TraceCompilationDetails app.js
```

### Execution Context

- **JVM Mode:** Yes
  - **Requirements:** Standard JVM with `--jvm` flag, available on GraalVM Community and Enterprise editions
  - **Command:** `<launcher> --jvm --engine.TraceCompilation <script>`
  
- **Native Image Mode:** Yes
  - **Requirements:** GraalVM Native Image executable (default mode in GraalVM distributions)
  - **Command:** `<launcher> --native --engine.TraceCompilation <script>`
  - **Note:** For deoptimization stack traces in native images, build with `-H:+IncludeNodeSourcePositions` (disabled by default to save image size)
  
- **Prerequisites:** `--experimental-options` flag required for most engine options

## 2. Command Options & Parameters

### Core Option

| Option                             | Type    | Default | Description                                                                                 |
| ---------------------------------- | ------- | ------- | ------------------------------------------------------------------------------------------- |
| `--engine.TraceCompilation`        | Boolean | `false` | Print information for compilation results with `[engine]` prefix to stdout                  |
| `--engine.TraceCompilationDetails` | Boolean | `false` | Print compilation queuing details (queued, unqueued, start, done events with queue metrics) |

### Compilation Control Options (for Testing)

| Option                                       | Type                 | Default        | Description                                                                 |
| -------------------------------------------- | -------------------- | -------------- | --------------------------------------------------------------------------- |
| `--engine.CompileOnly`                       | Comma-separated list | No restriction | Restrict compilation to specified method names (or exclude with '~' prefix) |
| `--engine.CompileImmediately`                | Boolean              | `false`        | Compile methods as soon as they are run (for testing)                       |
| `--engine.BackgroundCompilation`             | Boolean              | `true`         | Set to false for synchronous compilation (simplifies debugging)             |
| `--engine.Compilation`                       | Boolean              | `true`         | Enable or disable Truffle compilation entirely                              |
| `--engine.FirstTierCompilationThreshold=<N>` | Number               | 400            | Sets Tier 1 compilation threshold                                           |
| `--engine.LastTierCompilationThreshold=<N>`  | Number               | 10000          | Sets Tier 2 compilation threshold                                           |

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

## 4. Output Format & Structure

### Output Type

- **Console:** Standard output (stdout) with structured text format
- **Prefix:** All lines begin with `[engine]` marker
- **Format:** Pipe-separated fields for structured parsing

### Output Location

- **Default:** Standard output (stdout)
- **Cannot be redirected:** Output always goes to stdout; capture with shell redirection if needed

### Output Format Examples

#### Compilation Completion (opt done)

```
[engine] opt done id=244 EqualityConstraint.execute |Tier 1|Time 268( 220+47 )ms|AST 17|Inlined 0Y 2N|IR 238/ 437|CodeSize 1874|Timestamp 758868036671903|Src octane-deltablue.js:528
```

#### Extended Format (with engine and compilation IDs)

```
[engine] opt done engine=2 id=213 EqualityConstraint.execute |Tier 1|Time 14( 11+3 )ms|AST 31|Inlined 0Y 2N|IR 218/ 365|CodeSize 1386|Addr 0x782dd1fb9300|CompId 23519 |UTC 2025-07-08T08:25:20.339|Src octane-deltablue.js:528 0xb0b56c7b
```

#### Deoptimization Event

```
[engine] opt deopt EqualityConstraint.execute
```

#### Invalidation Event

```
[engine] opt inv. BinaryConstraint.output
```

#### Queue Events (with TraceCompilationDetails)

```
[engine] opt queued id=237 BinaryConstraint.output |Tier 1|Count/Thres 25/ 25|Queue: Size 1 Change +1 Load 0.06 Time 0us|Timestamp 758865671350686|Src octane-deltablue.js:416

[engine] opt start id=237 BinaryConstraint.output |Tier 1|Priority 25|Rate 0.000000|Queue: Size 0 Change +0 Load 0.06 Time 0us|Timestamp 758865708273384|Src octane-deltablue.js:416

[engine] opt unque. id=304 Date.prototype.valueOf |Tier 2|Count/Thres 80234/ 3125|Queue: Size 4 Change 0 Load 0.31 Time 0us|Timestamp 758899904132076|Src <builtin>:1|Reason Target inlined into only caller
```

### Output Fields/Metrics

#### Core Compilation Fields

| Field         | Description                                         | Unit                 | Interpretation                                                                                                 |
| ------------- | --------------------------------------------------- | -------------------- | -------------------------------------------------------------------------------------------------------------- |
| **engine**    | Unique identifier of the engine for the compilation | integer              | Multi-engine scenarios; distinguishes which engine context                                                     |
| **id**        | Unique identifier of the call target within engine  | integer              | Tracks specific call targets across events                                                                     |
| **Name**      | Method/function name being compiled                 | text                 | Identifies the guest-language method                                                                           |
| **Tier**      | Compilation tier                                    | "Tier 1" or "Tier 2" | Tier 1 = fast compilation; Tier 2 = full optimization                                                          |
| **Time**      | Compilation time breakdown                          | milliseconds         | Format: `Total(Truffle+Graal)ms` - first number is Truffle tier (partial evaluation), second is Graal compiler |
| **AST**       | Target's non-trivial Truffle node count             | integer              | Larger values indicate more complex methods                                                                    |
| **Inlined**   | Inlining statistics                                 | "XY ZN"              | X inlines succeeded (Y), Z remained as calls (N)                                                               |
| **IR**        | Graal IR node counts                                | "X / Y"              | X = nodes after partial evaluation; Y = nodes after full compilation                                           |
| **CodeSize**  | Generated machine code size                         | bytes                | Final native code size                                                                                         |
| **Addr**      | Memory address of installed code                    | hexadecimal          | Where compiled code resides in memory                                                                          |
| **CompId**    | VM-specific compilation ID                          | integer              | Matches IDs in deoptimization/code cache logs                                                                  |
| **Timestamp** | Event time from System.nanoTime()                   | nanoseconds          | High-precision event timing                                                                                    |
| **UTC**       | UTC timestamp                                       | ISO 8601             | Human-readable timestamp                                                                                       |
| **Src**       | Source location                                     | formatted string     | Abbreviated source section with hash code                                                                      |

#### Queue Event Fields (TraceCompilationDetails)

| Field             | Description                           | Unit                | Interpretation                                                                             |
| ----------------- | ------------------------------------- | ------------------- | ------------------------------------------------------------------------------------------ |
| **Count/Thres**   | Current count / Compilation threshold | "count / threshold" | Shows progress toward compilation threshold                                                |
| **Queue: Size**   | Number of compilations in queue       | integer             | Queue backlog; higher values indicate saturation                                           |
| **Queue: Change** | Queue size delta from this event      | integer             | +1 = added, -1 = removed, 0 = no change                                                    |
| **Queue: Load**   | Queue load metric                     | float               | 1.0 = normal; <1.0 = underloaded; >1.0 = overloaded (triggers dynamic threshold increases) |
| **Queue: Time**   | Event duration                        | microseconds        | Time spent in queue operation                                                              |
| **Priority**      | Compilation priority                  | integer             | Higher values compiled first                                                               |
| **Rate**          | Compilation rate metric               | float               | Internal scheduling metric                                                                 |
| **Reason**        | Event reason                          | text                | Runtime-reported reason for queue decision                                                 |

## 5. Insights & Analysis

### When should you use it?

TraceCompilation serves as an essential diagnostic tool during **initial language implementation** when Truffle language developers need to verify that compilation infrastructure functions correctly and that hot code paths are indeed being compiled. During **performance regression investigation**, it enables comparison of compilation patterns between different versions of a language implementation, revealing whether regressions stem from compilation failures, changed optimization decisions, or deoptimization introduction. For **optimization validation**, it confirms that expected optimizations—particularly inlining and specialization—occur as intended, with detailed statistics showing whether optimization efforts translate into compiler decisions. When **debugging deoptimizations**, it identifies the frequency and patterns of deoptimization, revealing whether performance degradation stems from repeated compilation-deoptimization cycles. Finally, for **compilation queue analysis**, particularly when combined with TraceCompilationDetails, it exposes compilation prioritization, queue saturation, and timing issues that affect warmup performance.

The tool is generally **not recommended for production** use due to output volume and slight overhead, though it can be enabled briefly for specific performance investigations. Using `--engine.CompileOnly` restricts output to specific methods, making short-term production diagnostics more feasible.

### Actionable Insights

#### Insight: Repeated deoptimization cycles

When a method repeatedly appears with "opt deopt" in the trace output, this symptom indicates a **deoptimization loop**—code compiles successfully but then deoptimizes during execution, potentially multiple times per second in severe cases. The root cause typically involves unstable type assumptions where runtime types differ from compilation assumptions, unguarded operations that trigger deoptimization when encountering unexpected values, or polymorphic behavior that violates specialization assumptions. This represents one of the most critical performance problems in Truffle applications as compilation overhead accumulates while execution never benefits from optimized code. To resolve this issue, first enable stack traces using `--engine.TraceTransferToInterpreter` for native images or `--vm.XX:+TraceDeoptimization` for JVM mode to identify the exact source location triggering deoptimization. Check for unstable type assumptions by reviewing guards in Truffle nodes and ensuring runtime behavior matches compilation-time assumptions. Review guest language code for polymorphic behavior—for instance, functions receiving different argument types on each invocation. Consider using `--engine.TraceAssumptions` to identify which specific assumptions are being invalidated. For verification, after implementing fixes, confirm that deoptimization frequency decreases or eliminates entirely by re-running with TraceCompilation and counting deopt events.

#### Insight: Hot methods never appearing in compilation output

When CPU profiling with `--cpusampler` shows methods consuming significant execution time but those methods never appear in TraceCompilation output, this symptom indicates **methods not reaching compilation threshold**. The root cause involves insufficient invocation counts (defaults: 400 for Tier 1, 10000 for Tier 2), methods executing only during interpreter warmup, or compilation being disabled entirely via configuration. To resolve this, first verify that `--engine.Compilation=true` (the default) to ensure compilation isn't disabled. Check call counts are sufficient by examining application structure—if methods execute fewer than 400 times, they won't compile at first tier. Use the profiler's tier information (`--cpusampler.ShowTiers=true`) to confirm methods show high T0 (interpreter) time. Consider lowering thresholds for testing purposes using `--engine.FirstTierCompilationThreshold=<value>` and `--engine.LastTierCompilationThreshold=<value>`, though production code should typically use defaults. For methods that should be hot but aren't being recognized as such, investigate whether they're being split into multiple call targets or whether loop counts aren't being accumulated properly. Verify resolution by confirming compilation events appear after threshold adjustments and that profiler T0 time decreases.

#### Insight: Excessive compilation times

When the Time field shows compilation times exceeding 100-500ms for individual methods, this symptom indicates **compilation complexity issues** stemming from large ASTs, excessive inlining, or complex control flow. The root cause typically involves methods with hundreds of Truffle nodes generating large compiler IR graphs, excessive inlining that pulls many callees into a single compilation unit, or algorithmic complexity in the compiler itself when analyzing certain code patterns. Long compilation times directly impact application warmup, increase memory pressure, and may indicate the compiler is working harder than necessary. To resolve this, first review the AST and IR node counts in the output—values exceeding thousands indicate high complexity. Check the "Inlined" field statistics; excessive Y (successful inline) counts suggest over-aggressive inlining. Use `--engine.TraceInlining` to understand inlining decisions in detail, identifying which callees contribute most to compilation unit size. Consider simplifying guest language constructs that generate complex ASTs, potentially splitting large methods in the language implementation. May need to adjust `--engine.MaximumGraalGraphSize` if compilations are hitting size limits. For particularly complex methods, evaluate whether compilation is actually beneficial or if interpreter performance would be acceptable. Verify resolution by confirming compilation times decrease while CodeSize and performance remain reasonable.

#### Insight: Failed inlining indicated by high "N" counts

When the Inlined field shows many "N" (not inlined) decisions such as "2Y 15N", this symptom indicates **inlining budget exhaustion or unresolved virtual calls**. The root cause involves the compiler refusing to inline calls due to size budgets, polymorphic call sites where target types can't be determined at compilation time, or recursive call patterns that exceed inlining depth limits. Failed inlining prevents the compiler from optimizing across call boundaries and increases call overhead. To resolve this, use `--engine.TraceInlining` for detailed inlining information showing exactly why each decision was made. Check for polymorphism using `--engine.TraceCompilationPolymorphism` to identify generic nodes that indicate type instability. Review whether failed inlines are monomorphic (single target, failed due to budget) or polymorphic (multiple targets, failed due to type uncertainty). For monomorphic cases, the inlining budget may be appropriate—not all calls should inline. For polymorphic cases, may indicate need for monomorphization through call target splitting (`--engine.TraceSplitting`). Review node specialization in the guest language implementation to ensure types resolve correctly. Verify resolution by observing increased Y counts and checking that performance improves with better inlining decisions.

#### Insight: Tier 1 compilations without Tier 2 progression

When methods show numerous Tier 1 compilations but never progress to Tier 2, this symptom indicates **methods warm but not hot enough for full optimization** or **compilation queue overload** preventing Tier 2 compilation. The root cause typically involves method invocation counts exceeding the Tier 1 threshold (400) but not reaching the Tier 2 threshold (10000), methods becoming "cold" after Tier 1 compilation as the application shifts to other code paths, or the compilation queue being saturated with Tier 1 compilations preventing Tier 2 work. To resolve this, check compilation thresholds and verify they're appropriate for the application's execution pattern. Use `--engine.TraceCompilationDetails` to examine queue state—if Queue Load consistently exceeds 1.0, the queue is saturated and Tier 2 compilations are being delayed or prevented. For applications with changing hotspot patterns, lack of Tier 2 compilation may be intentional and correct—methods that stop being hot after Tier 1 compilation don't need further optimization. If Tier 2 compilation is desired but not occurring, consider increasing compiler threads with `--engine.CompilerThreads=<N>` to reduce queue pressure. Verify resolution by confirming appropriate methods reach Tier 2 and that queue load remains manageable.

#### Insight: Queue overload indicated by high load metrics

When TraceCompilationDetails shows Queue Load values consistently above 1.0, this symptom indicates **compilation queue saturation** where compilation demand exceeds capacity. The root cause involves too many methods reaching compilation threshold simultaneously during warmup, insufficient compiler threads for the compilation workload, or individual compilations taking too long and backing up the queue. Queue overload causes increased warmup time, delayed compilation of hot methods, and automatic threshold increases that may prevent important methods from ever compiling. To resolve this, increase compiler threads using `--engine.CompilerThreads=<N>` to provide more parallel compilation capacity. Investigate whether any individual compilations show excessive Time values; addressing long compilation times (see previous insight) reduces queue pressure. Consider adjusting warmup characteristics—if possible, spread method invocations more evenly rather than having all methods reach threshold simultaneously. Use `--engine.CompilationStatistics` to understand overall compilation patterns and identify whether saturation is temporary (during initial warmup) or persistent. Verify resolution by confirming Queue Load stays near or below 1.0 and that important methods compile in a timely manner.

### Correlation with Other Tools

**CPU Profiler (--cpusampler)** provides the essential complement to TraceCompilation. The workflow begins with using the profiler to identify hot methods consuming execution time, then verifying those methods appear in TraceCompilation output to confirm they're being compiled. Methods spending significant time in the interpreter (high T0 percentage in profiler output) despite being hot indicate compilation issues that TraceCompilation can diagnose. The combined command `<launcher> --experimental-options --engine.TraceCompilation --engine.TraceCompilationDetails --cpusampler --cpusampler.Delay=5000 <script>` correlates compilation events with performance impact.

**Ideal Graph Visualizer (IGV)** shows the complementary view where TraceCompilation reveals *what* is compiled while IGV shows *how* it's compiled through graph structure visualization. Use `--vm.Djdk.graal.Dump=Truffle:1` with TraceCompilation to dump compilation graphs to IGV. Match compilation IDs (CompId field) between TraceCompilation output and IGV dumps to correlate specific compilation events with their graph representations. This enables deep understanding of optimization decisions, inlining outcomes, and partial evaluation effectiveness.

**TraceInlining** provides detailed expansion of the aggregate Inlined statistics shown in TraceCompilation. While TraceCompilation shows overall statistics like "5Y 12N", TraceInlining shows which specific call sites succeeded or failed to inline and why. Combine both to first identify compilations with poor inlining statistics, then use TraceInlining to understand the root causes.

**TraceAssumptions** explains the *why* behind invalidation events. TraceCompilation shows that invalidations occurred, but TraceAssumptions reveals which specific assumptions were violated—crucial for understanding instability. Use both together when debugging invalidation storms.

**Native Image Build** has a special relationship with runtime TraceCompilation. Some Truffle compilation can occur at native image build time through ahead-of-time (AOT) compilation. Runtime TraceCompilation shows additional just-in-time (JIT) compilations that occur during execution. Combining with Auxiliary Engine Caching reduces runtime compilation by pre-compiling common call targets, changing the profile of what appears in TraceCompilation output during warmup.

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
