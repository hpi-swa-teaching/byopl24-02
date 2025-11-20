# Workflow Guide

This guide provides a practical workflow for using the cpu-tracer tool.

## Quick Start

```bash
./lox --cputracer program.lox
```

See [usage.md](./usage.md) for detailed command options.

## Step-by-Step Investigation

The skill typically follows this analysis workflow:

### Step 1: Initial Function-Level Profile
```bash
./lox --cputracer program.lox > trace-functions.txt
```
**Analyze**: Find top 3-5 functions by total count

### Step 2: Check Compilation Effectiveness
**Look for**: Hot functions (>10,000 executions) with <95% compiled

### Step 3: Investigate Compilation Issues (if needed)
```bash
./lox --cputracer --engine.TraceCompilation program.lox
```
**Analyze**: Look for bailouts, deoptimizations affecting hot functions

### Step 4: Statement-Level Detail on Hot Functions
```bash
./lox --cputracer --cputracer.TraceStatements \
  --cputracer.FilterRootName=*hotFunction* \
  program.lox
```
**Analyze**: Find which specific statements dominate execution

### Step 5: Correlate with Time (CPU Sampler)
```bash
./lox --cpusampler --cpusampler.Delay=2000 \
  --cpusampler.ShowTiers=true \
  program.lox
```
**Compare**: High tracer count + high sampler time = critical optimization target

### Step 6: Verify Optimizations
After making changes, re-run Step 1 and compare:
- Did execution counts decrease? (Better algorithm)
- Did compiled % increase? (Fixed compilation barrier)

## Integration with Other Tools

### CPU Tracer + CPU Sampler (Recommended Combo)
```bash
# Run 1: Execution counts
./lox --cputracer program.lox > tracer.txt

# Run 2: Execution time
./lox --cpusampler --cpusampler.Delay=2000 --cpusampler.ShowTiers=true program.lox > sampler.txt

# Analyze correlation:
# - High tracer count + high sampler time = critical optimization target
# - High tracer count + low sampler time = frequent but fast (well-optimized)
# - Low tracer count + high sampler time = infrequent but slow operation
# - Low tracer count + low sampler time = not important
```

### CPU Tracer + Trace Compilation
```bash
./lox --cputracer --engine.TraceCompilation program.lox
```
- Tracer shows low compiled % → Trace compilation shows why
- Look for bailouts, deoptimizations, or missing compilation triggers

### CPU Tracer + Statement Tracing (Progressive Detail)
```bash
# Step 1: Find hot functions
./lox --cputracer program.lox

# Step 2: Trace statements in hot functions only
./lox --cputracer --cputracer.TraceStatements \
  --cputracer.FilterRootName=*hotFunction* \
  program.lox
```

## Related Documentation

- [Main Workflow](../SKILL.md) - Overall performance optimization process
- [Usage Guide](./usage.md) - Command syntax and options
- [Analysis Guide](./analysis.md) - How to interpret output
- [Common Patterns](./patterns.md) - Problem identification
- [Resolutions](./resolutions.md) - How to fix issues
