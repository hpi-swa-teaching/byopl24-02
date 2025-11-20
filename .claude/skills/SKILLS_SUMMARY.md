# Skills Summary

This document provides an overview of all skills created for Lox/Truffle performance analysis and optimization.

## Skill Organization

### 1. Individual Tool Skills (8 skills)

Quick-reference skills for individual profiling tools. Use when you need focused information about a specific tool.

| Skill File | Tool | Size | Purpose |
|------------|------|------|---------|
| `cpu-sampler-analyze.md` | CPU Sampler | 7.7KB | Time-based sampling - WHERE time spent |
| `cpu-tracer-analyze.md` | CPU Tracer | 16KB | Execution frequency - HOW OFTEN executed |
| `trace-compilation-analyze.md` | Trace Compilation | 19KB | Compilation lifecycle - WHEN compiles |
| `trace-inlining-analyze.md` | Trace Inlining | 20KB | Inlining decisions - WHAT inlines |
| `trace-transfer-to-interpreter-analyze.md` | Transfer to Interpreter | 21KB | Deoptimizations - WHEN falls back |
| `memory-tracer-analyze.md` | Memory Tracer | 20KB | Allocation patterns - WHAT allocates |
| `trace-performance-warnings-analyze.md` | Performance Warnings | 16KB | Optimization barriers - WHY fails |
| `analyze-compiler-graph.md` | Compiler Graphs | 14KB | IR analysis - HOW compiler optimized |

**Total**: 8 skills, ~134KB

### 2. Hierarchical Workflow Skill (1 skill)

Comprehensive workflow guide with structured subdocuments. Use when you need systematic optimization approach.

**Main Skill**: `language-implementation-performance/SKILL.md` (16KB)

**Structure**:
```
language-implementation-performance/
├── SKILL.md                    # Main workflow guide
├── README.md                   # Documentation
├── cpu-sampler/                # 6 files
├── cpu-tracer/                 # 6 files
├── trace-compilation/          # 6 files
├── trace-inlining/             # 6 files
├── trace-transfer-to-interpreter/  # 6 files
├── memory-tracer/              # 6 files
├── trace-performance-warnings/ # 6 files
└── compiler-graphs/            # 6 files
```

**Per-Tool Subdocuments** (6 files each):
- `overview.md` - What the tool does, when to use
- `usage.md` - Command syntax and options
- `analysis.md` - How to interpret output
- `patterns.md` - Common problem patterns
- `resolutions.md` - How to fix issues
- `workflow.md` - Step-by-step guide

**Total**: 50 markdown files (1 main + 1 README + 48 subdocuments)

## When to Use Which Skill

### Use Individual Skills When:
- ✅ You know which tool you need
- ✅ You want quick reference for command syntax
- ✅ You need focused information about one tool
- ✅ You're already familiar with the optimization workflow

**Example**: "I need to run CPU Sampler with tier information" → Use `cpu-sampler-analyze.md`

### Use Hierarchical Skill When:
- ✅ Starting performance optimization work
- ✅ Need systematic workflow guidance
- ✅ Want to understand tool relationships
- ✅ Following multi-tool diagnosis process
- ✅ Learning the optimization methodology

**Example**: "My Lox code is slow, where do I start?" → Use `language-implementation-performance/SKILL.md`

## The Performance Optimization Workflow

The hierarchical skill guides you through 7 phases:

```
1. IDENTIFY HOTSPOTS        → CPU Sampler, CPU Tracer
   ↓
2. CHECK COMPILATION        → Trace Compilation
   ↓
3. FIND BARRIERS ⭐         → Trace Performance Warnings (START HERE!)
   ↓
4. CHECK INLINING           → Trace Inlining
   ↓
5. CHECK STABILITY          → Trace Transfer to Interpreter
   ↓
6. CHECK ALLOCATIONS        → Memory Tracer
   ↓
7. DEEP DIVE (LAST!)        → Compiler Graphs
```

## Skill Features Comparison

| Feature | Individual Skills | Hierarchical Skill |
|---------|------------------|-------------------|
| **Focus** | Single tool | Complete workflow |
| **Structure** | Flat (single file) | Hierarchical (multiple files) |
| **Size** | 8-21KB per skill | 50 files total |
| **Best For** | Quick reference | Systematic optimization |
| **Cross-References** | Within tool | Across tools |
| **Examples** | Tool-specific | End-to-end workflows |
| **Patterns** | Tool-specific issues | Multi-tool diagnosis |

## Quick Reference: Finding Information

### "How do I profile my Lox program?"
→ `language-implementation-performance/SKILL.md` → Phase 1

### "What does this CPU Sampler output mean?"
→ `cpu-sampler-analyze.md` or `language-implementation-performance/cpu-sampler/analysis.md`

### "My code keeps deoptimizing"
→ `trace-transfer-to-interpreter-analyze.md` or `language-implementation-performance/trace-transfer-to-interpreter/patterns.md`

### "Functions won't inline"
→ `trace-inlining-analyze.md` or `language-implementation-performance/trace-inlining/patterns.md`

### "I see performance warnings, what now?"
→ `trace-performance-warnings-analyze.md` or `language-implementation-performance/trace-performance-warnings/resolutions.md`

### "Too much GC overhead"
→ `memory-tracer-analyze.md` or `language-implementation-performance/memory-tracer/workflow.md`

### "Need to understand compiler IR"
→ `analyze-compiler-graph.md` or `language-implementation-performance/compiler-graphs/overview.md`

## Permissions Configuration

All skills are enabled in `.claude/settings.local.json`:

```json
{
  "allow": [
    "Skill(language-implementation-performance)",  // Hierarchical skill
    "Skill(cpu-sampler-analyze)",                 // Individual skills
    "Skill(cpu-tracer-analyze)",
    "Skill(trace-inlining-analyze)",
    "Skill(trace-transfer-to-interpreter-analyze)",
    "Skill(memory-tracer-analyze)",
    "Skill(trace-compilation-analyze)",
    "Skill(trace-performance-warnings-analyze)",
    "Skill(analyze-compiler-graph)"
  ]
}
```

## Documentation Source

All skills are based on comprehensive documentation in:
- `docs/commands/cpu-sampler.md`
- `docs/commands/cpu-tracer.md`
- `docs/commands/trace-compilation.md`
- `docs/commands/trace-inlining.md`
- `docs/commands/trace-transfer-to-interpreter.md`
- `docs/commands/memory-tracer.md`
- `docs/commands/trace-performance-warnings.md`
- `docs/commands/analyze-compiler-graph.md`

The hierarchical skill's subdocuments are automatically split from these source files.

## Key Benefits

### Individual Skills
- ✅ Fast access to single tool information
- ✅ Concise reference for known tools
- ✅ Easy to scan for specific commands
- ✅ Self-contained documentation

### Hierarchical Skill
- ✅ Systematic optimization workflow
- ✅ Clear decision points for tool selection
- ✅ Cross-tool problem diagnosis
- ✅ Complete examples with verification
- ✅ Pattern-based troubleshooting
- ✅ Structured subdocuments for deep dives

## Statistics

- **Total Skills**: 9 (8 individual + 1 hierarchical)
- **Total Files**: 58 markdown files
- **Total Documentation**: ~200KB+
- **Tools Covered**: 8 profiling/diagnostic tools
- **Phases in Workflow**: 7 optimization phases
- **Common Patterns Documented**: 10+ across all tools

## Maintenance

- Individual skills: Self-contained, update in place
- Hierarchical skill:
  - Main workflow in `SKILL.md`
  - Subdocuments auto-split from `docs/commands/`
  - Use `/tmp/split_docs.py` script to regenerate if source docs change

## Usage Examples

### Example 1: Quick Tool Usage
```bash
# I want to profile with CPU Sampler
# Open: cpu-sampler-analyze.md
# Find: "Basic Profiling" section
# Run: ./lox --cpusampler --cpusampler.Delay=2000 program.lox
```

### Example 2: Systematic Optimization
```bash
# My Lox program is slow
# Open: language-implementation-performance/SKILL.md
# Follow: Phase 1 → Identify hotspots
# Then: Phase 3 → Find barriers
# Finally: Implement fixes and verify
```

### Example 3: Specific Problem
```bash
# Seeing deoptimization loops
# Open: language-implementation-performance/trace-transfer-to-interpreter/patterns.md
# Find: "Deoptimization Loop" pattern
# Follow: Resolution steps
# Verify: Re-run profiling
```

## Future Enhancements

Potential additions:
- Benchmark-specific workflows
- IDE integration guides
- Automated analysis scripts
- Performance regression testing guides
- Common optimization recipes

## Related Resources

- Main documentation: `docs/commands/`
- CLAUDE.md: Project-specific guidance
- GraalVM docs: https://www.graalvm.org/latest/
- Truffle optimization guide: https://github.com/oracle/graal/tree/master/truffle/docs
