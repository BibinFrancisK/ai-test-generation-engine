package com.testgen.analysis;

import com.testgen.model.DiffChangeType;
import com.testgen.model.FileDiff;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DiffParserTest {

    private static String fixtureDiff;
    private final DiffParser parser = new DiffParser();

    @BeforeAll
    static void loadFixture() throws IOException {
        InputStream stream = DiffParserTest.class.getResourceAsStream("/fixtures/sample.diff");
        assertNotNull(stream, "sample.diff fixture must exist on the classpath");
        fixtureDiff = new String(stream.readAllBytes());
    }

    @Test
    void parsesJavaFileFromFixtureDiff() {
        List<FileDiff> result = parser.parse(fixtureDiff);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).filePath()).endsWith("SampleService.java");
    }

    @Test
    void extractsCorrectHunkStartLines() {
        FileDiff javaFile = parser.parse(fixtureDiff).get(0);

        assertThat(javaFile.hunks()).hasSize(2);
        assertThat(javaFile.hunks().get(0).addedStartLine()).isEqualTo(20);
        assertThat(javaFile.hunks().get(1).addedStartLine()).isEqualTo(24);
    }

    @Test
    void ignoresNonJavaFiles() {
        List<FileDiff> result = parser.parse(fixtureDiff);

        assertThat(result).noneMatch(f -> f.filePath().endsWith(".md"));
    }

    @Test
    void returnsAddedChangeTypeForNewFile() {
        String newFileDiff = """
                diff --git a/src/main/java/com/example/NewService.java b/src/main/java/com/example/NewService.java
                new file mode 100644
                --- /dev/null
                +++ b/src/main/java/com/example/NewService.java
                @@ -0,0 +1,5 @@
                +package com.example;
                +
                +public class NewService {
                +    public void doSomething() {}
                +}
                """;

        List<FileDiff> result = parser.parse(newFileDiff);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).changeType()).isEqualTo(DiffChangeType.ADDED);
    }

    @Test
    void returnsEmptyListForBlankInput() {
        assertThat(parser.parse("")).isEmpty();
        assertThat(parser.parse("   ")).isEmpty();
    }

    @Test
    void returnsEmptyListForNullInput() {
        assertThat(parser.parse(null)).isEmpty();
    }
}
