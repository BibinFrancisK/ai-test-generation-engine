package com.testgen;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("WebhookSmokeTarget Tests")
class WebhookSmokeTargetTest {

    private WebhookSmokeTarget target;

    @Test
    @DisplayName("addOne should increment value by one for positive numbers")
    void testAddOneWithPositiveNumber() {
        target = new WebhookSmokeTarget();
        int result = target.addOne(5);
        assertEquals(6, result);
    }

    @Test
    @DisplayName("addOne should increment zero by one")
    void testAddOneWithZero() {
        target = new WebhookSmokeTarget();
        int result = target.addOne(0);
        assertEquals(1, result);
    }

    @Test
    @DisplayName("addOne should increment negative numbers by one")
    void testAddOneWithNegativeNumber() {
        target = new WebhookSmokeTarget();
        int result = target.addOne(-5);
        assertEquals(-4, result);
    }

    @Test
    @DisplayName("addOne should handle maximum integer value overflow")
    void testAddOneWithMaxIntValue() {
        target = new WebhookSmokeTarget();
        int result = target.addOne(Integer.MAX_VALUE);
        assertEquals(Integer.MIN_VALUE, result);
    }

    @Test
    @DisplayName("addOne should handle minimum integer value")
    void testAddOneWithMinIntValue() {
        target = new WebhookSmokeTarget();
        int result = target.addOne(Integer.MIN_VALUE);
        assertEquals(Integer.MIN_VALUE + 1, result);
    }
}