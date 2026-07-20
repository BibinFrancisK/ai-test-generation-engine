package com.testgen.healing;

import com.testgen.model.ChangedMethod;
import com.testgen.model.TestFailure;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ChangeCorrelator {

    private static final String TEST_CLASS_SUFFIX = "Test";

    public Map<TestFailure, List<ChangedMethod>> correlate(List<TestFailure> failures, List<ChangedMethod> changedMethods) {
        if (failures == null || failures.isEmpty()) {
            return Map.of();
        }

        Map<TestFailure, List<ChangedMethod>> result = new LinkedHashMap<>();

        for (TestFailure failure : failures) {
            String sourceClassName = stripTestSuffix(simpleName(failure.testClassName()));

            List<ChangedMethod> correlated = changedMethods == null
                    ? List.of()
                    : changedMethods.stream()
                            .filter(method -> simpleName(method.className()).equals(sourceClassName))
                            .toList();

            result.put(failure, correlated);
        }

        return result;
    }

    private String simpleName(String className) {
        int lastDot = className.lastIndexOf('.');
        return lastDot == -1 ? className : className.substring(lastDot + 1);
    }

    // Matching is by simple class name only (Test suffix stripped) — correlating to the
    // specific method under test would need AST-level correlation and is a stretch goal.
    private String stripTestSuffix(String simpleClassName) {
        return simpleClassName.endsWith(TEST_CLASS_SUFFIX)
                ? simpleClassName.substring(0, simpleClassName.length() - TEST_CLASS_SUFFIX.length())
                : simpleClassName;
    }
}
