package com.emenems.games.aliens.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.emenems.games.aliens.GameState;
import com.emenems.games.aliens.gamemachines.Alien;
import com.emenems.games.aliens.profiles.ProfileMenuState;
import java.util.List;
import org.junit.jupiter.api.Test;

class GamePanelTest {
    @Test
    void explosionSpriteResourceExists() {
        assertTrue(GamePanel.hasExplosionSprite());
    }

    @Test
    void rapidFireSecondsUseCeilingAndNeverShowZeroWhileActive() {
        assertEquals(3, GamePanel.rapidFireSecondsRemaining(180));
        assertEquals(3, GamePanel.rapidFireSecondsRemaining(179));
        assertEquals(1, GamePanel.rapidFireSecondsRemaining(1));
    }

    @Test
    void comboSecondsUseCeilingAndNeverShowZeroWhileActive() {
        assertEquals(2, GamePanel.comboSecondsRemaining(90));
        assertEquals(1, GamePanel.comboSecondsRemaining(60));
        assertEquals(1, GamePanel.comboSecondsRemaining(1));
    }

    @Test
    void comboIsVisibleOnlyForActiveMultiplierAboveOne() {
        assertFalse(GamePanel.isComboVisible(1, 90));
        assertFalse(GamePanel.isComboVisible(2, 0));
        assertTrue(GamePanel.isComboVisible(2, 1));
        assertTrue(GamePanel.isComboVisible(5, 90));
    }

    @Test
    void hudCardHeightGrowsOnlyForVisibleEffectRows() {
        int baseHeight = GamePanel.hudCardHeight(false, 1, 0);

        assertEquals(baseHeight + 24, GamePanel.hudCardHeight(true, 1, 0));
        assertEquals(baseHeight + 24, GamePanel.hudCardHeight(false, 2, 60));
        assertEquals(baseHeight + 48, GamePanel.hudCardHeight(true, 2, 60));
    }

    @Test
    void waveMessageIsVisibleOnlyWhilePlayingAndTicksRemain() {
        assertTrue(GamePanel.isWaveMessageVisible(GameState.PLAYING, true, 1));
        assertFalse(GamePanel.isWaveMessageVisible(GameState.START_MENU, true, 10));
        assertFalse(GamePanel.isWaveMessageVisible(GameState.PAUSED, true, 10));
        assertFalse(GamePanel.isWaveMessageVisible(GameState.GAME_OVER, true, 10));
        assertFalse(GamePanel.isWaveMessageVisible(GameState.PLAYING, false, 10));
        assertFalse(GamePanel.isWaveMessageVisible(GameState.PLAYING, true, 0));
    }

    @Test
    void pausedOverlayIsVisibleOnlyWhilePaused() {
        assertFalse(GamePanel.isPausedOverlayVisible(GameState.START_MENU));
        assertFalse(GamePanel.isPausedOverlayVisible(GameState.PLAYING));
        assertTrue(GamePanel.isPausedOverlayVisible(GameState.PAUSED));
        assertFalse(GamePanel.isPausedOverlayVisible(GameState.GAME_OVER));
    }

    @Test
    void profileMenuStateFormatsEmptyAndSelectedProfilePrompts() {
        ProfileMenuState emptyState = ProfileMenuState.empty();
        ProfileMenuState selectedState = new ProfileMenuState("Player", 120, 2, 1, false, "", "", false, false);

        assertFalse(emptyState.hasSelectedProfile());
        assertEquals("No profiles", emptyState.profileCounterText());
        assertEquals("Best: -", emptyState.bestScoreText());
        assertEquals("Create a profile to start", emptyState.startPromptText());
        assertTrue(selectedState.hasSelectedProfile());
        assertEquals("Profile 2 of 2", selectedState.profileCounterText());
        assertEquals("Best: 120", selectedState.bestScoreText());
        assertEquals("Press ENTER to Start", selectedState.startPromptText());
    }

    @Test
    void profileGameOverMessagesMatchProfileResultState() {
        ProfileMenuState neutralState = new ProfileMenuState("Player", 120, 1, 0, false, "", "", false, false);
        ProfileMenuState newBestState = new ProfileMenuState("Player", 140, 1, 0, false, "", "", false, true);
        ProfileMenuState saveFailedState = new ProfileMenuState("Player", 140, 1, 0, false, "", "", true, true);

        assertFalse(GamePanel.isNewBestScoreVisible(neutralState));
        assertTrue(GamePanel.isNewBestScoreVisible(newBestState));
        assertFalse(GamePanel.isSaveWarningVisible(newBestState));
        assertTrue(GamePanel.isSaveWarningVisible(saveFailedState));
    }

    @Test
    void leaderboardHelpersFormatRankingRows() {
        ProfileMenuState emptyState = ProfileMenuState.empty();
        ProfileMenuState rankedState = new ProfileMenuState(
            "Player",
            120,
            1,
            0,
            false,
            "",
            "",
            false,
            false,
            List.of(new ProfileMenuState.LeaderboardEntry(1, "Player", 120))
        );

        assertFalse(GamePanel.isLeaderboardVisible(emptyState));
        assertTrue(GamePanel.isLeaderboardVisible(rankedState));
        assertEquals(
            "1. Player  120",
            GamePanel.leaderboardRowText(rankedState.topProfiles().getFirst())
        );
    }

    @Test
    void shieldedSpecialAlienPredicateMatchesAlienState() {
        Alien standardAlien = new Alien(100, 100);
        Alien shieldedSpecialAlien = Alien.special(100, 100, 1);
        Alien damagedSpecialAlien = Alien.special(100, 100, 1);
        damagedSpecialAlien.takeHit();

        assertFalse(GamePanel.isShieldedSpecialAlien(standardAlien));
        assertTrue(GamePanel.isShieldedSpecialAlien(shieldedSpecialAlien));
        assertFalse(GamePanel.isShieldedSpecialAlien(damagedSpecialAlien));
    }
}
