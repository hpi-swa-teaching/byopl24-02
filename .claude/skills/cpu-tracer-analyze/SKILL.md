---
name: cpu-tracer-analyze
description: Counts exact execution frequencies at function/statement level with interpreted vs compiled split. Use to verify compilation effectiveness (aim for >95% compiled), understand control flow patterns, validate algorithmic complexity, and quantify execution frequencies. Complements CPUSampler by showing frequency rather than duration.
---

# Skill: Run and Analyze CPU Tracer

This skill runs the CPU Tracer profiling tool on your language implementation and provides detailed analysis of execution count patterns to help identify compilation issues, control flow patterns, and algorithmic complexity problems.

## What This Skill Does

1. **Runs CPU Tracer**: Executes the program with CPU tracing at appropriate granularity (roots/calls/statements)
2. **Analyzes Execution Counts**: Interprets the profiling output to identify:
   - Execution hotspots (frequently executed code paths)
   - Compilation effectiveness (interpreted vs compiled execution split)
   - Algorithmic complexity patterns
   - Specialization and splitting behavior
3. **Provides Actionable Recommendations**: Suggests specific optimizations based on execution count patterns

## When to Use This Skill

- Verify functions are being compiled (aim for >95% compiled on hot paths)
- Understand actual control flow patterns and loop iteration counts
- Validate algorithmic complexity matches expectations
- Quantify how often different code paths execute
- Debug why hot code remains interpreted instead of compiled
- Complement CPUSampler by correlating time with execution frequency

## Key Difference from CPU Sampler

**CPU Tracer counts executions** (frequency), while **CPU Sampler measures time** (duration):
- High tracer count + low sampler time = frequent but fast (well-optimized)
- Low tracer count + high sampler time = infrequent but slow (optimization target)
- Best practice: Use both tools together for complete understanding

## Prerequisites

Before running this skill, you should know:
- **Required**: Having benchmark baseline data for comparison
- The path to the program you want to profile
- Whether you need function-level or statement-level granularity
- Any specific functions to filter (for focused analysis)

## Fermi Verification: The Sanity Gate (MANDATORY)

**Principle:**  
The Tool Output is the highest authority for *data*, but your Fermi Estimate is the highest authority for *pipeline integrity*.

**The Logic:**
- **Small Deviation:** Tool works correctly. Update your mental model.
- **Massive Deviation (>1 Order of Magnitude):** Tool is likely **malfunctioning** (silent failure, misconfiguration, or wrong target).

**Protocol:**

### Step 1: Pre-Calculation
- In a scratchpad, estimate the expected output magnitude (e.g., "This acts on an array of 10k items, so I expect at least 10k executions").
- *Key:* You must write this down *before* generating the tool command.

### Step 2: Smoke Test (The Probe)
- Run on trivial input first to prove the tool *can* work.

### Step 3: Execute & Validate
- Run the actual command.
- **Credibility Threshold Check:** Compare Output vs. Estimate.
  - **Scenario A (Within 1 Order of Magnitude):** **ACCEPT.** The tool is the authority. Proceed with this result.
  - **Scenario B (>1 Order of Magnitude Divergence OR Unexpected Zero):** **REJECT & DIAGNOSE.**
    - **STOP.** Do not use this result for the next step.
    - **Hypothesis:** The tool failed silently, the path is wrong, or permissions are denied.
    - **Action:** Run a *Debug Command* (e.g., `ls -l target_file` to check size, or `echo $?` to check exit code) to prove the tool is healthy.
    - *Only* after the tool's health is proven via a secondary check may you accept the divergent result.

## How the Skill Works

In all examples, `<launcher>` refers to your programming language launcher script.

The skill follows this workflow:

### 1. Initial Setup
- Confirms the program path and any arguments
- Determines appropriate tracing granularity (roots/calls/statements)
- Sets up filters if focusing on specific code

### 2. Run CPU Tracer

Executes with these granularity levels:

#### Function-Level (Default - Low Overhead)
```bash
<launcher> --cputracer <program> [script args]
```
- Traces function/method entries only
- Shows total executions and interpreted vs compiled split
- Lowest overhead, suitable for longer runs

#### Call-Site Level (Medium Overhead)
```bash
<launcher> --cputracer --cputracer.TraceCalls <program>
```
- Adds call-site tracking within functions
- Shows which call sites are hot
- Moderate overhead

#### Statement-Level (Highest Overhead - Use with Filters!)
```bash
<launcher> --cputracer --cputracer.TraceStatements \
  --cputracer.FilterRootName=*hotFunction* \
  <program>
```
- Traces every statement execution
- Most detailed granularity for understanding control flow
- HIGH overhead - always use with filters

### 3. Analyze Output

The skill looks for these key patterns:

#### Low Compilation Percentage on Hot Code
- **Symptom**: High execution count (>10,000) with <95% compiled
- **Cause**: Compilation barrier, deoptimization, or compilation threshold not reached
- **Example**:
  ```
  accept    | 234117338 50.0%  | 100000 42.7%  | 134117338 57.3%  | <source>~15:245-258
  ```
  This shows only 57.3% compiled despite 234M executions - problematic!

- **Recommendations**:
  - Use `--engine.TraceCompilation` to see compilation events
  - Check for bailouts or deoptimization reasons
  - Review function for compilation barriers (reflective ops, boundaries)
  - Look for unstable types causing repeated invalidation
  - Target: >95% compiled for hot paths

#### Execution Hotspot Identification
- **Symptom**: Few functions consuming 80%+ of total executions
- **Example**:
  ```
  innerLoop | 450000000 85.0%  | 5000 0.0%     | 449995000 100.0% | <program>~42:512-530
  helper    |  50000000 9.5%   | 2000 0.0%     |  49998000 100.0% | <program>~18:203-220
  setup     |      5000 0.0%   | 5000 100.0%   |         0 0.0%   | <program>~5:45-89
  ```
  Focus on `innerLoop` and `helper` - they dominate execution

- **Interpretation**:
  - Top entries are critical path - optimize these first
  - Concentrated execution is ideal for focused optimization
  - Diffuse execution suggests architectural issues

- **Recommendations**:
  - Focus optimization on highest-count functions
  - Use `--cputracer.TraceStatements` on hot functions to find hot statements
  - Verify hot functions are compiled (>95% compiled)

#### Algorithmic Complexity Issues
- **Symptom**: Execution counts scale worse than expected with input size
- **Example**: Doubling input size quadruples execution counts (O(n²) instead of O(n))

- **Recommendations**:
  - Use statement-level tracing to find nested loops
  - Compare execution counts across different input sizes
  - Look for redundant iterations or missing early termination
  - Consider caching or memoization

#### Over-Specialization/Splitting
- **Symptom**: Many entries with same function name but different locations
- **Example**:
  ```
  process | 100000 45%  | 1000 1.0%  | 99000 99.0%   | app.lox~10:100-120
  process |  80000 36%  | 800 1.0%   | 79200 99.0%   | app.lox~11:125-145
  process |  30000 13.5% | 300 1.0%   | 29700 99.0%   | app.lox~12:150-170
  process |   8000 3.6%  | 80 1.0%    |  7920 99.0%   | app.lox~13:175-195
  process |   4000 1.8%  | 40 1.0%    |  3960 99.0%   | app.lox~14:200-220
  ```

- **Interpretation**:
  - Multiple specializations with high compiled % is GOOD (first 3 entries)
  - Too many variants with low counts (<1000) is BAD (last 2 entries)

- **Recommendations**:
  - A few heavily-executed specializations = good
  - Many lightly-executed specializations = code cache pollution
  - Consider consolidating specialization cases
  - Review DSL specialization annotations

### 4. Filter Strategies

The skill uses filters to manage overhead:

#### Filter by Function Name
```bash
<launcher> --cputracer --cputracer.FilterRootName=*parse* <program>
```
Focus on functions matching pattern (e.g., all parsing functions)

#### Filter by File
```bash
<launcher> --cputracer --cputracer.FilterFile=*benchmark* <program>
```
Only trace code in specific files

#### Combined Filtering
```bash
<launcher> --cputracer --cputracer.TraceStatements \
  --cputracer.FilterRootName=*innerLoop* \
  --cputracer.FilterFile=*<program>* \
  <program>
```
Statement-level detail on specific function in specific file

#### Output as JSON
```bash
<launcher> --cputracer --cputracer.Output=json \
  --cputracer.OutputFile=trace.json \
  <program>
```
Machine-readable format for programmatic analysis

### 5. Generate Follow-up Actions

Based on the analysis, the skill suggests:
- Specific functions to optimize
- Compilation tracing to investigate low compiled percentages
- Statement-level tracing for hot functions
- Algorithmic improvements for complexity issues
- Correlation with CPUSampler for time/frequency analysis

## Output Format Analysis

### Histogram Format

The skill interprets CPU Tracer histogram output:

```
-----------------------------------------------------------------------------------------
Tracing Histogram. Counted a total of 468336895 element executions.

Total Count: Number of times the element was executed and percentage of total executions.
Interpreted Count: Number of times the element was interpreted and percentage of total executions of this element.
Compiled Count: Number of times the compiled element was executed and percentage of total executions of this element.
-----------------------------------------------------------------------------------------

Name       | Total Count      | Interpreted Count | Compiled Count    | Location
-----------------------------------------------------------------------------------------
accept     | 234117338 50.0%  | 365660 0.2%      | 233751678 99.8%  | <source>~15:245-258
```

#### JSON Format

```json
{
    "version": "0.3.0",
    "profile": [
        {
            "source_section": {
                "path": "/Users/antonykamp/Projects/hpi-ma/byopl24-02/<program>",
                "language": "<language>",
                "end_column": 1,
                "end_line": 16,
                "start_column": 1,
                "source_name": "<program>",
                "start_line": 1
            },
            "compiled_count": 0,
            "root_name": "init",
            "interpreted_count": 2,
            "count": 2
        },
        {
            "source_section": {
                "path": "<path>",
                "language": "<language>",
                "end_column": 1,
                "end_line": 52,
                "start_column": 1,
                "source_name": "<source>",
                "start_line": 1
            },
            "compiled_count": 0,
            "root_name": "init",
            "interpreted_count": 8,
            "count": 8
        },
        {
            "source_section": {
                "path": "<path>",
                "language": "<language>",
                "end_column": 11,
                "end_line": 10,
                "start_column": 1,
                "source_name": "<source>",
                "start_line": 2
            },
            "compiled_count": 0,
            "root_name": "processArguments",
            "interpreted_count": 1,
            "count": 1
        },
        {
            "source_section": {
                "path": "<path>",
                "language": "<language>",
                "end_column": 33,
                "end_line": 57,
                "start_column": 1,
                "source_name": "<source>",
                "start_line": 1
            },
            "compiled_count": 9249,
            "root_name": "init",
            "interpreted_count": 2834,
            "count": 12083
        }
    ],
    "tool": "cputracer"
}
```

### Field Interpretation

- **Name**: Function/method/statement identifier
- **Total Count**: Absolute execution count and % of all traced executions
  - Higher = hotter code path
  - Top entries consuming 80%+ are critical path
- **Interpreted Count**: Executions before compilation
  - Absolute count and % relative to this element's total
  - Initial executions are interpreted (warmup phase)
- **Compiled Count**: Executions after Graal compilation
  - Absolute count and % relative to this element's total
  - **Target: >95% for hot code (>10,000 total executions)**
- **Location**: Source file, line numbers, character offsets
  - Format: `file.lox~line:offset` or `file.lox~startLine-endLine:startOffset-endOffset`

### Key Analysis Rules

1. **Hot code (>10,000 executions) should be >95% compiled**
   - If not, investigate compilation issues

2. **Total count % reveals critical path**
   - Focus on entries consuming majority of executions

3. **Multiple entries with same name = specialization**
   - Good if heavily executed
   - Bad if too many with low counts

4. **Setup/initialization code will be 100% interpreted**
   - This is normal and expected (runs once)

## Correlation with Other Tools

### CPU Tracer + CPU Sampler (Recommended Combo)
```bash
# Run 1: Execution counts
<launcher> --cputracer <program> [script args] > tracer.txt

# Run 2: Execution time
<launcher> --cpusampler --cpusampler.Delay=2000 --cpusampler.ShowTiers=true <program> [script args] > sampler.txt

# Analyze correlation:
# - High tracer count + high sampler time = critical optimization target
# - High tracer count + low sampler time = frequent but fast (well-optimized)
# - Low tracer count + high sampler time = infrequent but slow operation
# - Low tracer count + low sampler time = not important
```

### CPU Tracer + Trace Compilation
```bash
<launcher> --cputracer --engine.TraceCompilation <program> [script args]
```
- Tracer shows low compiled % → Trace compilation shows why
- Look for bailouts, deoptimizations, or missing compilation triggers

### CPU Tracer + Statement Tracing (Progressive Detail)
```bash
# Step 1: Find hot functions
<launcher> --cputracer <program> [script args]

# Step 2: Trace statements in hot functions only
<launcher> --cputracer --cputracer.TraceStatements \
  --cputracer.FilterRootName=*hotFunction* \
  <program> [script args]
```

## Advanced Options

### JSON Output for Analysis
```bash
<launcher> --cputracer --cputracer.Output=json \
  --cputracer.OutputFile=trace.json \
  <program>
```
- Machine-readable format
- Process with `jq` or custom scripts
- Contains hierarchical call graph information

### Disable Compilation for Debugging
```bash
<launcher> --cputracer --engine.BackgroundCompilation=false <program>
```
- Makes behavior deterministic
- Easier to correlate with execution patterns
- Useful for debugging

## Best Practices

The skill follows these profiling best practices:

1. **Progressive Granularity**
   1. Start with `--cputracer` only (function-level)
   2. Add `--cputracer.TraceCalls` for hot functions
   3. Add `--cputracer.TraceStatements` with filters for deep dive

2. **Always Filter with TraceStatements**
   ```bash
   # ❌ BAD: Overwhelming overhead and output
   <launcher> --cputracer --cputracer.TraceStatements <program>

   # ✅ GOOD: Focused statement-level detail
   <launcher> --cputracer --cputracer.TraceStatements \
     --cputracer.FilterRootName=*hotFunction* \
     <program>
   ```

3. **Save Output to Files**
   ```bash
   <launcher> --cputracer --cputracer.OutputFile=trace-run1.txt <program>
   ```
   - Enables comparison across runs
   - Prevents terminal buffer overflow

4. **Use Short Representative Workloads**
   - Enough iterations to reach steady state and trigger compilation
   - Not unnecessarily long given overhead
   - Representative of actual usage patterns

5. **Target >95% Compiled for Hot Paths**
   - Hot code = >10,000 executions
   - <95% compiled = investigate with TraceCompilation

6. **Combine with CPUSampler**
   - Understand both frequency (tracer) and duration (sampler)
   - Correlate to find true optimization targets

## Common Pitfalls to Avoid

The skill warns about these common mistakes:

- ❌ **Confusing with CPUSampler**: CPU Tracer counts executions, doesn't measure time
- ❌ **TraceStatements without filters**: Overwhelming overhead and output on large codebases
- ❌ **Ignoring multiple entries**: Same function name with different locations = specializations, not duplicates
- ❌ **Expecting real-time output**: Output appears only at program completion
- ❌ **Parsing histogram text**: Use `--cputracer.Output=json` for programmatic analysis
- ❌ **Drawing performance conclusions from overhead**: Runtime is affected by instrumentation, only counts are meaningful

## Typical Workflow

The skill typically follows this analysis workflow:

### Step 1: Initial Function-Level Profile
```bash
<launcher> --cputracer <program> [script args] > trace-functions.txt
```
**Analyze**: Find top 3-5 functions by total count

### Step 2: Check Compilation Effectiveness
**Look for**: Hot functions (>10,000 executions) with <95% compiled

### Step 3: Investigate Compilation Issues (if needed)
```bash
<launcher> --cputracer --engine.TraceCompilation <program>
```
**Analyze**: Look for bailouts, deoptimizations affecting hot functions

### Step 4: Statement-Level Detail on Hot Functions
```bash
<launcher> --cputracer --cputracer.TraceStatements \
  --cputracer.FilterRootName=*hotFunction* \
  <program>
```
**Analyze**: Find which specific statements dominate execution

### Step 5: Correlate with Time (CPU Sampler)
```bash
<launcher> --cpusampler --cpusampler.Delay=2000 \
  --cpusampler.ShowTiers=true \
  <program>
```
**Compare**: High tracer count + high sampler time = critical optimization target

### Step 6: Verify Optimizations
After making changes, re-run Step 1 and compare:
- Did execution counts decrease? (Better algorithm)
- Did compiled % increase? (Fixed compilation barrier)

## Example Analysis

### Example Output
```
Name          | Total Count      | Interpreted Count | Compiled Count    | Location
------------------------------------------------------------------------------------------
innerBench    | 500000000 71.4%  | 15000 0.003%     | 499985000 99.997% | <source>~42:512-530
isPrime       | 150000000 21.4%  | 8000 0.005%      | 149992000 99.995% | <source>~18:203-220
next          |  50000000 7.1%   | 2000 0.004%      |  49998000 99.996% | <source>~31:401-420
setup         |        10 0.0%   | 10 100.0%        |         0 0.0%    | <source>~5:45-89
```

### Skill Analysis
1. **Execution hotspots**: `innerBench` dominates with 71.4% of all executions - primary optimization target
2. **Compilation effectiveness**: ✅ All hot functions show >99.99% compiled - excellent!
3. **Setup code**: `setup` runs once (10 times), 100% interpreted - normal and expected
4. **Recommendation**: All functions are well-compiled. Use statement-level tracing on `innerBench` to find optimization opportunities within the function:
   ```bash
   <launcher> --cputracer --cputracer.TraceStatements \
     --cputracer.FilterRootName=*innerBench* \
     <source>
   ```

## Reference Documentation

For detailed information, see:
- Official GraalVM docs: https://www.graalvm.org/latest/tools/profiling/
- CPUTracer JavaDoc: https://www.graalvm.org/tools/javadoc/com/oracle/truffle/tools/profiler/CPUTracer.html
- Use Graal Truffle Docs skill

## Implementation Notes

This skill:
- Uses your language's launcher: `<launcher>`
- Defaults to function-level granularity (lowest overhead)
- Uses filters when enabling statement-level tracing
- Analyzes compiled % with >95% target for hot code
- Provides location references for investigation
- Suggests correlation with CPUSampler for complete picture
- Saves output to files for comparison across runs
- Combined with other performance analysis skills for full picture

## Related Skills

- Use Graal Truffle Docs skill to understand Truffle APIs and options
- Use CPU Sampler Analyze skill for initial profiling to identify hot functions
- Use Performance Warnings Analyze skill to find optimization barriers
- Use Compilation Trace Analyze skill to see inlining and compilation decisions
- Use Memory Tracer Analyze skill for allocation profiling
- Use Trace Inlining Analyze skill for inlining decision analysis
- Use Trace Transfer to Interpreter Analyze skill for deoptimization insights
- Use Benchmark Baseline skill for creating performance baselines with different benchmarks
