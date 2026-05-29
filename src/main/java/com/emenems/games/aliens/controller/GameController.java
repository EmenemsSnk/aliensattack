package com.emenems.games.aliens.controller;

import com.emenems.games.aliens.GameState;
import com.emenems.games.aliens.gamemachines.Alien;
import com.emenems.games.aliens.gamemachines.Missile;
import com.emenems.games.aliens.gamemachines.Spaceship;
import com.emenems.games.aliens.gui.GamePanel;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import javax.swing.Timer;

public class GameController implements ActionListener {
    private static final int TIMER_DELAY_MS = 16;
    private static final double BASE_ALIEN_SPEED = 1.4;
    private static final double MAX_ALIEN_SPEED = 4.5;
    private static final int ALIEN_COUNT = 10;
    private static final int ALIEN_START_MIN_Y = 30;
    private static final int ALIEN_START_X_JITTER = 20;
    private static final int ALIEN_START_Y_JITTER = 90;
    private static final int DEFAULT_LIVES = 3;
    private static final int HIT_FEEDBACK_TICKS = 18;
    private static final String DEFAULT_GAME_OVER_TITLE = "GAME OVER";
    private static final String ALIENS_WIN_TITLE = "ALIENS WIN";

    private final Spaceship spaceship;
    private final List<Missile> missiles;
    private final List<Alien> aliens;
    private final GamePanel gamePanel;
    private final Random random;
    private final Set<Integer> pressedMovementKeys = new HashSet<>();
    private Timer timer;
    private int score;
    private int wave = 1;
    private int lives = DEFAULT_LIVES;
    private GameState gameState = GameState.PLAYING;
    private int hitFeedbackTicks;
    private String gameOverTitle = DEFAULT_GAME_OVER_TITLE;

    public GameController(Spaceship spaceship, List<Missile> missiles, List<Alien> aliens, GamePanel gamePanel) {
        this(spaceship, missiles, aliens, gamePanel, new Random());
    }

    GameController(Spaceship spaceship, List<Missile> missiles, List<Alien> aliens, GamePanel gamePanel, Random random) {
        this.spaceship = spaceship;
        this.missiles =  missiles;
        this.aliens =  aliens;
        this.gamePanel = gamePanel;
        this.random = random;
    }

    public void initialize(){
        generateSpaceObjects();
        updatePanelState();
        gamePanel.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                super.keyPressed(e);
                handleKeyPressed(e.getKeyCode());
            }

            @Override
            public void keyReleased(KeyEvent e) {
                super.keyReleased(e);
                handleKeyReleased(e.getKeyCode());
            }
        });
        timer = new Timer(TIMER_DELAY_MS, this);
        timer.start();
    }

    private void generateSpaceObjects() {
        aliens.clear();
        double alienSpeed = calculateAlienSpeed(wave, BASE_ALIEN_SPEED, MAX_ALIEN_SPEED);
        int laneSpacing = (GamePanel.PANEL_WIDTH - GamePanel.DEFAULT_COMPONENT_SIZE) / ALIEN_COUNT;
        for (int index = 0; index < ALIEN_COUNT; index++) {
            int laneX = index * laneSpacing + (laneSpacing - GamePanel.DEFAULT_COMPONENT_SIZE) / 2;
            int x = laneX + random.nextInt(ALIEN_START_X_JITTER * 2 + 1) - ALIEN_START_X_JITTER;
            int y = ALIEN_START_MIN_Y + random.nextInt(ALIEN_START_Y_JITTER + 1);
            aliens.add(new Alien(x, y, alienSpeed));
        }
    }

    void handleKeyPressed(int keyCode) {
        if (gameState == GameState.GAME_OVER) {
            if (keyCode == KeyEvent.VK_SPACE) {
                restartGame();
            }
            return;
        }

        if (keyCode == KeyEvent.VK_SPACE) {
            missiles.add(new Missile(spaceship.getX(), spaceship.getY() - GamePanel.DEFAULT_COMPONENT_SIZE));
            repaintGamePanel();
        } else if (isMovementKey(keyCode)) {
            pressedMovementKeys.add(keyCode);
        }
    }

    void handleKeyReleased(int keyCode) {
        if (isMovementKey(keyCode)) {
            pressedMovementKeys.remove(keyCode);
        }
    }

    private boolean isMovementKey(int keyCode) {
        return keyCode == KeyEvent.VK_LEFT
            || keyCode == KeyEvent.VK_RIGHT
            || keyCode == KeyEvent.VK_UP
            || keyCode == KeyEvent.VK_DOWN;
    }

    private void moveSpaceshipFromPressedKeys() {
        if (pressedMovementKeys.contains(KeyEvent.VK_LEFT)) {
            spaceship.moveLeft();
        }
        if (pressedMovementKeys.contains(KeyEvent.VK_RIGHT)) {
            spaceship.moveRight();
        }
        if (pressedMovementKeys.contains(KeyEvent.VK_UP)) {
            spaceship.moveUp();
        }
        if (pressedMovementKeys.contains(KeyEvent.VK_DOWN)) {
            spaceship.moveDown();
        }
        spaceship.clampToBounds(
            0,
            0,
            GamePanel.PANEL_WIDTH - GamePanel.DEFAULT_COMPONENT_SIZE,
            GamePanel.PANEL_HEIGHT - GamePanel.DEFAULT_COMPONENT_SIZE
        );
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        tick();
        repaintGamePanel();
    }

    void tick() {
        if (gameState == GameState.GAME_OVER) {
            updatePanelState();
            return;
        }

        updateHitFeedback();
        moveSpaceshipFromPressedKeys();
        aliens.forEach(Alien::move);
        missiles.forEach(Missile::move);
        checkCollisions();
        if (gameState == GameState.GAME_OVER) {
            updatePanelState();
            return;
        }
        checkAlienInvasion();
        if (gameState == GameState.GAME_OVER) {
            updatePanelState();
            return;
        }
        cleanupOffscreenObjects();
        advanceWaveIfCleared();
        updatePanelState();
    }

    private void checkCollisions() {
        checkCollisionsWithMissile();
        checkCollisionsWithSpaceShip();
    }

    void checkCollisionsWithSpaceShip() {
        Rectangle spaceshipArea = new Rectangle(spaceship.getX(), spaceship.getY(), GamePanel.DEFAULT_COMPONENT_SIZE, GamePanel.DEFAULT_COMPONENT_SIZE);
        Alien collidingAlien = null;

        for (Alien alien : aliens) {
            Rectangle alienArea = new Rectangle(alien.getX(), alien.getY(), GamePanel.DEFAULT_COMPONENT_SIZE,
                GamePanel.DEFAULT_COMPONENT_SIZE);
            if (spaceshipArea.intersects(alienArea)) {
                collidingAlien = alien;
                break;
            }
        }

        if (collidingAlien == null) {
            return;
        }

        aliens.remove(collidingAlien);
        hitFeedbackTicks = HIT_FEEDBACK_TICKS;
        lives--;
        if (lives <= 0) {
            lives = 0;
            enterGameOver(DEFAULT_GAME_OVER_TITLE);
        }
    }

    void checkCollisionsWithMissile() {
        Set<Missile> missilesToRemove = new HashSet<>();
        Set<Alien> aliensToRemove = new HashSet<>();

        for (Missile missile : missiles) {
            Rectangle missileArea = objectArea(missile.getX(), missile.getY());
            for (Alien alien : aliens) {
                if (aliensToRemove.contains(alien)) {
                    continue;
                }

                Rectangle alienArea = objectArea(alien.getX(), alien.getY());
                if (alienArea.intersects(missileArea)) {
                    missilesToRemove.add(missile);
                    aliensToRemove.add(alien);
                    break;
                }
            }
        }

        missiles.removeAll(missilesToRemove);
        aliens.removeAll(aliensToRemove);
        score += aliensToRemove.size() * calculateAlienScore(wave);
    }

    void cleanupOffscreenObjects() {
        missiles.removeIf(missile -> missile.getY() + GamePanel.DEFAULT_COMPONENT_SIZE < 0);
        aliens.removeIf(alien ->
            alien.getY() > GamePanel.PANEL_HEIGHT
                || alien.getX() + GamePanel.DEFAULT_COMPONENT_SIZE < 0
                || alien.getX() > GamePanel.PANEL_WIDTH);
    }

    int getScore() {
        return score;
    }

    int getWave() {
        return wave;
    }

    int getLives() {
        return lives;
    }

    GameState getGameState() {
        return gameState;
    }

    boolean isHitFeedbackActive() {
        return hitFeedbackTicks > 0;
    }

    String getGameOverTitle() {
        return gameOverTitle;
    }

    private Rectangle objectArea(int x, int y) {
        return new Rectangle(x, y, GamePanel.DEFAULT_COMPONENT_SIZE, GamePanel.DEFAULT_COMPONENT_SIZE);
    }

    private void advanceWaveIfCleared() {
        if (!aliens.isEmpty()) {
            return;
        }

        wave++;
        generateSpaceObjects();
    }

    private void updatePanelState() {
        if (gamePanel != null) {
            gamePanel.updateGameState(score, wave, lives, gameState, isHitFeedbackActive(), gameOverTitle);
        }
    }

    static int calculateAlienScore(int wave) {
        return wave * 10;
    }

    static double calculateAlienSpeed(int wave, double baseSpeed, double maxSpeed) {
        double speed = baseSpeed * Math.pow(1.15, wave - 1);
        return Math.min(speed, maxSpeed);
    }

    private void repaintGamePanel() {
        if (gamePanel != null) {
            gamePanel.repaint();
        }
    }

    private void restartGame() {
        score = 0;
        wave = 1;
        lives = DEFAULT_LIVES;
        gameState = GameState.PLAYING;
        hitFeedbackTicks = 0;
        gameOverTitle = DEFAULT_GAME_OVER_TITLE;
        missiles.clear();
        pressedMovementKeys.clear();
        generateSpaceObjects();
        updatePanelState();
        repaintGamePanel();
    }

    private void checkAlienInvasion() {
        boolean alienReachedBottom = aliens.stream()
            .anyMatch(alien -> alien.getY() + GamePanel.DEFAULT_COMPONENT_SIZE >= GamePanel.PANEL_HEIGHT);
        if (alienReachedBottom) {
            enterGameOver(ALIENS_WIN_TITLE);
        }
    }

    private void enterGameOver(String title) {
        gameState = GameState.GAME_OVER;
        gameOverTitle = title;
        pressedMovementKeys.clear();
    }

    private void updateHitFeedback() {
        if (hitFeedbackTicks > 0) {
            hitFeedbackTicks--;
        }
    }
}
