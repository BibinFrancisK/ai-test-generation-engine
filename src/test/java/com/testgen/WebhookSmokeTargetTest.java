package com.testgen;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("WebhookSmokeTarget Tests")
class WebhookSmokeTargetTest {

    private WebhookSmokeTarget target;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        target = new WebhookSmokeTarget();
    }

    @Test
    @DisplayName("greet should return proper greeting with name")
    void testGreetWithValidName() {
        String result = target.greet("Alice");
        assertEquals("Hello, Alice!", result);
    }

    @Test
    @DisplayName("greet should handle empty string")
    void testGreetWithEmptyString() {
        String result = target.greet("");
        assertEquals("Hello, !", result);
    }

    @Test
    @DisplayName("greet should handle special characters")
    void testGreetWithSpecialCharacters() {
        String result = target.greet("Bob@123");
        assertEquals("Hello, Bob@123!", result);
    }

    @Test
    @DisplayName("farewell should return proper farewell with name")
    void testFarewellWithValidName() {
        String result = target.farewell("Charlie");
        assertEquals("Goodbye, Charlie!", result);
    }

    @Test
    @DisplayName("farewell should handle empty string")
    void testFarewellWithEmptyString() {
        String result = target.farewell("");
        assertEquals("Goodbye, !", result);
    }

    @Test
    @DisplayName("farewell should handle special characters")
    void testFarewellWithSpecialCharacters() {
        String result = target.farewell("Dave_2023");
        assertEquals("Goodbye, Dave_2023!", result);
    }

    @Test
    @DisplayName("greet should not return null")
    void testGreetNotNull() {
        String result = target.greet("Eve");
        assertNotNull(result);
    }

    @Test
    @DisplayName("farewell should not return null")
    void testFarewellNotNull() {
        String result = target.farewell("Frank");
        assertNotNull(result);
    }

    @Test
    @DisplayName("greet should handle whitespace names")
    void testGreetWithWhitespace() {
        String result = target.greet("  ");
        assertEquals("Hello,   !", result);
    }

    @Test
    @DisplayName("farewell should handle whitespace names")
    void testFarewellWithWhitespace() {
        String result = target.farewell("  ");
        assertEquals("Goodbye,   !", result);
    }
}