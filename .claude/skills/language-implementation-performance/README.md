# Language Implementation Performance - Hierarchical Skill

This directory contains a comprehensive hierarchical skill for analyzing and improving Truffle language implementation performance.

## Structure

```
language-implementation-performance/
├── SKILL.md                          # Main workflow guide (START HERE!)
├── README.md                         # This file
├── cpu-sampler/                      # Time-based sampling profiler
│   ├── overview.md                   # What it does, when to use it
│   ├── usage.md                      # Command syntax and options
│   ├── analysis.md                   # How to interpret output
│   ├── patterns.md                   # Common problem patterns
│   ├── resolutions.md                # How to fix identified issues
│   └── workflow.md                   # Step-by-step usage guide
├── cpu-tracer/                       # Execution frequency counter
│   ├── overview.md
│   ├── usage.md
│   ├── analysis.md
│   ├── patterns.md
│   ├── resolutions.md
│   └── workflow.md
├── trace-compilation/                # Compilation lifecycle tracker
│   ├── overview.md
│   ├── usage.md
│   ├── analysis.md
│   ├── patterns.md
│   ├── resolutions.md
│   └── workflow.md
├── trace-inlining/                   # Inlining decision analyzer
│   ├── overview.md
│   ├── usage.md
│   ├── analysis.md
│   ├── patterns.md
│   ├── resolutions.md
│   └── workflow.md
├── trace-transfer-to-interpreter/    # Deoptimization tracker
│   ├── overview.md
│   ├── usage.md
│   ├── analysis.md
│   ├── patterns.md
│   ├── resolutions.md
│   └── workflow.md
├── memory-tracer/                    # Allocation profiler
│   ├── overview.md
│   ├── usage.md
│   ├── analysis.md
│   ├── patterns.md
│   ├── resolutions.md
│   └── workflow.md
├── trace-performance-warnings/       # Optimization barrier detector
│   ├── overview.md
│   ├── usage.md
│   ├── analysis.md
│   ├── patterns.md
│   ├── resolutions.md
│   └── workflow.md
└── compiler-graphs/                  # Deep IR analysis
    ├── overview.md
    ├── usage.md
    ├── analysis.md
    ├── patterns.md
    ├── resolutions.md
    └── workflow.md
```

## How to Use This Skill

### Quick Start

1. **Read the main workflow guide**: Start with `SKILL.md` which provides:
   - The recommended optimization workflow
   - Decision points for tool selection
   - Complete examples
   - Common performance patterns

2. **Follow the phases**: The workflow is structured in 7 phases:
   - Phase 1: Identify hotspots (CPU Sampler, CPU Tracer)
   - Phase 2: Verify compilation (Trace Compilation)
   - Phase 3: Find barriers ⭐ (Trace Performance Warnings)
   - Phase 4: Verify inlining (Trace Inlining)
   - Phase 5: Check stability (Trace Transfer to Interpreter)
   - Phase 6: Check allocations (Memory Tracer)
   - Phase 7: Deep IR analysis (Compiler Graphs)

3. **Drill into specific tools**: When you need details about a specific tool:
   - `overview.md` - Understand what it does
   - `usage.md` - Learn command syntax
   - `analysis.md` - Interpret the output
   - `patterns.md` - Recognize common issues
   - `resolutions.md` - Fix identified problems
   - `workflow.md` - Follow step-by-step guide

## Relationship to Individual Skills

This hierarchical skill **complements** the individual tool skills:

- **Individual skills** (e.g., `cpu-sampler-analyze.md`): Quick reference for single tool
- **Hierarchical skill** (`SKILL.md` + subdirs): Complete workflow with tool coordination

Both are kept in the codebase for different use cases:
- Use individual skills for focused tool usage
- Use hierarchical skill for comprehensive optimization workflow

## Key Features

### Workflow-Driven
The main `SKILL.md` guides you through a systematic approach rather than just documenting individual tools.

### Cross-Referenced
Each phase references relevant tool subdocuments with specific sections (e.g., `./cpu-sampler/analysis.md#tier-information`).

### Pattern-Based
Common performance patterns are documented with diagnosis workflows spanning multiple tools.

### Example-Driven
Concrete examples show complete optimization workflows from problem identification to verification.

## Common Entry Points

### "I need to optimize my language implementation"
→ Start with `SKILL.md` and follow Phase 1

### "I know CPU Sampler shows high T0 time, what's wrong?"
→ Go to `SKILL.md` → Phase 3 (Performance Warnings)

### "How do I use CPU Sampler?"
→ See `cpu-sampler/usage.md`

### "What do these performance warnings mean?"
→ See `trace-performance-warnings/patterns.md`

### "My code keeps deoptimizing"
→ See `trace-transfer-to-interpreter/patterns.md#deoptimization-loop`

## Documentation Size

- Main workflow: ~16KB (SKILL.md)
- Total subdocuments: 48 files across 8 tools
- Each tool: 6 documents (overview, usage, analysis, patterns, resolutions, workflow)

## Source Documentation

All subdocuments are automatically split from:
- `/Users/antonykamp/Projects/hpi-ma/byopl24-02/docs/commands/`

This ensures consistency with the comprehensive command documentation.
