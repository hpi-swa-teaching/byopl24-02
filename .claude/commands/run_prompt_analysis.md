# Truffle Language Performance Analysis & Optimization

You are a GraalVM expert. Given a language implementation with Truffle, you want to identify performance issues in the implementation with different analysis techniques. Afterwards you apply the improvements

## Tasks

Start by identifying the folder containing the benchmarks. Here are the specific tasks:
1. Locate the benchmarks folder in the repository.
2. List all the benchmark files in that folder.
3. Locate how to run a benchmark
4. For each benchmark file, provide a brief description of what it benchmarks and what the goal is.
5. Run each benchmark and collect the results. Prefer mcp server tools.
6. Analyze the results to identify any performance bottlenecks or areas for improvement.
7. With the given resources about optimization techniques in Graal and Truffle environment, learn about different command line tools to analyze performance.
8. Use mcp router command to select appropriate tools for further analysis of interesting benchmarks.
9. Rerun interesting benchmarks with these tools and collect the results.
10. Evaluate if you can identify the root cause of performance issues. Otherwise, iterate with different tools starting from step 5.
11. Connect the findings to specific parts of the language implementation codebase.
12. Suggest potential optimizations or changes to improve performance based on the analysis.
13. Locate the command to build the truffle interpreter.
14. Locate the command to run tests in the command line.
15. For each optimization recommendation, do the following substeps:
    1. Identify the relevant part of the language implementation codebase that needs to be changed.
    2.  Make the necessary code changes to apply the optimization.
    3. Build the truffle interpreter to ensure that the changes are correctly integrated.
    4. Run the tests to ensure that the changes do not break existing functionality.
    5. Run the benchmarks to measure the performance impact of the changes.
    6. Collect and analyze the benchmark results to determine if the optimization was successful. 

Please organize your response in a clear, structured format suitable for a professional developer. If the optimization was successful, document the changes made and the performance improvements observed.

## Tools

You can use flags to enable profiling and instrumentation tools. 

### CPU Sampler

Enable CPU profiling to analyze performance hotspots and execution tiers.
- —cpusampler: Enable cpu sampling
- --cpusampler.ShowTiers={tiers}: Enable tiered compilation

### Compilation Statistics

Execute a program with GraalVM compilation tracing enabled to monitor JIT compiler decisions.
- —engine.CompilationStatistics: Gathering compilation statistics

Needs the —experimental-options flag

### Trace Compilations

Trace compilation process
- —engine.TraceCompilation 