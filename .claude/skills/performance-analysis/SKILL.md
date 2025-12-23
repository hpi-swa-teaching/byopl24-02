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
3. **Generates Theories**: Creates 5-15 testable hypotheses through systematic code analysis
4. **Verifies Theories**: Runs ALL required tools for 100% proof with mandatory Fermi verification
5. **Produces Report**: Generates `PERFORMANCE_ANALYSIS_REPORT.md` with actionable fixes

**Time Budget**: Maximum 1 hour total for complete analysis
**Theory Target**: 5-15 most impactful issues (comprehensive but bounded)
**Verification Requirement**: 100% proof - no shortcuts, run all tools needed

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

**Target**: 10-15 most impactful theories
**Method**: Systematic code analysis (not opportunistic spot-checks)

Theories are generated through:

1. **Performance Gap Analysis**: Identify slow benchmarks compared to baseline
2. **Systematic Code Review**:
   - Language configuration (bytecode settings, optimization flags)
   - ALL operations/nodes (every single one, not just a sample)
   - Runtime data structures (allocation patterns, boundaries)
   - Frame access patterns (dynamic vs constant slots)
   - Library usage (uncached, wrong limits)
3. **Anti-Pattern Detection**: Missing optimizations, unnecessary boundaries, architectural issues
4. **Multi-Tool Verification Planning**: List ALL tools needed for 100% proof (not just one)

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

## Common Pitfalls (Learn from Real Mistakes)

### Theory Generation Mistakes
- ❌ **Insufficient coverage**: Only finding 3-5 issues instead of systematically analyzing all code
  - **Wrong**: "I found boxing in arithmetic ops, that's enough"
  - **Right**: "Checked all 20 operations, found 7 with issues: arithmetic (5), comparison (2), array (0)"
- ❌ **Sampling instead of exhaustive**: Checking a few files instead of all
  - **Wrong**: "Looked at AddNode, seems fine"
  - **Right**: "Analyzed all 15 operation nodes, 8 missing primitive specializations"

### Verification Mistakes
- ❌ **Code analysis as verification**: Showing problematic code and calling it "verified"
  - **Wrong**: "GlobalObject uses HashMap with @TruffleBoundary → ✅ VERIFIED"
  - **Right**: "GlobalObject theory → Run cpu-tracer (frequency) + memory-tracer (allocations) + trace-compilation (boundaries) → ✅ VERIFIED"
- ❌ **Single-tool verification**: Running cpusampler and stopping
  - **Wrong**: "cpusampler shows 30% T0 → theory verified, done"
  - **Right**: "cpusampler (30% T0) + trace-compilation (why no T2?) + trace-transfer-to-interpreter (deopt loops?) → complete picture"
- ❌ **Stopping at symptoms**: Identifying problem but not investigating root cause
  - **Wrong**: "Functions aren't reaching T2" + recommendation "run these tools to investigate"
  - **Right**: "Functions aren't reaching T2" → ACTUALLY RUN trace-compilation + trace-inlining → find ROOT CAUSE → recommend fix

### Evidence Mistakes
- ❌ **Skipping Fermi verification**: Not estimating expected output before running tools
- ❌ **Accepting invalid results**: Tool returns 0 samples, using anyway
- ❌ **Not documenting inconclusive**: Tool provides no useful data, skipping it entirely
  - **Right**: Mark as "⚠️ INCONCLUSIVE - trace-compilation showed success but trace-inlining failed, needs manual graph analysis"

### Report Mistakes
- ❌ **Vague recommendations**: "Investigate compilation issues" instead of specific fixes
- ❌ **No evidence**: Claiming verification but only showing code review
- ❌ **Self-evident theories**: "Architectural issues are obvious from code" without tool proof

## Success Criteria

**Minimum requirements (must achieve all):**
- ✅ 5-15 theories generated through systematic code analysis
- ✅ ALL tools listed for each theory were executed (100% verification)
- ✅ Fermi verification passed for all tool runs
- ✅ Inconclusive tool results documented with alternatives attempted
- ✅ Specific recommendations with code examples
- ✅ Report saved to `PERFORMANCE_ANALYSIS_REPORT.md`
- ✅ Completed within 1 hour time budget

**Excellent analysis (exceeds requirements):**
- ✅ Multiple tools corroborate each finding
- ✅ Performance improvement estimates based on tool measurements
- ✅ Root causes identified (not just symptoms)
- ✅ Architectural vs implementation issues distinguished
- ✅ Follow-up investigation roadmap for inconclusive theories

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
