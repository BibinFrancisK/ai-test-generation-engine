package com.testgen.generation;

import com.testgen.model.ChangedMethod;
import com.testgen.model.GenerationContext;
import com.testgen.model.ProjectConventions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.testgen.util.Constants.CHANGED_METHODS_HEADER;
import static com.testgen.util.Constants.DEPENDENCY_SOURCES_HEADER;
import static com.testgen.util.Constants.EXISTING_TEST_FILE_HEADER;
import static com.testgen.util.Constants.FULL_SOURCE_HEADER;
import static com.testgen.util.Constants.PROMPT_METHOD_SIGNATURE_FORMAT;
import static com.testgen.util.Constants.PROMPT_TOKEN_BUDGET;
import static com.testgen.util.Constants.SYSTEM_PROMPT_TEMPLATE;

public class TestGenerationPromptBuilder {

    private static final Logger log = LoggerFactory.getLogger(TestGenerationPromptBuilder.class);

    public String buildSystemPrompt(ProjectConventions conventions) {
        return SYSTEM_PROMPT_TEMPLATE.formatted(conventions.testingFramework(), conventions.mockLibrary());
    }

    public String buildUserPrompt(GenerationContext context) {
        if (context.changedMethods() == null || context.changedMethods().isEmpty()) {
            throw new IllegalArgumentException("changedMethods must not be null or empty");
        }

        StringBuilder prompt = new StringBuilder();

        context.existingTestSource().ifPresent(existingTest ->
                prompt.append(EXISTING_TEST_FILE_HEADER).append("\n").append(existingTest).append("\n\n"));

        prompt.append(FULL_SOURCE_HEADER).append("\n").append(context.fullSourceCode()).append("\n\n");

        if (!context.dependencySources().isEmpty()) {
            prompt.append(DEPENDENCY_SOURCES_HEADER).append("\n");
            context.dependencySources().forEach(dep -> prompt.append(dep).append("\n\n"));
        }

        prompt.append(CHANGED_METHODS_HEADER).append("\n");
        context.changedMethods().forEach(method -> prompt.append(formatMethodSignature(method)).append("\n"));

        String userPrompt = prompt.toString();

        int estimatedTokens = userPrompt.length() / 4;
        log.debug("Estimated prompt token count: {}", estimatedTokens);
        if (estimatedTokens > PROMPT_TOKEN_BUDGET) {
            log.warn("Estimated prompt token count {} exceeds budget of {}", estimatedTokens, PROMPT_TOKEN_BUDGET);
        }

        return userPrompt;
    }

    private String formatMethodSignature(ChangedMethod method) {
        String annotations = method.annotations().isEmpty()
                ? ""
                : String.join(" ", method.annotations()) + " ";
        String params = String.join(", ", method.parameterTypes());
        return PROMPT_METHOD_SIGNATURE_FORMAT.formatted(annotations, method.returnType(), method.methodName(), params);
    }
}
