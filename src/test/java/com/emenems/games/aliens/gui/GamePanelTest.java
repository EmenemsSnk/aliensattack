package com.emenems.games.aliens.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.emenems.games.aliens.GameState;
import com.emenems.games.aliens.gamemachines.Alien;
import com.emenems.games.aliens.gamemachines.PowerUpType;
import com.emenems.games.aliens.profiles.ProfileMenuState;
import java.awt.Color;
import java.util.ArrayList;
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
    void speedBoostSecondsUseCeilingAndNeverShowZeroWhileActive() {
        assertEquals(3, GamePanel.speedBoostSecondsRemaining(180));
        assertEquals(3, GamePanel.speedBoostSecondsRemaining(179));
        assertEquals(1, GamePanel.speedBoostSecondsRemaining(1));
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
        int baseHeight = GamePanel.hudCardHeight(false, false, 1, 0);

        assertEquals(baseHeight + 24, GamePanel.hudCardHeight(true, false, 1, 0));
        assertEquals(baseHeight + 24, GamePanel.hudCardHeight(false, true, 1, 0));
        assertEquals(baseHeight + 24, GamePanel.hudCardHeight(false, false, 2, 60));
        assertEquals(baseHeight + 72, GamePanel.hudCardHeight(true, true, 2, 60));
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

    @Test
    void powerUpHelpersExposeDistinctLabelsAndColors() {
        assertEquals("R", GamePanel.powerUpLabel(PowerUpType.RAPID_FIRE));
        assertEquals("+", GamePanel.powerUpLabel(PowerUpType.EXTRA_LIFE));
        assertEquals("S", GamePanel.powerUpLabel(PowerUpType.SHIELD));
        assertEquals(">", GamePanel.powerUpLabel(PowerUpType.SPEED_BOOST));
        assertEquals(new Color(255, 220, 35, 220), GamePanel.powerUpColor(PowerUpType.RAPID_FIRE));
        assertEquals(new Color(255, 120, 120, 220), GamePanel.powerUpColor(PowerUpType.EXTRA_LIFE));
        assertEquals(new Color(120, 240, 255, 220), GamePanel.powerUpColor(PowerUpType.SHIELD));
        assertEquals(new Color(120, 255, 150, 220), GamePanel.powerUpColor(PowerUpType.SPEED_BOOST));
    }

    @Test
    void bossHealthBarHelpersReflectBossPresenceAndHealth() {
        List<Alien> aliens = new ArrayList<>();
        Alien boss = Alien.boss(200, 72);
        aliens.add(new Alien(100, 100));
        aliens.add(boss);

        assertEquals(boss, GamePanel.currentBoss(aliens));
        assertTrue(GamePanel.isBossHealthBarVisible(GameState.PLAYING, boss));
        assertFalse(GamePanel.isBossHealthBarVisible(GameState.GAME_OVER, boss));
        assertEquals(220, GamePanel.bossHealthBarFillWidth(20, 20, 220));
        assertEquals(110, GamePanel.bossHealthBarFillWidth(10, 20, 220));
        assertEquals(0, GamePanel.bossHealthBarFillWidth(0, 20, 220));
    }

    @Test
    void spaceshipShieldAndBossSizingHelpersReflectPresentationRules() {
        assertTrue(GamePanel.isSpaceshipShieldVisible(GameState.PLAYING, true));
        assertTrue(GamePanel.isSpaceshipShieldVisible(GameState.PAUSED, true));
        assertFalse(GamePanel.isSpaceshipShieldVisible(GameState.GAME_OVER, true));
        assertFalse(GamePanel.isSpaceshipShieldVisible(GameState.PLAYING, false));
        assertTrue(GamePanel.bossRenderWidth() > 42);
        assertTrue(GamePanel.bossRenderHeight() > 42);
    }
}
