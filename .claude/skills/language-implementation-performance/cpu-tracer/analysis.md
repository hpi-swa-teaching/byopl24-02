# Analysis


## 4. Output Format & Structure

### Output Type
- ☑ Console/Terminal (stdout by default)
- ☑ File output (text format via --cputracer.OutputFile)
- ☑ File output (JSON format via --cputracer.Output=json)
- ☐ Graphical
- ☐ Binary

### Output Location

- **Default**: stdout (console)
- **Custom specification**: Use `--cputracer.OutputFile=<path>` to specify file
- **Timing**: Output appears at program completion

### Output Format Example

**Histogram Format (Default)**:

```
-----------------------------------------------------------------------------------------
Tracing Histogram. Counted a total of 468336895 element executions.

Total Count: Number of times the element was executed and percentage of total executions.
Interpreted Count: Number of times the element was interpreted and percentage of total executions of this element.
Compiled Count: Number of times the compiled element was executed and percentage of total executions of this element.
-----------------------------------------------------------------------------------------

Name       | Total Count      | Interpreted Count | Compiled Count    | Location
-----------------------------------------------------------------------------------------
accept     | 234117338 50.0%  | 365660 0.2%      | 233751678 99.8%  | primes.js~15:245-258
accept     | 117053670 25.0%  | 182582 0.2%      | 116871088 99.8%  | primes.js~16-18:275-348
accept     | 117005061 25.0%  | 181001 0.2%      | 116824060 99.8%  | primes.js~19:362-381
accept     | 53608 0.0%       | 1829 3.4%        | 51779 96.6%      | primes.js~14:211-227
accept     | 53608 0.0%       | 1829 3.4%        | 51779 96.6%      | primes.js~13-22:191-419
accept     | 48609 0.0%       | 1581 3.3%        | 47028 96.7%      | primes.js~17:322-334
accept     | 4999 0.0%        | 248 5.0%         | 4751 95.0%       | primes.js~21:402-413
accept     | 1 0.0%           | 1 100.0%         | 0 0.0%           | primes.js~2-4:25-61
accept     | 1 0.0%           | 1 100.0%         | 0 0.0%           | primes.js~3:45-55
-----------------------------------------------------------------------------------------
```

**JSON Format** (via `--cputracer.Output=json`):
- Machine-readable JSON structure with similar data
- Includes hierarchical call graph information
- Parseable by automated analysis tools or jq
- Exact schema varies by version but maintains execution count and location data

### Output Fields/Metrics

| Field                 | Description                          | Unit                   | Interpretation                      |
| --------------------- | ------------------------------------ | ---------------------- | ----------------------------------- |
| **Name**              | Function/method/statement identifier | string                 | Code element being traced           |
| **Total Count**       | Absolute execution count             | integer count          | Higher = hotter code path           |
| **Total Count %**     | Percentage of all traced executions  | percentage (0-100%)    | Relative importance of this element |
| **Interpreted Count** | Executions in interpreted mode       | integer count          | Executions before compilation       |
| **Interpreted %**     | Interpreted as % of element's total  | percentage (0-100%)    | Lower is better for hot code        |
| **Compiled Count**    | Executions in compiled mode          | integer count          | Executions after Graal compilation  |
| **Compiled %**        | Compiled as % of element's total     | percentage (0-100%)    | Higher is better for hot code       |
| **Location**          | Source location                      | file~line(s):offset(s) | Where in source code                |

**Location Format Details**:
- Single line: `file.js~15:245-258` (file, line 15, characters 245-258)
- Line range: `file.js~16-18:275-348` (file, lines 16-18, characters 275-348)
- Multiple splits: Same function may appear multiple times with different locations due to specialization
