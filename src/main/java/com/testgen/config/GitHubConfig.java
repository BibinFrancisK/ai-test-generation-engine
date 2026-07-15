package com.testgen.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(GitHubProperties.class)
public class GitHubConfig {

    @Bean
    RestClient gitHubRestClient(GitHubProperties props) {
        return RestClient.builder()
                .baseUrl(props.apiBaseUrl())
                .defaultHeader("Accept", "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                .defaultHeader("Authorization", "Bearer " + props.token())
                .build();
    }
}
