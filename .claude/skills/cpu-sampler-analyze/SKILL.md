---
name: Run and Analyze CPU Sampler
description: Runs CPU sampling profiler on your language implementation to identify performance bottlenecks and analyzes the results to provide actionable insights
---

# Skill: Run and Analyze CPU Sampler

This skill runs the CPU Sampler profiling tool on your language implementation and provides detailed analysis of the results to help identify performance bottlenecks and optimization opportunities.

## What This Skill Does

1. **Runs CPU Sampler**: Executes your language implementation with CPU sampling enabled using appropriate options
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
- Understanding execution patterns in your language implementation

## Prerequisites

Before running this skill, you should know:
- **Required**: Having benchmark baseline data for comparison
- The path to the program you want to profile
- Whether the program takes command-line arguments
- Approximate runtime of the program (to set appropriate delay)

## Fermi Verification (MANDATORY)

**Context:** Tools often fail due to environment issues, permissions, or misconfiguration. To avoid hallucinating results, misinterpreting output, or wasting resources, you must follow this 4-step verification loop.

**Mitigation Strategy**: Fermi Verification Before accepting the tool's output, you must:

1. Estimate: Look at the complexity of the code. Perform a "Fermi Calculation" to estimate the expected runtime in milliseconds or seconds (e.g., 'This loops 10k times with O(n) work, I expect ~100-1000ms runtime').
2. Probe: Run the tool on a trivial input (e.g., a minimal program) to ensure it produces output quickly and correctly.
3. Execute: Run the tool on the real target.
4. Compare: If the tool output deviates from your Fermi Estimate by more than one order of magnitude (or is zero):
   - **STOP - The tool is broken or misconfigured**
   - **Do NOT rationalize unexpected results** (e.g., "maybe optimizations eliminated everything")
   - Verify tool prerequisites are met before proceeding
   - Invalid tool output = invalid analysis

## How the Skill Works

In all examples, `<launcher>` refers to your programming language launcher script.

The skill follows this workflow:

### 1. Initial Setup
- Confirms the program path and any arguments
- Determines appropriate profiling parameters (delay, output format)

### 2. Run CPU Sampler
Executes the program with these recommended options:
```bash
<launcher> --cpusampler \
  --cpusampler.Delay=<ms> \
  --cpusampler.ShowTiers=true \
  --cpusampler.Output=histogram \
  <program> [args...]
```

Key options explained:
- `--cpusampler.Delay`: Skip warmup phase to profile steady-state performance
- `--cpusampler.ShowTiers=true`: Show compilation tier information (T0/T1/T2)
- `--cpusampler.Output=histogram`: Default output format (can be changed to calltree or flamegraph)
- "Warmup phase" = JIT compilation happening; profiling it skews results

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

**User**: "Profile my <program> program"

**Skill Actions**:
1. Ask about runtime and arguments if not provided
2. Run: `<launcher> --cpusampler --cpusampler.Delay=2000 --cpusampler.ShowTiers=true <program>`
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
accept           || 2150ms 86.0%      || 2150ms 86.0%      || primes~13-22:191-419
next             || 2470ms 98.8%      ||  320ms 12.8%      || primes~31-37:537-737
:program         || 2500ms 100.0%     ||   30ms  1.2%      || primes~1-46:0-982
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
- Target: >80% T2 for hot functions, <10% T0

## Advanced Options

The skill can use additional options when needed:

### Flamegraph Generation
For complex call patterns:
```bash
<launcher> --cpusampler \
  --cpusampler.Output=flamegraph \
  --cpusampler.OutputFile=profile.svg \
  --cpusampler.Delay=2000 \
  <program> [script args]
```

- Open profile.svg in a web browser to visualize the flamegraph

### Call Tree Mode
To understand call hierarchies:
```bash
<launcher> --cpusampler --cpusampler.Output=calltree \
  --cpusampler.Delay=2000 \
  <program> [script args]
```

### Include Inlined Functions
For complete call picture:
```bash
<launcher> --cpusampler --cpusampler.Mode=roots \
  --cpusampler.ShowTiers=true \
  --cpusampler.Delay=2000 \
  <program> [script args]
```

### Sample Internal Sources
When application code looks clean:
```bash
<launcher> --cpusampler --cpusampler.SampleInternal=true \
  --cpusampler.ShowTiers=true \
  --cpusampler.Delay=2000 \
  <program> [script args]
```

## Complementary Tools

The skill may recommend using these tools for deeper analysis:

- **CPU Tracer**: Count execution frequencies (not time)
  ```bash
  <launcher> --cputracer --cputracer.TraceStatements <program> [script args]
  ```

- **Trace Compilation**: Understand compilation/deoptimization
  ```bash
  <launcher> --engine.TraceCompilation <program> [script args]
  ```

- **Trace Inlining**: See inlining decisions
  ```bash
  <launcher> --engine.TraceInlining <program> [script args]
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
- Official GraalVM docs: https://www.graalvm.org/latest/tools/profiling/
- Optimization guide: https://github.com/oracle/graal/blob/master/truffle/docs/Optimizing.md
- Use Graal Truffle Docs skill

## Implementation Notes

This skill:
- Uses your language's launcher: `<launcher>`
- Defaults to 2000ms delay for most programs
- Prefers histogram output for initial analysis
- Always enables tier information
- Provides line-number references for investigation
- Suggests follow-up actions based on findings
