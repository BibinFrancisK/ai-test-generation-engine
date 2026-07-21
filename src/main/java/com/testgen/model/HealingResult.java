package com.testgen.model;

import java.util.List;

public sealed interface HealingResult
        permits HealingResult.HealingSuccess, HealingResult.HealingFailure {

    record HealingSuccess(
            GeneratedTest fixedTest,
            String explanation
    ) implements HealingResult {
    }

    record HealingFailure(
            String reason,
            List<String> validationErrors
    ) implements HealingResult {
    }
}
