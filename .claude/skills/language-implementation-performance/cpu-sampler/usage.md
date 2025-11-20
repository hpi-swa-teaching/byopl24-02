# Usage


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
