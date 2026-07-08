package com.testgen.config;

import com.testgen.generation.AnthropicLlmProvider;
import com.testgen.generation.LlmProvider;
import com.testgen.generation.NoopProvider;
import com.testgen.generation.OpenAiLlmProvider;
import com.testgen.generation.TestGenerationPromptBuilder;
import com.testgen.generation.TestGenerationService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.testgen.util.Constants.ANTHROPIC;
import static com.testgen.util.Constants.NOOP;
import static com.testgen.util.Constants.OPENAI;

@Configuration
@EnableConfigurationProperties(LlmConfig.class)
public class AppConfig {

    @Bean
    LlmProvider llmProvider(LlmConfig config) {
        return switch (config.provider()) {
            case ANTHROPIC -> new AnthropicLlmProvider(config.apiKey(), config.model(), config.maxTokens());
            case OPENAI -> new OpenAiLlmProvider();
            case NOOP -> new NoopProvider();
            default -> throw new IllegalArgumentException(
                    "Unknown LLM provider: " + config.provider() + ". Valid values: "
                            + ANTHROPIC + ", " + OPENAI + ", " + NOOP);
        };
    }

    @Bean
    TestGenerationPromptBuilder testGenerationPromptBuilder() {
        return new TestGenerationPromptBuilder();
    }

    @Bean
    TestGenerationService testGenerationService(LlmProvider llmProvider, TestGenerationPromptBuilder promptBuilder) {
        return new TestGenerationService(llmProvider, promptBuilder);
    }
}
