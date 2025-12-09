# Benchmark Baseline Report for Lox

Generated: 2025-12-02

## Language Analysis

**Language**: Lox (Truffle Implementation)

### Characteristics
- **Type System**: Dynamic typing
- **Execution Model**: Bytecode VM with JIT compilation (Truffle/Graal)
- **Platform**: JVM-based (GraalVM Truffle 24.2.0)
- **Paradigm**: Object-oriented, imperative
- **Complexity**: Moderate (classes, closures, arrays, custom syntax)
- **GC**: Platform-managed (JVM garbage collection with Truffle optimizations)
- **Optimization**: Truffle DSL specialization, bytecode operations with @Specialization annotations, JIT compilation

### Implementation Details
Lox uses GraalVM's Truffle framework with:
- Bytecode DSL for operation definitions
- Node specialization for type-specific dispatch
- Frame-based closure support
- DynamicObject/Shape for optimized property storage
- InteropLibrary for polyglot integration

## Comparable Languages from Benchmarks Game

### 1. Ruby
**Rationale**: TruffleRuby is also built on GraalVM Truffle, making it the most directly comparable language with similar execution model, JIT compilation strategy, and dynamic typing.

**URL**: https://benchmarksgame-team.pages.debian.net/benchmarksgame/measurements/ruby.html

**Characteristics**: Dynamic typing, OO paradigm, Truffle-based JIT compilation

### 2. Lua
**Rationale**: Similar bytecode VM + JIT execution model (LuaJIT), dynamic typing, comparable abstraction level and language complexity.

**URL**: https://benchmarksgame-team.pages.debian.net/benchmarksgame/measurements/lua.html

**Characteristics**: Dynamic typing, bytecode VM with JIT (LuaJIT), lightweight and fast

### 3. JavaScript/Node
**Rationale**: Dynamic language with mature JIT compilation (V8), similar abstraction level, widely benchmarked for comparison.

**URL**: https://benchmarksgame-team.pages.debian.net/benchmarksgame/measurements/node.html

**Characteristics**: Dynamic typing, highly optimized JIT (V8), similar paradigm

## Benchmarks from Benchmarks Game

### 1. fannkuch-redux
**Rationale**: Array-intensive computation testing permutation generation and sequence manipulation. Pure CPU-bound benchmark.

**URL**: https://benchmarksgame-team.pages.debian.net/benchmarksgame/description/fannkuchredux.html

**Pattern**: Array manipulation, permutation generation, algorithmic optimization

### 2. binary-trees
**Rationale**: Object allocation and recursion benchmark. Stresses garbage collector with tree creation/destruction cycles.

**URL**: https://benchmarksgame-team.pages.debian.net/benchmarksgame/description/binarytrees.html

**Pattern**: Memory allocation, GC stress test, recursive tree traversal

### 3. n-body
**Rationale**: Numerical simulation with floating-point operations. Tests mathematical computation performance.

**URL**: https://benchmarksgame-team.pages.debian.net/benchmarksgame/description/nbody.html

**Pattern**: Floating-point arithmetic, gravitational simulation, numerical methods

### 4. spectral-norm
**Rationale**: Matrix operations and numerical methods. Pure numerical computation benchmark.

**URL**: https://benchmarksgame-team.pages.debian.net/benchmarksgame/description/spectralnorm.html

**Pattern**: Matrix multiplication, eigenvalue approximation, iterative computation

## Benchmarks from AreWeFastYet

AreWeFastYet micro-benchmarks are specifically designed for language implementation analysis. The following benchmarks were already present in the repository or have been implemented:

### 1. queens
**Rationale**: Backtracking algorithm solving the eight queens problem. Tests recursion and constraint satisfaction.

**Pattern**: Recursive backtracking, array manipulation, algorithmic problem-solving

### 2. list
**Rationale**: Recursive list creation and traversal. Tests object allocation and recursive method calls.

**Pattern**: Recursive data structure manipulation, object creation patterns

### 3. sieve
**Rationale**: Sieve of Eratosthenes for finding primes. Array-based algorithm testing iteration performance.

**Pattern**: Array manipulation, prime number generation, iterative algorithms

### 4. towers
**Rationale**: Towers of Hanoi puzzle solver. Classic recursive algorithm benchmark.

**Pattern**: Recursive algorithm, problem-solving, call stack depth

### 5. permute
**Rationale**: Array permutation generation. Tests array manipulation and iteration.

**Pattern**: Permutation algorithms, array operations, iterative generation

### 6. bounce
**Rationale**: Simulates ball bouncing physics. Tests object creation, method calls, and arithmetic operations.

**Pattern**: Object-oriented simulation, floating-point arithmetic, iteration

**Status**: Implemented but encountering runtime format exception (under investigation)

### 7. storage
**Rationale**: Creates tree of arrays to stress garbage collector. Tests memory allocation patterns.

**Pattern**: Recursive tree construction, GC stress, memory allocation

**Status**: Implemented but encountering runtime format exception (under investigation)

## How to Execute Benchmarks

### Benchmarks Game Benchmarks

#### fannkuch-redux
- **File**: `fannkuchredux.lox`
- **Command**: `./lox harness.lox fannkuchredux 1 1`
- **Test Parameter**: N=12
- **Expected Result**: Checksum=3968050, MaxFlips=65
- **Verification**: Compares against known correct values

#### binary-trees
- **File**: `binarytrees.lox`
- **Command**: `./lox harness.lox binarytrees 1 1`
- **Test Parameter**: N=10 (scaled down from standard N=21 for faster testing)
- **Expected Result**: Check value=2047
- **Verification**: Validates tree node count

#### n-body
- **File**: `nbody.lox`
- **Command**: `./lox harness.lox nbody 1 1`
- **Test Parameter**: N=50,000 iterations
- **Expected Result**: Initial energy≈-0.169075164, Final energy≈-0.169059907
- **Verification**: Checks energy values within tolerance

#### spectral-norm
- **File**: `spectralnorm.lox`
- **Command**: `./lox harness.lox spectralnorm 1 1`
- **Test Parameter**: N=100
- **Expected Result**: ≈1.274219991
- **Verification**: Validates eigenvalue approximation

### AreWeFastYet Benchmarks

#### queens
- **File**: `queens.lox`
- **Command**: `./lox harness.lox queens 1 1`
- **Test Parameter**: 8x8 board, 10 iterations
- **Verification**: Built-in verification

#### list
- **File**: `list.lox`
- **Command**: `./lox harness.lox list 1 1`
- **Verification**: Built-in verification

#### sieve
- **File**: `sieve.lox`
- **Command**: `./lox harness.lox sieve 1 1`
- **Test Parameter**: N=5000
- **Expected Result**: 669 primes
- **Verification**: Validates prime count

#### towers
- **File**: `towers.lox`
- **Command**: `./lox harness.lox towers 1 1`
- **Verification**: Built-in verification

#### permute
- **File**: `permute.lox`
- **Command**: `./lox harness.lox permute 1 1`
- **Verification**: Built-in verification

#### bounce
- **File**: `bounce.lox`
- **Status**: Under investigation (format exception)
- **Expected Result**: 1331 bounces

#### storage
- **File**: `storage.lox`
- **Status**: Under investigation (format exception)
- **Expected Result**: 5461 nodes

## Reference Language Performance from Benchmarks Game

### fannkuch-redux (N=12)

| Language     | Elapsed Time (s) | CPU Time (s)      | Memory (KB) | URL |
|--------------|------------------|-------------------|-------------|-----|
| Ruby 3.4.0   | 154.32           | 593.14            | ~500        | [Link](https://benchmarksgame-team.pages.debian.net/benchmarksgame/measurements/ruby.html) |
| Lua 5.4.7    | 3.494            | 3.29              | 3,391       | [Link](https://benchmarksgame-team.pages.debian.net/benchmarksgame/measurements/lua.html) |
| Node 23.8.0  | 11.066           | 43.76-44.47       | 111,055     | [Link](https://benchmarksgame-team.pages.debian.net/benchmarksgame/measurements/node.html) |

**Source**: Computer Language Benchmarks Game (December 2025)

**Expected Performance for Lox**:
- **Best case**: 10-20 seconds (approaching Lua/Node with good optimization)
- **Expected**: 50-150 seconds (typical for Truffle-optimized dynamic language)
- **Worst case**: >200 seconds (optimization not yet mature)

### binary-trees (N=21)

| Language     | Elapsed Time (s) | CPU Time (s)      | Memory (KB)  | URL |
|--------------|------------------|-------------------|--------------|-----|
| Ruby 3.4.0   | 47.09            | 47.06-47.17       | 487,592      | [Link](https://benchmarksgame-team.pages.debian.net/benchmarksgame/measurements/ruby.html) |
| Lua 5.4.7    | 0.430            | 0.03              | 6,128        | [Link](https://benchmarksgame-team.pages.debian.net/benchmarksgame/measurements/lua.html) |
| Node 23.8.0  | 16.071           | 28.29-28.55       | 1,128,059    | [Link](https://benchmarksgame-team.pages.debian.net/benchmarksgame/measurements/node.html) |

**Note**: Lox implementation uses N=10 for faster testing

**Source**: Computer Language Benchmarks Game (December 2025)

**Expected Performance for Lox (N=10)**:
- **Best case**: 0.1-0.5 seconds (good GC performance)
- **Expected**: 0.5-2 seconds (typical for Truffle GC)
- **Worst case**: >3 seconds (GC overhead issues)

### n-body (N=50,000,000)

| Language     | Elapsed Time (s) | CPU Time (s)      | Memory (KB) | URL |
|--------------|------------------|-------------------|-------------|-----|
| Ruby 3.4.0   | 166.67           | ~166              | ~500        | [Link](https://benchmarksgame-team.pages.debian.net/benchmarksgame/measurements/ruby.html) |
| Lua 5.4.7    | 2.415            | ~2.4              | ~3,500      | [Link](https://benchmarksgame-team.pages.debian.net/benchmarksgame/measurements/lua.html) |
| Node 23.8.0  | 8.548            | 8.60-8.73         | 67,244      | [Link](https://benchmarksgame-team.pages.debian.net/benchmarksgame/measurements/node.html) |

**Note**: Lox implementation uses N=50,000 (1000x smaller) for practical testing

**Source**: Computer Language Benchmarks Game (December 2025)

**Expected Performance for Lox (N=50,000)**:
- **Best case**: 0.01-0.05 seconds (good numerical optimization)
- **Expected**: 0.1-0.5 seconds (typical for Truffle numeric code)
- **Worst case**: >1 second (poor floating-point optimization)

### spectral-norm (N=5,500)

| Language     | Elapsed Time (s) | CPU Time (s)      | Memory (KB) | URL |
|--------------|------------------|-------------------|-------------|-----|
| Ruby 3.4.0   | 56.52            | ~56               | ~500        | [Link](https://benchmarksgame-team.pages.debian.net/benchmarksgame/measurements/ruby.html) |
| Lua 5.4.7    | 0.943            | 0.65              | 3,465       | [Link](https://benchmarksgame-team.pages.debian.net/benchmarksgame/measurements/lua.html) |
| Node 23.8.0  | 5.404            | 5.40              | 66,163      | [Link](https://benchmarksgame-team.pages.debian.net/benchmarksgame/measurements/node.html) |

**Note**: Lox implementation uses N=100 for faster testing

**Source**: Computer Language Benchmarks Game (December 2025)

**Expected Performance for Lox (N=100)**:
- **Best case**: 0.01-0.05 seconds (good matrix operation optimization)
- **Expected**: 0.1-0.5 seconds (typical for Truffle numerical code)
- **Worst case**: >1 second (optimization issues)

## Performance Analysis Guidelines

### Understanding the Baseline Data

1. **Benchmarks Game Data**: Measured on quad-core 3.0GHz Intel i5-3330, 15.8GB RAM, Ubuntu 24.04 using BenchExec with cgroups isolation. Results represent idealized single-run performance.

2. **Scale Adjustments**: Some Lox benchmarks use smaller N values for practical testing times. Scale expectations accordingly when comparing against full-scale Benchmarks Game results.

3. **Truffle Warm-up**: Initial runs will be slower as Truffle JIT compiler profiles and optimizes code. Performance improves over multiple iterations as hot code paths are compiled to machine code.

### Expected Performance Characteristics

**Strengths** (similar to TruffleRuby):
- JIT compilation can approach native speeds for hot loops
- Good GC performance with Truffle integration
- Efficient method dispatch via specialization

**Potential Bottlenecks**:
- Interpreter overhead during warm-up
- Object allocation patterns affecting GC
- Array access patterns (custom 👉👈 syntax)
- Lack of break statements may impact loop optimization

### Using Benchmarks for Optimization

1. **Establish Baseline**: Run all benchmarks with current implementation
2. **Compare Against Expected**: Use this report's expected ranges
3. **Profile Hot Paths**: Use `./lox --cputracer` or `--cpusampler`
4. **Analyze Compilation**: Use `EXTRA_JAVA_ARGS="-Djdk.graal.TraceTruffleCompilation=true"`
5. **Investigate Outliers**: Focus on benchmarks significantly outside expected ranges
6. **Iterate**: After optimizations, re-run to verify improvements

### Compiler Analysis Commands

```bash
# Trace compilation
EXTRA_JAVA_ARGS="-Djdk.graal.TraceTruffleCompilation=true" ./lox harness.lox queens 10 100

# Trace inlining decisions
EXTRA_JAVA_ARGS="-Djdk.graal.TraceTruffleInlining=true" ./lox harness.lox sieve 10 100

# Generate compiler graphs
EXTRA_JAVA_ARGS="-Djdk.graal.Dump=Truffle:1 -Djdk.graal.PrintGraph=File -Djdk.graal.DumpPath=compiler_graphs" ./lox harness.lox queens 10 100

# Analyze with Seafoam
seafoam compiler_graphs/*.bgv list
seafoam compiler_graphs/file.bgv graph
```

## Benchmark Suite Summary

### Fully Functional
- **Benchmarks Game**: fannkuch-redux, binary-trees, n-body, spectral-norm
- **AreWeFastYet**: queens, list, sieve, towers, permute

### Under Investigation
- **AreWeFastYet**: bounce, storage (runtime format exception)

## Next Steps

1. **Run All Benchmarks**: Execute all functional benchmarks to establish actual performance baseline
2. **Document Results**: Record execution times, memory usage, and any issues
3. **Compare vs. Expected**: Identify benchmarks that are significantly faster or slower than expected ranges
4. **Profile Outliers**: Use Truffle profiling tools on underperforming benchmarks
5. **Optimize**: Address identified bottlenecks
6. **Iterate**: Re-run benchmarks to verify improvements

## References

- **Computer Language Benchmarks Game**: https://benchmarksgame-team.pages.debian.net/benchmarksgame/
- **AreWeFastYet**: https://github.com/smarr/are-we-fast-yet
- **GraalVM Truffle**: https://www.graalvm.org/latest/graalvm-as-a-platform/language-implementation-framework/
- **Benchmark Methodology**: https://benchmarksgame-team.pages.debian.net/benchmarksgame/how-programs-are-measured.html
