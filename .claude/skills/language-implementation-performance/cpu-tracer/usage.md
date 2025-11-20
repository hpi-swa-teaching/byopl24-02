# Usage


## 1. Command Execution

### Basic Command
```bash
<language-launcher> --cputracer <script>
```

### Full Command Syntax
```bash
# Complete syntax with all available options
<language-launcher> <script> --cputracer \
  --cputracer.TraceStatements \
  --cputracer.TraceCalls \
  --cputracer.TraceRoots=true|false \
  --cputracer.FilterRootName=<pattern> \
  --cputracer.FilterFile=<pattern> \
  --cputracer.FilterLanguage=<languageId> \
  --cputracer.FilterMimeType=<mime-type> \
  --cputracer.Output=histogram|json \
  --cputracer.OutputFile=<path>

# Examples
js primes.js --cputracer
js app.js --cputracer --cputracer.TraceStatements --cputracer.FilterRootName=*parse*
js script.js --cputracer --cputracer.Output=json --cputracer.OutputFile=trace.json
```

### Execution Context

- **JVM Mode Compatibility**: Yes
  - **Requirements**: GraalVM with Truffle-based language runtime
  - **Access**: Standard command-line flag or Polyglot API
  
- **Native Image Mode Compatibility**: Yes
  - **Requirements**: Language compiled to native executable with Truffle support
  - **Functionality**: Full feature parity with JVM mode
  
- **Prerequisites**:
  - GraalVM installation (Community or Enterprise Edition)
  - Truffle-based language (GraalJS, GraalPy, TruffleRuby, FastR, GraalWasm)
  - No additional configuration files needed
  - Built-in Truffle instrument, no separate installation required

## 2. Command Options & Parameters

### Key Options

| Option                        | Type                  | Default   | Description                                                                |
| ----------------------------- | --------------------- | --------- | -------------------------------------------------------------------------- |
| `--cputracer`                 | Boolean flag          | false     | Enable the CPU tracer instrument                                           |
| `--cputracer.TraceRoots`      | Boolean               | true      | Trace function/method entries (root executions)                            |
| `--cputracer.TraceCalls`      | Boolean               | false     | Trace call sites within functions                                          |
| `--cputracer.TraceStatements` | Boolean               | false     | Trace individual statements (most detailed granularity)                    |
| `--cputracer.Output`          | Enum (histogram/json) | histogram | Output format: histogram (human-readable table) or json (machine-readable) |
| `--cputracer.OutputFile`      | String (path)         | stdout    | Redirect output from stdout to specified file path                         |

### Advanced Options

| Option                       | Type                      | Default   | Description                                                                  |
| ---------------------------- | ------------------------- | --------- | ---------------------------------------------------------------------------- |
| `--cputracer.FilterFile`     | String (wildcard pattern) | no filter | Filter by source file path (e.g., `program.sl`, `*.js`, `app/*`)             |
| `--cputracer.FilterLanguage` | String (languageId)       | no filter | Filter by language ID (e.g., `js`, `python`, `ruby`, `R`)                    |
| `--cputracer.FilterMimeType` | String (mime-type)        | no filter | Filter by MIME type (e.g., `application/javascript`, `text/x-python`)        |
| `--cputracer.FilterRootName` | String (wildcard pattern) | no filter | Filter by function/method name pattern (e.g., `Math.*`, `*accept`, `parse*`) |

**Advanced Option Details**:

- **Wildcard Patterns**: All filter options support `*` wildcard for pattern matching. For example, `Math.*` matches all functions starting with "Math.", `*parse*` matches functions containing "parse", and `*.js` matches all JavaScript files.

- **Granularity Hierarchy**: TraceRoots provides function-level data, TraceCalls adds call-site information within functions, and TraceStatements provides statement-level detail. These can be combined, with overhead increasing at each level.

- **Filter Combination**: Multiple filters can be applied simultaneously and work as logical AND—for example, combining FilterLanguage=js with FilterFile=*.js and FilterRootName=Math.* traces only JavaScript Math functions in .js files.
