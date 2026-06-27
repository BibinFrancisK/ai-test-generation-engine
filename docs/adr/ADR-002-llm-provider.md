# ADR-002 — LLM Provider: Anthropic Claude + Sealed Interface

## Context

The service needs to call an LLM to generate JUnit 5 tests from a `GenerationContext`. Requirements:
- Provider must be swappable without code changes — only a config change
- Cost must be predictable and bounded: $10/month hard cap
- Tests must never make real LLM calls — zero cost during `mvn verify`
- Java code generation quality is the primary selection criterion

Candidates evaluated: Anthropic Claude (via LangChain4j), OpenAI GPT-4o, Google Gemini.

## Decision

`LlmProvider` is a **Java sealed interface** permitting three implementations:

```java
public sealed interface LlmProvider
    permits AnthropicLlmProvider, OpenAiLlmProvider, NoopProvider { ... }
```

- **`AnthropicLlmProvider`** — active in production; uses LangChain4j `AnthropicChatModel`
- **`OpenAiLlmProvider`** — wired by config; one-line swap from Anthropic
- **`NoopProvider`** — returns a hardcoded stub test; used in all tests and local dev without an API key

Active provider selected by `testgen.llm.provider` in `application.yml`. `max_tokens=1500` on every LLM call. Hard spend cap set in the Anthropic console.

## Consequences

- Claude leads on Java code generation quality; LangChain4j has first-class Anthropic support with a typed API
- OpenAI is a one-line config swap, not a code change — demonstrates the value of the sealed interface
- `NoopProvider` enforces zero LLM spend during `mvn verify`; enforced in `src/test/resources/application-test.yml`
- Every `switch` on `LlmProvider` must be exhaustive — the compiler enforces this with sealed types
- Gemini rejected: LangChain4j Gemini support is less mature at time of writing
