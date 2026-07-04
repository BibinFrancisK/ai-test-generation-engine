package com.testgen.model;

import java.time.Instant;
import java.util.Optional;

public record ProjectConventions(
        String repositoryId,
        String schemaVersion,
        String testingFramework,
        String mockLibrary,
        Optional<String> baseTestClassName,
        String testPackagePattern,
        Instant analyzedAt
) {}
