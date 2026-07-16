package com.testgen.github;

import com.testgen.model.GeneratedTest;

import java.util.List;

public record PrDeliveryRequest(
        String owner,
        String repo,
        String sourceBranch,
        String sourceBranchSha,
        int sourcePrNumber,
        GeneratedTest generatedTest,
        String validationStatus,
        List<String> coveredMethods
) {}
