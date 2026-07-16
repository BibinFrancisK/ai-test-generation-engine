package com.testgen.github;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.testgen.api.TestGenerationRequest;
import com.testgen.model.PullRequestWebhookPayload;
import com.testgen.orchestration.TestGenerationOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Set;

@Component
public class GitHubWebhookHandler {

    private static final Logger log = LoggerFactory.getLogger(GitHubWebhookHandler.class);
    private static final String PULL_REQUEST_EVENT = "pull_request";
    private static final Set<String> HANDLED_ACTIONS = Set.of("opened", "synchronize");

    private final WebhookSignatureValidator signatureValidator;
    private final ObjectMapper objectMapper;
    private final GitHubAppAuthenticator gitHubAppAuthenticator;
    private final RestClient gitHubRestClient;
    private final TestGenerationOrchestrator orchestrator;

    public GitHubWebhookHandler(WebhookSignatureValidator signatureValidator,
                                 ObjectMapper objectMapper,
                                 GitHubAppAuthenticator gitHubAppAuthenticator,
                                 RestClient gitHubRestClient,
                                 TestGenerationOrchestrator orchestrator) {
        this.signatureValidator = signatureValidator;
        this.objectMapper = objectMapper;
        this.gitHubAppAuthenticator = gitHubAppAuthenticator;
        this.gitHubRestClient = gitHubRestClient;
        this.orchestrator = orchestrator;
    }

    public void handle(byte[] rawBody, String signatureHeader, String eventType, String deliveryId) {
        MDC.put("deliveryId", deliveryId == null ? "unknown" : deliveryId);
        try {
            signatureValidator.validate(rawBody, signatureHeader);

            if (!PULL_REQUEST_EVENT.equals(eventType)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unrecognised event type: " + eventType);
            }

            PullRequestWebhookPayload payload = parsePayload(rawBody);

            if (!HANDLED_ACTIONS.contains(payload.action())) {
                log.info("Ignoring pull_request action={}", payload.action());
                return;
            }

            dispatchGeneration(payload);
        } finally {
            MDC.remove("deliveryId");
        }
    }

    private void dispatchGeneration(PullRequestWebhookPayload payload) {
        String owner = payload.repository().owner().login();
        String repo = payload.repository().name();
        String sourceBranch = payload.pullRequest().head().ref();
        String sourceBranchSha = payload.pullRequest().head().sha();
        String repositoryId = payload.repository().fullName();
        String diffContent = fetchDiff(payload.pullRequest().diffUrl());

        TestGenerationRequest request = new TestGenerationRequest(
                owner, repo, sourceBranch, repositoryId, String.valueOf(payload.pullRequest().number()),
                diffContent, null, sourceBranchSha);

        orchestrator.orchestrate(request);
    }

    private PullRequestWebhookPayload parsePayload(byte[] rawBody) {
        try {
            return objectMapper.readValue(rawBody, PullRequestWebhookPayload.class);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Malformed webhook payload", e);
        }
    }

    private String fetchDiff(String diffUrl) {
        String diff = gitHubRestClient.get()
                .uri(diffUrl)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + gitHubAppAuthenticator.getInstallationToken())
                .retrieve()
                .body(String.class);

        if (diff == null) {
            throw new IllegalStateException("GitHub diff response was empty for " + diffUrl);
        }
        return diff;
    }
}
