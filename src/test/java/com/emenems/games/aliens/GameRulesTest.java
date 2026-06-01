package com.emenems.games.aliens;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GameRulesTest {

    @Test
    void alienScoreScalesWithWave() {
        assertEquals(10, GameRules.alienScoreForWave(1));
        assertEquals(30, GameRules.alienScoreForWave(3));
    }

    @Test
    void alienSpeedStartsAtBaseSpeed() {
        assertEquals(0.8, GameRules.alienSpeedForWave(1), 0.001);
    }

    @Test
    void alienSpeedIncreasesWithWave() {
        assertTrue(GameRules.alienSpeedForWave(2) > 0.8);
        assertTrue(GameRules.alienSpeedForWave(10) > 2.0);
    }

    @Test
    void alienSpeedNeverExceedsCap() {
        assertEquals(2.8, GameRules.alienSpeedForWave(20), 0.001);
    }
}
