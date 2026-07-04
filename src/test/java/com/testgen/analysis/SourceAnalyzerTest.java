package com.testgen.analysis;

import com.testgen.model.ChangedMethod;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SourceAnalyzerTest {

    private static final SourceAnalyzer analyzer = new SourceAnalyzer();
    private static String sampleServiceSource;

    @BeforeAll
    static void loadFixture() throws IOException {
        try (var stream = SourceAnalyzerTest.class.getResourceAsStream("/fixtures/SampleService.java")) {
            Assertions.assertNotNull(stream);
            sampleServiceSource = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    @DisplayName("extracts the correct number of methods — constructor must not be counted")
    void extractsCorrectMethodCount() {
        List<ChangedMethod> methods = analyzer.analyze(sampleServiceSource);
        assertThat(methods).hasSize(4);
    }

    @Test
    @DisplayName("extracts parameter types for a multi-parameter method in declaration order")
    void extractsParameterTypesCorrectly() {
        List<ChangedMethod> methods = analyzer.analyze(sampleServiceSource);
        ChangedMethod processOrder = methods.stream()
                .filter(m -> m.methodName().equals("processOrder"))
                .findFirst()
                .orElseThrow();
        assertThat(processOrder.parameterTypes()).containsExactly("String", "int");
    }

    @Test
    @DisplayName("extracts annotations present on a method declaration without the @ prefix")
    void extractsAnnotationsCorrectly() {
        List<ChangedMethod> methods = analyzer.analyze(sampleServiceSource);
        ChangedMethod saveOrder = methods.stream()
                .filter(m -> m.methodName().equals("saveOrder"))
                .findFirst()
                .orElseThrow();
        assertThat(saveOrder.annotations()).contains("Transactional");
    }

    @Test
    @DisplayName("extracts void as the return type string for void methods")
    void extractsVoidReturnType() {
        List<ChangedMethod> methods = analyzer.analyze(sampleServiceSource);
        ChangedMethod deleteOrder = methods.stream()
                .filter(m -> m.methodName().equals("deleteOrder"))
                .findFirst()
                .orElseThrow();
        assertThat(deleteOrder.returnType()).isEqualTo("void");
    }

    @Test
    @DisplayName("extracts positive 1-indexed start and end line numbers for every method")
    void extractsLineNumbers() {
        List<ChangedMethod> methods = analyzer.analyze(sampleServiceSource);
        methods.forEach(m -> {
            assertThat(m.startLine()).isGreaterThan(0);
            assertThat(m.endLine()).isGreaterThanOrEqualTo(m.startLine());
        });
    }

    @Test
    @DisplayName("returns an empty list when the class body contains no method declarations")
    void returnsEmptyListForEmptyClass() {
        List<ChangedMethod> methods = analyzer.analyze("package com.example; public class Empty {}");
        assertThat(methods).isEmpty();
    }
}
