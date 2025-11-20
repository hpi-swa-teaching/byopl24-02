# Analysis


## 4. Output Format & Structure

### Output Type

- **Console:** Standard output (stdout) with structured text format
- **Prefix:** All lines begin with `[engine]` marker
- **Format:** Pipe-separated fields for structured parsing

### Output Location

- **Default:** Standard output (stdout)
- **Cannot be redirected:** Output always goes to stdout; capture with shell redirection if needed

### Output Format Examples

#### Compilation Completion (opt done)

```
[engine] opt done id=244 EqualityConstraint.execute |Tier 1|Time 268( 220+47 )ms|AST 17|Inlined 0Y 2N|IR 238/ 437|CodeSize 1874|Timestamp 758868036671903|Src octane-deltablue.js:528
```

#### Extended Format (with engine and compilation IDs)

```
[engine] opt done engine=2 id=213 EqualityConstraint.execute |Tier 1|Time 14( 11+3 )ms|AST 31|Inlined 0Y 2N|IR 218/ 365|CodeSize 1386|Addr 0x782dd1fb9300|CompId 23519 |UTC 2025-07-08T08:25:20.339|Src octane-deltablue.js:528 0xb0b56c7b
```

#### Deoptimization Event

```
[engine] opt deopt EqualityConstraint.execute
```

#### Invalidation Event

```
[engine] opt inv. BinaryConstraint.output
```

#### Queue Events (with TraceCompilationDetails)

```
[engine] opt queued id=237 BinaryConstraint.output |Tier 1|Count/Thres 25/ 25|Queue: Size 1 Change +1 Load 0.06 Time 0us|Timestamp 758865671350686|Src octane-deltablue.js:416

[engine] opt start id=237 BinaryConstraint.output |Tier 1|Priority 25|Rate 0.000000|Queue: Size 0 Change +0 Load 0.06 Time 0us|Timestamp 758865708273384|Src octane-deltablue.js:416

[engine] opt unque. id=304 Date.prototype.valueOf |Tier 2|Count/Thres 80234/ 3125|Queue: Size 4 Change 0 Load 0.31 Time 0us|Timestamp 758899904132076|Src <builtin>:1|Reason Target inlined into only caller
```

### Output Fields/Metrics

#### Core Compilation Fields

| Field         | Description                                         | Unit                 | Interpretation                                                                                                 |
| ------------- | --------------------------------------------------- | -------------------- | -------------------------------------------------------------------------------------------------------------- |
| **engine**    | Unique identifier of the engine for the compilation | integer              | Multi-engine scenarios; distinguishes which engine context                                                     |
| **id**        | Unique identifier of the call target within engine  | integer              | Tracks specific call targets across events                                                                     |
| **Name**      | Method/function name being compiled                 | text                 | Identifies the guest-language method                                                                           |
| **Tier**      | Compilation tier                                    | "Tier 1" or "Tier 2" | Tier 1 = fast compilation; Tier 2 = full optimization                                                          |
| **Time**      | Compilation time breakdown                          | milliseconds         | Format: `Total(Truffle+Graal)ms` - first number is Truffle tier (partial evaluation), second is Graal compiler |
| **AST**       | Target's non-trivial Truffle node count             | integer              | Larger values indicate more complex methods                                                                    |
| **Inlined**   | Inlining statistics                                 | "XY ZN"              | X inlines succeeded (Y), Z remained as calls (N)                                                               |
| **IR**        | Graal IR node counts                                | "X / Y"              | X = nodes after partial evaluation; Y = nodes after full compilation                                           |
| **CodeSize**  | Generated machine code size                         | bytes                | Final native code size                                                                                         |
| **Addr**      | Memory address of installed code                    | hexadecimal          | Where compiled code resides in memory                                                                          |
| **CompId**    | VM-specific compilation ID                          | integer              | Matches IDs in deoptimization/code cache logs                                                                  |
| **Timestamp** | Event time from System.nanoTime()                   | nanoseconds          | High-precision event timing                                                                                    |
| **UTC**       | UTC timestamp                                       | ISO 8601             | Human-readable timestamp                                                                                       |
| **Src**       | Source location                                     | formatted string     | Abbreviated source section with hash code                                                                      |

#### Queue Event Fields (TraceCompilationDetails)

| Field             | Description                           | Unit                | Interpretation                                                                             |
| ----------------- | ------------------------------------- | ------------------- | ------------------------------------------------------------------------------------------ |
| **Count/Thres**   | Current count / Compilation threshold | "count / threshold" | Shows progress toward compilation threshold                                                |
| **Queue: Size**   | Number of compilations in queue       | integer             | Queue backlog; higher values indicate saturation                                           |
| **Queue: Change** | Queue size delta from this event      | integer             | +1 = added, -1 = removed, 0 = no change                                                    |
| **Queue: Load**   | Queue load metric                     | float               | 1.0 = normal; <1.0 = underloaded; >1.0 = overloaded (triggers dynamic threshold increases) |
| **Queue: Time**   | Event duration                        | microseconds        | Time spent in queue operation                                                              |
| **Priority**      | Compilation priority                  | integer             | Higher values compiled first                                                               |
| **Rate**          | Compilation rate metric               | float               | Internal scheduling metric                                                                 |
| **Reason**        | Event reason                          | text                | Runtime-reported reason for queue decision                                                 |
