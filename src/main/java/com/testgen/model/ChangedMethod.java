package com.testgen.model;

import java.util.List;

public record ChangedMethod(
        String className,
        String methodName,
        List<String> parameterTypes,
        String returnType,
        List<String> annotations,
        int startLine,
        int endLine
) {}
