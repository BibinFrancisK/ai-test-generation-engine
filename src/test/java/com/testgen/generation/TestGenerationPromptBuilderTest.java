package com.testgen.generation;

import com.testgen.model.ChangedMethod;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TestGenerationPromptBuilderTest {

    private final TestGenerationPromptBuilder builder = new TestGenerationPromptBuilder();

    private final ChangedMethod transactionalMethod = new ChangedMethod(
            "SampleService", "process", List.of("String"), "void",
            List.of("@Transactional"), 10, 20);

    @Test
    void systemPromptContainsJUnit5AndMockito() {
        String prompt = builder.buildSystemPrompt();

        assertThat(prompt).contains("JUnit 5").contains("Mockito");
    }

    @Test
    void systemPromptInstructsNoMarkdownFences() {
        assertThat(builder.buildSystemPrompt()).contains("no markdown code fences");
    }

    @Test
    void userPromptContainsClassName() {
        assertThat(builder.buildUserPrompt(List.of(transactionalMethod)))
                .contains("SampleService");
    }

    @Test
    void userPromptContainsMethodDetails() {
        String prompt = builder.buildUserPrompt(List.of(transactionalMethod));

        assertThat(prompt)
                .contains("process")
                .contains("String")
                .contains("void");
    }

    @Test
    void userPromptContainsAnnotations() {
        assertThat(builder.buildUserPrompt(List.of(transactionalMethod)))
                .contains("@Transactional");
    }

    @Test
    void userPromptDoesNotContainMarkdownFences() {
        assertThat(builder.buildUserPrompt(List.of(transactionalMethod)))
                .doesNotContain("```");
    }

    @Test
    void userPromptIncludesAllMethodsWhenMultipleProvided() {
        ChangedMethod secondMethod = new ChangedMethod(
                "SampleService", "validate", List.of("int", "String"), "boolean",
                List.of(), 25, 35);

        String prompt = builder.buildUserPrompt(List.of(transactionalMethod, secondMethod));

        assertThat(prompt).contains("process").contains("validate");
    }

    @Test
    void buildUserPromptThrowsOnEmptyList() {
        assertThatThrownBy(() -> builder.buildUserPrompt(List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
