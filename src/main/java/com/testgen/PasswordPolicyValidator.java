package com.testgen;

import java.util.ArrayList;
import java.util.List;

/**
 * Standalone demo target for the AI Test Generation Engine — not part of the
 * engine's own domain model. Lives in the root package deliberately, mirroring
 * the WebhookSmokeTarget pattern from the Day 14 e2e run: a small, self-contained
 * class the engine can analyze and generate real tests against when this repo
 * acts as its own webhook test target.
 */
public class PasswordPolicyValidator {

    /**
     * Validates a candidate password against the policy and returns the list of
     * violated rules. An empty list means the password satisfies every rule.
     * Deliberately has no length-based rule — every check here is categorical
     * (does the password contain a character of a given class), not a count.
     */
    public List<String> validate(String password) {
        if (password == null) {
            throw new IllegalArgumentException("password must not be null");
        }

        List<String> violations = new ArrayList<>();

        if (!containsUppercase(password)) {
            violations.add("must contain at least one uppercase letter");
        }
        if (!containsLowercase(password)) {
            violations.add("must contain at least one lowercase letter");
        }
        if (!containsDigit(password)) {
            violations.add("must contain at least one digit");
        }
        if (!containsSpecialCharacter(password)) {
            violations.add("must contain at least one special character");
        }
        if (containsWhitespace(password)) {
            violations.add("must not contain whitespace");
        }

        return violations;
    }

    /**
     * Convenience wrapper over {@link #validate(String)} for call sites that only
     * care whether the password is acceptable, not why it failed.
     */
    public boolean isValid(String password) {
        return validate(password).isEmpty();
    }

    private boolean containsUppercase(String password) {
        return password.chars().anyMatch(Character::isUpperCase);
    }

    private boolean containsLowercase(String password) {
        return password.chars().anyMatch(Character::isLowerCase);
    }

    private boolean containsDigit(String password) {
        return password.chars().anyMatch(Character::isDigit);
    }

    /**
     * A "special character" is any character that is neither a letter nor a digit nor
     * whitespace — punctuation and symbols such as {@code !@#$%}. This check is Unicode-aware
     * via {@link Character#isLetterOrDigit(char)}, so accented and non-Latin letters (e.g. "é",
     * "ñ", "Ω") count as ordinary letters, not as special characters.
     */
    private boolean containsSpecialCharacter(String password) {
        return password.chars().anyMatch(c -> !Character.isLetterOrDigit(c) && !Character.isWhitespace(c));
    }

    private boolean containsWhitespace(String password) {
        return password.chars().anyMatch(Character::isWhitespace);
    }
}
