# Workflow Guide

This guide provides a practical workflow for using the trace-inlining tool.

## Quick Start

```bash
./lox --experimental-options --engine.TraceInlining program.lox
```

See [usage.md](./usage.md) for detailed command options.

## Step-by-Step Investigation

The skill typically follows this analysis workflow:

### Step 1: Profile to Find Hot Functions
```bash
./lox --cpusampler --cpusampler.Delay=2000 \
  --cpusampler.ShowTiers=true \
  program.lox
```
**Identify**: Top 3-5 functions consuming most time

### Step 2: Trace Inlining for Hot Functions
```bash
./lox --experimental-options \
  --engine.TraceInlining \
  --engine.CompileOnly=hotFunction \
  program.lox > inlining.txt
```
**Analyze**: Check if hot functions inline their callees

### Step 3: Identify Inlining Issues
**Look for**:
- Cutoff states on important calls
- Expanded states on hot functions
- BailedOut states (critical issues)
- High IR node counts approaching budget

### Step 4: Address Issues

**If Budget Exhaustion**:
```bash
./lox --experimental-options \
  --engine.TraceInlining \
  --engine.InliningExpansionBudget=18000 \
  program.lox
```

**If Functions Too Large**: Refactor into smaller pieces

**If Bailouts**: Investigate with detailed logging

### Step 5: Verify Improvements
```bash
# Re-run profiling
./lox --cpusampler --cpusampler.Delay=2000 \
  --cpusampler.ShowTiers=true \
  program.lox

# Compare inlining trace
./lox --experimental-options --engine.TraceInlining program.lox
```
**Check**: Did performance improve? Are more calls inlined?

## Integration with Other Tools

### Trace Inlining + Trace Compilation
```bash
./lox --experimental-options \
  --engine.TraceInlining \
  --engine.TraceCompilation \
  program.lox
```
**Why combine**:
- TraceCompilation: Overall compilation success, timing, IR size
- TraceInlining: Detailed inlining decisions that produced that result
- Correlate "Inlined X Y" counts in compilation with inlining states

### Trace Inlining + Performance Warnings
```bash
./lox --experimental-options \
  --engine.TraceInlining \
  --engine.TracePerformanceWarnings=call \
  program.lox
```
**Why combine**:
- Performance warnings: Flags virtual calls remaining in compiled code
- TraceInlining: Shows why calls didn't inline (budget, size, indirect)
- Essential combo for achieving peak performance

### Trace Inlining + CPU Sampler
```bash
# Step 1: Profile to find hot functions
./lox --cpusampler --cpusampler.Delay=2000 \
  --cpusampler.ShowTiers=true \
  program.lox > profile.txt

# Step 2: Analyze inlining for hot functions
./lox --experimental-options \
  --engine.TraceInlining \
  --engine.CompileOnly=hotFunction \
  program.lox > inlining.txt
```
**Why combine**:
- CPU Sampler: Identifies hot functions consuming time
- TraceInlining: Shows if hot functions inline properly
- Focus optimization on actual bottlenecks

### Trace Inlining + IGV (Visual Analysis)
```bash
./lox --experimental-options \
  --engine.TraceInlining \
  --vm.Djdk.graal.Dump=Truffle:1 \
  --vm.Djdk.graal.PrintGraph=Network \
  program.lox
```
**Why combine**:
- TraceInlining: Text-based decision log
- IGV: Visual call tree and IR graphs
- Visual graphs reveal patterns hard to spot in text

## Related Documentation

- [Main Workflow](../SKILL.md) - Overall performance optimization process
- [Usage Guide](./usage.md) - Command syntax and options
- [Analysis Guide](./analysis.md) - How to interpret output
- [Common Patterns](./patterns.md) - Problem identification
- [Resolutions](./resolutions.md) - How to fix issues
