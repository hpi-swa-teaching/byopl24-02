# Performance Analysis Script Usage

## Quick Start

```bash
./run-perf-analysis.sh <prefix> <prompt-file> [baseline-branch]
```

**Example:**
```bash
./run-perf-analysis.sh 2025-01-30--perf prompts/optimize.txt main
```

## Parameters

- `prefix` - Unique identifier for this analysis run (used in branch names)
- `prompt-file` - Text file containing the optimization task for Claude
- `baseline-branch` - (Optional) Git branch to use as baseline (default: `main`)

## How It Works

1. **Creates isolated branches** for each iteration: `<prefix>-run-<N>-iteration-<M>`
2. **Runs Claude autonomously** with your prompt for multiple iterations
3. **Each iteration** builds upon the previous one's improvements
4. **Commits changes** automatically with descriptive messages
5. **Runs benchmarks** at the end of each run

## Configuration

Default settings in the script:
- **Iterations per run**: 10
- **Total runs**: 5
- **Timeout per run**: 2 hours

## Handling Rate Limits

When Claude hits the rate limit:

1. **Script automatically saves state** (branch, iteration, session ID)
2. **Changes are committed** as "Work in progress"
3. **Script pauses** with instructions

**To resume:**
```bash
./run-perf-analysis.sh --continue <prefix> <prompt-file>
```

The script will:
- Resume the exact same iteration (no skipping)
- Continue the Claude session with `claude --continue`
- Pick up where it left off

**Alternative - Manual Claude resume:**
```bash
claude --continue <session-id>  # Session ID shown in output
```

## Example Workflow

```bash
# Start analysis
./run-perf-analysis.sh 2025-01-30--opt prompts/perf.txt main

# ... Claude works autonomously ...
# ... Rate limit hit at iteration 3 ...

# Wait for limit to reset, then continue
./run-perf-analysis.sh --continue 2025-01-30--opt prompts/perf.txt

# ... Resumes iteration 3 and continues ...
```

## Output

- **Branches**: One per iteration (`<prefix>-run-<N>-iteration-<M>`)
- **Benchmark results**: `benchmark-results-<prefix>-run-<N>.txt`
- **State file**: `perf-analysis/.state-<prefix>` (for resuming)

## Error Handling

- **Rate limit**: Saves state and exits (resume with `--continue`)
- **2 consecutive failures**: Assumes persistent issue, saves state and stops
- **Timeout**: Commits partial changes and moves to next run
- **Single error**: Skips iteration and continues

## Tips

- Use a descriptive prefix with date: `2025-01-30--feature-name`
- Keep prompt files focused on specific optimization goals
- The script creates many branches - clean up old ones periodically
- State files are automatically deleted when runs complete successfully
