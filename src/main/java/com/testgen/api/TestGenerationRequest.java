package com.testgen.api;

public record TestGenerationRequest(
        String owner,
        String repo,
        String ref,
        String repositoryId,
        String pullRequestId,
        String diffContent,
        String changedFilePath,
        String sourceBranchSha
) {}
