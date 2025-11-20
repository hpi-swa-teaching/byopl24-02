# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Lox language implementation built with GraalVM Truffle as part of the "Build Your Own Programming Language" course at HPI. The implementation uses a bytecode interpreter with Truffle's DSL and bytecode generation features for performance optimization.

## Building and Testing

### Build
```bash
./mvnw package
```

### Run Tests
```bash
./mvnw test
```

### Run a Single Test
```bash
./mvnw test -Dtest=ClassNameTest
```

### Clean Build Artifacts
```bash
./mvnw clean
```

## Running Lox Programs

### Using the ./lox Wrapper Script
The `./lox` script is the primary way to run Lox programs:
```bash
./lox <program.lox>
./lox -c 'print true;'
```

The script automatically builds the classpath using Maven and caches it in `.script-classpath`. Set `EXTRA_JAVA_ARGS` environment variable to pass JVM options:
```bash
EXTRA_JAVA_ARGS="-Djdk.graal.TraceTruffleCompilation=true" ./lox harness.lox sieve 10 5000
```

### Using Maven Directly
```bash
./mvnw exec:java -Dexec.args="-c 'print true;'"
```

### Debugging
```bash
./debuglox <program>  # Then attach Java debugger in VSCode
./lox --dap <program>  # Debug Lox via DAP
```

## Performance Analysis and Optimization

This project includes comprehensive tooling for analyzing and optimizing Truffle language implementation performance. See the hierarchical skill at `.claude/skills/language-implementation-performance/SKILL.md` for the complete workflow.

### Benchmarking
Run benchmarks using the harness:
```bash
./lox harness.lox <benchmark> <num-iterations> <inner-iterations>
./lox harness.lox sieve 10 5000
./lox harness.lox towers 10 13
./lox harness.lox permute 10 6
./lox harness.lox list 10 18
```

Available benchmark programs: `sieve.lox`, `towers.lox`, `permute.lox`, `list.lox`, `queens.lox`

### Profiling Tools
```bash
./lox --cpusampler <program>                    # Time-based sampling profiler
./lox --cputracer <program>                     # Execution frequency counter
./lox --cpusampler --cpusampler.Output=flamegraph --cpusampler.OutputFile=flamegraph.svg <program>
```

### Tracing Compilation
```bash
EXTRA_JAVA_ARGS="-Djdk.graal.TraceTruffleCompilation=true" ./lox <program>
EXTRA_JAVA_ARGS="-Djdk.graal.TraceTruffleInlining=true" ./lox <program>
EXTRA_JAVA_ARGS="-Djdk.graal.TraceTruffleTransferToInterpreter=true" ./lox <program>
EXTRA_JAVA_ARGS="-Djdk.graal.TraceTrufflePerformanceWarnings=true" ./lox <program>
```

### Compiler Graph Analysis
To dump compiler graphs for deep IR analysis:
```bash
EXTRA_JAVA_ARGS="-Djdk.graal.Dump=Truffle:1 -Djdk.graal.PrintGraph=File -Djdk.graal.DumpPath=compiler_graphs" ./lox <program>
```

Use `bgv2json` to convert graphs to JSON for analysis.

## Architecture

### Core Components

#### Bytecode System (`src/main/java/de/hpi/swa/lox/bytecode/`)
- `LoxBytecodeRootNode.java`: Main bytecode interpreter using Truffle's `@GenerateBytecode` annotation
  - Defines custom operations (`@Operation`) for Lox semantics (print, arithmetic, truthiness, etc.)
  - Implements boxing elimination for `long` types
  - Uses short-circuit operations for `and`/`or` with custom truthy conversion

#### Parser (`src/main/java/de/hpi/swa/lox/parser/`)
- `Lox.g4`: ANTLR4 grammar defining Lox syntax
- `LoxBytecodeCompiler.java`: Compiles ANTLR parse tree to Truffle bytecode
  - Implements `BytecodeParser` interface
  - Handles variable scoping with local/global distinction
  - Generates bytecode for all Lox constructs (classes, functions, loops, etc.)

#### Runtime (`src/main/java/de/hpi/swa/lox/runtime/`)
- `LoxContext.java`: Language context holding global state and builtins
- `data/`: Runtime data types (`LoxFunction`, `LoxObject`, `LoxClass`, `LoxArray`, `LoxNumber`, `GlobalObject`, `Nil`)

#### Nodes (`src/main/java/de/hpi/swa/lox/nodes/`)
- Built-in functions: `ClockBuiltInNode`, `NumberBuiltInNode`, `StringBuiltInNode`, `MathRoundBuiltinNode`, `LoadBuiltInNode`, `LookupValueBuiltInNode`
- Object operations: `LoxReadPropertyNode`, `LoxWritePropertyNode`, `LoxLookupMethodNode`, `LoxCallFunctionNode`
- Uses Truffle DSL (`@Specialization`) for type specialization

#### Language Entry Point
- `LoxLanguage.java`: Truffle language registration and initialization
- `cli/LoxMain.java`: Command-line interface

### Build Process

Maven compiles in this order:
1. ANTLR4 generates parser/lexer from `src/main/antlr4/de/hpi/swa/lox/parser/Lox.g4`
2. Truffle DSL annotation processor generates specialized node implementations (e.g., `*NodeGen.java`)
3. Bytecode generator creates `LoxBytecodeRootNodeGen.java` from `@GenerateBytecode` annotation
4. Java compiler compiles all sources

Generated sources appear in:
- `target/generated-sources/antlr4/` (ANTLR)
- `target/generated-sources/annotations/` (Truffle DSL and bytecode)

## Lox Language Syntax Quirks

### Arrays
Uses emoji syntax:
```lox
var a = 👉👈;         // Empty array
a👉0👈 = 1;           // Array assignment
print a👉0👈;         // Array access (prints 1)
var b = 👉1,2,3👈;   // Array literal (Bonus C4.4)
```

### Object References
Uses `self` instead of `this`:
```lox
class A {
  printA() {
    print self.a;
  }
}
```

### For Loops
Conditions are required (no `break` statement exists):
```lox
for (var i = 0; i < 10; i = i + 1) {
  print i;
}
```

Empty init/increment are allowed:
```lox
for (; condition;) { ... }
```

### For-in/For-of Loops (Bonus C4.3)
Array iteration supported:
```lox
for (item in array) { ... }
for (item of array) { ... }
```

### Migration Tools
Convert standard Lox to emoji syntax:
- `migration_boring_lox_to_amazing_lox.sh` (Linux/other)
- `migration_boring_lox_to_amazing_lox_macos.sh` (macOS)

## Development Requirements

- **Development**: Oracle JDK 21, OpenJDK 21, or derivatives
- **Performance**: GraalVM for JDK 21 version 21.0.4+ recommended
- **Dependencies**: Uses custom GraalVM snapshot repository (see pom.xml)

## Bonus Challenges Implemented

- **C3.2**: REPL prints evaluated expressions
- **C4.3**: for-of/for-in loops for arrays
- **C4.4**: Array literals (`👉1,2,3👈`)
- **C9.1**: Benchmark for array literals
- **C9.2**: Optimization for array literals
- **C10.2**: Array interoperability

## Project Structure

```
src/main/java/de/hpi/swa/lox/
├── LoxLanguage.java              # Language registration
├── bytecode/
│   └── LoxBytecodeRootNode.java  # Bytecode interpreter with operations
├── cli/
│   └── LoxMain.java              # Entry point
├── nodes/                        # Truffle nodes (builtins, operations)
├── parser/
│   ├── LoxBytecodeCompiler.java  # Bytecode generation from AST
│   └── LoxParseError.java
└── runtime/
    ├── LoxContext.java           # Global context
    └── data/                     # Runtime types

src/main/antlr4/de/hpi/swa/lox/parser/
└── Lox.g4                        # ANTLR grammar

src/test/java/de/hpi/swa/lox/test/
└── *Test.java                    # JUnit tests (18 test classes)
```

## Key Implementation Notes

- **Bytecode Interpreter**: Uses Truffle's experimental bytecode DSL (`@GenerateBytecode`) instead of traditional AST interpreter
- **Custom Operations**: All Lox semantics (print, arithmetic, property access, etc.) defined as `@Operation` inner classes
- **Boxing Elimination**: Enabled for `long` primitives to reduce allocation overhead
- **Materialized Frames**: Enabled to support closures and lexical scoping
- **Serialization**: Bytecode serialization enabled for potential ahead-of-time compilation
- **Instrumentation**: Tagged with StandardTags for debugging and profiling support
