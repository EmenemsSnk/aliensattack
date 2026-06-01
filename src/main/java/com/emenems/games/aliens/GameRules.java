package com.emenems.games.aliens;

public final class GameRules {
    private static final double BASE_ALIEN_SPEED = 0.8;
    private static final double MAX_ALIEN_SPEED = 2.8;

    private GameRules() {
    }

    public static int alienScoreForWave(int wave) {
        return wave * 10;
    }

    public static double alienSpeedForWave(int wave) {
        double speed = BASE_ALIEN_SPEED * Math.pow(1.15, wave - 1);
        return Math.min(speed, MAX_ALIEN_SPEED);
    }
}
