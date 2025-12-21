---
name: graal-truffle-docs
description: Fetches information from official GraalVM, Truffle, and Graal compiler documentation for API guidance, performance optimization, compiler flags, profiling, specializations, Bytecode DSL, Truffle DSL, partial evaluation, and compilation analysis. Use when you need authoritative documentation about Truffle framework, GraalVM compiler options, or optimization techniques.
---

# Graal and Truffle Documentation Lookup

Searches official GraalVM, Truffle, and Graal compiler documentation to answer technical questions about APIs, performance optimization, and compiler behavior.

## Primary Documentation Sources

**GraalVM Official Docs**: https://docs.oracle.com/en/graalvm/
- Language implementation guides
- Tools and utilities
- Performance optimization guides

**GraalVM Tools Javadoc**: https://www.graalvm.org/tools/javadoc/
- API reference for `com.oracle.truffle.api.*` packages
- Node implementations, frame management, interop protocols

**Graal GitHub Repository**: https://github.com/oracle/graal
- Truffle framework documentation in `/truffle/docs/`
- Compiler documentation in `/compiler/docs/`
- SimpleLanguage reference implementation in `/truffle/src/com.oracle.truffle.sl/`
- Optimization guides and examples

## Performance and Analysis Documentation

### Compiler Options and Flags
**Profiling and Tracing**:
- `--engine.TraceCompilation` - Log compilation events
- `--engine.TracePerformanceWarnings` - Detect optimization barriers
- `--engine.TraceInlining` - Show inlining decisions
- `--engine.TraceTransferToInterpreter` - Track deoptimizations
- `--cpusampler`, `--cputracer`, `--memtracer` - Profiling tools

**Compilation Control**:
- `--engine.CompilationFailureAction` - Handle compilation failures
- `--engine.CompileImmediately` - Force immediate compilation
- `--engine.BackgroundCompilation` - Control async compilation
- `--engine.Inlining` - Enable/disable inlining
- `--engine.Splitting` - Control AST splitting

**Diagnostic Options**:
- `-Djdk.graal.Dump=Truffle` - Dump compiler graphs
- `-Djdk.graal.PrintGraph=File` - Output format for graphs
- `-Djdk.graal.DumpPath=<path>` - Location for dump files
- `-Djdk.graal.ShowConfiguration` - Display compiler settings

### Key Documentation Paths

**Performance Optimization**:
- `/truffle/docs/Optimizing.md` - Optimization strategies and best practices
- `/truffle/docs/Profiling.md` - Profiling tools and techniques
- `/truffle/docs/HostOptimization.md` - Host compilation optimization
- `/compiler/docs/` - Graal compiler architecture and tuning

**Truffle Framework**:
- `/truffle/docs/LanguageTutorial.md` - Language implementation guide
- `/truffle/docs/DSL.md` - Truffle DSL specializations and patterns
- `/truffle/docs/BytecodeDSL.md` - Bytecode DSL implementation
- `/truffle/docs/SpecializationStatistics.md` - Analyzing specialization usage

**Analysis and Debugging**:
- `/truffle/docs/Debugging.md` - Debugging Truffle languages
- `/truffle/docs/Instrumentation.md` - Instrumentation framework
- `/truffle/docs/Tools.md` - Available Truffle tools

**Reference Implementations**:
- `/truffle/src/com.oracle.truffle.sl/` - SimpleLanguage (complete example)
- `/truffle/src/com.oracle.truffle.sl.test/` - SimpleLanguage tests

## API Reference Areas

**Core Truffle API** (`com.oracle.truffle.api.*`):
- `com.oracle.truffle.api.nodes` - Node implementations, specializations
- `com.oracle.truffle.api.frame` - Frame descriptors, frame slots, frame access
- `com.oracle.truffle.api.dsl` - Truffle DSL annotations and node generation
- `com.oracle.truffle.api.bytecode` - Bytecode DSL annotations and builders
- `com.oracle.truffle.api.instrumentation` - Profiling and instrumentation
- `com.oracle.truffle.api.interop` - Polyglot interoperability
- `com.oracle.truffle.api.profiles` - Value profiles for optimization
- `com.oracle.truffle.api.assumption` - Assumptions and invalidation

**Tools and Instruments**:
- `org.graalvm.tools.insight` - Insight instrumentation tool
- `com.oracle.truffle.tools.profiler` - CPU and memory profilers
- `com.oracle.truffle.tools.chromeinspector` - Chrome Inspector protocol

## When to Use This Skill

Search documentation when you need:
- Explanation of compiler flags or profiling tools
- API usage for Truffle DSL, Bytecode DSL, or frame management
- Performance optimization strategies or partial evaluation guidance
- Understanding of compiler warnings or deoptimization causes
- Migration information between GraalVM versions
- Examples from SimpleLanguage or other reference implementations

## Complementary Skills

Use with performance analysis skills:
- **trace-performance-warnings-analyze** - Interpret specific compilation warnings
- **analyze-compiler-graph** - Understand graph optimizations with documentation context
- **cpu-sampler-analyze** - Learn profiling tool output interpretation
- **trace-compilation-analyze** - Understand compilation lifecycle and failures
- **trace-inlining-analyze** - Debug inlining decisions

## Notes

- Requires internet access for documentation fetching
- Documentation paths based on GraalVM repository structure (use GitHub search if paths change)
- Prioritizes official GraalVM/Truffle documentation over third-party sources
- Includes source links for deeper exploration
