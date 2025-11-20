# Analysis


## 4. Output Format & Structure

### Output Type

- **Console:** histogram and calltree formats default to stdout
- **File:** JSON and flamegraph output to files
- **Graphical:** Flamegraph generates interactive SVG visualization
- **Binary:** JSON format provides machine-readable structured data

### Output Location

| Format     | Default Location                     |
| ---------- | ------------------------------------ |
| histogram  | stdout                               |
| calltree   | stdout                               |
| json       | stdout                               |
| flamegraph | `flamegraph.svg` (current directory) |

Override any location with `--cpusampler.OutputFile=<path>`

### Output Format Examples

#### Histogram Format (Default)

```
----------------------------------------------------------------------------------------------
Sampling Histogram. Recorded 250 samples with period 10ms.
Self Time: Time spent on the top of the stack.
Total Time: Time spent somewhere on the stack.
----------------------------------------------------------------------------------------------
Thread[main,5,main]
Name             || Total Time        || Self Time         || Location
----------------------------------------------------------------------------------------------
accept           || 2150ms 86.0%      || 2150ms 86.0%      || primes.js~13-22:191-419
next             || 2470ms 98.8%      ||  320ms 12.8%      || primes.js~31-37:537-737
:program         || 2500ms 100.0%     ||   30ms  1.2%      || primes.js~1-46:0-982
----------------------------------------------------------------------------------------------
```

#### Call Tree Format

```
:program (100% total, 1.2% self)
├─ findPrime (95.7% total, 7.5% self)
│  └─ isPrime (88.2% total, 88.2% self)
└─ next (4.3% total, 4.3% self)
```

#### Tier Information Format (with --cpusampler.ShowTiers)

```
-----------------------------------------------------------------------------------------------------------------------------------------------------------
Sampling Histogram. Recorded 553 samples with period 10ms.
T0: Percent of time spent in interpreter.
T1: Percent of time spent in code compiled by tier 1 compiler.
T2: Percent of time spent in code compiled by tier 2 compiler.
-----------------------------------------------------------------------------------------------------------------------------------------------------------
Thread[main,5,main]
Name      || Total Time | T0    | T1    | T2    || Self Time | T0    | T1    | T2    || Location
-----------------------------------------------------------------------------------------------------------------------------------------------------------
accept    || 4860ms 87.9% | 31.1% | 18.3% | 50.6% || 4860ms 87.9% | 31.1% | 18.3% | 50.6% || primes.js~13-22:191-419
:program  || 5530ms 100.0% | 100.0%| 0.0%  | 0.0%  || 360ms 6.5%   | 100.0%| 0.0%  | 0.0%  || primes.js~1-46:0-982
-----------------------------------------------------------------------------------------------------------------------------------------------------------
```

### Output Fields/Metrics

| Field          | Description                                  | Unit                     | Interpretation                                                       |
| -------------- | -------------------------------------------- | ------------------------ | -------------------------------------------------------------------- |
| **Name**       | Function/root name from source code          | text                     | Identifies the profiled function or method                           |
| **Total Time** | Time function appears anywhere on call stack | milliseconds, percentage | Includes time in callees; high values indicate overall contribution  |
| **Self Time**  | Time function is at top of stack             | milliseconds, percentage | Excludes callees; high values indicate direct computation bottleneck |
| **Location**   | Source file, line range, character positions | formatted string         | Links profile data to source code for investigation                  |
| **T0**         | Percentage of time in interpreter            | percentage               | High values indicate lack of compilation or deoptimization           |
| **T1**         | Percentage of time in tier-1 compiled code   | percentage               | Fast-compiled code with basic optimizations                          |
| **T2**         | Percentage of time in tier-2 compiled code   | percentage               | Fully optimized code; high values indicate successful optimization   |
