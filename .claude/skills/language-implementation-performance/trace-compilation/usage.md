# Usage


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
