---
name: generating-performance-reports
description: Compiles verified performance findings into structured analysis reports. Creates prioritized issue lists with quantitative evidence from profiling tools. Use after theories have been verified to produce final documentation. Use when creating performance report, documenting findings, or summarizing optimization opportunities. (project)
---

# Generating Performance Reports

Compiles verified performance findings into a comprehensive, structured report with prioritized issues and quantitative evidence.

## What This Skill Does

1. **Compiles all verified data** - Gathers benchmark results, baseline comparisons, verified theories
2. **Structures the report** - Creates organized sections for easy navigation
3. **Prioritizes issues** - Ranks by impact based on tool evidence
4. **Documents evidence** - Includes tool output citations
5. **Outputs final report** - Creates `PERFORMANCE_ANALYSIS_REPORT.md`

## Prerequisites

- **REQUIRED**: Completed `verifying-performance-theories` with verified theories
- **REQUIRED**: Tool outputs saved in `tool-outputs/` directory
- **REQUIRED**: Verification checklists completed for all theories

## Quick Start

1. Gather all verified theories from `verifying-performance-theories`
2. Collect all tool outputs from `tool-outputs/` directory
3. Generate report following [TEMPLATE.md](TEMPLATE.md)
4. Save to `PERFORMANCE_ANALYSIS_REPORT.md`

## Report Focus

**IMPORTANT**: Report describes WHAT issues exist, WHERE they are, HOW SEVERE they are.

**NOT how to fix them** - That's a separate concern.

## Report Structure

### 1. Executive Summary
- High-level findings count
- Critical issues highlighted
- Performance gap overview

### 2. Benchmark Results
- Execution times
- Comparison with baseline expectations
- Performance ratios

### 3. Verified Issues
For each verified issue:
- Description with quantitative data
- Root cause from tool evidence
- Affected code locations (file:line)
- Impact measurement from tools
- Severity rating (based on evidence)
- Tool output citations

### 4. Prioritized Issue List
- **Priority 1 (Critical)**: Blocks optimization
- **Priority 2 (High)**: Significant degradation
- **Priority 3 (Medium)**: Noticeable impact
- **Priority 4 (Low)**: Minor issues

### 5. Appendix
- Full tool output excerpts
- Methodology notes
- Configuration details

## Evidence Requirements

Every issue in the report MUST have:
- ✅ Quantitative data (numbers from tools)
- ✅ Tool output citation
- ✅ Verified status from checklist
- ✅ Severity justified by evidence

Issues WITHOUT tool evidence:
- ❌ Must NOT appear as verified
- ⚠️ May appear in "Potential Issues (Unverified)" section if relevant

## Severity Ratings

Based on tool evidence, not code inspection:

| Severity | Criteria |
|----------|----------|
| Critical | >50% time in interpreter, deoptimization loops, 10x+ slower than expected |
| High | 20-50% time loss, significant compilation issues, 3-10x slower |
| Medium | 10-20% time loss, optimization barriers in moderate paths, 2-3x slower |
| Low | <10% impact, issues in cold paths, minor inefficiencies |

## Output File

**Location**: `PERFORMANCE_ANALYSIS_REPORT.md`
**Tool Outputs**: `tool-outputs/*.txt`

## Integration with Other Skills

**Predecessor Skills**:
- `establishing-benchmark-baseline` → BENCHMARK_BASELINE.md
- `generating-performance-theories` → Theory list
- `verifying-performance-theories` → Verified theories with evidence

**This is the FINAL skill in the performance analysis workflow.**

## Workflow Position

```
1. [establishing-benchmark-baseline] → Create baseline
2. [Run benchmarks]                  → Get timing data
3. [generating-performance-theories] → Generate theories
4. [verifying-performance-theories]  → Verify with tools
5. [generating-performance-reports]  → THIS SKILL (FINAL)
```

See [TEMPLATE.md](TEMPLATE.md) for the complete report template.
