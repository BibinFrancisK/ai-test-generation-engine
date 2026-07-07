package com.testgen.generation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NoopProviderTest {

    private final NoopProvider noop = new NoopProvider();

    @Test
    void generateReturnsNonBlankString() {
        assertThat(noop.generate("system prompt", "user prompt")).isNotBlank();
    }

    @Test
    void generateContainsValidJavaClass() {
        String result = noop.generate("system prompt", "user prompt");
        assertThat(result).contains("class").contains("@Test");
    }

    @Test
    void generateIsIdempotent() {
        String first  = noop.generate("prompt A", "user A");
        String second = noop.generate("prompt B", "user B");
        assertThat(first).isEqualTo(second);
    }
}
