# Tool Name: --engine.CompilationStatistics

## 1. Command Execution

### Basic Command
```bash
<language-launcher> --experimental-options --engine.CompilationStatistics <script>
```

### Full Command Syntax
```bash
# Complete syntax with common combinations
<language-launcher> --experimental-options \
  --engine.CompilationStatistics \
  --engine.CompilationStatisticDetails \
  --engine.CompileOnly=<name> \
  --engine.BackgroundCompilation=false \
  <script>

# Examples
js --experimental-options --engine.CompilationStatistics script.js
java -jar launcher.jar --jvm --experimental-options --engine.CompilationStatistics app
```

### Execution Context

- **JVM Mode Compatibility**: Yes
  - **Requirements**: GraalVM installation with HotSpot JVM, accessed via `--jvm` flag or standard launcher
  - **Default mode**: Fully supported with complete statistics
  
- **Native Image Mode Compatibility**: Yes
  - **Requirements**: Truffle language compiled to native executable
  - **Limitations**: Some statistics may be limited compared to JVM mode due to AOT compilation
  
- **Prerequisites**:
  - GraalVM installation (Community or Enterprise Edition)
  - `--experimental-options` flag must be enabled (required)
  - Truffle-based language implementation
  - Compatible with both single-tier and multi-tier compilation modes

## 2. Command Options & Parameters

### Key Options

| Option                                 | Type         | Default | Description                                                                                                 |
| -------------------------------------- | ------------ | ------- | ----------------------------------------------------------------------------------------------------------- |
| `--engine.CompilationStatistics`       | Boolean flag | false   | Prints comprehensive Truffle compilation statistics at program termination to stderr                        |
| `--engine.CompilationStatisticDetails` | Boolean flag | false   | Extends CompilationStatistics with verbose output including histogram information on individual Graal nodes |

### Advanced Options (Commonly Used Together)

| Option                           | Type          | Default | Description                                                                             |
| -------------------------------- | ------------- | ------- | --------------------------------------------------------------------------------------- |
| `--engine.CompileOnly`           | String (name) | none    | Restrict compilation to specific methods for focused analysis                           |
| `--engine.CompileImmediately`    | Boolean flag  | false   | Compile immediately for testing purposes                                                |
| `--engine.BackgroundCompilation` | Boolean       | true    | When set to false, disables asynchronous compilation for simpler deterministic analysis |

**Key Option Details**:

- **--engine.CompilationStatistics**: This is classified as an internal/expert option primarily for language implementation debugging. It hooks into the Truffle compilation pipeline and collects deterministic counters and metrics throughout program execution, outputting a comprehensive aggregated summary report when the program terminates.

- **--engine.CompilationStatisticDetails**: This extends the base statistics with additional histogram breakdowns showing counts for individual Graal IR node types (e.g., FrameState, LoadFieldNode, BeginNode) across different compilation phases, providing deeper insight into the compiler's internal behavior.

## 3. Functional Description

### Primary Purpose

The `--engine.CompilationStatistics` tool serves as a comprehensive statistical analysis feature for Truffle language implementations, designed to provide language developers with aggregated metrics about compilation behavior, performance characteristics, and code quality during the development and optimization phase.

### What the Tool Does

This tool collects and aggregates statistical data across all Truffle compilations that occur during program execution. It tracks compilation outcomes (successes, failures, bailouts, interruptions), measures performance metrics such as compilation throughput and timing, counts AST and IR node statistics, analyzes code generation metrics, monitors compilation queue behavior, and identifies invalidation and deoptimization patterns. The tool outputs a multi-section statistical report to stdout at program termination, providing a comprehensive view of the compiler's behavior throughout the entire execution.

### How It Works

The tool operates through statistical instrumentation rather than sampling or tracing. It hooks directly into the Truffle compilation pipeline and collects deterministic counters and measurements during each phase of compilation—including the Truffle tier (partial evaluation) and Graal tiers (backend optimization and code generation). During the collection phase, metrics are recorded for every compilation attempt as it happens. These statistics are continuously aggregated in memory throughout program execution. Finally, on program termination, the accumulated data is formatted and output as a comprehensive report to stdout. This approach provides exact counts and measurements rather than probabilistic estimates, though it focuses on aggregate statistics rather than individual compilation events.

### When to Use

Language developers should use this tool during the development phase when diagnosing compilation problems such as frequent bailouts or failures, when performance tuning to understand compilation overhead and throughput characteristics, when validating that code optimizations are being properly applied by the compiler, when analyzing warmup behavior to track compilation patterns during execution phases, when comparing different runtime configurations to evaluate the impact of compiler options, and when performing queue analysis to understand compilation queue behavior under load. This tool is particularly valuable during iterative development cycles where understanding the aggregate behavior of the compilation system helps identify systematic issues or validate optimization strategies.

## 4. Output Format & Structure

### Output Type
- ☑ Console/Terminal
- ☑ File output (via redirection)
- ☐ Graphical
- ☐ Binary

### Output Location

- **Default**: stdout stream (console)
- **Custom specification**: Redirect stoud to file using `1> filename.txt`
- **Timing**: Output appears at program exit via shutdown hook

### Output Format Example

```
[engine] Truffle runtime statistics for engine 1

Compilations                    : 2763
  Success                       : 2743
  Temporary Bailouts            : 17
    jdk.graal.compiler.core.common.CancellationBailoutException: Compilation cancelled.: 16
    jdk.graal.compiler.core.common.RetryableBailoutException: Assumption invalidated: 1
  Permanent Bailouts            : 0
  Failed                        : 0
  Interrupted                   : 3

Invalidated                     : 84
  Unknown Reason                : 45
  Profiled Argument Types       : 22
  validRootAssumption Split call node: 12
  expression invalidatePropertyAssumption: 1

Queues                          : 3410
Dequeues                        : 262
  Split call node               : 124
  Target inlined into only caller: 87
  validRootAssumption Split call node: 28

Splits                          : 5581
Compilation Accuracy            : 0.969598
Queue Accuracy                  : 0.923167
Compilation Utilization         : 2.685016
Remaining Compilation Queue     : 409

Time to queue                   : count=3410, sum=12895957640, min=0, average=3781805.76, max=11311217
Time waiting in queue           : count=2763, sum=1247424699, min=40, average=451474.74, max=4762017

---------------------------
AST node statistics:
  Truffle node count            : count=2752, sum=348085, min=1, average=126.48, max=5404
    Trivial                     : count=2752, sum=124817, min=0, average=45.36, max=2707
    Non Trivial                 : count=2752, sum=223268, min=1, average=81.13, max=3162
    Monomorphic                 : count=2752, sum=217660, min=1, average=79.09, max=3042
    Polymorphic                 : count=2752, sum=4239, min=0, average=1.54, max=71
    Megamorphic                 : count=2752, sum=1369, min=0, average=0.50, max=49
  Truffle call count            : count=2752, sum=7789, min=0, average=2.83, max=110
    Direct                      : count=2752, sum=7760, min=0, average=2.82, max=110
    Dispatched                  : count=2752, sum=6285, min=0, average=2.28, max=110
    Inlined                     : count=2752, sum=1475, min=0, average=0.54, max=60

---------------------------
Compilation Tier 1:
  Compilation Rate              : 471075.74 bytes/second
  Truffle Tier Rate             : 1273853.94 bytes/second
  Graal Tier Rate               : 922948.70 bytes/second
  Time for compilation (us)     : count=2637, sum=20538922, min=727, average=7788.75, max=2154173
    Truffle Tier (us)           : count=2623, sum=7595366, min=255, average=2895.68, max=306107
    Graal Tier (us)             : count=2623, sum=10483126, min=378, average=3996.62, max=1665018

Graal node count:
  After Truffle Tier            : count=2627, sum=1089218, min=88, average=414.62, max=12999
  After Graal Tier              : count=2624, sum=2190826, min=127, average=834.92, max=63837

Graal compilation result:
  Code size                     : count=2623, sum=9675388, min=492, average=3688.67, max=238448
  Total frame size              : count=2623, sum=350080, min=64, average=133.47, max=5328
  Exception handlers            : count=2623, sum=9316, min=1, average=3.55, max=125
```

### Output Fields/Metrics

| Field                                   | Description                      | Unit         | Interpretation                               |
| --------------------------------------- | -------------------------------- | ------------ | -------------------------------------------- |
| **Compilations**                        | Total compilation attempts       | count        | Higher indicates more compilation activity   |
| **Success**                             | Successful compilations          | count        | Should be >95% of total attempts             |
| **Temporary Bailouts**                  | Recoverable compilation failures | count        | High values indicate instability             |
| **Permanent Bailouts**                  | Unrecoverable failures           | count        | Should be zero; indicates fundamental issues |
| **Invalidated**                         | Compiled code invalidations      | count        | High values suggest assumption violations    |
| **Queues**                              | Tasks added to compilation queue | count        | Indicates compilation pressure               |
| **Compilation Accuracy**                | Success rate ratio               | 0.0-1.0      | >0.95 is healthy                             |
| **Queue Accuracy**                      | Queue management effectiveness   | 0.0-1.0      | Higher is better                             |
| **Time to queue**                       | Delay from trigger to queueing   | microseconds | Lower indicates responsive compilation       |
| **Time waiting in queue**               | Queue wait time                  | microseconds | Lower indicates less contention              |
| **Compilation Rate**                    | Overall code generation speed    | bytes/second | Higher is more efficient                     |
| **Truffle Tier Rate**                   | Partial evaluation throughput    | bytes/second | Speed of AST lowering                        |
| **Graal Tier Rate**                     | Backend compilation throughput   | bytes/second | Speed of optimization/codegen                |
| **Truffle node count**                  | AST nodes in compilation unit    | count        | Indicates code complexity                    |
| **Monomorphic/Polymorphic/Megamorphic** | Type specialization state        | count        | More monomorphic is better                   |
| **Inlined**                             | Successfully inlined calls       | count        | Higher ratio improves performance            |
| **After Truffle Tier**                  | Graal nodes after PE             | count        | Size of lowered graph                        |
| **After Graal Tier**                    | Graal nodes after optimization   | count        | Size of optimized graph                      |
| **Code size**                           | Generated machine code           | bytes        | Larger may indicate bloat                    |
| **Total frame size**                    | Stack frame size                 | bytes        | Larger increases memory pressure             |

## 5. Insights & Analysis

### When should you use it?

Language developers should reach for this tool when they encounter questions about why their language implementation isn't achieving expected performance, when compilation seems to be taking too long or consuming excessive resources, when they observe unexpected warmup behavior or instability in compiled code, or when they need to validate that their optimization annotations and specializations are working as intended. The tool answers fundamental questions about whether code is being successfully compiled, how much time compilation is consuming relative to execution, whether type assumptions are stable enough for effective optimization, and how the compilation queue is behaving under different workloads. It solves the problem of understanding the black box of the Truffle compiler by providing concrete, quantifiable metrics that reveal exactly what the compiler is doing throughout program execution.

### Insight: Compilation Health Analysis

When examining the output, a high success rate greater than 95% indicates good compilation behavior where most code paths are successfully optimized. However, if you observe many temporary bailouts listed with specific exception types, this symptom reveals that the compiler is encountering instability in the code—often due to unstable type assumptions, frequent assumption invalidations, or transient conditions during compilation. The root cause is typically that the language implementation allows too much dynamism in hot code paths, or that splitting and specialization strategies are causing excessive code duplication that leads to compilation cancellations. To resolve this, examine the specific bailout exception messages to identify patterns, stabilize type information by ensuring consistent types flow through hot paths, review assumption usage to avoid over-aggressive assumptions that frequently invalidate, and consider adjusting splitting strategies if split-related bailouts dominate. You should correlate these findings with `--engine.TraceCompilation` output to see exactly which methods are bailing out repeatedly.

### Insight: Performance Bottleneck Identification

When compilation rates appear low, this symptom indicates that the compiler is spending excessive time processing your code. The root cause typically lies in overly complex AST structures, excessive polymorphism forcing the compiler to explore many paths, too aggressive inlining creating bloated compilation units, or inefficient node types that create large intermediate representations. Resolution involves simplifying node implementations to reduce complexity, improving type stability to enable more aggressive optimization with less analysis overhead, tuning inlining budgets using options like `--engine.InliningRecursionDepth` and `--engine.InliningInliningBudget`, and examining the AST node statistics to identify sources of complexity like high megamorphic counts or exceptionally large node counts. Verification requires re-running with the changes and comparing the rate metrics, and cross-referencing with the Ideal Graph Visualizer (IGV) to inspect the actual compilation graphs for bloat.

### Insight: Queue Pressure and Utilization

When you see high "Remaining Compilation Queue" values at program exit or low Queue Accuracy, this symptom indicates that the compilation system is overwhelmed and cannot keep pace with compilation requests. The root cause is typically that the workload triggers too many compilation attempts simultaneously, background compilation cannot process tasks fast enough due to insufficient compiler threads, or compilation priorities are not well-tuned causing important compilations to wait. To resolve this, consider increasing compiler threads via `--engine.CompilerThreads=N`, adjust compilation thresholds to reduce eagerness, use `--engine.CompileOnly` to focus on truly hot methods during development, or enable multi-tier compilation if not already active. Correlate findings with `--engine.TraceCompilationDetails` to see detailed queue operations and identify which methods are flooding the queue.

### Insight: Type Instability Diagnosis

High counts of polymorphic or megamorphic nodes—particularly if polymorphic counts exceed 5-10% of total node counts—reveal that the code is not achieving stable type specialization. This root cause stems from the language implementation allowing type variations in hot loops, mixing types at call sites, or cache implementations that don't maintain consistent specialization. Resolution requires adding or improving specialization annotations in your Truffle nodes, ensuring consistent types flow through hot paths by refactoring code patterns that mix types, implementing caching strategies that maintain type stability, and potentially adding more specific specialization cases for common type combinations. Use `--engine.TraceCompilation` with `--engine.CompilationExceptionsAreFatal` during development to catch problematic patterns early, and verify improvements by monitoring the monomorphic/polymorphic ratio in subsequent runs.

### Insight: Code Size and Memory Pressure

When code size averages or maximums appear excessive this symptom suggests compilation bloat that can lead to instruction cache pressure and memory overhead. The underlying issue is typically over-inlining where small methods are repeatedly inlined into many callers, lack of specialization boundaries causing compilation units to become too large, or inefficient code generation patterns. Addressing this requires tuning inlining parameters to be more conservative, using `@TruffleBoundary` annotations to prevent inlining of non-critical methods, implementing method splitting strategies to break large compilation units into smaller ones, and examining the maxTarget annotations in the output to identify which specific methods are producing huge code. Verify improvements using the code size metrics and also check with native profiling tools to ensure instruction cache miss rates decrease.

### Insight: Invalidation Pattern Analysis

Frequent invalidations with specific reasons like "Profiled Argument Types" or property assumption violations indicate that compiled code is being thrown away due to violated assumptions. The root cause is that the runtime behavior doesn't match the assumptions made during compilation—often because profiling data isn't stabilizing before compilation, object shapes are changing after optimization, or code paths exhibit different behavior after warmup. Resolution involves extending warmup periods before compilation using `--engine.CompilationThreshold`, stabilizing object shapes by ensuring prototypes and structures are finalized before hot paths execute, reviewing DSL specialization guards to ensure they properly capture runtime conditions, and implementing better feedback mechanisms to detect instability before compilation. Cross-reference with `--engine.TraceAssumptions` to see exactly which assumptions are being invalidated and why.

### Correlation with Other Tools

The compilation statistics tool provides aggregate data that should be correlated with several complementary tools for comprehensive analysis. Use `--engine.TraceCompilation` alongside this tool to get the aggregate statistics view combined with per-compilation event logs—the statistics identify problematic patterns while TraceCompilation reveals which specific methods exhibit those patterns. The CPU Sampler tool (`--cpusampler`) reveals which methods consume the most runtime, and these should correlate with high execution counts in successful compilations—if a method is hot in the sampler but shows low success rates or high bailouts in compilation statistics, that's a critical issue to investigate. The Ideal Graph Visualizer (IGV) accessed via `--vm.Dgraal.Dump` allows visual inspection of the compilation graphs for methods identified as problematic in the statistics, helping you understand why certain compilations produce large node counts or fail to optimize well. Memory Tracer (`--experimental-options --memtracer`) can be correlated with frame sizes and code sizes to understand total memory pressure from both allocation patterns and compiled code. Finally, `--engine.CompilationExceptionsAreFatal` combined with this tool helps catch and debug compilation failures during development by failing fast rather than silently bailing out.

## 6. Additional Notes

### Overhead

The performance impact of enabling compilation statistics collection is negligible during normal execution, typically adding less than 1% overhead to total runtime. The memory impact is small, involving only the accumulation of counters and basic statistical data structures in memory throughout execution. The tool is suitable for production environments when necessary, though it's primarily designed for development and testing phases. Importantly, statistics collection does not impact code generation quality—the same compilation decisions and optimizations occur regardless of whether statistics are being gathered, ensuring that performance measurements remain representative of actual production behavior.

### Limitations

Several constraints should be understood when using this tool. The timing measurements have microsecond resolution which may introduce measurement noise on very fast operations. Statistics are only printed at program exit via shutdown hook, meaning you don't get real-time visibility into compilation behavior—if the program crashes or is forcibly terminated, statistics may be lost. The output is not automatically persisted to disk and must be manually redirected using stderr redirection. The statistics are aggregated across all compilations, so while you can see patterns, you cannot trace individual method compilation histories without combining with TraceCompilation. The tool is marked as experimental, meaning the API and output format may change between GraalVM versions without deprecation warnings. In Native Image mode, some metrics may be less meaningful or accurate due to the ahead-of-time compilation model where much compilation happens at build time rather than runtime.

### Best Practices

Effective use of this tool requires following several key practices. Always enable `--experimental-options` as a prerequisite since the compilation statistics options won't be recognized without it. oWhen investigating specific performance issues, combine CompilationStatistics with `--engine.CompileOnly=methodPattern` to focus the statistics on relevant methods and reduce noise from framework code. Run the tool multiple times with the same workload to ensure statistical consistency and account for JVM warmup variability. Use representative workloads that exercise the actual code paths and patterns you care about optimizing, as statistics are highly workload-dependent. When comparing different configurations or code changes, keep all other variables constant and only change the specific element being tested. For deterministic analysis, consider disabling background compilation using `--engine.BackgroundCompilation=false` to make compilation behavior more predictable and reproducible. Start with the basic `--engine.CompilationStatistics` option to get an overview, then add `--engine.CompilationStatisticDetails` only when you need the additional histogram detail, as the verbose output can be overwhelming. Establish baseline measurements early in development and track key metrics like compilation accuracy, success rate, and average compilation times across releases to catch regressions.

### Common Pitfalls

Developers frequently encounter several avoidable issues when using this tool. The most common mistake is forgetting to include `--experimental-options` before the engine options, which causes the options to be silently ignored. Another frequent problem is not waiting for program exit—since statistics are only printed at shutdown, developers sometimes interrupt programs prematurely and wonder why no output appears. There's often confusion about tier numbering in the output: Tier 1 is the first-tier quick compilation with limited optimization, while Tier 2 is the optimizing tier with aggressive optimization, which is counterintuitive to some who expect tier numbers to increase with urgency rather than optimization level. Unit mistakes are common—times are in microseconds and rates are in bytes per second, and forgetting these units leads to misinterpretation of whether values are good or bad. The separation of statistics by tier can be confusing, and it's important to understand that most production performance comes from Tier 2 (optimizing) compilations, so Tier 1 statistics are less critical. Finally, comparing statistics across different workloads without accounting for the inherent workload differences leads to false conclusions about performance changes—always compare like-with-like using identical workloads.

### Related Tools

Several related tools complement compilation statistics in a comprehensive performance analysis workflow. The `--engine.TraceCompilation` option provides per-compilation event logging, printing a line for each compilation with its outcome, timing, and key characteristics—use this when CompilationStatistics reveals aggregate patterns and you need to identify which specific methods exhibit those patterns. The `--engine.TraceInlining` option shows detailed inlining decisions, explaining why each call site was or wasn't inlined, which helps explain why certain compilation units become large or why performance doesn't meet expectations. The `--engine.TracePerformanceWarnings` option reports potential performance issues like megamorphic call sites or failed optimizations, automatically flagging problems that would otherwise require careful manual analysis of statistics. The `--engine.TraceSplitting` option details call target splitting decisions, explaining how the runtime is duplicating code for better specialization. For deeper investigation, the Ideal Graph Visualizer (IGV) tool accessed via `--vm.Dgraal.Dump=:2` allows visual inspection of the actual compiler intermediate representation graphs for any compilation, making it possible to see exactly why certain methods produce large node counts or fail to optimize. The C1 Visualizer serves a similar purpose for lower-level compilation phases. For runtime behavior, the CPU Sampler (`--cpusampler`) and CPU Tracer (`--cputracer`) reveal which code is actually consuming time at runtime versus which code is being compiled. Memory Tracer (`--experimental-options --memtracer`) tracks allocation patterns that can be correlated with frame sizes and code generation patterns. Finally, the `--engine.CompilationExceptionsAreFatal` option causes the JVM to fail immediately on compilation bailouts rather than falling back to interpretation, which is valuable during development for catching problems early rather than silently degrading performance.

## 7. Resources

### Official GraalVM Documentation

- GraalVM Truffle Options Guide: https://www.graalvm.org/latest/graalvm-as-a-platform/language-implementation-framework/Options/
- Optimizing Truffle Interpreters Guide: https://www.graalvm.org/latest/graalvm-as-a-platform/language-implementation-framework/Optimizing/
- Oracle GraalVM Documentation (JDK 17): https://docs.oracle.com/en/graalvm/jdk/17/docs/graalvm-as-a-platform/language-implementation-framework/Optimizing/
- GraalVM Truffle Options (Version-Specific): https://www.graalvm.org/22.0/graalvm-as-a-platform/language-implementation-framework/Options/
- Truffle Language Implementation Framework: https://www.graalvm.org/latest/graalvm-as-a-platform/language-implementation-framework/
- Profiling Truffle Interpreters: https://www.graalvm.org/latest/graalvm-as-a-platform/language-implementation-framework/Profiling/
- Truffle Compilation Queue: https://www.graalvm.org/latest/graalvm-as-a-platform/language-implementation-framework/TraversingCompilationQueue/

### GitHub Repository

- Oracle/Graal Repository: https://github.com/oracle/graal
- Truffle Documentation (truffle/docs/Optimizing.md): https://github.com/oracle/graal/tree/master/truffle/docs