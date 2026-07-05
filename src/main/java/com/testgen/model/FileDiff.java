package com.testgen.model;

import java.util.List;

public record FileDiff(
        String filePath,
        DiffChangeType changeType,
        List<DiffHunk> hunks
) {}
