package com.testgen.model;

public record CoverageStats(
        long totalTestRuns,
        long aiGeneratedTests,
        long compilePassed,
        long executionPassed,
        long selfHealingAttempts,
        long selfHealingSuccesses,
        double passRate
) {}
