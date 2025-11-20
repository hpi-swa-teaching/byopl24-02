---
name: Trace Performance Warnings
description: Detects optimization barriers during Truffle compilation. Use this FIRST when optimizing - it identifies virtual calls, non-constant stores, unresolved type checks, and TruffleBoundary issues that prevent peak performance. Reports exact source locations with stack traces. Essential for eliminating compilation warnings that block full optimization.
---

# Tool Name: --compiler.TracePerformanceWarnings

## 1. Command Execution

### Basic Command

```bash
<language-launcher> --compiler.TracePerformanceWarnings=all <script>
```

This enables all performance warning types at the Truffle engine level.

### Full Command Syntax

```bash
<langiage-launcher> --compiler.TracePerformanceWarnings=none|all|<perfWarning>,<perfWarning>,... \
  [--engine.TraceCompilation] \
  [--engine.CompileOnly=<name-list>] \
  [--engine.CompileImmediately] \
  [--engine.BackgroundCompilation=<boolean>] \
  <script>
```

**Valid Warning Types:** `call`, `instanceof`, `store`, `frame_merge`, `trivial`, `bailout`

**Full Example:**
```bash
js --experimental-options \
  --compiler.TracePerformanceWarnings=call,instanceof,store \
  --engine.TraceCompilation \
  app.js
```

### Execution Context

| Context               | Support | Requirements                                                                                                                                                   |
| --------------------- | ------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **JVM Mode**          | Yes     | GraalVM or standard JDK with Graal compiler installed. No special build configuration required.                                                                |
| **Native Image Mode** | Yes     | Works with ahead-of-time (AOT) compiled executables. Warnings appear during image build for AOT compilation or at runtime for JIT compilation in Native Image. |
| **Prerequisites**     | Varies  | May require `--experimental-options` flag in some GraalVM versions. Option marked as `OptionCategory.INTERNAL` (internal/debugging option).                    |

**Source:** [GraalVM Options Documentation](https://www.graalvm.org/latest/graalvm-as-a-platform/language-implementation-framework/Options/)

## 2. Command Options & Parameters

### Key Options

| Option        | Type         | Default       | Description                                                                               |
| ------------- | ------------ | ------------- | ----------------------------------------------------------------------------------------- |
| `none`        | Keyword      | Yes (default) | Disables all performance warnings; no output generated                                    |
| `all`         | Keyword      | No            | Enables all performance warning types; equivalent to specifying all warning categories    |
| `call`        | Warning Type | Disabled      | Enables virtual runtime call warnings when partial evaluation cannot inline method calls  |
| `instanceof`  | Warning Type | Disabled      | Enables virtual instanceof warnings when type checks cannot be resolved to exact types    |
| `store`       | Warning Type | Disabled      | Enables store location warnings when store locations are not partial evaluation constants |
| `frame_merge` | Warning Type | Disabled      | Enables frame merge warnings for incompatible frame states at control flow merge points   |
| `trivial`     | Warning Type | Disabled      | Enables trivial code warnings for unnecessarily complex simple operations                 |
| `bailout`     | Warning Type | Disabled      | Enables compilation bailout warnings (added in GraalVM 20.1.0+)                           |

**Source:** [oracle/graal Options.md](https://github.com/oracle/graal/blob/master/truffle/docs/Options.md)

### Advanced Options

Options can be combined with other engine options for comprehensive analysis:

| Combined Option                        | Purpose                                    | Example                                                  |
| -------------------------------------- | ------------------------------------------ | -------------------------------------------------------- |
| `--engine.CompileOnly`                 | Restrict warnings to specific methods      | `--engine.CompileOnly=myMethod`                          |
| `--engine.TraceCompilation`            | Correlate warnings with compilation events | Used together to see which compilations trigger warnings |
| `--engine.CompileImmediately`          | Force compilation without warmup           | Useful for testing; triggers warnings immediately        |
| `--engine.BackgroundCompilation=false` | Synchronous compilation                    | Deterministic warning order for debugging                |

**Source:** [GraalVM Options Documentation](https://www.graalvm.org/latest/graalvm-as-a-platform/language-implementation-framework/Options/)

## 3. Functional Description

### Primary Purpose

The `--compiler.TracePerformanceWarnings` option identifies code patterns that prevent optimal partial evaluation and compilation during the Truffle Tier compilation phase. This tool serves as the primary diagnostic mechanism for Truffle language developers to understand why their interpreter implementations fail to achieve peak performance after compilation. By revealing optimization barriers at the AST-to-IR transformation stage, it enables targeted improvements to language implementations.

### What the Tool Does

This option monitors the partial evaluation phase of Truffle compilation and prints warnings when the compiler encounters patterns that block optimization. During partial evaluation, the Truffle AST is transformed into Graal Intermediate Representation (IR), with aggressive inlining, constant folding, and type specialization applied. When these optimizations cannot proceed due to insufficient compile-time information or problematic code patterns, warnings are emitted to console with detailed stack traces showing the exact location of the issue. The tool tracks six distinct warning categories, each representing a specific optimization failure mode that impacts runtime performance.

**Source:** [Oracle GraalVM Optimizing Documentation](https://docs.oracle.com/en/graalvm/enterprise/21/docs/graalvm-as-a-platform/language-implementation-framework/Optimizing/)

### How It Works

The tool operates during the TruffleTier compilation phase, which occurs after the interpreter has determined a method is hot enough to compile. During partial evaluation, the compiler attempts to inline all method calls originating from the Truffle AST, resolve all type checks to concrete types, and eliminate all abstractions. The warning detection mechanism is integrated into the partial evaluation infrastructure and triggers when specific optimization goals cannot be achieved. For virtual call warnings, the system detects when method targets cannot be statically resolved. For instanceof warnings, it identifies type checks that remain polymorphic. For store warnings, it flags frame slot or property accesses with non-constant locations. Frame merge warnings appear when control flow paths converge with incompatible frame states, and trivial warnings identify simple operations that should optimize away but don't.

Each warning includes the compilation unit name, a descriptive message about the optimization failure, a node ID that can be used to locate the problematic node in Ideal Graph Visualizer (IGV), and an approximated stack trace showing the call hierarchy through Truffle nodes with source file locations. The warnings go to standard output with an `[engine] perf warn` prefix, making them easily identifiable in console output.

**Source:** [Oracle GraalVM Optimizing Documentation](https://docs.oracle.com/en/graalvm/enterprise/21/docs/graalvm-as-a-platform/language-implementation-framework/Optimizing/)

### When to Use

Language developers should employ this tool during interpreter development to proactively catch optimization barriers, particularly when implementing new language features or AST node types. The recommended workflow begins with profiling to identify hot methods using tools like `--cpusampler`, followed by enabling `--compiler.TracePerformanceWarnings=all` for those specific methods. After warnings are identified, developers should create minimal reproductions of the problematic patterns, use IGV dumps to visualize the compilation graphs, and implement fixes using Truffle DSL specializations, boundary annotations, or cached parameters. The tool proves essential when investigating why compiled code performs poorly despite reaching compilation thresholds, when methods show unexpected deoptimization patterns, or when optimizing existing language implementations for better peak performance.

**Source:** [Oracle GraalVM Optimizing Documentation](https://docs.oracle.com/en/graalvm/enterprise/21/docs/graalvm-as-a-platform/language-implementation-framework/Optimizing/)

## 4. Output Format & Structure

### Output Type

- ✅ **Console/Terminal** - Primary output destination (stdout)
- ❌ **File output** - Not directly supported; redirect stdout for file capture
- ❌ **Graphical output** - Text-based only; use with IGV for visual analysis
- ❌ **Binary data** - Human-readable text format only

### Output Location

| Aspect               | Details                                                                                                      |
| -------------------- | ------------------------------------------------------------------------------------------------------------ |
| **Default Location** | Standard output (stdout) with `[engine]` prefix                                                              |
| **Custom Location**  | Use shell redirection: `command > output.log 2>&1`                                                           |
| **Log Integration**  | General Truffle logging via `--log.file=<path>` does not capture performance warnings; they remain on stdout |

**Source:** [Oracle GraalVM Optimizing Documentation](https://docs.oracle.com/en/graalvm/enterprise/21/docs/graalvm-as-a-platform/language-implementation-framework/Optimizing/)

### Output Format Example

**Virtual Call Warning (from official documentation):**

```
[engine] perf warn ScaleConstraint.execute |Partial evaluation could not inline the virtual runtime call Virtual to HotSpotMethod<ConditionProfile.profile(boolean)> (167|MethodCallTarget).

Approximated stack trace for [167 | MethodCallTarget]:
  at com.oracle.truffle.js.nodes.control.IfNode.execute(IfNode.java:158)
  at com.oracle.truffle.js.nodes.binary.DualNode.execute(DualNode.java:125)
  at com.oracle.truffle.js.nodes.function.FunctionBodyNode.execute(FunctionBodyNode.java:73)
  at com.oracle.truffle.js.nodes.function.FunctionRootNode.executeInRealm(FunctionRootNode.java:147)
  at com.oracle.truffle.js.runtime.JavaScriptRealmBoundaryRootNode.execute(JavaScriptRealmBoundaryRootNode.java:93)
  at org.graalvm.compiler.truffle.runtime.OptimizedCallTarget.executeRootNode(OptimizedCallTarget.java:503)
  at org.graalvm.compiler.truffle.runtime.OptimizedCallTarget.profiledPERoot(OptimizedCallTarget.java:480)
```

**Annotations:**
- `[engine] perf warn` - Identifies message type and source
- `ScaleConstraint.execute` - Compilation unit (root method being compiled)
- `(167|MethodCallTarget)` - Graal IR node ID (searchable in IGV using `id=167`)
- Stack trace - Shows Truffle node execution path with source locations

**Source:** [Oracle GraalVM Optimizing Documentation](https://docs.oracle.com/en/graalvm/enterprise/21/docs/graalvm-as-a-platform/language-implementation-framework/Optimizing/)

### Output Fields/Metrics

| Field               | Description                     | Unit     | Interpretation                                                                    |
| ------------------- | ------------------------------- | -------- | --------------------------------------------------------------------------------- |
| **Prefix**          | `[engine] perf warn` identifier | N/A      | Distinguishes performance warnings from other engine messages                     |
| **Method Name**     | Compilation unit identifier     | String   | The root Truffle CallTarget being compiled; focus optimization efforts here       |
| **Warning Type**    | Implicit from message text      | Category | Identifies which optimization failed (call/instanceof/store/frame_merge/trivial)  |
| **Target**          | Problematic method/operation    | String   | The specific method call, type check, or store operation that cannot be optimized |
| **Node ID**         | Graal IR node identifier        | Integer  | Use in IGV to locate exact graph node; format: `(id\|NodeType)`                   |
| **Stack Trace**     | Approximated call hierarchy     | Lines    | Shows path through Truffle AST nodes; includes file:line when available           |
| **Source Location** | File and line number            | String   | Exact source code location causing the warning (when debug info available)        |

## 5. Insights & Analysis

### When Should You Use It?

Language developers should deploy this tool when diagnosing unexpected slow performance after warmup periods, as it reveals why compiled code fails to achieve expected peak performance. The tool proves essential during development of new Truffle language implementations, helping developers understand which coding patterns work well with partial evaluation and which create optimization barriers. When investigating methods that remain in interpreter mode despite being hot, or when compiled code shows performance worse than expected, this tool identifies the specific AST node patterns preventing effective compilation. It serves as a preventative measure during code review, catching performance regressions before they reach production, and as a teaching tool for teams learning Truffle best practices.

**Source:** [Oracle GraalVM Optimizing Documentation](https://docs.oracle.com/en/graalvm/enterprise/21/docs/graalvm-as-a-platform/language-implementation-framework/Optimizing/)

### Actionable Insights

#### Performance Issue: Virtual Runtime Calls (call)

**Symptom in Output:**
```
Partial evaluation could not inline the virtual runtime call Virtual to HotSpotMethod<ConditionProfile.profile(boolean)>
```

**Root Cause:** Virtual method calls occur when the compiler cannot determine the exact method target at compile time due to polymorphism, interface calls, or insufficient type information. Profile objects like `ConditionProfile` or `BranchProfile` that should be compilation constants may be accessed through non-constant references, preventing their methods from being inlined. The partial evaluator treats these as opaque calls that must remain in compiled code.

**Resolution:** Apply `@TruffleBoundary` annotation to methods that should explicitly not be inlined (typically I/O, logging, or complex library calls). Use Truffle DSL's `@Specialization` to create type-specific code paths that allow static dispatch. Employ `@Cached` parameters to cache method targets or profile objects, ensuring they become compilation constants. For example, cache profile creation: `@Cached("createBinaryProfile()") ConditionProfile profile`. Verify that specialization guards are constant-foldable to enable profile specialization.

**Verification Tool:** Use `--engine.TraceInlining` to confirm the method is now being inlined after fixes are applied.

**Source:** [Oracle GraalVM Optimizing Documentation](https://docs.oracle.com/en/graalvm/enterprise/21/docs/graalvm-as-a-platform/language-implementation-framework/Optimizing/)

#### Performance Issue: Unresolved Type Checks (instanceof)

**Symptom in Output:**
```
Partial evaluation could not resolve virtual instanceof to an exact type
```

**Root Cause:** Type information remains polymorphic at the point of instanceof checks, preventing the compiler from eliminating runtime type checks. This occurs when multiple types flow through the same code path without proper specialization, when guards fail to narrow types sufficiently, or when type profiles haven't converged to stable patterns.

**Resolution:** Add type-specific specializations using DSL guards: `@Specialization(guards = "isString(value)")`. Cache type information with `@Cached("value.getClass()") Class<?> cachedClass`. For dynamic objects, implement shape-based checks that can be constant-folded. Use `@Cached` with `limit` parameter to handle limited polymorphism: `@Specialization(limit = "3")`. Consider splitting call sites using Truffle's splitting mechanism to create monomorphic copies.

**Verification Tool:** Use `--engine.CompilationStatistics` to verify that specializations are activating correctly and polymorphism is reduced.

**Source:** [Oracle GraalVM Optimizing Documentation](https://docs.oracle.com/en/graalvm/enterprise/21/docs/graalvm-as-a-platform/language-implementation-framework/Optimizing/)

#### Performance Issue: Non-Constant Store Locations (store)

**Symptom in Output:**
```
Store location argument is not a partial evaluation constant
```

**Root Cause:** Frame slot indices or object property locations cannot be determined at compile time, forcing dynamic lookup at runtime. This happens with computed frame slot access, variable property names, or frame descriptors that aren't compilation constants. The compiler cannot optimize these into direct memory accesses.

**Resolution:** Ensure `FrameSlot` objects are created during parsing and stored as compilation constants in AST nodes. Use final fields to store frame slots so they become partial evaluation constants. For property access, implement specialized nodes for known property names and cache property locations in stable object shapes. Avoid dynamic frame slot lookup methods; instead, pass slot identifiers as compilation constants through node constructors.

**Verification Tool:** Use IGV dumps (`--vm.Dgraal.Dump=Truffle:1`) to verify that frame access nodes use constant slot indices in the compiled graph.

**Source:** [Oracle GraalVM Optimizing Documentation](https://docs.oracle.com/en/graalvm/enterprise/21/docs/graalvm-as-a-platform/language-implementation-framework/Optimizing/)

### Correlation with Other Tools

This tool integrates into a comprehensive performance analysis workflow with complementary Truffle diagnostics. Use `--engine.TraceCompilation` alongside performance warnings to correlate which compilation units succeed or fail and whether warnings correspond to compilation bailouts. Combine with `--engine.TraceInlining` to understand the relationship between inlining decisions and call warnings—methods that trigger call warnings should appear as "not inlined" in the inlining tree. The `--cpusampler` profiler identifies which methods are hot and warrant investigation, directing focus to performance warnings in those specific methods. IGV dumps (`--vm.Dgraal.Dump=Truffle:1`) provide visual representation of the problematic IR nodes, using the node IDs from warning messages to locate exact graph positions. The `--engine.CompilationStatistics` option offers aggregate metrics that contextualize individual warnings within overall compilation health. Together, these tools form a complete diagnostic suite where performance warnings highlight specific problems while other tools provide context, verification, and visual understanding of optimization failures.

**Source:** [Oracle GraalVM Optimizing Documentation](https://docs.oracle.com/en/graalvm/enterprise/21/docs/graalvm-as-a-platform/language-implementation-framework/Optimizing/)

## 6. Additional Notes

### Overhead

The performance overhead of enabling this option is minimal when disabled (the default state) and low when enabled, as warnings are generated only during compilation, not during interpreted or compiled execution. The tool adds no runtime overhead to executing compiled code and does not affect the quality of generated code—it purely observes and reports. Compilation time may increase by approximately 1-2% for large compilation units due to additional tracking overhead, but this is negligible for development scenarios. In production deployments, the option should remain disabled since compilation warnings provide no runtime benefit and the console output would be unnecessary overhead. The memory footprint is minimal as warnings are immediately printed rather than accumulated.

**Source:** Based on internal implementation analysis from oracle/graal source code

### Limitations

Stack traces provided with warnings are explicitly "approximated" and may not be 100% accurate, particularly in complex inlining scenarios where multiple layers of method calls are involved. The tool can generate overwhelming output for large applications with many compilation units, necessitating use of `--engine.CompileOnly` to focus on specific methods. No mechanism exists to suppress individual warnings selectively—developers must enable or disable entire warning categories. The option is marked as `OptionCategory.INTERNAL`, indicating it is primarily for debugging and may change between GraalVM versions without deprecation notices. Code must reach compilation thresholds to trigger warnings, meaning cold code paths won't generate warnings even if they contain problematic patterns. Not all warnings necessarily indicate critical performance issues; some operations legitimately cannot be optimized, such as genuine I/O operations or intentionally polymorphic dispatch points.

**Source:** [GraalVM Options Documentation](https://www.graalvm.org/latest/graalvm-as-a-platform/language-implementation-framework/Options/)

### Best Practices

Enable this tool regularly during development cycles to catch optimization barriers early, before they become entrenched in the codebase. Focus remediation efforts on warnings in hot paths identified by profiling, as not all warnings warrant fixing—cold code can tolerate suboptimal patterns. Combine with `--engine.CompileOnly=<method>` to reduce noise and concentrate on specific problematic methods. Document known acceptable warnings that represent unavoidable patterns (like necessary I/O operations) to prevent repeatedly investigating them. Use the tool in conjunction with IGV for visual debugging of complex optimization failures, searching for node IDs from warnings in the graph visualization. Establish a baseline of expected warnings during development and use `--engine.TreatPerformanceWarningsAsErrors` in CI/CD pipelines to prevent regressions. Always verify performance improvements with benchmarks after addressing warnings—fixing warnings doesn't guarantee performance gains, and measurements confirm the impact.

**Source:** [Oracle GraalVM Optimizing Documentation](https://docs.oracle.com/en/graalvm/enterprise/21/docs/graalvm-as-a-platform/language-implementation-framework/Optimizing/)

### Common Pitfalls

Developers often over-optimize by adding `@TruffleBoundary` annotations everywhere to eliminate call warnings, but this actually reduces optimization opportunities by preventing inlining of potentially beneficial methods. Misinterpreting the approximated stack traces leads to fixing the wrong code locations—careful analysis with IGV helps confirm the actual problematic node. Some developers attempt to fix warnings in cold code paths that never compile, wasting effort on optimization that provides no benefit. Ignoring the compilation context causes confusion when warnings don't appear during testing because the code hasn't reached compilation thresholds—use `--engine.CompileImmediately` for testing. Not measuring performance before and after changes results in addressing warnings that have no actual impact on runtime speed. Combining too many diagnostic options simultaneously creates overwhelming output that obscures the relevant information. Finally, assuming all warnings must be eliminated is counterproductive; some language features inherently require patterns that trigger warnings.

### Related Tools

The `--engine.TraceCompilation` option shows compilation events including success, failure, and timing, helping correlate warnings with compilation outcomes. The `--engine.TraceInlining` option displays guest-language inlining decisions and should be used to verify that fixing call warnings actually results in successful inlining. The `--engine.CompilationStatistics` option provides aggregate metrics including compilation counts, bailouts, and timing statistics that contextualize individual warnings. The `--engine.TraceSplitting` option shows call target splitting decisions, which can resolve instanceof warnings by creating monomorphic copies. The `--engine.TraceDeoptimizeFrame` and `--engine.TraceTransferToInterpreter` options track deoptimization events that may correlate with performance warnings. The `--cpusampler` tool identifies hot methods where warnings matter most. IGV (Ideal Graph Visualizer) with `--vm.Dgraal.Dump=Truffle:1` visualizes IR graphs for deep analysis. The companion `--engine.TreatPerformanceWarningsAsErrors` option enforces warning-free compilation as a quality gate.

**Source:** [GraalVM Options Documentation](https://www.graalvm.org/latest/graalvm-as-a-platform/language-implementation-framework/Options/)

## 7. Resources

### Official Documentation

- [GraalVM Language Implementation Framework Options](https://www.graalvm.org/latest/graalvm-as-a-platform/language-implementation-framework/Options/) - Complete reference for all Truffle engine options
- [Optimizing Truffle Interpreters](https://www.graalvm.org/latest/graalvm-as-a-platform/language-implementation-framework/Optimizing/) - Official guide on performance optimization with example outputs
- [Oracle GraalVM Enterprise Documentation - Optimizing](https://docs.oracle.com/en/graalvm/enterprise/21/docs/graalvm-as-a-platform/language-implementation-framework/Optimizing/) - Enterprise edition documentation with practical examples

### GitHub Repository

- [oracle/graal - Truffle Options.md](https://github.com/oracle/graal/blob/master/truffle/docs/Options.md) - Technical specifications and implementation details
- [oracle/graal - Truffle CHANGELOG.md](https://github.com/oracle/graal/blob/master/truffle/CHANGELOG.md) - Historical evolution of options and warning types
- [oracle/graal - SDK CHANGELOG.md](https://github.com/oracle/graal/blob/master/sdk/CHANGELOG.md) - Deprecation notices and migration guides

### Source Code

- PolyglotCompilerOptions.java in compiler/org.graalvm.compiler.truffle.options/ - PerformanceWarningKind enum definitions
- Oracle/graal GitHub repository - Implementation details and code comments
