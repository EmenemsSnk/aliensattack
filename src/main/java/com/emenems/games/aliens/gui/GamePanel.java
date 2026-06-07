package com.emenems.games.aliens.gui;

import com.emenems.games.aliens.GameConstants;
import com.emenems.games.aliens.GameRules;
import com.emenems.games.aliens.GameState;
import com.emenems.games.aliens.gamemachines.Alien;
import com.emenems.games.aliens.gamemachines.AlienExplosion;
import com.emenems.games.aliens.gamemachines.AlienType;
import com.emenems.games.aliens.gamemachines.AlienMissile;
import com.emenems.games.aliens.gamemachines.Missile;
import com.emenems.games.aliens.gamemachines.PowerUp;
import com.emenems.games.aliens.gamemachines.PowerUpType;
import com.emenems.games.aliens.gamemachines.Spaceship;
import com.emenems.games.aliens.profiles.ProfileMenuState;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JPanel;

public class GamePanel extends JPanel {
    private static final int TICKS_PER_SECOND = 60;
    private static final int HUD_X = 16;
    private static final int HUD_Y = 16;
    private static final int HUD_WIDTH = 220;
    private static final int HUD_ROW_HEIGHT = 24;
    private static final int HUD_PADDING = 14;
    private static final int HUD_ARC = 18;
    private static final int WAVE_BANNER_Y = 78;
    private static final int BOSS_HEALTH_BAR_WIDTH = 220;
    private static final int BOSS_HEALTH_BAR_HEIGHT = 14;
    private static final int BOSS_RENDER_WIDTH = 66;
    private static final int BOSS_RENDER_HEIGHT = 44;

    private Image space;
    private Image alienImage;
    private Image specialAlienImage;
    private Image alienMissileImage;
    private Image missileImage;
    private Image spaceshipImage;
    private Image explosionImage;
    private Spaceship spaceship;
    private List<Missile> missiles;
    private List<AlienMissile> alienMissiles;
    private List<Alien> aliens;
    private List<AlienExplosion> alienExplosions;
    private List<PowerUp> powerUps;
    private int score;
    private int wave = 1;
    private int lives = 3;
    private GameState gameState = GameState.PLAYING;
    private boolean hitFeedbackActive;
    private String gameOverTitle = "GAME OVER";
    private boolean waveMessageActive;
    private int waveMessageTicks;
    private boolean rapidFireActive;
    private int rapidFireTicks;
    private boolean shieldActive;
    private boolean speedBoostActive;
    private int speedBoostTicks;
    private int comboMultiplier = 1;
    private int comboTicks;
    private ProfileMenuState profileMenuState = ProfileMenuState.empty();

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
        this.spaceship = spaceship;
        this.missiles = missiles;
        this.alienMissiles = alienMissiles;
        this.aliens = aliens;
        this.alienExplosions = alienExplosions;
        this.powerUps = powerUps;
        initBoard();
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

    public void updateGameState(
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
        this.score = score;
        this.wave = wave;
        this.lives = lives;
        this.gameState = gameState;
        this.hitFeedbackActive = hitFeedbackActive;
        this.gameOverTitle = gameOverTitle;
        this.waveMessageActive = waveMessageActive;
        this.waveMessageTicks = waveMessageTicks;
        this.rapidFireActive = rapidFireActive;
        this.rapidFireTicks = rapidFireTicks;
        this.shieldActive = shieldActive;
        this.speedBoostActive = speedBoostActive;
        this.speedBoostTicks = speedBoostTicks;
        this.comboMultiplier = comboMultiplier;
        this.comboTicks = comboTicks;
        this.profileMenuState = profileMenuState;
    }

    private void initBoard() {
        loadImages();
        setPreferredSize(new Dimension(GameConstants.PANEL_WIDTH, GameConstants.PANEL_HEIGHT));
        setMinimumSize(new Dimension(GameConstants.PANEL_WIDTH, GameConstants.PANEL_HEIGHT));
        setMaximumSize(new Dimension(GameConstants.PANEL_WIDTH, GameConstants.PANEL_HEIGHT));
        setFocusable(true);
    }

    private void loadImages() {
        space = new ImageIcon(getClass().getResource("/images/space.jpeg")).getImage();
        alienImage = new ImageIcon(getClass().getResource("/images/alien.png")).getImage();
        specialAlienImage = new ImageIcon(getClass().getResource("/images/special-alien.png")).getImage();
        alienMissileImage = new ImageIcon(getClass().getResource("/images/alien-missile.png")).getImage();
        missileImage = new ImageIcon(getClass().getResource("/images/missile.gif")).getImage();
        spaceshipImage = new ImageIcon(getClass().getResource("/images/spaceship.png")).getImage();
        explosionImage = new ImageIcon(getClass().getResource("/images/explosion.png")).getImage();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(space,0,0,null);
        drawSpaceship(g);
        drawAliens(g);
        drawMissiles(g);
        drawAlienMissiles(g);
        drawAlienExplosions(g);
        drawPowerUps(g);
        drawBossHealthBar(g);
        drawHud(g);
        drawWaveMessage(g);
        drawHitFeedback(g);
        drawPausedOverlay(g);
        drawStartMenu(g);
        drawGameOver(g);
        Toolkit.getDefaultToolkit().sync();
    }

    private void drawHud(Graphics graphics) {
        int hudHeight = hudCardHeight(rapidFireActive, speedBoostActive, comboMultiplier, comboTicks);
        graphics.setColor(new Color(7, 14, 32, 118));
        graphics.fillRoundRect(HUD_X, HUD_Y, HUD_WIDTH, hudHeight, HUD_ARC, HUD_ARC);
        graphics.setColor(new Color(120, 210, 255, 72));
        graphics.drawRoundRect(HUD_X, HUD_Y, HUD_WIDTH, hudHeight, HUD_ARC, HUD_ARC);

        int textX = HUD_X + HUD_PADDING;
        int currentY = HUD_Y + HUD_PADDING + HUD_ROW_HEIGHT;
        graphics.setColor(Color.WHITE);
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        graphics.drawString("Score: " + score, textX, currentY);

        currentY += HUD_ROW_HEIGHT;
        graphics.drawString("Wave: " + wave, textX, currentY);

        currentY += HUD_ROW_HEIGHT;
        graphics.drawString("Lives: " + lives, textX, currentY);

        if (rapidFireActive) {
            currentY += HUD_ROW_HEIGHT;
            graphics.setColor(new Color(255, 230, 60));
            graphics.drawString("RAPID FIRE: " + rapidFireSecondsRemaining(rapidFireTicks) + "s", textX, currentY);
        }
        if (speedBoostActive) {
            currentY += HUD_ROW_HEIGHT;
            graphics.setColor(new Color(120, 255, 150));
            graphics.drawString("SPEED: " + speedBoostSecondsRemaining(speedBoostTicks) + "s", textX, currentY);
        }
        if (isComboVisible(comboMultiplier, comboTicks)) {
            currentY += HUD_ROW_HEIGHT;
            graphics.setColor(new Color(80, 220, 255));
            graphics.drawString(
                "COMBO x" + comboMultiplier + ": " + comboSecondsRemaining(comboTicks) + "s",
                textX,
                currentY
            );
        }
    }

    static int hudCardHeight(
        boolean rapidFireActive,
        boolean speedBoostActive,
        int comboMultiplier,
        int comboTicks
    ) {
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

    private void drawPausedOverlay(Graphics graphics) {
        if (!isPausedOverlayVisible(gameState)) {
            return;
        }

        graphics.setColor(new Color(0, 0, 0, 150));
        graphics.fillRect(0, 0, GameConstants.PANEL_WIDTH, GameConstants.PANEL_HEIGHT);

        graphics.setColor(Color.WHITE);
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 48));
        drawCenteredString(graphics, "PAUSED", GameConstants.PANEL_HEIGHT / 2 - 30);

        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
        drawCenteredString(graphics, "Press P to Resume", GameConstants.PANEL_HEIGHT / 2 + 20);
    }

    private void drawWaveMessage(Graphics graphics) {
        if (!isWaveMessageVisible(gameState, waveMessageActive, waveMessageTicks)) {
            return;
        }

        String text = "WAVE " + wave;
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
        int textWidth = graphics.getFontMetrics().stringWidth(text);
        int bannerWidth = textWidth + 36;
        int bannerHeight = 34;
        int bannerX = (GameConstants.PANEL_WIDTH - bannerWidth) / 2;
        int bannerY = WAVE_BANNER_Y - 24;

        graphics.setColor(new Color(8, 18, 48, 82));
        graphics.fillRoundRect(bannerX, bannerY, bannerWidth, bannerHeight, 20, 20);
        graphics.setColor(new Color(255, 214, 92, 70));
        graphics.drawRoundRect(bannerX, bannerY, bannerWidth, bannerHeight, 20, 20);
        graphics.setColor(new Color(255, 244, 196, 185));
        drawCenteredString(graphics, text, WAVE_BANNER_Y);
    }

    private void drawGameOver(Graphics graphics) {
        if (gameState != GameState.GAME_OVER) {
            return;
        }

        graphics.setColor(new Color(0, 0, 0, 180));
        graphics.fillRect(0, 0, GameConstants.PANEL_WIDTH, GameConstants.PANEL_HEIGHT);

        graphics.setColor(Color.WHITE);
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 48));
        drawCenteredString(graphics, gameOverTitle, 170);

        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
        drawCenteredString(graphics, "Final Score: " + score, 230);
        if (profileMenuState.hasSelectedProfile()) {
            drawCenteredString(
                graphics,
                "Profile: " + profileMenuState.selectedProfileName() + "    " + profileMenuState.bestScoreText(),
                264
            );
        }
        int currentY = 296;
        if (isNewBestScoreVisible(profileMenuState)) {
            graphics.setColor(new Color(255, 230, 60));
            drawCenteredString(graphics, "New Best Score!", currentY);
            graphics.setColor(Color.WHITE);
            currentY += 30;
        }
        if (isSaveWarningVisible(profileMenuState)) {
            graphics.setColor(new Color(255, 150, 100));
            drawCenteredString(graphics, "Profile save failed", currentY);
            graphics.setColor(Color.WHITE);
            currentY += 30;
        }
        drawLeaderboard(graphics, Math.max(currentY + 12, 326));
        drawCenteredString(graphics, "Press ENTER to Restart", 560);
    }

    private void drawLeaderboard(Graphics graphics, int startY) {
        if (!isLeaderboardVisible(profileMenuState)) {
            return;
        }

        graphics.setColor(new Color(255, 230, 120));
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        drawCenteredString(graphics, "TOP 5", startY);
        graphics.setColor(Color.WHITE);
        graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 18));
        int rowY = startY + 28;
        for (ProfileMenuState.LeaderboardEntry entry : profileMenuState.topProfiles()) {
            drawCenteredString(graphics, leaderboardRowText(entry), rowY);
            rowY += 24;
        }
    }

    private void drawStartMenu(Graphics graphics) {
        if (gameState != GameState.START_MENU) {
            return;
        }

        graphics.setColor(new Color(0, 0, 0, 185));
        graphics.fillRect(0, 0, GameConstants.PANEL_WIDTH, GameConstants.PANEL_HEIGHT);

        graphics.setColor(Color.WHITE);
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 52));
        drawCenteredString(graphics, "ALIENS ATTACK", GameConstants.PANEL_HEIGHT / 2 - 150);

        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
        drawCenteredString(graphics, profileMenuState.startPromptText(), GameConstants.PANEL_HEIGHT / 2 - 95);

        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
        if (profileMenuState.hasSelectedProfile()) {
            drawCenteredString(
                graphics,
                "Profile: " + profileMenuState.selectedProfileName() + "    " + profileMenuState.bestScoreText(),
                GameConstants.PANEL_HEIGHT / 2 - 50
            );
            graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 18));
            drawCenteredString(graphics, profileMenuState.profileCounterText(), GameConstants.PANEL_HEIGHT / 2 - 20);
        } else {
            drawCenteredString(graphics, "No profile selected", GameConstants.PANEL_HEIGHT / 2 - 50);
        }

        graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 18));
        if (profileMenuState.inputMode()) {
            drawCenteredString(
                graphics,
                "Name: " + profileMenuState.draftName() + "_",
                GameConstants.PANEL_HEIGHT / 2 + 18
            );
            drawCenteredString(graphics, "ENTER saves    ESC cancels", GameConstants.PANEL_HEIGHT / 2 + 48);
        } else {
            drawCenteredString(graphics, "Left/Right select    N creates profile", GameConstants.PANEL_HEIGHT / 2 + 18);
        }

        if (!profileMenuState.message().isBlank()) {
            graphics.setColor(isSaveWarningVisible(profileMenuState) ? new Color(255, 150, 100) : new Color(255, 230, 120));
            drawCenteredString(graphics, profileMenuState.message(), GameConstants.PANEL_HEIGHT / 2 + 86);
            graphics.setColor(Color.WHITE);
        }

        graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 18));
        drawCenteredString(graphics, "Arrow keys move    Hold Space to fire    P pauses", GameConstants.PANEL_HEIGHT / 2 + 132);
    }

    private void drawCenteredString(Graphics graphics, String text, int y) {
        int textWidth = graphics.getFontMetrics().stringWidth(text);
        graphics.drawString(text, (GameConstants.PANEL_WIDTH - textWidth) / 2, y);
    }

    private void drawMissiles(Graphics graphics) {
        missiles.forEach(missile -> graphics.drawImage(missileImage, missile.getX(),
            missile.getY(), GameConstants.COMPONENT_SIZE, GameConstants.COMPONENT_SIZE, this));
    }

    private void drawAlienMissiles(Graphics graphics) {
        alienMissiles.forEach(missile ->
            graphics.drawImage(
                alienMissileImage,
                missile.getX(),
                missile.getY(),
                GameConstants.COMPONENT_SIZE,
                GameConstants.COMPONENT_SIZE,
                this
            ));
    }

    private void drawPowerUps(Graphics graphics) {
        powerUps.forEach(powerUp -> drawPowerUp(graphics, powerUp));
    }

    private void drawAlienExplosions(Graphics graphics) {
        alienExplosions.forEach(explosion ->
            graphics.drawImage(
                explosionImage,
                explosion.getX(),
                explosion.getY(),
                GameConstants.COMPONENT_SIZE,
                GameConstants.COMPONENT_SIZE,
                this
            ));
    }

    private void drawHitFeedback(Graphics graphics) {
        if (!hitFeedbackActive) {
            return;
        }

        graphics.setColor(new Color(255, 0, 0, 70));
        graphics.fillRect(0, 0, GameConstants.PANEL_WIDTH, GameConstants.PANEL_HEIGHT);
        graphics.setColor(new Color(255, 80, 80, 220));
        graphics.drawRect(2, 2, GameConstants.PANEL_WIDTH - 5, GameConstants.PANEL_HEIGHT - 5);
        graphics.drawRect(5, 5, GameConstants.PANEL_WIDTH - 11, GameConstants.PANEL_HEIGHT - 11);
    }

    private void drawAliens(Graphics graphics) {
        aliens.forEach(alien -> {
            if (alien.isBoss()) {
                drawBoss(graphics, alien);
            } else if (isShieldedSpecialAlien(alien)) {
                graphics.drawImage(alienImageFor(alien), alien.getX(),
                    alien.getY(), GameConstants.COMPONENT_SIZE, GameConstants.COMPONENT_SIZE, this);
                drawSpecialAlienShield(graphics, alien);
            } else {
                graphics.drawImage(alienImageFor(alien), alien.getX(),
                    alien.getY(), GameConstants.COMPONENT_SIZE, GameConstants.COMPONENT_SIZE, this);
            }
        });
    }

    static boolean isShieldedSpecialAlien(Alien alien) {
        return alien.getType() == AlienType.SPECIAL && !alien.isDamaged();
    }

    Image alienImageFor(Alien alien) {
        return alien.getType() == AlienType.SPECIAL ? specialAlienImage : alienImage;
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
        return GamePanel.class.getResource("/images/explosion.png") != null;
    }

    private void drawSpecialAlienShield(Graphics graphics, Alien alien) {
        int x = alien.getX();
        int y = alien.getY();
        graphics.setColor(new Color(90, 240, 255, 70));
        graphics.fillOval(x - 4, y - 4, GameConstants.COMPONENT_SIZE + 8, GameConstants.COMPONENT_SIZE + 8);
        graphics.setColor(new Color(160, 250, 255, 180));
        graphics.drawOval(x - 3, y - 3, GameConstants.COMPONENT_SIZE + 6, GameConstants.COMPONENT_SIZE + 6);
        graphics.setColor(new Color(255, 255, 255, 180));
        graphics.drawArc(x - 1, y - 5, GameConstants.COMPONENT_SIZE + 2, GameConstants.COMPONENT_SIZE + 2, 25, 120);
    }

    private void drawSpaceship(Graphics graphics) {
        graphics.drawImage(spaceshipImage, spaceship.getX(), spaceship.getY(), GameConstants.COMPONENT_SIZE, GameConstants.COMPONENT_SIZE, this);
        if (isSpaceshipShieldVisible(gameState, shieldActive)) {
            drawSpaceshipShield(graphics);
        }
    }

    private void drawSpaceshipShield(Graphics graphics) {
        int x = spaceship.getX();
        int y = spaceship.getY();
        graphics.setColor(new Color(90, 240, 255, 60));
        graphics.fillOval(x - 7, y - 7, GameConstants.COMPONENT_SIZE + 14, GameConstants.COMPONENT_SIZE + 14);
        graphics.setColor(new Color(160, 250, 255, 180));
        graphics.drawOval(x - 5, y - 5, GameConstants.COMPONENT_SIZE + 10, GameConstants.COMPONENT_SIZE + 10);
        graphics.setColor(new Color(255, 255, 255, 170));
        graphics.drawArc(x - 2, y - 7, GameConstants.COMPONENT_SIZE + 4, GameConstants.COMPONENT_SIZE + 4, 18, 132);
    }

    private void drawBoss(Graphics graphics, Alien boss) {
        int x = boss.getX() - (BOSS_RENDER_WIDTH - GameConstants.COMPONENT_SIZE) / 2;
        int y = boss.getY() - 6;
        graphics.setColor(new Color(95, 18, 22, 220));
        graphics.fillRoundRect(x + 8, y + 8, BOSS_RENDER_WIDTH - 16, BOSS_RENDER_HEIGHT - 12, 18, 18);
        graphics.setColor(new Color(196, 46, 58, 235));
        graphics.fillRoundRect(x + 2, y + 4, BOSS_RENDER_WIDTH - 4, BOSS_RENDER_HEIGHT - 18, 20, 20);
        graphics.setColor(new Color(255, 202, 112, 235));
        graphics.fillRect(x + 10, y + 10, BOSS_RENDER_WIDTH - 20, 6);
        graphics.setColor(new Color(32, 10, 14, 220));
        graphics.fillOval(x + 12, y + 18, 14, 14);
        graphics.fillOval(x + BOSS_RENDER_WIDTH - 26, y + 18, 14, 14);
        graphics.setColor(new Color(255, 240, 180));
        graphics.fillOval(x + 16, y + 22, 6, 6);
        graphics.fillOval(x + BOSS_RENDER_WIDTH - 22, y + 22, 6, 6);
        graphics.setColor(new Color(255, 150, 120));
        graphics.fillRoundRect(x + 18, y + 30, BOSS_RENDER_WIDTH - 36, 6, 6, 6);
        graphics.setColor(new Color(255, 220, 160));
        graphics.drawRoundRect(x + 2, y + 4, BOSS_RENDER_WIDTH - 4, BOSS_RENDER_HEIGHT - 18, 20, 20);
        graphics.drawLine(x + 4, y + BOSS_RENDER_HEIGHT - 10, x + 14, y + BOSS_RENDER_HEIGHT);
        graphics.drawLine(x + BOSS_RENDER_WIDTH - 4, y + BOSS_RENDER_HEIGHT - 10, x + BOSS_RENDER_WIDTH - 14, y + BOSS_RENDER_HEIGHT);
    }

    private void drawPowerUp(Graphics graphics, PowerUp powerUp) {
        int inset = 5;
        int diameter = GameConstants.COMPONENT_SIZE - inset * 2;
        graphics.setColor(powerUpColor(powerUp.getType()));
        graphics.fillOval(powerUp.getX() + inset, powerUp.getY() + inset, diameter, diameter);
        graphics.setColor(new Color(16, 24, 40));
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        graphics.drawString(powerUpLabel(powerUp.getType()), powerUp.getX() + 14, powerUp.getY() + 28);
    }

    private void drawBossHealthBar(Graphics graphics) {
        Alien boss = currentBoss(aliens);
        if (!isBossHealthBarVisible(gameState, boss)) {
            return;
        }

        int barX = (GameConstants.PANEL_WIDTH - BOSS_HEALTH_BAR_WIDTH) / 2;
        int barY = 18;
        graphics.setColor(new Color(25, 16, 16, 180));
        graphics.fillRoundRect(barX, barY, BOSS_HEALTH_BAR_WIDTH, BOSS_HEALTH_BAR_HEIGHT, 12, 12);
        graphics.setColor(new Color(255, 110, 110));
        graphics.fillRoundRect(
            barX,
            barY,
            bossHealthBarFillWidth(boss.getHealth(), GameRules.bossHealth(), BOSS_HEALTH_BAR_WIDTH),
            BOSS_HEALTH_BAR_HEIGHT,
            12,
            12
        );
        graphics.setColor(new Color(255, 220, 220));
        graphics.drawRoundRect(barX, barY, BOSS_HEALTH_BAR_WIDTH, BOSS_HEALTH_BAR_HEIGHT, 12, 12);
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        graphics.drawString("BOSS", barX, barY - 4);
    }

}
