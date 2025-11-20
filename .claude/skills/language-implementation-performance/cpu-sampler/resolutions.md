# Resolutions

This guide provides solutions to common issues identified by cpu-sampler.

## Best Practices for Fixes

The skill follows these profiling best practices:

1. **Always use delay**: Skip warmup phase with `--cpusampler.Delay=<ms>`
2. **Start with histogram**: Get overview before drilling down
3. **Enable tier info**: Always use `--cpusampler.ShowTiers=true`
4. **Progressive analysis**: histogram → calltree → flamegraph as needed
5. **Verify fixes**: Re-run profiling after optimizations to confirm improvements

## Related Documentation

- [Common Patterns](./patterns.md) - Problem identification
- [Analysis Guide](./analysis.md) - Understanding the output
- [Main Workflow](../SKILL.md) - Overall optimization process
