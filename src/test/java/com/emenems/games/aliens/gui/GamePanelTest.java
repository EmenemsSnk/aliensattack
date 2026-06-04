package com.emenems.games.aliens.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class GamePanelTest {

    @Test
    void rapidFireSecondsUseCeilingAndNeverShowZeroWhileActive() {
        assertEquals(3, GamePanel.rapidFireSecondsRemaining(180));
        assertEquals(3, GamePanel.rapidFireSecondsRemaining(179));
        assertEquals(1, GamePanel.rapidFireSecondsRemaining(1));
    }
}
