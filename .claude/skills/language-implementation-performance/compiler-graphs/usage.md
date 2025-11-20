# Usage


## 1. Command Execution

### Basic Command

**JVM mode with Graal JIT compiler:**
```bash
# Modern syntax (JDK 22+)
java -Djdk.graal.Dump=:2 \
     -Djdk.graal.MethodFilter=MyClass.myMethod \
     -Djdk.graal.DumpPath=/tmp/dumps \
     Application
```

**Truffle language launchers:**
```bash
<language-launcher> --vm.Djdk.graal.Dump=Truffle:1 \
   <script>

# Example Ruby with compilation control
ruby --experimental-options \
     --engine.CompileOnly=fib \
     --engine.Inlining=false \
     --vm.Djdk.graal.Dump=Truffle:1 \
     fib.rb
```

**Native Image compilation:**
```bash
# During image build (hosted options)
native-image -H:Dump=:1 \
             -H:MethodFilter=ClassName.methodName \
             -H:DumpPath=/path/to/dumps \
             MyApplication

# Runtime dynamic compilation (runtime options)
./myapp -XX:Dump=:2 \
        -XX:MethodFilter=ClassName.methodName
```

### Full Command Syntax

```bash
# JVM Mode (Modern - JDK 22+)
java -Djdk.graal.Dump=<scope_pattern>:<level> \
     -Djdk.graal.MethodFilter=<pattern> \
     -Djdk.graal.DumpPath=<path> \
     -Djdk.graal.PrintGraph=<Network|File> \
     -Djdk.graal.PrintGraphHost=<host> \
     -Djdk.graal.PrintGraphPort=<port> \
     -Djdk.graal.CompilationFailureAction=<action> \
     -Djdk.graal.TrackNodeSourcePosition=<true|false> \
     -Djdk.graal.PrintBackendCFG=<true|false> \
     -Djdk.graal.PrintUnmodifiedGraphs=<true|false> \
     Application

# Legacy syntax (JDK 21 and earlier, still supported in JDK 23)
java -Dgraal.Dump=<scope_pattern>:<level> \
     -Dgraal.MethodFilter=<pattern> \
     Application
```

### Execution Context

- **JVM Mode:** Yes - works with Graal JIT compiler enabled. Requires GraalVM or Graal JIT compiler integration
- **Native Image Mode:** Yes - supports both build-time (hosted compilation with `-H:` prefix) and runtime (dynamic compilation with `-XX:` prefix for PGO-enabled images)
- **Prerequisites:**
  - GraalVM installation or Graal JIT compiler integration
  - For JDK 22+: use `jdk.graal.*` prefix; for JDK 21 and earlier: use `graal.*` prefix
  - For Truffle languages: launcher must support `--vm.D` option pass-through
  - For graph visualization: Graphviz installation required for Seafoam rendering
  - For post-processing: Ruby and Seafoam gem (`gem install seafoam`)

---

## 2. Command Options & Parameters

| Option                               | Type                         | Default                         | Description                                                         |
| ------------------------------------ | ---------------------------- | ------------------------------- | ------------------------------------------------------------------- |
| `Djdk.graal.Dump`                    | String (scope_pattern:level) | Empty (disabled)                | Enables graph dumping with scope filter and verbosity level (1-4+)  |
| `Djdk.graal.MethodFilter`            | String (pattern)             | Empty (all methods)             | Filters which methods produce output using wildcards and exclusions |
| `Djdk.graal.DumpPath`                | String (path)                | `$PWD/graal-dumps/<timestamp>/` | Custom output directory for dump files                              |
| `Djdk.graal.PrintGraph`              | String (Network/File)        | File                            | Delivery mechanism: file system or network streaming to IGV         |
| `Djdk.graal.PrintGraphHost`          | String (host)                | localhost                       | Network destination host when PrintGraph=Network                    |
| `Djdk.graal.PrintGraphPort`          | Integer                      | 4445                            | Network destination port when PrintGraph=Network                    |
| `Djdk.graal.TrackNodeSourcePosition` | Boolean                      | false                           | Embeds source location metadata in graph nodes                      |
| `Djdk.graal.PrintBackendCFG`         | Boolean                      | false                           | Enables LIR output for level 3 dumps (viewable in C1Visualizer)     |
| `Djdk.graal.PrintUnmodifiedGraphs`   | Boolean                      | true                            | Controls whether to dump graphs that haven't changed between phases |


### Key Options

- **`Dump=<pattern>:<level>`**: The core option controlling graph dumping behavior. Pattern syntax supports wildcards (`*`), exclusions (`~pattern`), comma-separated lists (`pattern1,pattern2`), and per-scope levels (`Outer:2,Inner:0`). Common patterns:
  - `:1` or `:2` - Dump all scopes at specified level
  - `Truffle:1` - Truffle compilations only (recommended for Truffle languages)
  - `CodeGen:2,Dead:0` - Specific scopes with different verbosity
  - `*,~Dead` - All scopes except Dead code elimination

- **`MethodFilter=<pattern>`**: Essential for limiting output volume in real applications. Supports wildcards and exclusions. Examples:
  - `java.lang.String.*` - All methods in String class
  - `canonical` - Methods named canonical in any class
  - `*Array*.*` - All methods in classes containing "Array"
  - `method1,method2` - Multiple specific methods (OR operator)
  - `java.util.*,~*Array*` - java.util package excluding Array classes

- **`DumpPath=<path>`**: Specifies custom output directory. Defaults to timestamped subdirectory in current working directory. The timestamp format is Unix epoch milliseconds (e.g., `./graal-dumps/1499768882600/`).

- **`TrackNodeSourcePosition=true`**: Critical for correlating IR nodes with guest-language source code. For Truffle languages, use `--engine.NodeSourcePositions` which sets both this option and Truffle-specific tracking. Enables the `seafoam source` command to show inlining call stacks.

---
