package com.emenems.games.aliens.gui;

import com.emenems.games.aliens.GameConstants;
import com.emenems.games.aliens.gamemachines.Alien;
import com.emenems.games.aliens.gamemachines.AlienExplosion;
import com.emenems.games.aliens.gamemachines.AlienMissile;
import com.emenems.games.aliens.gamemachines.Missile;
import com.emenems.games.aliens.gamemachines.PowerUp;
import com.emenems.games.aliens.gamemachines.Spaceship;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JPanel;

public class GamePanel extends JPanel {
    private final Image space;
    private final HudRenderer hudRenderer;
    private final MenuRenderer menuRenderer;
    private final EntityRenderer entityRenderer;
    private GamePanelState state = GamePanelState.initial();

    public GamePanel(Spaceship spaceship, List<Missile> missiles, List<AlienMissile> alienMissiles, List<Alien> aliens) {
        this(spaceship, missiles, alienMissiles, aliens, new ArrayList<>(), new ArrayList<>());
    }

    public GamePanel(
        Spaceship spaceship,
        List<Missile> missiles,
        List<AlienMissile> alienMissiles,
        List<Alien> aliens,
        List<AlienExplosion> alienExplosions,
        List<PowerUp> powerUps
    ) {
        hudRenderer = new HudRenderer();
        menuRenderer = new MenuRenderer();
        entityRenderer = new EntityRenderer(spaceship, missiles, alienMissiles, aliens, alienExplosions, powerUps, this);
        space = new ImageIcon(getClass().getResource("/images/space.jpeg")).getImage();
        setPreferredSize(new Dimension(GameConstants.PANEL_WIDTH, GameConstants.PANEL_HEIGHT));
        setMinimumSize(new Dimension(GameConstants.PANEL_WIDTH, GameConstants.PANEL_HEIGHT));
        setMaximumSize(new Dimension(GameConstants.PANEL_WIDTH, GameConstants.PANEL_HEIGHT));
        setFocusable(true);
    }

    public GamePanel(
        Spaceship spaceship,
        List<Missile> missiles,
        List<AlienMissile> alienMissiles,
        List<Alien> aliens,
        List<PowerUp> powerUps
    ) {
        this(spaceship, missiles, alienMissiles, aliens, new ArrayList<>(), powerUps);
    }

    public void updateGameState(GamePanelState newState) {
        this.state = newState;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(space, 0, 0, null);
        entityRenderer.draw(g, state);
        hudRenderer.drawHud(g, state);
        hudRenderer.drawWaveMessage(g, state);
        entityRenderer.drawHitFeedback(g, state);
        menuRenderer.drawPausedOverlay(g, state);
        menuRenderer.drawStartMenu(g, state);
        menuRenderer.drawGameOver(g, state);
        Toolkit.getDefaultToolkit().sync();
    }

    // Delegation stubs so existing call sites (GamePanelTest) compile without change

    static int hudCardHeight(boolean rapidFireActive, boolean speedBoostActive, int comboMultiplier, int comboTicks) {
        return HudRenderer.hudCardHeight(rapidFireActive, speedBoostActive, comboMultiplier, comboTicks);
    }

    static int rapidFireSecondsRemaining(int ticks) {
        return HudRenderer.rapidFireSecondsRemaining(ticks);
    }

    static int comboSecondsRemaining(int ticks) {
        return HudRenderer.comboSecondsRemaining(ticks);
    }

    static int speedBoostSecondsRemaining(int ticks) {
        return HudRenderer.speedBoostSecondsRemaining(ticks);
    }

    static boolean isComboVisible(int multiplier, int ticks) {
        return HudRenderer.isComboVisible(multiplier, ticks);
    }

    static boolean isWaveMessageVisible(com.emenems.games.aliens.GameState gameState, boolean active, int ticks) {
        return HudRenderer.isWaveMessageVisible(gameState, active, ticks);
    }

    static boolean isPausedOverlayVisible(com.emenems.games.aliens.GameState gameState) {
        return MenuRenderer.isPausedOverlayVisible(gameState);
    }

    static boolean isNewBestScoreVisible(com.emenems.games.aliens.profiles.ProfileMenuState profileMenuState) {
        return MenuRenderer.isNewBestScoreVisible(profileMenuState);
    }

    static boolean isSaveWarningVisible(com.emenems.games.aliens.profiles.ProfileMenuState profileMenuState) {
        return MenuRenderer.isSaveWarningVisible(profileMenuState);
    }

    static boolean isLeaderboardVisible(com.emenems.games.aliens.profiles.ProfileMenuState profileMenuState) {
        return MenuRenderer.isLeaderboardVisible(profileMenuState);
    }

    static String leaderboardRowText(com.emenems.games.aliens.profiles.ProfileMenuState.LeaderboardEntry entry) {
        return MenuRenderer.leaderboardRowText(entry);
    }

    static boolean isShieldedSpecialAlien(Alien alien) {
        return EntityRenderer.isShieldedSpecialAlien(alien);
    }

    static Alien currentBoss(List<Alien> aliens) {
        return EntityRenderer.currentBoss(aliens);
    }

    static boolean isBossHealthBarVisible(com.emenems.games.aliens.GameState gameState, Alien boss) {
        return EntityRenderer.isBossHealthBarVisible(gameState, boss);
    }

    static int bossHealthBarFillWidth(int currentHealth, int maxHealth, int totalWidth) {
        return EntityRenderer.bossHealthBarFillWidth(currentHealth, maxHealth, totalWidth);
    }

    static boolean isSpaceshipShieldVisible(com.emenems.games.aliens.GameState gameState, boolean shieldActive) {
        return EntityRenderer.isSpaceshipShieldVisible(gameState, shieldActive);
    }

    static int bossRenderWidth() {
        return EntityRenderer.bossRenderWidth();
    }

    static int bossRenderHeight() {
        return EntityRenderer.bossRenderHeight();
    }

    static java.awt.Color powerUpColor(com.emenems.games.aliens.gamemachines.PowerUpType type) {
        return EntityRenderer.powerUpColor(type);
    }

    static String powerUpLabel(com.emenems.games.aliens.gamemachines.PowerUpType type) {
        return EntityRenderer.powerUpLabel(type);
    }

    static boolean hasExplosionSprite() {
        return EntityRenderer.hasExplosionSprite();
    }
}
