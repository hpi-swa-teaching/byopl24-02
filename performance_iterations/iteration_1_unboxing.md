# Performance Optimization Iteration 1: Primitive Double Unboxing

## Date
2026-01-16

## Problem Identified
The Lox implementation was using boxed `Double` wrapper objects throughout the arithmetic operations and in the `LoxNumber` class internal storage. This created unnecessary object allocations and prevented optimal JIT compilation.

## Theory
Arithmetic-intensive benchmarks (like nbody and mandelbrot) perform millions of numeric operations. Each operation was:
1. Unwrapping `Double` from `LoxNumber`
2. Performing arithmetic on boxed `Double` objects
3. Wrapping result back into new `LoxNumber` with new `Double`

This pattern prevents escape analysis and creates GC pressure.

## Implementation
Changed `LoxNumber` internal representation from `Double` (boxed) to `double` (primitive):

### Files Modified:
1. **LoxNumber.java**:
   - Changed `private Double internalValue` to `private double internalValue`
   - Updated `getValue()` to return `double` instead of `Double`
   - Changed constructor to accept `double` instead of `Double`
   - Updated all methods that called wrapper methods (`.intValue()`, `.longValue()`, etc.) to use casts
   - Changed `Double.valueOf()` to `Double.parseDouble()` for primitive parsing

2. **LoxBytecodeRootNode.java**:
   - Changed all arithmetic operations (Add, Subtract, Multiply, Divide, Negate) to use primitive `double` instead of `Double`
   - Example: `Double result = left.getValue() + right.getValue()` → `double result = left.getValue() + right.getValue()`
   - Fixed array access operations to use direct casts: `(int) index.getValue()` instead of `index.getValue().intValue()`
   - Simplified LoxWriteArray to a single specialization with conditional logic (guards don't support cast expressions)

## Results

### Micro-benchmark (1M addition operations):
- Before: ~1.1 seconds
- After: ~0.7-1.0 seconds
- Improvement: ~10-15% faster

### All tests pass:
- 161 tests run, 0 failures

### Benefits:
1. Reduced object allocations (no `Double` wrapper creation in hot loops)
2. Better JIT compilation opportunities (primitives can be kept in registers)
3. Improved escape analysis (primitives don't escape)
4. Lower GC pressure

## Technical Notes
- The `@GenerateBytecode` configuration already had `boxingEliminationTypes = { long.class }` but this wasn't sufficient because operations worked with `LoxNumber` objects
- Array guards had to be simplified because Truffle DSL guard expressions don't support cast syntax like `(int) value`
- The optimization maintains full compatibility - all existing tests pass

## Next Steps
Could explore:
1. Adding specializations for primitive `double` directly in bytecode operations (avoiding LoxNumber entirely in hot paths)
2. Investigating if other data types could benefit from similar unboxing
3. Profiling with Graal compiler to verify boxing elimination is working as expected
