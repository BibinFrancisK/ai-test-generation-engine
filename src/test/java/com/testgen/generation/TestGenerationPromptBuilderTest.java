package com.testgen.generation;

import com.testgen.model.ChangedMethod;
import com.testgen.model.GenerationContext;
import com.testgen.model.ProjectConventions;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TestGenerationPromptBuilderTest {

    private final TestGenerationPromptBuilder builder = new TestGenerationPromptBuilder();

    private final ChangedMethod transactionalMethod = new ChangedMethod(
            "SampleService", "process", List.of("String"), "void",
            List.of("@Transactional"), 10, 20);

    private final ProjectConventions conventions = new ProjectConventions(
            "repo-1", "v1", "junit5", "mockito", Optional.empty(), "mirrors-source", Instant.now());

    @Test
    void systemPromptContainsConventionFrameworkAndMockLibrary() {
        String prompt = builder.buildSystemPrompt(conventions);

        assertThat(prompt).contains("junit5").contains("mockito");
    }

    @Test
    void systemPromptInstructsNoMarkdownFences() {
        assertThat(builder.buildSystemPrompt(conventions)).contains("no markdown code fences");
    }

    @Test
    void userPromptContainsFullSourceCode() {
        assertThat(builder.buildUserPrompt(contextWith(Optional.empty())))
                .contains("class SampleService {}");
    }

    @Test
    void userPromptContainsMethodDetails() {
        String prompt = builder.buildUserPrompt(contextWith(Optional.empty()));

        assertThat(prompt)
                .contains("process")
                .contains("String")
                .contains("void")
                .contains("@Transactional");
    }

    @Test
    void userPromptIncludesExistingTestFileWhenPresent() {
        String prompt = builder.buildUserPrompt(contextWith(Optional.of("class SampleServiceTest {}")));

        assertThat(prompt)
                .contains("Existing test file")
                .contains("class SampleServiceTest {}");
    }

    @Test
    void userPromptOmitsExistingTestFileSectionWhenAbsent() {
        String prompt = builder.buildUserPrompt(contextWith(Optional.empty()));

        assertThat(prompt).doesNotContain("Existing test file");
    }

    @Test
    void userPromptIncludesDependencySourcesWhenPresent() {
        GenerationContext context = new GenerationContext(
                "class SampleService {}", Optional.empty(), List.of("class Dependency {}"),
                conventions, List.of(transactionalMethod));

        assertThat(builder.buildUserPrompt(context)).contains("class Dependency {}");
    }

    @Test
    void userPromptDoesNotContainMarkdownFences() {
        assertThat(builder.buildUserPrompt(contextWith(Optional.empty())))
                .doesNotContain("```");
    }

    @Test
    void buildUserPromptThrowsOnEmptyChangedMethods() {
        GenerationContext context = new GenerationContext(
                "class SampleService {}", Optional.empty(), List.of(), conventions, List.of());

        assertThatThrownBy(() -> builder.buildUserPrompt(context))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private GenerationContext contextWith(Optional<String> existingTestSource) {
        return new GenerationContext(
                "class SampleService {}", existingTestSource, List.of(), conventions, List.of(transactionalMethod));
    }
}
