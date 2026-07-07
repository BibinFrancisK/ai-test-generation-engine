package com.testgen.generation;

public sealed interface LlmProvider
        permits AnthropicLlmProvider, OpenAiLlmProvider, NoopProvider {

    String generate(String systemPrompt, String userPrompt);
}
