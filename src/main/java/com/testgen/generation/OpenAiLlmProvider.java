package com.testgen.generation;

// TODO Day 6+: wire OpenAiChatModel from langchain4j-open-ai
public final class OpenAiLlmProvider implements LlmProvider {

    @Override
    public String generate(String systemPrompt, String userPrompt) {
        throw new UnsupportedOperationException("OpenAI provider not yet wired — Day 5 stub");
    }
}
