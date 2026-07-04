package com.testgen.analysis;

import com.testgen.model.ProjectConventions;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class ProjectConventionsAnalyzerTest {

    private static final ProjectConventionsAnalyzer analyzer = new ProjectConventionsAnalyzer();
    private static String sampleServiceTestSource;

    // Inline JUnit 4 fixture — avoids polluting the conventions scanner in real runs
    private static final String JUNIT4_SOURCE = """
            package com.example;
            import org.junit.Test;
            import static org.junit.Assert.assertEquals;
            public class LegacyServiceTest {
                @Test
                public void testSomething() {
                    assertEquals(1, 1);
                }
            }
            """;

    @BeforeAll
    static void loadFixture() throws IOException {
        try (var stream = ProjectConventionsAnalyzerTest.class
                .getResourceAsStream("/fixtures/SampleServiceTest.java")) {
            Assertions.assertNotNull(stream);
            sampleServiceTestSource = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    @DisplayName("detects junit5 as the testing framework from the SampleServiceTest fixture")
    void detectsJUnit5FromSampleServiceTest() {
        ProjectConventions conventions = analyzer.analyze("test-repo", List.of(sampleServiceTestSource));
        assertThat(conventions.testingFramework()).isEqualTo("junit5");
    }

    @Test
    @DisplayName("detects mockito as the mock library from the SampleServiceTest fixture")
    void detectsMockitoFromSampleServiceTest() {
        ProjectConventions conventions = analyzer.analyze("test-repo", List.of(sampleServiceTestSource));
        assertThat(conventions.mockLibrary()).isEqualTo("mockito");
    }

    @Test
    @DisplayName("detects junit4 when only JUnit 4 imports are present and no JUnit 5 imports exist")
    void detectsJUnit4WhenOnlyJUnit4ImportsPresent() {
        ProjectConventions conventions = analyzer.analyze("test-repo", List.of(JUNIT4_SOURCE));
        assertThat(conventions.testingFramework()).isEqualTo("junit4");
    }

    @Test
    @DisplayName("returns junit5, mockito, and empty baseTestClassName defaults for an empty file list")
    void returnsDefaultsForEmptyFileList() {
        ProjectConventions conventions = analyzer.analyze("test-repo", List.of());
        assertThat(conventions.testingFramework()).isEqualTo("junit5");
        assertThat(conventions.mockLibrary()).isEqualTo("mockito");
        assertThat(conventions.baseTestClassName()).isEqualTo(Optional.empty());
    }

    @Test
    @DisplayName("sets analyzedAt to a timestamp within 5 seconds of the current time")
    void setsAnalyzedAtToNow() {
        ProjectConventions conventions = analyzer.analyze("test-repo", List.of(sampleServiceTestSource));
        assertThat(conventions.analyzedAt()).isCloseTo(Instant.now(), within(5, ChronoUnit.SECONDS));
    }
}
