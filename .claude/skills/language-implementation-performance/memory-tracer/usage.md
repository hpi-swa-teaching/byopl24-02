# Usage


## 1. Command Execution

### Basic Command
```bash
<language-launcher> --experimental-options --memtracer <script>
```

### Full Command Syntax
```bash
<language-launcher> --experimental-options --memtracer [--memtracer.<option>=<value>] <script-file>

# Examples with different output modes
js --experimental-options --memtracer --memtracer.Output=histogram primes.js 
js --experimental-options --memtracer --memtracer.Output=typehistogram primes.js 
js --experimental-options --memtracer --memtracer.Output=calltree primes.js 

# With tracing options
js --experimental-options --memtracer --memtracer.TraceStatements --memtracer.TraceCalls primes.js

# With filters
js --experimental-options --memtracer --memtracer.FilterRootName="Math.*" --memtracer.FilterFile="*mycode*" script.js
```

### Execution Context

* **JVM Mode**: Yes - Full support with all Truffle-based languages
* **Native Image Mode**: Yes - Available when Truffle languages are embedded (requires Truffle framework)
* **Prerequisites**:
  - GraalVM installation with Truffle framework
  - `--experimental-options` flag (mandatory, tool is experimental)
  - Language implementation must use Truffle AllocationReporter API
  - Not suitable for production (development/debugging only)

## 2. Command Options & Parameters

| Option                        | Type          | Default   | Description                                                                      |
| ----------------------------- | ------------- | --------- | -------------------------------------------------------------------------------- |
| `--experimental-options`      | Boolean       | false     | Required flag to enable experimental tools (mandatory prefix)                    |
| `--memtracer`                 | Boolean       | false     | Enable the Memory Tracer tool                                                    |
| `--memtracer.Output`          | String (enum) | histogram | Output format: `histogram`, `typehistogram`, or `calltree`                       |
| `--memtracer.StackLimit`      | Integer       | 10000     | Maximum number of stack elements to record (range: 1 to ∞)                       |
| `--memtracer.TraceCalls`      | Boolean       | false     | Capture function/method call sites in the trace                                  |
| `--memtracer.TraceRoots`      | Boolean       | true      | Capture allocations at root function level                                       |
| `--memtracer.TraceStatements` | Boolean       | false     | Capture allocations at statement-level granularity                               |
| `--memtracer.TraceInternal`   | Boolean       | false     | Include internal/framework allocations in trace                                  |
| `--memtracer.FilterFile`      | String        | none      | Wildcard filter for source file paths (e.g., `*program*.sl`)                     |
| `--memtracer.FilterLanguage`  | String        | none      | Only profile languages with specified ID (e.g., `js`)                            |
| `--memtracer.FilterMimeType`  | String        | none      | Only profile languages with specified MIME type (e.g., `application/javascript`) |
| `--memtracer.FilterRootName`  | String        | none      | Wildcard filter for program roots/function names (e.g., `Math.*`)                |

### Key Options

**--memtracer.Output**: This option controls the aggregation and presentation format of allocation data. The default `histogram` mode groups allocations by source location (function and line), showing where in the code allocations occur. The `typehistogram` mode groups by the type or class of allocated objects, revealing what kinds of objects are being created most frequently. The `calltree` mode presents allocations in a hierarchical call tree structure, similar to CPU profiler output, showing the allocation patterns within the calling context.

**--memtracer.TraceStatements vs --memtracer.TraceRoots**: These options control granularity of allocation attribution. `TraceRoots` (enabled by default) attributes allocations to root-level functions, providing a high-level view suitable for initial investigation. `TraceStatements` increases granularity to individual statements within functions, enabling precise identification of allocation sources at the cost of more detailed (and potentially overwhelming) output. These options can be combined: enabling both provides full detail while maintaining the high-level summary.

**--memtracer.TraceCalls**: Enabling this option instructs the tool to record not just the immediate location of allocations but also the call sites—which functions called the allocating function. This creates a richer call hierarchy in the output, particularly valuable with `calltree` output mode, as it shows allocation patterns in their full calling context. The additional call site tracking increases overhead but provides crucial information for understanding why certain functions allocate frequently.

### Advanced Options

**Filter Options (FilterFile, FilterLanguage, FilterMimeType, FilterRootName)**: These options enable focused profiling on specific code regions, essential when analyzing large applications where full allocation traces become overwhelming. Filters use wildcard expressions for file names and root names, allowing patterns like `*core*` to match any file containing "core" in its name. Multiple filters can be combined—for instance, filtering by both language and root name to profile only JavaScript functions whose names match a pattern. Effective filtering dramatically reduces overhead and output volume, making the tool practical for larger codebases.

**--memtracer.StackLimit**: This option caps the number of stack frames recorded per allocation event. The default of 10,000 is sufficient for most programs, but deeply recursive algorithms or pathological call stacks might hit this limit. Increasing the limit captures more complete stack information at the cost of higher memory usage for the profiling data structures. Conversely, decreasing the limit on very allocation-heavy workloads can reduce profiler memory overhead, though at the risk of truncating important call stack information.

**--memtracer.TraceInternal**: This advanced option includes allocations from internal framework code and runtime libraries rather than exclusively guest language code. While normally disabled to focus on user code, enabling it helps language implementers understand memory behavior of the language runtime itself, including allocations in built-in libraries, runtime support code, and Truffle framework infrastructure. This option is primarily valuable for language implementers debugging the language implementation rather than application developers optimizing their code.
