package com.emenems.games.aliens.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.emenems.games.aliens.GameState;
import com.emenems.games.aliens.gamemachines.Alien;
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
        assertFalse(GamePanel.isWaveMessageVisible(GameState.GAME_OVER, true, 10));
        assertFalse(GamePanel.isWaveMessageVisible(GameState.PLAYING, false, 10));
        assertFalse(GamePanel.isWaveMessageVisible(GameState.PLAYING, true, 0));
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
