---
name: performance-analysis
description: Comprehensive performance analysis for Truffle language implementations. Loads benchmarks and baseline data, builds performance theories, verifies them using appropriate tools (with mandatory documentation file loading, Fermi verification, and smoke tests), and generates a detailed analysis report with recommendations. You MUST load this skill when you want to perform language performance analysis. YOU MUST run the benchmark baseline skill first.
---

# Skill: Comprehensive Performance Analysis

Systematic performance analysis of Truffle language implementations through theory-driven verification.

---

## CRITICAL: File Loading Requirements

**You MUST read the referenced documentation files at specific points during analysis. Do not skip these steps.**

| When | Action | File to Read |
|------|--------|--------------|
| Before starting analysis | Read workflow details | [WORKFLOW.md](WORKFLOW.md) |
| Before running ANY tool | Read that tool's documentation | Tool-specific `.md` file |
| Before writing report | Read report template and examples | [EXAMPLES.md](EXAMPLES.md) |

**Failure to load these files will result in incorrect tool usage, missed theories, and poor-quality reports.**

---

## What This Skill Does

1. **Loads Data**: Reads benchmark results and BENCHMARK_BASELINE.md
2. **Compares Performance**: Identifies gaps between actual and expected performance
3. **Generates Theories**: Creates testable hypotheses based on gaps and patterns
4. **Verifies Theories**: Uses appropriate tools with mandatory Fermi verification
5. **Produces Report**: Generates `PERFORMANCE_ANALYSIS_REPORT.md` with actionable fixes

## Prerequisites

Before running this skill:

- **Required**: Benchmarks have been executed (timing data available)
- **Required**: `BENCHMARK_BASELINE.md` exists (from benchmark-baseline skill)
- **Required**: Tool documentation files in `.claude/skills/performance-analysis/`
- **Recommended**: `bgv2json` or `seafoam` for compiler graph analysis

## Workflow Overview

The skill follows a 4-phase workflow.

**ACTION REQUIRED**: Before starting, read [WORKFLOW.md](WORKFLOW.md) for detailed steps, Fermi verification protocol, and phase-by-phase instructions.

```
Phase 1: Load benchmark results and baseline expectations
    ↓
Phase 2: Generate theories based on gaps and patterns
    ↓
Phase 3: Verify each theory (documentation → Fermi → smoke test → execute)
         → ACTION: Read tool's .md file before EACH tool execution
    ↓
Phase 4: Generate comprehensive report with recommendations
         → ACTION: Read [EXAMPLES.md](EXAMPLES.md) before writing report
```

### MANDATORY: Fermi Verification Protocol

**Before running ANY tool**, you MUST:

1. **Load documentation**: Read the tool's `.md` file from this skill directory
2. **Pre-calculate**: Estimate expected output magnitude BEFORE running
3. **Smoke test**: Run tool on trivial input to verify it works
4. **Validate results**: Accept only if within 1 order of magnitude of estimate
   - If divergent: STOP, diagnose tool health, only then accept

This prevents silent tool failures and wrong conclusions. See [WORKFLOW.md](WORKFLOW.md#phase-3-verify-theories-systematically) for details.

## Tool Selection Guide

Select tools based on theory type.

**MANDATORY**: Before executing any tool, you MUST read its documentation file to understand:
- Correct command syntax and required options
- Output format and how to interpret results
- What constitutes evidence for/against theories
- Common pitfalls and best practices

### Compilation Issues
| Tool | When to Use | Documentation |
|------|-------------|---------------|
| trace-performance-warnings | WHY optimization fails (virtual calls, type checks) | [Trace Performance Warnings.md](Trace%20Performance%20Warnings.md) |
| trace-compilation | WHEN/IF compilation happens (bailouts, invalidations) | [Trace Compilation.md](Trace%20Compilation.md) |

### Call and Inlining Issues
| Tool | When to Use | Documentation |
|------|-------------|---------------|
| trace-inlining | Verify inlining decisions, budget issues | [Trace Inlining.md](Trace%20Inlining.md) |

### Execution Patterns
| Tool | When to Use | Documentation |
|------|-------------|---------------|
| cpu-sampler | WHERE time is spent (use FIRST for profiling) | [CPUSampler.md](CPUSampler.md) |
| cpu-tracer | Execution frequency, compiled % | [CPUTracer.md](CPUTracer.md) |

### Memory Issues
| Tool | When to Use | Documentation |
|------|-------------|---------------|
| memory-tracer | Allocation hotspots, memory pressure | [MemoryTracer.md](MemoryTracer.md) |

### Deoptimization
| Tool | When to Use | Documentation |
|------|-------------|---------------|
| trace-transfer-to-interpreter | Deoptimization events, unstable assumptions | [Trace Transfer To Interpreter.md](Trace%20Transfer%20To%20Interpreter.md) |

### Deep Investigation
| Tool | When to Use | Documentation |
|------|-------------|---------------|
| analyze-compiler-graph skill | IR-level analysis (escape analysis, boxing, indirect calls) | [Dump Compiler Graph.md](Dump%20Compiler%20Graph.md) |

**Note**: Use compiler graphs LAST (most complex but most detailed).

## Theory Generation

Theories are generated based on:

1. **Performance vs Baseline**: Gaps compared to comparable languages
2. **Known Anti-Patterns**: Low compiled %, indirect calls, high allocation
3. **Benchmark Characteristics**: Recursive, allocation-heavy, arithmetic-heavy
4. **Code Analysis**: Missing @Cached, missing primitive specializations, dynamic frame slots

## Output

### Files Generated
- **Report**: `PERFORMANCE_ANALYSIS_REPORT.md`
- **Tool outputs**: `tool-outputs/[tool-name]-[benchmark].txt`

### Report Structure
```markdown
# Performance Analysis Report

## Executive Summary
- Critical Issues: [COUNT]
- Expected Improvement: [ESTIMATE]

## Benchmark Results
| Benchmark | Actual | Expected Range | Status |

## Theory Verification Results
### Theory N: "[description]"
- Status: ✅ VERIFIED / ❌ FALSIFIED
- Evidence: [tool output excerpt]
- Recommendation: [specific fix]

## Prioritized Recommendations
### Priority 1: [High Impact]
- Problem: ...
- Fix: [code example]
- Expected Impact: ...
```

**ACTION REQUIRED**: Before writing the report, read [EXAMPLES.md](EXAMPLES.md) to see:
- Complete example analysis walkthrough
- Full report template with all sections
- Example tool outputs and their interpretation
- How to format recommendations with code examples

## When to Use This Skill

**Use this skill when:**
- After benchmark-baseline skill to understand performance gaps
- Debugging slow performance with no clear cause
- Before optimization work to identify bottlenecks
- After implementing features to understand impact

**Do NOT use this skill when:**
- You already know the specific problem → Run the tool directly
- Quick investigation needed → Use cpu-sampler directly
- Benchmarks not run yet → Run benchmark-baseline skill first

## Best Practices

1. **Progressive Refinement**: Start with cpu-sampler, use targeted tools, deep-dive last
2. **Theory Prioritization**: High-impact first, quick-to-verify early, complex later
3. **Evidence-Based**: Verify with data, don't assume root causes
4. **Fermi Verification**: NEVER skip pre-calculation and smoke tests
5. **Actionable Recommendations**: Specific code changes, not vague suggestions

## Common Pitfalls

- ❌ Rerunning benchmarks (reuse existing timing data)
- ❌ Skipping Fermi verification (leads to wrong conclusions)
- ❌ Running all tools blindly (overwhelming data)
- ❌ Assuming root causes (verify with evidence)
- ❌ Vague recommendations ("make it faster" isn't actionable)

## Success Criteria

**Good analysis:**
- ✅ >3 theories generated and verified/falsified
- ✅ Fermi verification passed for all tool runs
- ✅ Specific recommendations with fix examples
- ✅ Report saved to `PERFORMANCE_ANALYSIS_REPORT.md`

**Excellent analysis:**
- ✅ Multiple tools corroborate findings
- ✅ Performance improvement estimates provided
- ✅ Root causes identified (not just symptoms)
- ✅ Follow-up theories generated from findings

## Related Skills

### Prerequisite
- **benchmark-baseline**: Run FIRST to create BENCHMARK_BASELINE.md

### Deep Investigation
- **analyze-compiler-graph**: Comprehensive IR analysis with seafoam/bgv2json/jq

### Documentation
- **graal-truffle-docs**: Search GraalVM Truffle documentation

## Detailed Documentation

**Reminder**: You must read these files at the appropriate points during analysis (see "CRITICAL: File Loading Requirements" above).

| File | Read When | Contains |
|------|-----------|----------|
| [WORKFLOW.md](WORKFLOW.md) | Before starting | 4-phase workflow, Fermi verification details |
| [EXAMPLES.md](EXAMPLES.md) | Before Phase 4 | Example walkthrough, report template |

## External Resources

- **GraalVM Profiling**: https://www.graalvm.org/latest/tools/profiling/
- **Truffle Optimization**: https://www.graalvm.org/latest/graalvm-as-a-platform/language-implementation-framework/Optimizing/
- **Benchmarks Game**: https://benchmarksgame-team.pages.debian.net/benchmarksgame/
- **AreWeFastYet**: https://github.com/smarr/are-we-fast-yet
