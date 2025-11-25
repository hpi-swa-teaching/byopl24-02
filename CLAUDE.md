# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Lox language implementation using the GraalVM Truffle framework. Lox is a dynamically-typed, object-oriented language implemented as part of the "Build Your Own Programming Language" course at HPI Potsdam. The implementation uses ANTLR for parsing and Truffle's bytecode DSL for execution.

## Build and Development Commands

### Building and Testing
```bash
# Compile the project
./mvnw package

# Run tests
./mvnw test

# Clean build artifacts
./mvnw clean

# Run with Maven (for inline code execution)
./mvnw exec:java -Dexec.args="-c 'print true;'"
```

### Running Lox Programs
```bash
# Basic execution
./lox <program.lox>

# Run with CPU profiling (sampling)
./lox --cpusampler <program.lox>

# Run with CPU tracing (exact call counts)
./lox --cputracer <program.lox>

# Generate flamegraph
./lox --cpusampler --cpusampler.Output=flamegraph --cpusampler.OutputFile=flamegraph.svg <program.lox>

# Debug mode (Java debugger)
./debuglox <program.lox>

# Debug mode (DAP for Lox)
./lox --dap <program.lox>
```

### Benchmarking
```bash
# Standard benchmark format
./lox harness.lox <benchmark-name> <num-iterations> <inner-iterations>

# Examples
./lox harness.lox sieve 10 5000
./lox harness.lox towers 10 13
./lox harness.lox permute 10 6
./lox harness.lox list 10 18
```

### Performance Analysis Commands
The repository includes comprehensive Graal/Truffle tracing capabilities. Use `EXTRA_JAVA_ARGS` environment variable to pass JVM flags:

```bash
# Dump compiler graphs for analysis
EXTRA_JAVA_ARGS="-Djdk.graal.Dump=Truffle:1 -Djdk.graal.PrintGraph=File -Djdk.graal.DumpPath=compiler_graphs" ./lox <program.lox>

# Trace inlining decisions
./lox --experimental-options --traceInlining <program.lox>

# Trace compilation
./lox --experimental-options --traceCompilation <program.lox>

# Performance warnings
./lox --experimental-options --tracePerformanceWarnings=all <program.lox>
```

See `docs/commands/` for detailed documentation on CPU sampler, tracer, compiler graph analysis, and other Truffle profiling tools.

## Language-Specific Syntax

Lox in this implementation has custom syntax deviations from the standard:

### Arrays
Arrays use special emoji syntax:
```lox
var a = 👉👈;           // Empty array
a👉0👈 = 1;             // Array assignment
print a👉0👈;           // Array access
print a;               // Prints 👉1👈
```

Array literals are supported:
```lox
var arr = 👉1, 2, 3👈;
```

### Self Instead of This
Uses `self` instead of `this` for object context:
```lox
class A {
  printA() {
    print self.a;
  }
}
```

### Class Inheritance
Uses handshake emoji for inheritance:
```lox
class Derived 🤝 Base {
  // class body
}
```

### For Loop Condition Required
For loops require a condition (no infinite loops, as `break` is not provided):
```lox
for (var i = 0; i < 10; i = i + 1) {
  print i;
}
```

Empty init and increment are allowed:
```lox
for (; condition; ) {
  // body
}
```

### Array Iteration
For-of and for-in loops are supported:
```lox
for (var elem of array) { }    // iterate values
for (var idx in array) { }     // iterate indices
```

## Architecture

### Core Components

**LoxLanguage** (`src/main/java/de/hpi/swa/lox/LoxLanguage.java`)
- Main Truffle language registration class
- Defines built-in functions: `clock`, `Number`, `String`, `load`, `round`, `lookup`
- Creates and manages `LoxContext`

**Parser** (`src/main/antlr4/de/hpi/swa/lox/parser/Lox.g4` and `src/main/java/de/hpi/swa/lox/parser/`)
- ANTLR4 grammar defines Lox syntax
- `LoxBytecodeCompiler` translates ANTLR parse trees to Truffle bytecode using Truffle's bytecode DSL
- Bytecode-based execution for performance

**Nodes** (`src/main/java/de/hpi/swa/lox/nodes/`)
- Truffle nodes implement language operations
- Use Truffle DSL annotations for specialization and type profiling
- Built-in nodes handle standard library functions
- Key nodes: `LoxCallFunctionNode`, `LoxReadPropertyNode`, `LoxWritePropertyNode`, `LoxLookupMethodNode`

**Runtime** (`src/main/java/de/hpi/swa/lox/runtime/`)
- `LoxContext`: Per-context state and environment
- `LoxDisplay`: Display management for graphical programs
- `LoxRuntimeError`: Exception handling

**Runtime Data Types** (`src/main/java/de/hpi/swa/lox/runtime/data/`)
- `LoxFunction`: Functions with their call targets and closure frames
- `LoxObject`: Object instances with property storage
- `LoxClass`: Class definitions
- `LoxArray`: Array implementation with interop support
- `LoxNumber`: Numeric type wrapper with specialized storage
- `GlobalObject`: Global scope storage
- `Nil`: Null value representation

**Bytecode** (`src/main/java/de/hpi/swa/lox/bytecode/`)
- `LoxBytecodeRootNode`: Root node for bytecode-based execution using Truffle's bytecode DSL

### Execution Model

1. Source code is parsed by ANTLR-generated parser
2. `LoxBytecodeCompiler` walks the parse tree and generates Truffle bytecode
3. Truffle executes bytecode with support for:
   - Specialization (type-specific optimizations)
   - Inlining (function call elimination)
   - Partial evaluation (compile-time computation)
   - On-stack replacement (optimization of running code)
4. GraalVM JIT compiles hot paths to native code

### Testing

Tests are located in `src/test/java/de/hpi/swa/lox/test/`. Key test classes:
- `AbstractLoxTest`: Base class for all Lox tests with helper methods
- Domain-specific tests: `ArithmeticExpressionTest`, `ControlFlowTest`, `ClassTest`, `FunctionTest`, `ArrayTest`, etc.
- `InteropTest`: Tests polyglot interoperability features

## Development Requirements

- **Java Version**: JDK 21 (Oracle JDK, OpenJDK, or derivatives)
- **Recommended Runtime**: GraalVM for JDK 21 version 21.0.4 for best performance
- **Build Tool**: Maven (use included `./mvnw` wrapper)
- **GraalVM Version**: 24.2.0-SNAPSHOT (configured in pom.xml)

## Benchmark Programs

Available in repository root:
- `sieve.lox`: Sieve of Eratosthenes
- `primeCount.lox`: Prime counting
- `queens.lox`: N-queens problem
- `towers.lox`: Towers of Hanoi
- `permute.lox`: Permutation generation
- `list.lox`: Linked list operations
- `harness.lox`: Benchmark harness that loads and runs benchmarks from `run.lox`

## Migration Tools

For converting standard Lox programs to this implementation's syntax:
- `migration_boring_lox_to_amazing_lox.sh` (Linux/Windows)
- `migration_boring_lox_to_amazing_lox_macos.sh` (macOS)

## Bonus Features Implemented

- C3.2: REPL evaluates and prints expressions
- C4.3: For-of/for-in loops for array iteration
- C4.4: Array literals
- C9.1: Benchmark for array literals with documentation
- C9.2: Optimizations for array literals
- C10.2: Array interoperability (polyglot integration)
