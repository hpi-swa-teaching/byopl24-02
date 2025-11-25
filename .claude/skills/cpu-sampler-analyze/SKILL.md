---
name: Run and Analyze CPU Sampler
description: Runs CPU sampling profiler on a Lox program to identify performance bottlenecks and analyzes the results to provide actionable insights
---

# Skill: Run and Analyze CPU Sampler

This skill runs the CPU Sampler profiling tool on a Lox program and provides detailed analysis of the results to help identify performance bottlenecks and optimization opportunities.

## What This Skill Does

1. **Runs CPU Sampler**: Executes the Lox program with CPU sampling enabled using appropriate options
2. **Analyzes Results**: Interprets the profiling output to identify:
   - Hot functions consuming most execution time
   - Compilation effectiveness (interpreter vs compiled code)
   - Potential deoptimization issues
   - Algorithm inefficiencies
3. **Provides Recommendations**: Suggests specific optimizations based on the profiling data

## When to Use This Skill

- Initial performance investigation to understand where time is spent
- Verifying that critical code paths are being compiled
- Identifying deoptimization issues (functions stuck in interpreter mode)
- Comparing performance before/after optimizations
- Understanding execution patterns in Lox programs

## Prerequisites

Before running this skill, you should know:
- The path to the Lox program you want to profile
- Whether the program takes command-line arguments
- Approximate runtime of the program (to set appropriate delay)

## Reliability Protocol (MANDATORY)

**Context:** Tools often fail due to environment issues, permissions, or misconfiguration. To avoid hallucinating results, misinterpreting output, or wasting resources, you must follow this 3-step verification loop.

### Step 1: Pre-Execution Baseline

Before executing the primary task, establish a mental baseline:
* **Complexity Estimate:** asking yourself what you expect from a run with a trivial input (i.e. "If I run this on trivial input, how fast should it be?").
* **Failure Mode Prediction:** "If this tool is broken, will it hang, crash, or return empty text?"
* **Sanity Check:** If the tool takes 100x longer than your estimate, **STOP**. It is likely misconfigured or waiting on input.

### Step 2: The Probe (Dry Run)
Never run a complex or heavy command blind. Execute a **Probe** first:
* **The Test:** Run the exact command structure on a trivial target (e.g., `print "test";`, `SELECT 1`, or a dummy file).
* **Constraint:** If the Probe hangs, errors, or produces empty output, **STOP**. Do not proceed to the main task.

### Step 3: Output Audit (Verification)

Do not assume success based on exit codes.
* **Physical Check:** verify the output artifact exists and has a file size > 0 bytes.
* **Content Scan:** Read the first 5 lines/bytes of the output to ensure it is not an error message written to stdout (e.g., "Error: Command not found" saved inside `output.json`). Verify it's in the range of expected content and metrics. If it's to far off, **STOP**.

## How the Skill Works

The skill follows this workflow:

### 1. Initial Setup
- Confirms the Lox program path and any arguments
- Determines appropriate profiling parameters (delay, output format)

### 2. Run CPU Sampler
Executes the program with these recommended options:
```bash
./lox --cpusampler \
  --cpusampler.Delay=<ms> \
  --cpusampler.ShowTiers=true \
  --cpusampler.Output=histogram \
  <program.lox> [args...]
```

Key options explained:
- `--cpusampler.Delay`: Skip warmup phase to profile steady-state performance
- `--cpusampler.ShowTiers=true`: Show compilation tier information (T0/T1/T2)
- `--cpusampler.Output=histogram`: Default output format (can be changed to calltree or flamegraph)

### 3. Analyze Output

The skill looks for these key patterns:

#### High Self-Time Concentration
- **Symptom**: Single function with >80% self-time
- **Cause**: Algorithm inefficiency or excessive computation
- **Recommendation**: Optimize algorithm, consider caching, review data structures

#### High Interpreter Time (T0)
- **Symptom**: Hot function with >30% T0 time
- **Cause**: Deoptimization or compilation failure
- **Recommendations**:
  - Check for polymorphic call sites
  - Review Truffle node specializations
  - Use `--engine.TraceCompilation` for deeper analysis

#### Time Fragmented Across Functions
- **Symptom**: Many functions each consuming 1-5% time
- **Cause**: Excessive call overhead or lack of inlining
- **Recommendations**:
  - Use `--cpusampler.Mode=roots` to see inlined functions
  - Check `--engine.TraceInlining` for inlining decisions

#### Time in Internal Sources
- **Symptom**: No clear hotspots in application code
- **Cause**: Standard library or internal functions consuming time
- **Recommendation**: Re-run with `--cpusampler.SampleInternal=true`

### 4. Generate Follow-up Actions

Based on the analysis, the skill suggests:
- Specific functions to optimize
- Additional profiling commands to run (e.g., cpu-tracer for statement-level detail)
- Compilation flags to investigate issues
- Code changes to consider

## Example Usage

**User**: "Profile my benchmark.lox program"

**Skill Actions**:
1. Ask about runtime and arguments if not provided
2. Run: `./lox --cpusampler --cpusampler.Delay=2000 --cpusampler.ShowTiers=true benchmark.lox`
3. Analyze the histogram output
4. Identify top 3 hotspots
5. Check tier distribution (T0/T1/T2) for hot functions
6. Provide specific optimization recommendations

## Output Format Analysis

The skill interprets CPU Sampler output formats:

### Histogram Format
```
Name             || Total Time        || Self Time         || Location
---------------------------------------------------------------------------------
accept           || 2150ms 86.0%      || 2150ms 86.0%      || primes.lox~13-22:191-419
next             || 2470ms 98.8%      ||  320ms 12.8%      || primes.lox~31-37:537-737
:program         || 2500ms 100.0%     ||   30ms  1.2%      || primes.lox~1-46:0-982
```

Analysis focuses on:
- **Total Time**: Includes time in callees (hierarchical impact)
- **Self Time**: Direct computation time (immediate bottleneck)
- **Location**: Source file and line numbers for investigation

### Tier Information Format
```
Name      || Total Time | T0    | T1    | T2    || Self Time | T0    | T1    | T2
---------------------------------------------------------------------------------------
accept    || 4860ms 87.9% | 31.1% | 18.3% | 50.6% || 4860ms 87.9% | 31.1% | 18.3% | 50.6%
```

Analysis focuses on:
- **T0** (Interpreter): High values indicate compilation issues
- **T1** (Tier-1 Compiled): Basic optimizations applied
- **T2** (Tier-2 Compiled): Full optimizations - target for hot code

## Advanced Options

The skill can use additional options when needed:

### Flamegraph Generation
For complex call patterns:
```bash
./lox --cpusampler=flamegraph \
  --cpusampler.OutputFile=profile.svg \
  --cpusampler.Delay=2000 \
  program.lox
```

### Call Tree Mode
To understand call hierarchies:
```bash
./lox --cpusampler --cpusampler.Output=calltree \
  --cpusampler.Delay=2000 \
  program.lox
```

### Include Inlined Functions
For complete call picture:
```bash
./lox --cpusampler --cpusampler.Mode=roots \
  --cpusampler.ShowTiers=true \
  --cpusampler.Delay=2000 \
  program.lox
```

### Sample Internal Sources
When application code looks clean:
```bash
./lox --cpusampler --cpusampler.SampleInternal=true \
  --cpusampler.ShowTiers=true \
  --cpusampler.Delay=2000 \
  program.lox
```

## Complementary Tools

The skill may recommend using these tools for deeper analysis:

- **CPU Tracer**: Count execution frequencies (not time)
  ```bash
  ./lox --cputracer --cputracer.TraceStatements program.lox
  ```

- **Trace Compilation**: Understand compilation/deoptimization
  ```bash
  ./lox --engine.TraceCompilation program.lox
  ```

- **Trace Inlining**: See inlining decisions
  ```bash
  ./lox --engine.TraceInlining program.lox
  ```

## Best Practices

The skill follows these profiling best practices:

1. **Always use delay**: Skip warmup phase with `--cpusampler.Delay=<ms>`
2. **Start with histogram**: Get overview before drilling down
3. **Enable tier info**: Always use `--cpusampler.ShowTiers=true`
4. **Progressive analysis**: histogram → calltree → flamegraph as needed
5. **Verify fixes**: Re-run profiling after optimizations to confirm improvements

## Common Pitfalls to Avoid

The skill warns about these common mistakes:

- ❌ Profiling warmup phase (missing `--cpusampler.Delay`)
- ❌ Ignoring tier information (missing `--cpusampler.ShowTiers`)
- ❌ Not sampling internal sources when needed
- ❌ Using default mode when inlined functions are important
- ❌ Forgetting to specify unique `--cpusampler.OutputFile` for flamegraphs

## Reference Documentation

For detailed information, see:
- `/Users/antonykamp/Projects/hpi-ma/byopl24-02/docs/commands/cpu-sampler.md` - Complete CPU Sampler documentation
- Official GraalVM docs: https://www.graalvm.org/latest/tools/profiling/
- Optimization guide: https://github.com/oracle/graal/blob/master/truffle/docs/Optimizing.md

## Implementation Notes

This skill:
- Uses the Lox launcher: `./lox`
- Defaults to 2000ms delay for most programs
- Prefers histogram output for initial analysis
- Always enables tier information
- Provides line-number references for investigation
- Suggests follow-up actions based on findings
