package com.emenems.games.aliens.controller;

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

    private final Spaceship spaceship;
    private final List<Missile> missiles;
    private final List<Alien> aliens;
    private final GamePanel gamePanel;
    private final Set<Integer> pressedMovementKeys = new HashSet<>();
    private Timer timer;

    public GameController(Spaceship spaceship, List<Missile> missiles, List<Alien> aliens, GamePanel gamePanel) {
        this.spaceship = spaceship;
        this.missiles =  missiles;
        this.aliens =  aliens;
        this.gamePanel = gamePanel;
    }

    public void initialize(){
        generateSpaceObjects();
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
        aliens.add(new Alien(10, 30));
        aliens.add(new Alien(100, 30));
        aliens.add(new Alien(200, 30));
        aliens.add(new Alien(300, 30));
        aliens.add(new Alien(400, 30));
        aliens.add(new Alien(500, 30));
        aliens.add(new Alien(600, 30));
        aliens.add(new Alien(700, 30));
        aliens.add(new Alien(800, 30));
        aliens.add(new Alien(900, 30));
    }

    void handleKeyPressed(int keyCode) {
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
        moveSpaceshipFromPressedKeys();
        aliens.forEach(Alien::move);
        missiles.forEach(Missile::move);
        checkCollisions();
        cleanupOffscreenObjects();
    }

    private void checkCollisions() {
        checkCollisionsWithMissile();
        checkCollisionsWithSpaceShip();
    }

    void checkCollisionsWithSpaceShip() {
        Rectangle spaceshipArea = new Rectangle(spaceship.getX(), spaceship.getY(), GamePanel.DEFAULT_COMPONENT_SIZE, GamePanel.DEFAULT_COMPONENT_SIZE);

        for (Alien alien : aliens) {
            Rectangle alienArea = new Rectangle(alien.getX(), alien.getY(), GamePanel.DEFAULT_COMPONENT_SIZE,
                GamePanel.DEFAULT_COMPONENT_SIZE);
            if (spaceshipArea.intersects(alienArea)) {
                return;
            }
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
    }

    void cleanupOffscreenObjects() {
        missiles.removeIf(missile -> missile.getY() + GamePanel.DEFAULT_COMPONENT_SIZE < 0);
        aliens.removeIf(alien ->
            alien.getY() > GamePanel.PANEL_HEIGHT
                || alien.getX() + GamePanel.DEFAULT_COMPONENT_SIZE < 0
                || alien.getX() > GamePanel.PANEL_WIDTH);
    }

    private Rectangle objectArea(int x, int y) {
        return new Rectangle(x, y, GamePanel.DEFAULT_COMPONENT_SIZE, GamePanel.DEFAULT_COMPONENT_SIZE);
    }

    private void repaintGamePanel() {
        if (gamePanel != null) {
            gamePanel.repaint();
        }
    }
}
