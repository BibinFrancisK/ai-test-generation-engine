package com.testgen;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

@DisplayName("WebhookSmokeTarget")
class WebhookSmokeTargetTest {

    private final WebhookSmokeTarget target = new WebhookSmokeTarget();

    @Test
    @DisplayName("addOne should return value + 1")
    void testAddOneWithPositiveNumber() {
        assertEquals(6, target.addOne(5));
    }

    @Test
    @DisplayName("addOne should handle zero")
    void testAddOneWithZero() {
        assertEquals(1, target.addOne(0));
    }

    @Test
    @DisplayName("addOne should handle negative numbers")
    void testAddOneWithNegativeNumber() {
        assertEquals(-4, target.addOne(-5));
    }

    @Test
    @DisplayName("addOne should handle large numbers")
    void testAddOneWithLargeNumber() {
        assertEquals(Integer.MAX_VALUE, target.addOne(Integer.MAX_VALUE - 1));
    }

    @Test
    @DisplayName("subtractOne should return value - 1")
    void testSubtractOneWithPositiveNumber() {
        assertEquals(4, target.subtractOne(5));
    }

    @Test
    @DisplayName("subtractOne should handle zero")
    void testSubtractOneWithZero() {
        assertEquals(-1, target.subtractOne(0));
    }

    @Test
    @DisplayName("subtractOne should handle negative numbers")
    void testSubtractOneWithNegativeNumber() {
        assertEquals(-6, target.subtractOne(-5));
    }

    @Test
    @DisplayName("subtractOne should handle large numbers")
    void testSubtractOneWithLargeNumber() {
        assertEquals(Integer.MIN_VALUE, target.subtractOne(Integer.MIN_VALUE + 1));
    }
}