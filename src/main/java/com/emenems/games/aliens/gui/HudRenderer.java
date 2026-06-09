package com.emenems.games.aliens.gui;

import com.emenems.games.aliens.GameConstants;
import com.emenems.games.aliens.GameState;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;

class HudRenderer {
    private static final int TICKS_PER_SECOND = 60;
    private static final int HUD_X = 16;
    private static final int HUD_Y = 16;
    private static final int HUD_WIDTH = 220;
    private static final int HUD_ROW_HEIGHT = 24;
    private static final int HUD_PADDING = 14;
    private static final int HUD_ARC = 18;
    private static final int WAVE_BANNER_Y = 78;

    void drawHud(Graphics g, GamePanelState state) {
        int hudHeight = hudCardHeight(state.rapidFireActive(), state.speedBoostActive(), state.comboMultiplier(), state.comboTicks());
        g.setColor(new Color(7, 14, 32, 118));
        g.fillRoundRect(HUD_X, HUD_Y, HUD_WIDTH, hudHeight, HUD_ARC, HUD_ARC);
        g.setColor(new Color(120, 210, 255, 72));
        g.drawRoundRect(HUD_X, HUD_Y, HUD_WIDTH, hudHeight, HUD_ARC, HUD_ARC);

        int textX = HUD_X + HUD_PADDING;
        int currentY = HUD_Y + HUD_PADDING + HUD_ROW_HEIGHT;
        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        g.drawString("Score: " + state.score(), textX, currentY);

        currentY += HUD_ROW_HEIGHT;
        g.drawString("Wave: " + state.wave(), textX, currentY);

        currentY += HUD_ROW_HEIGHT;
        g.drawString("Lives: " + state.lives(), textX, currentY);

        if (state.rapidFireActive()) {
            currentY += HUD_ROW_HEIGHT;
            g.setColor(new Color(255, 230, 60));
            g.drawString("RAPID FIRE: " + rapidFireSecondsRemaining(state.rapidFireTicks()) + "s", textX, currentY);
        }
        if (state.speedBoostActive()) {
            currentY += HUD_ROW_HEIGHT;
            g.setColor(new Color(120, 255, 150));
            g.drawString("SPEED: " + speedBoostSecondsRemaining(state.speedBoostTicks()) + "s", textX, currentY);
        }
        if (isComboVisible(state.comboMultiplier(), state.comboTicks())) {
            currentY += HUD_ROW_HEIGHT;
            g.setColor(new Color(80, 220, 255));
            g.drawString(
                "COMBO x" + state.comboMultiplier() + ": " + comboSecondsRemaining(state.comboTicks()) + "s",
                textX,
                currentY
            );
        }
    }

    void drawWaveMessage(Graphics g, GamePanelState state) {
        if (!isWaveMessageVisible(state.gameState(), state.waveMessageActive(), state.waveMessageTicks())) {
            return;
        }

        String text = "WAVE " + state.wave();
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
        int textWidth = g.getFontMetrics().stringWidth(text);
        int bannerWidth = textWidth + 36;
        int bannerHeight = 34;
        int bannerX = (GameConstants.PANEL_WIDTH - bannerWidth) / 2;
        int bannerY = WAVE_BANNER_Y - 24;

        g.setColor(new Color(8, 18, 48, 82));
        g.fillRoundRect(bannerX, bannerY, bannerWidth, bannerHeight, 20, 20);
        g.setColor(new Color(255, 214, 92, 70));
        g.drawRoundRect(bannerX, bannerY, bannerWidth, bannerHeight, 20, 20);
        g.setColor(new Color(255, 244, 196, 185));
        drawCenteredString(g, text, WAVE_BANNER_Y);
    }

    static int hudCardHeight(boolean rapidFireActive, boolean speedBoostActive, int comboMultiplier, int comboTicks) {
        int visibleRows = 3;
        if (rapidFireActive) {
            visibleRows++;
        }
        if (speedBoostActive) {
            visibleRows++;
        }
        if (isComboVisible(comboMultiplier, comboTicks)) {
            visibleRows++;
        }
        return HUD_PADDING * 2 + visibleRows * HUD_ROW_HEIGHT;
    }

    static int rapidFireSecondsRemaining(int ticks) {
        return Math.ceilDiv(ticks, TICKS_PER_SECOND);
    }

    static int comboSecondsRemaining(int ticks) {
        return Math.ceilDiv(ticks, TICKS_PER_SECOND);
    }

    static int speedBoostSecondsRemaining(int ticks) {
        return Math.ceilDiv(ticks, TICKS_PER_SECOND);
    }

    static boolean isComboVisible(int multiplier, int ticks) {
        return multiplier >= 2 && ticks > 0;
    }

    static boolean isWaveMessageVisible(GameState gameState, boolean active, int ticks) {
        return gameState == GameState.PLAYING && active && ticks > 0;
    }

    private static void drawCenteredString(Graphics g, String text, int y) {
        int textWidth = g.getFontMetrics().stringWidth(text);
        g.drawString(text, (GameConstants.PANEL_WIDTH - textWidth) / 2, y);
    }
}
