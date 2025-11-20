# Usage


## 1. Command Execution

### Basic command syntax

```bash
<language-launcher> --experimental-options --engine.TraceInlining=true --engine.TraceInliningDetails <script>
```

### Full command syntax with flags

**JVM Mode:**
```bash
<language-launcher> --experimental-options --engine.TraceInlining \
  [--engine.InliningExpansionBudget<N>] \
  [--engine.InliningInliningBudget=<N>] \
  [--engine.InliningRecursionDepth=<N>] \
  [--engine.Inlining=<boolean>]
  <script>

# Example usage
js --experimental-options --engine.TraceInlinin script.js
```

### Execution context

| Mode                  | Supported | Requirements                                                                            |
| --------------------- | --------- | --------------------------------------------------------------------------------------- |
| **JVM Mode**          | Yes       | No special prerequisites; may require `--experimental-options` in some GraalVM versions |
| **Native Image Mode** | Yes       | No special prerequisites                                                                |

Both modes produce identical output format and tracing behavior.

### Prerequisites

- **No special build flags required** (unlike TraceTransferToInterpreter on Native Image)
- **Available as** `--engine.TraceInlining` or `--compiler.TraceInlining` (both equivalent)
- **GraalVM version**: Available in GraalVM 20.2.0+ (when language-agnostic inlining was introduced)
- May require `--experimental-options` flag depending on GraalVM version

---

## 2. Command Options & Parameters

### Primary option table

| Option Name                     | Type    | Default Value | Description                                            |
| ------------------------------- | ------- | ------------- | ------------------------------------------------------ |
| `--engine.TraceInlining`        | Boolean | `false`       | Print information for inlining decisions               |
| `--engine.TraceInliningDetails` | Boolean | `false`       | Print detailed information (entire explored call tree) |

### Inlining control options

| Option Name                        | Type                 | Default Value  | Description                                                                 |
| ---------------------------------- | -------------------- | -------------- | --------------------------------------------------------------------------- |
| `--engine.InliningExpansionBudget` | Integer [1, inf)     | `12000`        | Exploration budget in Graal node count                                      |
| `--engine.InliningInliningBudget`  | Integer [1, inf)     | `12000`        | Inlining budget in Graal node count                                         |
| `--engine.InliningRecursionDepth`  | Integer [0, inf)     | `2`            | Maximum depth for recursive inlining                                        |
| `--engine.Inlining`                | Boolean              | `true`         | Enable/disable automatic inlining                                           |
| `--engine.CompileOnly`             | Comma-separated list | No restriction | Restrict compilation to specified method names (or exclude with '~' prefix) |


### Key options explained in detail

The `--engine.TraceInlining` option enables tracing of language-agnostic inlining decisions during Truffle compilation. Unlike legacy AST-based inlining, this modern approach (introduced in GraalVM 20.2.0) performs partial evaluation on call candidates before making inlining decisions, using actual Graal IR node count as the size metric. This produces more accurate decisions but increases average compilation time by approximately 10%.

The `--engine.TraceInliningDetails` flag expands tracing to show the entire explored call tree, not just final inlining decisions. This reveals which methods were considered but rejected for inlining and why. The output volume increases substantially, making it suitable for deep analysis of specific compilations rather than routine use.

The two budget parameters control inlining aggressiveness through distinct mechanisms. **InliningExpansionBudget** (default 12,000 Graal nodes) limits how much partial evaluation the compiler performs to explore inlining candidates. When this budget exhausts, remaining candidates receive "Cutoff" status without evaluation. **InliningInliningBudget** (default 12,000 Graal nodes) limits the total size of the compilation unit after inlining. Methods that would exceed this budget are marked "Expanded" but not inlined. These budgets prevent compilation units from growing pathologically large, which would cause excessive compilation time or compiler failures.

### Advanced options for expert users

The `--engine.InliningRecursionDepth` parameter (default 2) controls how deeply recursive calls can be inlined. Recursive inlining unrolls recursive functions to a specified depth, allowing optimization of recursive algorithms. Higher values enable more loop unrolling but risk code explosion. Setting to 0 disables recursive inlining entirely.

Completely disabling inlining via `--engine.Inlining=false` serves as a diagnostic tool to measure inlining's performance impact. Comparing execution with and without inlining quantifies the benefit. Note that disabling inlining dramatically reduces performance—typically by 2-10x—because call overhead remains and cross-function optimizations cannot occur.

For focused debugging, combine `--engine.CompileOnly=functionName` with TraceInlining to isolate specific methods. This drastically reduces output volume when investigating why a particular function isn't inlining its callees as expected.

---
