package com.testgen.fixtures;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ValidTestFixture {

    @Test
    void alwaysPasses() {
        assertEquals(2, 1 + 1);
    }
}
