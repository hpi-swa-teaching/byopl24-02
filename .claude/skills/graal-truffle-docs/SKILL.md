---
name: Graal and Truffle Documentation Lookup
description: Fetches up-to-date information from official GraalVM, Truffle, and Graal compiler documentation to answer technical questions and provide guidance
---

# Skill: Graal and Truffle Documentation Lookup

This skill searches and retrieves information from official GraalVM, Truffle, and Graal compiler documentation sources to answer technical questions, provide API guidance, and help with implementation decisions.

## What This Skill Does

1. **Searches Documentation**: Queries official documentation sources for relevant information
2. **Provides Context**: Returns detailed technical information with references to source documentation
3. **Answers Questions**: Helps with:
   - Truffle API usage and best practices
   - GraalVM compiler options and flags
   - Performance optimization techniques
   - Truffle DSL annotations and patterns
   - Bytecode DSL usage
   - Specialization strategies
   - Interoperability features

## Documentation Sources

This skill has access to these authoritative sources:

### Primary Sources
- **GraalVM Official Docs**: https://docs.oracle.com/en/graalvm/jdk/20/docs
  - Language implementation guides
  - Tools and utilities documentation
  - Performance optimization guides
  - Security and deployment information

- **GraalVM Tools Javadoc**: https://www.graalvm.org/tools/javadoc/
  - Complete API reference
  - Package documentation
  - Class and method details
  - Code examples

- **Graal GitHub Repository**: https://github.com/oracle/graal
  - Source code and examples
  - Truffle framework documentation
  - Optimization guides
  - Migration guides
  - Issue discussions and solutions

### Key Documentation Areas

**Truffle Framework**:
- `/truffle/docs/` - Core Truffle documentation
  - Language implementation tutorial
  - DSL (Domain Specific Language) guide
  - Profiling and debugging
  - AOT (Ahead-of-Time) compilation
  - Interoperability

**Compiler Options**:
- Compilation flags and tuning
- Diagnostic and tracing options
- Performance optimization settings
- Debug and development options

**API Reference**:
- `com.oracle.truffle.api.*` packages
- Node implementations
- Frame management
- Assumptions and invalidation
- Interop protocols

## When to Use This Skill

Use this skill when you need to:

### API and Implementation Questions
- "How do I use Truffle DSL specializations?"
- "What's the correct way to implement a Truffle node?"
- "How do I use the bytecode DSL?"
- "What are the available interop messages?"

### Performance and Optimization
- "What compiler options can improve performance?"
- "How does partial evaluation work in Truffle?"
- "What are best practices for PE (Partial Evaluation)?"
- "How can I avoid deoptimization?"

### Debugging and Diagnostics
- "What do these compilation warnings mean?"
- "How do I trace compilation decisions?"
- "What profiling tools are available?"
- "How do I debug PE failures?"

### Migration and Updates
- "How do I migrate from Truffle API version X to Y?"
- "What are the new features in GraalVM 24.x?"
- "How do I update deprecated API usage?"

## How the Skill Works

### 1. Query Analysis
The skill analyzes your question to determine:
- The specific topic area (API, performance, debugging, etc.)
- Relevant documentation sources to search
- Key terms and concepts to look for

### 2. Documentation Search
The skill searches documentation using:
- **WebFetch**: For direct documentation page retrieval
- **WebSearch**: For finding relevant pages across multiple sources
- **Pattern matching**: To locate specific API classes, methods, or concepts

### 3. Information Synthesis
The skill:
- Extracts relevant information from documentation
- Provides code examples where available
- Includes links to authoritative sources
- Highlights important caveats or best practices

### 4. Response Format
Responses include:
- Direct answer to your question
- Code examples (when applicable)
- Links to detailed documentation
- Related topics for further exploration

## Example Usage

### Example 1: API Question
**User**: "How do I create a specialization that handles both long and double types?"

**Skill Actions**:
1. Search Truffle DSL documentation for specialization patterns
2. Look for examples in GitHub repository
3. Find relevant Javadoc for `@Specialization` annotation
4. Provide code example with explanation

**Response Format**:
```
In Truffle DSL, you can create separate specializations for each type:

[Code example from documentation]

Key points:
- Use @Specialization with type guards
- Order matters (specific before general)
- Consider using @Cached for type profiles

Reference: [Link to Truffle DSL guide]
```

### Example 2: Compiler Options
**User**: "What flags should I use to debug why a function isn't being compiled?"

**Skill Actions**:
1. Search for compilation tracing options
2. Find relevant flags in compiler documentation
3. Look for examples in GitHub issues
4. Provide recommended flag combinations

**Response Format**:
```
Use these flags for debugging compilation:
- --engine.TraceCompilation
- --engine.TraceCompilationDetails
- --engine.CompilationFailureAction=Print

[Explanation of each flag and output format]

Reference: [Link to compiler options documentation]
```

### Example 3: Best Practices
**User**: "What are best practices for avoiding boxing in Truffle?"

**Skill Actions**:
1. Search optimization guides
2. Find boxing-related documentation
3. Look for code examples
4. Extract best practices

**Response Format**:
```
Best practices to avoid boxing in Truffle:

1. Use specialized frame slots
2. Leverage Truffle DSL type specializations
3. Avoid Object[] arrays for primitives
4. Use @Cached for type stability

[Detailed explanation with examples]

References:
- [Optimization guide link]
- [Frame API documentation]
```

## Advanced Usage

### Searching Specific Sources

You can request searches in specific documentation areas:

**GitHub Repository Search**:
- "Search the Graal GitHub for examples of bytecode DSL usage"
- "Find Truffle DSL examples in the SimpleLanguage"

**API Documentation**:
- "Look up the Frame API in the Javadoc"
- "Find documentation for TruffleInstrument"

**Official Guides**:
- "Find the language implementation tutorial"
- "Get the profiling guide from GraalVM docs"

### Version-Specific Information

The skill can search for version-specific information:
- "What changed in Truffle API for GraalVM 24.0?"
- "Show me the migration guide for GraalVM 23.x to 24.x"

### Code Example Searches

Request specific code examples:
- "Show me a complete example of implementing a Truffle language"
- "Find examples of using @Cached in specializations"
- "Get SimpleLanguage examples of array implementation"

## Common Documentation Paths

The skill knows these common documentation locations:

### Truffle Core Documentation
- `/truffle/docs/LanguageTutorial.md` - Language implementation tutorial
- `/truffle/docs/DSL.md` - Domain Specific Language guide
- `/truffle/docs/Profiling.md` - Profiling and optimization
- `/truffle/docs/Optimizing.md` - Optimization guide
- `/truffle/docs/BytecodeDSL.md` - Bytecode DSL documentation

### Compiler Documentation
- `/compiler/docs/` - Compiler architecture and options
- `/sdk/docs/` - SDK and embedding documentation

### Example Implementations
- `/truffle/src/com.oracle.truffle.sl/` - SimpleLanguage reference implementation
- `/truffle/src/com.oracle.truffle.sl.test/` - SimpleLanguage tests

## Best Practices for Using This Skill

1. **Be Specific**: Include relevant context about your implementation
   - ✓ "How do I implement array indexing with bounds checking in Truffle DSL?"
   - ✗ "How do arrays work?"

2. **Mention Your Version**: If relevant, specify your GraalVM version
   - "Using GraalVM 24.2.0, how do I..."

3. **Include Error Messages**: If debugging, include relevant error text
   - "I'm getting 'PartialEvaluationFailure' when compiling..."

4. **Reference Existing Code**: Mention what you've tried or what patterns you're using
   - "I'm using bytecode DSL like in SimpleLanguage, but need to add..."

5. **Ask Follow-ups**: If the initial answer isn't complete, ask for more detail
   - "Can you show me a more complex example?"
   - "What about the case where...?"

## Complementary Skills

This skill works well with other skills:

- **trace-performance-warnings-analyze**: Get documentation about specific warnings
- **analyze-compiler-graph**: Understand compiler decisions with reference documentation
- **cpu-sampler-analyze**: Learn about profiling options and interpretation

## Limitations

This skill:
- Fetches publicly available documentation (requires internet access)
- May not have access to pre-release or internal documentation
- Provides information as of the documentation's last update
- Cannot access private repositories or internal Oracle documentation

## Output Format

Responses will include:

1. **Direct Answer**: Clear, actionable information
2. **Code Examples**: When applicable and available
3. **Source References**: Links to authoritative documentation
4. **Related Topics**: For further exploration
5. **Version Notes**: If information is version-specific

Example:
```
## Answer
[Direct answer to your question]

## Code Example
```java
[Relevant code snippet]
```

## Explanation
[Detailed explanation of concepts]

## References
- [Official docs link]
- [GitHub link]
- [Javadoc link]

## See Also
- [Related topic 1]
- [Related topic 2]
```

## Implementation Notes

This skill:
- Uses WebFetch for direct documentation page access
- Uses WebSearch for finding relevant pages
- Prioritizes official documentation over third-party sources
- Cites sources for all information
- Provides links for deeper exploration
- Focuses on GraalVM 24.x and current Truffle API unless specified otherwise
