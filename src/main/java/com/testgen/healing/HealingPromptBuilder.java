package com.testgen.healing;

import com.testgen.model.TestFailure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.testgen.util.Constants.HEALING_CURRENT_SOURCE_HEADER;
import static com.testgen.util.Constants.HEALING_ERROR_MESSAGE_HEADER;
import static com.testgen.util.Constants.HEALING_FAILING_TEST_HEADER;
import static com.testgen.util.Constants.HEALING_PREVIOUS_ERRORS_HEADER;
import static com.testgen.util.Constants.HEALING_PROMPT_TOKEN_BUDGET;
import static com.testgen.util.Constants.HEALING_STACK_TRACE_HEADER;
import static com.testgen.util.Constants.HEALING_SYSTEM_PROMPT;

public class HealingPromptBuilder {

    private static final Logger log = LoggerFactory.getLogger(HealingPromptBuilder.class);

    public String buildSystemPrompt() {
        return HEALING_SYSTEM_PROMPT;
    }

    public String buildUserPrompt(String failingTestCode, String classUnderTestSource, TestFailure failure) {
        return buildPrompt(failingTestCode, classUnderTestSource, failure, List.of());
    }

    public String buildRetryUserPrompt(String failingTestCode, String classUnderTestSource, TestFailure failure,
                                        List<String> previousValidationErrors) {
        return buildPrompt(failingTestCode, classUnderTestSource, failure, previousValidationErrors);
    }

    private String buildPrompt(String failingTestCode, String classUnderTestSource, TestFailure failure,
                                List<String> previousValidationErrors) {
        StringBuilder prompt = new StringBuilder();

        prompt.append(HEALING_FAILING_TEST_HEADER).append("\n").append(failingTestCode).append("\n\n");
        prompt.append(HEALING_CURRENT_SOURCE_HEADER).append("\n").append(classUnderTestSource).append("\n\n");
        prompt.append(HEALING_ERROR_MESSAGE_HEADER).append("\n").append(failure.errorMessage()).append("\n\n");
        prompt.append(HEALING_STACK_TRACE_HEADER).append("\n").append(failure.stackTrace()).append("\n\n");

        if (previousValidationErrors != null && !previousValidationErrors.isEmpty()) {
            prompt.append(HEALING_PREVIOUS_ERRORS_HEADER).append("\n");
            previousValidationErrors.forEach(error -> prompt.append("- ").append(error).append("\n"));
        }

        String userPrompt = prompt.toString();

        int estimatedTokens = userPrompt.length() / 4;
        log.debug("Estimated healing prompt token count: {}", estimatedTokens);
        if (estimatedTokens > HEALING_PROMPT_TOKEN_BUDGET) {
            log.warn("Estimated healing prompt token count {} exceeds budget of {}", estimatedTokens, HEALING_PROMPT_TOKEN_BUDGET);
        }

        return userPrompt;
    }
}
