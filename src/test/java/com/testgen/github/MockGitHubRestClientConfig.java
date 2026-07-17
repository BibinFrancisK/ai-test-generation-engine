package com.testgen.github;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

// GitHubConfig's real gitHubRestClient bean makes real HTTP calls; this test-only config
// replaces it with one bound to a MockRestServiceServer so fetchDiff() never leaves the JVM.
@TestConfiguration
public class MockGitHubRestClientConfig {

    @Bean
    RestClient.Builder gitHubRestClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    MockRestServiceServer mockGitHubServer(RestClient.Builder gitHubRestClientBuilder) {
        return MockRestServiceServer.bindTo(gitHubRestClientBuilder).build();
    }

    @Bean
    @Primary
    RestClient mockedGitHubRestClient(RestClient.Builder gitHubRestClientBuilder, MockRestServiceServer mockGitHubServer) {
        return gitHubRestClientBuilder.build();
    }
}
