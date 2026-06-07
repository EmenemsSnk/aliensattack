package com.emenems.games.aliens.gamemachines;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.emenems.games.aliens.GameConstants;
import com.emenems.games.aliens.GameRules;
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

    @Test
    void bossNeedsTwentyHitsAndMovesHorizontallyWithoutDescending() {
        Alien boss = Alien.boss(200, GameRules.bossTopLaneY());
        int originalX = boss.getX();

        boss.move(new SequenceRandom(new double[] { 1.0, 0.5 }), 0, GameConstants.PANEL_WIDTH - GameConstants.COMPONENT_SIZE);

        assertEquals(GameRules.bossTopLaneY(), boss.getY());
        assertTrue(boss.getX() > originalX);
        assertEquals(AlienType.BOSS, boss.getType());
        assertTrue(boss.isBoss());

        for (int hit = 0; hit < GameRules.bossHealth() - 1; hit++) {
            assertFalse(boss.takeHit());
        }

        assertTrue(boss.isDamaged());
        assertTrue(boss.takeHit());
    }

    @Test
    void bossCanRandomlyChangeDirectionInsteadOfSimplePingPong() {
        Alien boss = Alien.boss(200, GameRules.bossTopLaneY());

        boss.move(new SequenceRandom(new double[] { 0.0, 0.5 }), 0, GameConstants.PANEL_WIDTH - GameConstants.COMPONENT_SIZE);

        assertTrue(boss.getX() < 200);
        assertEquals(GameRules.bossTopLaneY(), boss.getY());
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

    private static final class SequenceRandom extends Random {
        private final double[] values;
        private int index;

        private SequenceRandom(double[] values) {
            this.values = values;
        }

        @Override
        public double nextDouble() {
            double value = values[Math.min(index, values.length - 1)];
            index++;
            return value;
        }
    }
}
