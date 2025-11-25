---
name: Establish Benchmark Baseline
description: Analyzes the current language implementation to determine its complexity and abstraction level, then queries the Computer Language Benchmarks Game to find comparable languages and retrieve their benchmark results for establishing performance baselines
---

# Skill: Establish Benchmark Baseline

This skill establishes performance baselines for any language implementation by:
1. Analyzing the codebase to understand the language's characteristics (typing, execution model, complexity)
2. Querying the Computer Language Benchmarks Game (https://benchmarksgame-team.pages.debian.net/benchmarksgame/) to find comparable languages
3. Discovering available benchmarks on the Benchmarks Game website
4. Selecting appropriate benchmarks to implement locally (3-5 covering different patterns)
5. Fetching reference implementations from comparable languages
6. Creating local benchmark files translated to the target language syntax
7. Preparing benchmarks to run with appropriate test parameters
8. Providing baseline data and context for performance comparison

**Important**: This skill should be run BEFORE any performance analysis to establish realistic expectations and create the benchmarks to measure.

## What This Skill Does

### Phase 1: Language Analysis
**Analyzes the current language implementation** by examining:
- Type system (static/dynamic typing)
- Execution model (interpreted, bytecode VM, JIT-compiled, AOT-compiled)
- Paradigm support (imperative, object-oriented, functional)
- Runtime characteristics (garbage collection, memory management)
- Implementation platform (native, JVM/Truffle, LLVM, custom VM)
- Language complexity level (minimal, moderate, full-featured)

### Phase 2: Comparable Language Identification
**Determines which languages provide good comparison points** based on:
- Similar abstraction levels
- Similar execution models
- Similar complexity and feature sets
- Similar runtime characteristics

### Phase 3: Benchmark Discovery and Data Retrieval
**Queries the Computer Language Benchmarks Game** to:
- Discover all available benchmark programs
- Fetch execution times (elapsed and CPU) for each benchmark
- Retrieve memory usage measurements
- Identify multiple implementation variants per benchmark
- Select 3-5 appropriate benchmarks to implement locally

### Phase 4: Local Benchmark Implementation
**Creates local benchmark files** by:
- Fetching reference implementations from comparable languages
- Translating algorithms to target language syntax
- Adapting to target language's features and conventions
- Writing benchmark files ready to execute
- Including verification logic and test parameters

### Phase 5: Baseline Report Generation
**Produces and saves a comprehensive baseline report** containing:
- Identified language characteristics
- Selected comparable languages with rationale
- List of benchmarks discovered on Benchmarks Game
- Newly created local benchmark files
- Execution instructions for each benchmark
- Cached performance data from comparable languages (date-stamped)
- Performance expectations and interpretation guidance
- All saved to `BENCHMARK_BASELINE.md` for future reference

## When to Use This Skill

Use this skill **before** running performance analysis to:

- **Establish realistic performance expectations** based on language characteristics
- **Understand the performance landscape** of comparable languages
- **Set optimization goals** grounded in empirical data
- **Provide context** for interpreting profiling results
- **Compare actual results** against industry-standard benchmarks

**Important**: This skill should be the FIRST step in performance optimization work. It provides the baseline context needed to interpret results from profiling skills (CPU sampler, tracer, compiler graph analysis).

## How the Skill Works

### Phase 1: Analyze Language Implementation

**Objective**: Understand the current language's characteristics to determine appropriate comparisons

**Process**:

1. **Examine project structure and documentation**
   - Read CLAUDE.md, README files
   - Analyze build configuration (pom.xml, package.json, etc.)
   - Review language grammar and parser files

2. **Determine type system**
   - Static typing vs. dynamic typing
   - Type inference capabilities
   - Type checking approach

3. **Identify execution model**
   - Pure interpreter (AST walking)
   - Bytecode VM with interpreter
   - JIT compilation (method-based, tracing, etc.)
   - AOT compilation
   - Hybrid approaches

4. **Assess implementation platform**
   - Native implementation (C/C++/Rust)
   - JVM-based (Truffle, Graal, standard JVM)
   - LLVM-based
   - Custom VM
   - Existing runtime (Node.js, CPython, etc.)

5. **Evaluate language complexity**
   - Minimal (basic types, control flow)
   - Moderate (OO, closures, arrays)
   - Full-featured (modules, metaprogramming, advanced features)

6. **Identify runtime characteristics**
   - Garbage collection (mark-sweep, generational, etc.)
   - Memory management approach
   - Concurrency model
   - Standard library richness

**Output**:
- Structured profile of the language implementation
- Key characteristics summary
- Complexity level assessment

**Example Output**:
```
Language: [Detected from project]
Type System: [static/dynamic] typing
Execution Model: [interpreter/bytecode VM/JIT/AOT/hybrid]
Platform: [native/JVM/LLVM/custom]
Paradigm: [imperative/OO/functional/multi-paradigm]
Complexity: [minimal/moderate/full-featured]
GC: [mark-sweep/generational/reference counting/manual/platform-managed]
Optimization: [specific optimizations detected]
```

### Phase 2: Identify Comparable Languages

**Objective**: Find languages in the Benchmarks Game that match the analyzed characteristics

**Process**:

1. **Match by execution model priority**
   - First: Same execution model (bytecode VM + JIT)
   - Second: Similar execution model (interpreted dynamic languages)
   - Third: Aspirational targets (same platform, e.g., other JVM languages)

2. **Match by type system**
   - Prioritize languages with same typing discipline
   - Dynamic-typed languages for dynamic implementations
   - Static-typed languages only as aspirational comparisons

3. **Match by paradigm and complexity**
   - Similar language features (OO, closures, etc.)
   - Similar complexity level
   - Similar abstraction capabilities

4. **Validate availability**
   - Check if languages exist on Benchmarks Game
   - Verify they have sufficient benchmark coverage
   - Ensure recent measurements available

**Output**:
- List of 2-4 comparable languages
- Rationale for each selection
- URL to their Benchmarks Game measurement pages

**Example Output (for a dynamic, OO, VM-based language)**:
```
Primary Comparisons:
1. [Language A] - [Characteristics matching target language]
   Rationale: Most similar execution model and abstraction level
   URL: https://benchmarksgame-team.pages.debian.net/benchmarksgame/measurements/[lang-a].html

2. [Language B] - [Characteristics matching target language]
   Rationale: Similar paradigm and complexity
   URL: https://benchmarksgame-team.pages.debian.net/benchmarksgame/measurements/[lang-b].html

3. [Language C] - [Characteristics matching target language]
   Rationale: Similar feature set and runtime characteristics
   URL: https://benchmarksgame-team.pages.debian.net/benchmarksgame/measurements/[lang-c].html

Aspirational (if applicable):
4. [Language D] - [Same platform or highly optimized]
   Rationale: Shows optimization potential or platform capabilities
   URL: https://benchmarksgame-team.pages.debian.net/benchmarksgame/measurements/[lang-d].html
```

**Common Comparison Patterns**:
- Dynamic languages → Lua, Python, Ruby, JavaScript, PHP
- Static JVM languages → Java, Scala, Kotlin
- Static native languages → C, C++, Rust, Go
- Functional languages → Haskell, OCaml, F#, Erlang
- Scripting languages → Perl, PHP, JavaScript

### Phase 3: Retrieve Benchmark Data

**Objective**: Fetch performance measurements and source code from the Computer Language Benchmarks Game

**Process**:

1. **Query Benchmarks Game for comparable languages**
   - Use WebFetch to retrieve measurement pages for each identified language
   - URLs format: `https://benchmarksgame-team.pages.debian.net/benchmarksgame/measurements/{language}.html`
   - Examples: `lua.html`, `python3.html`, `mri.html` (Ruby), `java.html`

2. **Extract available benchmark programs**
   - Parse benchmark categories from Benchmarks Game:
     - **Insignificant I/O** (pure computation): fannkuch-redux, n-body, spectral-norm
     - **Significant I/O**: mandelbrot, fasta, k-nucleotide, reverse-complement
     - **Contentious**: binary-trees, pidigits, regex-redux
   - List all benchmarks that have implementations for selected languages
   - Note benchmark variants (#1, #2, etc.) and their characteristics
   - Prioritize "insignificant I/O" benchmarks for pure computational comparison

3. **Collect performance metrics**

   For each benchmark and language, extract:

   - **Elapsed Time** - Wall-clock execution time (seconds)
     - Format: Single value or 95% confidence interval
     - Represents actual runtime from start to finish

   - **CPU Time** - Actual CPU consumption (seconds)
     - Format: Mean with confidence interval (e.g., "24.17–25.73")
     - More stable metric for CPU-bound tasks

   - **Memory** - Peak memory usage (bytes)
     - Represents maximum memory consumption during execution

   - **Test Parameter (N)** - Input size/scale for the benchmark
     - Important for understanding workload size
     - Example: N=21 for binary-trees, N=25000000 for fasta

4. **Select benchmarks for local implementation**
   - Review available benchmarks on Benchmarks Game
   - Prioritize benchmarks to implement based on:
     - Computational focus (prefer "insignificant I/O")
     - Language feature coverage (arrays, objects, recursion, etc.)
     - Relevance to language's intended use cases
     - Availability of good reference implementations
   - Typically select 3-5 benchmarks covering different computational patterns
   - Check if any already exist locally (to avoid duplicates)

5. **Handle measurement methodology**

   Understanding from https://benchmarksgame-team.pages.debian.net/benchmarksgame/how-programs-are-measured.html:

   - **Measurement Tool**: BenchExec with Linux cgroups for isolation
   - **Environment**: Quad-core 3.0GHz Intel i5-3330, 15.8GB RAM, Ubuntu 24.04
   - **Protocol**:
     - 12 runs if within timeout
     - First run excluded to reduce bias
     - Results: lowest elapsed time OR 95% confidence interval of CPU time
     - File system caches cleared before measurement
   - **Caveats**: Isolated environment, not representative of production use

**Output**:
- Performance data table for each benchmark
- Measurement methodology notes
- Comparison URLs for language pairs
- Identification of fastest implementations per benchmark

### Phase 4: Implement Benchmarks Locally

**Objective**: Create local implementations of benchmarks from Benchmarks Game in the target language

**Process**:

1. **Select benchmarks from Benchmarks Game**
   - Use the list of benchmarks identified in Phase 3
   - Select 3-5 benchmarks to implement covering different patterns:
     - Array-intensive (fannkuch-redux, spectral-norm)
     - Object/allocation-heavy (binary-trees)
     - Numerical computation (n-body, mandelbrot)
     - String manipulation (reverse-complement, fasta)
   - Check if any already exist locally to avoid duplication
   - Prioritize "insignificant I/O" benchmarks for pure performance testing

2. **Fetch reference implementations from comparable languages**
   - Retrieve source code from Benchmarks Game for selected benchmarks
   - Get implementations from comparable languages (identified in Phase 2)
   - Focus on "idiomatic" implementations (not heavily optimized variants)
   - Examine algorithm structure, data structures, and approach

3. **Translate to target language syntax**

   Analyze the target language's syntax and convert reference implementations accordingly:

   **Key Translation Areas:**

   - **Data Structures**: Convert arrays, hash tables, lists to target language equivalents
     - Reference: `arr = []` or `arr[0] = 1`
     - Target: Use language-specific array syntax (may be native arrays, library types, etc.)

   - **Control Flow**: Adapt loops and conditionals to target syntax
     - Reference: `for i in range(10):` or `while condition:`
     - Target: Use language-specific loop syntax (C-style, iterator-based, etc.)

   - **Object-Oriented Features**: Convert classes, methods, inheritance
     - Reference: `class Foo(Base):` or `def method(self):`
     - Target: Use language-specific OO syntax (if supported)

   - **Function Definitions**: Adapt function/method declaration syntax
     - Reference: `def func(x, y):` or `function func(x, y) {}`
     - Target: Use language-specific function syntax

   - **Type Annotations**: Add or remove type information as needed
     - Static languages: Add type declarations where required
     - Dynamic languages: Remove type annotations from references

   - **Memory Management**: Adapt allocation/deallocation patterns
     - Manual memory: Add explicit allocation/freeing
     - GC languages: Remove explicit memory management

   **Conversion Strategy:**
   1. Identify core algorithm and data structures
   2. Map reference language constructs to target language equivalents
   3. Preserve algorithmic structure while adapting syntax
   4. Handle language feature mismatches (e.g., no hash tables, no classes)
   5. Maintain comparable complexity (don't over-optimize or under-implement)
   6. **Important**: Don't change the algorithmic logic—only adapt syntax and constructs!

4. **Structure according to project conventions**

   Examine existing benchmarks in the project to understand:
   - File organization and naming conventions
   - Benchmark harness or framework usage
   - Entry point patterns (main function, class, module export, etc.)
   - Setup/teardown patterns
   - Result verification methods
   - Command-line argument handling

   Follow the established patterns:
   ```
   [Language-specific imports or includes]

   [Benchmark class/function/module definition]
       [Initialization/setup code]

       [Main benchmark logic]

       [Result verification]

       [Cleanup if needed]

   [Entry point following project conventions]
   ```

5. **Write to local file**
   - Create file with appropriate extension in same location as existing benchmarks
   - Use descriptive filename matching benchmark purpose
   - Follow project naming conventions
   - Example: `fannkuch.ext`, `nbody.ext`, `binarytrees.ext`

6. **Prepare execution instructions**
   - Document the command to run the benchmark
   - Specify appropriate iteration counts
   - Note the expected N parameter to match Benchmarks Game tests
   - Differentiate N for a `DEV_N` (smaller for quick tests) vs. full N for performance runs

**Example Output for fannkuch-redux:**

File: `fannkuch.[ext]`
```
[Language-specific imports/includes]

[Benchmark structure following project conventions]
    [State variables]:
        perm, perm1, count arrays
        maxFlips, checksum counters

    [Main benchmark method]:
        n = 12
        [Algorithm implementation translated from reference language]
        return result

    [Result verification]:
        [Check result matches expected value for N=12]

[Entry point following project pattern]
```

**Run Command:**
```bash
[project-specific command] [benchmark-name] [iterations]
```

**Expected Performance** (from baseline data):
- Reference Language A: 24.15 seconds
- Reference Language B: 311-2,457 seconds
- Target Range: [estimated based on language characteristics]

**Output**:
- New benchmark file(s) created in project directory
- Documented run commands
- Expected performance ranges from baseline data
- Verification logic to ensure correctness

### Phase 5: Generate and Save Baseline Report

**Objective**: Produce and save a comprehensive baseline report for future reference

**Report File**: Create `BENCHMARK_BASELINE.md` (or similar) in project root

**Report Structure:**

#### 1. Language Analysis Summary
   - Analyzed language characteristics
   - Type system, execution model, platform
   - Complexity level and feature set
   - Runtime characteristics

#### 2. Comparable Languages Selection
   - List of 2-4 selected comparable languages
   - Rationale for each selection
   - Classification (primary comparison vs. aspirational target)
   - Links to Benchmarks Game measurement pages

#### 3. Measurement Methodology Context
   - How benchmarks are measured (from https://benchmarksgame-team.pages.debian.net/benchmarksgame/how-programs-are-measured.html)
   - Test environment specifications
   - Measurement protocol (12 runs, confidence intervals, etc.)
   - Important caveats about isolated environment

#### 4. Benchmark Performance Data

   For each relevant benchmark:

   **Benchmark Name**: [e.g., binary-trees, fannkuch-redux, n-body]

   **Comparable Language Results**:
   | Language | Elapsed Time (s) | CPU Time (s) | Memory (bytes) | N Parameter |
   |----------|------------------|--------------|----------------|-------------|
   | Lua      | X.XX             | Y.YY–Z.ZZ    | M              | N           |
   | Python 3 | X.XX             | Y.YY–Z.ZZ    | M              | N           |
   | Ruby     | X.XX             | Y.YY–Z.ZZ    | M              | N           |
   | Java*    | X.XX             | Y.YY–Z.ZZ    | M              | N           |

   *Aspirational target

   **Performance Range**: Minimum to maximum across comparable languages
   **Expected Performance**: Realistic target range for the analyzed language

#### 5. Benchmarks Selected from Benchmarks Game
   - List of benchmarks chosen for implementation
   - Rationale for each selection
   - Computational patterns covered:
     - Array operations
     - Object allocation
     - Numerical computation
     - String manipulation
     - Recursion
   - Links to Benchmarks Game benchmark descriptions

#### 6. Newly Created Local Implementations
   - List of benchmark files created during this skill execution
   - File locations and names (e.g., `fannkuch.[ext]`, `nbody.[ext]`)
   - Verification logic included
   - Test parameters (N values) matching Benchmarks Game tests

#### 7. How to Execute Benchmarks

   For each created benchmark, document:

   **Benchmark Name**: [e.g., fannkuch-redux]

   **File**: `[benchmark-name].[ext]`

   **Run Command**:
   ```bash
   [project-specific command] [benchmark-name] [iterations]
   ```

   **Test Parameter**: N=[value] (matches Benchmarks Game test size)

   **Verification**: [How to verify correctness, if applicable]

   **Example**:
   ```bash
   # For fannkuch with N=12, run 10 iterations
   [project-command] fannkuch 10
   ```

#### 8. Cached Reference Language Performance

   For each benchmark, save baseline data for future comparison:

   **Benchmark**: [benchmark-name]

   **Test Parameter**: N=[value]

   **Reference Performance** (from Benchmarks Game):

   | Language     | Elapsed Time (s) | CPU Time (s)   | Memory (bytes) | Date Retrieved |
   |--------------|------------------|----------------|----------------|----------------|
   | [Language A] | X.XX             | Y.YY–Z.ZZ      | M              | YYYY-MM-DD     |
   | [Language B] | X.XX             | Y.YY–Z.ZZ      | M              | YYYY-MM-DD     |
   | [Language C] | X.XX             | Y.YY–Z.ZZ      | M              | YYYY-MM-DD     |

   **Source**: [URL to Benchmarks Game measurement page]

   **Performance Range**: [Min] - [Max] seconds across comparable languages

   This cached data allows future performance comparisons without re-fetching from the web.

#### 9. Performance Expectations
   - **Best Case**: If optimizer works exceptionally well (approach aspirational targets)
   - **Expected Case**: Typical performance for similar language category
   - **Worst Case**: If optimization fails or significant issues present

#### 10. Next Steps
   - Test newly created benchmarks to verify correctness
   - Run all benchmarks using documented commands
   - Compare actual results against cached baseline ranges
   - Use profiling skills if performance is below expected range
   - Iterate on optimizations based on findings
   - Update this report with actual results as you optimize

**Output Format:**
- Saved markdown file: `BENCHMARK_BASELINE.md` (or similar name)
- Structured sections with clear headings
- Performance comparison tables with cached data
- Executable commands for all benchmarks
- Links to all Benchmarks Game sources
- Date-stamped baseline data for future reference

## Example Usage Scenarios

### Scenario 1: Initial Baseline Before Any Optimization

**User**: "I want to understand how fast my language implementation should be compared to other languages"

**Skill Actions**:
1. Analyzes language implementation characteristics
2. Identifies comparable languages from Benchmarks Game
3. Fetches benchmark data for all available programs
4. Creates comparison table showing expected performance ranges
5. Generates report with realistic performance targets

**Output Example**:
```
## Baseline Performance Expectations for [Language Name]

### Language Analysis
- Type System: Dynamic typing
- Execution Model: Bytecode VM with JIT compilation
- Platform: [Detected platform]
- Paradigm: Object-oriented, imperative

### Comparable Languages Selected
1. **[Language A]** - Most similar execution model and abstraction level
2. **[Language B]** - Similar paradigm and type system
3. **[Language C]** - Similar complexity level
4. **[Language D]** (aspirational) - Shows optimization potential

### Benchmark: [Local Benchmark Name]

**Benchmarks Game Equivalent**: binary-trees (tree allocation patterns)

**Performance Baselines**:
- Language A: 47.56 seconds, 2.0 MB memory
- Language B: 33-100 seconds, variable memory
- Language C: 50-120 seconds, variable memory
- Language D: 2.62-5.05 seconds (aspirational)

**Expected Performance**: 30-100 seconds
- Best case: 30-40s (if optimizer works well)
- Expected: 50-80s (typical for similar languages)
- Worst case: 100-150s (if optimization struggles)

**Key Insights**:
- Tree allocation/GC pressure is main bottleneck
- Object creation patterns affect performance significantly
- Optimization potential depends on implementation quality

**Next Steps**:
1. Run local benchmark to get actual time
2. Compare against 50-80s expected range
3. If above range, investigate with profiling tools
4. If below range, optimization is working well
```

### Scenario 2: Discovering and Creating Benchmarks from the Web

**User**: "Create benchmark implementations from the Benchmarks Game"

**Skill Actions**:
1. Analyzes language implementation characteristics
2. Identifies comparable languages from Benchmarks Game
3. Lists available benchmarks on Benchmarks Game
4. Selects 3-5 appropriate benchmarks (fannkuch-redux, binary-trees, n-body, etc.)
5. Fetches reference implementations from comparable languages
6. Translates algorithms to target language syntax
7. Creates multiple benchmark files in project directory
8. Provides run commands and expected performance for each

```
## Benchmarks Discovered and Created

### Available Benchmarks on Benchmarks Game
Found 10 benchmarks with implementations in comparable languages:
- fannkuch-redux, binary-trees, n-body, spectral-norm (insignificant I/O)
- mandelbrot, fasta, k-nucleotide, reverse-complement (significant I/O)
- pidigits, regex-redux (contentious)

### Selected for Implementation: 5 benchmarks

**1. fannkuch-redux** (permutation generation, array operations)
**2. binary-trees** (object allocation, tree structures)
**3. n-body** (numerical computation, floating point)
**4. spectral-norm** (array operations, mathematical computation)
**5. fasta** (string generation, pseudo-random)

---

## Created File: fannkuch.[ext]

Successfully created fannkuch benchmark from Lua reference implementation.

### Run Command:
```bash
[project-command] fannkuch [iterations]
```

### Performance Baselines:

| Language  | Elapsed Time | N Parameter |
|-----------|--------------|-------------|
| Lua       | 24.15s       | N=12        |
| Python 3  | 311-2,457s   | N=12        |
| Ruby MRI  | ~500s        | N=12        |

### Expected Performance: 50-250 seconds

- Best case: ~50s (approaching Lua)
- Expected: 100-200s (typical for language category)
- Worst case: >250s (optimization issues)

### Key Optimizations to Watch:
- Array access patterns
- Loop unrolling
- Integer arithmetic

---

## Created Files Summary:

**Benchmark Implementations:**
- `fannkuch.[ext]` - Array permutations (N=12)
- `binarytrees.[ext]` - Tree allocation (N=21)
- `nbody.[ext]` - N-body simulation (N=50M)
- `spectralnorm.[ext]` - Matrix operations (N=5500)
- `fasta.[ext]` - String generation (N=25M)

**Baseline Report:**
- `BENCHMARK_BASELINE.md` - Complete baseline report with:
  - Language analysis
  - Comparable languages selection
  - Execution instructions for all benchmarks
  - Cached performance data from Lua, Python, Ruby
  - Performance expectations and next steps

---

## Sample from BENCHMARK_BASELINE.md:

### How to Execute: fannkuch

**File**: `fannkuch.[ext]`

**Run Command**:
```bash
[project-command] fannkuch 10
```

**Test Parameter**: N=12 (matches Benchmarks Game)

### Cached Baseline Performance: fannkuch

| Language  | Elapsed Time | CPU Time      | Memory   | Date       |
|-----------|--------------|---------------|----------|------------|
| Lua       | 24.15s       | 24.17–25.73s  | 3.6 KB   | 2025-11-25 |
| Python 3  | 311s         | 310–312s      | 8.2 KB   | 2025-11-25 |
| Ruby MRI  | 500s         | 498–502s      | 9.1 KB   | 2025-11-25 |

**Source**: https://benchmarksgame-team.pages.debian.net/benchmarksgame/measurements/lua.html

**Expected Performance**: 50-250 seconds

---

### Next Steps:
1. Review `BENCHMARK_BASELINE.md` for complete execution instructions
2. Test each benchmark for correctness
3. Run full benchmark suite using documented commands
4. Compare results against cached baseline data
5. Profile any benchmarks outside expected range
```

### Scenario 3: Interpreting Benchmark Results

**User**: "I ran my benchmark and got 75 seconds. Is this good?"

**Skill Actions**:
1. Analyzes language implementation characteristics
2. Identifies comparable languages
3. Fetches baseline data for equivalent Benchmarks Game program
4. Compares 75s against established ranges
5. Provides interpretation and recommendations

**Output Example**:
```
## Benchmark Analysis: 75 Seconds

### Language Characteristics
- Type System: [Detected]
- Execution Model: [Detected]
- Platform: [Detected]
- Paradigm: [Detected]

### Comparable Languages Selected
1. [Language A] (primary) - Similar execution model
2. [Language B] (primary) - Similar abstraction level
3. [Language C] (primary) - Similar paradigm
4. [Language D] (aspirational) - Optimization potential

### Benchmarks Game Equivalent: binary-trees
(Closest match - allocation patterns)

| Language  | Elapsed Time | Memory    | Test Parameter |
|-----------|--------------|-----------|----------------|
| Lang A    | 47.56s       | 2.0 MB    | N=21           |
| Lang B    | 33-100s      | Variable  | N=21           |
| Lang C    | 50-120s      | Variable  | N=21           |
| Lang D    | 2.62-5.05s   | Low       | N=21           |

### Your Result: 75 seconds

**Performance Assessment**: GOOD ✓

- Within Language B range (33-100s)
- Within Language C range (50-120s)
- 58% slower than Language A (acceptable)
- Significantly slower than Language D (expected, aspirational target)

### Interpretation
Your implementation performs comparably to similar languages.
Optimization appears to be working reasonably well.

### Next Steps
1. Profile to verify optimization is active
2. Check for performance barriers or deoptimization
3. If optimization metrics look good, 75s is a solid baseline
4. To improve further, analyze allocation patterns and hot paths
```

## Data Sources and URLs

### Main Entry Points
- **Homepage**: https://benchmarksgame-team.pages.debian.net/benchmarksgame/
- **Summary Charts**: https://benchmarksgame-team.pages.debian.net/benchmarksgame/box-plot-summary-charts.html

### Language Measurement Pages
Format: `https://benchmarksgame-team.pages.debian.net/benchmarksgame/measurements/{language}.html`

**Primary Comparable Languages**:
- Lua: https://benchmarksgame-team.pages.debian.net/benchmarksgame/measurements/lua.html
- Python 3: https://benchmarksgame-team.pages.debian.net/benchmarksgame/measurements/python3.html
- Ruby (MRI): https://benchmarksgame-team.pages.debian.net/benchmarksgame/measurements/mri.html

**Secondary Languages**:
- Node.js: https://benchmarksgame-team.pages.debian.net/benchmarksgame/measurements/node.html
- PHP: https://benchmarksgame-team.pages.debian.net/benchmarksgame/measurements/php.html

**Aspirational Targets**:
- Java: https://benchmarksgame-team.pages.debian.net/benchmarksgame/measurements/java.html
- Scala: https://benchmarksgame-team.pages.debian.net/benchmarksgame/measurements/scala.html

### Language Comparison Pages
Format: `https://benchmarksgame-team.pages.debian.net/benchmarksgame/fastest/{lang1}-{lang2}.html`

**Key Comparisons**:
- Python vs Lua: https://benchmarksgame-team.pages.debian.net/benchmarksgame/fastest/python3-lua.html
- Ruby vs Lua: https://benchmarksgame-team.pages.debian.net/benchmarksgame/fastest/ruby-lua.html
- Ruby vs Python: https://benchmarksgame-team.pages.debian.net/benchmarksgame/fastest/ruby-python3.html
- Node.js vs Ruby: https://benchmarksgame-team.pages.debian.net/benchmarksgame/fastest/node-ruby.html

### Benchmark-Specific Data

Individual benchmark results include:
- Multiple implementations per language
- Elapsed time (seconds)
- CPU time (with confidence intervals)
- Memory usage (bytes)
- Source code links
- Compiler flags and build information

## Understanding the Data

### Metrics Explained

**Elapsed Time**:
- Wall-clock time for program execution
- Most relevant for comparing overall performance
- Can include system delays, I/O wait

**CPU Time**:
- Actual CPU cycles consumed
- Often shown with confidence intervals (mean ± margin)
- More stable metric for CPU-bound benchmarks

**Memory Usage**:
- Peak memory consumption during execution
- Important for understanding allocation pressure
- Can indicate GC overhead

**Source Code Size**:
- Compressed (gzip) size of implementation
- Useful for understanding code complexity
- Not directly relevant for runtime performance

### Interpreting Variance

Benchmark results often show multiple implementations per language:
- Different algorithms or approaches
- Various optimization levels
- Trade-offs between speed and memory

**Example**: Lua mandelbrot benchmark has 4 variants with times ranging from fast to slow implementations.

**Implication**: There's often significant room for algorithmic optimization beyond language-level performance.

### Caveats and Limitations

From the Benchmarks Game documentation:

1. **"How the programs are written matters!"**
   - Implementation quality significantly affects results
   - Not just language speed, but programmer skill

2. **"Micro benchmarks"**
   - These are small, focused programs
   - Not representative of real-world applications
   - Good for comparing language performance, not predicting app performance

3. **Contentious Benchmarks**
   - Some benchmarks (pidigits, regex-redux) depend heavily on libraries
   - May not reflect language performance accurately
   - Focus on "insignificant I/O" benchmarks for purer comparisons

4. **Multiple Implementations**
   - Each language may have several implementations per benchmark
   - Some use "unsafe" features or hand-written optimizations
   - Look for "idiomatic" implementations for fairest comparison

## Integration with Other Skills

This skill works best in combination with:

### Before This Skill
- **None required** - This is typically the first skill to use

### After This Skill

Use profiling and analysis skills appropriate to your language implementation:

1. **Profiling Tools** (if available)
   - Run CPU profiler on benchmarks
   - Compare hotspots against baseline implementations
   - Identify where implementation is slower than comparable languages

2. **Execution Analysis** (if available)
   - Get exact execution counts or timing breakdowns
   - Verify optimization effectiveness
   - Compare execution patterns to baseline algorithms

3. **Optimization Diagnostics** (if available)
   - Check for optimization barriers
   - Compare warnings against patterns in baseline implementations
   - Identify platform-specific optimization opportunities

4. **Compilation Analysis** (if JIT/AOT compiled)
   - Verify critical code paths are compiling
   - Compare compilation success rate to expected behavior
   - Diagnose compilation failures

5. **Deep Optimization Analysis** (if advanced tools available)
   - Deep-dive into specific optimization failures
   - Understand why implementation isn't matching baseline performance
   - Identify missing optimizations

### Workflow Example

```
1. [benchmark-baseline] → Establish expected performance range (50-80s)
2. [Run benchmark]       → Actually get 120s (slower than expected)
3. [Profile]             → Identify hot code paths
4. [Diagnose]            → Find optimization barriers
5. [Fix code]            → Address identified issues
6. [Run benchmark]       → Now get 75s (within expected range!)
7. [Verify]              → Confirm optimizations are working
8. [Deep analysis]       → Understand remaining opportunities
9. [Optimize further]    → Improve implementation
10. [Run benchmark]      → Achieve 50s (approaching best comparable!)
```

## Important Notes

### Skill Workflow
1. **Run this skill FIRST** before any performance analysis
2. Skill analyzes language characteristics automatically
3. Skill fetches comparison data from Benchmarks Game
4. Skill creates local benchmark implementations (if needed)
5. Run newly created benchmarks to collect actual performance data
6. Use baseline report to interpret benchmark results
7. Follow up with profiling skills if performance is below expectations

### Understanding Baselines
- Benchmarks Game data is from isolated test environment
- Results show what's possible, not guaranteed performance
- Multiple implementations per language show variance
- Focus on performance ranges, not single data points

### Limitations
- Only retrieves data from Benchmarks Game website
- Cannot predict exact performance for your implementation
- Micro-benchmarks don't represent real-world applications
- Some language features may not have direct equivalents

## Summary

The **Establish Benchmark Baseline** skill analyzes your language implementation's characteristics, discovers benchmarks from the Computer Language Benchmarks Game, creates local implementations of those benchmarks translated to your language, and generates a comprehensive baseline report. This is a web-first approach: it finds industry-standard benchmarks online and brings them into your project as:

1. **Benchmark files** - Executable implementations in your language
2. **Baseline report** (`BENCHMARK_BASELINE.md`) - Saved documentation containing:
   - Execution instructions for all benchmarks
   - Cached performance data from comparable languages
   - Performance expectations and interpretation guidance
   - Links to Benchmarks Game sources (date-stamped for reference)

This gives you both the actual benchmarks to run and the saved baseline data to interpret results without re-fetching from the web. Always use this skill before detailed performance analysis.
