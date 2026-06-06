package com.emenems.games.aliens.profiles;

public final class ProfileNameValidator {
    public static final int MAX_NAME_LENGTH = 16;
    public static final String EMPTY_MESSAGE = "Enter a profile name";
    public static final String TOO_LONG_MESSAGE = "Use 16 characters or fewer";
    public static final String INVALID_CHARACTER_MESSAGE = "Use letters, digits, spaces, - or _";

    private ProfileNameValidator() {
    }

    public static ValidationResult validate(String rawName) {
        if (rawName == null) {
            return ValidationResult.invalid("", EMPTY_MESSAGE);
        }

        String normalizedName = rawName.trim();
        if (normalizedName.isEmpty()) {
            return ValidationResult.invalid(normalizedName, EMPTY_MESSAGE);
        }
        if (normalizedName.length() > MAX_NAME_LENGTH) {
            return ValidationResult.invalid(normalizedName, TOO_LONG_MESSAGE);
        }
        for (int index = 0; index < normalizedName.length(); index++) {
            if (!isAllowedCharacter(normalizedName.charAt(index))) {
                return ValidationResult.invalid(normalizedName, INVALID_CHARACTER_MESSAGE);
            }
        }
        return ValidationResult.valid(normalizedName);
    }

    public static boolean isAllowedCharacter(char character) {
        return Character.isLetterOrDigit(character)
            || character == ' '
            || character == '-'
            || character == '_';
    }

    public record ValidationResult(boolean valid, String normalizedName, String message) {
        private static ValidationResult valid(String normalizedName) {
            return new ValidationResult(true, normalizedName, "");
        }

        private static ValidationResult invalid(String normalizedName, String message) {
            return new ValidationResult(false, normalizedName, message);
        }
    }
}
