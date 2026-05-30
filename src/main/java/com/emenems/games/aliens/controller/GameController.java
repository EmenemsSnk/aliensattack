package com.emenems.games.aliens.controller;

import com.emenems.games.aliens.GameConstants;
import com.emenems.games.aliens.GameState;
import com.emenems.games.aliens.audio.ArcadeSoundPlayer;
import com.emenems.games.aliens.gamemachines.Alien;
import com.emenems.games.aliens.gamemachines.AlienMissile;
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
    private static final double BASE_ALIEN_SPEED = 0.8;
    private static final double MAX_ALIEN_SPEED = 2.8;
    private static final int ALIEN_COUNT = 6;
    private static final int ALIEN_START_MIN_Y = 30;
    private static final int ALIEN_START_X_JITTER = 20;
    private static final int ALIEN_START_Y_JITTER = 90;
    private static final int DEFAULT_LIVES = 3;
    private static final int HIT_FEEDBACK_TICKS = 18;
    private static final String DEFAULT_GAME_OVER_TITLE = "GAME OVER";
    private static final String ALIENS_WIN_TITLE = "ALIENS WIN";
    static final int MAX_ALIEN_MISSILES = 2;
    private static final double ALIEN_FIRE_CHANCE = 0.008;
    private static final int PLAYER_FIRE_COOLDOWN_TICKS = 10;
    private static final int ALIEN_MISSILE_HITBOX_X_OFFSET = 9;
    private static final int ALIEN_MISSILE_HITBOX_WIDTH = 7;

    private final Spaceship spaceship;
    private final List<Missile> missiles;
    private final List<AlienMissile> alienMissiles;
    private final List<Alien> aliens;
    private final GamePanel gamePanel;
    private final Random random;
    private final ArcadeSoundPlayer soundPlayer;
    private final Set<Integer> pressedMovementKeys = new HashSet<>();
    private Timer timer;
    private int score;
    private int wave = 1;
    private int lives = DEFAULT_LIVES;
    private GameState gameState = GameState.START_MENU;
    private int hitFeedbackTicks;
    private int playerFireCooldownTicks;
    private boolean spacePressed;
    private String gameOverTitle = DEFAULT_GAME_OVER_TITLE;

    public GameController(
        Spaceship spaceship,
        List<Missile> missiles,
        List<AlienMissile> alienMissiles,
        List<Alien> aliens,
        GamePanel gamePanel
    ) {
        this(spaceship, missiles, alienMissiles, aliens, gamePanel, new Random(), new ArcadeSoundPlayer());
    }

    GameController(
        Spaceship spaceship,
        List<Missile> missiles,
        List<AlienMissile> alienMissiles,
        List<Alien> aliens,
        GamePanel gamePanel,
        Random random,
        ArcadeSoundPlayer soundPlayer
    ) {
        this.spaceship = spaceship;
        this.missiles = missiles;
        this.alienMissiles = alienMissiles;
        this.aliens = aliens;
        this.gamePanel = gamePanel;
        this.random = random;
        this.soundPlayer = soundPlayer;
    }

    public void initialize() {
        generateSpaceObjects();
        updatePanelState();
        gamePanel.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                handleKeyPressed(e.getKeyCode());
            }

            @Override
            public void keyReleased(KeyEvent e) {
                handleKeyReleased(e.getKeyCode());
            }
        });
        timer = new Timer(TIMER_DELAY_MS, this);
        timer.start();
    }

    private void generateSpaceObjects() {
        aliens.clear();
        double alienSpeed = calculateAlienSpeed(wave, BASE_ALIEN_SPEED, MAX_ALIEN_SPEED);
        int laneSpacing = (GameConstants.PANEL_WIDTH - GameConstants.COMPONENT_SIZE) / ALIEN_COUNT;
        for (int index = 0; index < ALIEN_COUNT; index++) {
            int laneX = index * laneSpacing + (laneSpacing - GameConstants.COMPONENT_SIZE) / 2;
            int x = laneX + random.nextInt(ALIEN_START_X_JITTER * 2 + 1) - ALIEN_START_X_JITTER;
            int y = ALIEN_START_MIN_Y + random.nextInt(ALIEN_START_Y_JITTER + 1);
            aliens.add(new Alien(x, y, alienSpeed));
        }
    }

    void handleKeyPressed(int keyCode) {
        switch (gameState) {
            case START_MENU -> {
                if (keyCode == KeyEvent.VK_ENTER) {
                    startGame();
                }
            }
            case GAME_OVER -> {
                if (keyCode == KeyEvent.VK_ENTER) {
                    restartGame();
                }
            }
            case PLAYING -> handlePlayingKeyPressed(keyCode);
        }
    }

    private void handlePlayingKeyPressed(int keyCode) {
        if (keyCode == KeyEvent.VK_SPACE) {
            spacePressed = true;
            firePlayerMissileIfReady();
        } else if (isMovementKey(keyCode)) {
            pressedMovementKeys.add(keyCode);
        }
    }

    void handleKeyReleased(int keyCode) {
        if (keyCode == KeyEvent.VK_SPACE) {
            spacePressed = false;
        }
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
            GameConstants.PANEL_WIDTH - GameConstants.COMPONENT_SIZE,
            GameConstants.PANEL_HEIGHT - GameConstants.COMPONENT_SIZE
        );
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        tick();
        repaintGamePanel();
    }

    void tick() {
        if (gameState != GameState.PLAYING) {
            updatePanelState();
            return;
        }

        updateHitFeedback();
        updatePlayerFireCooldown();
        fireHeldPlayerMissileIfReady();
        moveSpaceshipFromPressedKeys();
        aliens.forEach(Alien::move);
        missiles.forEach(Missile::move);
        alienMissiles.forEach(AlienMissile::move);
        fireAlienMissileIfReady();
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
        checkCollisionsWithAlienMissile();
        checkCollisionsWithSpaceShip();
    }

    void checkCollisionsWithSpaceShip() {
        Rectangle spaceshipArea = new Rectangle(spaceship.getX(), spaceship.getY(), GameConstants.COMPONENT_SIZE, GameConstants.COMPONENT_SIZE);
        Alien collidingAlien = null;

        for (Alien alien : aliens) {
            Rectangle alienArea = new Rectangle(alien.getX(), alien.getY(), GameConstants.COMPONENT_SIZE,
                GameConstants.COMPONENT_SIZE);
            if (spaceshipArea.intersects(alienArea)) {
                collidingAlien = alien;
                break;
            }
        }

        if (collidingAlien == null) {
            return;
        }

        aliens.remove(collidingAlien);
        loseLife();
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
        if (!aliensToRemove.isEmpty()) {
            soundPlayer.playExplosion();
        }
    }

    void checkCollisionsWithAlienMissile() {
        AlienMissile collidingMissile = null;
        Rectangle spaceshipArea = objectArea(spaceship.getX(), spaceship.getY());

        for (AlienMissile alienMissile : alienMissiles) {
            if (spaceshipArea.intersects(alienMissileArea(alienMissile))) {
                collidingMissile = alienMissile;
                break;
            }
        }

        if (collidingMissile == null) {
            return;
        }

        alienMissiles.remove(collidingMissile);
        loseLife();
    }

    void cleanupOffscreenObjects() {
        missiles.removeIf(missile -> missile.getY() + GameConstants.COMPONENT_SIZE < 0);
        alienMissiles.removeIf(missile -> missile.getY() > GameConstants.PANEL_HEIGHT);
        aliens.removeIf(alien ->
            alien.getY() > GameConstants.PANEL_HEIGHT
                || alien.getX() + GameConstants.COMPONENT_SIZE < 0
                || alien.getX() > GameConstants.PANEL_WIDTH);
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
        return new Rectangle(x, y, GameConstants.COMPONENT_SIZE, GameConstants.COMPONENT_SIZE);
    }

    private Rectangle alienMissileArea(AlienMissile alienMissile) {
        return new Rectangle(
            alienMissile.getX() + ALIEN_MISSILE_HITBOX_X_OFFSET,
            alienMissile.getY(),
            ALIEN_MISSILE_HITBOX_WIDTH,
            GameConstants.COMPONENT_SIZE
        );
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

    private void startGame() {
        resetSession();
        soundPlayer.startBackgroundMusic();
        repaintGamePanel();
    }

    private void restartGame() {
        resetSession();
        soundPlayer.startBackgroundMusic();
        repaintGamePanel();
    }

    private void resetSession() {
        score = 0;
        wave = 1;
        lives = DEFAULT_LIVES;
        gameState = GameState.PLAYING;
        hitFeedbackTicks = 0;
        gameOverTitle = DEFAULT_GAME_OVER_TITLE;
        missiles.clear();
        alienMissiles.clear();
        pressedMovementKeys.clear();
        spacePressed = false;
        playerFireCooldownTicks = 0;
        generateSpaceObjects();
        updatePanelState();
    }

    private void checkAlienInvasion() {
        boolean alienReachedBottom = aliens.stream()
            .anyMatch(alien -> alien.getY() + GameConstants.COMPONENT_SIZE >= GameConstants.PANEL_HEIGHT);
        if (alienReachedBottom) {
            enterGameOver(ALIENS_WIN_TITLE);
        }
    }

    private void enterGameOver(String title) {
        gameState = GameState.GAME_OVER;
        gameOverTitle = title;
        pressedMovementKeys.clear();
        spacePressed = false;
        soundPlayer.stopBackgroundMusic();
    }

    private void updateHitFeedback() {
        if (hitFeedbackTicks > 0) {
            hitFeedbackTicks--;
        }
    }

    private void updatePlayerFireCooldown() {
        if (playerFireCooldownTicks > 0) {
            playerFireCooldownTicks--;
        }
    }

    private void fireHeldPlayerMissileIfReady() {
        if (spacePressed) {
            firePlayerMissileIfReady();
        }
    }

    private void firePlayerMissileIfReady() {
        if (playerFireCooldownTicks > 0) {
            return;
        }

        missiles.add(new Missile(spaceship.getX(), spaceship.getY() - GameConstants.COMPONENT_SIZE));
        playerFireCooldownTicks = PLAYER_FIRE_COOLDOWN_TICKS;
        soundPlayer.playShoot();
        repaintGamePanel();
    }

    void fireAlienMissileIfReady() {
        if (aliens.isEmpty() || alienMissiles.size() >= MAX_ALIEN_MISSILES) {
            return;
        }
        if (random.nextDouble() > ALIEN_FIRE_CHANCE) {
            return;
        }

        Alien alien = aliens.get(random.nextInt(aliens.size()));
        int missileX = alien.getX();
        int missileY = alien.getY() + GameConstants.COMPONENT_SIZE;
        alienMissiles.add(new AlienMissile(missileX, missileY));
    }

    private void loseLife() {
        hitFeedbackTicks = HIT_FEEDBACK_TICKS;
        lives--;
        if (lives <= 0) {
            lives = 0;
            enterGameOver(DEFAULT_GAME_OVER_TITLE);
        }
    }
}
