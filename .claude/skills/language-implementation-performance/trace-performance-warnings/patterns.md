# Common Patterns


## 5. Insights & Analysis

### When Should You Use It?

Language developers should deploy this tool when diagnosing unexpected slow performance after warmup periods, as it reveals why compiled code fails to achieve expected peak performance. The tool proves essential during development of new Truffle language implementations, helping developers understand which coding patterns work well with partial evaluation and which create optimization barriers. When investigating methods that remain in interpreter mode despite being hot, or when compiled code shows performance worse than expected, this tool identifies the specific AST node patterns preventing effective compilation. It serves as a preventative measure during code review, catching performance regressions before they reach production, and as a teaching tool for teams learning Truffle best practices.

**Source:** [Oracle GraalVM Optimizing Documentation](https://docs.oracle.com/en/graalvm/enterprise/21/docs/graalvm-as-a-platform/language-implementation-framework/Optimizing/)

### Actionable Insights

#### Performance Issue: Virtual Runtime Calls (call)

**Symptom in Output:**
```
Partial evaluation could not inline the virtual runtime call Virtual to HotSpotMethod<ConditionProfile.profile(boolean)>
```

**Root Cause:** Virtual method calls occur when the compiler cannot determine the exact method target at compile time due to polymorphism, interface calls, or insufficient type information. Profile objects like `ConditionProfile` or `BranchProfile` that should be compilation constants may be accessed through non-constant references, preventing their methods from being inlined. The partial evaluator treats these as opaque calls that must remain in compiled code.

**Resolution:** Apply `@TruffleBoundary` annotation to methods that should explicitly not be inlined (typically I/O, logging, or complex library calls). Use Truffle DSL's `@Specialization` to create type-specific code paths that allow static dispatch. Employ `@Cached` parameters to cache method targets or profile objects, ensuring they become compilation constants. For example, cache profile creation: `@Cached("createBinaryProfile()") ConditionProfile profile`. Verify that specialization guards are constant-foldable to enable profile specialization.

**Verification Tool:** Use `--engine.TraceInlining` to confirm the method is now being inlined after fixes are applied.

**Source:** [Oracle GraalVM Optimizing Documentation](https://docs.oracle.com/en/graalvm/enterprise/21/docs/graalvm-as-a-platform/language-implementation-framework/Optimizing/)

#### Performance Issue: Unresolved Type Checks (instanceof)

**Symptom in Output:**
```
Partial evaluation could not resolve virtual instanceof to an exact type
```

**Root Cause:** Type information remains polymorphic at the point of instanceof checks, preventing the compiler from eliminating runtime type checks. This occurs when multiple types flow through the same code path without proper specialization, when guards fail to narrow types sufficiently, or when type profiles haven't converged to stable patterns.

**Resolution:** Add type-specific specializations using DSL guards: `@Specialization(guards = "isString(value)")`. Cache type information with `@Cached("value.getClass()") Class<?> cachedClass`. For dynamic objects, implement shape-based checks that can be constant-folded. Use `@Cached` with `limit` parameter to handle limited polymorphism: `@Specialization(limit = "3")`. Consider splitting call sites using Truffle's splitting mechanism to create monomorphic copies.

**Verification Tool:** Use `--engine.CompilationStatistics` to verify that specializations are activating correctly and polymorphism is reduced.

**Source:** [Oracle GraalVM Optimizing Documentation](https://docs.oracle.com/en/graalvm/enterprise/21/docs/graalvm-as-a-platform/language-implementation-framework/Optimizing/)

#### Performance Issue: Non-Constant Store Locations (store)

**Symptom in Output:**
```
Store location argument is not a partial evaluation constant
```

**Root Cause:** Frame slot indices or object property locations cannot be determined at compile time, forcing dynamic lookup at runtime. This happens with computed frame slot access, variable property names, or frame descriptors that aren't compilation constants. The compiler cannot optimize these into direct memory accesses.

**Resolution:** Ensure `FrameSlot` objects are created during parsing and stored as compilation constants in AST nodes. Use final fields to store frame slots so they become partial evaluation constants. For property access, implement specialized nodes for known property names and cache property locations in stable object shapes. Avoid dynamic frame slot lookup methods; instead, pass slot identifiers as compilation constants through node constructors.

**Verification Tool:** Use IGV dumps (`--vm.Dgraal.Dump=Truffle:1`) to verify that frame access nodes use constant slot indices in the compiled graph.

**Source:** [Oracle GraalVM Optimizing Documentation](https://docs.oracle.com/en/graalvm/enterprise/21/docs/graalvm-as-a-platform/language-implementation-framework/Optimizing/)

### Correlation with Other Tools

This tool integrates into a comprehensive performance analysis workflow with complementary Truffle diagnostics. Use `--engine.TraceCompilation` alongside performance warnings to correlate which compilation units succeed or fail and whether warnings correspond to compilation bailouts. Combine with `--engine.TraceInlining` to understand the relationship between inlining decisions and call warnings—methods that trigger call warnings should appear as "not inlined" in the inlining tree. The `--cpusampler` profiler identifies which methods are hot and warrant investigation, directing focus to performance warnings in those specific methods. IGV dumps (`--vm.Dgraal.Dump=Truffle:1`) provide visual representation of the problematic IR nodes, using the node IDs from warning messages to locate exact graph positions. The `--engine.CompilationStatistics` option offers aggregate metrics that contextualize individual warnings within overall compilation health. Together, these tools form a complete diagnostic suite where performance warnings highlight specific problems while other tools provide context, verification, and visual understanding of optimization failures.

**Source:** [Oracle GraalVM Optimizing Documentation](https://docs.oracle.com/en/graalvm/enterprise/21/docs/graalvm-as-a-platform/language-implementation-framework/Optimizing/)
