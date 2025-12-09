# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Lox programming language implementation built with GraalVM Truffle, developed as part of the Build Your Own Programming Language course at HPI. The implementation uses a bytecode interpreter architecture with Truffle's bytecode DSL for optimized execution.

## Development Commands

### Build and Compile
```bash
./mvnw package          # Compile the project
./mvnw clean            # Clean build artifacts
```

### Running Tests
```bash
./mvnw test             # Run all tests
./mvnw test -Dtest=ControlFlowTest    # Run a specific test class
```

### Executing Lox Programs
```bash
./lox <file.lox>                    # Run a Lox file
./lox -c 'print true;'              # Execute Lox code directly
./mvnw exec:java -Dexec.args="-c 'print true;'"   # Alternative execution via Maven
```

### Debugging
```bash
./debuglox <file.lox>               # Start with Java debugger (port 5005)
./lox --dap <file.lox>              # Start with DAP for Lox-level debugging
```

## Architecture

### Core Components

**Language Entry Point**: `LoxLanguage.java` - Truffle language registration, context creation, and parsing entry point

**Bytecode Interpreter**: `LoxBytecodeRootNode.java` - Uses Truffle's `@GenerateBytecode` annotation to define bytecode operations. This is the heart of the interpreter implementation.

**Parser**: `LoxBytecodeCompiler.java` - ANTLR-based parser that compiles source to bytecode using the BytecodeBuilder API

**Grammar**: `src/main/antlr4/de/hpi/swa/lox/parser/Lox.g4` - ANTLR4 grammar defining Lox syntax

**CLI**: `LoxMain.java` - Command-line launcher using GraalVM's AbstractLanguageLauncher

### Runtime Data Types

Located in `src/main/java/de/hpi/swa/lox/runtime/data/`:
- `LoxObject.java` - Object instances
- `LoxClass.java` - Class definitions
- `LoxFunction.java` - Functions and methods
- `LoxArray.java` - Array implementation with special syntax
- `LoxNumber.java` - Numeric values
- `Nil.java` - Null/nil value
- `GlobalObject.java` - Global scope object

### Node Types

`src/main/java/de/hpi/swa/lox/nodes/` contains:
- Built-in functions (Clock, Number, String, MathRound)
- Property access nodes (LoxReadPropertyNode, LoxWritePropertyNode)
- Method lookup and function call nodes
- Value conversion nodes

### Operations

Operations are defined as nested static classes in `LoxBytecodeRootNode.java` using the `@Operation` annotation. Examples:
- `LoxPrint` - Print statement
- `LoxInvert` - Boolean negation
- `LoxNewArray` - Array creation (uses `@Variadic` for array literals)
- `LoxReadArray`, `LoxWriteArray` - Array element access
- Various arithmetic and comparison operations

Each operation uses Truffle DSL `@Specialization` annotations for type-specific behavior and optimization.

## Lox Language Extensions

This implementation has several unique features:

### Array Syntax
Arrays use emoji-based syntax:
```lox
var a = 👉👈;          // Empty array
a👉0👈 = 1;            // Set element
print a👉0👈;          // Read element
var b = 👉1, 2, 3👈;  // Array literal
```

### Self Instead of This
Uses `self` keyword instead of `this`:
```lox
class A {
  printA() {
    print self.a;
  }
}
```

### For Loop Condition Required
For loops must have a condition (no infinite loops without break):
```lox
for (var i = 0; i < 10; i = i + 1) {
  print i;
}
```

### For-of and For-in Loops
```lox
for (var element of array) { ... }  // Iterate over values
for (var index in array) { ... }    // Iterate over indices
```

### Class Inheritance Syntax
Uses 🤝 emoji for inheritance:
```lox
class Child 🤝 Parent { }
```

### Comments
Supports single-line comments with `//`

## Testing

Tests are in `src/test/java/de/hpi/swa/lox/test/`. Each test extends `AbstractLoxTest.java` which provides:
- `executeSource(String source)` - Execute Lox code and capture output
- Output comparison helpers
- Error testing utilities

Test files follow pattern `*Test.java` and are automatically discovered by Maven Surefire.

## Performance Notes

The implementation includes optimizations for array literals (see `bonusOptimizations/arrayLiterals/README.md`):
- Array literals use `@Variadic` operations instead of repeated append operations
- Boxing elimination enabled for `long.class` in bytecode configuration
- Uncached interpreter enabled for startup performance
- Serialization support for bytecode caching

## Migration Tools

For converting standard Lox to this implementation's syntax:
- `migration_boring_lox_to_amazing_lox.sh` - For Linux/Unix
- `migration_boring_lox_to_amazing_lox_macos.sh` - For macOS

## Dependencies

- GraalVM 24.2.0-SNAPSHOT (Truffle API)
- ANTLR 4.12.0 (parser generation)
- JUnit 4.13.2 (testing)
- Java 21 (source and target)

The project uses a custom Maven repository hosted at lively-kernel.org for GraalVM artifacts.

## File Organization

- `src/main/java/de/hpi/swa/lox/` - Core implementation
- `src/main/antlr4/de/hpi/swa/lox/parser/` - Grammar files
- `src/test/java/de/hpi/swa/lox/test/` - Test suite
- Root directory - Example `.lox` programs and benchmark files
