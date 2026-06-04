package com.emenems.games.aliens.gamemachines;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.emenems.games.aliens.GameConstants;
import java.util.Random;
import org.junit.jupiter.api.Test;

class AlienTest {

    @Test
    void standardAlienMovesStraightDownAndDiesOnFirstHit() {
        Alien alien = new Alien(100, 100, 2.0);

        alien.move();

        assertEquals(102, alien.getY());
        assertTrue(alien.takeHit());
        assertEquals(AlienType.STANDARD, alien.getType());
    }

    @Test
    void specialAlienUsesBoostedSpeedAndNeedsTwoHits() {
        Alien alien = Alien.special(100, 100, 2.0);

        alien.move();

        assertEquals(102, alien.getY());
        assertFalse(alien.takeHit());
        assertTrue(alien.isDamaged());
        assertTrue(alien.takeHit());
    }

    @Test
    void specialAlienCanChangeDirectionAndStaysInBounds() {
        Alien alien = Alien.special(GameConstants.PANEL_WIDTH - GameConstants.COMPONENT_SIZE, 100, 0);

        alien.move(new FixedRandom(0.0), 0, GameConstants.PANEL_WIDTH - GameConstants.COMPONENT_SIZE);

        assertTrue(alien.getX() < GameConstants.PANEL_WIDTH - GameConstants.COMPONENT_SIZE);

        alien.move(new FixedRandom(1.0), 0, GameConstants.PANEL_WIDTH - GameConstants.COMPONENT_SIZE);

        assertTrue(alien.getX() >= 0);
    }

    private static final class FixedRandom extends Random {
        private final double nextDouble;

        private FixedRandom(double nextDouble) {
            this.nextDouble = nextDouble;
        }

        @Override
        public double nextDouble() {
            return nextDouble;
        }
    }
}
