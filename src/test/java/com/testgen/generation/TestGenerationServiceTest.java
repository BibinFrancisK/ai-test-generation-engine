package com.testgen.generation;

import com.testgen.model.ChangedMethod;
import com.testgen.model.GeneratedTest;
import com.testgen.model.GenerationContext;
import com.testgen.model.ProjectConventions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TestGenerationServiceTest {

    private static final String VALID_TEST_CLASS = """
            package com.example;

            import org.junit.jupiter.api.Test;
            import static org.junit.jupiter.api.Assertions.assertTrue;

            class SampleServiceTest {
                @Test
                void placeholderTest() {
                    assertTrue(true);
                }
            }
            """;

    // Spy on a concrete permitted type — Mockito cannot mock sealed interfaces directly
    @Spy
    private NoopProvider llmProvider;

    @Mock
    private TestGenerationPromptBuilder promptBuilder;

    private TestGenerationService service;

    private Path writtenFile;

    @BeforeEach
    void setUp() {
        service = new TestGenerationService(llmProvider, promptBuilder);
    }

    @AfterEach
    void cleanup() throws IOException {
        if (writtenFile != null) {
            Files.deleteIfExists(writtenFile);
        }
    }

    @Test
    void generateExtractsClassNameAndWritesFileToDisk() {
        when(promptBuilder.buildSystemPrompt(any())).thenReturn("system prompt");
        when(promptBuilder.buildUserPrompt(any())).thenReturn("user prompt");
        doReturn(VALID_TEST_CLASS).when(llmProvider).generate(any(), any());

        GeneratedTest result = service.generate(sampleContext());
        writtenFile = result.savedPath();

        assertThat(result.className()).isEqualTo("SampleServiceTest");
        assertThat(result.packageName()).isEqualTo("com.example");
        assertThat(result.savedPath()).isNotNull();
        assertThat(result.savedPath().toFile()).exists();
        assertThat(result.generatedAt()).isNotNull().isBeforeOrEqualTo(Instant.now());
    }

    @Test
    void generateStripsMarkdownFencesFromLlmResponse() throws IOException {
        String withFences = "```java\n" + VALID_TEST_CLASS + "\n```";
        when(promptBuilder.buildSystemPrompt(any())).thenReturn("system prompt");
        when(promptBuilder.buildUserPrompt(any())).thenReturn("user prompt");
        doReturn(withFences).when(llmProvider).generate(any(), any());

        GeneratedTest result = service.generate(sampleContext());
        writtenFile = result.savedPath();

        assertThat(result.testCode()).doesNotContain("```");
        assertThat(Files.readString(writtenFile)).isEqualTo(result.testCode());
    }

    @Test
    void generateThrowsWhenLlmResponseHasNoClassDeclaration() {
        when(promptBuilder.buildSystemPrompt(any())).thenReturn("system prompt");
        when(promptBuilder.buildUserPrompt(any())).thenReturn("user prompt");
        doReturn("This is not valid Java code, no class declaration here.")
                .when(llmProvider).generate(any(), any());

        assertThatThrownBy(() -> service.generate(sampleContext()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no Java class declaration");
    }

    private GenerationContext sampleContext() {
        ChangedMethod method = new ChangedMethod(
                "SampleService", "process", List.of("String"), "void",
                List.of("@Transactional"), 10, 20);
        ProjectConventions conventions = new ProjectConventions(
                "repo-1", "v1", "junit5", "mockito", Optional.empty(), "mirrors-source", Instant.now());

        return new GenerationContext(
                "class SampleService {}", Optional.empty(), List.of(), conventions, List.of(method));
    }
}
