package com.emenems.games.aliens.gamemachines;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RapidFirePowerUpTest {

    @Test
    void movesDownwardAtFixedSpeed() {
        RapidFirePowerUp powerUp = new RapidFirePowerUp(100, 200);

        powerUp.move();

        assertEquals(100, powerUp.getX());
        assertEquals(203, powerUp.getY());
    }
}
