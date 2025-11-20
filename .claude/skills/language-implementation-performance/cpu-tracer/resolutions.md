# Resolutions

This guide provides solutions to common issues identified by cpu-tracer.

## Best Practices for Fixes

The skill follows these profiling best practices:

1. **Progressive Granularity**
   - Start with `--cputracer` only (function-level)
   - Add `--cputracer.TraceCalls` for hot functions
   - Add `--cputracer.TraceStatements` with filters for deep dive

2. **Always Filter with TraceStatements**
   ```bash
   # ❌ BAD: Overwhelming overhead and output
   ./lox --cputracer --cputracer.TraceStatements program.lox

   # ✅ GOOD: Focused statement-level detail
   ./lox --cputracer --cputracer.TraceStatements \
     --cputracer.FilterRootName=*hotFunction* \
     program.lox
   ```

3. **Save Output to Files**
   ```bash
   ./lox --cputracer --cputracer.OutputFile=trace-run1.txt program.lox
   ```
   - Enables comparison across runs
   - Prevents terminal buffer overflow

4. **Use Short Representative Workloads**
   - Enough iterations to reach steady state and trigger compilation
   - Not unnecessarily long given overhead
   - Representative of actual usage patterns

5. **Target >95% Compiled for Hot Paths**
   - Hot code = >10,000 executions
   - <95% compiled = investigate with TraceCompilation

6. **Combine with CPUSampler**
   - Understand both frequency (tracer) and duration (sampler)
   - Correlate to find true optimization targets

## Related Documentation

- [Common Patterns](./patterns.md) - Problem identification
- [Analysis Guide](./analysis.md) - Understanding the output
- [Main Workflow](../SKILL.md) - Overall optimization process
