# Analysis


## 4. Output Format & Structure

### Output Type

- ✅ **Console/Terminal** - Primary output destination (stdout)
- ❌ **File output** - Not directly supported; redirect stdout for file capture
- ❌ **Graphical output** - Text-based only; use with IGV for visual analysis
- ❌ **Binary data** - Human-readable text format only

### Output Location

| Aspect               | Details                                                                                                      |
| -------------------- | ------------------------------------------------------------------------------------------------------------ |
| **Default Location** | Standard output (stdout) with `[engine]` prefix                                                              |
| **Custom Location**  | Use shell redirection: `command > output.log 2>&1`                                                           |
| **Log Integration**  | General Truffle logging via `--log.file=<path>` does not capture performance warnings; they remain on stdout |

**Source:** [Oracle GraalVM Optimizing Documentation](https://docs.oracle.com/en/graalvm/enterprise/21/docs/graalvm-as-a-platform/language-implementation-framework/Optimizing/)

### Output Format Example

**Virtual Call Warning (from official documentation):**

```
[engine] perf warn ScaleConstraint.execute |Partial evaluation could not inline the virtual runtime call Virtual to HotSpotMethod<ConditionProfile.profile(boolean)> (167|MethodCallTarget).

Approximated stack trace for [167 | MethodCallTarget]:
  at com.oracle.truffle.js.nodes.control.IfNode.execute(IfNode.java:158)
  at com.oracle.truffle.js.nodes.binary.DualNode.execute(DualNode.java:125)
  at com.oracle.truffle.js.nodes.function.FunctionBodyNode.execute(FunctionBodyNode.java:73)
  at com.oracle.truffle.js.nodes.function.FunctionRootNode.executeInRealm(FunctionRootNode.java:147)
  at com.oracle.truffle.js.runtime.JavaScriptRealmBoundaryRootNode.execute(JavaScriptRealmBoundaryRootNode.java:93)
  at org.graalvm.compiler.truffle.runtime.OptimizedCallTarget.executeRootNode(OptimizedCallTarget.java:503)
  at org.graalvm.compiler.truffle.runtime.OptimizedCallTarget.profiledPERoot(OptimizedCallTarget.java:480)
```

**Annotations:**
- `[engine] perf warn` - Identifies message type and source
- `ScaleConstraint.execute` - Compilation unit (root method being compiled)
- `(167|MethodCallTarget)` - Graal IR node ID (searchable in IGV using `id=167`)
- Stack trace - Shows Truffle node execution path with source locations

**Source:** [Oracle GraalVM Optimizing Documentation](https://docs.oracle.com/en/graalvm/enterprise/21/docs/graalvm-as-a-platform/language-implementation-framework/Optimizing/)

### Output Fields/Metrics

| Field               | Description                     | Unit     | Interpretation                                                                    |
| ------------------- | ------------------------------- | -------- | --------------------------------------------------------------------------------- |
| **Prefix**          | `[engine] perf warn` identifier | N/A      | Distinguishes performance warnings from other engine messages                     |
| **Method Name**     | Compilation unit identifier     | String   | The root Truffle CallTarget being compiled; focus optimization efforts here       |
| **Warning Type**    | Implicit from message text      | Category | Identifies which optimization failed (call/instanceof/store/frame_merge/trivial)  |
| **Target**          | Problematic method/operation    | String   | The specific method call, type check, or store operation that cannot be optimized |
| **Node ID**         | Graal IR node identifier        | Integer  | Use in IGV to locate exact graph node; format: `(id\|NodeType)`                   |
| **Stack Trace**     | Approximated call hierarchy     | Lines    | Shows path through Truffle AST nodes; includes file:line when available           |
| **Source Location** | File and line number            | String   | Exact source code location causing the warning (when debug info available)        |
