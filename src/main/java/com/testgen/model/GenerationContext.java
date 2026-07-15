package com.testgen.model;

import java.util.List;
import java.util.Optional;

public record GenerationContext(
        String fullSourceCode,
        Optional<String> existingTestSource,
        List<String> dependencySources,
        ProjectConventions conventions,
        List<ChangedMethod> changedMethods
) {}
