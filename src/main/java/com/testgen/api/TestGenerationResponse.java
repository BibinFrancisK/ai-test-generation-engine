package com.testgen.api;

import java.time.Instant;
import java.util.List;

public record TestGenerationResponse(
        String testRunId,
        String generatedTestCode,
        String validationStatus,
        List<String> validationErrors,
        String s3ArtifactUrl,
        String testPrUrl,
        Instant createdAt
) {}
