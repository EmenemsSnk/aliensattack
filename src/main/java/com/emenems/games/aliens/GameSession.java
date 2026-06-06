package com.emenems.games.aliens;

public final class GameSession {
    private static final int DEFAULT_LIVES = 3;
    private static final int HIT_FEEDBACK_TICKS = 18;
    public static final int WAVE_MESSAGE_DURATION_TICKS = 72;
    public static final int RAPID_FIRE_DURATION_TICKS = 180;
    public static final int COMBO_DURATION_TICKS = 90;
    private static final String DEFAULT_GAME_OVER_TITLE = "GAME OVER";
    private static final String ALIENS_WIN_TITLE = "ALIENS WIN";

    private int score;
    private int wave = 1;
    private int lives = DEFAULT_LIVES;
    private GameState gameState = GameState.START_MENU;
    private int hitFeedbackTicks;
    private int waveMessageTicks;
    private int rapidFireTicks;
    private int comboMultiplier = 1;
    private int comboTicks;
    private String gameOverTitle = DEFAULT_GAME_OVER_TITLE;

    public int getScore() {
        return score;
    }

    public int getWave() {
        return wave;
    }

    public int getLives() {
        return lives;
    }

    public GameState getGameState() {
        return gameState;
    }

    public boolean isHitFeedbackActive() {
        return hitFeedbackTicks > 0;
    }

    public boolean isWaveMessageActive() {
        return waveMessageTicks > 0;
    }

    public int getWaveMessageTicks() {
        return waveMessageTicks;
    }

    public String getGameOverTitle() {
        return gameOverTitle;
    }

    public boolean isRapidFireActive() {
        return rapidFireTicks > 0;
    }

    public int getRapidFireTicks() {
        return rapidFireTicks;
    }

    public int getComboMultiplier() {
        return comboMultiplier;
    }

    public int getComboTicks() {
        return comboTicks;
    }

    public boolean isComboActive() {
        return comboTicks > 0;
    }

    public void startOrRestart() {
        score = 0;
        wave = 1;
        lives = DEFAULT_LIVES;
        gameState = GameState.PLAYING;
        hitFeedbackTicks = 0;
        waveMessageTicks = WAVE_MESSAGE_DURATION_TICKS;
        rapidFireTicks = 0;
        resetCombo();
        gameOverTitle = DEFAULT_GAME_OVER_TITLE;
    }

    public void pause() {
        if (gameState == GameState.PLAYING) {
            gameState = GameState.PAUSED;
        }
    }

    public void resume() {
        if (gameState == GameState.PAUSED) {
            gameState = GameState.PLAYING;
        }
    }

    public void addAlienKills(int count) {
        if (count <= 0) {
            return;
        }

        if (isComboActive()) {
            comboMultiplier = Math.min(comboMultiplier + 1, GameRules.MAX_COMBO_MULTIPLIER);
        } else {
            comboMultiplier = 1;
        }
        comboTicks = COMBO_DURATION_TICKS;
        score += GameRules.alienKillBatchScore(count, wave, comboMultiplier);
    }

    public void advanceWave() {
        wave++;
        resetCombo();
        waveMessageTicks = WAVE_MESSAGE_DURATION_TICKS;
    }

    public void loseLife() {
        hitFeedbackTicks = HIT_FEEDBACK_TICKS;
        rapidFireTicks = 0;
        resetCombo();
        lives--;
        if (lives <= 0) {
            lives = 0;
            enterGameOver(DEFAULT_GAME_OVER_TITLE);
        }
    }

    public void enterAlienInvasionGameOver() {
        enterGameOver(ALIENS_WIN_TITLE);
    }

    public void tickHitFeedback() {
        if (hitFeedbackTicks > 0) {
            hitFeedbackTicks--;
        }
    }

    public void activateRapidFire() {
        rapidFireTicks = RAPID_FIRE_DURATION_TICKS;
    }

    public void tickWaveMessage() {
        if (waveMessageTicks > 0) {
            waveMessageTicks--;
        }
    }

    public void tickRapidFire() {
        if (rapidFireTicks > 0) {
            rapidFireTicks--;
        }
    }

    public void tickCombo() {
        if (comboTicks <= 0) {
            return;
        }

        comboTicks--;
        if (comboTicks == 0) {
            comboMultiplier = 1;
        }
    }

    private void resetCombo() {
        comboMultiplier = 1;
        comboTicks = 0;
    }

    private void enterGameOver(String title) {
        gameState = GameState.GAME_OVER;
        gameOverTitle = title;
    }
}
