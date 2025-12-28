# Performance Analysis Workflow

Detailed 5-phase workflow for systematic performance analysis.

---

## Phase 0: Determine Analysis Focus

**Objective**: Understand what the user wants to investigate

### Process

1. **Check if user specified focus in the initial prompt**
   - Look for keywords: "implementation issues", "configuration", "architectural", "minor issues", "critical only", "all issues"
   - If focus is clear, proceed with that focus

2. **If focus NOT specified, ask the user**

   Use AskUserQuestion tool with:

   **Question**: "What aspects of performance should I focus on?"

   **Header**: "Analysis Focus"

   **Options**:

   1. **All issues (comprehensive)** - Analyze implementation, configuration, and architectural issues at all severity levels
      - Description: "Complete analysis covering all performance aspects"

   2. **Critical & high-impact only** - Focus on issues with significant performance impact, skip minor optimizations
      - Description: "Focus on severe performance problems only"

   3. **Implementation issues only** - Focus on method-level code issues (missing specializations, caching, boundaries)
      - Description: "Code-level optimizations and patterns"

   4. **Configuration issues only** - Focus on bytecode config, compiler settings, optimization flags
      - Description: "Language and compiler configuration"

   5. **Architectural issues only** - Focus on design patterns, data structure choices, overall architecture
      - Description: "High-level design and structure decisions"

3. **Document focus decision**
   - Record which categories to analyze
   - Record which severity levels to include
   - Note any custom criteria from user

### Output

- Clear understanding of analysis scope
- Focus areas documented for theory generation
- Severity filter determined (all levels vs critical/high only)

### Example

```
User prompt: "Analyze the code base for performance issues"

No focus specified in prompt → Ask user

User response: "Critical & high-impact only"

Documented focus:
- Categories: ALL (implementation, configuration, architectural)
- Severity filter: Critical and High only (skip Medium and Low)

Proceeding with focused analysis...
```

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

**Objective**: Generate testable performance theories based on gaps, patterns, and **user's focus areas**

**Target**: All theories found through systematic code analysis (within user's focus areas)

### Process

**STEP 0: Apply Focus Filter** (from Phase 0)

Based on user's specified focus, determine which analysis categories to pursue:

- **If "All issues"**: Analyze all categories below
- **If "Critical & high-impact only"**: Analyze all categories, but filter out Medium and Low severity theories
- **If "Implementation issues only"**: Focus on step 3b, 3d, 3e (operations/nodes, frame access, library usage)
- **If "Configuration issues only"**: Focus on step 3a (language definition & bytecode config)
- **If "Architectural issues only"**: Focus on step 3c (runtime data structures) and architectural patterns
- **If "Custom focus"**: Apply user's specific criteria

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

   **Filter analysis by user's focus:**

   **MUST analyze the following systematically (filtered by user's focus)**:

   a. **Language definition & bytecode configuration** [CONFIGURATION]
      - Check configuration settings for optimization opportunities
      - Identify missing or suboptimal configurations

   b. **ALL operations/nodes in the implementation** [IMPLEMENTATION]
      - Check EVERY operation for missing optimizations
      - Look for patterns that prevent compilation
      - Identify missing specializations or caching

   c. **Runtime data structures and types** [ARCHITECTURAL + IMPLEMENTATION]
      - Analyze allocation patterns
      - Check for optimization boundaries
      - Identify inefficient data structure choices

   d. **Frame and variable access patterns** [IMPLEMENTATION]
      - Analyze slot access patterns
      - Check for dynamic vs constant access
      - Identify materialization overhead

   e. **Library and interop usage** [IMPLEMENTATION]
      - Check for uncached library usage
      - Analyze limit parameters on cached libraries
      - Identify missing exports or specializations

   **For each anti-pattern found**:
   - Record specific file location (file:line)
   - Extract code excerpt showing the issue
   - Estimate impact (Critical/High/Medium/Low)
   - Categorize: Implementation vs Configuration vs Architectural
   - **Filter by user's focus** - skip if outside focus areas or severity threshold

4. **Prioritize theories by impact level**
   - **Priority 1**: Critical impact (blocks optimization, causes severe slowdowns)
   - **Priority 2**: High impact (significant performance degradation)
   - **Priority 3**: Medium impact (noticeable but not severe)
   - **Priority 4**: Low impact (minor optimizations)

   **Apply user's focus filter:**
   - If "Critical & high-impact only": Include only Priority 1-2
   - If other focus specified: Include all priorities within focus categories
   - Document skipped theories (e.g., "Skipped 5 low-priority theories per user request")

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

- Prioritized list of all theories found (comprehensive analysis within user's focus areas)
- Count of skipped theories (if focus filter applied)
- For EACH theory:
  - Specific code location (file:line)
  - Code excerpt showing the issue
  - Category: Implementation/Configuration/Architectural
  - ALL tools needed for 100% verification (complete list)
  - Expected evidence from each tool
  - Theory rationale and root cause
  - Impact estimate (Critical/High/Medium/Low)

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

**🔴 MANDATORY: Use VERIFICATION_CHECKLIST.md for EVERY theory**

Before verifying any theory:
1. Read [VERIFICATION_CHECKLIST.md](VERIFICATION_CHECKLIST.md)
2. Copy the per-theory template for each theory
3. Complete ALL checklist items before marking theory as verified
4. Keep checklist alongside your work as you execute tools

**FILE LOCATION**: `.claude/skills/performance-analysis/VERIFICATION_CHECKLIST.md`

---

**Objective**: Verify or falsify each theory using appropriate tools with rigorous methodology

**Requirement**: 100% proof - run ALL tools needed for complete verification

**CRITICAL RULES**:
1. **Run ALL tools listed for the theory** - do not skip tools even if earlier ones seem conclusive
2. **Actually execute tools** - do not substitute code analysis for tool verification
3. **Document inconclusive results** - if a tool produces no useful data, try alternatives
4. **Complete all verifications** - verify ALL theories in scope (per user's focus)
5. **Use VERIFICATION_CHECKLIST.md** - complete the checklist for each theory

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

5. **Document issue characteristics** (if verified)
   - Identify root cause from tool evidence
   - Quantify impact from tool measurements (frequency, time%, allocations)
   - Determine issue category (implementation/configuration/architectural)
   - Note specific code locations affected
   - Document severity based on quantitative data

6. **Record findings**
   - Save ALL tool outputs to files
   - Record theory verdict with confidence level
   - Extract quantitative measurements
   - Note any follow-up theories generated

### Output

- Theory verdict: ✅ VERIFIED / ❌ FALSIFIED / ⚠️ INCONCLUSIVE
- Evidence from ALL tools (not just one):
  - Tool 1: [result] (saved to tool-outputs/...)
  - Tool 2: [result] (saved to tool-outputs/...)
  - Tool N: [result] (saved to tool-outputs/...)
- Issue characterization (if verified):
  - Root cause
  - Quantified impact
  - Affected locations
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

Issue Characterization:
**Issue**: queens function executing 95.2% in interpreter (target: <10%)
**Root Cause**: Unknown - requires deeper investigation (compilation not happening OR deoptimization)
**Impact**: 10x worse than target (95.2% T0 vs <10% target)
**Severity**: Critical
**Location**: queens function (primary hotspot, 88% of total time)
**Category**: Compilation effectiveness issue

**Generated Follow-up Theory**: "Compilation is failing or bailouts occurring"
- Tool: trace-compilation (to determine if compilation happening)
- Tool: trace-transfer-to-interpreter (to check for deoptimization)
- Priority: High (needed to identify root cause)
```

---

### Step 3.4: Complete Verification Checklist

**MANDATORY**: Before moving to Phase 4, you MUST complete this step.

**Process**:

1. **Open VERIFICATION_CHECKLIST.md**
   - File location: `.claude/skills/performance-analysis/VERIFICATION_CHECKLIST.md`
   - Review the "Pre-Report Verification Gate" section

2. **Complete checklist for EVERY theory**
   - For each theory generated in Phase 2:
     - [ ] Copy the per-theory template from VERIFICATION_CHECKLIST.md
     - [ ] Fill in all checklist items (tool selection, documentation, Fermi verification, evidence)
     - [ ] Mark verdict: ✅ VERIFIED / ❌ FALSIFIED / ⚠️ INCONCLUSIVE
     - [ ] Verify you have tool-based quantitative evidence (not just code analysis)

3. **Verify all theories have proper evidence**
   - [ ] All "✅ VERIFIED" theories have tool output citations
   - [ ] All "❌ FALSIFIED" theories are excluded from report
   - [ ] All "⚠️ INCONCLUSIVE" theories marked appropriately or excluded
   - [ ] NO theory relies solely on code analysis without tool verification
   - [ ] All tool outputs saved to `tool-outputs/` directory

4. **Self-assessment questions**
   - Did I run ALL tools listed for each theory? (not just one tool)
   - Did I complete Fermi verification for each tool execution?
   - Do I have quantitative data (numbers, percentages, measurements)?
   - Can I cite specific tool output files for every verified issue?

**CRITICAL**: If ANY answer to the self-assessment questions is NO, go back and complete verification properly. Do NOT proceed to Phase 4 without completing all checklists.

**Output**:
- [ ] All theory verification checklists completed
- [ ] Only verified theories (with tool evidence) will be included in report
- [ ] Ready to proceed to Phase 4

---

## Phase 4: Generate Comprehensive Report

**Objective**: Produce detailed analysis document identifying and quantifying all verified performance issues

**FOCUS**: Document WHAT issues exist, WHERE they are, HOW SEVERE they are - NOT how to fix them

### Process

1. **Compile all data**
   - Benchmark execution times
   - Baseline comparisons and expectations
   - All theories with verdicts and evidence
   - Tool outputs (excerpts and full outputs)
   - Quantitative measurements from tools

2. **Generate report structure**
   - Executive summary (high-level findings, issue counts)
   - Benchmark results section
   - Performance comparison section
   - Theory verification section (each theory with evidence)
   - Prioritized issues section (by impact/severity)
   - Appendix with full tool outputs

3. **Document verified issues**
   - For each verified theory:
     - Describe the issue clearly with quantitative data
     - State root cause (if identified from tools)
     - Document affected code locations (file:line)
     - Quantify impact from tool measurements
     - Categorize (implementation/configuration/architectural)
     - Assign severity based on tool evidence

4. **Prioritize issues by impact**
   - **Priority 1**: Critical impact (blocks optimization, severe slowdowns)
   - **Priority 2**: High impact (significant performance degradation)
   - **Priority 3**: Medium impact (noticeable but not severe)
   - **Priority 4**: Low impact (minor issues)

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