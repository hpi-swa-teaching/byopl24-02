# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Lox language implementation built on GraalVM Truffle for the "Build Your Own Programming Language" course at HPI. The implementation uses:
- **GraalVM Truffle** (v24.2.0-SNAPSHOT) for the language runtime
- **ANTLR4** (v4.12.0) for parsing
- **Bytecode DSL** for compilation
- **Java 21** as the base language

## Key Commands

### Build and Test
```bash
# Compile the project
./mvnw package

# Run tests (files ending with *Test.java, excluding Abstract*Test.java)
./mvnw test

# Clean build artifacts
./mvnw clean
```

### Running Lox Programs
```bash
# Execute a one-liner
./mvnw exec:java -Dexec.args="-c 'print true;'"

# Run a script file
./lox <program.lox>

# Debug with Java debugger (attach via VSCode)
./debuglox <program>

# Debug with DAP protocol
./lox --dap <program>
```

### Profiling and Tracing
```bash
# CPU tracer
./lox --cputracer <program>

# CPU sampler with flamegraph output
./lox --cpusampler --cpusampler.Output=flamegraph --cpusampler.OutputFile=output.svg <program>
```

### Testing Specific Tests
```bash
# Run a single test class
./mvnw test -Dtest=ArrayTest

# Run a specific test method
./mvnw test -Dtest=ArrayTest#testArrayLiterals
```

## Architecture Overview

### Compilation Pipeline

1. **Parsing**: ANTLR4 grammar (`src/main/antlr4/de/hpi/swa/lox/parser/Lox.g4`) generates lexer/parser
2. **Compilation**: `LoxBytecodeCompiler` (visitor pattern) traverses AST and emits bytecode using `LoxBytecodeRootNodeGen.Builder`
3. **Execution**: Bytecode DSL-generated nodes execute via Truffle framework

Entry point: `LoxLanguage.parse()` → `LoxBytecodeCompiler.parseLox()` → `LoxBytecodeRootNodeGen.create()`

### Core Components

**Language Registration** (`LoxLanguage.java`):
- Registered as "lox" language with Truffle
- Creates `LoxContext` with built-in functions (clock, Number, String, load, round, lookup)
- Configured with instrumentation tags for debugging/profiling

**Runtime Context** (`LoxContext.java`):
- Manages global scope via `GlobalObject`
- Initializes built-ins and ARGV array
- Provides environment access

**Variable Scoping** (`LoxBytecodeCompiler.LoxLexicalScope`):
- Handles local, global, and closure variables
- Tracks function depth for materialized frames (closures)
- Uses `BytecodeLocal` for stack-allocated locals

**Data Types** (`src/main/java/de/hpi/swa/lox/runtime/data/`):
- `LoxNumber`: Numeric values (supports both int/double)
- `LoxFunction`: Functions and closures with materialized frames
- `LoxClass`: Class definitions with inheritance
- `LoxObject`: Class instances
- `LoxArray`: Dynamic arrays with iterator support
- `Nil`: Singleton null value

**Built-in Nodes** (`src/main/java/de/hpi/swa/lox/nodes/`):
- Extend `BuiltInNode` or `BuiltInNodeWithArgs`
- Use Truffle DSL annotations for specialization
- Examples: `ClockBuiltInNode`, `NumberBuiltInNode`, `LoadBuiltInNode`

### Bytecode DSL Integration

The implementation uses Truffle's Bytecode DSL (not AST-based):
- `LoxBytecodeRootNode` defined in `src/main/java/de/hpi/swa/lox/bytecode/`
- Compiler uses builder pattern: `b.beginX()` / `b.endX()` for operations
- Generated code creates efficient bytecode instructions
- Supports local variables, closures, and complex control flow

### Instrumentation

Tagged nodes for tooling support:
- `StatementTag`: Statements
- `ExpressionTag`: Expressions
- `CallTag`: Function calls
- `ReadVariableTag` / `WriteVariableTag`: Variable access
- `RootBodyTag`: Function bodies
- `DebuggerTags.AlwaysHalt`: Breakpoints

## Language-Specific Features

### Array Syntax
Uses emoji syntax: `👉` and `👈`
```lox
var a = 👉👈;        // Empty array
a👉0👈 = 1;          // Index assignment
var b = 👉1,2,3👈;   // Array literal
```

### Self vs This
Uses `self` instead of `this` for instance references:
```lox
class A {
  printA() {
    print self.a;
  }
}
```

### Class Inheritance
Uses handshake emoji `🤝` for extends:
```lox
class B 🤝 A {
  // inherits from A
}
```

### For Loop Variants
- Standard `for`: Requires condition (no `break` statement)
- `for-of`: Iterate over array elements (`for (var elem of arr)`)
- `for-in`: Iterate over array indices (`for (var i in arr)`)

### Comments
Supports both line and block comments:
- Line: `// comment`
- Block: `/* comment */`

## Development Notes

### REPL Mode
- Detected via environment variable `isRepl=true`
- Automatically prints expression results (see `LoxBytecodeCompiler.visitExprStmt()`)

### Error Handling
- Parse errors: `LoxParseError` (with source location)
- Runtime errors: `LoxRuntimeError` (TruffleException)
- Bail-out on syntax errors via `BailoutErrorListener`

### Testing Structure
All tests extend `AbstractLoxTest` which:
- Creates isolated `Context` per test
- Captures stdout/stderr
- Provides `run()`, `runAndExpect()`, `runAndExpectError()` helpers

### Generated Code
ANTLR and Truffle DSL generate code in `target/generated-sources/`:
- `antlr4/`: Parser/lexer from Lox.g4
- `annotations/`: Truffle DSL specializations (*Gen.java files)

Never edit generated files directly.

### Native Image Build
Use the `native` profile:
```bash
./mvnw package -Pnative
```
Creates native executable as `lox`.

### Migration Scripts
Convert standard Lox to custom syntax:
- macOS: `./migration_boring_lox_to_amazing_lox_macos.sh`
- Other: `./migration_boring_lox_to_amazing_lox.sh`

## Common Patterns

### Adding Built-in Functions
1. Create node extending `BuiltInNode` or `BuiltInNodeWithArgs` in `nodes/`
2. Use `@GenerateNodeFactory` and specialization annotations
3. Register in `LoxLanguage.getBuiltins()`
4. Built-in becomes available globally as a function

### Adding Language Operations
1. Define operation in `LoxBytecodeRootNode` (bytecode/)
2. Emit via compiler in `LoxBytecodeCompiler.visit*()` methods
3. Use `b.beginLox*()` / `b.endLox*()` pattern

### Variable Resolution
- Global: Via `GlobalObject` hash map
- Local: Via `BytecodeLocal` (stack slots)
- Closure: Via materialized frames (when `functionDepth > 0`)

Lookup happens in `LoxLexicalScope.lookupVariableName()` traversing parent scopes.

### Class/Method Binding
- Methods receive `self` as implicit first parameter
- Super calls via `super.method` syntax
- Inheritance handled in `visitClassDecl()` with `extends_` check

## Known Constraints

- For loops require explicit condition (no infinite `for(;;)`)
- No `break`/`continue` statements
- Single inheritance only
- Arrays are 0-indexed like standard Lox
