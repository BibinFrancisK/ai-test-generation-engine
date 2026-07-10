package com.testgen.validation;

import com.testgen.model.GeneratedTest;
import com.testgen.model.ValidationResult;
import com.testgen.model.ValidationStage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

@Component
public class TestValidator {

    private static final Logger log = LoggerFactory.getLogger(TestValidator.class);

    private final TestCompiler compiler;
    private final TestExecutor executor;

    public TestValidator(TestCompiler compiler, TestExecutor executor) {
        this.compiler = compiler;
        this.executor = executor;
    }

    public ValidationResult validate(GeneratedTest generatedTest) {
        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("testgen-validate-");
            Path sourceFile = tempDir.resolve(generatedTest.className() + ".java");
            Files.writeString(sourceFile, generatedTest.testCode());

            List<String> compileErrors = compiler.compile(sourceFile, tempDir);
            if (!compileErrors.isEmpty()) {
                log.debug("Compile failure for {}: {} error(s)",
                        generatedTest.className(), compileErrors.size());
                return new ValidationResult.ValidationFailure(
                        generatedTest.className(), compileErrors, ValidationStage.COMPILE);
            }

            String fqn = generatedTest.packageName().isBlank()
                    ? generatedTest.className()
                    : generatedTest.packageName() + "." + generatedTest.className();

            ExecutionResult result = executor.execute(tempDir, fqn);

            if (!result.failedTests().isEmpty()) {
                log.debug("Execution failure for {}: {} test(s) failed",
                        generatedTest.className(), result.failedTests().size());
                return new ValidationResult.ValidationFailure(
                        generatedTest.className(), result.errorMessages(), ValidationStage.EXECUTE);
            }

            log.debug("Validation succeeded for {}: {} test(s) passed",
                    generatedTest.className(), result.passedTests().size());
            return new ValidationResult.ValidationSuccess(
                    generatedTest.className(),
                    result.passedTests(),
                    result.passedTests().size());

        } catch (IOException e) {
            log.warn("I/O error during validation of {}: {}", generatedTest.className(), e.getMessage());
            return new ValidationResult.ValidationFailure(
                    generatedTest.className(),
                    List.of("I/O error: " + e.getMessage()),
                    ValidationStage.COMPILE);
        } finally {
            delete_(tempDir);
        }
    }

    private void delete_(Path dir) {
        if (dir == null) return;
        try (var paths = Files.walk(dir)) {
            paths.sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (IOException exception) {
                            log.trace("An error occurred while deleting the path: {}", exception.getMessage());
                        }
                    });
        } catch (IOException exception) {
            log.trace("An error occurred while deleting the directory: {}", exception.getMessage());
        }
    }
}
