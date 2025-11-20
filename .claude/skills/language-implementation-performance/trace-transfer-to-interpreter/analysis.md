# Analysis


## 4. Output Format & Structure

### Output characteristics

- **Output type**: Text-based console output to standard error (stderr)
- **Output location**: Console by default; redirectable using standard shell redirection (`2>file.txt`) or `--log.file=<path>`
- **Format prefix**: All output lines prefixed with `[engine]` tag
- **Timing**: Output appears immediately when transfers occur during execution

### Example output with annotations

**Native Image Mode (Full Stack Trace):**

```
[Deoptimization initiated
name: String#[]
sp: 0x7ffd7b992710 ip: 0x7f26a8d8079f
reason: TransferToInterpreter action: InvalidateReprofile
debugId: 25 speculation: jdk.vm.ci.meta.SpeculationLog$NoSpeculationReason@13dbed9e
stack trace that triggered deoptimization:
at org.truffleruby.core.string.StringNodesFactory$StringSubstringPrimitiveNodeFactory$StringSubstringPrimitiveNodeGen.execute(StringNodesFactory.java:12760)
at org.truffleruby.core.string.StringNodes$GetIndexNode.substring(StringNodes.java:836)
at org.truffleruby.core.string.StringNodes$GetIndexNode.getIndex(StringNodes.java:650)
at org.truffleruby.core.string.StringNodesFactory$GetIndexNodeFactory$GetIndexNodeGen.execute(StringNodesFactory.java:1435)
at org.truffleruby.language.RubyCoreMethodRootNode.execute(RubyCoreMethodRootNode.java:53)
[Deoptimization of frame
name: String#[]
sp: 0x7ffd7b992710 ip: 0x7f26a8d8079f
stack trace where execution continues:
at org.truffleruby.core.string.StringNodesFactory$StringSubstringPrimitiveNodeFactory$StringSubstringPrimitiveNodeGen.execute(StringNodesFactory.java:12760) bci 99 return address 0x4199a1d
]
]
```

**HotSpot Mode (Guest Language Stack):**

```
[engine] transferToInterpreter at
BinaryConstraint.output(../../../../4dev/js-benchmarks/octane-deltablue.js:416)
Constraint.satisfy(../../../../4dev/js-benchmarks/octane-deltablue.js:183)
Planner.incrementalAdd(../../../../4dev/js-benchmarks/octane-deltablue.js:597) <split-609bcfb6>
Constraint.addConstraint(../../../../4dev/js-benchmarks/octane-deltablue.js:165) <split-7d94beb9>
UnaryConstraint(../../../../4dev/js-benchmarks/octane-deltablue.js:219) <split-560348e6>
Function.prototype.call(<builtin>:1) <split-1df8b5b8>
EditConstraint(../../../../4dev/js-benchmarks/octane-deltablue.js:315) <split-23202fce>
...
com.oracle.truffle.api.CompilerDirectives.transferToInterpreterAndInvalidate(CompilerDirectives.java:90)
com.oracle.truffle.js.nodes.access.PropertyCacheNode.deoptimize(PropertyCacheNode.java:1269)
com.oracle.truffle.js.nodes.access.PropertyGetNode.getValueOrDefault(PropertyGetNode.java:305)
```

### Output fields and metrics table

| Field Name            | Description                            | Unit            | Interpretation                                                              |
| --------------------- | -------------------------------------- | --------------- | --------------------------------------------------------------------------- |
| **name**              | CallTarget or method being deoptimized | String          | Identifies which compiled code is exiting                                   |
| **sp**                | Stack pointer at deoptimization        | Memory address  | Low-level debugging reference                                               |
| **ip**                | Instruction pointer at deoptimization  | Memory address  | Exact machine code location                                                 |
| **reason**            | Why deoptimization occurred            | Enum            | Most commonly "TransferToInterpreter" for explicit transfers                |
| **action**            | What action compiler should take       | Enum            | "InvalidateReprofile" means discard and recompile with new profile          |
| **debugId**           | Debug identifier                       | Integer         | Correlates with IGV graph dumps when using `--vm.Djdk.graal.Dump=Truffle:1` |
| **speculation**       | Speculation reason object              | Class reference | Tracks speculative optimizations that failed                                |
| **bci**               | Bytecode index                         | Integer         | Position in method bytecode (Native Image)                                  |
| **return address**    | Memory address to return to            | Memory address  | Continuation point in interpreter                                           |
| **Split identifiers** | `<split-xxxxxxxx>` tags                | Hex string      | Identifies split (monomorphized) call targets                               |

---
