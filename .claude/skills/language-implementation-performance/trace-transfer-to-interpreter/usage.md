# Usage


## 1. Command Execution

### Basic command syntax

```bash
<language-launcher> --experimental-options --engine.TraceTransferToInterpreter <script>
```

### Full command syntax with flags

```bash
<language-launcher> --experimental-options --engine.TraceTransferToInterpreter \
  [--engine.CompileOnly=<name-list>] \
  [--engine.CompileImmediately] \
  [--engine.BackgroundCompilation=<boolean>] \
  [--engine.FirstTierCompilationThreshold=<N>] \
  [--engine.LastTierCompilationThreshold=<N>] \
  [--engine.TraceStackTraceLimit] \
  [--engine.NodeSourcePositions] \
  <script>
# JavaScript example
js --experimental-options --engine.TraceTransferToInterpreter myapp.js
```

### Execution context

| Mode                  | Supported | Requirements                                                          |
| --------------------- | --------- | --------------------------------------------------------------------- |
| **JVM Mode**          | Yes       | `--experimental-options` flag required                                |
| **Native Image Mode** | Yes       | `-H:+IncludeNodeSourcePositions` build flag for accurate stack traces |

### Prerequisites

- **Required flag**: `--experimental-options` must be enabled (internal/expert option)
- **For Native Image stack traces**: Build with `-H:+IncludeNodeSourcePositions` flag (disabled by default to reduce image size)
- **GraalVM version**: Available in all modern GraalVM versions (0.8 or later)

---

## 2. Command Options & Parameters

### Core oOption

| Option Name                           | Type    | Default Value | Description                                                   |
| ------------------------------------- | ------- | ------------- | ------------------------------------------------------------- |
| `--engine.TraceTransferToInterpreter` | Boolean | `false`       | Enable tracing of transfers from compiled code to interpreter |