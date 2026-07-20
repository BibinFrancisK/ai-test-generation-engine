package com.testgen.model;

public record TestFailure(
        String testClassName,
        String testMethodName,
        String errorMessage,
        String stackTrace
) {}
