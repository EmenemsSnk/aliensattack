package com.emenems.games.aliens.gui;

import com.emenems.games.aliens.GameConstants;
import com.emenems.games.aliens.GameRules;
import com.emenems.games.aliens.GameState;
import com.emenems.games.aliens.gamemachines.Alien;
import com.emenems.games.aliens.gamemachines.AlienExplosion;
import com.emenems.games.aliens.gamemachines.AlienMissile;
import com.emenems.games.aliens.gamemachines.AlienType;
import com.emenems.games.aliens.gamemachines.Missile;
import com.emenems.games.aliens.gamemachines.PowerUp;
import com.emenems.games.aliens.gamemachines.PowerUpType;
import com.emenems.games.aliens.gamemachines.Spaceship;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JPanel;

class EntityRenderer {
    private static final int BOSS_RENDER_WIDTH = 66;
    private static final int BOSS_RENDER_HEIGHT = 44;
    private static final int BOSS_HEALTH_BAR_WIDTH = 220;
    private static final int BOSS_HEALTH_BAR_HEIGHT = 14;

    private final Spaceship spaceship;
    private final List<Missile> missiles;
    private final List<AlienMissile> alienMissiles;
    private final List<Alien> aliens;
    private final List<AlienExplosion> alienExplosions;
    private final List<PowerUp> powerUps;
    private final JPanel panel;
    private final Image alienImage;
    private final Image specialAlienImage;
    private final Image alienMissileImage;
    private final Image missileImage;
    private final Image spaceshipImage;
    private final Image explosionImage;

    EntityRenderer(
        Spaceship spaceship,
        List<Missile> missiles,
        List<AlienMissile> alienMissiles,
        List<Alien> aliens,
        List<AlienExplosion> alienExplosions,
        List<PowerUp> powerUps,
        JPanel panel
    ) {
        this.spaceship = spaceship;
        this.missiles = missiles;
        this.alienMissiles = alienMissiles;
        this.aliens = aliens;
        this.alienExplosions = alienExplosions;
        this.powerUps = powerUps;
        this.panel = panel;
        alienImage = new ImageIcon(getClass().getResource("/images/alien.png")).getImage();
        specialAlienImage = new ImageIcon(getClass().getResource("/images/special-alien.png")).getImage();
        alienMissileImage = new ImageIcon(getClass().getResource("/images/alien-missile.png")).getImage();
        missileImage = new ImageIcon(getClass().getResource("/images/missile.gif")).getImage();
        spaceshipImage = new ImageIcon(getClass().getResource("/images/spaceship.png")).getImage();
        explosionImage = new ImageIcon(getClass().getResource("/images/explosion.png")).getImage();
    }

    void draw(Graphics g, GamePanelState state) {
        drawSpaceship(g, state);
        drawAliens(g);
        drawMissiles(g);
        drawAlienMissiles(g);
        drawAlienExplosions(g);
        drawPowerUps(g);
        drawBossHealthBar(g, state);
    }

    void drawHitFeedback(Graphics g, GamePanelState state) {
        if (!state.hitFeedbackActive()) {
            return;
        }

        g.setColor(new Color(255, 0, 0, 70));
        g.fillRect(0, 0, GameConstants.PANEL_WIDTH, GameConstants.PANEL_HEIGHT);
        g.setColor(new Color(255, 80, 80, 220));
        g.drawRect(2, 2, GameConstants.PANEL_WIDTH - 5, GameConstants.PANEL_HEIGHT - 5);
        g.drawRect(5, 5, GameConstants.PANEL_WIDTH - 11, GameConstants.PANEL_HEIGHT - 11);
    }

    private void drawSpaceship(Graphics g, GamePanelState state) {
        g.drawImage(spaceshipImage, spaceship.getX(), spaceship.getY(), GameConstants.COMPONENT_SIZE, GameConstants.COMPONENT_SIZE, panel);
        if (isSpaceshipShieldVisible(state.gameState(), state.shieldActive())) {
            drawSpaceshipShield(g);
        }
    }

    private void drawSpaceshipShield(Graphics g) {
        int x = spaceship.getX();
        int y = spaceship.getY();
        g.setColor(new Color(90, 240, 255, 60));
        g.fillOval(x - 7, y - 7, GameConstants.COMPONENT_SIZE + 14, GameConstants.COMPONENT_SIZE + 14);
        g.setColor(new Color(160, 250, 255, 180));
        g.drawOval(x - 5, y - 5, GameConstants.COMPONENT_SIZE + 10, GameConstants.COMPONENT_SIZE + 10);
        g.setColor(new Color(255, 255, 255, 170));
        g.drawArc(x - 2, y - 7, GameConstants.COMPONENT_SIZE + 4, GameConstants.COMPONENT_SIZE + 4, 18, 132);
    }

    private void drawAliens(Graphics g) {
        aliens.forEach(alien -> {
            if (alien.isBoss()) {
                drawBoss(g, alien);
            } else if (isShieldedSpecialAlien(alien)) {
                g.drawImage(alienImageFor(alien), alien.getX(), alien.getY(), GameConstants.COMPONENT_SIZE, GameConstants.COMPONENT_SIZE, panel);
                drawSpecialAlienShield(g, alien);
            } else {
                g.drawImage(alienImageFor(alien), alien.getX(), alien.getY(), GameConstants.COMPONENT_SIZE, GameConstants.COMPONENT_SIZE, panel);
            }
        });
    }

    private void drawSpecialAlienShield(Graphics g, Alien alien) {
        int x = alien.getX();
        int y = alien.getY();
        g.setColor(new Color(90, 240, 255, 70));
        g.fillOval(x - 4, y - 4, GameConstants.COMPONENT_SIZE + 8, GameConstants.COMPONENT_SIZE + 8);
        g.setColor(new Color(160, 250, 255, 180));
        g.drawOval(x - 3, y - 3, GameConstants.COMPONENT_SIZE + 6, GameConstants.COMPONENT_SIZE + 6);
        g.setColor(new Color(255, 255, 255, 180));
        g.drawArc(x - 1, y - 5, GameConstants.COMPONENT_SIZE + 2, GameConstants.COMPONENT_SIZE + 2, 25, 120);
    }

    private void drawBoss(Graphics g, Alien boss) {
        int x = boss.getX() - (BOSS_RENDER_WIDTH - GameConstants.COMPONENT_SIZE) / 2;
        int y = boss.getY() - 6;
        g.setColor(new Color(95, 18, 22, 220));
        g.fillRoundRect(x + 8, y + 8, BOSS_RENDER_WIDTH - 16, BOSS_RENDER_HEIGHT - 12, 18, 18);
        g.setColor(new Color(196, 46, 58, 235));
        g.fillRoundRect(x + 2, y + 4, BOSS_RENDER_WIDTH - 4, BOSS_RENDER_HEIGHT - 18, 20, 20);
        g.setColor(new Color(255, 202, 112, 235));
        g.fillRect(x + 10, y + 10, BOSS_RENDER_WIDTH - 20, 6);
        g.setColor(new Color(32, 10, 14, 220));
        g.fillOval(x + 12, y + 18, 14, 14);
        g.fillOval(x + BOSS_RENDER_WIDTH - 26, y + 18, 14, 14);
        g.setColor(new Color(255, 240, 180));
        g.fillOval(x + 16, y + 22, 6, 6);
        g.fillOval(x + BOSS_RENDER_WIDTH - 22, y + 22, 6, 6);
        g.setColor(new Color(255, 150, 120));
        g.fillRoundRect(x + 18, y + 30, BOSS_RENDER_WIDTH - 36, 6, 6, 6);
        g.setColor(new Color(255, 220, 160));
        g.drawRoundRect(x + 2, y + 4, BOSS_RENDER_WIDTH - 4, BOSS_RENDER_HEIGHT - 18, 20, 20);
        g.drawLine(x + 4, y + BOSS_RENDER_HEIGHT - 10, x + 14, y + BOSS_RENDER_HEIGHT);
        g.drawLine(x + BOSS_RENDER_WIDTH - 4, y + BOSS_RENDER_HEIGHT - 10, x + BOSS_RENDER_WIDTH - 14, y + BOSS_RENDER_HEIGHT);
    }

    private void drawMissiles(Graphics g) {
        missiles.forEach(missile -> g.drawImage(missileImage, missile.getX(), missile.getY(), GameConstants.COMPONENT_SIZE, GameConstants.COMPONENT_SIZE, panel));
    }

    private void drawAlienMissiles(Graphics g) {
        alienMissiles.forEach(missile ->
            g.drawImage(alienMissileImage, missile.getX(), missile.getY(), GameConstants.COMPONENT_SIZE, GameConstants.COMPONENT_SIZE, panel));
    }

    private void drawAlienExplosions(Graphics g) {
        alienExplosions.forEach(explosion ->
            g.drawImage(explosionImage, explosion.getX(), explosion.getY(), GameConstants.COMPONENT_SIZE, GameConstants.COMPONENT_SIZE, panel));
    }

    private void drawPowerUps(Graphics g) {
        powerUps.forEach(powerUp -> drawPowerUp(g, powerUp));
    }

    private void drawPowerUp(Graphics g, PowerUp powerUp) {
        int inset = 5;
        int diameter = GameConstants.COMPONENT_SIZE - inset * 2;
        g.setColor(powerUpColor(powerUp.getType()));
        g.fillOval(powerUp.getX() + inset, powerUp.getY() + inset, diameter, diameter);
        g.setColor(new Color(16, 24, 40));
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        g.drawString(powerUpLabel(powerUp.getType()), powerUp.getX() + 14, powerUp.getY() + 28);
    }

    private void drawBossHealthBar(Graphics g, GamePanelState state) {
        Alien boss = currentBoss(aliens);
        if (!isBossHealthBarVisible(state.gameState(), boss)) {
            return;
        }

        int barX = (GameConstants.PANEL_WIDTH - BOSS_HEALTH_BAR_WIDTH) / 2;
        int barY = 18;
        g.setColor(new Color(25, 16, 16, 180));
        g.fillRoundRect(barX, barY, BOSS_HEALTH_BAR_WIDTH, BOSS_HEALTH_BAR_HEIGHT, 12, 12);
        g.setColor(new Color(255, 110, 110));
        g.fillRoundRect(
            barX,
            barY,
            bossHealthBarFillWidth(boss.getHealth(), GameRules.bossHealth(), BOSS_HEALTH_BAR_WIDTH),
            BOSS_HEALTH_BAR_HEIGHT,
            12,
            12
        );
        g.setColor(new Color(255, 220, 220));
        g.drawRoundRect(barX, barY, BOSS_HEALTH_BAR_WIDTH, BOSS_HEALTH_BAR_HEIGHT, 12, 12);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        g.drawString("BOSS", barX, barY - 4);
    }

    private Image alienImageFor(Alien alien) {
        return alien.getType() == AlienType.SPECIAL ? specialAlienImage : alienImage;
    }

    static boolean isShieldedSpecialAlien(Alien alien) {
        return alien.getType() == AlienType.SPECIAL && !alien.isDamaged();
    }

    static Alien currentBoss(List<Alien> aliens) {
        return aliens.stream().filter(Alien::isBoss).findFirst().orElse(null);
    }

    static boolean isBossHealthBarVisible(GameState gameState, Alien boss) {
        return gameState == GameState.PLAYING && boss != null;
    }

    static int bossHealthBarFillWidth(int currentHealth, int maxHealth, int totalWidth) {
        if (maxHealth <= 0) {
            return 0;
        }
        int clampedHealth = Math.max(0, Math.min(currentHealth, maxHealth));
        return (int) Math.round((clampedHealth / (double) maxHealth) * totalWidth);
    }

    static boolean isSpaceshipShieldVisible(GameState gameState, boolean shieldActive) {
        return shieldActive && (gameState == GameState.PLAYING || gameState == GameState.PAUSED);
    }

    static int bossRenderWidth() {
        return BOSS_RENDER_WIDTH;
    }

    static int bossRenderHeight() {
        return BOSS_RENDER_HEIGHT;
    }

    static Color powerUpColor(PowerUpType type) {
        return switch (type) {
            case RAPID_FIRE -> new Color(255, 220, 35, 220);
            case EXTRA_LIFE -> new Color(255, 120, 120, 220);
            case SHIELD -> new Color(120, 240, 255, 220);
            case SPEED_BOOST -> new Color(120, 255, 150, 220);
        };
    }

    static String powerUpLabel(PowerUpType type) {
        return switch (type) {
            case RAPID_FIRE -> "R";
            case EXTRA_LIFE -> "+";
            case SHIELD -> "S";
            case SPEED_BOOST -> ">";
        };
    }

    static boolean hasExplosionSprite() {
        return EntityRenderer.class.getResource("/images/explosion.png") != null;
    }
}
