package com.testgen.validation;

import java.util.List;

public record ExecutionResult(
        List<String> passedTests,
        List<String> failedTests,
        List<String> errorMessages
) {}
