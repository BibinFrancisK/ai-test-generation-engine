package com.testgen.model;

import java.util.List;

public sealed interface ValidationResult
        permits ValidationResult.ValidationSuccess, ValidationResult.ValidationFailure {

    record ValidationSuccess(
            String className,
            List<String> passedTests,
            int testCount
    ) implements ValidationResult {
    }

    record ValidationFailure(
            String className,
            List<String> errors,
            ValidationStage failedAt
    ) implements ValidationResult {
    }
}
