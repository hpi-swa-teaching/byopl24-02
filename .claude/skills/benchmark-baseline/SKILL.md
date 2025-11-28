---
name: Establish Benchmark Baseline
description: Analyzes the current language implementation to determine its complexity and abstraction level, then queries the Computer Language Benchmarks Game and AreWeFastYet to find comparable languages and retrieve their benchmark results for establishing performance baselines
---

# Skill: Establish Benchmark Baseline

This skill establishes performance baselines for any language implementation by:
1. Analyzing the codebase to understand the language's characteristics (typing, execution model, complexity)
2. Querying the Computer Language Benchmarks Game (https://benchmarksgame-team.pages.debian.net/benchmarksgame/) to find comparable languages
3. Discovering available benchmarks on the Benchmarks Game website
4. Selecting appropriate benchmarks to implement locally (3-5 covering different patterns)
5. Fetching reference implementations from comparable languages
6. Creating local benchmark files translated to the target language syntax
7. Fetching the benchmark implementation from AreWeFastYet
8. Implementing the AreWeFastYet benchmarks in the target language syntax
9. Preparing benchmarks to run with appropriate test parameters
10. Providing baseline data and context for performance comparison

**Important**: This skill should be run BEFORE any performance analysis to establish realistic expectations and create the benchmarks to measure.

**Side Note**: Phases 1-2 must complete first, then 3-4 and 5-6 can run independently in parallel.

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

### Phase 3: Benchmarks Game Discovery and Data Retrieval
**Queries the Computer Language Benchmarks Game** to:
- Discover all available benchmark programs
- Fetch execution times (elapsed and CPU) for each benchmark
- Retrieve memory usage measurements
- Identify multiple implementation variants per benchmark
- Select 3-5 appropriate benchmarks to implement locally

### Phase 4: Local Benchmark Implementation of Benchmarks Game Programs
**Creates local benchmark files** by:
- Fetching reference implementations from comparable languages
- Fetching the implementation description from Benchmarks Game
- Translating algorithms to target language syntax
- Adapting to target language's features and conventions
- Writing benchmark files ready to execute
- Including verification logic and test parameters

### Phase 5: AreWeFastYet Benchmark Discovery and Data Retrieval
**Queries AreWeFastYet** to:
- Discover available benchmarks relevant to the target language
- Fetch execution times and memory usage for each benchmark
- Select appropriate benchmarks to implement locally

### Phase 6: Local Benchmark Implementation of AreWeFastYet Programs
**Creates local benchmark files** by:
- Fetching reference implementations from comparable languages
- Fetching the implementation description from AreWeFastYet
- Translating algorithms to target language syntax
- Adapting to target language's features and conventions
- Writing benchmark files ready to execute
- Including verification logic and test parameters

### Phase 7: Baseline Report Generation for Benchmark Game and AreWeFastYet
**Produces and saves a comprehensive baseline report** containing:
- Identified language characteristics
- Selected comparable languages with rationale
- List of benchmarks discovered on Benchmarks Game
- List of benchmarks discovered on AreWeFastYet
- Newly created local benchmark files
- Execution instructions for each benchmark
- Cached performance data from comparable languages if available
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
- List of 1-2 comparable languages
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
```

**Common Comparison Patterns**:
- Dynamic languages → Lua, Python, Ruby, JavaScript, PHP
- Static JVM languages → Java, Scala, Kotlin
- Static native languages → C, C++, Rust, Go
- Functional languages → Haskell, OCaml, F#, Erlang
- Scripting languages → Perl, PHP, JavaScript

### Phase 3: Retrieve Benchmark Data from Benchmarks Game

**Objective**: Fetch performance measurements and source code from the Computer Language Benchmarks Game.

**Process**:

1. **Query Benchmarks Game for comparable languages**
   - Use WebFetch to retrieve measurement pages for each identified language
   - URLs format: `https://benchmarksgame-team.pages.debian.net/benchmarksgame/measurements/{language}.html`
   - Examples: `lua.html`, `python3.html`, `ruby.html`, `javavm.html`, `julia.html`, `node.html`

2. **Extract available benchmark programs**
   - Parse benchmark categories from Benchmarks Game:
     - **Insignificant I/O** (pure computation): fannkuch-redux, n-body, spectral-norm
     - **Significant I/O**: mandelbrot, fasta, k-nucleotide, reverse-complement
     - **Contentious**: binary-trees, pidigits, regex-redux
   - List all benchmarks that have implementations for selected languages
   - Note benchmark variants (#1, #2, etc.) and their characteristics
   - URLs format algorithm description: `https://benchmarksgame-team.pages.debian.net/benchmarksgame/description/{benchmark}.html`
   - URLs format measurements: `https://benchmarksgame-team.pages.debian.net/benchmarksgame/performance/{benchmark}.html`
   - Examples: `binarytrees.html`, `fannkuchredux.html`, `nbody.html`

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
   - Typically select minimum 3, maximum 5 benchmarks covering different computational patterns
   - If Benchmark Game benchmark implementations already exist, count them toward the 3-5 total. Only implement missing ones from your selection.

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

### Phase 4: Implement Benchmarks from Benchmarks Game Locally

**Objective**: Create local implementations of benchmarks from Benchmarks Game in the target language

**Process**:

1. **Select benchmarks from Benchmarks Game**
   - Use the list of benchmarks identified in Phase 3

2. **Fetch the implementation description from Benchmarks Game**
   - Retrieve algorithm descriptions for selected benchmarks
   - URLs format: `https://benchmarksgame-team.pages.debian.net/benchmarksgame/description/{benchmark}.html`
   - Understand the computational pattern and requirements

3. **Fetch reference implementations from comparable languages**
   - Retrieve source code from Benchmarks Game for selected benchmarks
   - Get implementations from comparable languages (identified in Phase 2)
   - Focus on "idiomatic" implementations (not heavily optimized variants)
   - Examine algorithm structure, data structures, and approach
   - URLs format: `https://benchmarksgame-team.pages.debian.net/benchmarksgame/programs/{benchmark}-{language}.html`

4. **Structure according to project conventions**
Examine existing benchmarks in the project to understand:
   - File organization and naming conventions
   - Benchmark harness or framework usage
   - Entry point patterns (main function, class, module export, etc.)
   - Setup/teardown patterns
   - Result verification methods
   - Command-line argument handling

5. **Implement Benchmarks Locally**
   - Given the benchmark algorithm description and reference implementations, create local files in the target language
   - Translate algorithms to target language syntax
   - Adapt to target language features (data structures, control flow, libraries)
   - Follow project conventions for structure and organization

6. **Write to local file**
   - Create file with appropriate extension in same location as existing benchmarks
   - Use descriptive filename matching benchmark purpose
   - Follow project naming conventions
   - Include verification logic in benchmark to ensure correct results
   - Compare output against known correct values from Benchmarks Game
   - Example: `fannkuch.ext`, `nbody.ext`, `binarytrees.ext`

7. **Prepare execution instructions**
   - Document the command to run the benchmark
   - Specify appropriate iteration counts
   - Note the expected N parameter to match Benchmarks Game tests

8. **Run the benchmark**
   - Test the benchmark locally to ensure it runs without errors
   - Verify output correctness against expected results
   - Fix any translation issues or bugs
   - If cannot be fixed, document in baseline report and skip

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

### Phase 5: Retrieve Benchmark Data from AreWeFastYet

**Objective**: Fetch performance measurements and source code from AreWeFastYet

**Process**:
1. **Query available benchmarks from AreWeFastYet**
   - Use WebFetch to retrieve the list of benchmarks
   - AreWeFastYet differentiates between micro and macro benchmarks
   - Focus on micro benchmarks
   - Ignore macro benchmarks and benchmarks from Benchmarks Game (same name)
   - URL README: `https://raw.githubusercontent.com/smarr/are-we-fast-yet/refs/heads/master/README.md`
   - URL Guidelines: `https://raw.githubusercontent.com/smarr/are-we-fast-yet/refs/heads/master/docs/guidelines.md`

2. **Query available programming languages from AreWeFastYet**
   - Use WebFetch to retrieve the list of programming languages
   - Identify comparable languages from Phase 2
   - URL Language Folder: `https://raw.githubusercontent.com/smarr/are-we-fast-yet/refs/heads/master/benchmarks`

**Output**:

- List of benchmarks discovered on AreWeFastYet
- List of programming languages discovered on AreWeFastYet

### Phase 6: Implement Benchmarks from AreWeFastYet Locally

1. **Extract available benchmark programs**
   - Look for available benchmark implementations
   - Select the implementation for the comparable languages identified in Phase 2
   - Extract verification logic and test parameters from implementations
   - URL format for benchmarks: `https://raw.githubusercontent.com/smarr/are-we-fast-yet/refs/heads/master/benchmarks/{language}/{benchmark}.{ext}`

2. **Implement the harness structure according to project conventions**
   - Examine existing benchmarks in the project to understand:
      - File organization and naming conventions
      - Benchmark harness or framework usage
      - Entry point patterns (main function, class, module export, etc.)
      - Setup/teardown patterns
      - Result verification methods
      - Command-line argument handling

3. **Create local benchmark files**
   - Translate algorithms to target language syntax
   - Translate ALL  micro benchmarks from AreWeFastYet (AreWeFastYet specifically designed for language implementation analysis)
   - Adapt to target language's features and conventions
   - Write benchmark files ready to execute
   - Include verification logic and test parameters
   - Don't implement macro benchmarks or benchmarks already from Benchmarks Game
   - Don't change the algorithms, only translate them to the target language
   - Run each benchmark to verify correctness, fix any issues (if needed query AreWeFastYet for clarification again)
   - If cannot be fixed, document in baseline report and skip

### Phase 7: Generate and Save Baseline Report

**Objective**: Produce and save a comprehensive baseline report for future reference

**Report File**: Create `BENCHMARK_BASELINE.md` (or similar) in project root

**Report Structure:**

#### 1. Language Analysis

- Analyzed language characteristics
- Type system, execution model, platform
- Complexity level and feature set
- Runtime characteristics
- Categories: Type system, Execution model, Platform, Paradigm, Complexity

#### 2. Comparable Languages Selection from Benchmarks Game

- List of 1-2 selected comparable languages
- Rationale for each selection
- Classification (primary comparison vs. aspirational target)
- Links to Benchmarks Game measurement pages

For each comparable language:

   **Language Name**: [e.g., Lua, Python 3]
   **Rationale**: Why this language was selected
   **Url**: Link to Benchmarks Game measurement page

#### 3. Benchmark Selected from Benchmarks Game

For each relevant benchmark:

   **Benchmark Name**: [e.g., binary-trees, fannkuch-redux, n-body]
   **Rationale**: Why this benchmark was selected
   **Url**: Link to Benchmarks Game description page

#### 4. Benchmark Selected from AreWeFastYet

For each relevant benchmark:

   **Benchmark Name**: [e.g., [benchmark-name]]
   **Rationale**: Why this benchmark was selected

#### 5. How to Execute Benchmarks

For each benchmark form both Benchmarks Game and AreWeFastYet:

   **Benchmark Name**: [e.g., fannkuch-redux]
   **File**: `[benchmark-name].[ext]`
   **Run Command**: `[project-specific command] [benchmark-name] [iterations]`
   **Test Parameter**: N=[value] (matches Benchmarks Game test size)
   **Verification**: [How to verify correctness, if applicable]

#### 6. Reference Language Performance from Benchmarks Game

For each benchmark from Benchmarks Game, provide cached baseline data:

   **Benchmark**: [benchmark-name] (N=[value])

   | Language     | Elapsed Time (s) | CPU Time (s)   | Memory (bytes) |Url |
   |--------------|------------------|----------------|----------------|----------------|
   | [Language A] | X.XX             | Y.YY–Z.ZZ      | M              | http://example.com/langA |
   | [Language B] | X.XX             | Y.YY–Z.ZZ      | M              | http://example.com/langB |
   | [Language C] | X.XX             | Y.YY–Z.ZZ      | M              | http://example.com/langC |

   **Source**: [URL to Benchmarks Game measurement page]
   **Expected Performance**: [Estimated range based on comparable languages]
      - Best Case: [Min] seconds
      - Expected Case: [Avg] seconds
      - Worst Case: [Max] seconds

**Output Format:**

- Saved markdown file: `BENCHMARK_BASELINE.md` (or similar name)
- Structured sections with clear headings
- Performance comparison tables with cached data
- Executable commands for all benchmarks
- Links to all Benchmarks Game sources
- Date-stamped baseline data for future reference

## Example Usage Scenarios: Initial Baseline Before Optimization

**User**: "Create a baseline for future performance analysis of my language implementation"

**Skill Actions**:

1. Analyzes language implementation characteristics
2. Identifies comparable languages from Benchmarks Game
3. Discovers available benchmarks on Benchmarks Game website 
4. Creates local benchmark files in project directory
5. Saves comprehensive baseline report to `BENCHMARK_BASELINE.md`

### Phase 1: Language Analysis

**Detected Language Characteristics**:

- Type System: Dynamic typing
- Execution Model: Bytecode VM with JIT compilation (Truffle/Graal)
- Platform: JVM-based (GraalVM Truffle)
- Paradigm: Object-oriented, imperative
- Complexity: Moderate (OO, closures, arrays)

### Phase 2: Comparable Language Identification

- Query Benchmarks Game for languages with similar characteristics

**Selected Comparable Languages**:

1. **Lua** - Most similar execution model (bytecode VM + JIT, dynamic typing)
   URL: https://benchmarksgame-team.pages.debian.net/benchmarksgame/measurements/lua.html

2. **Python 3** - Similar abstraction level and paradigm
   URL: https://benchmarksgame-team.pages.debian.net/benchmarksgame/measurements/python3.html

### Phase 3: Discover Benchmarks

- Query Benchmarks Game for available benchmarks

**Available Benchmarks on Benchmarks Game**:
- Found 10 benchmarks with implementations:
  - **Insignificant I/O**: fannkuch-redux, binary-trees, n-body, spectral-norm
  - **Significant I/O**: mandelbrot, fasta, k-nucleotide, reverse-complement
  - **Contentious**: pidigits, regex-redux

- Select 4 benchmarks for implementation:
  - 1. fannkuch-redux
  - 2. binary-trees
  - 3. n-body
  - 4. spectral-norm

### Phase 4: Create Benchmark Files

- Fetch reference implementations from Lua
- Fetch algorithm descriptions from Benchmarks Game
- Analyse current benchmark structure in project -> harness script
- Translate algorithms to target language syntax
- Create files: `fannkuch.lox`, `binarytrees.lox`, `nbody.lox`, `spectralnorm.lox`
- Run each benchmark to verify correctness, fix any issues (if needed query Benchmarks Game for clarification again)

### Phase 5: Discover AreWeFastYet Benchmarks

Query AreWeFastYet for available benchmarks and languages.

**Available Benchmarks on AreWeFastYet**:
- Bounce: Simulates a ball bouncing within a box
- List: Recursively creates and traverses lists
- Permute: Generates permutations of an array
- Queens: Solves the eight queens problem
- Sieve: Finds prime numbers based on the sieve of Eratosthenes
- Storage: Creates and verifies a tree of arrays to stress the garbage collector
- Towers: Solves the Towers of Hanoi game

**Available Languages on AreWeFastYet**:
- Python
- JavaScript (Node.js)
- Ruby
- Lua
- Java
- C#
- Go
- Rust

### Phase 6: Create AreWeFastYet Benchmark Files

- Fetch reference implementations from Lua
- Fetch algorithm descriptions from AreWeFastYet
- Analyse current benchmark structure in project -> harness script
- Translate algorithms to target language syntax
- Create files: `bounce.lox`, `list.lox`, `permute.lox`, `queens.lox`, `sieve.lox`, `storage.lox`, `towers.lox`
- Run each benchmark to verify correctness, fix any issues (if needed query AreWeFastYet for clarification again)

### Phase 7: Generate Baseline Report

- Compile all findings into `BENCHMARK_BASELINE.md`

```
# Benchmark Baseline Report for my Language

## Language Analysis

- Type System: Dynamic typing
- Execution Model: Bytecode VM with JIT compilation (Truffle/Graal)
- Platform: JVM-based (GraalVM Truffle)
- Paradigm: Object-oriented, imperative
- Complexity: Moderate (OO, closures, arrays)

## Comparable Languages Selected

### 1. Lua
   Rationale: Most similar execution model (bytecode VM + JIT, dynamic typing)
   Url: https://benchmarksgame-team.pages.debian.net/benchmarksgame/measurements/lua.html

### 2. Python 3
   Rationale: Similar abstraction level and paradigm
   Url: https://benchmarksgame-team.pages.debian.net/benchmarksgame/measurements/python3.html

## Benchmarks Selected from Benchmarks Game

### 1. fannkuch-redux

**Rationale**: Array-intensive computation
Url: https://benchmarksgame-team.pages.debian.net/benchmarksgame/description/fannkuchredux.html

### 2. binary-trees

**Rationale**: Object allocation and recursion
Url: https://benchmarksgame-team.pages.debian.net/benchmarksgame/description/binarytrees.html

### 3. n-body

**Rationale**: Numerical computation and floating-point operations
Url: https://benchmarksgame-team.pages.debian.net/benchmarksgame/description/nbody.html

### 4. spectral-norm

**Rationale**: Matrix operations and numerical methods
Url: https://benchmarksgame-team.pages.debian.net/benchmarksgame/description/spectralnorm.html

## Benchmarks Selected from AreWeFastYet

### 1. bounce
**Rationale**: Simulates a ball bouncing within a box

### 2. list
**Rationale**: Recursively creates and traverses lists

### 3. permute
**Rationale**: Generates permutations of an array

### 4. queens
**Rationale**: Solves the eight queens problem

### 5. sieve
**Rationale**: Finds prime numbers based on the sieve of Eratosthenes

### 6. storage
**Rationale**: Creates and verifies a tree of arrays to stress the garbage collector

### 7. towers
**Rationale**: Solves the Towers of Hanoi game

## How to Execute Benchmarks

### 1. fannkuch

**File**: `fannkuch.lox`
**Command**: `./lox harness.lox fannkuch 1 1`
**Test Parameter**: N=12
**Verification**: Compares output against known correct value for N=12

### 2. binarytrees

**File**: `binarytrees.lox`
**Command**: `./lox harness.lox binarytrees 1 1`
**Test Parameter**: N=21
**Verification**: Compares output against known correct value for N=21

### nbody

**File**: `nbody.lox`
**Command**: `./lox harness.lox nbody 1 1`
**Test Parameter**: N=50000000
**Verification**: Compares output against known correct value for N=50000000

### spectralnorm
**File**: `spectralnorm.lox`
**Command**: `./lox harness.lox spectralnorm 1 1`
**Test Parameter**: N=5500
**Verification**: Compares output against known correct value for N=5500

### bounce
**File**: `bounce.lox`
**Command**: `./lox harness.lox bounce 1000 5000`
**Test Parameter**: 1000 iterations, 5000 inner iterations
**Verification**: Compares output against known correct value for 1000 iterations and 5000 inner iterations

### list
**File**: `list.lox`
**Command**: `./lox harness.lox list 10 5000`
**Test Parameter**: 10 iterations, inner iteration 5000
**Verification**: Compares output against known correct value for 10 iterations and inner iteration 5000

### permute

**File**: `permute.lox`
**Command**: `./lox harness.lox permute 1 10`
**Test Parameter**: N=10
**Verification**: Compares output against known correct value for order after permutations.

### queens
**File**: `queens.lox`
**Command**: `./lox harness.lox queens 1 8`
**Test Parameter**: N=8
**Verification**: Compares output against known correct value for 8 queens problem

### sieve
**File**: `sieve.lox`
**Command**: `./lox harness.lox sieve 1 10000`
**Test Parameter**: N=10000
**Verification**: Compares output against known correct value for sieve of Eratosthenes

## Reference Language Performance

### Benchmark: fannkuch-redux (N=12)

| Language  | Elapsed Time | CPU Time      | Memory   | Url       |
|-----------|--------------|---------------|----------|------------|
| Lua       | 24.15s       | 24.17-25.73s  | 3.6 KB   | [Link](https://benchmarksgame-team.pages.debian.net/benchmarksgame/measurements/lua.html) |
| Python 3  | 311s         | 310-312s      | 8.2 KB   | [Link](https://benchmarksgame-team.pages.debian.net/benchmarksgame/measurements/python3.html) |

**Url**: https://benchmarksgame-team.pages.debian.net/benchmarksgame/measurements/fannkuchredux.html
**Expected Performance**: 50-300 seconds
- Best case: 50-100s (approaching Lua)
- Expected: 150-250s (typical for similar dynamic languages)
- Worst case: >300s (optimization issues)

### Benchmark: binary-trees (N=21)

| Language  | Elapsed Time | CPU Time      | Memory    | Url       |
|-----------|--------------|---------------|-----------|------------|
| Lua       | 47.56s       | 47.60-48.52s  | 2.0 MB    | [Link](https://benchmarksgame-team.pages.debian.net/benchmarksgame/measurements/lua.html) |
| Python 3  | 33-100s      | 95.12-101.34s | 52.3 MB   | [Link](https://benchmarksgame-team.pages.debian.net/benchmarksgame/measurements/python3.html) |

**Url**: https://benchmarksgame-team.pages.debian.net/benchmarksgame/measurements/binarytrees.html
**Expected Performance**: 40-100 seconds
- Best case: 40-50s (approaching Lua performance)
- Expected: 60-90s (typical for similar languages)
- Worst case: 100-150s (if optimization struggles)

### Benchmark: n-body (N=50000000)

| Language  | Elapsed Time | CPU Time      | Memory    | Url       |
|-----------|--------------|---------------|-----------|------------|
| Lua       | 12.34s       | 12.36-12.89s  | 1.5 MB    | [Link](https://benchmarksgame-team.pages.debian.net/benchmarksgame/measurements/lua.html) |
| Python 3  | 150-200s     | 180-210s      | 30.2 MB   | [Link](https://benchmarksgame-team.pages.debian.net/benchmarksgame/measurements/python3.html) |

**Url**: https://benchmarksgame-team.pages.debian.net/benchmarksgame/measurements/nbody.html
**Expected Performance**: 20-100 seconds
- Best case: 20-30s (approaching Lua)
- Expected: 50-80s (typical for similar dynamic languages)
- Worst case: >100s (optimization issues)

### Benchmark: spectral-norm (N=5500)

| Language  | Elapsed Time | CPU Time      | Memory    | Url       |
|-----------|--------------|---------------|-----------|------------|
| Lua       | 8.76s        | 8.78-9.12s    | 1.2 MB    | [Link](https://benchmarksgame-team.pages.debian.net/benchmarksgame/measurements/lua.html) |
| Python 3  | 90-120s      | 95-130s       | 20.5 MB   | [Link](https://benchmarksgame-team.pages.debian.net/benchmarksgame/measurements/python3.html) |

**Url**: https://benchmarksgame-team.pages.debian.net/benchmarksgame/measurements/spectralnorm.html
**Expected Performance**: 10-50 seconds
- Best case: 10-15s (approaching Lua)
- Expected: 20-40s (typical for similar dynamic languages)
- Worst case: >50s (optimization issues)

```

## Integration with Other Skills

This skill works best in combination with:

### Before This Skill

- **None required** - This is typically the first skill to use

### Workflow Example

```
1. [benchmark-baseline] → Establish expected performance range (50-80s) with both Benchmarks Game and AreWeFastYet data
2. [Run benchmark]       → Actually get 120s (slower than expected)
3. [Profile]             → Identify hot code paths primarily with AreWeFastYet benchmarks
4. [Diagnose]            → Find optimization barriers
5. [Fix code]            → Address identified issues
6. [Run benchmark]       → Now get 75s (within expected range!) primarily with AreWeFastYet benchmarks
7. [Verify]              → Confirm optimizations are working
8. [Deep analysis]       → Understand remaining opportunities
9. [Optimize further]    → Improve implementation
10. [Run benchmark]      → Achieve 50s (approaching best comparable!)
```

**Important** Use the AreWeFastYet benchmarks for profiling and optimization work after establishing the baseline with this skill. Use the Benchmarks Game data for broader context and comparison with other languages.

## Important Notes

### Benchmark Game vs. AreWeFastYet

- The Computer Language Benchmarks Game provides a wide range of benchmarks across many languages for general performance comparison
- AreWeFastYet focuses on micro-benchmarks for language performance analysis, often with more detailed measurements
- Use the benchmark game data for broader context and AreWeFastYet for specific micro-benchmark insights
- Use the Benchmarks Game data to compare with other languages
- Don't use AreWeFastYet to compare with other languages
- Use AreWeFastYet benchmarks to analyze specific performance characteristics of your own language implementation
- Benchmark Game runs longer, while AreWeFastYet focuses on quick micro-benchmarks
- AreWeFastYet benchmarks specifically designed for language implementation analysis

### Skill Workflow

1. **Run this skill FIRST** before any performance analysis
2. Skill analyzes language characteristics automatically
3. Skill fetches comparison data from Benchmarks Game and AreWeFastYet
4. Skill creates local benchmark implementations (if needed) for Benchmarks Game and AreWeFastYet
5. Run newly created benchmarks to collect actual performance data
6. Use baseline report to interpret benchmark results
7. Follow up with profiling skills if performance is below expectations

### Understanding Baselines
- Benchmarks Game data is from isolated test environment
- AreWeFastYet data may differ in methodology and scope
- Results show what's possible, not guaranteed performance
- Multiple implementations per language show variance
- Focus on performance ranges, not single data points

### Limitations
- Only retrieves data from Benchmarks Game website
- Cannot predict exact performance for your implementation
- Micro-benchmarks don't represent real-world applications
- Some language features may not have direct equivalents

## Summary

The **Establish Benchmark Baseline** skill analyzes your language implementation's characteristics, discovers benchmarks from the Computer Language Benchmarks Game and AreWeFastYet, creates local implementations of those benchmarks translated to your language, and generates a comprehensive baseline report. This is a web-first approach: it finds industry-standard benchmarks online and brings them into your project as:

1. **Benchmark files** - Executable implementations in your language
2. **Baseline report** (`BENCHMARK_BASELINE.md`) - Saved documentation containing:
   - Execution instructions for all benchmarks
   - Cached performance data from comparable languages
   - Performance expectations and interpretation guidance
   - Links to Benchmarks Game sources (date-stamped for reference)
   - AreWeFastYet benchmark list (date-stamped for reference)

This gives you both the actual benchmarks to run and the saved baseline data to interpret results without re-fetching from the web. Always use this skill before detailed performance analysis.
