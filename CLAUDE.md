# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Lox language implementation built on GraalVM Truffle, developed as part of the Build Your Own Programming Language course at HPI. The implementation uses ANTLR4 for parsing and Truffle's bytecode DSL for execution.

## Key Commands

### Build and Run
```bash
# Build the project
./mvnw package

# Run Lox code
./lox <program.lox>
./lox -c 'print true;'

# Run via Maven
./mvnw exec:java -Dexec.args="-c 'print true;'"

# Run tests
./mvnw test

# Clean build artifacts
./mvnw clean
```

### Debugging and Profiling
```bash
# CPU Tracer
./lox --cputracer <program>

# CPU Sampler
./lox --cpusampler <program>
./lox --cpusampler --cpusampler.Output=flamegraph --cpusampler.OutputFile=flamegraph.svg <program>

# Java Debugging (attaches on port 5005)
./debuglox <program>

# DAP Debugging
./lox --dap <program>

# Compiler Graph Dumping
EXTRA_JAVA_ARGS="-Djdk.graal.Dump=Truffle:1 -Djdk.graal.PrintGraph=File -Djdk.graal.DumpPath=compiler_graphs" ./lox <program>
```

### Performance Testing
The repository includes benchmark harness and example benchmarks:
```bash
./lox harness.lox sieve 10 5000
./lox harness.lox towers 10 13
./lox harness.lox queens 10 3000
```

## Architecture

### Language Entry Point
- `LoxLanguage.java`: Truffle language registration and built-in functions setup
- Main class: `de.hpi.swa.lox.cli.LoxMain`
- Language ID: `"lox"`

### Parsing Pipeline
1. ANTLR4 grammar: `src/main/antlr4/de/hpi/swa/lox/parser/Lox.g4`
2. Compiler: `LoxBytecodeCompiler.java` - walks ANTLR parse tree and generates Truffle bytecode
3. Bytecode root node: `LoxBytecodeRootNode` (generated via Truffle bytecode DSL)

### Runtime Components
- `LoxContext`: Language context holding global object and built-ins
- `GlobalObject`: Global variable/function storage
- Data types in `runtime/data/`: `LoxFunction`, `LoxClass`, `LoxObject`, `LoxArray`, `LoxNumber`, `Nil`

### Node Types
- Built-in functions: Clock, Number, String, load, round, lookup (in `nodes/` directory)
- Specialized nodes: `LoxCallFunctionNode`, `LoxReadPropertyNode`, `LoxWritePropertyNode`, `LoxLookupMethodNode`

### Testing
- Base test class: `AbstractLoxTest.java`
- Test pattern: Create polyglot context, eval Lox code, assert output
- Test naming convention: `*Test.java` (excluding `Abstract*Test.java`)
- Run single test class: `./mvnw test -Dtest=ClassName`

## Custom Lox Syntax

This implementation has custom syntax differences from standard Lox:

**Arrays**: Use emoji syntax
```lox
var a = 👉👈;           // empty array
var b = 👉1,2,3👈;      // array literal
a👉0👈 = 1;             // array assignment
print a👉0👈;           // array access
```

**Self vs this**: Uses `self` instead of `this`
```lox
class A {
  printA() {
    print self.a;
  }
}
```

**Class inheritance**: Uses handshake emoji
```lox
class Child 🤝 Parent { }
```

**For loops**: Condition is required (no `break` statement exists)

**Iteration**: Supports `for-of` and `for-in` loops
```lox
for (var element of array) { }
for (var index in array) { }
```

### Migration Tools
- `migration_boring_lox_to_amazing_lox.sh` (Linux/Windows)
- `migration_boring_lox_to_amazing_lox_macos.sh` (macOS)

## Development Notes

### JDK Requirements
- Development: Oracle JDK 21, OpenJDK 21, or derivatives
- Best performance: GraalVM for JDK 21 version 21.0.4

### ANTLR
- Grammar changes require rebuild: `./mvnw antlr4:antlr4`
- Maven automatically invokes ANTLR plugin before Java compilation

### Wrapper Script
The `./lox` script uses `rlwrap` for REPL line editing and sets up classpath via `.script-classpath` file. Use `EXTRA_JAVA_ARGS` environment variable to pass JVM arguments.

### Built-in Functions
Registered in `LoxLanguage.getBuiltins()`: clock, Number, String, load, round, lookup

### Bonus Features Implemented
- C3.2: REPL prints evaluated expressions
- C4.3: for-of/for-in loops for arrays
- C4.4: Array literals
- C9.1: Benchmark for array literals
- C9.2: Optimization for array literals (using `@Variadic` instead of append)
- C10.2: Array interoperability

## Example Programs

Root directory contains example Lox programs:
- `sieve.lox`, `primeCount.lox`: Prime number algorithms
- `harness.lox`, `benchmark.lox`, `run.lox`: Benchmarking framework
- `queens.lox`, `towers.lox`, `permute.lox`, `list.lox`: Benchmark implementations
- `game.lox`, `display_bounce.lox`: Display/game examples
