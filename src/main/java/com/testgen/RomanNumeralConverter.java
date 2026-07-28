package com.testgen;

/**
 * Standalone demo target for the AI Test Generation Engine — not part of the
 * engine's own domain model. Lives in the root package deliberately, mirroring
 * the WebhookSmokeTarget pattern from the Day 14 e2e run: a small, self-contained
 * class the engine can analyze and generate real tests against when this repo
 * acts as its own webhook test target.
 *
 * Deliberately avoids exact-length string counting and Unicode edge cases —
 * every expected value here is a well-known, unambiguous Roman numeral mapping,
 * not something that needs to be hand-computed or hand-counted.
 */
public class RomanNumeralConverter {

    private static final int[] VALUES = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
    private static final String[] SYMBOLS = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};

    /**
     * Converts a number in the standard classical range (1–3999 inclusive) to its
     * Roman numeral representation, using subtractive notation (e.g. 4 -&gt; "IV",
     * 1994 -&gt; "MCMXCIV").
     */
    public String toRoman(int number) {
        if (number < 1 || number > 3999) {
            throw new IllegalArgumentException("number must be between 1 and 3999, got: " + number);
        }

        StringBuilder result = new StringBuilder();
        int remaining = number;
        for (int i = 0; i < VALUES.length; i++) {
            while (remaining >= VALUES[i]) {
                result.append(SYMBOLS[i]);
                remaining -= VALUES[i];
            }
        }
        return result.toString();
    }

    /**
     * Parses a Roman numeral string back into its integer value (e.g. "IV" -&gt; 4,
     * "MCMXCIV" -&gt; 1994). Case-sensitive — only uppercase symbols are recognized.
     */
    public int fromRoman(String roman) {
        if (roman == null || roman.isEmpty()) {
            throw new IllegalArgumentException("roman must not be null or empty");
        }

        int total = 0;
        int position = 0;
        while (position < roman.length()) {
            int symbolIndex = matchSymbolAt(roman, position);
            if (symbolIndex == -1) {
                throw new IllegalArgumentException("invalid Roman numeral: " + roman);
            }
            total += VALUES[symbolIndex];
            position += SYMBOLS[symbolIndex].length();
        }
        return total;
    }

    private int matchSymbolAt(String roman, int position) {
        for (int i = 0; i < SYMBOLS.length; i++) {
            if (roman.startsWith(SYMBOLS[i], position)) {
                return i;
            }
        }
        return -1;
    }
}
