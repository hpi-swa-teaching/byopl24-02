# Workflow Guide

This guide provides a practical workflow for using the cpu-sampler tool.

## Quick Start

```bash
./lox --cpusampler \
  --cpusampler.Delay=<ms> \
  --cpusampler.ShowTiers=true \
  --cpusampler.Output=histogram \
  <program.lox> [args...]
```

See [usage.md](./usage.md) for detailed command options.

## Integration with Other Tools

The skill may recommend using these tools for deeper analysis:

- **CPU Tracer**: Count execution frequencies (not time)
  ```bash
  ./lox --cputracer --cputracer.TraceStatements program.lox
  ```

- **Trace Compilation**: Understand compilation/deoptimization
  ```bash
  ./lox --engine.TraceCompilation program.lox
  ```

- **Trace Inlining**: See inlining decisions
  ```bash
  ./lox --engine.TraceInlining program.lox
  ```

## Related Documentation

- [Main Workflow](../SKILL.md) - Overall performance optimization process
- [Usage Guide](./usage.md) - Command syntax and options
- [Analysis Guide](./analysis.md) - How to interpret output
- [Common Patterns](./patterns.md) - Problem identification
- [Resolutions](./resolutions.md) - How to fix issues
