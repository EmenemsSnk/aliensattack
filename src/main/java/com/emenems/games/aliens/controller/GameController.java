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
import com.emenems.games.aliens.gamemachines.PowerUp;
import com.emenems.games.aliens.gamemachines.PowerUpType;
import com.emenems.games.aliens.gamemachines.Spaceship;
import com.emenems.games.aliens.gui.GamePanel;
import com.emenems.games.aliens.profiles.PlayerProfile;
import com.emenems.games.aliens.profiles.ProfileMenuState;
import com.emenems.games.aliens.profiles.ProfileNameValidator;
import com.emenems.games.aliens.profiles.ProfileStore;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import javax.swing.Timer;

public class GameController implements ActionListener {
    private static final int TIMER_DELAY_MS = 16;
    private static final int ALIEN_COUNT = 6;
    private static final int ALIEN_START_MIN_Y = 30;
    private static final int ALIEN_START_X_JITTER = 20;
    private static final int ALIEN_START_Y_JITTER = 90;
    private static final int BOSS_SPAWN_MARGIN_X = 96;
    static final int MAX_ALIEN_MISSILES = 5;
    private static final int PLAYER_FIRE_COOLDOWN_TICKS = 14;
    private static final int RAPID_FIRE_COOLDOWN_TICKS = 7;
    private static final int ALIEN_MISSILE_HITBOX_X_OFFSET = 9;
    private static final int ALIEN_MISSILE_HITBOX_WIDTH = 7;

    private final Spaceship spaceship;
    private final List<Missile> missiles;
    private final List<AlienMissile> alienMissiles;
    private final List<Alien> aliens;
    private final List<AlienExplosion> alienExplosions;
    private final List<PowerUp> powerUps;
    private final GamePanel gamePanel;
    private final Random random;
    private final ArcadeSoundPlayer soundPlayer;
    private final ProfileStore profileStore;
    private final GameSession session = new GameSession();
    private final Set<Integer> pressedMovementKeys = new HashSet<>();
    private List<PlayerProfile> profiles = new ArrayList<>();
    private Timer timer;
    private int playerFireCooldownTicks;
    private boolean spacePressed;
    private int selectedProfileIndex = -1;
    private boolean profileInputMode;
    private String profileDraftName = "";
    private String profileStatusMessage = "Create a profile with N";
    private boolean profileSaveFailed;
    private boolean newBestScore;
    private boolean gameOverScoreHandled;

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
        List<PowerUp> powerUps,
        GamePanel gamePanel
    ) {
        this(
            spaceship,
            missiles,
            alienMissiles,
            aliens,
            new ArrayList<>(),
            powerUps,
            gamePanel,
            new Random(),
            new ArcadeSoundPlayer(),
            new ProfileStore()
        );
    }

    public GameController(
        Spaceship spaceship,
        List<Missile> missiles,
        List<AlienMissile> alienMissiles,
        List<Alien> aliens,
        List<AlienExplosion> alienExplosions,
        List<PowerUp> powerUps,
        GamePanel gamePanel
    ) {
        this(
            spaceship,
            missiles,
            alienMissiles,
            aliens,
            alienExplosions,
            powerUps,
            gamePanel,
            new Random(),
            new ArcadeSoundPlayer(),
            new ProfileStore()
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
            soundPlayer,
            new ProfileStore()
        );
    }

    GameController(
        Spaceship spaceship,
        List<Missile> missiles,
        List<AlienMissile> alienMissiles,
        List<Alien> aliens,
        List<PowerUp> powerUps,
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
            powerUps,
            gamePanel,
            random,
            soundPlayer,
            new ProfileStore()
        );
    }

    GameController(
        Spaceship spaceship,
        List<Missile> missiles,
        List<AlienMissile> alienMissiles,
        List<Alien> aliens,
        List<AlienExplosion> alienExplosions,
        List<PowerUp> powerUps,
        GamePanel gamePanel,
        Random random,
        ArcadeSoundPlayer soundPlayer
    ) {
        this(
            spaceship,
            missiles,
            alienMissiles,
            aliens,
            alienExplosions,
            powerUps,
            gamePanel,
            random,
            soundPlayer,
            new ProfileStore()
        );
    }

    GameController(
        Spaceship spaceship,
        List<Missile> missiles,
        List<AlienMissile> alienMissiles,
        List<Alien> aliens,
        List<AlienExplosion> alienExplosions,
        List<PowerUp> powerUps,
        GamePanel gamePanel,
        Random random,
        ArcadeSoundPlayer soundPlayer,
        ProfileStore profileStore
    ) {
        this.spaceship = spaceship;
        this.missiles = missiles;
        this.alienMissiles = alienMissiles;
        this.aliens = aliens;
        this.alienExplosions = alienExplosions;
        this.powerUps = powerUps;
        this.gamePanel = gamePanel;
        this.random = random;
        this.soundPlayer = soundPlayer;
        this.profileStore = profileStore;
    }

    public void initialize() {
        loadProfiles();
        generateSpaceObjects();
        updatePanelState();
        gamePanel.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                handleKeyPressed(e.getKeyCode(), e.getKeyChar());
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
        if (GameRules.isBossWave(session.getWave())) {
            spawnBossWave();
            return;
        }

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

    private void spawnBossWave() {
        int minBossX = BOSS_SPAWN_MARGIN_X;
        int maxBossX = GameConstants.PANEL_WIDTH - GameConstants.COMPONENT_SIZE - BOSS_SPAWN_MARGIN_X;
        int spawnX = minBossX + random.nextInt(maxBossX - minBossX + 1);
        aliens.add(Alien.boss(spawnX, GameRules.bossTopLaneY()));
    }

    void handleKeyPressed(int keyCode) {
        handleKeyPressed(keyCode, KeyEvent.CHAR_UNDEFINED);
    }

    void handleKeyPressed(int keyCode, char keyChar) {
        switch (session.getGameState()) {
            case START_MENU -> handleStartMenuKeyPressed(keyCode, keyChar);
            case GAME_OVER -> {
                if (keyCode == KeyEvent.VK_ENTER) {
                    restartGame();
                }
            }
            case PAUSED -> {
                if (keyCode == KeyEvent.VK_P) {
                    resumeGame();
                }
            }
            case PLAYING -> handlePlayingKeyPressed(keyCode);
        }
    }

    private void handleStartMenuKeyPressed(int keyCode, char keyChar) {
        newBestScore = false;
        if (profileInputMode) {
            handleProfileInputKeyPressed(keyCode, keyChar);
            return;
        }

        if (keyCode == KeyEvent.VK_ENTER) {
            startGame();
        } else if (keyCode == KeyEvent.VK_N) {
            beginProfileInput();
        } else if (keyCode == KeyEvent.VK_LEFT) {
            selectPreviousProfile();
        } else if (keyCode == KeyEvent.VK_RIGHT) {
            selectNextProfile();
        }
    }

    private void handleProfileInputKeyPressed(int keyCode, char keyChar) {
        if (keyCode == KeyEvent.VK_ENTER) {
            createProfileFromDraft();
        } else if (keyCode == KeyEvent.VK_ESCAPE) {
            cancelProfileInput();
        } else if (keyCode == KeyEvent.VK_BACK_SPACE) {
            if (!profileDraftName.isEmpty()) {
                profileDraftName = profileDraftName.substring(0, profileDraftName.length() - 1);
            }
            profileStatusMessage = "Type a profile name";
        } else if (keyChar != KeyEvent.CHAR_UNDEFINED && ProfileNameValidator.isAllowedCharacter(keyChar)) {
            if (profileDraftName.length() < ProfileNameValidator.MAX_NAME_LENGTH) {
                profileDraftName += keyChar;
                profileStatusMessage = "Press ENTER to save";
            } else {
                profileStatusMessage = ProfileNameValidator.TOO_LONG_MESSAGE;
            }
        }
        updatePanelState();
        repaintGamePanel();
    }

    private void handlePlayingKeyPressed(int keyCode) {
        if (keyCode == KeyEvent.VK_P) {
            pauseGame();
        } else if (keyCode == KeyEvent.VK_SPACE) {
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
        int moveStep = session.isSpeedBoostActive()
            ? GameRules.speedBoostMoveStep()
            : Spaceship.defaultMoveStep();
        if (pressedMovementKeys.contains(KeyEvent.VK_LEFT)) {
            spaceship.moveLeft(moveStep);
        }
        if (pressedMovementKeys.contains(KeyEvent.VK_RIGHT)) {
            spaceship.moveRight(moveStep);
        }
        if (pressedMovementKeys.contains(KeyEvent.VK_UP)) {
            spaceship.moveUp(moveStep);
        }
        if (pressedMovementKeys.contains(KeyEvent.VK_DOWN)) {
            spaceship.moveDown(moveStep);
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
        session.tickSpeedBoost();
        session.tickCombo();
        updatePlayerFireCooldown();
        fireHeldPlayerMissileIfReady();
        moveSpaceshipFromPressedKeys();
        updateAlienExplosions();
        moveAliens();
        missiles.forEach(Missile::move);
        alienMissiles.forEach(AlienMissile::move);
        powerUps.forEach(PowerUp::move);
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
        checkCollisionsWithPowerUp();
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

        if (collidingAlien.isBoss()) {
            return;
        }

        aliens.remove(collidingAlien);
        if (!session.consumeShield()) {
            loseLife();
        }
    }

    void checkCollisionsWithMissile() {
        Set<Missile> missilesToRemove = new HashSet<>();
        Set<Alien> aliensToRemove = new HashSet<>();
        int bossKills = 0;

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
                        if (alien.isBoss()) {
                            bossKills++;
                        }
                    }
                    break;
                }
            }
        }

        missiles.removeAll(missilesToRemove);
        spawnAlienExplosions(aliensToRemove);
        spawnBossRewards(aliensToRemove);
        spawnSupportPowerUps(aliensToRemove);
        aliens.removeAll(aliensToRemove);
        session.addAlienKills(aliensToRemove.size() - bossKills);
        if (bossKills > 0) {
            session.addAlienKills(bossKills);
            session.addBossBonus();
        }
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
        if (!session.consumeShield()) {
            loseLife();
        }
    }

    void checkCollisionsWithPowerUp() {
        Rectangle spaceshipArea = objectArea(spaceship.getX(), spaceship.getY());
        PowerUp collectedPowerUp = powerUps.stream()
            .filter(powerUp -> spaceshipArea.intersects(objectArea(powerUp.getX(), powerUp.getY())))
            .findFirst()
            .orElse(null);

        if (collectedPowerUp == null) {
            return;
        }

        powerUps.remove(collectedPowerUp);
        applyPowerUpEffect(collectedPowerUp.getType());
    }

    void cleanupOffscreenObjects() {
        missiles.removeIf(missile -> missile.getY() + GameConstants.COMPONENT_SIZE < 0);
        alienMissiles.removeIf(missile -> missile.getY() > GameConstants.PANEL_HEIGHT);
        powerUps.removeIf(powerUp -> powerUp.getY() > GameConstants.PANEL_HEIGHT);
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

    ProfileMenuState getProfileMenuState() {
        PlayerProfile selectedProfile = selectedProfile();
        return new ProfileMenuState(
            selectedProfile == null ? "" : selectedProfile.name(),
            selectedProfile == null ? 0 : selectedProfile.bestScore(),
            profiles.size(),
            selectedProfileIndex,
            profileInputMode,
            profileDraftName,
            profileStatusMessage,
            profileSaveFailed,
            newBestScore,
            topProfiles()
        );
    }

    void replaceProfilesForTesting(List<PlayerProfile> testProfiles) {
        profiles = new ArrayList<>(testProfiles);
        selectedProfileIndex = profiles.isEmpty() ? -1 : 0;
        profileInputMode = false;
        profileDraftName = "";
        profileStatusMessage = profiles.isEmpty()
            ? "Create a profile with N"
            : "Selected profile: " + profiles.get(selectedProfileIndex).name();
        profileSaveFailed = false;
        newBestScore = false;
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

    boolean isShieldActive() {
        return session.isShieldActive();
    }

    boolean isSpeedBoostActive() {
        return session.isSpeedBoostActive();
    }

    int getSpeedBoostTicks() {
        return session.getSpeedBoostTicks();
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
                session.isShieldActive(),
                session.isSpeedBoostActive(),
                session.getSpeedBoostTicks(),
                session.getComboMultiplier(),
                session.getComboTicks(),
                getProfileMenuState()
            );
        }
    }

    private void repaintGamePanel() {
        if (gamePanel != null) {
            gamePanel.repaint();
        }
    }

    private void startGame() {
        if (!hasSelectedProfile()) {
            profileStatusMessage = "Create or select a profile first";
            updatePanelState();
            repaintGamePanel();
            return;
        }
        resetSession();
        soundPlayer.startBackgroundMusic();
        repaintGamePanel();
    }

    private void restartGame() {
        resetSession();
        soundPlayer.startBackgroundMusic();
        repaintGamePanel();
    }

    private void pauseGame() {
        session.pause();
        clearHeldInput();
        soundPlayer.stopBackgroundMusic();
        updatePanelState();
        repaintGamePanel();
    }

    private void resumeGame() {
        session.resume();
        soundPlayer.startBackgroundMusic();
        updatePanelState();
        repaintGamePanel();
    }

    private void resetSession() {
        session.startOrRestart();
        gameOverScoreHandled = false;
        newBestScore = false;
        profileSaveFailed = false;
        spaceship.moveTo(
            (GameConstants.PANEL_WIDTH - GameConstants.COMPONENT_SIZE) / 2,
            GameConstants.PANEL_HEIGHT - GameConstants.COMPONENT_SIZE - GameConstants.SPACESHIP_START_BOTTOM_MARGIN
        );
        missiles.clear();
        alienMissiles.clear();
        alienExplosions.clear();
        powerUps.clear();
        clearHeldInput();
        generateSpaceObjects();
        updatePanelState();
    }

    private void checkAlienInvasion() {
        boolean alienReachedBottom = aliens.stream()
            .filter(alien -> !alien.isBoss())
            .anyMatch(alien -> alien.getY() + GameConstants.COMPONENT_SIZE >= GameConstants.PANEL_HEIGHT);
        if (alienReachedBottom) {
            session.enterAlienInvasionGameOver();
            clearInputAndStopMusic();
            handleGameOverScoreUpdate();
        }
    }

    private void clearInputAndStopMusic() {
        clearHeldInput();
        soundPlayer.stopBackgroundMusic();
    }

    private void clearHeldInput() {
        pressedMovementKeys.clear();
        spacePressed = false;
        playerFireCooldownTicks = 0;
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
        Alien alien = selectAlienShooter();
        double fireChance = alien.isBoss()
            ? GameRules.bossFireChance()
            : GameRules.alienFireChanceForWave(session.getWave(), aliens.size());
        if (random.nextDouble() > fireChance) {
            return;
        }

        if (alien.isBoss()) {
            fireBossBurst(alien);
            return;
        }

        int missileX = alien.getX();
        int missileY = alien.getY() + GameConstants.COMPONENT_SIZE;
        alienMissiles.add(new AlienMissile(missileX, missileY));
    }

    private void fireBossBurst(Alien boss) {
        int availableSlots = MAX_ALIEN_MISSILES - alienMissiles.size();
        int burstCount = Math.min(GameRules.bossBurstCount(), availableSlots);
        int centerIndex = burstCount / 2;
        for (int index = 0; index < burstCount; index++) {
            int offsetIndex = index - centerIndex;
            int missileX = boss.getX() + offsetIndex * GameRules.bossBurstSpacing();
            missileX = Math.clamp(missileX, 0, GameConstants.PANEL_WIDTH - GameConstants.COMPONENT_SIZE);
            int missileY = boss.getY() + GameConstants.COMPONENT_SIZE;
            alienMissiles.add(new AlienMissile(missileX, missileY));
        }
    }

    private void spawnSupportPowerUps(Set<Alien> destroyedAliens) {
        for (Alien alien : destroyedAliens) {
            if (alien.isBoss()) {
                continue;
            }
            if (random.nextDouble() < GameRules.supportDropChance()) {
                int typeRoll = random.nextInt(GameRules.totalSupportDropWeight());
                PowerUpType type = GameRules.supportDropTypeForRoll(typeRoll);
                powerUps.add(new PowerUp(type, alien.getX(), alien.getY()));
            }
        }
    }

    private void spawnBossRewards(Set<Alien> destroyedAliens) {
        for (Alien alien : destroyedAliens) {
            if (!alien.isBoss()) {
                continue;
            }

            int rewardRoll = random.nextInt(GameRules.totalBossRewardWeight());
            PowerUpType rewardType = GameRules.bossRewardTypeForRoll(rewardRoll);
            powerUps.add(new PowerUp(rewardType, alien.getX(), alien.getY()));
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
        soundPlayer.playLifeLoss();
        if (session.getGameState() == GameState.GAME_OVER) {
            clearInputAndStopMusic();
            handleGameOverScoreUpdate();
        }
    }

    void loadProfiles() {
        profiles = new ArrayList<>(profileStore.loadProfiles());
        selectedProfileIndex = profiles.isEmpty() ? -1 : 0;
        profileStatusMessage = profiles.isEmpty()
            ? "Create a profile with N"
            : "Selected profile: " + profiles.get(selectedProfileIndex).name();
        profileSaveFailed = false;
    }

    private void beginProfileInput() {
        profileInputMode = true;
        profileDraftName = "";
        profileStatusMessage = "Type a profile name";
        updatePanelState();
        repaintGamePanel();
    }

    private void cancelProfileInput() {
        profileInputMode = false;
        profileDraftName = "";
        profileStatusMessage = hasSelectedProfile() ? "Profile creation cancelled" : "Create a profile with N";
    }

    private void createProfileFromDraft() {
        ProfileNameValidator.ValidationResult result = ProfileNameValidator.validate(profileDraftName);
        if (!result.valid()) {
            profileStatusMessage = result.message();
            return;
        }
        if (profileNameExists(result.normalizedName())) {
            profileStatusMessage = "Profile already exists";
            return;
        }

        PlayerProfile profile = new PlayerProfile(result.normalizedName(), 0);
        profiles.add(profile);
        selectedProfileIndex = profiles.size() - 1;
        profileInputMode = false;
        profileDraftName = "";
        ProfileStore.SaveResult saveResult = profileStore.saveProfiles(profiles);
        profileSaveFailed = !saveResult.success();
        profileStatusMessage = saveResult.success()
            ? "Created profile: " + profile.name()
            : "Profile created, but save failed";
    }

    private boolean profileNameExists(String name) {
        String key = name.toLowerCase(Locale.ROOT);
        return profiles.stream()
            .map(profile -> profile.name().toLowerCase(Locale.ROOT))
            .anyMatch(key::equals);
    }

    private void selectPreviousProfile() {
        if (profiles.isEmpty()) {
            profileStatusMessage = "Create a profile with N";
            return;
        }
        selectedProfileIndex = Math.floorMod(selectedProfileIndex - 1, profiles.size());
        profileStatusMessage = "Selected profile: " + profiles.get(selectedProfileIndex).name();
        updatePanelState();
        repaintGamePanel();
    }

    private void selectNextProfile() {
        if (profiles.isEmpty()) {
            profileStatusMessage = "Create a profile with N";
            return;
        }
        selectedProfileIndex = Math.floorMod(selectedProfileIndex + 1, profiles.size());
        profileStatusMessage = "Selected profile: " + profiles.get(selectedProfileIndex).name();
        updatePanelState();
        repaintGamePanel();
    }

    private boolean hasSelectedProfile() {
        return selectedProfile() != null;
    }

    private PlayerProfile selectedProfile() {
        if (selectedProfileIndex < 0 || selectedProfileIndex >= profiles.size()) {
            return null;
        }
        return profiles.get(selectedProfileIndex);
    }

    private List<ProfileMenuState.LeaderboardEntry> topProfiles() {
        List<PlayerProfile> rankedProfiles = profiles.stream()
            .sorted(
                Comparator.comparingInt(PlayerProfile::bestScore)
                    .reversed()
                    .thenComparing(PlayerProfile::name)
            )
            .limit(5)
            .toList();
        List<ProfileMenuState.LeaderboardEntry> entries = new ArrayList<>();
        for (int index = 0; index < rankedProfiles.size(); index++) {
            PlayerProfile profile = rankedProfiles.get(index);
            entries.add(new ProfileMenuState.LeaderboardEntry(index + 1, profile.name(), profile.bestScore()));
        }
        return entries;
    }

    private void handleGameOverScoreUpdate() {
        if (gameOverScoreHandled) {
            return;
        }
        gameOverScoreHandled = true;
        PlayerProfile selectedProfile = selectedProfile();
        if (selectedProfile == null || session.getScore() <= selectedProfile.bestScore()) {
            newBestScore = false;
            return;
        }

        PlayerProfile updatedProfile = selectedProfile.withBestScore(session.getScore());
        profiles.set(selectedProfileIndex, updatedProfile);
        ProfileStore.SaveResult saveResult = profileStore.saveProfiles(profiles);
        profileSaveFailed = !saveResult.success();
        newBestScore = true;
        profileStatusMessage = saveResult.success()
            ? "New best score for " + updatedProfile.name()
            : "New best score kept, but save failed";
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

    private void applyPowerUpEffect(PowerUpType type) {
        switch (type) {
            case RAPID_FIRE -> session.activateRapidFire();
            case EXTRA_LIFE -> session.addExtraLife();
            case SHIELD -> session.activateShield();
            case SPEED_BOOST -> session.activateSpeedBoost();
        }
    }
}
