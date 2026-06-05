package com.emenems.games.aliens.gamemachines;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AlienExplosionTest {
    @Test
    void explosionKeepsPositionAndExpiresAfterTwelveTicks() {
        AlienExplosion explosion = new AlienExplosion(120, 240);

        assertEquals(120, explosion.getX());
        assertEquals(240, explosion.getY());

        for (int tick = 0; tick < 11; tick++) {
            explosion.tick();
            assertFalse(explosion.isExpired());
            assertEquals(120, explosion.getX());
            assertEquals(240, explosion.getY());
        }

        explosion.tick();

        assertTrue(explosion.isExpired());
        assertEquals(120, explosion.getX());
        assertEquals(240, explosion.getY());
    }
}
