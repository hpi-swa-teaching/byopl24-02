# Workflow Guide

This guide provides a practical workflow for using the trace-performance-warnings tool.

## Quick Start

```bash
./lox --compiler.TracePerformanceWarnings=all program.lox
```

See [usage.md](./usage.md) for detailed command options.

## Step-by-Step Investigation

The skill typically follows this workflow:

### Step 1: Baseline Profiling
```bash
./lox --cpusampler --cpusampler.ShowTiers=true \
  --cpusampler.Delay=5000 \
  program.lox > cpu.txt
```
**Identify**: Top 3-5 hot functions

### Step 2: Check for Warnings
```bash
./lox --compiler.TracePerformanceWarnings=all \
  program.lox 2>&1 | tee warnings.txt
```
**Look for**: Warnings in hot functions

### Step 3: Count Warnings per Function
```bash
grep "perf warn" warnings.txt | \
  sed 's/.*perf warn \([^ ]*\).*/\1/' | \
  sort | uniq -c | sort -rn
```
**Identify**: Functions with most warnings

### Step 4: Analyze Specific Warnings
```bash
grep "perf warn hotFunction" warnings.txt
```
**Understand**: What optimization is blocked

### Step 5: Implement Fixes
- Virtual calls → Cache profiles, add specializations
- Type checks → Add type-specific specializations
- Store locations → Use compilation-constant frame slots
- Bailouts → Simplify, add boundaries

### Step 6: Verify Fixes
```bash
# No more warnings for this function
./lox --compiler.TracePerformanceWarnings=all \
  --engine.CompileOnly=hotFunction \
  program.lox

# Methods now inline
./lox --experimental-options \
  --engine.TraceInlining \
  --engine.CompileOnly=hotFunction \
  program.lox
```

### Step 7: Measure Impact
```bash
# Benchmark
time ./lox program.lox  # Should be faster
```

## Integration with Other Tools

See the main [workflow guide](../SKILL.md) for how this tool fits into the overall performance optimization workflow.

## Related Documentation

- [Main Workflow](../SKILL.md) - Overall performance optimization process
- [Usage Guide](./usage.md) - Command syntax and options
- [Analysis Guide](./analysis.md) - How to interpret output
- [Common Patterns](./patterns.md) - Problem identification
- [Resolutions](./resolutions.md) - How to fix issues
