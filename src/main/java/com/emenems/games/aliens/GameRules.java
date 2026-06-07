package com.emenems.games.aliens;

import com.emenems.games.aliens.gamemachines.PowerUpType;

public final class GameRules {
    public static final int MAX_COMBO_MULTIPLIER = 5;
    private static final double BASE_ALIEN_SPEED = 0.8;
    private static final double MAX_ALIEN_SPEED = 2.8;
    private static final double SPECIAL_ALIEN_SPEED_MULTIPLIER = 1.2;
    private static final double SPECIAL_ALIEN_DIRECTION_CHANGE_CHANCE = 0.02;
    private static final int SPECIAL_ALIEN_FIRING_WEIGHT = 2;
    private static final int MAX_LIVES = 5;
    private static final int SPEED_BOOST_DURATION_TICKS = 180;
    private static final int SPEED_BOOST_MOVE_STEP = 8;
    private static final double SUPPORT_DROP_CHANCE = 0.12;
    private static final int RAPID_FIRE_DROP_WEIGHT = 4;
    private static final int EXTRA_LIFE_DROP_WEIGHT = 1;
    private static final int SHIELD_DROP_WEIGHT = 2;
    private static final int SPEED_BOOST_DROP_WEIGHT = 2;
    private static final int BOSS_WAVE_INTERVAL = 5;
    private static final int BOSS_HEALTH = 20;
    private static final int BOSS_SCORE_BONUS = 200;
    private static final double BOSS_HORIZONTAL_SPEED = 5.0;
    private static final int BOSS_TOP_LANE_Y = 72;
    private static final double BOSS_DIRECTION_CHANGE_CHANCE = 0.08;
    private static final int EXTRA_LIFE_BOSS_REWARD_WEIGHT = 1;
    private static final int SHIELD_BOSS_REWARD_WEIGHT = 1;
    private static final double BOSS_FIRE_CHANCE = 0.03;
    private static final int BOSS_BURST_COUNT = 3;
    private static final int BOSS_BURST_SPACING = 24;

    private GameRules() {
    }

    public static int alienScoreForWave(int wave) {
        return wave * 10;
    }

    public static int alienKillBatchScore(int killCount, int wave, int comboMultiplier) {
        int boundedKillCount = Math.max(0, killCount);
        int boundedMultiplier = Math.clamp(comboMultiplier, 1, MAX_COMBO_MULTIPLIER);
        return boundedKillCount * alienScoreForWave(wave) * boundedMultiplier;
    }

    public static double alienSpeedForWave(int wave) {
        double speed = BASE_ALIEN_SPEED * Math.pow(1.15, wave - 1);
        return Math.min(speed, MAX_ALIEN_SPEED);
    }

    public static boolean hasSpecialAlien(int wave) {
        return wave >= 2 && wave % 2 == 0;
    }

    public static int specialAlienCount(int wave, int totalAlienCount) {
        return hasSpecialAlien(wave) && totalAlienCount > 0 ? 1 : 0;
    }

    public static double specialAlienSpeedMultiplier() {
        return SPECIAL_ALIEN_SPEED_MULTIPLIER;
    }

    public static double specialAlienDirectionChangeChance() {
        return SPECIAL_ALIEN_DIRECTION_CHANGE_CHANCE;
    }

    public static int alienFiringWeight(boolean specialAlien) {
        return specialAlien ? SPECIAL_ALIEN_FIRING_WEIGHT : 1;
    }

    public static double alienFireChanceForWave(int wave, int alienCount) {
        int specialAlienCount = specialAlienCount(wave, alienCount);
        int standardAlienCount = Math.max(0, alienCount - specialAlienCount);
        int totalWeight = standardAlienCount + specialAlienCount * alienFiringWeight(true);
        return 0.008 * Math.max(totalWeight, 1) / Math.max(alienCount, 1);
    }

    public static int maxLives() {
        return MAX_LIVES;
    }

    public static int speedBoostDurationTicks() {
        return SPEED_BOOST_DURATION_TICKS;
    }

    public static int speedBoostMoveStep() {
        return SPEED_BOOST_MOVE_STEP;
    }

    public static double supportDropChance() {
        return SUPPORT_DROP_CHANCE;
    }

    public static int supportDropWeight(PowerUpType type) {
        return switch (type) {
            case RAPID_FIRE -> RAPID_FIRE_DROP_WEIGHT;
            case EXTRA_LIFE -> EXTRA_LIFE_DROP_WEIGHT;
            case SHIELD -> SHIELD_DROP_WEIGHT;
            case SPEED_BOOST -> SPEED_BOOST_DROP_WEIGHT;
        };
    }

    public static PowerUpType supportDropTypeForRoll(int roll) {
        int remaining = roll;
        for (PowerUpType type : PowerUpType.values()) {
            remaining -= supportDropWeight(type);
            if (remaining < 0) {
                return type;
            }
        }
        return PowerUpType.RAPID_FIRE;
    }

    public static int totalSupportDropWeight() {
        int total = 0;
        for (PowerUpType type : PowerUpType.values()) {
            total += supportDropWeight(type);
        }
        return total;
    }

    public static boolean isBossWave(int wave) {
        return wave >= BOSS_WAVE_INTERVAL && wave % BOSS_WAVE_INTERVAL == 0;
    }

    public static int bossHealth() {
        return BOSS_HEALTH;
    }

    public static int bossScoreBonus() {
        return BOSS_SCORE_BONUS;
    }

    public static double bossHorizontalSpeed() {
        return BOSS_HORIZONTAL_SPEED;
    }

    public static int bossTopLaneY() {
        return BOSS_TOP_LANE_Y;
    }

    public static double bossDirectionChangeChance() {
        return BOSS_DIRECTION_CHANGE_CHANCE;
    }

    public static int bossRewardWeight(PowerUpType type) {
        return switch (type) {
            case EXTRA_LIFE -> EXTRA_LIFE_BOSS_REWARD_WEIGHT;
            case SHIELD -> SHIELD_BOSS_REWARD_WEIGHT;
            default -> 0;
        };
    }

    public static int totalBossRewardWeight() {
        return bossRewardWeight(PowerUpType.EXTRA_LIFE) + bossRewardWeight(PowerUpType.SHIELD);
    }

    public static PowerUpType bossRewardTypeForRoll(int roll) {
        if (roll < bossRewardWeight(PowerUpType.EXTRA_LIFE)) {
            return PowerUpType.EXTRA_LIFE;
        }
        return PowerUpType.SHIELD;
    }

    public static double bossFireChance() {
        return BOSS_FIRE_CHANCE;
    }

    public static int bossBurstCount() {
        return BOSS_BURST_COUNT;
    }

    public static int bossBurstSpacing() {
        return BOSS_BURST_SPACING;
    }
}
