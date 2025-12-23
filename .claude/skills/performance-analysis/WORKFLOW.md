# Performance Analysis Workflow

Detailed 4-phase workflow for systematic performance analysis.

---

## Phase 1: Load Benchmark and Baseline Data

**Objective**: Gather all existing performance data and expectations

### Process

1. **Load benchmark results**
   - Read `BENCHMARK_BASELINE.md` for benchmark list
   - Request user to provide timing data or locate result files
   - Parse execution times for each benchmark
   - Identify successful vs failed benchmarks

2. **Load baseline comparison data**
   - Read `BENCHMARK_BASELINE.md`:
     - Language characteristics
     - Comparable languages
     - Expected performance ranges per benchmark
     - Reference implementation times
   - Extract performance expectations

3. **Initial comparison**
   - Calculate performance ratios (actual / expected)
   - Identify significantly slower benchmarks (>2x slower than best case)
   - Identify unexpectedly slow benchmarks (>expected range)
   - Identify unexpectedly fast benchmarks (<expected range)

### Output

- Benchmark execution times loaded
- Baseline expectations loaded
- Performance gap analysis
- List of problem benchmarks

### Example

```
Loaded Results:
- queens: 2.1s (actual)
- Expected range: 0.8s (Lua best) to 3.5s (Python worst)
- Ratio: 2.6x slower than best case
- Status: ⚠️ INVESTIGATE (slower than expected)

- bounce: 1.2s (actual)
- Expected range: 0.5s to 2.0s
- Status: ✅ OK (within expected range)
```

---

## Phase 2: Generate Performance Theories

**Objective**: Generate testable performance theories based on gaps and patterns

**Target**: 5-15 most impactful theories (comprehensive but bounded)
**Time Budget**: 15-20 minutes for theory generation

### Process

1. **Analyze performance gaps**
   - For each benchmark significantly slower than baseline:
     - Identify gap magnitude (2x, 3x, 10x slower)
     - Consider benchmark type (recursive, allocation-heavy, arithmetic)
     - Generate specific theories

2. **Apply pattern matching**
   - **Recursive benchmarks** → Inlining/caching theories
   - **Allocation benchmarks** → Escape analysis theories
   - **Arithmetic benchmarks** → Specialization theories
   - **All slow benchmarks** → Compilation effectiveness theories

3. **Systematic code analysis for anti-patterns** (CRITICAL - Most Important)

   **MUST analyze the following systematically**:

   a. **Language definition & bytecode configuration**
      - Check configuration settings for optimization opportunities
      - Identify missing or suboptimal configurations

   b. **ALL operations/nodes in the implementation**
      - Check EVERY operation for missing optimizations
      - Look for patterns that prevent compilation
      - Identify missing specializations or caching

   c. **Runtime data structures and types**
      - Analyze allocation patterns
      - Check for optimization boundaries
      - Identify inefficient data structure choices

   d. **Frame and variable access patterns**
      - Analyze slot access patterns
      - Check for dynamic vs constant access
      - Identify materialization overhead

   e. **Library and interop usage**
      - Check for uncached library usage
      - Analyze limit parameters on cached libraries
      - Identify missing exports or specializations

   **For each anti-pattern found**:
   - Record specific file location (file:line)
   - Extract code excerpt showing the issue
   - Estimate impact (critical/high/moderate/minor)
   - Identify root cause (architectural vs implementation)

4. **Prioritize theories by impact × verification cost**
   - **Priority 1**: Critical impact, quick verification (5-10 min per theory)
   - **Priority 2**: High impact, moderate verification (10-20 min per theory)
   - **Priority 3**: Moderate impact or complex verification (20-30 min per theory)

   **Limit to top 15 theories** - rank by (impact score / verification time)

5. **Select ALL verification tools needed for each theory**
   - Match theory type to appropriate tools
   - **CRITICAL**: List ALL tools needed for 100% proof (not just one)
   - If theory requires multiple tools for complete verification, list them all
   - Example: Architectural theory about data structure choice may need:
     - Profiler to measure frequency
     - Memory tracer to measure allocation overhead
     - Compilation tracer to verify optimization barriers
   - Consider verification workflow dependencies

### Output

- Prioritized list of 5-15 theories (comprehensive analysis)
- For EACH theory:
  - Specific code location (file:line)
  - Code excerpt showing the issue
  - ALL tools needed for 100% verification (complete list)
  - Expected evidence from each tool
  - Theory rationale and root cause
  - Impact estimate (critical/high/moderate/minor)
  - Estimated verification time

### Example

```
Generated Theories for queens benchmark (2.1s, 2.6x slower than Lua):

Priority 1: "Hot functions have low compilation effectiveness"
- Tool: cpu-sampler
- Evidence if true: High T0 (interpreter) time percentage
- Rationale: Fundamental performance issue if code isn't compiling

Priority 2: "Optimization barriers prevent peak performance"
- Tool: trace-performance-warnings
- Evidence if true: Virtual call warnings, type check failures
- Rationale: Recursive algorithms need good inlining and caching

Priority 3: "Inlining budget exhausted for recursive calls"
- Tool: trace-inlining
- Evidence if true: Cutoff state for queens function
- Rationale: Deep recursion can exhaust inlining budget
```

---

## Phase 3: Verify Theories Systematically

**Objective**: Verify or falsify each theory using appropriate tools with rigorous methodology

**Time Budget**: Maximum 1 hour total (including Phase 2 theory generation)
**Requirement**: 100% proof - run ALL tools needed for complete verification

**CRITICAL RULES**:
1. **Run ALL tools listed for the theory** - do not skip tools even if earlier ones seem conclusive
2. **Actually execute tools** - do not substitute code analysis for tool verification
3. **Document inconclusive results** - if a tool produces no useful data, try alternatives
4. **Respect time budget** - if approaching 1h limit, prioritize remaining high-impact theories

For each theory in priority order, execute this sub-workflow:

### Step 3.1: Load Tool Documentation (MANDATORY)

**Process**:
- **MUST** read the tool's documentation file before running the tool
- Documentation files located in `.claude/skills/performance-analysis/`
- Extract:
  - Tool command syntax and required options
  - Output format and field meanings
  - Interpretation guidelines (what's good vs bad)
  - Best practices for the tool
  - Expected evidence patterns for verification

**Tool Documentation Files**:
- `./CPUSampler.md` - For cpu-sampler
- `./CPUTracer.md` - For cpu-tracer
- `./Trace Performance Warnings.md` - For trace-performance-warnings
- `./Trace Inlining.md` - For trace-inlining
- `./Trace Compilation.md` - For trace-compilation
- `./Trace Transfer To Interpreter.md` - For trace-transfer-to-interpreter
- `./MemoryTracer.md` - For memory-tracer
- `./Dump Compiler Graph.md` - For compiler graph analysis

**Example**:
```
Theory: "Hot functions have low compilation effectiveness"
Tool: cpu-sampler

Loading documentation:
→ Reading .claude/skills/performance-analysis/CPUSampler.md

Retrieved Information:
- Command: <launcher> --cpusampler --cpusampler.ShowTiers=true --cpusampler.Delay=2000 <program>
- Tiers: T0=interpreter, T1=basic compiled, T2=fully compiled
- Target: >80% T2 for hot functions, <10% T0
- ShowTiers=true REQUIRED to see tier breakdown
- Evidence if verified: Hot function shows >30% T0 (target <10%)
```

### Step 3.2: Fermi Verification (MANDATORY)

**CRITICAL - This prevents silent tool failures**

#### Sub-step 2.1: Pre-Calculation
- Estimate expected output magnitude BEFORE running tool
- Write down estimate explicitly
- Base estimate on:
  - Benchmark characteristics (known loop counts, recursion depth)
  - Language patterns
  - Theory prediction
- Be conservative: order of magnitude is sufficient

#### Sub-step 2.2: Smoke Test (The Probe)
- Run tool on trivial input first
- Verify tool produces expected output format
- Confirm tool is functional and accessible
- Example: Run cpu-sampler on "print 1;" program

#### Sub-step 2.3: Execute & Validate
- Run actual tool command on benchmark
- Compare output magnitude vs pre-calculated estimate
- **Credibility Threshold Check**:
  - **Scenario A (Within 1 Order of Magnitude)**: **ACCEPT** result
    - Tool is working correctly
    - Proceed with analysis
  - **Scenario B (>1 Order of Magnitude Divergence OR Unexpected Zero)**: **REJECT & DIAGNOSE**
    - **STOP** - Do NOT use this result
    - **Hypothesis**: Tool failed silently, wrong path, or permissions issue
    - **Action**: Run debug commands:
      - `ls -l <benchmark-file>` (check file exists and size)
      - `echo $?` (check exit code)
      - Re-run with verbose flags
      - Check tool installation
    - **Only** after proving tool health may you accept divergent result

**Example**:
```
Theory: "Hot functions have low compilation effectiveness"
Tool: cpu-sampler
Benchmark: queens.lox

Sub-step 2.1: Pre-Calculation
- queens.lox has recursive function
- Recursive calls likely hot
- Expected: 1-5 hot functions in output
- Estimated execution time: 2-3 seconds (known from benchmark run)
- **Pre-calculation**: Expect 1-5 functions in histogram

Sub-step 2.2: Smoke Test
- Command: ./lox --cpusampler --cpusampler.Delay=1000 trivial.lox
- Expected: Histogram with at least 1 function
- Result: Shows histogram with 2 functions ✓
- **Smoke test PASSED**: Tool works

Sub-step 2.3: Execute & Validate
- Command: ./lox --cpusampler --cpusampler.ShowTiers=true --cpusampler.Delay=2000 queens.lox
- Output: Histogram with 3 functions
- **Validation**: 3 functions is within 1 order of magnitude of estimate (1-5) ✓
- **Credibility**: ACCEPT result, proceed with analysis

Alternative failure scenario:
- Output: 0 functions (unexpected zero)
- **Validation**: REJECTED (divergence too large)
- **Diagnosis**:
  - Run: ls -l queens.lox → File exists, 523 bytes ✓
  - Run: ./lox queens.lox → Runs successfully ✓
  - Run: ./lox --cpusampler queens.lox → 0 functions still
  - **Root cause**: --cpusampler.Delay=2000 exceeds runtime (2.1s)
  - **Fix**: Reduce delay to 500ms
  - Rerun: Now shows 3 functions ✓
  - **Credibility**: ACCEPT after fix
```

### Step 3.3: Run ALL Tools and Analyze Results

**CRITICAL**: For each theory, run EVERY tool listed in the theory's verification plan. Do not stop after the first tool even if it seems conclusive.

**Process**:

**For EACH tool listed in the theory's verification plan**:

1. **Execute tool command**
   - Use appropriate options based on documentation
   - Redirect output to file for analysis: `command > output.txt 2>&1`
   - Capture both stdout and stderr

2. **Parse output**
   - Extract relevant metrics
   - Identify key findings
   - Look for expected evidence (from theory description)

3. **Evaluate tool result**
   - **✅ Evidence Found**: Tool confirms theory prediction
     - Extract specific data points
     - Note magnitude of issue (how far from target)
     - Continue to next tool in verification plan

   - **❌ Evidence Contradicts**: Tool output contradicts theory
     - Document what was found instead
     - Continue to next tool (may clarify contradiction)

   - **⚠️ Inconclusive**: Tool produces no useful data
     - **REQUIRED ACTIONS**:
       1. Document why inconclusive (no data? wrong benchmark? tool misconfigured?)
       2. Try alternative tool if available
       3. If all tools inconclusive, mark theory as "⚠️ INCONCLUSIVE - needs further investigation"
     - Examples of inconclusive results:
       - Profiler shows 0 samples (runtime too short / delay too long)
       - Tracer shows no events (feature not exercised in benchmark)
       - Graph dump fails (compilation didn't happen)
     - **Mitigation**: Document in report, suggest follow-up investigation

4. **Combine evidence from all tools**
   - After running ALL tools, synthesize findings
   - **✅ Verified**: Majority of tools confirm theory
     - List supporting evidence from each tool
     - Note if any tools showed contradictory data
   - **❌ Falsified**: Majority of tools contradict theory
     - Explain why theory was wrong
     - Note what alternative explanation fits the data
   - **⚠️ Inconclusive**: Tools provide insufficient or contradictory data
     - Document all tool results
     - Suggest additional investigation needed

5. **Generate recommendations** (if verified)
   - Identify specific fix based on evidence
   - Provide code examples where applicable
   - Estimate impact based on tool measurements
   - Reference similar implementations or documentation
   - Note verification steps to confirm fix worked

6. **Record findings**
   - Save ALL tool outputs to files
   - Record theory verdict with confidence level
   - Extract actionable insights
   - Note any follow-up theories generated

### Output

- Theory verdict: ✅ VERIFIED / ❌ FALSIFIED / ⚠️ INCONCLUSIVE
- Evidence from ALL tools (not just one):
  - Tool 1: [result] (saved to tool-outputs/...)
  - Tool 2: [result] (saved to tool-outputs/...)
  - Tool N: [result] (saved to tool-outputs/...)
- Recommendation (if verified)
- Follow-up investigation needed (if inconclusive)

### Example

```
Theory: "Hot functions have low compilation effectiveness"
Tool: cpu-sampler
Command: ./lox --cpusampler --cpusampler.ShowTiers=true --cpusampler.Delay=2000 queens.lox

Output (saved to cpu-sampler-queens.txt):
-----------------------------------------------------------
Sampling Histogram. Recorded 412 samples with period 10ms.
  Self Time: Time spent in function (excluding callees)
  Total Time: Time in function including callees

Name     || Total Time    || Self Time     || T0     | T1    | T2
queens   || 1850ms 88.0%  || 1850ms 88.0%  || 95.2% | 3.1%  | 1.7%
hasConflict || 250ms 11.9% || 250ms 11.9%  || 92.8% | 4.2%  | 3.0%
-----------------------------------------------------------

Parsing:
- Hot function: queens (88% of total time)
- Tier distribution: 95.2% T0 (interpreter), only 1.7% T2 (compiled)
- Target: >80% T2, <10% T0
- Gap: 95.2% T0 vs <10% target → 10x worse than target

Evaluation:
- Evidence found: ✅ YES
  - queens function shows 95.2% interpreter time (T0)
  - Only 1.7% fully compiled (T2)
  - Target is <10% T0, >80% T2
- Verdict: ✅ VERIFIED
  - Theory is correct: low compilation effectiveness
  - Severity: Critical (10x worse than target)

Recommendation:
**Problem**: queens function executing 95.2% in interpreter (target: <10%)
**Root Cause**: Functions likely not compiling or deoptimizing constantly
**Next Steps**:
  1. Run trace-compilation to see if compilation is happening
  2. If compiling: Check for deoptimization with trace-transfer-to-interpreter
  3. If not compiling: Check for compilation barriers with trace-performance-warnings
**Expected Impact**: Fixing this should yield 5-10x speedup (based on T0→T2 improvement)

**Generated Follow-up Theory**: "Compilation is failing or bailouts occurring"
- Tool: trace-compilation
- Priority: High (explains current finding)
```

---

## Phase 4: Generate Comprehensive Report

**Objective**: Produce detailed analysis document with all findings and recommendations

### Process

1. **Compile all data**
   - Benchmark execution times
   - Baseline comparisons and expectations
   - All theories with verdicts and evidence
   - Tool outputs (excerpts and full outputs)
   - Recommendations prioritized by impact

2. **Generate report structure**
   - Executive summary (high-level findings)
   - Benchmark results section
   - Performance comparison section
   - Theory verification section (each theory)
   - Prioritized recommendations section
   - Appendix with full tool outputs

3. **Write recommendations**
   - For each verified theory:
     - Explain the problem clearly
     - Provide specific fix (code examples if applicable)
     - Reference implementation examples or documentation
     - Estimate performance impact
     - Note dependencies between fixes

4. **Prioritize recommendations**
   - **Priority 1**: High impact, must-fix issues
   - **Priority 2**: Medium impact, should-fix issues
   - **Priority 3**: Low impact, nice-to-have improvements

5. **Save report**
   - Write to `PERFORMANCE_ANALYSIS_REPORT.md`
   - Include date, configuration, tool versions
   - Format for readability (tables, code blocks)
   - Include file references for tool outputs

### Output

- `PERFORMANCE_ANALYSIS_REPORT.md`
- `tool-outputs/` directory with all tool outputs

### File Naming Convention

- Report: `PERFORMANCE_ANALYSIS_REPORT.md`
- Tool outputs: `tool-outputs/[tool-name]-[benchmark].txt`

See [EXAMPLES.md](EXAMPLES.md) for a complete example report.

---

## Workflow Integration

Typical performance optimization workflow:

```
Step 1: benchmark-baseline skill
→ Creates benchmarks
→ Fetches baseline data
→ Produces BENCHMARK_BASELINE.md

Step 2: Run benchmarks
→ Execute each benchmark
→ Record timing data

Step 3: performance-analysis skill ← THIS SKILL
→ Loads benchmark results
→ Loads BENCHMARK_BASELINE.md
→ Generates and verifies theories
→ Produces PERFORMANCE_ANALYSIS_REPORT.md

Step 4: Implement fixes
→ Follow recommendations from report
→ Make code changes

Step 5: Re-run benchmarks
→ Measure improvement

Step 6: (Optional) Re-run performance-analysis
→ Verify issues resolved
→ Find remaining opportunities
```

---

## Implementation Notes

This skill:
- **Loads BENCHMARK_BASELINE.md** (from benchmark-baseline skill)
- **Generates theories automatically** based on gaps, patterns, and code analysis
- **Loads tool documentation files** (MANDATORY before each tool execution)
- **Enforces Fermi verification** (MANDATORY for all tool executions)
- **Saves all tool outputs** to `tool-outputs/` directory
- **Produces final report**: `PERFORMANCE_ANALYSIS_REPORT.md`
- **Intermediate saves**: Theory status and findings saved incrementally

## Help Flags Reference


Get Truffle general help options:

```bash
<language-launcher> --help                 # Basic usage and available tools
<language-launcher> --help:all             # All options (103+ options)
<language-launcher> --help:vm              # Host VM options
<language-launcher> --help:compiler        # Basic compiler options
<language-launcher> --help:compiler:internal # Internal compiler options (includes trace flags)
<language-launcher> --help:engine          # User and expert engine options
<language-launcher> --help:engine:internal # Internal engine options (includes trace flags)
```

### Tool-Specific Help

Get detailed options for each profiling tool:

```bash
<language-launcher> --help:cpusampler      # CPU Sampler options
<language-launcher> --help:cputracer       # CPU Tracer options
<language-launcher> --help:memtracer       # Memory Tracer options