package com.testgen.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "github.app")
public record GitHubAppProperties(String id, String privateKey, String installationId) {
}
