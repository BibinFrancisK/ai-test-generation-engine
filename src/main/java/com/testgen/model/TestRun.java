package com.testgen.model;

public record TestRun(
        String testRunId,
        String repositoryId,
        String pullRequestId,
        String generatedTestCode,
        String validationStatus,
        String createdAt,
        String s3ArtifactKey,
        String testPrUrl,
        String schemaVersion
) {}
