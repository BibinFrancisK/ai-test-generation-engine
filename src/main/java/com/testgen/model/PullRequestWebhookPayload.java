package com.testgen.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PullRequestWebhookPayload(
        String action,
        Repository repository,
        @JsonProperty("pull_request") PullRequest pullRequest
) {

    public record Repository(Owner owner, String name, @JsonProperty("full_name") String fullName) {}

    public record Owner(String login) {}

    public record PullRequest(int number, Head head, @JsonProperty("diff_url") String diffUrl) {}

    public record Head(String ref, String sha) {}
}
