package com.testgen.generation;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.assertj.core.api.Assertions.assertThat;

// Runs under Surefire (matches its *Test.java default include, unaffected by the *IT.java
// exclude) but its single @Test method is gated by @EnabledIfEnvironmentVariable below, so it
// never actually executes — and never makes a real Anthropic call — unless LLM_API_KEY is set.
// The name deliberately does NOT match Failsafe's *IT.java / IT*.java / *ITCase.java includes,
// so renaming it to *IT.java would additionally wire it into CI's integration-test phase, where
// LLM_API_KEY is never set anyway, but would be a needless duplicate execution path. Keep it as
// LlmProviderIntegrationTest.
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
