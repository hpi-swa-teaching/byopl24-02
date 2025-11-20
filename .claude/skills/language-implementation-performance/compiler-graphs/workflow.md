# Workflow Guide

This guide provides a practical workflow for using the compiler-graphs tool.

## Quick Start

```bash
EXTRA_JAVA_ARGS="-Djdk.graal.Dump=Truffle:1 \
  -Djdk.graal.PrintGraph=File \
  -Djdk.graal.DumpPath=compiler_graphs" \
  ./lox program.lox
```

See [usage.md](./usage.md) for detailed command options.

## Step-by-Step Investigation

### Step 1: Profile to Identify Hot Function
```bash
./lox --cpusampler --cpusampler.ShowTiers=true program.lox
```
**Identify**: Function consuming most time

### Step 2: Dump Graphs for Hot Function Only
```bash
EXTRA_JAVA_ARGS="-Djdk.graal.Dump=Truffle:1 \
  -Djdk.graal.MethodFilter=*hotFunction* \
  -Djdk.graal.DumpPath=compiler_graphs" \
  ./lox --experimental-options \
  --engine.CompileOnly=hotFunction \
  program.lox
```

### Step 3: Convert to JSON
```bash
bgv2json compiler_graphs/*.bgv > graphs.json
```

### Step 4: Find "After TruffleTier" Graph
```bash
cat graphs.json | jq 'select(.name | contains("After TruffleTier"))'  | head -1 > truffle-tier.json
```

### Step 5: Check for Common Issues
```bash
# Indirect calls?
cat truffle-tier.json | jq '.nodes[] | select(.props.label | contains("IndirectCall"))'

# Allocations?
cat truffle-tier.json | jq '.nodes[] | select(.props.label | test("Alloc|New"))'

# Boxing?
cat truffle-tier.json | jq '.nodes[] | select(.props.label | test("Box|Unbox"))'

# InvokeNodes (unspecialized)?
cat truffle-tier.json | jq '.nodes[] | select(.props.label == "InvokeNode") | .props'
```

### Step 6: Fix Issues

### Step 7: Verify Fixes
```bash
# Re-dump and re-analyze
# Should see problems eliminated
```

## Integration with Other Tools

See the main [workflow guide](../SKILL.md) for how this tool fits into the overall performance optimization workflow.

## Related Documentation

- [Main Workflow](../SKILL.md) - Overall performance optimization process
- [Usage Guide](./usage.md) - Command syntax and options
- [Analysis Guide](./analysis.md) - How to interpret output
- [Common Patterns](./patterns.md) - Problem identification
- [Resolutions](./resolutions.md) - How to fix issues
