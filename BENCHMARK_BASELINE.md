# Benchmark Baseline Report

## Executive Summary

This report establishes performance baselines for the Lox programming language implementation using the Are We Fast Yet micro-benchmark suite. 

**Date:** 2026-01-15
**Lox Version:** 0.0.1
**GraalVM Version:** 24.2.0-SNAPSHOT
**Platform:** macOS (Darwin 25.0.0)

## Language Characteristics Analysis

### Type System
- **Dynamic typing**: No static type annotations, runtime type checking
- **Type specialization**: Truffle DSL @Specialization annotations for type-specific optimizations
- **Boxing elimination**: Enabled for `long.class` in bytecode configuration

### Execution Model
- **Bytecode interpreter**: Uses Truffle's @GenerateBytecode DSL
- **JIT compilation**: GraalVM compiler with tiered compilation (interpreted → compiled)
- **Optimizations**: Specialization, inlining, escape analysis, boxing elimination

### Platform
- **Runtime**: JVM/GraalVM Truffle framework
- **Garbage Collection**: JVM garbage collector
- **Memory Management**: Automatic via JVM

### Paradigm
- **Object-oriented**: Classes, inheritance (🤝 syntax), methods
- **Imperative**: Statements, control flow, mutable state
- **Complexity**: Moderate (full OO support, arrays with 👉👈 syntax, functions, control flow)

### Notable Features
- **Truffle optimizations**: Specializations, inlining, partial evaluation
- **Uncached interpreter**: Enabled for startup performance
- **Serialization support**: Bytecode caching enabled
- **Array literals**: Optimized using @Variadic operations

## Comparable Languages

For performance comparison, we selected two languages from the Are We Fast Yet benchmark suite:

### 1. JavaScript (Node.js)
**Rationale**:
- Dynamic typing matches Lox
- Object-oriented paradigm
- V8 JIT compiler provides excellent optimization baseline
- Widely recognized performance reference

**Characteristics**:
- Execution: JIT compilation (V8 TurboFan)
- Type system: Dynamic, prototype-based OO
- Platform: Native (V8 engine)

### 2. Python (CPython/PyPy)
**Rationale**:
- Dynamic typing matches Lox
- Object-oriented paradigm
- Widely recognized language for comparison
- Both interpreted (CPython) and JIT (PyPy) implementations available

**Characteristics**:
- Execution: Bytecode interpreter (CPython) or JIT (PyPy)
- Type system: Dynamic, class-based OO
- Platform: Native (C-based runtime)

## Benchmark Suite

### Implemented Benchmarks

| Benchmark | Description | Computational Pattern | Lines of Code |
|-----------|-------------|----------------------|---------------|
| bounce | Physics simulation of bouncing balls | Floating-point arithmetic, collision detection | 116 |
| list | Linked list operations | Object allocation, pointer chasing | ~150 |
| mandelbrot | Mandelbrot set calculation | Intensive FP math, bit operations | 154 |
| nbody | N-body gravitational simulation | Intensive FP math, nested loops, physics | ~250 |
| permute | Permutation generation | Recursion, array manipulation | ~100 |
| queens | N-Queens solver | Backtracking, constraint satisfaction | 60 |
| sieve | Prime number sieve | Array operations, arithmetic | ~80 |
| storage | Tree building and allocation | Deep recursion, GC stress test | ~65 |
| towers | Towers of Hanoi | Deep recursion, call-intensive | ~120 |


### Execution Instructions

All benchmarks can be run using the harness:
```bash
./lox harness.lox <benchmark> <iterations> <innerIterations>
```

### Measured Performance (Lox Implementation)

Performance data collected with 10 outer iterations after warmup:

| Benchmark | Inner Iterations | Average Time (μs) | Steady-State (μs) | First Iteration (μs) | Warmup Factor |
|-----------|------------------|-------------------|-------------------|----------------------|---------------|
| bounce | 100 | 60,396 | ~22,000 | 376,133 | 17.1x |
| list | 100 | 583,035 | ~555,000 | 788,765 | 1.4x |
| mandelbrot | 100 | 60,632 | ~5,000 | 377,965 | 75.6x |
| nbody | 100 | 20,023 | ~1,600 | 76,508 | 47.8x |
| permute | 10000 | 809,971 | ~745,000 | 1,133,309 | 1.5x |
| queens | 3000 | 637,548 | ~592,000 | 1,000,664 | 1.7x |
| sieve | 10000 | 448,115 | ~410,000 | 679,488 | 1.7x |
| storage | 100 | 92,329 | ~65,000 | 316,141 | 4.9x |
| towers | 300 | 1,765,540 | ~1,700,000 | 2,175,435 | 1.3x |

**Notes**:
- All times in microseconds (μs)
- Warmup factor = First iteration / Steady-state (indicates JIT compilation effectiveness)
- Steady-state estimated from last 5 iterations
- JIT compilation shows significant impact (1.3x-75x speedup after warmup)

## Performance Expectations

### Based on Comparable Languages

From the Are We Fast Yet project data and literature:

**General Performance Ranges** (relative to C++ baseline):
- **JavaScript (V8)**: 1x-3x slower than C++ (excellent JIT)
- **Python (CPython)**: 10x-50x slower than C++ (interpreted)
- **Python (PyPy)**: 2x-10x slower than C++ (JIT)
- **Lua**: 5x-15x slower than C++ (lightweight interpreter)
- **Ruby (YJIT)**: 3x-10x slower than C++ (recent JIT improvements)
