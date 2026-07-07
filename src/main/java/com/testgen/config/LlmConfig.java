package com.testgen.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "testgen.llm")
public record LlmConfig(String provider, String apiKey, String model, int maxTokens) {
}
