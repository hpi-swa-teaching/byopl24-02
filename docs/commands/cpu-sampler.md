---
name: CPU Sampler
description: Time-based sampling profiler showing WHERE execution time is spent (wall-clock time, not frequency). Provides histogram with self/total time, compilation tiers (T0/T1/T2), and flame graphs. Use as FIRST step to identify hot functions consuming most time. Low overhead, suitable for longer runs. Pair with cpu-tracer to understand time-per-execution vs execution frequency.
---
# Tool: --cpusampler

## 1. Command Execution

### Basic Command
```bash
<language-launcher> --cpusampler [options] script.ext
```

### Full Command Syntax
```bash
<language-launcher> --cpusampler[=<output-format>] \
  [--cpusampler.Delay=<ms>] \
  [--cpusampler.Period=<ms>] \
  [--cpusampler.StackLimit=<integer>] \
  [--cpusampler.Output=histogram|calltree|json|flamegraph] \
  [--cpusampler.OutputFile=<path>] \
  [--cpusampler.FilterFile=<filter>] \
  [--cpusampler.FilterLanguage=<languageId>] \
  [--cpusampler.FilterMimeType=<mime-type>] \
  [--cpusampler.FilterRootName=<filter>] \
  [--cpusampler.MinSamples=<integer>] \
  [--cpusampler.GatherHitTimes=<boolean>] \
  [--cpusampler.GatherAsyncStackTrace=<boolean>] \
  [--cpusampler.SampleInternal=<boolean>] \
  [--cpusampler.SampleContextInitialization=<boolean>] \
  [--cpusampler.ShowTiers=<boolean>|0,1,2] \
  [--cpusampler.SummariseThreads=<boolean>] \
  [--cpusampler.Mode=roots|statements] \
  <script>

# Example usage
js --cpusampler --cpusampler.Delay=5000 --cpusampler.ShowTiers=true app.js
js --cpusampler=flamegraph --cpusampler.OutputFile=profile.svg app.js
```

### Execution Context

- **JVM Mode:** Yes
  - **Requirements:** Standard GraalVM distribution, works with all Truffle languages (JavaScript, Python, Ruby, R)
  - **Compatibility:** Can be used alongside JVM profiling tools (VisualVM, Java Flight Recorder, Oracle Developer Studio)
  
- **Native Image Mode:** Designed primarily for JVM/language execution contexts
  - **Alternative Profiling:** For Native Image binaries, use system profilers: callgrind (Valgrind), strace, perf, Oracle Developer Studio Performance Analyzer
  
- **Prerequisites:** GraalVM installation with Truffle language support

## 2. Command Options & Parameters

### Key Options

| Option                    | Type                                    | Default     | Description                                                                       |
| ------------------------- | --------------------------------------- | ----------- | --------------------------------------------------------------------------------- |
| `--cpusampler`            | `true\|false\|<Output>`                 | `false`     | Enable CPU sampler or enable with specific output format                          |
| `--cpusampler.Delay`      | `<ms>` (Long)                           | `0`         | Delay sampling for specified milliseconds (useful to skip warmup phase)           |
| `--cpusampler.Period`     | `<ms>` (Long)                           | `10`        | Sampling period - time between stack samples in milliseconds                      |
| `--cpusampler.StackLimit` | `[1, inf)` (Integer)                    | `10000`     | Maximum number of stack frames to sample                                          |
| `--cpusampler.Output`     | `histogram\|calltree\|json\|flamegraph` | `histogram` | Output format specification                                                       |
| `--cpusampler.OutputFile` | `<path>` (String)                       | stdout      | File path to save output (default: stdout; flamegraph defaults to flamegraph.svg) |
| `--cpusampler.MinSamples` | `[0, inf)` (Integer)                    | `0`         | Remove elements from output with fewer samples than threshold                     |

### Advanced Options

| Option                                     | Type                    | Default             | Description                                                            |
| ------------------------------------------ | ----------------------- | ------------------- | ---------------------------------------------------------------------- |
| `--cpusampler.FilterFile`                  | `<filter>` (Expression) | no filter           | Wildcard filter for source file paths (e.g., `*program*.sl`)           |
| `--cpusampler.FilterLanguage`              | `<languageId>` (String) | profile all         | Only profile specified language ID (e.g., `js`)                        |
| `--cpusampler.FilterMimeType`              | `<mime-type>` (String)  | profile all         | Only profile language with specified MIME type                         |
| `--cpusampler.FilterRootName`              | `<filter>` (Expression) | no filter           | Wildcard filter for program roots/functions (e.g., `Math.*`)           |
| `--cpusampler.GatherHitTimes`              | boolean                 | not set             | Save timestamp for each taken sample                                   |
| `--cpusampler.GatherAsyncStackTrace`       | `true\|false`           | `true`              | Gather async stack trace elements; disabling reduces overhead          |
| `--cpusampler.SampleInternal`              | boolean                 | `false`             | Profile internal sources (standard library functions)                  |
| `--cpusampler.SampleContextInitialization` | boolean                 | `false`             | Enable sampling during context initialization                          |
| `--cpusampler.ShowTiers`                   | `true\|false\|0,1,2`    | `false`             | Show compilation tier information (T0=interpreter, T1=tier1, T2=tier2) |
| `--cpusampler.SummariseThreads`            | boolean                 | `false`             | Print output as summary of all per-thread profiles                     |
| `--cpusampler.Mode`                        | `roots\|statements`     | default (not roots) | Sample mode: `roots` includes inlined functions (higher overhead)      |

## 3. Functional Description

### Primary Purpose

The CPU Sampler identifies performance bottlenecks in Truffle-based applications by determining where execution time is spent, enabling language developers to focus optimization efforts on critical code paths that consume the majority of runtime.

### What the Tool Does

The CPU Sampler performs **periodic stack sampling** across all application threads, capturing call stacks at regular intervals (default: every 10 milliseconds). It aggregates these samples into tree structures of ProfilerNode objects that represent the execution profile, calculating statistical approximations of time spent in each function based on sample frequency. The tool generates multi-format output (histogram, call tree, JSON, or flamegraph) that reveals both self-time (time spent directly in a function) and total-time (time spent in a function including its callees). Optionally, it tracks JIT compilation tier information to show whether code executes in the interpreter or compiled form.

### How It Works

The CPU Sampler underwent a **major architectural redesign in GraalVM 21.3.0**, transitioning from an instrumentation-based approach to a **safepoint-based mechanism** built on top of Truffle safepoints. Prior to 21.3.0, the sampler maintained a shadow stack using the Truffle instrumentation framework, manipulating an additional stack data structure on every function call and exit, which introduced significant overhead. The new implementation uses the TruffleSafepoint API introduced in GraalVM 21.1.0, which allows thread-local actions to be executed at safe interruption points. When the sampling timer expires (at the configured period), the safepoint mechanism triggers on all threads, stack frames are captured via the `iterateFrames()` API, and the stack trace is copied to the ProfilerNode tree structure with hit counts incremented for sampled nodes. This eliminates per-call overhead and **greatly reduces CPU and memory consumption** compared to the pre-21.3.0 instrumentation approach.

### When to Use

Language developers should use the CPU Sampler during the **development phase** for several critical use cases. First, for **initial performance investigation** to quickly understand where time is spent in guest language code. Second, for **compilation effectiveness analysis** by combining the sampler with `--cpusampler.ShowTiers` to verify which functions are compiled and at what optimization level (interpreter T0, tier-1 T1, or tier-2 T2). Third, for **deoptimization detection**, as functions spending significant time in interpreter mode (T0) indicate potential deoptimization issues requiring investigation. Fourth, for **algorithm comparison** to validate that optimizations target actual hotspots and that alternative implementations deliver expected performance improvements. Fifth, for **warmup analysis** using `--cpusampler.Delay` to profile post-warmup steady-state performance and identify functions that remain uncompiled. Finally, for **language implementation testing** to profile interpreter and compiler effectiveness during Truffle language development, including seamless profiling across language boundaries in polyglot applications.

## 4. Output Format & Structure

### Output Type

- **Console:** histogram and calltree formats default to stdout
- **File:** JSON and flamegraph output to files
- **Graphical:** Flamegraph generates interactive SVG visualization
- **Binary:** JSON format provides machine-readable structured data

### Output Location

| Format     | Default Location                     |
| ---------- | ------------------------------------ |
| histogram  | stdout                               |
| calltree   | stdout                               |
| json       | stdout                               |
| flamegraph | `flamegraph.svg` (current directory) |

Override any location with `--cpusampler.OutputFile=<path>`

### Output Format Examples

#### Histogram Format (Default)

```
----------------------------------------------------------------------------------------------
Sampling Histogram. Recorded 250 samples with period 10ms.
Self Time: Time spent on the top of the stack.
Total Time: Time spent somewhere on the stack.
----------------------------------------------------------------------------------------------
Thread[main,5,main]
Name             || Total Time        || Self Time         || Location
----------------------------------------------------------------------------------------------
accept           || 2150ms 86.0%      || 2150ms 86.0%      || primes.js~13-22:191-419
next             || 2470ms 98.8%      ||  320ms 12.8%      || primes.js~31-37:537-737
:program         || 2500ms 100.0%     ||   30ms  1.2%      || primes.js~1-46:0-982
----------------------------------------------------------------------------------------------
```

#### Call Tree Format

```
:program (100% total, 1.2% self)
├─ findPrime (95.7% total, 7.5% self)
│  └─ isPrime (88.2% total, 88.2% self)
└─ next (4.3% total, 4.3% self)
```

#### Tier Information Format (with --cpusampler.ShowTiers)

```
-----------------------------------------------------------------------------------------------------------------------------------------------------------
Sampling Histogram. Recorded 553 samples with period 10ms.
T0: Percent of time spent in interpreter.
T1: Percent of time spent in code compiled by tier 1 compiler.
T2: Percent of time spent in code compiled by tier 2 compiler.
-----------------------------------------------------------------------------------------------------------------------------------------------------------
Thread[main,5,main]
Name      || Total Time | T0    | T1    | T2    || Self Time | T0    | T1    | T2    || Location
-----------------------------------------------------------------------------------------------------------------------------------------------------------
accept    || 4860ms 87.9% | 31.1% | 18.3% | 50.6% || 4860ms 87.9% | 31.1% | 18.3% | 50.6% || primes.js~13-22:191-419
:program  || 5530ms 100.0% | 100.0%| 0.0%  | 0.0%  || 360ms 6.5%   | 100.0%| 0.0%  | 0.0%  || primes.js~1-46:0-982
-----------------------------------------------------------------------------------------------------------------------------------------------------------
```

### Output Fields/Metrics

| Field          | Description                                  | Unit                     | Interpretation                                                       |
| -------------- | -------------------------------------------- | ------------------------ | -------------------------------------------------------------------- |
| **Name**       | Function/root name from source code          | text                     | Identifies the profiled function or method                           |
| **Total Time** | Time function appears anywhere on call stack | milliseconds, percentage | Includes time in callees; high values indicate overall contribution  |
| **Self Time**  | Time function is at top of stack             | milliseconds, percentage | Excludes callees; high values indicate direct computation bottleneck |
| **Location**   | Source file, line range, character positions | formatted string         | Links profile data to source code for investigation                  |
| **T0**         | Percentage of time in interpreter            | percentage               | High values indicate lack of compilation or deoptimization           |
| **T1**         | Percentage of time in tier-1 compiled code   | percentage               | Fast-compiled code with basic optimizations                          |
| **T2**         | Percentage of time in tier-2 compiled code   | percentage               | Fully optimized code; high values indicate successful optimization   |

## 5. Insights & Analysis

### When should you use it?

Language developers should reach for the CPU Sampler as the **first step in any performance investigation** to establish baseline understanding of execution time distribution. The tool proves particularly valuable during **post-development profiling** when validating that optimizations target actual hotspots rather than assumed bottlenecks. During **continuous performance monitoring**, it serves as a regression detection mechanism throughout the development cycle. When **verifying compilation effectiveness**, combining the sampler with `--cpusampler.ShowTiers` confirms that critical code paths are indeed compiled at appropriate optimization levels. For **polyglot applications**, it uniquely reveals cross-language performance characteristics without requiring separate profiling infrastructure for each language. Finally, it provides **quick feedback during Truffle language implementation**, helping language developers understand interpreter and compiler effectiveness as they iterate on language design.

The official GraalVM documentation specifically recommends using a sampling delay: "You probably want to use a sampling delay with --cpusampler.Delay=MILLISECONDS to only start profiling after warmup. That way, you can easily identify which functions get compiled and which do not and yet take a significant amount of time to execute."

### Actionable Insights

#### Insight: High self-time concentration in single function

When the output reveals one function showing 80% or greater self-time, this symptom indicates **algorithm inefficiency or excessive computation** concentrated in a single hotspot. The root cause typically stems from an algorithmically suboptimal implementation, unnecessary repeated calculations, or computationally expensive operations executed in tight loops. To resolve this issue, developers should first optimize the algorithm itself, considering more efficient data structures or algorithmic approaches. Next, use call tree mode (`--cpusampler.Output=calltree`) to identify which specific call sites contribute most to the problem. Finally, consider caching or memoization strategies for repeated calculations. As documented in the GraalVM profiling examples, when the `isPrime` function showed 88.2% self-time in the primes benchmark, algorithm redesign reduced execution time from hundreds of milliseconds to approximately 60ms. For verification, use `--cputracer` to identify which specific statements within the hot function execute most frequently, pinpointing the exact optimization target.

#### Insight: High interpreter time (T0) in hot functions

When a function displays greater than 30% T0 time while also showing significant total time in the profile, this symptom signals **deoptimization or compilation failure**. The root cause typically involves polymorphism causing repeated deoptimizations, unstable type assumptions, or Truffle AST nodes with incorrect specialization assumptions. As the documentation states, "Methods that are listed to run mostly in the interpreter likely have a problem with deoptimization." To resolve this, first check for polymorphic call sites causing deoptimizations by examining the call patterns and argument types. Next, review assumptions in Truffle AST node implementations to ensure they match runtime behavior. Then use `--engine.TraceCompilation` to diagnose compilation issues and identify deoptimization events. Finally, consult the Optimizing guide at https://github.com/oracle/graal/blob/master/truffle/docs/Optimizing.md for detailed guidance on addressing compilation problems. Verify the fix by re-running with `--cpusampler.ShowTiers=true` after implementing changes to confirm T0 time has decreased.

#### Insight: Time fragmented across many small functions

When no single function dominates the profile and instead shows many functions each consuming 1-5% of execution time, this symptom indicates **excessive call overhead or lack of inlining**. The root cause stems from either failure to inline guest-language calls that should be inlined, or genuine architectural issues with too many small function calls. To resolve this, first use `--cpusampler.Mode=roots` to reveal inlined functions and understand the complete call pattern. Check compilation logs for inlining decisions using `--engine.TraceInlining` to understand why expected inlines didn't occur. Consider adjusting Truffle compilation thresholds if appropriate, though this should be done cautiously. Use flamegraph output for visual call pattern analysis, which makes architectural issues more apparent. For verification, combine with `--engine.TraceCompilation` to ensure hot paths are being compiled and compare inlining statistics before and after optimization efforts.

#### Insight: Significant time invisible in application code

When the profile shows no clear hotspots in application code yet the application runs slowly, this symptom indicates that **standard library or internal sources are consuming time** but aren't being sampled. By default, the CPU Sampler excludes internal sources to focus on application code. To resolve this, enable `--cpusampler.SampleInternal=true` to include standard library functions in the profile. This often reveals that built-in functions or language runtime operations are the actual bottlenecks. Once identified, consider whether the application is using library functions inefficiently or whether there are opportunities to optimize the library implementations themselves. For language developers, this may indicate that certain standard library functions need specialization or better compilation support. Verify by comparing profiles with and without `SampleInternal` enabled to quantify internal vs application time consumption.

#### Insight: Call-site-dependent performance variations

When call tree output reveals the same function performing differently when invoked from different callers, this symptom indicates **context-dependent optimization or polymorphism**. The root cause typically involves different argument types at each call site, leading to different specialization paths, or polymorphic inline caches with different optimization outcomes. To resolve this, analyze argument types at each call site to understand the source of variation. Consider splitting the function into specialized variants for different usage patterns, or ensure the function's specializations cover all call contexts adequately. Review for polymorphic inline caches that may require monomorphization through call target splitting. For verification, use `--engine.TraceCompilationPolymorphism` to identify generic nodes and `--engine.TraceSplitting` to understand splitting decisions.

### Correlation with Other Tools

The CPU Sampler integrates deeply with the broader GraalVM profiling ecosystem. **VisualVM and Chrome Inspector** both leverage the CPUSampler API internally, as documented in the official materials: "The CPU Sampler provides an API that other GraalVM tools can use to expose the data it provides to other applications. Two such examples are the VisualVM and Chrome inspector integrations... Both of these tools leverage the integrated CPU Sampler to get the profile and then present it in a familiar way to the end user." This integration provides GUI-based profile visualization, timeline views, and integration with other monitoring features for developers who prefer graphical interfaces.

**CPU Tracer** serves as a complementary tool where the sampler measures time while the tracer counts executions. The workflow combines both: first use `--cpusampler` to identify hot functions consuming time, then use `--cputracer --cputracer.TraceStatements` to pinpoint which specific statements within those functions execute most frequently. This combination provides both statistical time approximation and precise execution counts.

For **compilation tracing**, combine the tools with `--cpusampler --cpusampler.ShowTiers=true --engine.TraceCompilation` to correlate compilation events with performance. The workflow proceeds as follows: CPUSampler identifies functions with high T0 interpreter time, TraceCompilation logs reveal compilation issues or deoptimization events, and developers can then target optimization efforts accordingly. Finally, **Memory Tracer** complements CPU profiling for comprehensive performance analysis using `--cpusampler --experimental-options --memtracer`, providing both time analysis and allocation analysis to identify whether performance issues stem from computation or memory management.

## 6. Additional Notes

### Overhead

The CPU Sampler demonstrates dramatically different overhead characteristics before and after the GraalVM 21.3.0 architectural redesign. Pre-21.3.0 implementations maintained a shadow stack via instrumentation, introducing overhead on every function call and exit with significant CPU and memory impact. Post-21.3.0 implementations use safepoint-based sampling with no per-call overhead and minimal impact only during sampling intervals. As documented in the Medium blog post "Where Has All My Run Time Gone?", the rewrite "greatly reducing the CPU and memory overhead of using the CPU Sampler" through elimination of shadow stack maintenance. Overhead can be further reduced using `--cpusampler.GatherAsyncStackTrace=false` to disable async stack gathering, avoiding `--cpusampler.Mode=roots` which adds overhead for inlined function tracking, or increasing `--cpusampler.Period` to reduce sampling frequency. The tool is suitable for development use but typically not deployed in production due to output volume rather than runtime overhead concerns.

### Limitations

Sampling-based profiling provides **statistical approximation rather than exact measurement**. Short-running functions may be under-sampled, and the default 10ms period may miss very brief operations. The sampler exhibits **native code blindness**, unable to profile into native libraries where C extensions appear as atomic operations—system profilers are required for native code analysis. A configurable **stack depth limit** defaults to 10,000 frames; developers can check for truncation using the `hasStackOverflowed()` API method and increase the limit with `--cpusampler.StackLimit` if needed. By default, **context initialization** is grouped into a single entry rather than being profiled in detail; use `--cpusampler.SampleContextInitialization` to profile initialization separately. Finally, **async stack traces** for asynchronous operations may not show the complete logical call stack, controlled by `--cpusampler.GatherAsyncStackTrace` which defaults to true.

### Best Practices

For effective CPU sampling, always **use delay for steady-state profiling** with commands like `js --cpusampler --cpusampler.Delay=5000 app.js` to skip warmup and focus on optimized execution behavior. Follow a workflow that **starts with histogram format to identify hot functions**, then **drills down with calltree** to understand why they're hot and which call sites contribute to overhead. For large applications, **leverage flamegraphs** which enable visual pattern recognition, interactive exploration, and easy sharing with team members. Practice **strategic filtering** to focus analysis, using `--cpusampler.FilterRootName=*accept*` to examine specific function families or `--cpusampler.FilterFile=*myapp*` to profile only application code. Always **combine with tier analysis** using `--cpusampler --cpusampler.ShowTiers=true --cpusampler.Delay=2000` to verify optimization effectiveness. For custom analysis needs, **export JSON format** with `--cpusampler.Output=json --cpusampler.OutputFile=data.json` to build custom tooling or dashboards. When investigating startup performance, **profile context initialization separately** using `--cpusampler.SampleContextInitialization=true` to understand initialization impact.

### Common Pitfalls

**Profiling the warmup phase** remains the most common mistake—results become dominated by interpreter and compilation activity rather than steady-state performance. Always use `--cpusampler.Delay` to skip warmup. **Ignoring tier information** causes developers to miss critical deoptimization problems; use `--cpusampler.ShowTiers` for all performance-critical analysis. **Not sampling internal sources** makes hotspots in the standard library invisible; add `--cpusampler.SampleInternal=true` when application code looks clean but performance issues persist. In **default mode, inlined functions** don't appear in the call tree, making flamegraphs incomplete; use `--cpusampler.Mode=roots` when you need the full picture, accepting the higher overhead. When generating flamegraphs, **forgetting to specify OutputFile** causes each run to overwrite `flamegraph.svg`; always use `--cpusampler.OutputFile=unique-name.svg` for reproducible analysis. Finally, using **excessively short sampling periods** introduces overhead that distorts results; increase the period for longer-running applications.

### Related Tools

The GraalVM ecosystem provides numerous complementary profiling and analysis tools. **CPU Tracer** complements the sampler by counting executions rather than measuring time. **Memory Tracer** enables allocation profiling to identify memory-related performance issues. **VisualVM** provides GUI integration with the CPU Sampler for developers preferring graphical interfaces. **Chrome DevTools** offers inspector protocol integration for web-familiar debugging workflows. **Oracle Developer Studio** combines native and JVM profiling in a single tool. **Java Flight Recorder (JFR)** provides JVM-level profiling that can correlate with Truffle-level analysis. **Ideal Graph Visualizer (IGV)** visualizes compiler optimization decisions and graph transformations. Finally, **Proftool** (available since 21.3) enables assembly-level performance analysis for the deepest optimization work.

For programmatic control, language and tool developers can use the Java API from the `com.oracle.truffle.tools.profiler` package. The API provides classes including `CPUSampler`, `CPUSamplerData`, `ProfilerNode`, and `StackTraceEntry` for programmatic configuration, data collection, and analysis. This enables integration of profiling capabilities directly into language implementations, IDEs, or custom analysis tools.

## 7. Resources

### Official GraalVM Documentation

1. **Profiling Command Line Tools (Latest)**
   - https://www.graalvm.org/latest/tools/profiling/
   - Complete reference for all cpusampler options and usage examples

2. **Profiling Truffle Interpreters (Latest)**
   - https://www.graalvm.org/latest/graalvm-as-a-platform/language-implementation-framework/Profiling/
   - Guidance for Truffle language developers on profiling techniques

3. **CPUSampler Java API Documentation**
   - https://www.graalvm.org/tools/javadoc/com/oracle/truffle/tools/profiler/CPUSampler.html
   - Complete API reference for programmatic usage

4. **Oracle Documentation - Profiling Tools (JDK 20)**
   - https://docs.oracle.com/en/graalvm/jdk/20/docs/tools/profiling/
   - Oracle's documentation with examples

### GitHub Repository

5. **oracle/graal - Truffle Profiling Documentation**
   - https://github.com/oracle/graal/blob/master/truffle/docs/Profiling.md
   - Official profiling guide in source repository

6. **oracle/graal - CPUSampler Source Code**
   - https://github.com/oracle/graal/blob/master/tools/src/com.oracle.truffle.tools.profiler/src/com/oracle/truffle/tools/profiler/CPUSampler.java
   - Implementation details and internal architecture

7. **oracle/graal - Optimizing Truffle Interpreters**
   - https://github.com/oracle/graal/blob/master/truffle/docs/Optimizing.md
   - Context on using cpusampler for optimization workflows

### Technical Blog Posts

8. **"Where Has All My Run Time Gone?" by Boris Spasojević**
   - https://medium.com/graalvm/where-has-all-my-run-time-gone-245f0ccde853
   - Comprehensive tutorial explaining 21.3.0 architectural improvements from instrumentation to safepoints

9. **"GraalVM 21.3 Release Announcement" by Alina Yurenko**
   - https://medium.com/graalvm/graalvm-21-3-is-here-java-17-native-image-performance-updates-and-more-ac4cbafcfc05
   - Release notes covering cpusampler improvements

### Release Notes

10. **GraalVM 21.3 Release Notes**
    - https://www.graalvm.org/release-notes/21_3/
    - Official changelog documenting cpusampler **improvements**