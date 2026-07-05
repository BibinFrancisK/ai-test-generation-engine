package com.testgen.model;

public record DiffHunk(
        String fileName,
        int addedStartLine,
        int addedLineCount,
        String rawContent
) {}
