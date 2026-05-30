package com.emenems.games.aliens.gui;

import com.emenems.games.aliens.GameState;
import com.emenems.games.aliens.gamemachines.Alien;
import com.emenems.games.aliens.gamemachines.AlienMissile;
import com.emenems.games.aliens.gamemachines.Missile;
import com.emenems.games.aliens.gamemachines.Spaceship;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Toolkit;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JPanel;

public class GamePanel extends JPanel {
    public static final int MINIMUM_BORDER_VALUE = 10;
    public static final int PANEL_HEIGHT = 650;
    public static final int PANEL_WIDTH = 760;
    public static final Dimension DEFAULT_DIMENSION = new Dimension(42, 42);
    public static final int DEFAULT_COMPONENT_SIZE = 42;
    private Image space;
    private Image alienImage;
    private Image missileImage;
    private Image spaceshipImage;
    private Spaceship spaceship;
    private List<Missile> missiles;
    private List<AlienMissile> alienMissiles;
    private List<Alien> aliens;
    private int score;
    private int wave = 1;
    private int lives = 3;
    private GameState gameState = GameState.PLAYING;
    private boolean hitFeedbackActive;
    private String gameOverTitle = "GAME OVER";

    public GamePanel(Spaceship spaceship, List<Missile> missiles, List<Alien> aliens) {
        this(spaceship, missiles, List.of(), aliens);
    }

    public GamePanel(Spaceship spaceship, List<Missile> missiles, List<AlienMissile> alienMissiles, List<Alien> aliens) {
        this.spaceship = spaceship;
        this.missiles = missiles;
        this.alienMissiles = alienMissiles;
        this.aliens = aliens;
        initBoard();
    }

    public void updateHud(int score, int wave) {
        updateGameState(score, wave, lives, gameState);
    }

    public void updateGameState(int score, int wave, int lives, GameState gameState) {
        updateGameState(score, wave, lives, gameState, hitFeedbackActive, gameOverTitle);
    }

    public void updateGameState(
        int score,
        int wave,
        int lives,
        GameState gameState,
        boolean hitFeedbackActive,
        String gameOverTitle
    ) {
        this.score = score;
        this.wave = wave;
        this.lives = lives;
        this.gameState = gameState;
        this.hitFeedbackActive = hitFeedbackActive;
        this.gameOverTitle = gameOverTitle;
    }

    private void initBoard() {
        loadImages();
        setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
        setMinimumSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
        setMaximumSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
        setFocusable(true);
    }

    private void loadImages() {
        space = new ImageIcon(getClass().getResource("/images/space.jpeg")).getImage();
        alienImage = new ImageIcon(getClass().getResource("/images/alien.png")).getImage();
        missileImage = new ImageIcon(getClass().getResource("/images/missile.gif")).getImage();
        spaceshipImage = new ImageIcon(getClass().getResource("/images/spaceship.png")).getImage();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(space,0,0,null);
        drawSpaceship(g);
        drawAliens(g);
        drawMissiles(g);
        drawAlienMissiles(g);
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
    }

    private void drawGameOver(Graphics graphics) {
        if (gameState != GameState.GAME_OVER) {
            return;
        }

        graphics.setColor(new Color(0, 0, 0, 180));
        graphics.fillRect(0, 0, PANEL_WIDTH, PANEL_HEIGHT);

        graphics.setColor(Color.WHITE);
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 48));
        drawCenteredString(graphics, gameOverTitle, PANEL_HEIGHT / 2 - 60);

        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
        drawCenteredString(graphics, "Final Score: " + score, PANEL_HEIGHT / 2);
        drawCenteredString(graphics, "Press ENTER to Restart", PANEL_HEIGHT / 2 + 40);
    }

    private void drawStartMenu(Graphics graphics) {
        if (gameState != GameState.START_MENU) {
            return;
        }

        graphics.setColor(new Color(0, 0, 0, 185));
        graphics.fillRect(0, 0, PANEL_WIDTH, PANEL_HEIGHT);

        graphics.setColor(Color.WHITE);
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 52));
        drawCenteredString(graphics, "ALIENS ATTACK", PANEL_HEIGHT / 2 - 80);

        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
        drawCenteredString(graphics, "Press ENTER to Start", PANEL_HEIGHT / 2 - 25);

        graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 18));
        drawCenteredString(graphics, "Arrow keys move    Hold Space to fire", PANEL_HEIGHT / 2 + 20);
    }

    private void drawCenteredString(Graphics graphics, String text, int y) {
        int textWidth = graphics.getFontMetrics().stringWidth(text);
        graphics.drawString(text, (PANEL_WIDTH - textWidth) / 2, y);
    }

    private void drawMissiles(Graphics graphics) {
        missiles.forEach(missile -> graphics.drawImage(missileImage, missile.getX(),
            missile.getY(), DEFAULT_COMPONENT_SIZE, DEFAULT_COMPONENT_SIZE, this));
    }

    private void drawAlienMissiles(Graphics graphics) {
        graphics.setColor(new Color(255, 80, 40));
        alienMissiles.forEach(missile ->
            graphics.fillRect(missile.getX() + 17, missile.getY(), 8, DEFAULT_COMPONENT_SIZE));
    }

    private void drawHitFeedback(Graphics graphics) {
        if (!hitFeedbackActive) {
            return;
        }

        graphics.setColor(new Color(255, 0, 0, 70));
        graphics.fillRect(0, 0, PANEL_WIDTH, PANEL_HEIGHT);
        graphics.setColor(new Color(255, 80, 80, 220));
        graphics.drawRect(2, 2, PANEL_WIDTH - 5, PANEL_HEIGHT - 5);
        graphics.drawRect(5, 5, PANEL_WIDTH - 11, PANEL_HEIGHT - 11);
    }

    private void drawAliens(Graphics graphics) {
        aliens.forEach(alien -> graphics.drawImage(alienImage, alien.getX(),
            alien.getY(), DEFAULT_COMPONENT_SIZE, DEFAULT_COMPONENT_SIZE, this));
    }

    private void drawSpaceship(Graphics graphics) {
        graphics.drawImage(spaceshipImage, spaceship.getX(), spaceship.getY(), DEFAULT_COMPONENT_SIZE, DEFAULT_COMPONENT_SIZE, this);
    }

}
