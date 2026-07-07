package com.testgen.generation;

public final class NoopProvider implements LlmProvider {

    @Override
    public String generate(String systemPrompt, String userPrompt) {
        return """
                package com.example;
                import org.junit.jupiter.api.Test;
                import static org.junit.jupiter.api.Assertions.*;
                class NoopGeneratedTest {
                    @Test void placeholder() { assertTrue(true); }
                }
                """;
    }
}
