package com.emenems.games.aliens.gui;

import com.emenems.games.aliens.GameConstants;
import com.emenems.games.aliens.GameState;
import com.emenems.games.aliens.profiles.ProfileMenuState;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;

class MenuRenderer {

    void drawStartMenu(Graphics g, GamePanelState state) {
        if (state.gameState() != GameState.START_MENU) {
            return;
        }

        ProfileMenuState profileMenuState = state.profileMenuState();
        g.setColor(new Color(0, 0, 0, 185));
        g.fillRect(0, 0, GameConstants.PANEL_WIDTH, GameConstants.PANEL_HEIGHT);

        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 52));
        drawCenteredString(g, "ALIENS ATTACK", GameConstants.PANEL_HEIGHT / 2 - 150);

        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
        drawCenteredString(g, profileMenuState.startPromptText(), GameConstants.PANEL_HEIGHT / 2 - 95);

        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
        if (profileMenuState.hasSelectedProfile()) {
            drawCenteredString(
                g,
                "Profile: " + profileMenuState.selectedProfileName() + "    " + profileMenuState.bestScoreText(),
                GameConstants.PANEL_HEIGHT / 2 - 50
            );
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 18));
            drawCenteredString(g, profileMenuState.profileCounterText(), GameConstants.PANEL_HEIGHT / 2 - 20);
        } else {
            drawCenteredString(g, "No profile selected", GameConstants.PANEL_HEIGHT / 2 - 50);
        }

        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 18));
        if (profileMenuState.inputMode()) {
            drawCenteredString(
                g,
                "Name: " + profileMenuState.draftName() + "_",
                GameConstants.PANEL_HEIGHT / 2 + 18
            );
            drawCenteredString(g, "ENTER saves    ESC cancels", GameConstants.PANEL_HEIGHT / 2 + 48);
        } else {
            drawCenteredString(g, "Left/Right select    N creates profile", GameConstants.PANEL_HEIGHT / 2 + 18);
        }

        if (!profileMenuState.message().isBlank()) {
            g.setColor(isSaveWarningVisible(profileMenuState) ? new Color(255, 150, 100) : new Color(255, 230, 120));
            drawCenteredString(g, profileMenuState.message(), GameConstants.PANEL_HEIGHT / 2 + 86);
            g.setColor(Color.WHITE);
        }

        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 18));
        drawCenteredString(g, "Arrow keys move    Hold Space to fire    P pauses", GameConstants.PANEL_HEIGHT / 2 + 132);
    }

    void drawGameOver(Graphics g, GamePanelState state) {
        if (state.gameState() != GameState.GAME_OVER) {
            return;
        }

        ProfileMenuState profileMenuState = state.profileMenuState();
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRect(0, 0, GameConstants.PANEL_WIDTH, GameConstants.PANEL_HEIGHT);

        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 48));
        drawCenteredString(g, state.gameOverTitle(), 170);

        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
        drawCenteredString(g, "Final Score: " + state.score(), 230);
        if (profileMenuState.hasSelectedProfile()) {
            drawCenteredString(
                g,
                "Profile: " + profileMenuState.selectedProfileName() + "    " + profileMenuState.bestScoreText(),
                264
            );
        }
        int currentY = 296;
        if (isNewBestScoreVisible(profileMenuState)) {
            g.setColor(new Color(255, 230, 60));
            drawCenteredString(g, "New Best Score!", currentY);
            g.setColor(Color.WHITE);
            currentY += 30;
        }
        if (isSaveWarningVisible(profileMenuState)) {
            g.setColor(new Color(255, 150, 100));
            drawCenteredString(g, "Profile save failed", currentY);
            g.setColor(Color.WHITE);
            currentY += 30;
        }
        drawLeaderboard(g, profileMenuState, Math.max(currentY + 12, 326));
        drawCenteredString(g, "Press ENTER to Restart", 560);
    }

    void drawPausedOverlay(Graphics g, GamePanelState state) {
        if (!isPausedOverlayVisible(state.gameState())) {
            return;
        }

        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(0, 0, GameConstants.PANEL_WIDTH, GameConstants.PANEL_HEIGHT);

        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 48));
        drawCenteredString(g, "PAUSED", GameConstants.PANEL_HEIGHT / 2 - 30);

        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
        drawCenteredString(g, "Press P to Resume", GameConstants.PANEL_HEIGHT / 2 + 20);
    }

    private void drawLeaderboard(Graphics g, ProfileMenuState profileMenuState, int startY) {
        if (!isLeaderboardVisible(profileMenuState)) {
            return;
        }

        g.setColor(new Color(255, 230, 120));
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        drawCenteredString(g, "TOP 5", startY);
        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 18));
        int rowY = startY + 28;
        for (ProfileMenuState.LeaderboardEntry entry : profileMenuState.topProfiles()) {
            drawCenteredString(g, leaderboardRowText(entry), rowY);
            rowY += 24;
        }
    }

    static boolean isPausedOverlayVisible(GameState gameState) {
        return gameState == GameState.PAUSED;
    }

    static boolean isNewBestScoreVisible(ProfileMenuState profileMenuState) {
        return profileMenuState.newBestScore() && profileMenuState.hasSelectedProfile();
    }

    static boolean isSaveWarningVisible(ProfileMenuState profileMenuState) {
        return profileMenuState.saveFailed();
    }

    static boolean isLeaderboardVisible(ProfileMenuState profileMenuState) {
        return !profileMenuState.topProfiles().isEmpty();
    }

    static String leaderboardRowText(ProfileMenuState.LeaderboardEntry entry) {
        return entry.rank() + ". " + entry.name() + "  " + entry.bestScore();
    }

    private static void drawCenteredString(Graphics g, String text, int y) {
        int textWidth = g.getFontMetrics().stringWidth(text);
        g.drawString(text, (GameConstants.PANEL_WIDTH - textWidth) / 2, y);
    }
}
