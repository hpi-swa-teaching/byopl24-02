# Usage


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
