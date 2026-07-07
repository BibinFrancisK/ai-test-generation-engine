package com.testgen.generation;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.anthropic.AnthropicChatModel;

import static com.testgen.util.Constants.HARD_MAX_TOKENS;

public final class AnthropicLlmProvider implements LlmProvider {

    private final AnthropicChatModel model;

    public AnthropicLlmProvider(String apiKey, String modelName, int maxTokens) {
        this.model = AnthropicChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .maxTokens(Math.min(maxTokens, HARD_MAX_TOKENS))
                .build();
    }

    @Override
    public String generate(String systemPrompt, String userPrompt) {
        return model.chat(
                SystemMessage.from(systemPrompt),
                UserMessage.from(userPrompt)
        ).aiMessage().text();
    }
}
