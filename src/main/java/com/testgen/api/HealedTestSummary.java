package com.testgen.api;

import java.util.List;

public record HealedTestSummary(
        String testClassName,
        String status,
        String testPrUrl,
        List<String> validationErrors
) {}
