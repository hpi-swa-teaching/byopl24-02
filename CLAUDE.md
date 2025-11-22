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
./lox --traceCompilation harness.lox sieve 10 5000
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
./lox --engine.TraceCompilation <program>
./lox --engine.TraceInlining <program>
./lox --engine.TraceTransferToInterpreter <program>
./lox --engine.TracePerformanceWarnings <program>
./lox --engine.TraceCompilationDetails <program>  # More detailed compilation info
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

## Known Performance Issues and How to Identify Them

This section documents common performance issues found in this Lox implementation and provides strategies to identify them during optimization work.

### 1. LoxNumber Architecture Issues

The `LoxNumber` class (src/main/java/de/hpi/swa/lox/runtime/data/LoxNumber.java) wraps a `Double` object, which creates several performance problems.

#### 1.1 Boxing Overhead

**Description**: `LoxNumber` stores numbers as `Double` (boxed) instead of primitive `double`. Every numeric operation requires boxing/unboxing: unwrap → compute → rewrap.

**Location**: LoxNumber.java:17, LoxBytecodeRootNode.java:90-110 (arithmetic operations)

**How to Identify**:
1. **Code Review**: Look for `new LoxNumber(Double)` and `.getValue()` calls throughout arithmetic operations
2. **Compilation Analysis**: Run `./lox --engine.TraceCompilation harness.lox sieve 5 1000` and look for large compiled code sizes:
   ```
   [engine] opt done ... root innerBenchmarkLoop |Tier 2|... CodeSize 13544|...
   ```
   Code size >10000 bytes indicates overhead from boxing/unboxing
3. **IR Analysis**: Dump compiler graphs with:
   ```bash
   EXTRA_JAVA_ARGS="-Djdk.graal.Dump=Truffle:1 -Djdk.graal.PrintGraph=File -Djdk.graal.DumpPath=compiler_graphs" ./lox harness.lox sieve 5 1000
   ```
   Then search for Double.value accesses:
   ```bash
   bgv2json "compiler_graphs/TruffleHotSpotCompilation-*[root_sieve].bgv" 2>&1 | grep -i "double.value"
   ```
   Multiple `Double.value` field accesses confirm boxing/unboxing overhead

**Example from code**:
```java
// LoxBytecodeRootNode.java:106-109
static LoxNumber doNumbers(LoxNumber left, LoxNumber right) {
    Double result = left.getValue() + right.getValue();  // Unbox, compute, box
    return new LoxNumber(result);
}
```

#### 1.2 Missing Boxing Elimination for Double

**Description**: Boxing elimination only enabled for `long.class` in `@GenerateBytecode` annotation. Missing `double.class` and `boolean.class`, forcing all floating-point operations to use boxed values.

**Location**: LoxBytecodeRootNode.java:52

**How to Identify**:
1. **Code Review**: Check `@GenerateBytecode` annotation - `boxingEliminationTypes = { long.class }` is incomplete
2. **IR Analysis**: Look for boxing/unboxing operations on doubles in compiler graphs that should be eliminated
3. **Benchmark Comparison**: Compare performance before/after adding `double.class` to boxing elimination types

**Current code**:
```java
// LoxBytecodeRootNode.java:51-52
@GenerateBytecode(languageClass = LoxLanguage.class, enableMaterializedLocalAccesses = true,
        boxingEliminationTypes = { long.class }, // Missing double.class, boolean.class
```

#### 1.3 No Specialization for Primitive Operations

**Description**: Arithmetic operations use LoxNumber wrapper instead of Truffle specializations on primitives. This prevents the compiler from generating optimized machine code for primitive types.

**Location**: All arithmetic @Operation classes in LoxBytecodeRootNode.java (LoxAdd, LoxSubtract, LoxMultiply, LoxDivide, LoxNegate)

**How to Identify**:
1. **Code Review**: Check if @Operation specializations accept primitive types (double, long) or only wrapper types (LoxNumber)
2. **Compilation Size**: Large compiled code indicates wrapper overhead
3. **IR Complexity**: Complex IR with allocation nodes instead of simple arithmetic nodes

**Example**:
```java
// Current: LoxBytecodeRootNode.java:106-109
@Specialization
static LoxNumber doNumbers(LoxNumber left, LoxNumber right) {
    Double result = left.getValue() + right.getValue();
    return new LoxNumber(result);
}

// Better: Add specialization for primitives
@Specialization
static double doDouble(double left, double right) {
    return left + right;
}
```

#### 1.4 No Number Pooling

**Description**: Every `new LoxNumber()` allocates a new object. No caching for common values (0, 1, -1, small integers). While the compiler may virtualize these allocations, reducing creation overhead can still help.

**Location**: LoxNumber.java constructors, all arithmetic operations

**How to Identify**:
1. **Code Review**: Look for `new LoxNumber()` calls without any pooling/caching mechanism
2. **Allocation Profiling**: Use `--cpusampler` to identify hot allocation sites
3. **Heap Analysis**: Monitor object allocation rates during benchmark runs

**Impact**: Lower priority - mostly mitigated by escape analysis, but reduces compiler optimization burden.

#### 1.5 Redundant Equality Checks in Comparisons

**Description**: Comparison operations perform `.equals()` check before numeric comparison, adding unnecessary overhead.

**Location**: LoxBytecodeRootNode.java:210-215, 230-235, 250-255, 270-275

**How to Identify**:
1. **Code Review**: Look for `if (left.equals(right))` guards before comparison operations
2. **Compilation Analysis**: `--traceCompilation` shows larger compiled code size
3. **IR Analysis**: Extra branch nodes in IR for equality check

**Example**:
```java
// LoxBytecodeRootNode.java:210-215
static boolean doLoxNumbers(LoxNumber left, LoxNumber right) {
    if (left.equals(right)) {  // Redundant check
        return false;
    }
    return left.getValue() < right.getValue();
}
```

### 2. Array Implementation Issues

The `LoxArray` class (src/main/java/de/hpi/swa/lox/runtime/data/LoxArray.java) has iterator and bounds checking overhead.

#### 2.1 Inefficient Array Access Operations

**Description**: Array access performs multiple redundant checks. Index extraction `index.getValue().intValue()` called multiple times. Bounds checking should be in specialization guards.

**Location**: LoxBytecodeRootNode.java:411-458 (LoxReadArray, LoxWriteArray operations)

**How to Identify**:
1. **Code Review**: Look for repeated `.getValue().intValue()` calls and multiple bounds checks
2. **Compilation Analysis**: `--engine.TraceCompilation` reveals large compiled code
3. **IR Analysis**: Search for repeated getValue/intValue calls:
   ```bash
   bgv2json "compiler_graphs/TruffleHotSpotCompilation-*[root_sieve].bgv" 2>&1 | grep -E "(intValue|getValue)" | wc -l
   ```
   High count indicates redundant index extraction

**Example**:
```java
// LoxBytecodeRootNode.java:433-437
@Specialization(guards = { "index.getValue().intValue() >= 0",
        "array.getSize() > index.getValue().intValue()" })
static Void writeArrayInSize(LoxArray array, LoxNumber index, Object value) {
    array.setInSize(index.getValue().intValue(), value);  // 4th call to getValue().intValue()
    return null;
}
```

#### 2.2 Iterator Implementation Overhead

**Description**: For-in/for-of loops use `buildListIterator()` which: (1) has `@TruffleBoundary` preventing inlining, (2) creates 2-3 array copies per iterator, (3) unnecessarily filters null elements.

**Location**: LoxArray.java:43-50, 52-59

**How to Identify**:
1. **Code Review**: Check for `@TruffleBoundary` on `buildListIterator()` method in LoxArray.java:43
2. **Profiling**: Iterator methods cannot be compiled due to boundary annotation
3. **Code Analysis**: Look for multiple array copies in iterator creation (Arrays.asList → stream → filter → toList)

**Example**:
```java
// LoxArray.java:43-50
@TruffleBoundary
private ListIterator<Object> buildListIterator() {
    return Arrays.asList(innerArray)  // Copy 1
            .stream()                  // Copy 2
            .filter(element -> element != null)  // Unnecessary
            .toList().listIterator();  // Copy 3
}
```

#### 2.3 Missing TruffleBoundary on ensureCapacity

**Description**: `ensureCapacity()` method (Arrays.copyOf) not marked with `@TruffleBoundary`, causing complex IR and large compiled code when inlined.

**Location**: LoxArray.java:101-103

**How to Identify**:
1. **Code Review**: Check if `ensureCapacity()` has `@TruffleBoundary` annotation - it's missing at LoxArray.java:101
2. **Compilation Analysis**: `--engine.TraceCompilation` shows very large compiled code for array operations
3. **IR Analysis**: Arrays.copyOf inlined into compiled code causing bloat

**Example**:
```java
// LoxArray.java:101-103
private void ensureCapacity() {  // Missing @TruffleBoundary
    innerArray = Arrays.copyOf(innerArray, Math.max(innerArray.length, size) * 2);
}
```

### 3. Property Access Overhead

**Description**: String comparison on every property access. No specialization for common properties like "length".

**Location**: LoxReadPropertyNode.java:27-33

**How to Identify**:
1. **Code Review**: Look for string `.equals()` calls in property access code paths
2. **Profiling**: High time spent in property access methods when using `.length` frequently
3. **IR Analysis**: String comparison nodes appear in hot paths

**Example**:
```java
// LoxReadPropertyNode.java:27-32
@Specialization
public static Object read(String name, LoxArray array) {
    if (name.equals("length")) {  // String comparison on every access
        return new LoxNumber(array.getSize());
    } else {
        return Nil.INSTANCE;
    }
}
```

**Better approach**: Use inline caches or assumption-based specialization for property names.

### 4. Argument Passing with TruffleBoundary

**Description**: `createArguments()` marked with `@TruffleBoundary`, forcing every function call (including recursive calls) to exit compiled code. The DirectCallNode optimization is defeated because it immediately calls boundary method.

**Location**: LoxFunction.java:70-76, LoxCallFunctionNode.java:23

**How to Identify**:
1. **Code Review**: Check for `@TruffleBoundary` on `createArguments()` method at LoxFunction.java:70
2. **Profiling**: Use `--cpusampler` on recursive benchmarks:
   ```bash
   ./lox --cpusampler harness.lox list 3 10
   ```
   Look for high self-time in recursive functions despite compilation
3. **Compilation Analysis**: Large compiled code size due to DirectCallNode calling boundary method

**Example**:
```java
// LoxFunction.java:70-76
@TruffleBoundary  // Forces exit from compiled code!
public Object[] createArguments(Object[] userArguments) {
    Object[] result = new Object[userArguments.length + 1];
    System.arraycopy(userArguments, 0, result, 1, userArguments.length);
    result[0] = this;
    return result;
}

// LoxCallFunctionNode.java:23 - optimization defeated
return directCallNode.call(function.createArguments(arguments));
```

**Solution approach**: Remove `@TruffleBoundary` and add fast path for common argument counts (0-4 args).

### 5. Recursion & Loops Compilation

**Description**: Functions containing both recursion and loops fail to compile. Only recursion alone or loops alone compile properly. Compiler lacks information to inline recursive calls inside loops.

**Location**: Any recursive function called within a loop

**How to Identify**:
1. **Profiling**: Use `--cpusampler` on recursive benchmarks:
   - Recursion only: Shows high T2 (compiled) percentage
   - Loops only: Shows high T2 percentage
   - Recursion + loops: Shows 100% T0 (interpreted), 0% T2
2. **Compilation Trace**: `--traceCompilation` won't show compilation events for combined recursion+loops
3. **Benchmark Comparison**: Significant performance difference between separated vs combined recursion/loops

**Note**: This may be a fundamental Truffle limitation requiring deeper compiler hints or restructuring.

### 6. Using Specialization Instead of Fallback

**Description**: Some operations incorrectly use `@Specialization` for error cases instead of `@Fallback`. This installs error paths as active specializations rather than fallback guards, polluting the specialization profile.

**Location**: Check all @Operation classes for error-throwing specializations

**How to Identify**:
1. **Code Review**: Search for error-throwing specializations:
   ```bash
   grep -B3 "LoxRuntimeError" src/main/java/de/hpi/swa/lox/bytecode/LoxBytecodeRootNode.java | grep -B1 "@Specialization"
   ```
   Found: LoxAdd.doOtherTypes (line 119) incorrectly uses @Specialization
2. **Comparison**: Check that other operations (LoxSubtract, LoxMultiply, etc.) correctly use @Fallback
3. **Expected Pattern**: Error-throwing methods should use @Fallback, not @Specialization

**Example**:
```java
// Bad: Using @Specialization for error case
@Specialization
static LoxNumber doOtherTypes(Object left, Object right, @Bind Node node) {
    throw new LoxRuntimeError(...);
}

// Good: Using @Fallback
@Fallback
static LoxNumber doOtherTypes(Object left, Object right, @Bind Node node) {
    throw new LoxRuntimeError(...);
}
```

**Impact**: **Verified**: LoxAdd (line 119) uses @Specialization incorrectly. LoxSubtract, LoxMultiply, LoxDivide, LoxNegate, and comparison operations correctly use @Fallback.

### 7. String Concatenation Using Uncached Operations

**Description**: String concatenation uses `concatUncached()` instead of cached string operations, missing optimization opportunities.

**Location**: LoxBytecodeRootNode.java:114

**How to Identify**:
1. **Code Review**: Search for `concatUncached()` calls
2. **IR Analysis**: Uncached nodes appear in compiler graphs instead of inline cache nodes
3. **Profiling**: String-heavy benchmarks show poor performance

**Example**:
```java
// LoxBytecodeRootNode.java:113-114
@Specialization
static TruffleString doStrings(TruffleString left, TruffleString right) {
    return left.concatUncached(right, TruffleString.Encoding.UTF_8, false);  // Uncached!
}
```

**Better approach**: Use cached TruffleString operations with `@Cached` nodes.

## Performance Issue Identification Workflow

When investigating performance problems, follow this systematic approach:

1. **Run Benchmarks**: Establish baseline performance
   ```bash
   ./lox harness.lox sieve 5 1000
   ```

2. **Profile Execution**: Identify hot methods and compilation status
   ```bash
   ./lox --cpusampler harness.lox sieve 5 1000
   ```
   Look for: Low T2 percentage, high time in specific methods

3. **Trace Compilation**: Check what's being compiled and code size
   ```bash
   ./lox --engine.TraceCompilation harness.lox sieve 5 1000 2>&1 | grep "opt done"
   ```
   Look for: Large compiled code sizes (CodeSize >10000), missing compilations, deoptimizations
   Example output: `[engine] opt done ... |Tier 2|... CodeSize 13544|...`

4. **Analyze IR**: Examine compiler graphs for optimization barriers
   ```bash
   EXTRA_JAVA_ARGS="-Djdk.graal.Dump=Truffle:1 -Djdk.graal.PrintGraph=File -Djdk.graal.DumpPath=compiler_graphs" ./lox harness.lox sieve 5 1000
   ```
   Then search graphs for specific issues:
   ```bash
   # Check for boxing overhead
   bgv2json "compiler_graphs/TruffleHotSpotCompilation-*[root_sieve].bgv" 2>&1 | grep -i "double.value"

   # Check for redundant calls
   bgv2json "compiler_graphs/TruffleHotSpotCompilation-*[root_sieve].bgv" 2>&1 | grep -E "(getValue|intValue|equals)"
   ```
   Look for: Boxing nodes (Double.value), complex control flow, boundary calls, redundant method calls

5. **Review Code**: Check for known anti-patterns documented above
   - @TruffleBoundary on hot paths
   - Missing boxing elimination types
   - @Specialization instead of @Fallback for errors
   - Uncached operations in hot loops

6. **Verify Fix**: Re-run benchmarks and profiling to confirm improvements
