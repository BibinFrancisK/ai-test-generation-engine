# ADR-005 — Source Analysis: JavaParser

## Context

The service needs to parse Java source files to:
1. Extract method signatures (`ChangedMethod`) from changed classes — class name, method name, parameter types, return type, annotations, line range
2. Detect per-repository test conventions (`ProjectConventions`) from existing test files — test framework (JUnit 5 / JUnit 4), mock library (Mockito / EasyMock / none), base test class

Source is available as raw `String` content fetched from the GitHub Contents API — no compiled bytecode, no local filesystem, no full classpath available.

Options evaluated: JavaParser, Spoon, byte-buddy, manual regex parsing.

## Decision

**`com.github.javaparser:javaparser-core`** (symbol solver not required).

- `StaticJavaParser.parse(String source)` produces a `CompilationUnit` from a raw source string
- `VoidVisitorAdapter<Void>` walks `MethodDeclaration` nodes to extract signatures
- Import declarations and `@ExtendWith` annotations are sufficient to detect JUnit and Mockito — no type resolution needed

## Consequences

- Works on raw source strings — no classpath, no compiled output, no filesystem access required
- Mature library (10+ years), direct Maven dependency (`javaparser-core` only — no symbol solver needed)
- Simpler than Spoon, which requires a full source classpath for type resolution
- Simpler than byte-buddy, which operates on bytecode rather than source
- Manual regex rejected: fragile against formatting variations, generics, annotations with parameters
- No type resolution means we cannot follow method calls across files — this is acceptable because we only need method signatures and annotations, not full type graphs
