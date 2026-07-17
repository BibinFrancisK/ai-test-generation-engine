package com.testgen.github;

import com.testgen.api.TestGenerationResponse;
import com.testgen.orchestration.TestGenerationOrchestrator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;

import static com.testgen.util.Constants.HMAC_ALGORITHM;
import static com.testgen.util.Constants.SIGNATURE_PREFIX;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Tag("integration")
@ActiveProfiles("test")
class GitHubWebhookHandlerIT {

    private static final String WEBHOOK_SECRET = "test-webhook-secret-not-a-real-secret";
    private static final String DIFF_URL = "https://github.com/sample-owner/sample-repo/pull/42.diff";
    private static final String DIFF_CONTENT = "diff --git a/Foo.java b/Foo.java";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private MockRestServiceServer mockGitHubServer;

    @MockitoBean
    private TestGenerationOrchestrator orchestrator;

    @MockitoBean
    private GitHubAppAuthenticator gitHubAppAuthenticator;

    @BeforeEach
    void resetMockServer() {
        mockGitHubServer.reset();
    }

    @Test
    void validSignatureDispatchesGenerationWithFieldsFromPayload() throws Exception {
        byte[] body = loadFixture();
        when(gitHubAppAuthenticator.getInstallationToken()).thenReturn("installation-token");
        mockGitHubServer.expect(requestTo(DIFF_URL))
                .andRespond(withSuccess(DIFF_CONTENT, MediaType.TEXT_PLAIN));
        when(orchestrator.orchestrate(any())).thenReturn(dummyResponse());

        ResponseEntity<Void> response = post(body, validSignature(body), "pull_request");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(orchestrator).orchestrate(argThat(request ->
                request.owner().equals("sample-owner")
                        && request.repo().equals("sample-repo")
                        && request.ref().equals("feature/sample-change")
                        && request.repositoryId().equals("sample-owner/sample-repo")
                        && request.pullRequestId().equals("42")
                        && request.sourceBranchSha().equals("a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0")
                        && request.diffContent().equals(DIFF_CONTENT)));
        mockGitHubServer.verify();
    }

    @Test
    void missingOrBadSignatureIsRejectedAndNeverDispatched() throws Exception {
        byte[] body = loadFixture();

        ResponseEntity<Void> response = post(body, "sha256=not-a-real-signature", "pull_request");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(orchestrator);
        verifyNoInteractions(gitHubAppAuthenticator);
    }

    private ResponseEntity<Void> post(byte[] body, String signature, String eventType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Hub-Signature-256", signature);
        headers.set("X-GitHub-Event", eventType);
        headers.set("X-GitHub-Delivery", "delivery-it-1");

        return restTemplate.postForEntity("/webhook/github", new HttpEntity<>(body, headers), Void.class);
    }

    private String validSignature(byte[] body) throws Exception {
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        mac.init(new SecretKeySpec(WEBHOOK_SECRET.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
        return SIGNATURE_PREFIX + HexFormat.of().formatHex(mac.doFinal(body));
    }

    private byte[] loadFixture() throws Exception {
        return Files.readAllBytes(new ClassPathResource("fixtures/sample-pr-payload.json").getFile().toPath());
    }

    private TestGenerationResponse dummyResponse() {
        return new TestGenerationResponse(
                "run-it-1", "class FooTest {}", "SUCCESS", List.of(), "s3://bucket/key", null, Instant.now());
    }

    // GitHubConfig's real gitHubRestClient bean makes real HTTP calls; this test-only config
    // replaces it with one bound to a MockRestServiceServer so fetchDiff() never leaves the JVM.
    @TestConfiguration
    static class MockGitHubRestClientConfig {

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
        RestClient gitHubRestClient(RestClient.Builder gitHubRestClientBuilder, MockRestServiceServer mockGitHubServer) {
            return gitHubRestClientBuilder.build();
        }
    }
}
