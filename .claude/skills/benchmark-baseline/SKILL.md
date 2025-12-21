---
name: benchmark-baseline
description: Establishes performance baselines by analyzing language characteristics, querying Computer Language Benchmarks Game and AreWeFastYet for comparable languages, creating local benchmark implementations, and generating a comprehensive baseline report. Use before performance optimization to set realistic expectations.
---

# Benchmark Baseline

Establishes performance baselines for your language implementation through automated discovery and implementation of industry-standard benchmarks.

**Use this skill FIRST** before any performance analysis or optimization work.

## Quick Start

**Step 1**: Ask user which benchmarks to generate using AskUserQuestion tool:
- Benchmarks Game only
- AreWeFastYet only
- Both (recommended)

**Step 2**: Copy this checklist to track progress:

```
Baseline Progress:
- [ ] Phase 1: Analyze language characteristics
- [ ] Phase 2: Identify comparable languages
- [ ] Phase 3: Discover and fetch Benchmarks Game data (if selected)
- [ ] Phase 4: Implement and measure Benchmarks Game benchmarks (if selected)
- [ ] Phase 5: Discover and fetch AreWeFastYet data (if selected)
- [ ] Phase 6: Implement and measure AreWeFastYet benchmarks (if selected)
- [ ] Phase 7: Generate and save baseline report with performance data
```

**Step 3**: Execute workflow following phase instructions below.

## Workflow Overview

### Sequential Phases (Must Complete First)
1. **Analyze language**: Determine type system, execution model, platform, paradigm
2. **Identify comparable languages**: Find 1-2 similar languages for comparison

### Parallel Phases (Run After 1-2 Complete)

**Run these phases in parallel** when user selects both sources:

**Benchmarks Game Track** (Phases 3-4):
- Phase 3: Query Benchmarks Game for benchmarks and performance data
- Phase 4: Implement 3-5 selected benchmarks locally

**AreWeFastYet Track** (Phases 5-6):
- Phase 5: Query AreWeFastYet for micro-benchmarks
- Phase 6: Implement ALL micro-benchmarks locally

### Final Phase
7. **Generate baseline report**: Create `BENCHMARK_BASELINE.md` with all findings

## What This Skill Does

### Phase 1: Language Analysis
Examines the codebase to determine:
- Type system (static/dynamic)
- Execution model (interpreter, bytecode VM, JIT, AOT)
- Platform (native, JVM/Truffle, LLVM, custom VM)
- Paradigm (imperative, OO, functional)
- Complexity level (minimal, moderate, full-featured)
- Runtime characteristics (GC, memory management)

See [WORKFLOW.md](WORKFLOW.md#phase-1-analyze-language-implementation) for detailed instructions.

### Phase 2: Comparable Language Identification
Identifies 1-2 languages that match analyzed characteristics:
- Prioritize same execution model and type system
- Prefer GraalVM Truffle languages if applicable
- Verify languages exist on Benchmarks Game with sufficient coverage

See [WORKFLOW.md](WORKFLOW.md#phase-2-identify-comparable-languages) for matching patterns.

### Phase 3: Benchmarks Game Discovery (If Selected)
Queries https://benchmarksgame-team.pages.debian.net/benchmarksgame/ to:
- Discover all available benchmarks
- Fetch performance data for comparable languages
- Select 3-5 benchmarks covering different computational patterns
- Prioritize "insignificant I/O" benchmarks (pure computation)

See [WORKFLOW.md](WORKFLOW.md#phase-3-retrieve-benchmark-data-from-benchmarks-game) for URL formats and data extraction.

### Phase 4: Benchmarks Game Implementation (If Selected)
Creates local benchmark files:
- **Skip existing files** - check for existing implementations first
- Fetch reference implementations from comparable languages
- Translate to target language syntax following project conventions
- Include verification logic with expected outputs
- **Run each benchmark** - test locally and verify correctness
- **Fix any failures** - verification failures BLOCK workflow; must be resolved
- **Collect performance data** - measure and record execution time for each benchmark
- **Append to baseline** - add performance results to BENCHMARK_BASELINE.md
- Document and skip if unfixable after multiple attempts

See [WORKFLOW.md](WORKFLOW.md#phase-4-implement-benchmarks-from-benchmarks-game-locally) for implementation details.

### Phase 5: AreWeFastYet Discovery (If Selected)
Queries https://github.com/smarr/are-we-fast-yet to:
- Discover all micro-benchmarks (ignore macro benchmarks)
- Identify available languages
- Select implementations from comparable languages

See [WORKFLOW.md](WORKFLOW.md#phase-5-retrieve-benchmark-data-from-arewefastyet) for discovery process.

### Phase 6: AreWeFastYet Implementation (If Selected)
Creates ALL micro-benchmark files:
- **Skip existing files** - check for existing implementations first
- **Implement ALL micro-benchmarks** (specifically designed for language analysis)
- Translate from comparable language implementations
- Follow project conventions for structure
- **Run each benchmark** - test locally and verify correctness
- **Fix any failures** - verification failures BLOCK workflow; must be resolved
- **Collect performance data** - measure and record execution time for each benchmark
- **Append to baseline** - add performance results to BENCHMARK_BASELINE.md
- Document and skip if unfixable after multiple attempts

See [WORKFLOW.md](WORKFLOW.md#phase-6-implement-benchmarks-from-arewefastyet-locally) for implementation details.

### Phase 7: Baseline Report Generation
Creates `BENCHMARK_BASELINE.md` containing:
- Language analysis results
- Comparable languages with rationale
- Selected benchmarks with descriptions
- Execution instructions for all benchmarks
- Cached performance data from comparable languages (Benchmarks Game)
- **Actual performance data from your implementation** (collected during Phases 4 & 6)
- Performance expectations and interpretation guidance
- Performance comparison table showing your results vs. comparable languages

See [WORKFLOW.md](WORKFLOW.md#phase-7-generate-and-save-baseline-report) for report structure.

## Critical Workflow Rules

### User Choice
**ALWAYS ask user first** which benchmarks to generate:
- Use AskUserQuestion tool before starting
- Options: Benchmarks Game, AreWeFastYet, Both
- Never assume - always ask explicitly

### Parallel Execution
**Run phases 3-4 and 5-6 in parallel** when both sources selected:
- Phases 1-2 must complete sequentially first
- Then launch parallel execution for both tracks
- Phase 7 waits for all parallel work to complete

### Existing Files
**Skip existing benchmark files**:
- Check if file exists before implementation
- If exists, note in report and skip to next benchmark
- Only implement missing benchmarks

### Verification Failures
**Block workflow on verification failures**:
- Every benchmark MUST run successfully
- Every benchmark MUST produce correct output
- If verification fails, debug and fix the issue
- Query source website for clarification if needed
- Only skip if genuinely unfixable after multiple attempts
- Document reason for skipping in baseline report

## Key URLs

**Benchmarks Game**:
- Measurements: `https://benchmarksgame-team.pages.debian.net/benchmarksgame/measurements/{language}.html`
- Descriptions: `https://benchmarksgame-team.pages.debian.net/benchmarksgame/description/{benchmark}.html`
- Programs: `https://benchmarksgame-team.pages.debian.net/benchmarksgame/programs/{benchmark}-{language}.html`

**AreWeFastYet**:
- README: `https://raw.githubusercontent.com/smarr/are-we-fast-yet/refs/heads/master/README.md`
- Guidelines: `https://raw.githubusercontent.com/smarr/are-we-fast-yet/refs/heads/master/docs/guidelines.md`
- Benchmarks: `https://raw.githubusercontent.com/smarr/are-we-fast-yet/refs/heads/master/benchmarks/{language}/{benchmark}.{ext}`

## Output Files

This skill creates:
1. **Local benchmark files** - Executable implementations in target language
2. **BENCHMARK_BASELINE.md** - Comprehensive report with:
   - Language analysis
   - Comparable languages
   - Benchmark descriptions
   - Execution instructions
   - Reference performance data from comparable languages (Benchmarks Game)
   - **Actual performance data from your implementation**
   - Performance comparison tables
   - Interpretation guidance

The baseline file is continuously updated as benchmarks are run, creating a performance history.

## Usage Example

See [EXAMPLES.md](EXAMPLES.md) for complete example walkthrough showing:
- Language analysis for Lox implementation
- Comparable language selection (Lua, Python 3)
- Benchmark discovery and selection
- Local implementation process
- Generated baseline report structure

## Integration with Other Skills

**Use this skill BEFORE**:
- `trace-performance-warnings` - Identifies optimization barriers
- `cpu-sampler` - Profiles execution time
- `trace-compilation` - Analyzes compilation behavior
- `analyze-compiler-graph` - Deep-dive compiler optimization

**Workflow**:
```
1. [benchmark-baseline] → Establish expectations (e.g., 50-80s range)
2. [Run benchmarks]     → Get actual results (e.g., 120s - slower!)
3. [Profile/diagnose]   → Identify optimization barriers
4. [Fix issues]         → Address problems
5. [Run benchmarks]     → Verify improvements (e.g., 75s - better!)
6. [Iterate]            → Continue optimizing toward baseline
```

**Important**: Use AreWeFastYet benchmarks for profiling work (micro-benchmarks designed for language implementation analysis). Use Benchmarks Game data for broader language comparison context.

## Limitations

- Benchmarks Game: Isolated test environment, not production-representative
- AreWeFastYet: Micro-benchmarks don't reflect real applications
- Performance data shows what's possible, not guaranteed results
- Some language features may lack direct equivalents
- Translation quality depends on language similarity

## Summary

The benchmark-baseline skill provides automated baseline establishment through:
1. **Analysis** - Understand your language's characteristics
2. **Discovery** - Find comparable languages and industry-standard benchmarks
3. **Implementation** - Create local executable benchmarks with verification
4. **Documentation** - Generate comprehensive baseline report

Always run this skill before performance optimization to set realistic expectations and establish measurement infrastructure.
