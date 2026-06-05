package com.emenems.games.aliens.controller;

import com.emenems.games.aliens.GameConstants;
import com.emenems.games.aliens.GameRules;
import com.emenems.games.aliens.GameSession;
import com.emenems.games.aliens.GameState;
import com.emenems.games.aliens.audio.ArcadeSoundPlayer;
import com.emenems.games.aliens.gamemachines.Alien;
import com.emenems.games.aliens.gamemachines.AlienExplosion;
import com.emenems.games.aliens.gamemachines.AlienType;
import com.emenems.games.aliens.gamemachines.AlienMissile;
import com.emenems.games.aliens.gamemachines.Missile;
import com.emenems.games.aliens.gamemachines.RapidFirePowerUp;
import com.emenems.games.aliens.gamemachines.Spaceship;
import com.emenems.games.aliens.gui.GamePanel;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import javax.swing.Timer;

public class GameController implements ActionListener {
    private static final int TIMER_DELAY_MS = 16;
    private static final int ALIEN_COUNT = 6;
    private static final int ALIEN_START_MIN_Y = 30;
    private static final int ALIEN_START_X_JITTER = 20;
    private static final int ALIEN_START_Y_JITTER = 90;
    static final int MAX_ALIEN_MISSILES = 5;
    private static final int PLAYER_FIRE_COOLDOWN_TICKS = 14;
    private static final int RAPID_FIRE_COOLDOWN_TICKS = 7;
    private static final double RAPID_FIRE_DROP_CHANCE = 0.12;
    private static final int ALIEN_MISSILE_HITBOX_X_OFFSET = 9;
    private static final int ALIEN_MISSILE_HITBOX_WIDTH = 7;

    private final Spaceship spaceship;
    private final List<Missile> missiles;
    private final List<AlienMissile> alienMissiles;
    private final List<Alien> aliens;
    private final List<AlienExplosion> alienExplosions;
    private final List<RapidFirePowerUp> rapidFirePowerUps;
    private final GamePanel gamePanel;
    private final Random random;
    private final ArcadeSoundPlayer soundPlayer;
    private final GameSession session = new GameSession();
    private final Set<Integer> pressedMovementKeys = new HashSet<>();
    private Timer timer;
    private int playerFireCooldownTicks;
    private boolean spacePressed;

    public GameController(
        Spaceship spaceship,
        List<Missile> missiles,
        List<AlienMissile> alienMissiles,
        List<Alien> aliens,
        GamePanel gamePanel
    ) {
        this(spaceship, missiles, alienMissiles, aliens, new ArrayList<>(), new ArrayList<>(), gamePanel);
    }

    public GameController(
        Spaceship spaceship,
        List<Missile> missiles,
        List<AlienMissile> alienMissiles,
        List<Alien> aliens,
        List<RapidFirePowerUp> rapidFirePowerUps,
        GamePanel gamePanel
    ) {
        this(
            spaceship,
            missiles,
            alienMissiles,
            aliens,
            new ArrayList<>(),
            rapidFirePowerUps,
            gamePanel,
            new Random(),
            new ArcadeSoundPlayer()
        );
    }

    public GameController(
        Spaceship spaceship,
        List<Missile> missiles,
        List<AlienMissile> alienMissiles,
        List<Alien> aliens,
        List<AlienExplosion> alienExplosions,
        List<RapidFirePowerUp> rapidFirePowerUps,
        GamePanel gamePanel
    ) {
        this(
            spaceship,
            missiles,
            alienMissiles,
            aliens,
            alienExplosions,
            rapidFirePowerUps,
            gamePanel,
            new Random(),
            new ArcadeSoundPlayer()
        );
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
        this(
            spaceship,
            missiles,
            alienMissiles,
            aliens,
            new ArrayList<>(),
            new ArrayList<>(),
            gamePanel,
            random,
            soundPlayer
        );
    }

    GameController(
        Spaceship spaceship,
        List<Missile> missiles,
        List<AlienMissile> alienMissiles,
        List<Alien> aliens,
        List<RapidFirePowerUp> rapidFirePowerUps,
        GamePanel gamePanel,
        Random random,
        ArcadeSoundPlayer soundPlayer
    ) {
        this(
            spaceship,
            missiles,
            alienMissiles,
            aliens,
            new ArrayList<>(),
            rapidFirePowerUps,
            gamePanel,
            random,
            soundPlayer
        );
    }

    GameController(
        Spaceship spaceship,
        List<Missile> missiles,
        List<AlienMissile> alienMissiles,
        List<Alien> aliens,
        List<AlienExplosion> alienExplosions,
        List<RapidFirePowerUp> rapidFirePowerUps,
        GamePanel gamePanel,
        Random random,
        ArcadeSoundPlayer soundPlayer
    ) {
        this.spaceship = spaceship;
        this.missiles = missiles;
        this.alienMissiles = alienMissiles;
        this.aliens = aliens;
        this.alienExplosions = alienExplosions;
        this.rapidFirePowerUps = rapidFirePowerUps;
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
        double alienSpeed = GameRules.alienSpeedForWave(session.getWave());
        int laneSpacing = (GameConstants.PANEL_WIDTH - GameConstants.COMPONENT_SIZE) / ALIEN_COUNT;
        int specialIndex = GameRules.hasSpecialAlien(session.getWave()) ? random.nextInt(ALIEN_COUNT) : -1;
        for (int index = 0; index < ALIEN_COUNT; index++) {
            int laneX = index * laneSpacing + (laneSpacing - GameConstants.COMPONENT_SIZE) / 2;
            int x = laneX + random.nextInt(ALIEN_START_X_JITTER * 2 + 1) - ALIEN_START_X_JITTER;
            int y = ALIEN_START_MIN_Y + random.nextInt(ALIEN_START_Y_JITTER + 1);
            aliens.add(index == specialIndex
                ? Alien.special(x, y, alienSpeed)
                : new Alien(x, y, alienSpeed));
        }
    }

    void handleKeyPressed(int keyCode) {
        switch (session.getGameState()) {
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
        if (session.getGameState() != GameState.PLAYING) {
            updatePanelState();
            return;
        }

        updateHitFeedback();
        session.tickWaveMessage();
        session.tickRapidFire();
        session.tickCombo();
        updatePlayerFireCooldown();
        fireHeldPlayerMissileIfReady();
        moveSpaceshipFromPressedKeys();
        updateAlienExplosions();
        moveAliens();
        missiles.forEach(Missile::move);
        alienMissiles.forEach(AlienMissile::move);
        rapidFirePowerUps.forEach(RapidFirePowerUp::move);
        fireAlienMissileIfReady();
        checkCollisions();
        if (session.getGameState() == GameState.GAME_OVER) {
            updatePanelState();
            return;
        }
        checkAlienInvasion();
        if (session.getGameState() == GameState.GAME_OVER) {
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
        checkCollisionsWithRapidFirePowerUp();
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
                    if (alien.takeHit()) {
                        aliensToRemove.add(alien);
                    }
                    break;
                }
            }
        }

        missiles.removeAll(missilesToRemove);
        spawnAlienExplosions(aliensToRemove);
        spawnRapidFirePowerUps(aliensToRemove);
        aliens.removeAll(aliensToRemove);
        session.addAlienKills(aliensToRemove.size());
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

    void checkCollisionsWithRapidFirePowerUp() {
        Rectangle spaceshipArea = objectArea(spaceship.getX(), spaceship.getY());
        RapidFirePowerUp collectedPowerUp = rapidFirePowerUps.stream()
            .filter(powerUp -> spaceshipArea.intersects(objectArea(powerUp.getX(), powerUp.getY())))
            .findFirst()
            .orElse(null);

        if (collectedPowerUp == null) {
            return;
        }

        rapidFirePowerUps.remove(collectedPowerUp);
        session.activateRapidFire();
    }

    void cleanupOffscreenObjects() {
        missiles.removeIf(missile -> missile.getY() + GameConstants.COMPONENT_SIZE < 0);
        alienMissiles.removeIf(missile -> missile.getY() > GameConstants.PANEL_HEIGHT);
        rapidFirePowerUps.removeIf(powerUp -> powerUp.getY() > GameConstants.PANEL_HEIGHT);
        aliens.removeIf(alien ->
            alien.getY() > GameConstants.PANEL_HEIGHT
                || alien.getX() + GameConstants.COMPONENT_SIZE < 0
                || alien.getX() > GameConstants.PANEL_WIDTH);
    }

    int getScore() {
        return session.getScore();
    }

    int getWave() {
        return session.getWave();
    }

    int getLives() {
        return session.getLives();
    }

    GameState getGameState() {
        return session.getGameState();
    }

    boolean isHitFeedbackActive() {
        return session.isHitFeedbackActive();
    }

    String getGameOverTitle() {
        return session.getGameOverTitle();
    }

    boolean isRapidFireActive() {
        return session.isRapidFireActive();
    }

    boolean isWaveMessageActive() {
        return session.isWaveMessageActive();
    }

    int getWaveMessageTicks() {
        return session.getWaveMessageTicks();
    }

    int getRapidFireTicks() {
        return session.getRapidFireTicks();
    }

    int getComboMultiplier() {
        return session.getComboMultiplier();
    }

    int getComboTicks() {
        return session.getComboTicks();
    }

    int getPlayerFireCooldownTicks() {
        return playerFireCooldownTicks;
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

        session.advanceWave();
        generateSpaceObjects();
    }

    private void updatePanelState() {
        if (gamePanel != null) {
            gamePanel.updateGameState(
                session.getScore(),
                session.getWave(),
                session.getLives(),
                session.getGameState(),
                session.isHitFeedbackActive(),
                session.getGameOverTitle(),
                session.isWaveMessageActive(),
                session.getWaveMessageTicks(),
                session.isRapidFireActive(),
                session.getRapidFireTicks(),
                session.getComboMultiplier(),
                session.getComboTicks()
            );
        }
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
        session.startOrRestart();
        spaceship.moveTo(
            (GameConstants.PANEL_WIDTH - GameConstants.COMPONENT_SIZE) / 2,
            GameConstants.PANEL_HEIGHT - GameConstants.COMPONENT_SIZE - GameConstants.SPACESHIP_START_BOTTOM_MARGIN
        );
        missiles.clear();
        alienMissiles.clear();
        alienExplosions.clear();
        rapidFirePowerUps.clear();
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
            session.enterAlienInvasionGameOver();
            clearInputAndStopMusic();
        }
    }

    private void clearInputAndStopMusic() {
        pressedMovementKeys.clear();
        spacePressed = false;
        soundPlayer.stopBackgroundMusic();
    }

    private void updateHitFeedback() {
        session.tickHitFeedback();
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
        playerFireCooldownTicks = session.isRapidFireActive()
            ? RAPID_FIRE_COOLDOWN_TICKS
            : PLAYER_FIRE_COOLDOWN_TICKS;
        soundPlayer.playShoot();
        repaintGamePanel();
    }

    void fireAlienMissileIfReady() {
        if (aliens.isEmpty() || alienMissiles.size() >= MAX_ALIEN_MISSILES) {
            return;
        }
        if (random.nextDouble() > GameRules.alienFireChanceForWave(session.getWave(), aliens.size())) {
            return;
        }

        Alien alien = selectAlienShooter();
        int missileX = alien.getX();
        int missileY = alien.getY() + GameConstants.COMPONENT_SIZE;
        alienMissiles.add(new AlienMissile(missileX, missileY));
    }

    private void spawnRapidFirePowerUps(Set<Alien> destroyedAliens) {
        for (Alien alien : destroyedAliens) {
            if (random.nextDouble() < RAPID_FIRE_DROP_CHANCE) {
                rapidFirePowerUps.add(new RapidFirePowerUp(alien.getX(), alien.getY()));
            }
        }
    }

    private void spawnAlienExplosions(Set<Alien> destroyedAliens) {
        destroyedAliens.forEach(alien -> alienExplosions.add(new AlienExplosion(alien.getX(), alien.getY())));
    }

    private void updateAlienExplosions() {
        alienExplosions.forEach(AlienExplosion::tick);
        alienExplosions.removeIf(AlienExplosion::isExpired);
    }

    private void loseLife() {
        session.loseLife();
        if (session.getGameState() == GameState.GAME_OVER) {
            clearInputAndStopMusic();
        }
    }

    private void moveAliens() {
        int minX = 0;
        int maxX = GameConstants.PANEL_WIDTH - GameConstants.COMPONENT_SIZE;
        aliens.forEach(alien -> {
            alien.move(random, minX, maxX);
            Alien collidingAlien = findCollidingAlien(alien);
            if (alien.isSpecial() && collidingAlien != null) {
                alien.separateFrom(collidingAlien, minX, maxX);
            }
        });
    }

    private Alien selectAlienShooter() {
        int totalWeight = aliens.stream()
            .mapToInt(alien -> GameRules.alienFiringWeight(alien.getType() == AlienType.SPECIAL))
            .sum();
        int roll = random.nextInt(totalWeight);
        for (Alien alien : aliens) {
            roll -= GameRules.alienFiringWeight(alien.getType() == AlienType.SPECIAL);
            if (roll < 0) {
                return alien;
            }
        }
        return aliens.getFirst();
    }

    private Alien findCollidingAlien(Alien alien) {
        Rectangle alienArea = objectArea(alien.getX(), alien.getY());
        return aliens.stream()
            .filter(otherAlien -> otherAlien != alien)
            .filter(otherAlien -> alienArea.intersects(objectArea(otherAlien.getX(), otherAlien.getY())))
            .findFirst()
            .orElse(null);
    }
}
