package com.testgen;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RomanNumeralConverter Tests")
class RomanNumeralConverterTest {

    private final RomanNumeralConverter converter = new RomanNumeralConverter();

    // ============= toRoman Tests =============

    @Test
    @DisplayName("toRoman converts 1 to I")
    void testToRomanOne() {
        assertEquals("I", converter.toRoman(1));
    }

    @Test
    @DisplayName("toRoman converts 3999 to MMMCMXCIX")
    void testToRomanMaximum() {
        assertEquals("MMMCMXCIX", converter.toRoman(3999));
    }

    @Test
    @DisplayName("toRoman converts 4 to IV (subtractive notation)")
    void testToRomanSubtractiveIV() {
        assertEquals("IV", converter.toRoman(4));
    }

    @Test
    @DisplayName("toRoman converts 9 to IX (subtractive notation)")
    void testToRomanSubtractiveIX() {
        assertEquals("IX", converter.toRoman(9));
    }

    @Test
    @DisplayName("toRoman converts 1994 to MCMXCIV")
    void testToRomanMixedNotation() {
        assertEquals("MCMXCIV", converter.toRoman(1994));
    }

    @ParameterizedTest
    @CsvSource({
        "1, I",
        "2, II",
        "3, III",
        "4, IV",
        "5, V",
        "6, VI",
        "7, VII",
        "8, VIII",
        "9, IX",
        "10, X",
        "20, XX",
        "30, XXX",
        "40, XL",
        "50, L",
        "90, XC",
        "100, C",
        "400, CD",
        "500, D",
        "900, CM",
        "1000, M",
        "58, LVIII",
        "444, CDXLIV",
        "2024, MMXXIV"
    })
    @DisplayName("toRoman converts various numbers correctly")
    void testToRomanVariousNumbers(int number, String expected) {
        assertEquals(expected, converter.toRoman(number));
    }

    @Test
    @DisplayName("toRoman throws IllegalArgumentException for 0")
    void testToRomanZeroThrows() {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> converter.toRoman(0)
        );
        assertTrue(ex.getMessage().contains("must be between 1 and 3999"));
    }

    @Test
    @DisplayName("toRoman throws IllegalArgumentException for negative number")
    void testToRomanNegativeThrows() {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> converter.toRoman(-5)
        );
        assertTrue(ex.getMessage().contains("must be between 1 and 3999"));
    }

    @Test
    @DisplayName("toRoman throws IllegalArgumentException for 4000")
    void testToRomanAboveMaximumThrows() {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> converter.toRoman(4000)
        );
        assertTrue(ex.getMessage().contains("must be between 1 and 3999"));
    }

    @Test
    @DisplayName("toRoman throws IllegalArgumentException for large number")
    void testToRomanLargeNumberThrows() {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> converter.toRoman(10000)
        );
        assertTrue(ex.getMessage().contains("must be between 1 and 3999"));
    }

    // ============= fromRoman Tests =============

    @Test
    @DisplayName("fromRoman converts I to 1")
    void testFromRomanOne() {
        assertEquals(1, converter.fromRoman("I"));
    }

    @Test
    @DisplayName("fromRoman converts MMMCMXCIX to 3999")
    void testFromRomanMaximum() {
        assertEquals(3999, converter.fromRoman("MMMCMXCIX"));
    }

    @Test
    @DisplayName("fromRoman converts IV to 4")
    void testFromRomanSubtractiveIV() {
        assertEquals(4, converter.fromRoman("IV"));
    }

    @Test
    @DisplayName("fromRoman converts IX to 9")
    void testFromRomanSubtractiveIX() {
        assertEquals(9, converter.fromRoman("IX"));
    }

    @Test
    @DisplayName("fromRoman converts MCMXCIV to 1994")
    void testFromRomanMixedNotation() {
        assertEquals(1994, converter.fromRoman("MCMXCIV"));
    }

    @ParameterizedTest
    @CsvSource({
        "I, 1",
        "II, 2",
        "III, 3",
        "IV, 4",
        "V, 5",
        "VI, 6",
        "VII, 7",
        "VIII, 8",
        "IX, 9",
        "X, 10",
        "XX, 20",
        "XXX, 30",
        "XL, 40",
        "L, 50",
        "XC, 90",
        "C, 100",
        "CD, 400",
        "D, 500",
        "CM, 900",
        "M, 1000",
        "LVIII, 58",
        "CDXLIV, 444",
        "MMXXIV, 2024"
    })
    @DisplayName("fromRoman converts various Roman numerals correctly")
    void testFromRomanVariousValues(String roman, int expected) {
        assertEquals(expected, converter.fromRoman(roman));
    }

    @Test
    @DisplayName("fromRoman throws IllegalArgumentException for null")
    void testFromRomanNullThrows() {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> converter.fromRoman(null)
        );
        assertTrue(ex.getMessage().contains("must not be null or empty"));
    }

    @Test
    @DisplayName("fromRoman throws IllegalArgumentException for empty string")
    void testFromRomanEmptyStringThrows() {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> converter.fromRoman("")
        );
        assertTrue(ex.getMessage().contains("must not be null or empty"));
    }

    @Test
    @DisplayName("fromRoman throws IllegalArgumentException for invalid character")
    void testFromRomanInvalidCharacterThrows() {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> converter.fromRoman("IXA")
        );
        assertTrue(ex.getMessage().contains("invalid Roman numeral"));
    }

    @Test
    @DisplayName("fromRoman throws IllegalArgumentException for lowercase")
    void testFromRomanLowercaseThrows() {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> converter.fromRoman("iv")
        );
        assertTrue(ex.getMessage().contains("invalid Roman numeral"));
    }

    @Test
    @DisplayName("fromRoman throws IllegalArgumentException for mixed case")
    void testFromRomanMixedCaseThrows() {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> converter.fromRoman("Iv")
        );
        assertTrue(ex.getMessage().contains("invalid Roman numeral"));
    }

    // ============= Round-trip Tests =============

    @ParameterizedTest
    @ValueSource(ints = {1, 4, 5, 9, 10, 27, 49, 58, 100, 444, 500, 900, 1000, 1994, 2024, 3999})
    @DisplayName("toRoman and fromRoman are inverse operations")
    void testRoundTrip(int number) {
        String roman = converter.toRoman(number);
        int result = converter.fromRoman(roman);
        assertEquals(number, result);
    }

    @Test
    @DisplayName("toRoman produces uppercase only")
    void testToRomanProducesUppercase() {
        String roman = converter.toRoman(1994);
        assertEquals(roman, roman.toUpperCase());
    }
}