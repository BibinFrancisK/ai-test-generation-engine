package com.testgen.model;

import java.nio.file.Path;
import java.time.Instant;

public record GeneratedTest(
        String className,
        String packageName,
        String testCode,
        Path savedPath,
        Instant generatedAt
) {}
