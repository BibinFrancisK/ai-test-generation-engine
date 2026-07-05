package com.testgen.analysis;

import com.testgen.model.ChangedMethod;
import com.testgen.model.DiffChangeType;
import com.testgen.model.DiffHunk;
import com.testgen.model.FileDiff;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DiffAnalyzerTest {

    private static String fixtureDiff;
    private static String fixtureSource;

    private static final String SAMPLE_SERVICE_PATH =
            "src/main/java/com/example/service/SampleService.java";

    private final DiffParser parser = new DiffParser();
    private final DiffAnalyzer analyzer = new DiffAnalyzer(new SourceAnalyzer());

    @BeforeAll
    static void loadFixtures() throws IOException {
        InputStream diffStream = DiffAnalyzerTest.class.getResourceAsStream("/fixtures/sample.diff");
        assertNotNull(diffStream, "sample.diff fixture must exist on the classpath");
        fixtureDiff = new String(diffStream.readAllBytes());

        InputStream sourceStream = DiffAnalyzerTest.class.getResourceAsStream("/fixtures/SampleService.java");
        assertNotNull(sourceStream, "SampleService.java fixture must exist on the classpath");
        fixtureSource = new String(sourceStream.readAllBytes());
    }

    @Test
    void returnsChangedMethodsOverlappingHunks() {
        List<FileDiff> fileDiffs = parser.parse(fixtureDiff);
        Map<String, String> sourceMap = Map.of(SAMPLE_SERVICE_PATH, fixtureSource);

        List<ChangedMethod> result = analyzer.analyze(fileDiffs, sourceMap);

        assertThat(result).extracting(ChangedMethod::methodName)
                .containsExactlyInAnyOrder("processOrder", "saveOrder");
        assertThat(result).extracting(ChangedMethod::methodName)
                .doesNotContain("getStatus", "deleteOrder");
    }

    @Test
    void returnsAllMethodsWhenHunkSpansWholeFile() {
        FileDiff wholeFile = new FileDiff(
                SAMPLE_SERVICE_PATH,
                DiffChangeType.MODIFIED,
                List.of(new DiffHunk(SAMPLE_SERVICE_PATH, 1, 999, ""))
        );

        List<ChangedMethod> result = analyzer.analyze(
                List.of(wholeFile), Map.of(SAMPLE_SERVICE_PATH, fixtureSource));

        assertThat(result).extracting(ChangedMethod::methodName)
                .containsExactlyInAnyOrder("getStatus", "processOrder", "saveOrder", "deleteOrder");
    }

    @Test
    void skipsFilesNotPresentInSourceMap() {
        List<FileDiff> fileDiffs = parser.parse(fixtureDiff);

        List<ChangedMethod> result = analyzer.analyze(fileDiffs, Map.of());

        assertThat(result).isEmpty();
    }

    @Test
    void skipsDeletedFiles() {
        FileDiff deleted = new FileDiff(
                SAMPLE_SERVICE_PATH,
                DiffChangeType.DELETED,
                List.of(new DiffHunk(SAMPLE_SERVICE_PATH, 1, 32, ""))
        );

        List<ChangedMethod> result = analyzer.analyze(
                List.of(deleted), Map.of(SAMPLE_SERVICE_PATH, fixtureSource));

        assertThat(result).isEmpty();
    }

    @Test
    void returnsEmptyForEmptyFileDiffs() {
        assertThat(analyzer.analyze(List.of(), Map.of())).isEmpty();
    }
}
