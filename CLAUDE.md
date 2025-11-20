# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Lox language implementation built with GraalVM Truffle, developed as part of the Build Your Own Programming Language (BYOPL) course at HPI. The implementation uses the **Truffle Bytecode DSL** for compilation and execution.

## Build and Test Commands

### Maven Wrapper Commands
- **Build**: `./mvnw package`
- **Run all tests**: `./mvnw test`
- **Run single test**: `./mvnw test -Dtest=ClassNameTest` (e.g., `./mvnw test -Dtest=ArrayTest`)
- **Clean**: `./mvnw clean`
- **Execute code**: `./mvnw exec:java -Dexec.args="-c 'print true;'"`

### Direct Execution (after build)
- **Run Lox file**: `./lox <file.lox>`
- **Run with arguments**: `./lox harness.lox <benchmark> <iterations> <inner-iterations>`
- **REPL mode**: `./lox` (without arguments)
- **Debug (Java)**: `./debuglox <file.lox>` (opens port 5005 for debugger)
- **Debug (DAP)**: `./lox --dap <file.lox>` (for Lox-level debugging)

### Profiling and Tracing
- **CPU Sampler**: `./lox --cpusampler <file.lox>`
- **CPU Tracer**: `./lox --cputracer <file.lox>`
- **Memory Tracer**: `./lox --memtracer <file.lox>`
- **Trace Compilation**: `EXTRA_JAVA_ARGS="-Djdk.graal.TraceCompilation=true" ./lox <file.lox>`
- **Generate Flamegraph**: `./lox --cpusampler --cpusampler.Output=flamegraph --cpusampler.OutputFile=flamegraph.svg <file.lox>`
- **Dump Compiler Graphs**: `EXTRA_JAVA_ARGS="-Djdk.graal.Dump=Truffle:1 -Djdk.graal.PrintGraph=File -Djdk.graal.DumpPath=compiler_graphs" ./lox <file.lox>`

The `./lox` script respects the `EXTRA_JAVA_ARGS` environment variable for passing JVM/GraalVM options.

## Language-Specific Syntax

This Lox implementation has custom syntax differences from standard Lox:

### Arrays
- **Empty array**: `👉👈`
- **Array literal**: `👉1, 2, 3👈`
- **Array access**: `arr👉0👈`
- **Array assignment**: `arr👉0👈 = value;`

### Classes
- **Inheritance syntax**: `class Child 🤝 Parent { ... }` (uses handshake emoji instead of `<`)
- **Instance reference**: Uses `self` instead of `this`

### For Loops
- **Standard for**: Requires a condition (no `break` statement exists)
- **For-of**: `for (var elem of arr) { ... }` (iterates over elements)
- **For-in**: `for (var i in arr) { ... }` (iterates over indices)

### Comments
- Single-line: `// comment`

### Syntax Migration
Use migration scripts to convert standard Lox to this implementation's syntax:
- **Non-macOS**: `./migration_boring_lox_to_amazing_lox.sh`
- **macOS**: `./migration_boring_lox_to_amazing_lox_macos.sh`

## Architecture

### Core Components

**Language Entry Point** (`LoxLanguage.java`)
- Truffle language registration with ID "lox"
- Manages built-in functions (clock, Number, String, load, round, lookup)
- Creates language context and delegates parsing to bytecode compiler

**Parser** (`src/main/antlr4/de/hpi/swa/lox/parser/Lox.g4`)
- ANTLR4 grammar defining Lox syntax
- Auto-generates parser code during Maven build via `antlr4-maven-plugin`

**Bytecode Compiler** (`LoxBytecodeCompiler.java`)
- Visitor pattern over ANTLR parse tree
- Uses Truffle **Bytecode DSL** (`LoxBytecodeRootNodeGen`) instead of traditional AST nodes
- Compiles to bytecode for better performance and reduced memory footprint
- Handles source tagging for instrumentation (debugging, profiling)

**Runtime Data Types** (`runtime/data/`)
- `LoxArray`: Dynamic array with iterator support and interop
- `LoxClass`: Class definitions
- `LoxObject`: Class instances
- `LoxFunction`: Function objects
- `LoxNumber`: Numeric wrapper
- `Nil`: Null value singleton
- `GlobalObject`: Global scope container

**Built-in Nodes** (`nodes/`)
- Predefined functions using Truffle DSL
- Examples: `ClockBuiltInNode`, `NumberBuiltInNode`, `StringBuiltInNode`
- Use DSL specializations for performance

**CLI** (`cli/LoxMain.java`)
- Main entry point for command-line execution
- Handles REPL and file execution modes

### Package Structure
```
de.hpi.swa.lox/
├── LoxLanguage.java          # Truffle language registration
├── cli/                      # Command-line interface
├── parser/                   # ANTLR grammar and bytecode compiler
├── bytecode/                 # Bytecode DSL root nodes
├── nodes/                    # Built-in function nodes
└── runtime/
    ├── LoxContext.java       # Language context
    ├── LoxDisplay.java       # Display/output utilities
    └── data/                 # Runtime object types
```

### Truffle Integration
- Uses **Truffle Bytecode DSL** (newer approach) instead of AST interpreter nodes
- Supports standard Truffle instrumentation tags (CallTag, StatementTag, etc.)
- Enables Truffle profiling tools (CPU sampler, tracer, etc.)
- Polyglot interoperability via `InteropLibrary` exports
- DAP (Debug Adapter Protocol) support for debugging

### Test Structure
All tests extend `AbstractLoxTest` which provides:
- `assertPrints(code, expected)`: Execute code and verify output
- `assertErrors(code)`: Verify code produces runtime error
- Uses GraalVM Polyglot API `Context.eval()` for execution

Tests are located in `src/test/java/de/hpi/swa/lox/test/` and follow naming pattern `*Test.java`.

## Development Requirements

- **JDK**: Oracle JDK 21, OpenJDK 21, or derivatives
- **Recommended**: GraalVM for JDK 21 version 21.0.4 (for best performance)
- **Build tool**: Maven (use included `./mvnw` wrapper)

## Benchmark Harness

The `harness.lox` file provides a standard benchmark runner:
```bash
./lox harness.lox <benchmark-name> <num-iterations> <inner-iterations>
```

Available benchmarks include: sieve, queens, towers, permute, list

Example: `./lox harness.lox sieve 10 5000`

## Skills

This repository includes Claude Code skills in `.claude/skills/` for analyzing performance data:
- `cpu-sampler`: Analyze CPU sampling profiles
- `cpu-tracer`: Analyze CPU traces
- `trace-compilation`: Analyze compilation traces
- `trace-inlining`: Analyze inlining decisions
- `trace-performance-warnings`: Analyze performance warnings
- `trace-transfer-to-interpreter`: Analyze deoptimizations
- `memory-tracer`: Analyze memory allocation
- `analyze-compiler-graph`: Analyze Graal compiler graphs
- `specialization-statistics`: Analyze node specializations
