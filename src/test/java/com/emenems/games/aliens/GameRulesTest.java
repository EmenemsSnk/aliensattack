package com.emenems.games.aliens;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.emenems.games.aliens.gamemachines.PowerUpType;
import org.junit.jupiter.api.Test;

class GameRulesTest {

    @Test
    void alienScoreScalesWithWave() {
        assertEquals(10, GameRules.alienScoreForWave(1));
        assertEquals(30, GameRules.alienScoreForWave(3));
    }

    @Test
    void alienKillBatchScoreAppliesComboAndKillCount() {
        assertEquals(10, GameRules.alienKillBatchScore(1, 1, 1));
        assertEquals(60, GameRules.alienKillBatchScore(2, 1, 3));
        assertEquals(120, GameRules.alienKillBatchScore(2, 2, 3));
    }

    @Test
    void alienKillBatchScoreBoundsMultiplierAndIgnoresNonPositiveKills() {
        assertEquals(10, GameRules.alienKillBatchScore(1, 1, 0));
        assertEquals(50, GameRules.alienKillBatchScore(1, 1, 99));
        assertEquals(0, GameRules.alienKillBatchScore(0, 1, 5));
        assertEquals(0, GameRules.alienKillBatchScore(-1, 1, 5));
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

    @Test
    void specialAlienRulesMatchPlannedTuning() {
        assertTrue(GameRules.hasSpecialAlien(2));
        assertFalse(GameRules.hasSpecialAlien(3));
        assertTrue(GameRules.hasSpecialAlien(4));
        assertEquals(1, GameRules.specialAlienCount(2, 6));
        assertEquals(1.2, GameRules.specialAlienSpeedMultiplier(), 0.001);
        assertEquals(0.02, GameRules.specialAlienDirectionChangeChance(), 0.0001);
        assertEquals(2, GameRules.alienFiringWeight(true));
        assertEquals(1, GameRules.alienFiringWeight(false));
    }

    @Test
    void supportDropRulesExposeLifeCapDurationAndWeights() {
        assertEquals(5, GameRules.maxLives());
        assertEquals(180, GameRules.speedBoostDurationTicks());
        assertEquals(8, GameRules.speedBoostMoveStep());
        assertEquals(0.12, GameRules.supportDropChance(), 0.0001);
        assertEquals(9, GameRules.totalSupportDropWeight());
        assertEquals(4, GameRules.supportDropWeight(PowerUpType.RAPID_FIRE));
        assertEquals(1, GameRules.supportDropWeight(PowerUpType.EXTRA_LIFE));
        assertEquals(2, GameRules.supportDropWeight(PowerUpType.SHIELD));
        assertEquals(2, GameRules.supportDropWeight(PowerUpType.SPEED_BOOST));
    }

    @Test
    void supportDropRollMapsAcrossWeightedRanges() {
        assertEquals(PowerUpType.RAPID_FIRE, GameRules.supportDropTypeForRoll(0));
        assertEquals(PowerUpType.RAPID_FIRE, GameRules.supportDropTypeForRoll(3));
        assertEquals(PowerUpType.EXTRA_LIFE, GameRules.supportDropTypeForRoll(4));
        assertEquals(PowerUpType.SHIELD, GameRules.supportDropTypeForRoll(5));
        assertEquals(PowerUpType.SHIELD, GameRules.supportDropTypeForRoll(6));
        assertEquals(PowerUpType.SPEED_BOOST, GameRules.supportDropTypeForRoll(7));
        assertEquals(PowerUpType.SPEED_BOOST, GameRules.supportDropTypeForRoll(8));
    }

    @Test
    void bossRulesExposeCadenceHealthBonusMovementAndRewardPool() {
        assertFalse(GameRules.isBossWave(1));
        assertFalse(GameRules.isBossWave(4));
        assertTrue(GameRules.isBossWave(5));
        assertTrue(GameRules.isBossWave(10));
        assertEquals(20, GameRules.bossHealth());
        assertEquals(200, GameRules.bossScoreBonus());
        assertEquals(5.0, GameRules.bossHorizontalSpeed(), 0.001);
        assertEquals(72, GameRules.bossTopLaneY());
        assertEquals(2, GameRules.totalBossRewardWeight());
        assertEquals(1, GameRules.bossRewardWeight(PowerUpType.EXTRA_LIFE));
        assertEquals(1, GameRules.bossRewardWeight(PowerUpType.SHIELD));
        assertEquals(0, GameRules.bossRewardWeight(PowerUpType.RAPID_FIRE));
    }

    @Test
    void bossRewardRollOnlyMapsToExtraLifeOrShield() {
        assertEquals(PowerUpType.EXTRA_LIFE, GameRules.bossRewardTypeForRoll(0));
        assertEquals(PowerUpType.SHIELD, GameRules.bossRewardTypeForRoll(1));
    }

    @Test
    void bossBurstRulesExposeBurstCountSpacingAndFireChance() {
        assertEquals(3, GameRules.bossBurstCount());
        assertEquals(24, GameRules.bossBurstSpacing());
        assertEquals(0.03, GameRules.bossFireChance(), 0.0001);
    }
}
