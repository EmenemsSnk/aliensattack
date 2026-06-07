package com.emenems.games.aliens.gamemachines;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PowerUpTest {
    @Test
    void constructorRetainsTypeAndCoordinatesForAllPowerUps() {
        for (PowerUpType type : PowerUpType.values()) {
            PowerUp powerUp = new PowerUp(type, 100, 200);

            assertEquals(type, powerUp.getType());
            assertEquals(100, powerUp.getX());
            assertEquals(200, powerUp.getY());
        }
    }

    @Test
    void moveAdvancesPowerUpDownward() {
        PowerUp powerUp = new PowerUp(PowerUpType.RAPID_FIRE, 100, 200);

        powerUp.move();

        assertEquals(203, powerUp.getY());
    }
}
