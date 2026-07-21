package com.testgen.api;

import java.time.Instant;
import java.util.List;

public record HealingResponse(
        String testRunId,
        List<HealedTestSummary> results,
        Instant createdAt
) {}
