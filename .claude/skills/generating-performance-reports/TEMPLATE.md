# Performance Analysis Report Template

Use this template for generating `PERFORMANCE_ANALYSIS_REPORT.md`.

---

```markdown
# Performance Analysis Report

**Generated**: [Date]
**Configuration**: [Language version, JVM version, hardware]
**Analysis Focus**: [User-selected focus from theory generation]

---

## Executive Summary

**Total Issues Found**: X verified issues (Y critical, Z high, W medium, V low)

**Key Findings**:
- [Most critical issue - one sentence]
- [Second critical issue - one sentence]
- [Overall performance status - one sentence]

**Performance Status**:
- Benchmark X: [On track / Needs attention / Critical]
- Benchmark Y: [On track / Needs attention / Critical]

---

## Benchmark Results

### Actual Performance

| Benchmark | Execution Time | Expected Range | Status |
|-----------|---------------|----------------|--------|
| [name] | X.XX s | Y.YY - Z.ZZ s | ✅/⚠️/❌ |

### Performance Gap Analysis

| Benchmark | Actual | Best Case | Ratio | Status |
|-----------|--------|-----------|-------|--------|
| [name] | X.XX s | Y.YY s | Z.Zx slower | [Status] |

---

## Verified Issues

### Issue 1: [Title]

**Severity**: Critical
**Category**: Implementation
**Location**: `src/path/to/file.java:123`

**Description**:
[Clear description of the issue with quantitative data from tools]

**Root Cause**:
[Identified cause based on tool evidence]

**Impact**:
- Time: X% of total execution time
- Frequency: Y executions per benchmark run
- Effect: [Description of performance effect]

**Evidence**:
- `tool-outputs/cpu-sampler-benchmark.txt`: Shows X% time in [function]
- `tool-outputs/perf-warnings-benchmark.txt`: Virtual call warning at line Y

**Code Excerpt**:
```java
// Line 123
[relevant code showing the issue]
```

---

### Issue 2: [Title]

**Severity**: High
...

---

## Prioritized Issue Summary

### Priority 1: Critical (Blocks Optimization)

1. **[Issue Title]** - [Location] - [Brief impact]
2. **[Issue Title]** - [Location] - [Brief impact]

### Priority 2: High (Significant Degradation)

1. **[Issue Title]** - [Location] - [Brief impact]

### Priority 3: Medium (Noticeable Impact)

1. **[Issue Title]** - [Location] - [Brief impact]

### Priority 4: Low (Minor Optimizations)

1. **[Issue Title]** - [Location] - [Brief impact]

---

## Potential Issues (Unverified)

*Issues identified through code analysis but not verified with tools.*

| Issue | Location | Reason Unverified |
|-------|----------|-------------------|
| [description] | file:line | [why not verified] |

---

## Appendix

### A. Tool Outputs

Full tool outputs available in `tool-outputs/` directory:
- `cpu-sampler-[benchmark].txt`
- `trace-compilation-[benchmark].txt`
- `perf-warnings-[benchmark].txt`
- ...

### B. Methodology

- **Fermi Verification**: Applied to all tool executions
- **Tools Used**: [list of tool skills used]
- **Verification Checklists**: Completed for all theories

### C. Configuration Details

- **JVM**: [version]
- **GraalVM**: [version]
- **Compiler Flags**: [flags used]
- **Benchmark Parameters**: [N values, iterations]

---

*Report generated using the performance analysis skill workflow.*
```
