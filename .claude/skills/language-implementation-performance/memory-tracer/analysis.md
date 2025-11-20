# Analysis


## 4. Output Format & Structure

### Output Type
- ☑ Console/Terminal output (printed on program exit)
- ☐ File output (can be redirected via shell)
- ☐ Graphical output
- ☐ Binary output

### Output Location
**Default Location**: Standard output (stdout) when program terminates

**Custom Location**: Use shell redirection to save output: `js script.js --experimental-options --memtracer > allocations.txt`

### Output Format Examples

**Histogram Mode (Default):**
```
Location Histogram with Allocation Counts.
Recorded a total of 5007 allocations.

Total Count: Number of allocations during the execution of this element.
Self Count: Number of allocations in this element alone (excluding sub calls).

Name                | Self Count    | Total Count   | Location
--------------------------------------------------------------------------------
next                | 5000  99.9%   | 5000  99.9%   | primes.js~31-37:537-737
:program            | 6     0.1%    | 5007  100.0%  | primes.js~1-46:0-982
Primes              | 1     0.0%    | 1     0.0%    | primes.js~25-38:424-739
```

**Annotations for Histogram:**
- Header explains total allocations recorded
- Each row represents a function or code location
- Self Count: allocations directly in this element (excluding called functions)
- Total Count: allocations including all sub-calls
- Percentages show proportion of total allocations
- Location format: `file~line-range:character-range`
- Ordered by Total Count descending (most allocations at top)

**Typehistogram Mode:** (format shows allocation counts grouped by object type/class rather than location; specific output format not documented in official sources but analogous to histogram with type names instead of function names)

**Calltree Mode:** (format shows hierarchical call tree with allocation counts at each level, similar to CPU profiler calltree; specific output format not documented in official sources but follows standard call tree conventions with indentation showing call hierarchy)

### Output Fields/Metrics

| Field       | Description                                                          | Unit                       | Interpretation                                                                                      |
| ----------- | -------------------------------------------------------------------- | -------------------------- | --------------------------------------------------------------------------------------------------- |
| Name        | Function name, method name, or code element identifier               | String                     | Identifies where allocations occur; `:program` represents top-level code                            |
| Self Count  | Number of allocations directly in this element, excluding sub-calls  | Count (with percentage)    | Shows allocations attributable exclusively to this function's own code                              |
| Total Count | Number of allocations including all functions called by this element | Count (with percentage)    | Shows total allocation impact including all downstream effects                                      |
| Location    | Source file and position                                             | Format: `file~lines:chars` | Direct pointer to source code location; lines and character ranges enable precise navigation        |
| Type/Class  | Object type or class name (typehistogram mode)                       | String                     | Shows what kinds of objects are created (e.g., Array, String, custom classes)                       |
| Percentage  | Proportion relative to total allocations                             | Percentage                 | Enables quick identification of allocation hotspots; high percentages indicate optimization targets |

**Note on Size Information**: The AllocationReporter API includes size parameters, but many language implementations report `SIZE_UNKNOWN` rather than actual byte counts. Therefore, memtracer primarily focuses on allocation counts rather than memory sizes in bytes. The tool tracks how many objects are allocated rather than how much memory they consume.
