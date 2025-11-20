# Overview

# Tool: --cpusampler


## 3. Functional Description

### Primary Purpose

The CPU Sampler identifies performance bottlenecks in Truffle-based applications by determining where execution time is spent, enabling language developers to focus optimization efforts on critical code paths that consume the majority of runtime.

### What the Tool Does

The CPU Sampler performs **periodic stack sampling** across all application threads, capturing call stacks at regular intervals (default: every 10 milliseconds). It aggregates these samples into tree structures of ProfilerNode objects that represent the execution profile, calculating statistical approximations of time spent in each function based on sample frequency. The tool generates multi-format output (histogram, call tree, JSON, or flamegraph) that reveals both self-time (time spent directly in a function) and total-time (time spent in a function including its callees). Optionally, it tracks JIT compilation tier information to show whether code executes in the interpreter or compiled form.

### How It Works

The CPU Sampler underwent a **major architectural redesign in GraalVM 21.3.0**, transitioning from an instrumentation-based approach to a **safepoint-based mechanism** built on top of Truffle safepoints. Prior to 21.3.0, the sampler maintained a shadow stack using the Truffle instrumentation framework, manipulating an additional stack data structure on every function call and exit, which introduced significant overhead. The new implementation uses the TruffleSafepoint API introduced in GraalVM 21.1.0, which allows thread-local actions to be executed at safe interruption points. When the sampling timer expires (at the configured period), the safepoint mechanism triggers on all threads, stack frames are captured via the `iterateFrames()` API, and the stack trace is copied to the ProfilerNode tree structure with hit counts incremented for sampled nodes. This eliminates per-call overhead and **greatly reduces CPU and memory consumption** compared to the pre-21.3.0 instrumentation approach.

### When to Use

Language developers should use the CPU Sampler during the **development phase** for several critical use cases. First, for **initial performance investigation** to quickly understand where time is spent in guest language code. Second, for **compilation effectiveness analysis** by combining the sampler with `--cpusampler.ShowTiers` to verify which functions are compiled and at what optimization level (interpreter T0, tier-1 T1, or tier-2 T2). Third, for **deoptimization detection**, as functions spending significant time in interpreter mode (T0) indicate potential deoptimization issues requiring investigation. Fourth, for **algorithm comparison** to validate that optimizations target actual hotspots and that alternative implementations deliver expected performance improvements. Fifth, for **warmup analysis** using `--cpusampler.Delay` to profile post-warmup steady-state performance and identify functions that remain uncompiled. Finally, for **language implementation testing** to profile interpreter and compiler effectiveness during Truffle language development, including seamless profiling across language boundaries in polyglot applications.

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