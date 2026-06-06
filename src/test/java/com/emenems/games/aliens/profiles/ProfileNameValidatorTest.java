package com.emenems.games.aliens.profiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ProfileNameValidatorTest {
    @Test
    void acceptsLettersDigitsSpacesHyphensAndUnderscoresAfterTrimming() {
        ProfileNameValidator.ValidationResult result = ProfileNameValidator.validate("  Player 1-A_B  ");

        assertTrue(result.valid());
        assertEquals("Player 1-A_B", result.normalizedName());
        assertEquals("", result.message());
    }

    @Test
    void rejectsBlankNames() {
        ProfileNameValidator.ValidationResult result = ProfileNameValidator.validate("   ");

        assertFalse(result.valid());
        assertEquals(ProfileNameValidator.EMPTY_MESSAGE, result.message());
    }

    @Test
    void rejectsNamesLongerThanSixteenCharacters() {
        ProfileNameValidator.ValidationResult result = ProfileNameValidator.validate("abcdefghijklmnopq");

        assertFalse(result.valid());
        assertEquals(ProfileNameValidator.TOO_LONG_MESSAGE, result.message());
    }

    @Test
    void rejectsTabsNewlinesPunctuationAndControlCharacters() {
        assertFalse(ProfileNameValidator.validate("A\tB").valid());
        assertFalse(ProfileNameValidator.validate("A\nB").valid());
        assertFalse(ProfileNameValidator.validate("A.B").valid());
        assertFalse(ProfileNameValidator.validate("A\u0001B").valid());
    }

    @Test
    void allowedCharacterPredicateMatchesInputModeRules() {
        assertTrue(ProfileNameValidator.isAllowedCharacter('A'));
        assertTrue(ProfileNameValidator.isAllowedCharacter('7'));
        assertTrue(ProfileNameValidator.isAllowedCharacter(' '));
        assertTrue(ProfileNameValidator.isAllowedCharacter('-'));
        assertTrue(ProfileNameValidator.isAllowedCharacter('_'));
        assertFalse(ProfileNameValidator.isAllowedCharacter('\t'));
        assertFalse(ProfileNameValidator.isAllowedCharacter('.'));
    }
}
