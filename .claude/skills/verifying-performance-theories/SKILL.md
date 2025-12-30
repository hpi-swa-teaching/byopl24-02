---
name: verifying-performance-theories
description: Coordinates verification of performance theories using profiling tools. Enforces Fermi verification protocol, runs appropriate tool skills, and synthesizes evidence from multiple sources. Use after generating theories to prove which issues actually matter. Use when validating hypotheses, running profiling tools systematically, or collecting quantitative evidence. (project)
---

# Verifying Performance Theories

Systematically verifies performance theories using profiling tools. Enforces rigorous methodology to ensure only proven issues make it into the final report.

## Core Principle

**Code analysis finds POTENTIAL issues. Tools PROVE which issues actually matter.**

## Quick Start

**Input**: List of theories from `generating-performance-theories` skill

**Approach**: Verify one theory at a time, highest-severity first. Fix it, re-profile, continue.

**Output**: Verified findings for `generating-performance-reports`

## Workflow Overview

1. **Select** highest-priority unverified theory
2. **Prepare** - load tool docs, form expectations (Fermi estimation)
3. **Execute** tools from the theory's verification plan
4. **Handle emergent issues** - pivot if more critical, note as future work if not
5. **Synthesize** evidence → VERIFIED / FALSIFIED / INCONCLUSIVE
6. **Next steps** - if verified, recommend fix and stop; otherwise continue

See [WORKFLOW.md](WORKFLOW.md) for detailed procedures.

## Tool Skills

| Purpose | Tool Skill |
|---------|-----------|
| Hot function identification | `profiling-with-cpu-sampler` |
| Execution frequency | `tracing-execution-counts` |
| Optimization barriers | `detecting-performance-warnings` |
| Compilation behavior | `tracing-compilation-events` |
| Inlining analysis | `tracing-inlining-decisions` |
| Type stability | `detecting-deoptimizations` |
| Allocation patterns | `profiling-memory-allocations` |
| Deep IR analysis | `analyzing-compiler-graphs` |

**Note on Compiler Graphs**: When theories come from code analysis (e.g., "this allocation should be eliminated"), compiler graphs provide **direct evidence** of what the compiler actually did. Use them early for allocation/boxing theories, not as a last resort.

## Common Pitfalls

- ❌ **Substituting code analysis for tool verification** - Code shows potential, tools prove actuality
- ❌ **Running only one tool** - Multiple tools required for confidence
- ❌ **Skipping Fermi verification** - Silent tool failures produce garbage data
- ❌ **Ignoring emergent issues** - If tools reveal a critical issue (like deopt loops), evaluate whether to pivot
- ❌ **Always pivoting** - Only pivot if the new issue is MORE critical than the current theory

## Related Skills

**Predecessor**: `generating-performance-theories` → Provides theories to verify

**Successor**: `generating-performance-reports` → Compiles verified findings

**Tool Skills Used**:
- `profiling-with-cpu-sampler`
- `tracing-execution-counts`
- `detecting-performance-warnings`
- `tracing-compilation-events`
- `tracing-inlining-decisions`
- `detecting-deoptimizations`
- `profiling-memory-allocations`
- `analyzing-compiler-graphs`

## Workflow Position

```
1. [establishing-benchmark-baseline] → Create baseline
2. [Run benchmarks]                  → Get timing data
3. [generating-performance-theories] → Generate theories
4. [verifying-performance-theories]  → THIS SKILL
5. [generating-performance-reports]  → Document findings
```
