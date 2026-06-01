package com.emenems.games.aliens;

public final class GameSession {
    private static final int DEFAULT_LIVES = 3;
    private static final int HIT_FEEDBACK_TICKS = 18;
    private static final String DEFAULT_GAME_OVER_TITLE = "GAME OVER";
    private static final String ALIENS_WIN_TITLE = "ALIENS WIN";

    private int score;
    private int wave = 1;
    private int lives = DEFAULT_LIVES;
    private GameState gameState = GameState.START_MENU;
    private int hitFeedbackTicks;
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

    public String getGameOverTitle() {
        return gameOverTitle;
    }

    public void startOrRestart() {
        score = 0;
        wave = 1;
        lives = DEFAULT_LIVES;
        gameState = GameState.PLAYING;
        hitFeedbackTicks = 0;
        gameOverTitle = DEFAULT_GAME_OVER_TITLE;
    }

    public void addAlienKills(int count) {
        score += count * GameRules.alienScoreForWave(wave);
    }

    public void advanceWave() {
        wave++;
    }

    public void loseLife() {
        hitFeedbackTicks = HIT_FEEDBACK_TICKS;
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

    private void enterGameOver(String title) {
        gameState = GameState.GAME_OVER;
        gameOverTitle = title;
    }
}
