package com.testgen.healing;

import com.testgen.generation.LlmProvider;
import com.testgen.model.GeneratedTest;
import com.testgen.model.HealingResult;
import com.testgen.model.TestFailure;
import com.testgen.model.ValidationResult;
import com.testgen.validation.TestValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

public class TestHealer {

    private static final Logger log = LoggerFactory.getLogger(TestHealer.class);

    private final LlmProvider llmProvider;
    private final HealingPromptBuilder promptBuilder;
    private final TestValidator testValidator;

    public TestHealer(LlmProvider llmProvider, HealingPromptBuilder promptBuilder, TestValidator testValidator) {
        this.llmProvider = llmProvider;
        this.promptBuilder = promptBuilder;
        this.testValidator = testValidator;
    }

    public HealingResult heal(String failingTestCode, String classUnderTestSource,
                               String classUnderTestClassName, TestFailure failure) {
        String systemPrompt = promptBuilder.buildSystemPrompt();
        String userPrompt = promptBuilder.buildUserPrompt(failingTestCode, classUnderTestSource, failure);

        Attempt first = attempt(systemPrompt, userPrompt, classUnderTestSource, classUnderTestClassName, failure);
        if (first.validationResult() instanceof ValidationResult.ValidationSuccess) {
            return new HealingResult.HealingSuccess(first.generatedTest(), "Fixed on first attempt");
        }

        // Capped at one retry — an unbounded loop risks runaway LLM spend; the plan
        ValidationResult.ValidationFailure firstFailure = (ValidationResult.ValidationFailure) first.validationResult();
        log.info("First healing attempt failed validation for {} at {}: retrying once",
                failure.testClassName(), firstFailure.failedAt());

        String retryUserPrompt = promptBuilder.buildRetryUserPrompt(
                failingTestCode, classUnderTestSource, failure, firstFailure.errors());
        Attempt retry = attempt(systemPrompt, retryUserPrompt, classUnderTestSource, classUnderTestClassName, failure);

        return switch (retry.validationResult()) {
            case ValidationResult.ValidationSuccess ignored ->
                    new HealingResult.HealingSuccess(retry.generatedTest(), "Fixed after one retry");
            case ValidationResult.ValidationFailure retryFailure ->
                    new HealingResult.HealingFailure(
                            "Validation failed after retry at " + retryFailure.failedAt(), retryFailure.errors());
        };
    }

    private Attempt attempt(String systemPrompt, String userPrompt, String classUnderTestSource,
                             String classUnderTestClassName, TestFailure failure) {
        String rawResponse = llmProvider.generate(systemPrompt, userPrompt);
        String testCode = rawResponse.replaceAll("```java|```", "").trim();

        GeneratedTest generatedTest = new GeneratedTest(
                simpleName(failure.testClassName()), packageName(failure.testClassName()),
                testCode, null, Instant.now());

        ValidationResult validationResult = testValidator.validate(
                generatedTest, classUnderTestSource, classUnderTestClassName);
        return new Attempt(generatedTest, validationResult);
    }

    private String simpleName(String fullyQualifiedClassName) {
        int lastDot = fullyQualifiedClassName.lastIndexOf('.');
        return lastDot == -1 ? fullyQualifiedClassName : fullyQualifiedClassName.substring(lastDot + 1);
    }

    private String packageName(String fullyQualifiedClassName) {
        int lastDot = fullyQualifiedClassName.lastIndexOf('.');
        return lastDot == -1 ? "" : fullyQualifiedClassName.substring(0, lastDot);
    }

    private record Attempt(GeneratedTest generatedTest, ValidationResult validationResult) {
    }
}
