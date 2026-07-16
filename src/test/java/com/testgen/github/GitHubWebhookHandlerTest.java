package com.testgen.github;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.testgen.orchestration.TestGenerationOrchestrator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(MockitoExtension.class)
class GitHubWebhookHandlerTest {

    private static final String DIFF_URL = "http://localhost/owner/repo/pull/42.diff";

    @Mock
    private WebhookSignatureValidator signatureValidator;
    @Mock
    private GitHubAppAuthenticator gitHubAppAuthenticator;
    @Mock
    private TestGenerationOrchestrator orchestrator;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void invalidSignatureIsRejectedBeforeDeserializingPayload() {
        byte[] body = "not even valid json".getBytes(StandardCharsets.UTF_8);
        doThrow(new WebhookAuthException()).when(signatureValidator).validate(body, "bad-signature");

        GitHubWebhookHandler handler = new GitHubWebhookHandler(
                signatureValidator, objectMapper, gitHubAppAuthenticator, RestClient.builder().build(), orchestrator);

        assertThatThrownBy(() -> handler.handle(body, "bad-signature", "pull_request", "delivery-1"))
                .isInstanceOf(WebhookAuthException.class);

        verifyNoInteractions(orchestrator);
        verifyNoInteractions(gitHubAppAuthenticator);
    }

    @Test
    void unrecognisedEventTypeReturns400() {
        byte[] body = samplePayload("opened");

        GitHubWebhookHandler handler = new GitHubWebhookHandler(
                signatureValidator, objectMapper, gitHubAppAuthenticator, RestClient.builder().build(), orchestrator);

        assertThatThrownBy(() -> handler.handle(body, "sig", "push", "delivery-2"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Unrecognised event type");

        verifyNoInteractions(orchestrator);
    }

    @Test
    void ignoresActionsOtherThanOpenedOrSynchronize() {
        byte[] body = samplePayload("closed");

        GitHubWebhookHandler handler = new GitHubWebhookHandler(
                signatureValidator, objectMapper, gitHubAppAuthenticator, RestClient.builder().build(), orchestrator);

        handler.handle(body, "sig", "pull_request", "delivery-3");

        verifyNoInteractions(orchestrator);
    }

    @Test
    void dispatchesGenerationWithFieldsExtractedFromPayload() {
        byte[] body = samplePayload("opened");

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer mockServer = MockRestServiceServer.bindTo(builder).build();
        when(gitHubAppAuthenticator.getInstallationToken()).thenReturn("installation-token");
        mockServer.expect(requestTo(DIFF_URL))
                .andRespond(withSuccess("diff --git a/Foo.java b/Foo.java", MediaType.TEXT_PLAIN));

        GitHubWebhookHandler handler = new GitHubWebhookHandler(
                signatureValidator, objectMapper, gitHubAppAuthenticator, builder.build(), orchestrator);

        handler.handle(body, "sig", "pull_request", "delivery-4");

        verify(orchestrator).orchestrate(argThat(request ->
                request.owner().equals("owner")
                        && request.repo().equals("repo")
                        && request.ref().equals("feature/x")
                        && request.repositoryId().equals("owner/repo")
                        && request.pullRequestId().equals("42")
                        && request.sourceBranchSha().equals("abc123")
                        && request.changedFilePath() == null
                        && request.diffContent().equals("diff --git a/Foo.java b/Foo.java")));
        mockServer.verify();
    }

    private byte[] samplePayload(String action) {
        String json = """
                {
                  "action": "%s",
                  "repository": {
                    "name": "repo",
                    "full_name": "owner/repo",
                    "owner": { "login": "owner" }
                  },
                  "pull_request": {
                    "number": 42,
                    "head": { "ref": "feature/x", "sha": "abc123" },
                    "diff_url": "%s"
                  }
                }
                """.formatted(action, DIFF_URL);
        return json.getBytes(StandardCharsets.UTF_8);
    }
}
