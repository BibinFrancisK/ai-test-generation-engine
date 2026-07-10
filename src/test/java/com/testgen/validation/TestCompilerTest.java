package com.testgen.validation;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TestCompilerTest {

    private final TestCompiler compiler = new TestCompiler();

    private Path tempDir;

    @AfterEach
    void cleanup() throws IOException {
        if (tempDir == null) return;
        try (var paths = Files.walk(tempDir)) {
            paths.sorted(Comparator.reverseOrder()).forEach(p -> {
                try { Files.delete(p); } catch (IOException ignored) {}
            });
        }
    }

    @Test
    void compilesValidSource_returnsNoErrors() throws IOException {
        String source = loadFixture("fixtures/ValidTestFixture.java");
        tempDir = Files.createTempDirectory("testgen-compiler-test-");
        Path sourceFile = tempDir.resolve("ValidTestFixture.java");
        Files.writeString(sourceFile, source);

        List<String> errors = compiler.compile(sourceFile, tempDir);

        assertThat(errors).isEmpty();
        boolean classFileExists;
        try (var paths = Files.walk(tempDir)) {
            classFileExists = paths.anyMatch(p -> p.toString().endsWith(".class"));
        }
        assertThat(classFileExists).as("Expected a .class file under output dir").isTrue();
    }

    @Test
    void compilesInvalidSource_returnsErrors() throws IOException {
        String source = loadFixture("fixtures/InvalidTestFixture.java");
        tempDir = Files.createTempDirectory("testgen-compiler-test-");
        Path sourceFile = tempDir.resolve("InvalidTestFixture.java");
        Files.writeString(sourceFile, source);

        List<String> errors = compiler.compile(sourceFile, tempDir);

        assertThat(errors).isNotEmpty();
        assertThat(errors.getFirst())
                .as("Error message should reference the file and include a line number")
                .contains("InvalidTestFixture")
                .containsPattern("line \\d+");
    }

    private String loadFixture(String resourcePath) throws IOException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            assertThat(is).as("Fixture not found on classpath: " + resourcePath).isNotNull();
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
