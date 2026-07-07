package com.testgen.generation;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
class LlmProviderIntegrationTest {

    @Test
    @EnabledIfEnvironmentVariable(named = "LLM_API_KEY", matches = ".+")
    void pingReturnsResponse() {
        String apiKey = System.getenv("LLM_API_KEY");
        var provider = new AnthropicLlmProvider(apiKey, "claude-haiku-4-5-20251001", 100);

        String result = provider.generate(
                "You are a test assistant. Return only the word PING.",
                "PING"
        );

        assertThat(result).containsIgnoringCase("ping");
    }
}
