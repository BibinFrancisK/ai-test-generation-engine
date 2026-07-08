package com.testgen.generation;

import com.testgen.model.ChangedMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.testgen.util.Constants.PROMPT_METHOD_SIGNATURE_FORMAT;
import static com.testgen.util.Constants.SYSTEM_PROMPT;
import static com.testgen.util.Constants.USER_PROMPT_TEMPLATE;

// TODO Day 11: upgrade to accept GenerationContext — add full source, existing tests,
//              dependency sources, and project conventions to the user prompt.
public class TestGenerationPromptBuilder {

    private static final Logger log = LoggerFactory.getLogger(TestGenerationPromptBuilder.class);

    public String buildSystemPrompt() {
        return SYSTEM_PROMPT;
    }

    public String buildUserPrompt(List<ChangedMethod> changedMethods) {
        if (changedMethods == null || changedMethods.isEmpty()) {
            throw new IllegalArgumentException("changedMethods must not be null or empty");
        }

        String className = changedMethods.getFirst().className();

        String methodSignatures = String.join("\n", changedMethods.stream()
                .map(this::formatMethodSignature)
                .toList());

        String userPrompt = USER_PROMPT_TEMPLATE.formatted(className, methodSignatures);

        int estimatedTokens = userPrompt.length() / 4;
        log.debug("Estimated prompt token count: {}", estimatedTokens);

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
