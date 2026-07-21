package com.testgen.api;

import java.util.Map;

public record HealingRequest(
        String testRunId,
        String sourceBranch,
        String sourceBranchSha,
        String owner,
        String repo,
        int prNumber,
        String junitXmlReport,
        String diffContent,
        Map<String, String> sourceFiles
) {}
