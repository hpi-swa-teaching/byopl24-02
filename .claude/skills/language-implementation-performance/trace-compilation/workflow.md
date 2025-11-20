# Workflow Guide

This guide provides a practical workflow for using the trace-compilation tool.

## Quick Start

```bash
./lox --experimental-options \
  --engine.TraceCompilation \
  program.lox
```

See [usage.md](./usage.md) for detailed command options.

## Step-by-Step Investigation

The skill typically follows this analysis workflow:

### Step 1: CPU Profile First
```bash
./lox --cpusampler \
  --cpusampler.ShowTiers=true \
  --cpusampler.Delay=5000 \
  program.lox > cpu.txt
```
**Identify**: Hot functions and tier distribution

### Step 2: Basic Compilation Trace
```bash
./lox --experimental-options \
  --engine.TraceCompilation \
  program.lox > compilation.txt
```
**Verify**: Hot functions have "opt done" events

### Step 3: Check for Deoptimizations
```bash
grep "opt deopt" compilation.txt | sort | uniq -c | sort -rn
```
**Look for**: Repeated deoptimizations (same function multiple times)

### Step 4: Investigate Issues
**If deoptimization loop**:
```bash
./lox --experimental-options \
  --engine.TraceCompilation \
  --engine.TraceTransferToInterpreter \
  program.lox 2>&1
```

**If poor inlining**:
```bash
./lox --experimental-options \
  --engine.TraceCompilation \
  --engine.TraceInlining \
  --engine.CompileOnly=problemFunction \
  program.lox
```

**If queue issues**:
```bash
./lox --experimental-options \
  --engine.TraceCompilation \
  --engine.TraceCompilationDetails \
  program.lox | grep "Queue: Load"
```

### Step 5: Verify Fixes
```bash
# Re-run traces after fixes
# Confirm:
# - No repeated deoptimizations
# - Hot functions compile
# - Tier progression to T2
# - Reasonable compilation times
```

## Integration with Other Tools

See the main [workflow guide](../SKILL.md) for how this tool fits into the overall performance optimization workflow.

## Related Documentation

- [Main Workflow](../SKILL.md) - Overall performance optimization process
- [Usage Guide](./usage.md) - Command syntax and options
- [Analysis Guide](./analysis.md) - How to interpret output
- [Common Patterns](./patterns.md) - Problem identification
- [Resolutions](./resolutions.md) - How to fix issues
