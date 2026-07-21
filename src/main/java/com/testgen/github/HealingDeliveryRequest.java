package com.testgen.github;

import com.testgen.model.GeneratedTest;

public record HealingDeliveryRequest(
        String owner,
        String repo,
        String sourceBranch,
        String sourceBranchSha,
        int sourcePrNumber,
        GeneratedTest healedTest,
        String testClassName
) {}
