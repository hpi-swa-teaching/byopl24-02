# Benchmark Baseline Report for Lox Language

**Generated**: 2025-11-28

---

## 1. Language Analysis

### Detected Lox Characteristics

- **Type System**: Dynamic typing
- **Execution Model**: Bytecode VM with JIT compilation (Truffle/Graal)
- **Platform**: JVM-based (GraalVM Truffle framework v24.2.0-SNAPSHOT)
- **Paradigm**: Object-oriented, imperative
- **Complexity**: Moderate (OO with classes/inheritance, closures, arrays, for-of/for-in loops)
- **GC**: Platform-managed (JVM garbage collection)
- **Optimization Features**:
  - Truffle specialization
  - Inlining (function call elimination)
  - Partial evaluation (compile-time computation)
  - On-stack replacement (OSR)
  - Graal JIT compilation to native code

### Implementation Details

- **Parser**: ANTLR4-based grammar
- **Bytecode**: Truffle Bytecode DSL for execution
- **Built-in Functions**: clock, Number, String, load, round, lookup
- **Special Features**:
  - Emoji-based array syntax (👉👈)
  - `self` keyword instead of `this`
  - Emoji-based inheritance (🤝)
  - For-of and for-in loops

---

## 2. Comparable Languages Selection

### Primary Comparisons

#### 1. Lua (LuaJIT 5.4.7)
**Rationale**: Most similar execution model (bytecode VM + JIT compilation, dynamic typing, similar complexity level)

**URL**: https://benchmarksgame-team.pages.debian.net/benchmarksgame/measurements/lua.html

**Characteristics**:
- Bytecode VM with LuaJIT optimization
- Dynamic typing
- Lightweight, performance-focused
- Similar paradigm (scripting, OO-capable)

#### 2. Python 3 (3.13.0)
**Rationale**: Similar abstraction level, dynamic typing, object-oriented paradigm, widely benchmarked for comparison context

**URL**: https://benchmarksgame-team.pages.debian.net/benchmarksgame/measurements/python3.html

**Characteristics**:
- Dynamic typing
- Object-oriented
- Interpreted with some JIT (PyPy)
- Similar feature richness

---

## 3. Benchmarks Selected from Benchmarks Game

### 1. fannkuch-redux

**Rationale**: Array-intensive computation, tests permutation generation and array manipulation performance

**Description**: Simulates pancake-flipping operations on permutations. For each permutation, reverses elements until first element is 1, counting flips. Computes maximum flip count and checksum.

**URL**: https://benchmarksgame-team.pages.debian.net/benchmarksgame/description/fannkuchredux.html

### 2. binary-trees

**Rationale**: Object allocation and recursion heavy, stresses garbage collector and memory management

**Description**: Creates and destroys perfect binary tree structures to evaluate memory allocation and GC performance. Recursively allocates trees at various depths.

**URL**: https://benchmarksgame-team.pages.debian.net/benchmarksgame/description/binarytrees.html

### 3. n-body

**Rationale**: Numerical computation and floating-point operations, tests mathematical performance

**Description**: Models orbits of Jovian planets using symplectic integrator. Simulates gravitational interactions between celestial bodies over many time steps.

**URL**: https://benchmarksgame-team.pages.debian.net/benchmarksgame/description/nbody.html

### 4. spectral-norm

**Rationale**: Matrix operations and numerical methods, tests linear algebra performance

**Description**: Calculates the spectral norm (largest singular value) of an infinite matrix using iterative power method.

**URL**: https://benchmarksgame-team.pages.debian.net/benchmarksgame/description/spectralnorm.html

---

## 4. Benchmarks Selected from AreWeFastYet

These micro-benchmarks are specifically designed for language implementation analysis.

### 1. bounce

**Rationale**: Simulates a ball bouncing within a box, tests object-oriented programming patterns and iteration

**Description**: Creates 100 ball objects with random positions and velocities. Runs 50 iterations of physics simulation, counting boundary collisions.

### 2. list

**Rationale**: Recursively creates and traverses lists, tests linked data structure performance

**Description**: Builds and manipulates linked list structures recursively.

### 3. permute

**Rationale**: Generates permutations of an array, tests array manipulation

**Description**: Creates all permutations of an array using swap-based algorithm.

### 4. queens

**Rationale**: Solves the eight queens problem, tests backtracking and constraint satisfaction

**Description**: Places N queens on NxN chessboard with no conflicts using backtracking.

### 5. sieve

**Rationale**: Finds prime numbers using sieve of Eratosthenes, tests array-based algorithms

**Description**: Classic sieve algorithm for finding all primes up to N.

### 6. storage

**Rationale**: Creates and verifies a tree of arrays to stress garbage collector

**Description**: Recursively builds tree structure containing arrays, measures allocation performance.

### 7. towers

**Rationale**: Solves the Towers of Hanoi game, tests recursion

**Description**: Classic recursive puzzle solution moving disks between pegs.

---

## 5. How to Execute Benchmarks

### Benchmarks Game Benchmarks

#### fannkuch-redux

- **File**: `fannkuch.lox`
- **Command**: `./lox harness.lox fannkuch 1 1`
- **Test Parameter**: N=12
- **Expected Result**: checksum=3968050, maxFlips=65
- **Verification**: Compares checksum and max flips against expected values

#### binary-trees

- **File**: `binarytrees.lox`
- **Command**: `./lox harness.lox binarytrees 1 1`
- **Test Parameter**: N=21 (depth)
- **Expected Result**: 8388607 (long-lived tree check)
- **Verification**: Validates final tree checksum

#### n-body

- **File**: `nbody.lox`
- **Command**: `./lox harness.lox nbody 1 1`
- **Test Parameter**: N=50000000 (iterations)
- **Expected Result**: -0.169075164 (±0.000001)
- **Verification**: Checks final energy value within tolerance

#### spectral-norm

- **File**: `spectralnorm.lox`
- **Command**: `./lox harness.lox spectralnorm 1 1`
- **Test Parameter**: N=5500
- **Expected Result**: 1.274224153 (±0.000001)
- **Verification**: Validates spectral norm value within tolerance

### AreWeFastYet Benchmarks

#### bounce

- **File**: `bounce.lox`
- **Command**: `./lox harness.lox bounce 10 5000`
- **Test Parameter**: 5000 inner iterations
- **Expected Result**: 1331 bounces per iteration
- **Verification**: Confirms bounce count equals 1331

#### list

- **File**: `list.lox`
- **Command**: `./lox harness.lox list 10 18`
- **Test Parameter**: 18 inner iterations
- **Verification**: Validates list construction and traversal

#### permute

- **File**: `permute.lox`
- **Command**: `./lox harness.lox permute 10 6`
- **Test Parameter**: 6 inner iterations
- **Verification**: Checks permutation count and final order

#### queens

- **File**: `queens.lox`
- **Command**: `./lox harness.lox queens 10 8`
- **Test Parameter**: 8 queens (8x8 board)
- **Verification**: Validates solution count

#### sieve

- **File**: `sieve.lox`
- **Command**: `./lox harness.lox sieve 10 5000`
- **Test Parameter**: Find primes up to 5000
- **Expected Result**: 669 primes
- **Verification**: Confirms prime count equals 669

#### storage

- **File**: `storage.lox`
- **Command**: `./lox harness.lox storage 10 10`
- **Test Parameter**: Tree depth 7
- **Expected Result**: 5461 node count
- **Verification**: Validates node allocation count

#### towers

- **File**: `towers.lox`
- **Command**: `./lox harness.lox towers 10 13`
- **Test Parameter**: 13 disks
- **Verification**: Validates move sequence correctness

---

## 6. Reference Language Performance from Benchmarks Game

### Benchmark: fannkuch-redux (N=12)

| Language  | Elapsed Time (s) | CPU Time (s)  | Memory (KB) | URL |
|-----------|------------------|---------------|-------------|-----|
| Lua       | 604.543          | 604.44        | 3,330       | [Link](https://benchmarksgame-team.pages.debian.net/benchmarksgame/measurements/lua.html) |
| Python 3  | 311.18           | 1221.85       | 28,701      | [Link](https://benchmarksgame-team.pages.debian.net/benchmarksgame/measurements/python3.html) |

**Source**: https://benchmarksgame-team.pages.debian.net/benchmarksgame/performance/fannkuchredux.html

**Expected Performance**:
- **Best Case**: 300-600s (approaching Lua performance with good optimization)
- **Expected**: 600-1200s (typical for similar dynamic languages)
- **Worst Case**: >1200s (optimization issues or interpreter-only mode)

**Analysis**: This benchmark is compute-intensive with heavy array manipulation. Lox's Truffle optimizations (specialization, inlining) should help but may not match LuaJIT's mature optimizations initially.

---

### Benchmark: binary-trees (N=21)

| Language  | Elapsed Time (s) | CPU Time (s)   | Memory (KB)  | URL |
|-----------|------------------|----------------|--------------|-----|
| Lua       | 47.559           | 175.72-176.19  | 2,055,766    | [Link](https://benchmarksgame-team.pages.debian.net/benchmarksgame/measurements/lua.html) |
| Python 3  | 33.37            | 121.32-121.68  | 460,771      | [Link](https://benchmarksgame-team.pages.debian.net/benchmarksgame/measurements/python3.html) |

**Source**: https://benchmarksgame-team.pages.debian.net/benchmarksgame/performance/binarytrees.html

**Expected Performance**:
- **Best Case**: 30-50s (if Truffle optimizes allocation well)
- **Expected**: 50-120s (typical for object-heavy workloads)
- **Worst Case**: >150s (if escape analysis fails or GC thrashes)

**Analysis**: Heavily stresses object allocation and garbage collection. Truffle's escape analysis should help eliminate some allocations. JVM GC is mature but memory overhead may be higher than native implementations.

---

### Benchmark: n-body (N=50000000)

| Language  | Elapsed Time (s) | CPU Time (s)   | Memory (KB) | URL |
|-----------|------------------|----------------|-------------|-----|
| Lua       | 244.499          | 250.36-259.88  | 3,273       | [Link](https://benchmarksgame-team.pages.debian.net/benchmarksgame/measurements/lua.html) |
| Python 3  | 372.41           | 371.23         | 12,198      | [Link](https://benchmarksgame-team.pages.debian.net/benchmarksgame/measurements/python3.html) |

**Source**: https://benchmarksgame-team.pages.debian.net/benchmarksgame/performance/nbody.html

**Expected Performance**:
- **Best Case**: 200-300s (if float ops fully optimize)
- **Expected**: 300-500s (typical for dynamic languages with JIT)
- **Worst Case**: >600s (if floating-point ops don't specialize well)

**Analysis**: Pure numerical computation with floating-point arithmetic. Truffle should specialize arithmetic operations well. Custom sqrt implementation may be slower than native.

---

### Benchmark: spectral-norm (N=5500)

| Language  | Elapsed Time (s) | CPU Time (s)  | Memory (KB) | URL |
|-----------|------------------|---------------|-------------|-----|
| Lua       | 77.628           | 77.91-90.12   | 3,568       | [Link](https://benchmarksgame-team.pages.debian.net/benchmarksgame/measurements/lua.html) |
| Python 3  | 90.37            | 352.91        | 34,693      | [Link](https://benchmarksgame-team.pages.debian.net/benchmarksgame/measurements/python3.html) |

**Source**: https://benchmarksgame-team.pages.debian.net/benchmarksgame/performance/spectralnorm.html

**Expected Performance**:
- **Best Case**: 70-100s (good numerical optimization)
- **Expected**: 100-200s (typical for matrix operations)
- **Worst Case**: >300s (if nested loops don't optimize)

**Analysis**: Matrix operations with nested loops. Loop unrolling and arithmetic specialization critical. Array access patterns should optimize well with Truffle.

---

## 7. Integration with Other Profiling Skills

### Recommended Workflow

```
1. [This Skill: benchmark-baseline]
   → Establish expected performance ranges
   → Create benchmark implementations
   → Generate this baseline report

2. [Run All Benchmarks]
   → Execute benchmarks to get actual performance
   → Compare against expected ranges above

3. [If Performance Below Expected]
   → Use trace-performance-warnings-analyze
   → Identify optimization barriers
   → Use cpu-sampler-analyze for hotspots
   → Use trace-compilation-analyze to verify compilation
   → Use trace-inlining-analyze for inlining decisions

4. [Fix Issues & Re-test]
   → Address identified problems
   → Re-run benchmarks
   → Iterate until performance acceptable

5. [Deep Optimization (Optional)]
   → Use trace-transfer-to-interpreter-analyze for deopt issues
   → Use analyze-compiler-graph for low-level inspection
   → Advanced tuning based on Graal IR
```

### Key Points

- **Use AreWeFastYet benchmarks** for iterative optimization (faster execution)
- **Use Benchmarks Game benchmarks** for final validation and comparison
- **Focus on performance ranges**, not single values
- **Benchmarks Game data** is from isolated environment (not production-representative)
- **Micro-benchmarks** don't represent real applications

---

## 8. Performance Expectations Summary

### Benchmarks Game Benchmarks

| Benchmark      | Best Case | Expected | Worst Case | Notes |
|----------------|-----------|----------|------------|-------|
| fannkuch-redux | 300-600s  | 600-1200s | >1200s    | Array manipulation intensive |
| binary-trees   | 30-50s    | 50-120s   | >150s     | Allocation/GC heavy |
| n-body         | 200-300s  | 300-500s  | >600s     | Floating-point intensive |
| spectral-norm  | 70-100s   | 100-200s  | >300s     | Matrix operations |

### AreWeFastYet Benchmarks

These benchmarks are designed for quick iteration during optimization:

- **bounce**: Quick execution, tests OO patterns
- **list**: Tests linked structures
- **permute**: Array manipulation
- **queens**: Backtracking algorithms
- **sieve**: Array-based computation
- **storage**: GC stress test
- **towers**: Pure recursion

**Iteration Strategy**: Run AreWeFastYet benchmarks frequently during development. Run Benchmarks Game benchmarks for validation and comparison with other languages.

---

## 9. Next Steps

1. **Run Initial Benchmark Suite**:
   ```bash
   # Quick smoke test (AreWeFastYet)
   ./lox harness.lox bounce 1 100
   ./lox harness.lox sieve 1 500

   # Full benchmark run (expect long runtime)
   ./lox harness.lox fannkuch 1 1
   ./lox harness.lox binarytrees 1 1
   ```

2. **Analyze Results**: Compare actual performance against expected ranges above

3. **Profile if Needed**: Use Truffle profiling tools if performance below expectations

4. **Iterate**: Fix issues, re-test, repeat

---

**Report Generated**: 2025-11-28
**Lox Implementation**: GraalVM Truffle v24.2.0-SNAPSHOT
**Baseline Data Source**: Computer Language Benchmarks Game & AreWeFastYet (as of 2025-11-28)
