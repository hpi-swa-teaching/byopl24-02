---
name: verifying-performance-theories
description: Coordinates verification of performance theories using profiling tools. Enforces Fermi verification protocol, runs appropriate tool skills, and synthesizes evidence from multiple sources. Use after generating theories to prove which issues actually matter. Use when validating hypotheses, running profiling tools systematically, or collecting quantitative evidence. (project)
---

# Verifying Performance Theories

Systematically verifies performance theories using profiling tools. Enforces rigorous methodology to ensure only proven issues make it into the final report.

## Core Principle

**Code analysis finds POTENTIAL issues. Tools PROVE which issues actually matter.**

A theory is only verified when:
1. Documentation was loaded before running the tool
2. Fermi verification was completed (estimate → smoke test → validate)
3. ALL required tools were executed
4. Quantitative evidence was collected
5. Evidence synthesis confirms the theory

## Quick Start

**Input**: List of theories from `generating-performance-theories` skill

**Iterative Approach** (recommended):
1. **Pick the highest-severity theory** from the list
2. Verify it using the workflow below
3. If verified → **Recommend fix to user, STOP investigation**
4. After fix applied → Re-profile and continue with next theory

**Why iterative?** Multiple performance issues create noise in profiling tools. Fixing the biggest issue first clears the signal for finding smaller issues. Don't try to verify all theories at once.

**Output**: One verified issue with fix recommendation, then iterate

## Fermi Verification Protocol (MANDATORY)

**Principle**: Tool output is authority for *data*, Fermi estimate is authority for *pipeline integrity*.

### Step 1: Pre-Calculation
Before running any tool:
- Estimate expected output magnitude
- Write down estimate explicitly
- Base on benchmark characteristics, language patterns, theory prediction

### Step 2: Smoke Test
- Run tool on trivial input first
- Verify tool produces expected output format
- Confirm tool is functional and accessible

### Step 3: Execute & Validate
- Run actual tool command on benchmark
- Compare output magnitude vs pre-calculated estimate

**Credibility Threshold Check**:
- **Scenario A (Within 1 Order of Magnitude)**: ACCEPT result, proceed with analysis
- **Scenario B (>1 Order of Magnitude Divergence OR Unexpected Zero)**: REJECT & DIAGNOSE
  - STOP - Do NOT use this result
  - Run debug commands to prove tool health
  - Only accept after proving tool is working correctly

## Tool Skills for Verification

| Purpose | Tool Skill | When to Use |
|---------|-----------|-------------|
| Hot function identification | `profiling-with-cpu-sampler` | FIRST step for any performance issue |
| Execution frequency | `tracing-execution-counts` | Understand how often code runs |
| Optimization barriers | `detecting-performance-warnings` | Find virtual calls, type checks, boundaries |
| Compilation behavior | `tracing-compilation-events` | Verify code is compiling |
| Inlining analysis | `tracing-inlining-decisions` | Check call inlining |
| Type stability | `detecting-deoptimizations` | Find deoptimization loops |
| Allocation patterns | `profiling-memory-allocations` | Track memory allocations |
| Deep IR analysis | `analyzing-compiler-graphs` | **Essential for code-derived theories** (escape analysis, boxing, allocations) |

**Note on Compiler Graphs**: When theories come from code analysis (e.g., "this allocation should be eliminated"), compiler graphs provide **direct evidence** of what the compiler actually did. Use them early for allocation/boxing theories, not as a last resort.

## Verification Workflow

### For the Current Highest-Priority Theory:

#### 1. Load Tool Documentation (MANDATORY)
Before running any tool:
- Use the corresponding tool skill
- Extract: Command syntax, output format, interpretation guidelines
- Understand: What evidence confirms/denies the theory

#### 2. Complete Fermi Verification (MANDATORY)
For each tool:
- Pre-calculate expected output
- Run smoke test
- Execute on actual benchmark
- Validate results vs estimate

#### 3. Run ALL Tools and Analyze
**CRITICAL**: Run EVERY tool listed in the theory's verification plan

For each tool:
1. Execute tool command with appropriate options
2. Save output to file for reference
3. Parse and extract relevant metrics
4. Evaluate: Evidence Found / Contradicts / Inconclusive

#### 4. Synthesize Evidence
After running ALL tools:
- **✅ VERIFIED**: Majority of tools confirm theory
- **❌ FALSIFIED**: Majority of tools contradict theory
- **⚠️ INCONCLUSIVE**: Insufficient or contradictory data

#### 5. Characterize Issue (if verified)
- Root cause from tool evidence
- Quantified impact (frequency, time%, allocations)
- Specific code locations affected
- Severity based on quantitative data

#### 6. Recommend Fix and Stop
If theory is verified:
- **Present fix recommendation to user**
- **STOP further investigation** - noise from this issue obscures others
- After user applies fix → Re-run profiling → Continue with next theory

## Evidence Requirements

**Verified theories MUST have**:
- Concrete numbers (frequency, time, allocations)
- Tool output citations (saved files)
- Severity confirmed by tool data
- Root cause identified from evidence

**Falsified theories document**:
- Why theory was wrong
- What was found instead
- Learned from incorrect assumption

**Inconclusive theories require**:
- Which tools worked and which didn't
- Alternative verification approaches attempted
- Decision: include with disclaimer OR exclude

## Tool Output Management

Save all outputs to `tool-outputs/` directory:
```
tool-outputs/
├── cpu-sampler-queens.txt
├── trace-compilation-queens.txt
├── perf-warnings-queens.txt
└── ...
```

## Common Pitfalls to Avoid

- ❌ **Substituting code analysis for tool verification** - Code shows potential, tools prove actuality
- ❌ **Running only one tool** - Multiple tools required for confidence
- ❌ **Skipping Fermi verification** - Silent tool failures produce garbage data
- ❌ **Ignoring inconclusive results** - Document what couldn't be verified
- ❌ **Not saving outputs** - Need citations for report

## Integration with Other Skills

**Predecessor Skills**:
- `generating-performance-theories` → Provides theories to verify

**Tool Skills Used**:
- `profiling-with-cpu-sampler`
- `tracing-execution-counts`
- `detecting-performance-warnings`
- `tracing-compilation-events`
- `tracing-inlining-decisions`
- `detecting-deoptimizations`
- `profiling-memory-allocations`
- `analyzing-compiler-graphs`

**Successor Skill**:
- `generating-performance-reports` → Compiles verified findings

## Workflow Position

```
1. [establishing-benchmark-baseline] → Create baseline
2. [Run benchmarks]                  → Get timing data
3. [generating-performance-theories] → Generate theories
4. [verifying-performance-theories]  → THIS SKILL
5. [generating-performance-reports]  → Document findings
```

See [VERIFICATION-CHECKLIST.md](VERIFICATION-CHECKLIST.md) for the per-theory checklist template.
