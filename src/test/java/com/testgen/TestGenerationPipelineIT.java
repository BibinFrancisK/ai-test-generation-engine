package com.testgen;

import com.testgen.analysis.SourceAnalyzer;
import com.testgen.generation.NoopProvider;
import com.testgen.generation.TestGenerationPromptBuilder;
import com.testgen.generation.TestGenerationService;
import com.testgen.model.ChangedMethod;
import com.testgen.model.GeneratedTest;
import com.testgen.model.GenerationContext;
import com.testgen.model.ProjectConventions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;

@Tag("integration")
@ExtendWith(MockitoExtension.class)
class TestGenerationPipelineIT {

    private static final String VALID_TEST_CLASS = """
            package com.example.service;

            import org.junit.jupiter.api.Test;
            import static org.junit.jupiter.api.Assertions.assertEquals;

            class SampleServiceTest {
                @Test
                void getStatusReturnsOk() {
                    assertEquals("OK", "OK");
                }
            }
            """;

    // Spy on a concrete permitted type — Mockito cannot mock sealed interfaces directly
    @Spy
    private NoopProvider llmProvider;

    private Path writtenFile;

    @AfterEach
    void cleanup() throws IOException {
        if (writtenFile != null) {
            Files.deleteIfExists(writtenFile);
        }
    }

    @Test
    void pipelineGeneratesTestFileFromRealSourceAnalysis() throws IOException {
        String source = loadFixture("fixtures/SampleService.java");

        // Real SourceAnalyzer parses the fixture and returns changed methods
        SourceAnalyzer sourceAnalyzer = new SourceAnalyzer();
        List<ChangedMethod> changedMethods = sourceAnalyzer.analyze(source);
        assertThat(changedMethods).hasSize(4);

        // Real TestGenerationPromptBuilder builds prompts from the method list
        TestGenerationPromptBuilder promptBuilder = new TestGenerationPromptBuilder();

        // NoopProvider spy returns a controlled, well-formed test class
        doReturn(VALID_TEST_CLASS).when(llmProvider).generate(anyString(), anyString());

        ProjectConventions conventions = new ProjectConventions(
                "repo-1", "v1", "junit5", "mockito", Optional.empty(), "mirrors-source", Instant.now());
        GenerationContext context = new GenerationContext(
                source, Optional.empty(), List.of(), conventions, changedMethods);

        TestGenerationService service = new TestGenerationService(llmProvider, promptBuilder);
        GeneratedTest result = service.generate(context);
        writtenFile = result.savedPath();

        assertThat(result.className()).isEqualTo("SampleServiceTest");
        assertThat(result.packageName()).isEqualTo("com.example.service");
        assertThat(result.savedPath()).isNotNull();
        assertThat(result.savedPath().toFile()).exists();
        assertThat(result.generatedAt()).isNotNull().isBeforeOrEqualTo(Instant.now());
        assertThat(Files.readString(result.savedPath())).contains("class SampleServiceTest");
    }

    private String loadFixture(String resourcePath) throws IOException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            assertThat(is).as("Fixture not found on classpath: " + resourcePath).isNotNull();
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
