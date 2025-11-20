# Workflow Guide

This guide provides a practical workflow for using the memory-tracer tool.

## Quick Start

```bash
./lox --experimental-options --memtracer program.lox
```

See [usage.md](./usage.md) for detailed command options.

## Step-by-Step Investigation

The skill typically follows this analysis workflow:

### Step 1: CPU Profile First
```bash
./lox --cpusampler --cpusampler.Delay=2000 \
  --cpusampler.ShowTiers=true \
  program.lox > cpu.txt
```
**Identify**: Top 3-5 functions consuming most time

### Step 2: Memory Profile
```bash
./lox --experimental-options --memtracer \
  program.lox > memory.txt
```
**Identify**: Functions creating most allocations

### Step 3: Correlate Profiles
```bash
# Find functions in both profiles
grep "hotFunction" cpu.txt
grep "hotFunction" memory.txt
```
**Target**: Functions with high CPU and high allocations

### Step 4: Type Analysis (Optional)
```bash
./lox --experimental-options \
  --memtracer \
  --memtracer.Output=typehistogram \
  program.lox
```
**Understand**: What kinds of objects being created

### Step 5: Statement-Level Detail
```bash
./lox --experimental-options \
  --memtracer \
  --memtracer.TraceStatements \
  --memtracer.FilterRootName=*hotFunction* \
  program.lox
```
**Pinpoint**: Exact statements causing allocations

### Step 6: Optimize
- Reduce unnecessary allocations
- Reuse objects where possible
- Avoid intermediate collections
- Fix type stability issues

### Step 7: Verify
```bash
# Re-run memory profile
./lox --experimental-options --memtracer \
  program.lox > after-memory.txt

# Compare counts
diff memory.txt after-memory.txt

# Measure actual performance
time ./lox program.lox
```
**Confirm**: Allocations reduced and performance improved

## Integration with Other Tools

See the main [workflow guide](../SKILL.md) for how this tool fits into the overall performance optimization workflow.

## Related Documentation

- [Main Workflow](../SKILL.md) - Overall performance optimization process
- [Usage Guide](./usage.md) - Command syntax and options
- [Analysis Guide](./analysis.md) - How to interpret output
- [Common Patterns](./patterns.md) - Problem identification
- [Resolutions](./resolutions.md) - How to fix issues
