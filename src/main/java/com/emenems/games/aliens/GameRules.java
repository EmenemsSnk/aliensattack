package com.emenems.games.aliens;

public final class GameRules {
    public static final int MAX_COMBO_MULTIPLIER = 5;
    private static final double BASE_ALIEN_SPEED = 0.8;
    private static final double MAX_ALIEN_SPEED = 2.8;
    private static final double SPECIAL_ALIEN_SPEED_MULTIPLIER = 1.2;
    private static final double SPECIAL_ALIEN_DIRECTION_CHANGE_CHANCE = 0.02;
    private static final int SPECIAL_ALIEN_FIRING_WEIGHT = 2;

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
}
