package com.emenems.games.aliens.gui;

import com.emenems.games.aliens.GameState;
import com.emenems.games.aliens.profiles.ProfileMenuState;

public record GamePanelState(
    int score,
    int wave,
    int lives,
    GameState gameState,
    boolean hitFeedbackActive,
    String gameOverTitle,
    boolean waveMessageActive,
    int waveMessageTicks,
    boolean rapidFireActive,
    int rapidFireTicks,
    boolean shieldActive,
    boolean speedBoostActive,
    int speedBoostTicks,
    int comboMultiplier,
    int comboTicks,
    ProfileMenuState profileMenuState
) {
    static GamePanelState initial() {
        return new GamePanelState(
            0, 1, 3,
            GameState.START_MENU,
            false, "GAME OVER",
            false, 0,
            false, 0,
            false,
            false, 0,
            1, 0,
            ProfileMenuState.empty()
        );
    }
}
