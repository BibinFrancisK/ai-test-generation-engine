package com.testgen.validation;

import com.testgen.model.GeneratedTest;
import com.testgen.model.ValidationResult;
import com.testgen.model.ValidationStage;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

class TestValidatorTest {

    private final TestValidator validator = new TestValidator(new TestCompiler(), new TestExecutor());

    @Test
    void validTest_returnsValidationSuccess() throws IOException {
        GeneratedTest generatedTest = buildGeneratedTest(
                "ValidTestFixture", "com.testgen.fixtures", "fixtures/ValidTestFixture.java");

        ValidationResult result = validator.validate(generatedTest);

        switch (result) {
            case ValidationResult.ValidationSuccess success -> {
                assertThat(success.className()).isEqualTo("ValidTestFixture");
                assertThat(success.passedTests()).isNotEmpty();
                assertThat(success.testCount()).isPositive();
            }
            case ValidationResult.ValidationFailure failure ->
                fail("Expected ValidationSuccess but got ValidationFailure(failedAt=%s, errors=%s)"
                        .formatted(failure.failedAt(), failure.errors()));
        }
    }

    @Test
    void invalidTest_returnsCompileFailure() throws IOException {
        GeneratedTest generatedTest = buildGeneratedTest(
                "InvalidTestFixture", "com.testgen.fixtures", "fixtures/InvalidTestFixture.java");

        ValidationResult result = validator.validate(generatedTest);

        switch (result) {
            case ValidationResult.ValidationFailure failure -> {
                assertThat(failure.failedAt()).isEqualTo(ValidationStage.COMPILE);
                assertThat(failure.errors()).isNotEmpty();
            }
            case ValidationResult.ValidationSuccess success ->
                fail("Expected ValidationFailure(COMPILE) but got ValidationSuccess with %d test(s)"
                        .formatted(success.testCount()));
        }
    }

    private GeneratedTest buildGeneratedTest(String className, String packageName, String fixturePath) throws IOException {
        String source = loadFixture(fixturePath);
        return new GeneratedTest(className, packageName, source, Path.of("tmp/" + className + ".java"), Instant.now());
    }

    private String loadFixture(String resourcePath) throws IOException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            assertThat(is).as("Fixture not found on classpath: " + resourcePath).isNotNull();
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
