# Workflow Guide

This guide provides a practical workflow for using the trace-transfer-to-interpreter tool.

## Quick Start

```bash
./lox --experimental-options \
  --engine.TraceTransferToInterpreter \
  program.lox
```

See [usage.md](./usage.md) for detailed command options.

## Step-by-Step Investigation

The skill typically follows this analysis workflow:

### Step 1: Initial Trace
```bash
./lox --experimental-options \
  --engine.TraceTransferToInterpreter \
  --engine.TraceCompilation \
  program.lox 2>&1 | tee initial-trace.log
```

### Step 2: Count and Categorize Transfers
```bash
# Total transfers
grep -c "transferToInterpreter" initial-trace.log

# Transfers per location
grep "transferToInterpreter at" initial-trace.log | \
  sort | uniq -c | sort -rn > transfer-counts.txt

# Look for high counts (deoptimization loops)
head -20 transfer-counts.txt
```

### Step 3: Identify Deoptimization Loops
```bash
# Locations with 10+ transfers
awk '$1 >= 10' transfer-counts.txt
```
**If found**: Critical problem! Fix immediately.

### Step 4: Analyze Compilation Cycles
```bash
# Extract compilation and deopt events for specific function
grep "myFunction" initial-trace.log | \
  grep -E "opt done|opt deopt|transferToInterpreter"
```
**Look for**: Compile → transfer → deopt → recompile pattern

### Step 5: Fix Issues
- **Deoptimization loops**: Fix type stability, add specializations
- **Property access**: Stabilize object shapes
- **Excessive warmup**: Increase compilation thresholds

### Step 6: Verify Fixes
```bash
# Re-run trace
./lox --experimental-options \
  --engine.TraceTransferToInterpreter \
  program.lox 2>&1 | tee fixed-trace.log

# Count transfers
grep -c "transferToInterpreter" fixed-trace.log

# Goal: Zero or single-digit count
```

### Step 7: Measure Performance Impact
```bash
# Before fix
time ./lox program.lox

# After fix (should be much faster)
time ./lox program.lox
```

## Integration with Other Tools

### Transfer Tracer + Compilation Trace (Essential!)
```bash
./lox --experimental-options \
  --engine.TraceTransferToInterpreter \
  --engine.TraceCompilation \
  program.lox 2>&1 | tee full-trace.log
```
**Why**:
- TraceCompilation: When/what compiled, invalidations
- TraceTransferToInterpreter: Where/why deoptimizations
- Together: Complete deoptimization cycle picture

### Transfer Tracer + Assumption Trace
```bash
./lox --experimental-options \
  --engine.TraceTransferToInterpreter \
  --engine.TraceAssumptions \
  program.lox 2>&1 | tee assumptions.log
```
**Why**:
- TraceAssumptions: Which specific assumptions invalidating
- TraceTransferToInterpreter: Where deoptimizations occur
- Together: Understand assumption instability

### Transfer Tracer + CPU Sampler
```bash
# Step 1: Identify deoptimization issues
./lox --experimental-options \
  --engine.TraceTransferToInterpreter \
  program.lox 2>transfers.log

# Step 2: Profile to see impact
./lox --cpusampler --cpusampler.ShowTiers=true \
  --cpusampler.Delay=10000 \
  program.lox
```
**Why**:
- CPU Sampler: Shows time in interpreted vs compiled
- Transfer Tracer: Shows why code is interpreted
- Together: Quantify deoptimization performance impact

### Transfer Tracer + Compilation Statistics
```bash
./lox --experimental-options \
  --engine.TraceTransferToInterpreter \
  --engine.CompilationStatistics \
  program.lox 2>&1
```
**Why**:
- CompilationStatistics: Aggregate invalidation counts/rates
- TraceTransferToInterpreter: Individual deoptimization events
- Together: Quantify overall deoptimization problem

## Related Documentation

- [Main Workflow](../SKILL.md) - Overall performance optimization process
- [Usage Guide](./usage.md) - Command syntax and options
- [Analysis Guide](./analysis.md) - How to interpret output
- [Common Patterns](./patterns.md) - Problem identification
- [Resolutions](./resolutions.md) - How to fix issues
