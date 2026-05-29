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
import java.util.Set;
import javax.swing.Timer;

public class GameController implements ActionListener {
    private static final int TIMER_DELAY_MS = 16;
    private static final int BASE_ALIEN_SPEED = 1;
    private static final int MAX_ALIEN_SPEED = BASE_ALIEN_SPEED * 2;
    private static final int ALIEN_START_Y = 30;
    private static final int[] ALIEN_START_X_VALUES = {10, 100, 200, 300, 400, 500, 600, 700, 800, 900};
    private static final int DEFAULT_LIVES = 3;

    private final Spaceship spaceship;
    private final List<Missile> missiles;
    private final List<Alien> aliens;
    private final GamePanel gamePanel;
    private final Set<Integer> pressedMovementKeys = new HashSet<>();
    private Timer timer;
    private int score;
    private int wave = 1;
    private int lives = DEFAULT_LIVES;
    private GameState gameState = GameState.PLAYING;

    public GameController(Spaceship spaceship, List<Missile> missiles, List<Alien> aliens, GamePanel gamePanel) {
        this.spaceship = spaceship;
        this.missiles =  missiles;
        this.aliens =  aliens;
        this.gamePanel = gamePanel;
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
        int alienSpeed = calculateAlienSpeed(wave, BASE_ALIEN_SPEED, MAX_ALIEN_SPEED);
        for (int x : ALIEN_START_X_VALUES) {
            aliens.add(new Alien(x, ALIEN_START_Y, alienSpeed));
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

        moveSpaceshipFromPressedKeys();
        aliens.forEach(Alien::move);
        missiles.forEach(Missile::move);
        checkCollisions();
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
        lives--;
        if (lives <= 0) {
            lives = 0;
            gameState = GameState.GAME_OVER;
            pressedMovementKeys.clear();
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
            gamePanel.updateGameState(score, wave, lives, gameState);
        }
    }

    static int calculateAlienScore(int wave) {
        return wave * 10;
    }

    static int calculateAlienSpeed(int wave, int baseSpeed, int maxSpeed) {
        int speed = (int) Math.round(baseSpeed * Math.pow(1.1, wave - 1));
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
        missiles.clear();
        pressedMovementKeys.clear();
        generateSpaceObjects();
        updatePanelState();
        repaintGamePanel();
    }
}
