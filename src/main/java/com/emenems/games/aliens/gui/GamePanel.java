package com.emenems.games.aliens.gui;

import com.emenems.games.aliens.GameConstants;
import com.emenems.games.aliens.GameState;
import com.emenems.games.aliens.gamemachines.Alien;
import com.emenems.games.aliens.gamemachines.AlienExplosion;
import com.emenems.games.aliens.gamemachines.AlienType;
import com.emenems.games.aliens.gamemachines.AlienMissile;
import com.emenems.games.aliens.gamemachines.Missile;
import com.emenems.games.aliens.gamemachines.RapidFirePowerUp;
import com.emenems.games.aliens.gamemachines.Spaceship;
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
    private List<RapidFirePowerUp> rapidFirePowerUps;
    private int score;
    private int wave = 1;
    private int lives = 3;
    private GameState gameState = GameState.PLAYING;
    private boolean hitFeedbackActive;
    private String gameOverTitle = "GAME OVER";
    private boolean rapidFireActive;
    private int rapidFireTicks;
    private int comboMultiplier = 1;
    private int comboTicks;

    public GamePanel(Spaceship spaceship, List<Missile> missiles, List<AlienMissile> alienMissiles, List<Alien> aliens) {
        this(spaceship, missiles, alienMissiles, aliens, new ArrayList<>(), new ArrayList<>());
    }

    public GamePanel(
        Spaceship spaceship,
        List<Missile> missiles,
        List<AlienMissile> alienMissiles,
        List<Alien> aliens,
        List<AlienExplosion> alienExplosions,
        List<RapidFirePowerUp> rapidFirePowerUps
    ) {
        this.spaceship = spaceship;
        this.missiles = missiles;
        this.alienMissiles = alienMissiles;
        this.aliens = aliens;
        this.alienExplosions = alienExplosions;
        this.rapidFirePowerUps = rapidFirePowerUps;
        initBoard();
    }

    public GamePanel(
        Spaceship spaceship,
        List<Missile> missiles,
        List<AlienMissile> alienMissiles,
        List<Alien> aliens,
        List<RapidFirePowerUp> rapidFirePowerUps
    ) {
        this(spaceship, missiles, alienMissiles, aliens, new ArrayList<>(), rapidFirePowerUps);
    }

    public void updateGameState(
        int score,
        int wave,
        int lives,
        GameState gameState,
        boolean hitFeedbackActive,
        String gameOverTitle,
        boolean rapidFireActive,
        int rapidFireTicks,
        int comboMultiplier,
        int comboTicks
    ) {
        this.score = score;
        this.wave = wave;
        this.lives = lives;
        this.gameState = gameState;
        this.hitFeedbackActive = hitFeedbackActive;
        this.gameOverTitle = gameOverTitle;
        this.rapidFireActive = rapidFireActive;
        this.rapidFireTicks = rapidFireTicks;
        this.comboMultiplier = comboMultiplier;
        this.comboTicks = comboTicks;
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
        drawRapidFirePowerUps(g);
        drawHud(g);
        drawHitFeedback(g);
        drawStartMenu(g);
        drawGameOver(g);
        Toolkit.getDefaultToolkit().sync();
    }

    private void drawHud(Graphics graphics) {
        graphics.setColor(Color.WHITE);
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        graphics.drawString("Score: " + score, 20, 30);
        graphics.drawString("Wave: " + wave, 20, 55);
        graphics.drawString("Lives: " + lives, 20, 80);
        if (rapidFireActive) {
            graphics.setColor(new Color(255, 230, 60));
            graphics.drawString("RAPID FIRE: " + rapidFireSecondsRemaining(rapidFireTicks) + "s", 20, 105);
        }
        if (isComboVisible(comboMultiplier, comboTicks)) {
            graphics.setColor(new Color(80, 220, 255));
            graphics.drawString(
                "COMBO x" + comboMultiplier + ": " + comboSecondsRemaining(comboTicks) + "s",
                20,
                130
            );
        }
    }

    static int rapidFireSecondsRemaining(int ticks) {
        return Math.ceilDiv(ticks, TICKS_PER_SECOND);
    }

    static int comboSecondsRemaining(int ticks) {
        return Math.ceilDiv(ticks, TICKS_PER_SECOND);
    }

    static boolean isComboVisible(int multiplier, int ticks) {
        return multiplier >= 2 && ticks > 0;
    }

    private void drawGameOver(Graphics graphics) {
        if (gameState != GameState.GAME_OVER) {
            return;
        }

        graphics.setColor(new Color(0, 0, 0, 180));
        graphics.fillRect(0, 0, GameConstants.PANEL_WIDTH, GameConstants.PANEL_HEIGHT);

        graphics.setColor(Color.WHITE);
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 48));
        drawCenteredString(graphics, gameOverTitle, GameConstants.PANEL_HEIGHT / 2 - 60);

        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
        drawCenteredString(graphics, "Final Score: " + score, GameConstants.PANEL_HEIGHT / 2);
        drawCenteredString(graphics, "Press ENTER to Restart", GameConstants.PANEL_HEIGHT / 2 + 40);
    }

    private void drawStartMenu(Graphics graphics) {
        if (gameState != GameState.START_MENU) {
            return;
        }

        graphics.setColor(new Color(0, 0, 0, 185));
        graphics.fillRect(0, 0, GameConstants.PANEL_WIDTH, GameConstants.PANEL_HEIGHT);

        graphics.setColor(Color.WHITE);
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 52));
        drawCenteredString(graphics, "ALIENS ATTACK", GameConstants.PANEL_HEIGHT / 2 - 80);

        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
        drawCenteredString(graphics, "Press ENTER to Start", GameConstants.PANEL_HEIGHT / 2 - 25);

        graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 18));
        drawCenteredString(graphics, "Arrow keys move    Hold Space to fire", GameConstants.PANEL_HEIGHT / 2 + 20);
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

    private void drawRapidFirePowerUps(Graphics graphics) {
        rapidFirePowerUps.forEach(powerUp -> {
            int inset = 5;
            int diameter = GameConstants.COMPONENT_SIZE - inset * 2;
            graphics.setColor(new Color(255, 220, 35, 220));
            graphics.fillOval(powerUp.getX() + inset, powerUp.getY() + inset, diameter, diameter);
            graphics.setColor(new Color(255, 80, 20));
            graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
            graphics.drawString("R", powerUp.getX() + 14, powerUp.getY() + 28);
        });
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
            graphics.drawImage(alienImageFor(alien), alien.getX(),
                alien.getY(), GameConstants.COMPONENT_SIZE, GameConstants.COMPONENT_SIZE, this);
            if (isShieldedSpecialAlien(alien)) {
                drawSpecialAlienShield(graphics, alien);
            }
        });
    }

    static boolean isShieldedSpecialAlien(Alien alien) {
        return alien.getType() == AlienType.SPECIAL && !alien.isDamaged();
    }

    Image alienImageFor(Alien alien) {
        return alien.getType() == AlienType.SPECIAL ? specialAlienImage : alienImage;
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
    }

}
