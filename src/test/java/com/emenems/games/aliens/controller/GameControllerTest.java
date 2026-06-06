package com.emenems.games.aliens.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.emenems.games.aliens.GameConstants;
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
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

class GameControllerTest {

    @Test
    void tickDoesNotMoveObjectsWhileStartMenuIsShowing() {
        Spaceship spaceship = new Spaceship(500, 680);
        List<Missile> missiles = new ArrayList<>();
        Missile missile = new Missile(100, 100);
        missiles.add(missile);
        List<Alien> aliens = new ArrayList<>();
        Alien alien = new Alien(100, 100);
        aliens.add(alien);
        GameController controller = new GameController(spaceship, missiles, new ArrayList<>(), aliens, null);

        controller.tick();

        assertEquals(100, missile.getY());
        assertEquals(100, alien.getY());
        assertEquals(GameState.START_MENU, controller.getGameState());
    }

    @Test
    void cleanupRemovesMissilesAbovePanel() {
        List<Missile> missiles = new ArrayList<>();
        Missile offscreen = new Missile(100, -GameConstants.COMPONENT_SIZE - 1);
        Missile visible = new Missile(100, 0);
        missiles.add(offscreen);
        missiles.add(visible);
        List<Alien> aliens = new ArrayList<>();
        GameController controller = newController(missiles, aliens);

        controller.cleanupOffscreenObjects();

        assertEquals(1, missiles.size());
        assertSame(visible, missiles.getFirst());
    }

    @Test
    void cleanupRemovesAliensOutsidePanel() {
        List<Missile> missiles = new ArrayList<>();
        List<Alien> aliens = new ArrayList<>();
        Alien belowPanel = new Alien(100, GameConstants.PANEL_HEIGHT + 1);
        Alien leftOfPanel = new Alien(-GameConstants.COMPONENT_SIZE - 1, 100);
        Alien rightOfPanel = new Alien(GameConstants.PANEL_WIDTH + 1, 100);
        Alien visible = new Alien(100, 100);
        aliens.add(belowPanel);
        aliens.add(leftOfPanel);
        aliens.add(rightOfPanel);
        aliens.add(visible);
        GameController controller = newController(missiles, aliens);

        controller.cleanupOffscreenObjects();

        assertEquals(1, aliens.size());
        assertSame(visible, aliens.getFirst());
    }

    @Test
    void missileAlienCollisionRemovesBothObjects() {
        List<Missile> missiles = new ArrayList<>();
        Missile missile = new Missile(100, 100);
        missiles.add(missile);
        List<Alien> aliens = new ArrayList<>();
        Alien alien = new Alien(100, 100);
        aliens.add(alien);
        List<AlienExplosion> explosions = new ArrayList<>();
        GameController controller = newController(missiles, aliens, explosions);

        controller.checkCollisionsWithMissile();

        assertEquals(0, missiles.size());
        assertEquals(0, aliens.size());
        assertEquals(1, explosions.size());
        assertEquals(100, explosions.getFirst().getX());
        assertEquals(100, explosions.getFirst().getY());
    }

    @Test
    void missileAlienCollisionAddsCurrentWaveScore() {
        List<Missile> missiles = new ArrayList<>();
        missiles.add(new Missile(100, 100));
        List<Alien> aliens = new ArrayList<>();
        aliens.add(new Alien(100, 100));
        GameController controller = newController(missiles, aliens);

        controller.checkCollisionsWithMissile();

        assertEquals(10, controller.getScore());
    }

    @Test
    void firstHitOnSpecialAlienConsumesMissileWithoutKillSideEffects() {
        List<Missile> missiles = new ArrayList<>();
        List<Alien> aliens = new ArrayList<>();
        List<AlienExplosion> explosions = new ArrayList<>();
        List<RapidFirePowerUp> powerUps = new ArrayList<>();
        Alien specialAlien = Alien.special(100, 100, 0);
        missiles.add(new Missile(100, 100));
        aliens.add(specialAlien);
        CountingDropRandom random = new CountingDropRandom(0.0);
        GameController controller = new GameController(
            new Spaceship(500, 600),
            missiles,
            new ArrayList<>(),
            aliens,
            explosions,
            powerUps,
            null,
            random,
            new ArcadeSoundPlayer()
        );

        controller.checkCollisionsWithMissile();

        assertEquals(0, missiles.size());
        assertEquals(1, aliens.size());
        assertSame(specialAlien, aliens.getFirst());
        assertTrue(specialAlien.isDamaged());
        assertEquals(0, controller.getScore());
        assertEquals(0, explosions.size());
        assertEquals(0, powerUps.size());
        assertEquals(0, random.doubleCalls);
    }

    @Test
    void secondHitOnSpecialAlienAppliesNormalKillEffectsOnce() {
        List<Missile> missiles = new ArrayList<>();
        List<Alien> aliens = new ArrayList<>();
        List<AlienExplosion> explosions = new ArrayList<>();
        Alien specialAlien = Alien.special(100, 100, 0);
        missiles.add(new Missile(100, 100));
        missiles.add(new Missile(100, 100));
        aliens.add(specialAlien);
        CountingDropRandom random = new CountingDropRandom(0.5);
        GameController controller = new GameController(
            new Spaceship(500, 600),
            missiles,
            new ArrayList<>(),
            aliens,
            explosions,
            new ArrayList<>(),
            null,
            random,
            new ArcadeSoundPlayer()
        );

        controller.checkCollisionsWithMissile();

        assertEquals(0, aliens.size());
        assertEquals(10, controller.getScore());
        assertEquals(1, controller.getComboMultiplier());
        assertEquals(1, explosions.size());
        assertEquals(1, random.doubleCalls);
    }

    @Test
    void sameTickMultipleHitsDestroySpecialAlienOnlyOnce() {
        List<Missile> missiles = new ArrayList<>();
        List<Alien> aliens = new ArrayList<>();
        List<AlienExplosion> explosions = new ArrayList<>();
        Alien specialAlien = Alien.special(100, 100, 0);
        missiles.add(new Missile(100, 100));
        missiles.add(new Missile(100, 100));
        missiles.add(new Missile(100, 100));
        aliens.add(specialAlien);
        CountingDropRandom random = new CountingDropRandom(0.5);
        GameController controller = new GameController(
            new Spaceship(500, 600),
            missiles,
            new ArrayList<>(),
            aliens,
            explosions,
            new ArrayList<>(),
            null,
            random,
            new ArcadeSoundPlayer()
        );

        controller.checkCollisionsWithMissile();

        assertEquals(0, aliens.size());
        assertEquals(10, controller.getScore());
        assertEquals(1, controller.getComboMultiplier());
        assertEquals(1, explosions.size());
        assertEquals(1, random.doubleCalls);
    }

    @Test
    void sameTickMultipleKillsCreateMultipleExplosions() {
        List<Missile> missiles = new ArrayList<>();
        List<Alien> aliens = new ArrayList<>();
        List<AlienExplosion> explosions = new ArrayList<>();
        missiles.add(new Missile(100, 100));
        missiles.add(new Missile(200, 100));
        aliens.add(new Alien(100, 100, 0));
        aliens.add(new Alien(200, 100, 0));
        GameController controller = newController(missiles, aliens, explosions);

        controller.checkCollisionsWithMissile();

        assertEquals(2, explosions.size());
    }

    @Test
    void playingTicksExpireAlienExplosions() {
        List<AlienExplosion> explosions = new ArrayList<>();
        GameController controller = new GameController(
            new Spaceship(500, 680),
            new ArrayList<>(),
            new ArrayList<>(),
            new ArrayList<>(),
            explosions,
            new ArrayList<>(),
            null
        );
        startPlaying(controller);
        explosions.add(new AlienExplosion(100, 100));

        for (int tick = 0; tick < 11; tick++) {
            controller.tick();
            assertEquals(1, explosions.size());
        }

        controller.tick();

        assertEquals(0, explosions.size());
    }

    @Test
    void oneMissileCannotRemoveMultipleAliensInOneTick() {
        List<Missile> missiles = new ArrayList<>();
        Missile missile = new Missile(100, 100);
        missiles.add(missile);
        List<Alien> aliens = new ArrayList<>();
        Alien firstAlien = new Alien(100, 100);
        Alien secondAlien = new Alien(100, 100);
        aliens.add(firstAlien);
        aliens.add(secondAlien);
        GameController controller = newController(missiles, aliens);

        controller.checkCollisionsWithMissile();

        assertEquals(0, missiles.size());
        assertEquals(1, aliens.size());
        assertSame(secondAlien, aliens.getFirst());
    }

    @Test
    void heldArrowMovesSpaceshipOnEveryTickUntilReleased() {
        Spaceship spaceship = new Spaceship(500, GameConstants.PANEL_HEIGHT - GameConstants.COMPONENT_SIZE - GameConstants.SPACESHIP_START_BOTTOM_MARGIN);
        List<Missile> missiles = new ArrayList<>();
        List<Alien> aliens = new ArrayList<>();
        GameController controller = new GameController(spaceship, missiles, new ArrayList<>(), aliens, null);
        startPlaying(controller);

        controller.handleKeyPressed(KeyEvent.VK_RIGHT);
        controller.tick();
        controller.tick();
        controller.handleKeyReleased(KeyEvent.VK_RIGHT);
        controller.tick();

        assertEquals(startX() + 10, spaceship.getX());
        assertEquals(GameConstants.PANEL_HEIGHT - GameConstants.COMPONENT_SIZE - GameConstants.SPACESHIP_START_BOTTOM_MARGIN, spaceship.getY());
    }

    @Test
    void heldArrowMovementCannotMoveSpaceshipOutsidePanel() {
        Spaceship spaceship = new Spaceship(0, GameConstants.PANEL_HEIGHT - GameConstants.COMPONENT_SIZE);
        List<Missile> missiles = new ArrayList<>();
        List<Alien> aliens = new ArrayList<>();
        GameController controller = new GameController(spaceship, missiles, new ArrayList<>(), aliens, null);
        startPlaying(controller);
        spaceship.moveTo(0, GameConstants.PANEL_HEIGHT - GameConstants.COMPONENT_SIZE);
        aliens.clear();

        controller.handleKeyPressed(KeyEvent.VK_LEFT);
        controller.handleKeyPressed(KeyEvent.VK_DOWN);
        controller.tick();

        assertEquals(0, spaceship.getX());
        assertEquals(GameConstants.PANEL_HEIGHT - GameConstants.COMPONENT_SIZE, spaceship.getY());
    }

    @Test
    void tickAdvancesWaveAndSpawnsAliensWhenWaveIsCleared() {
        List<Missile> missiles = new ArrayList<>();
        List<Alien> aliens = new ArrayList<>();
        GameController controller = newController(missiles, aliens);
        startPlaying(controller);
        aliens.clear();

        controller.tick();

        assertEquals(2, controller.getWave());
        assertEquals(6, aliens.size());
    }

    @Test
    void startingPlayActivatesWaveOneMessage() {
        GameController controller = newController(new ArrayList<>(), new ArrayList<>());

        controller.handleKeyPressed(KeyEvent.VK_ENTER);

        assertTrue(controller.isWaveMessageActive());
        assertEquals(GameSession.WAVE_MESSAGE_DURATION_TICKS, controller.getWaveMessageTicks());
    }

    @Test
    void playingTicksAdvanceWaveMessageCountdown() {
        GameController controller = newController(new ArrayList<>(), new ArrayList<>());

        startPlaying(controller);
        int startingTicks = controller.getWaveMessageTicks();
        controller.tick();

        assertEquals(startingTicks - 1, controller.getWaveMessageTicks());
    }

    @Test
    void clearingWaveRefreshesWaveMessageForNextWave() {
        List<Missile> missiles = new ArrayList<>();
        List<Alien> aliens = new ArrayList<>();
        GameController controller = newController(missiles, aliens);
        startPlaying(controller);

        for (int tick = 0; tick < 10; tick++) {
            controller.tick();
        }
        assertTrue(controller.getWaveMessageTicks() < GameSession.WAVE_MESSAGE_DURATION_TICKS);

        aliens.clear();
        controller.tick();

        assertEquals(2, controller.getWave());
        assertTrue(controller.isWaveMessageActive());
        assertEquals(GameSession.WAVE_MESSAGE_DURATION_TICKS, controller.getWaveMessageTicks());
    }

    @Test
    void waveOneHasOnlyStandardAliensAndWaveTwoHasExactlyOneSpecialAlien() {
        List<Missile> missiles = new ArrayList<>();
        List<Alien> aliens = new ArrayList<>();
        GameController controller = newController(missiles, aliens, new Random(1));

        startPlaying(controller);

        assertEquals(6, aliens.size());
        assertEquals(0, aliens.stream().filter(alien -> alien.getType() == AlienType.SPECIAL).count());

        aliens.clear();
        controller.tick();

        assertEquals(2, controller.getWave());
        assertEquals(6, aliens.size());
        assertEquals(1, aliens.stream().filter(alien -> alien.getType() == AlienType.SPECIAL).count());

        aliens.clear();
        controller.tick();

        assertEquals(3, controller.getWave());
        assertEquals(0, aliens.stream().filter(alien -> alien.getType() == AlienType.SPECIAL).count());
    }

    @Test
    void generatedWaveUsesVariedNonOverlappingTopFifthSpawnPoints() {
        List<Missile> missiles = new ArrayList<>();
        List<Alien> aliens = new ArrayList<>();
        GameController controller = newController(missiles, aliens, new Random(1));
        startPlaying(controller);
        aliens.clear();

        controller.tick();

        Set<Integer> yValues = new HashSet<>();
        int topFifthLimit = GameConstants.PANEL_HEIGHT / 5;
        for (Alien alien : aliens) {
            assertTrue(alien.getY() <= topFifthLimit);
            yValues.add(alien.getY());
        }
        assertTrue(yValues.size() > 1);

        for (int first = 0; first < aliens.size(); first++) {
            Rectangle firstArea = alienArea(aliens.get(first));
            for (int second = first + 1; second < aliens.size(); second++) {
                assertFalse(firstArea.intersects(alienArea(aliens.get(second))));
            }
        }
    }

    @Test
    void generatedWavePositionsChangeBetweenWaves() {
        List<Missile> missiles = new ArrayList<>();
        List<Alien> aliens = new ArrayList<>();
        GameController controller = newController(missiles, aliens, new Random(1));
        startPlaying(controller);
        aliens.clear();

        controller.tick();
        List<String> firstWavePositions = aliens.stream()
            .map(alien -> alien.getX() + "," + alien.getY())
            .toList();
        aliens.clear();
        controller.tick();
        List<String> secondWavePositions = aliens.stream()
            .map(alien -> alien.getX() + "," + alien.getY())
            .toList();

        assertFalse(firstWavePositions.equals(secondWavePositions));
    }

    @Test
    void spaceshipAlienCollisionRemovesOneLifeAndCollidingAlien() {
        List<Missile> missiles = new ArrayList<>();
        List<Alien> aliens = new ArrayList<>();
        Alien otherAlien = new Alien(100, 100);
        aliens.add(otherAlien);
        GameController controller = newController(missiles, aliens);
        startPlaying(controller);
        Alien collidingAlien = new Alien(startX(), startY());
        aliens.clear();
        aliens.add(collidingAlien);
        aliens.add(otherAlien);

        controller.checkCollisionsWithSpaceShip();

        assertEquals(2, controller.getLives());
        assertEquals(GameState.PLAYING, controller.getGameState());
        assertEquals(1, aliens.size());
        assertSame(otherAlien, aliens.getFirst());
    }

    @Test
    void thirdSpaceshipAlienCollisionEntersGameOver() {
        List<Missile> missiles = new ArrayList<>();
        List<Alien> aliens = new ArrayList<>();
        GameController controller = newController(missiles, aliens);
        startPlaying(controller);
        aliens.clear();

        aliens.add(new Alien(startX(), startY()));
        controller.checkCollisionsWithSpaceShip();
        aliens.add(new Alien(startX(), startY()));
        controller.checkCollisionsWithSpaceShip();
        aliens.add(new Alien(startX(), startY()));
        controller.checkCollisionsWithSpaceShip();

        assertEquals(0, controller.getLives());
        assertEquals(GameState.GAME_OVER, controller.getGameState());
        assertEquals("GAME OVER", controller.getGameOverTitle());
    }

    @Test
    void alienReachingBottomEntersGameOverWithAliensWinTitle() {
        List<Missile> missiles = new ArrayList<>();
        List<Alien> aliens = new ArrayList<>();
        aliens.add(new Alien(100, GameConstants.PANEL_HEIGHT - GameConstants.COMPONENT_SIZE));
        GameController controller = newController(missiles, aliens);
        startPlaying(controller);
        aliens.clear();
        aliens.add(new Alien(100, GameConstants.PANEL_HEIGHT - GameConstants.COMPONENT_SIZE));

        controller.tick();

        assertEquals(GameState.GAME_OVER, controller.getGameState());
        assertEquals("ALIENS WIN", controller.getGameOverTitle());
    }

    @Test
    void spaceshipAlienCollisionActivatesHitFeedbackTemporarily() {
        List<Missile> missiles = new ArrayList<>();
        List<Alien> aliens = new ArrayList<>();
        GameController controller = newController(missiles, aliens);
        startPlaying(controller);
        aliens.clear();
        aliens.add(new Alien(startX(), startY()));

        controller.checkCollisionsWithSpaceShip();

        assertTrue(controller.isHitFeedbackActive());

        for (int tick = 0; tick < 18; tick++) {
            controller.tick();
        }

        assertFalse(controller.isHitFeedbackActive());
    }

    @Test
    void tickDoesNotMoveObjectsWhileGameOver() {
        Spaceship spaceship = new Spaceship(500, 680);
        List<Missile> missiles = new ArrayList<>();
        Missile missile = new Missile(100, 100);
        missiles.add(missile);
        List<Alien> aliens = new ArrayList<>();
        Alien stationaryAlien = new Alien(100, 100);
        aliens.add(stationaryAlien);
        GameController controller = new GameController(spaceship, missiles, new ArrayList<>(), aliens, null);
        startPlaying(controller);
        aliens.clear();
        aliens.add(stationaryAlien);

        aliens.add(new Alien(startX(), startY()));
        controller.checkCollisionsWithSpaceShip();
        aliens.add(new Alien(startX(), startY()));
        controller.checkCollisionsWithSpaceShip();
        aliens.add(new Alien(startX(), startY()));
        controller.checkCollisionsWithSpaceShip();
        controller.tick();

        assertEquals(100, missile.getY());
        assertEquals(100, stationaryAlien.getY());
        assertEquals(GameState.GAME_OVER, controller.getGameState());
    }

    @Test
    void enterStartsGameAndRestartsGameOverWhileSpaceFiresOnlyDuringPlay() {
        Spaceship spaceship = new Spaceship(500, 680);
        List<Missile> missiles = new ArrayList<>();
        List<Alien> aliens = new ArrayList<>();
        GameController controller = new GameController(spaceship, missiles, new ArrayList<>(), aliens, null);

        controller.handleKeyPressed(KeyEvent.VK_ENTER);

        assertEquals(GameState.PLAYING, controller.getGameState());

        controller.handleKeyPressed(KeyEvent.VK_SPACE);

        assertEquals(1, missiles.size());

        enterGameOverBySpaceshipCollisions(controller, aliens, spaceship);
        controller.handleKeyPressed(KeyEvent.VK_SPACE);

        assertEquals(GameState.GAME_OVER, controller.getGameState());

        controller.handleKeyPressed(KeyEvent.VK_ENTER);

        assertEquals(GameState.PLAYING, controller.getGameState());
        assertEquals(0, missiles.size());
        assertEquals(6, aliens.size());
    }

    @Test
    void pTogglesPauseAndResumeWhileOtherPausedKeysAreIgnored() {
        Spaceship spaceship = new Spaceship(startX(), startY());
        List<Missile> missiles = new ArrayList<>();
        List<Alien> aliens = new ArrayList<>();
        CountingSoundPlayer soundPlayer = new CountingSoundPlayer();
        GameController controller = new GameController(
            spaceship,
            missiles,
            new ArrayList<>(),
            aliens,
            null,
            new Random(1),
            soundPlayer
        );
        startPlaying(controller);
        aliens.clear();

        controller.handleKeyPressed(KeyEvent.VK_P);

        assertEquals(GameState.PAUSED, controller.getGameState());
        assertEquals(1, soundPlayer.stopBackgroundMusicCalls);

        controller.handleKeyPressed(KeyEvent.VK_ENTER);
        controller.handleKeyPressed(KeyEvent.VK_SPACE);
        controller.handleKeyPressed(KeyEvent.VK_RIGHT);
        controller.tick();

        assertEquals(GameState.PAUSED, controller.getGameState());
        assertEquals(0, missiles.size());
        assertEquals(startX(), spaceship.getX());

        controller.handleKeyPressed(KeyEvent.VK_P);

        assertEquals(GameState.PLAYING, controller.getGameState());
        assertEquals(2, soundPlayer.startBackgroundMusicCalls);
    }

    @Test
    void pausingClearsHeldInputAndFireCooldownBeforeResume() {
        Spaceship spaceship = new Spaceship(startX(), startY());
        List<Missile> missiles = new ArrayList<>();
        List<Alien> aliens = new ArrayList<>();
        GameController controller = new GameController(spaceship, missiles, new ArrayList<>(), aliens, null);
        startPlaying(controller);
        aliens.clear();
        controller.handleKeyPressed(KeyEvent.VK_RIGHT);
        controller.handleKeyPressed(KeyEvent.VK_SPACE);

        assertEquals(1, missiles.size());
        assertEquals(14, controller.getPlayerFireCooldownTicks());

        controller.handleKeyPressed(KeyEvent.VK_P);

        assertEquals(GameState.PAUSED, controller.getGameState());
        assertEquals(0, controller.getPlayerFireCooldownTicks());

        controller.handleKeyPressed(KeyEvent.VK_P);
        controller.tick();

        assertEquals(GameState.PLAYING, controller.getGameState());
        assertEquals(startX(), spaceship.getX());
        assertEquals(1, missiles.size());
    }

    @Test
    void pausedTicksDoNotAdvanceObjectsOrGameplayTimers() {
        Spaceship spaceship = new Spaceship(startX(), startY());
        List<Missile> missiles = new ArrayList<>();
        List<AlienMissile> alienMissiles = new ArrayList<>();
        List<Alien> aliens = new ArrayList<>();
        List<AlienExplosion> explosions = new ArrayList<>();
        List<RapidFirePowerUp> powerUps = new ArrayList<>();
        GameController controller = new GameController(
            spaceship,
            missiles,
            alienMissiles,
            aliens,
            explosions,
            powerUps,
            null
        );
        startPlaying(controller);
        aliens.clear();
        missiles.clear();
        alienMissiles.clear();
        explosions.clear();
        powerUps.clear();
        powerUps.add(new RapidFirePowerUp(startX(), startY()));
        controller.checkCollisionsWithRapidFirePowerUp();
        missiles.add(new Missile(100, 100));
        aliens.add(new Alien(100, 100, 0));
        controller.checkCollisionsWithMissile();
        missiles.add(new Missile(200, 100));
        aliens.add(new Alien(200, 100, 0));
        controller.checkCollisionsWithMissile();
        Missile movingMissile = new Missile(300, 300);
        AlienMissile movingAlienMissile = new AlienMissile(400, 300);
        Alien movingAlien = new Alien(500, 100, 1);
        RapidFirePowerUp fallingPowerUp = new RapidFirePowerUp(600, 100);
        missiles.add(movingMissile);
        alienMissiles.add(movingAlienMissile);
        aliens.add(movingAlien);
        powerUps.add(fallingPowerUp);
        explosions.add(new AlienExplosion(250, 250));
        int waveMessageTicks = controller.getWaveMessageTicks();
        int rapidFireTicks = controller.getRapidFireTicks();
        int comboTicks = controller.getComboTicks();
        int explosionCount = explosions.size();

        controller.handleKeyPressed(KeyEvent.VK_P);
        controller.tick();
        controller.tick();

        assertEquals(GameState.PAUSED, controller.getGameState());
        assertEquals(300, movingMissile.getY());
        assertEquals(300, movingAlienMissile.getY());
        assertEquals(100, movingAlien.getY());
        assertEquals(100, fallingPowerUp.getY());
        assertEquals(explosionCount, explosions.size());
        assertEquals(waveMessageTicks, controller.getWaveMessageTicks());
        assertEquals(rapidFireTicks, controller.getRapidFireTicks());
        assertEquals(2, controller.getComboMultiplier());
        assertEquals(comboTicks, controller.getComboTicks());
        assertEquals(0, controller.getPlayerFireCooldownTicks());
    }

    @Test
    void holdingSpaceAutoFiresWithCooldown() {
        Spaceship spaceship = new Spaceship(500, 680);
        List<Missile> missiles = new ArrayList<>();
        List<Alien> aliens = new ArrayList<>();
        GameController controller = new GameController(spaceship, missiles, new ArrayList<>(), aliens, null);
        startPlaying(controller);
        aliens.clear();

        controller.handleKeyPressed(KeyEvent.VK_SPACE);

        assertEquals(1, missiles.size());

        for (int tick = 0; tick < 13; tick++) {
            controller.tick();
        }

        assertEquals(1, missiles.size());

        controller.tick();

        assertEquals(2, missiles.size());
    }

    @Test
    void restartResetsSessionStateAndPressedMovementKeys() {
        Spaceship spaceship = new Spaceship(500, 680);
        List<Missile> missiles = new ArrayList<>();
        List<Alien> aliens = new ArrayList<>();
        GameController controller = new GameController(spaceship, missiles, new ArrayList<>(), aliens, null);
        startPlaying(controller);
        aliens.clear();
        missiles.add(new Missile(100, 100));
        aliens.add(new Alien(100, 100));
        controller.checkCollisionsWithMissile();
        controller.handleKeyPressed(KeyEvent.VK_RIGHT);

        enterGameOverBySpaceshipCollisions(controller, aliens, spaceship);
        controller.handleKeyPressed(KeyEvent.VK_ENTER);
        controller.tick();

        assertEquals(GameState.PLAYING, controller.getGameState());
        assertEquals(0, controller.getScore());
        assertEquals(1, controller.getWave());
        assertEquals(3, controller.getLives());
        assertEquals(6, aliens.size());
        assertEquals(0, missiles.size());
        assertEquals(startX(), spaceship.getX());
    }

    @Test
    void restartDoesNotAutoFireHeldSpaceAndAllowsImmediateFreshShot() {
        Spaceship spaceship = new Spaceship(500, 680);
        List<Missile> missiles = new ArrayList<>();
        List<Alien> aliens = new ArrayList<>();
        GameController controller = new GameController(spaceship, missiles, new ArrayList<>(), aliens, null);
        startPlaying(controller);

        controller.handleKeyPressed(KeyEvent.VK_SPACE);

        assertEquals(1, missiles.size());

        enterGameOverBySpaceshipCollisions(controller, aliens, spaceship);
        controller.handleKeyPressed(KeyEvent.VK_ENTER);
        controller.tick();

        assertEquals(GameState.PLAYING, controller.getGameState());
        assertEquals(0, missiles.size());

        controller.handleKeyPressed(KeyEvent.VK_SPACE);

        assertEquals(1, missiles.size());
    }

    @Test
    void restartMovesSpaceshipToStartPosition() {
        Spaceship spaceship = new Spaceship(500, 680);
        List<Missile> missiles = new ArrayList<>();
        List<Alien> aliens = new ArrayList<>();
        GameController controller = new GameController(spaceship, missiles, new ArrayList<>(), aliens, null);
        startPlaying(controller);

        controller.handleKeyPressed(KeyEvent.VK_RIGHT);
        controller.handleKeyPressed(KeyEvent.VK_UP);
        controller.tick();
        controller.handleKeyReleased(KeyEvent.VK_RIGHT);
        controller.handleKeyReleased(KeyEvent.VK_UP);

        enterGameOverBySpaceshipCollisions(controller, aliens, spaceship);
        controller.handleKeyPressed(KeyEvent.VK_ENTER);

        assertEquals(GameState.PLAYING, controller.getGameState());
        assertEquals((GameConstants.PANEL_WIDTH - GameConstants.COMPONENT_SIZE) / 2, spaceship.getX());
        assertEquals(
            GameConstants.PANEL_HEIGHT - GameConstants.COMPONENT_SIZE - GameConstants.SPACESHIP_START_BOTTOM_MARGIN,
            spaceship.getY()
        );
    }

    @Test
    void restartClearsProjectilesAndSpawnsFreshWave() {
        Spaceship spaceship = new Spaceship(500, 680);
        List<Missile> missiles = new ArrayList<>();
        List<AlienMissile> alienMissiles = new ArrayList<>();
        List<Alien> aliens = new ArrayList<>();
        List<AlienExplosion> explosions = new ArrayList<>();
        GameController controller = new GameController(
            spaceship,
            missiles,
            alienMissiles,
            aliens,
            explosions,
            new ArrayList<>(),
            null
        );
        startPlaying(controller);
        missiles.add(new Missile(100, 100));
        alienMissiles.add(new AlienMissile(100, 100));
        explosions.add(new AlienExplosion(120, 120));

        enterGameOverBySpaceshipCollisions(controller, aliens, spaceship);
        controller.handleKeyPressed(KeyEvent.VK_ENTER);

        assertEquals(GameState.PLAYING, controller.getGameState());
        assertEquals(0, missiles.size());
        assertEquals(0, alienMissiles.size());
        assertEquals(0, explosions.size());
        assertEquals(6, aliens.size());
    }

    @Test
    void restartClearsHitFeedbackAndRestoresDefaultGameOverTitle() {
        Spaceship spaceship = new Spaceship(500, 680);
        List<Missile> missiles = new ArrayList<>();
        List<Alien> aliens = new ArrayList<>();
        GameController controller = new GameController(spaceship, missiles, new ArrayList<>(), aliens, null);
        startPlaying(controller);
        aliens.clear();
        aliens.add(new Alien(startX(), startY()));
        controller.checkCollisionsWithSpaceShip();
        aliens.add(new Alien(100, GameConstants.PANEL_HEIGHT - GameConstants.COMPONENT_SIZE));
        controller.tick();

        controller.handleKeyPressed(KeyEvent.VK_ENTER);

        assertEquals(GameState.PLAYING, controller.getGameState());
        assertFalse(controller.isHitFeedbackActive());
        assertEquals("GAME OVER", controller.getGameOverTitle());
    }

    @Test
    void missileAlienCollisionAfterAdvancingToWaveTwoAddsWaveTwoScore() {
        List<Missile> missiles = new ArrayList<>();
        List<Alien> aliens = new ArrayList<>();
        GameController controller = newController(missiles, aliens);
        startPlaying(controller);
        aliens.clear();
        controller.tick();
        aliens.clear();
        missiles.add(new Missile(100, 100));
        aliens.add(new Alien(100, 100));

        controller.checkCollisionsWithMissile();

        assertEquals(2, controller.getWave());
        assertEquals(20, controller.getScore());
    }

    @Test
    void alienMissileCollisionRemovesOneLifeAndProjectile() {
        Spaceship spaceship = new Spaceship(500, 680);
        List<Missile> missiles = new ArrayList<>();
        List<AlienMissile> alienMissiles = new ArrayList<>();
        List<Alien> aliens = new ArrayList<>();
        GameController controller = new GameController(spaceship, missiles, alienMissiles, aliens, null);
        startPlaying(controller);
        AlienMissile alienMissile = new AlienMissile(startX(), startY());
        alienMissiles.add(alienMissile);

        controller.checkCollisionsWithAlienMissile();

        assertEquals(2, controller.getLives());
        assertEquals(0, alienMissiles.size());
        assertEquals(GameState.PLAYING, controller.getGameState());
        assertTrue(controller.isHitFeedbackActive());
    }

    @Test
    void thirdAlienMissileCollisionEntersGameOver() {
        Spaceship spaceship = new Spaceship(500, 680);
        List<Missile> missiles = new ArrayList<>();
        List<AlienMissile> alienMissiles = new ArrayList<>();
        List<Alien> aliens = new ArrayList<>();
        GameController controller = new GameController(spaceship, missiles, alienMissiles, aliens, null);
        startPlaying(controller);

        alienMissiles.add(new AlienMissile(startX(), startY()));
        controller.checkCollisionsWithAlienMissile();
        alienMissiles.add(new AlienMissile(startX(), startY()));
        controller.checkCollisionsWithAlienMissile();
        alienMissiles.add(new AlienMissile(startX(), startY()));
        controller.checkCollisionsWithAlienMissile();

        assertEquals(0, controller.getLives());
        assertEquals(GameState.GAME_OVER, controller.getGameState());
    }

    @Test
    void cleanupRemovesAlienMissilesBelowPanel() {
        List<Missile> missiles = new ArrayList<>();
        List<AlienMissile> alienMissiles = new ArrayList<>();
        AlienMissile offscreen = new AlienMissile(100, GameConstants.PANEL_HEIGHT + 1);
        AlienMissile visible = new AlienMissile(100, GameConstants.PANEL_HEIGHT);
        alienMissiles.add(offscreen);
        alienMissiles.add(visible);
        List<Alien> aliens = new ArrayList<>();
        GameController controller = new GameController(
            new Spaceship(500, 680),
            missiles,
            alienMissiles,
            aliens,
            null
        );

        controller.cleanupOffscreenObjects();

        assertEquals(1, alienMissiles.size());
        assertSame(visible, alienMissiles.getFirst());
    }

    @Test
    void alienFireRespectsActiveProjectileCap() {
        List<Missile> missiles = new ArrayList<>();
        List<AlienMissile> alienMissiles = new ArrayList<>();
        List<Alien> aliens = new ArrayList<>();
        aliens.add(new Alien(100, 100));
        GameController controller = new GameController(
            new Spaceship(500, 680),
            missiles,
            alienMissiles,
            aliens,
            null,
            new AlwaysFireRandom(),
            new ArcadeSoundPlayer()
        );
        startPlaying(controller);
        aliens.clear();
        aliens.add(new Alien(100, 100));

        for (int count = 0; count < GameController.MAX_ALIEN_MISSILES + 3; count++) {
            controller.fireAlienMissileIfReady();
        }

        assertEquals(GameController.MAX_ALIEN_MISSILES, alienMissiles.size());
    }

    @Test
    void specialAlienHasHigherWeightedChanceToBeSelectedAsShooter() {
        List<AlienMissile> alienMissiles = new ArrayList<>();
        List<Alien> aliens = new ArrayList<>();
        Alien standardAlien = new Alien(100, 100, 0);
        Alien specialAlien = Alien.special(200, 100, 0);
        aliens.add(standardAlien);
        aliens.add(specialAlien);
        GameController controller = new GameController(
            new Spaceship(500, 680),
            new ArrayList<>(),
            alienMissiles,
            aliens,
            null,
            new ScriptedRandom(new double[] { 0.0 }, new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1 }),
            new ArcadeSoundPlayer()
        );
        startPlaying(controller);
        aliens.clear();
        aliens.add(standardAlien);
        aliens.add(specialAlien);

        controller.fireAlienMissileIfReady();

        assertEquals(1, alienMissiles.size());
        assertEquals(specialAlien.getX(), alienMissiles.getFirst().getX());
        assertEquals(specialAlien.getY() + GameConstants.COMPONENT_SIZE, alienMissiles.getFirst().getY());
    }

    @Test
    void specialAlienMovementStaysInsidePanelDuringTick() {
        List<Alien> aliens = new ArrayList<>();
        Alien specialAlien = Alien.special(GameConstants.PANEL_WIDTH - GameConstants.COMPONENT_SIZE, 100, 0);
        aliens.add(specialAlien);
        GameController controller = new GameController(
            new Spaceship(500, 680),
            new ArrayList<>(),
            new ArrayList<>(),
            aliens,
            null,
            new ScriptedRandom(new double[] { 0.0, 1.0 }, new int[] { 0 }),
            new ArcadeSoundPlayer()
        );
        startPlaying(controller);
        aliens.clear();
        aliens.add(specialAlien);

        controller.tick();

        assertTrue(specialAlien.getX() >= 0);
        assertTrue(specialAlien.getX() <= GameConstants.PANEL_WIDTH - GameConstants.COMPONENT_SIZE);
    }

    @Test
    void specialAlienReversesInsteadOfOverlappingAnotherAlien() {
        List<Alien> aliens = new ArrayList<>();
        Alien specialAlien = Alien.special(100, 100, 0);
        Alien blockingAlien = new Alien(100 + GameConstants.COMPONENT_SIZE, 100, 0);
        aliens.add(specialAlien);
        aliens.add(blockingAlien);
        GameController controller = new GameController(
            new Spaceship(500, 680),
            new ArrayList<>(),
            new ArrayList<>(),
            aliens,
            null,
            new ScriptedRandom(new double[] { 1.0, 1.0 }, new int[] { 0 }),
            new ArcadeSoundPlayer()
        );
        startPlaying(controller);
        aliens.clear();
        aliens.add(specialAlien);
        aliens.add(blockingAlien);

        controller.tick();

        assertEquals(100, specialAlien.getX());
        controller.tick();
        assertTrue(specialAlien.getX() < 100);
    }

    @Test
    void specialAlienSeparatesWhenCatchingUpFromAbove() {
        List<Alien> aliens = new ArrayList<>();
        Alien specialAlien = Alien.special(100, 58, 5);
        Alien blockingAlien = new Alien(100, 100, 0);
        aliens.add(specialAlien);
        aliens.add(blockingAlien);
        GameController controller = new GameController(
            new Spaceship(500, 680),
            new ArrayList<>(),
            new ArrayList<>(),
            aliens,
            null,
            new ScriptedRandom(new double[] { 1.0, 1.0 }, new int[] { 0 }),
            new ArcadeSoundPlayer()
        );
        startPlaying(controller);
        aliens.clear();
        aliens.add(specialAlien);
        aliens.add(blockingAlien);

        controller.tick();

        assertTrue(specialAlien.getY() + GameConstants.COMPONENT_SIZE <= blockingAlien.getY());
        assertTrue(Math.abs(specialAlien.getX() - blockingAlien.getX()) >= GameConstants.COMPONENT_SIZE);
    }

    @Test
    void playingTickCreatesAtMostOneAlienMissile() {
        List<AlienMissile> alienMissiles = new ArrayList<>();
        List<Alien> aliens = new ArrayList<>();
        aliens.add(new Alien(100, 100));
        aliens.add(Alien.special(200, 100, 0));
        GameController controller = new GameController(
            new Spaceship(500, 680),
            new ArrayList<>(),
            alienMissiles,
            aliens,
            null,
            new AlwaysFireRandom(),
            new ArcadeSoundPlayer()
        );
        startPlaying(controller);
        aliens.clear();
        aliens.add(new Alien(100, 100));
        aliens.add(Alien.special(200, 100, 0));

        controller.tick();

        assertEquals(1, alienMissiles.size());
    }

    @Test
    void restartClearsAlienMissiles() {
        Spaceship spaceship = new Spaceship(500, 680);
        List<Missile> missiles = new ArrayList<>();
        List<AlienMissile> alienMissiles = new ArrayList<>();
        List<Alien> aliens = new ArrayList<>();
        GameController controller = new GameController(spaceship, missiles, alienMissiles, aliens, null);
        startPlaying(controller);
        alienMissiles.add(new AlienMissile(500, 680));

        aliens.clear();
        enterGameOverBySpaceshipCollisions(controller, aliens, spaceship);
        controller.handleKeyPressed(KeyEvent.VK_ENTER);

        assertEquals(GameState.PLAYING, controller.getGameState());
        assertEquals(0, alienMissiles.size());
        assertEquals(6, aliens.size());
    }

    @Test
    void restartReturnsToWaveOneCompositionWithoutSpecialAlien() {
        Spaceship spaceship = new Spaceship(500, 680);
        List<Alien> aliens = new ArrayList<>();
        GameController controller = new GameController(
            spaceship,
            new ArrayList<>(),
            new ArrayList<>(),
            aliens,
            null,
            new Random(1),
            new ArcadeSoundPlayer()
        );
        startPlaying(controller);

        aliens.clear();
        controller.tick();
        assertEquals(2, controller.getWave());
        assertEquals(1, aliens.stream().filter(alien -> alien.getType() == AlienType.SPECIAL).count());

        enterGameOverBySpaceshipCollisions(controller, aliens, spaceship);
        controller.handleKeyPressed(KeyEvent.VK_ENTER);

        assertEquals(1, controller.getWave());
        assertEquals(6, aliens.size());
        assertEquals(0, aliens.stream().filter(alien -> alien.getType() == AlienType.SPECIAL).count());
    }

    @Test
    void resolvedAlienKillCreatesRapidFireDropOnSuccessfulRoll() {
        List<Missile> missiles = new ArrayList<>();
        List<Alien> aliens = new ArrayList<>();
        List<RapidFirePowerUp> powerUps = new ArrayList<>();
        missiles.add(new Missile(100, 120));
        aliens.add(new Alien(100, 120));
        GameController controller = new GameController(
            new Spaceship(500, 600),
            missiles,
            new ArrayList<>(),
            aliens,
            powerUps,
            null,
            new AlwaysFireRandom(),
            new ArcadeSoundPlayer()
        );

        controller.checkCollisionsWithMissile();

        assertEquals(1, powerUps.size());
        assertEquals(100, powerUps.getFirst().getX());
        assertEquals(120, powerUps.getFirst().getY());
    }

    @Test
    void failedDropRollCreatesNoPowerUpAndResolvedKillRollsOnce() {
        List<Missile> missiles = new ArrayList<>();
        List<Alien> aliens = new ArrayList<>();
        List<RapidFirePowerUp> powerUps = new ArrayList<>();
        missiles.add(new Missile(100, 120));
        missiles.add(new Missile(100, 120));
        aliens.add(new Alien(100, 120));
        CountingDropRandom random = new CountingDropRandom(0.5);
        GameController controller = new GameController(
            new Spaceship(500, 600),
            missiles,
            new ArrayList<>(),
            aliens,
            powerUps,
            null,
            random,
            new ArcadeSoundPlayer()
        );

        controller.checkCollisionsWithMissile();

        assertEquals(0, powerUps.size());
        assertEquals(1, random.doubleCalls);
    }

    @Test
    void rapidFirePowerUpsMoveCleanUpAndActivateOnCollection() {
        Spaceship spaceship = new Spaceship(startX(), startY());
        List<Alien> aliens = new ArrayList<>();
        List<RapidFirePowerUp> powerUps = new ArrayList<>();
        RapidFirePowerUp falling = new RapidFirePowerUp(100, 100);
        RapidFirePowerUp offscreen = new RapidFirePowerUp(100, GameConstants.PANEL_HEIGHT + 1);
        GameController controller = new GameController(
            spaceship,
            new ArrayList<>(),
            new ArrayList<>(),
            aliens,
            powerUps,
            null
        );
        startPlaying(controller);
        aliens.clear();
        powerUps.add(falling);
        powerUps.add(offscreen);

        controller.tick();

        assertEquals(103, falling.getY());
        assertEquals(1, powerUps.size());

        powerUps.add(new RapidFirePowerUp(spaceship.getX(), spaceship.getY()));
        controller.checkCollisionsWithRapidFirePowerUp();

        assertTrue(controller.isRapidFireActive());
        assertEquals(GameSession.RAPID_FIRE_DURATION_TICKS, controller.getRapidFireTicks());
    }

    @Test
    void collectingAgainRefreshesRapidFireDuration() {
        Spaceship spaceship = new Spaceship(startX(), startY());
        List<RapidFirePowerUp> powerUps = new ArrayList<>();
        GameController controller = new GameController(
            spaceship,
            new ArrayList<>(),
            new ArrayList<>(),
            new ArrayList<>(),
            powerUps,
            null
        );
        startPlaying(controller);
        powerUps.add(new RapidFirePowerUp(startX(), startY()));
        controller.checkCollisionsWithRapidFirePowerUp();
        controller.tick();
        powerUps.add(new RapidFirePowerUp(startX(), startY()));

        controller.checkCollisionsWithRapidFirePowerUp();

        assertEquals(GameSession.RAPID_FIRE_DURATION_TICKS, controller.getRapidFireTicks());
    }

    @Test
    void heldSpaceUsesRapidFireCooldownThenReturnsToNormalAfterExpiration() {
        Spaceship spaceship = new Spaceship(startX(), startY());
        List<Missile> missiles = new ArrayList<>();
        List<RapidFirePowerUp> powerUps = new ArrayList<>();
        List<Alien> aliens = new ArrayList<>();
        GameController controller = new GameController(
            spaceship,
            missiles,
            new ArrayList<>(),
            aliens,
            powerUps,
            null
        );
        startPlaying(controller);
        aliens.clear();
        powerUps.add(new RapidFirePowerUp(startX(), startY()));
        controller.checkCollisionsWithRapidFirePowerUp();

        controller.handleKeyPressed(KeyEvent.VK_SPACE);
        assertEquals(7, controller.getPlayerFireCooldownTicks());

        for (int tick = 0; tick < GameSession.RAPID_FIRE_DURATION_TICKS; tick++) {
            controller.tick();
        }
        controller.handleKeyReleased(KeyEvent.VK_SPACE);
        for (int tick = 0; tick < 10; tick++) {
            controller.tick();
        }

        controller.handleKeyPressed(KeyEvent.VK_SPACE);

        assertEquals(14, controller.getPlayerFireCooldownTicks());
    }

    @Test
    void rapidFireSurvivesWaveAdvanceButLifeLossAndRestartClearItAndDrops() {
        Spaceship spaceship = new Spaceship(startX(), startY());
        List<Alien> aliens = new ArrayList<>();
        List<RapidFirePowerUp> powerUps = new ArrayList<>();
        GameController controller = new GameController(
            spaceship,
            new ArrayList<>(),
            new ArrayList<>(),
            aliens,
            powerUps,
            null
        );
        startPlaying(controller);
        powerUps.add(new RapidFirePowerUp(startX(), startY()));
        controller.checkCollisionsWithRapidFirePowerUp();
        aliens.clear();
        aliens.clear();
        controller.tick();

        assertTrue(controller.isRapidFireActive());
        assertEquals(2, controller.getWave());

        aliens.add(new Alien(startX(), startY()));
        controller.checkCollisionsWithSpaceShip();

        assertFalse(controller.isRapidFireActive());

        powerUps.add(new RapidFirePowerUp(startX(), startY()));
        controller.checkCollisionsWithRapidFirePowerUp();
        powerUps.add(new RapidFirePowerUp(100, 100));
        enterGameOverBySpaceshipCollisions(controller, aliens, spaceship);
        controller.handleKeyPressed(KeyEvent.VK_ENTER);

        assertFalse(controller.isRapidFireActive());
        assertEquals(0, powerUps.size());
    }

    @Test
    void collectingRapidFireDoesNotShortenCurrentNormalCooldown() {
        Spaceship spaceship = new Spaceship(startX(), startY());
        List<RapidFirePowerUp> powerUps = new ArrayList<>();
        GameController controller = new GameController(
            spaceship,
            new ArrayList<>(),
            new ArrayList<>(),
            new ArrayList<>(),
            powerUps,
            null
        );
        startPlaying(controller);
        controller.handleKeyPressed(KeyEvent.VK_SPACE);
        powerUps.add(new RapidFirePowerUp(startX(), startY()));

        controller.checkCollisionsWithRapidFirePowerUp();

        assertTrue(controller.isRapidFireActive());
        assertEquals(14, controller.getPlayerFireCooldownTicks());
    }

    @Test
    void rapidFireTimerDoesNotAdvanceOutsidePlaying() {
        Spaceship spaceship = new Spaceship(startX(), startY());
        List<Alien> aliens = new ArrayList<>();
        List<RapidFirePowerUp> powerUps = new ArrayList<>();
        GameController controller = new GameController(
            spaceship,
            new ArrayList<>(),
            new ArrayList<>(),
            aliens,
            powerUps,
            null
        );
        startPlaying(controller);
        powerUps.add(new RapidFirePowerUp(startX(), startY()));
        controller.checkCollisionsWithRapidFirePowerUp();
        enterGameOverBySpaceshipCollisions(controller, aliens, spaceship);
        int ticksAtGameOver = controller.getRapidFireTicks();

        controller.tick();

        assertEquals(GameState.GAME_OVER, controller.getGameState());
        assertEquals(ticksAtGameOver, controller.getRapidFireTicks());
    }

    @Test
    void consecutiveKillTicksIncreaseComboAndRefreshTimer() {
        List<Missile> missiles = new ArrayList<>();
        List<Alien> aliens = new ArrayList<>();
        GameController controller = newController(missiles, aliens);
        startPlaying(controller);
        aliens.clear();
        aliens.add(new Alien(100, 100, 0));
        aliens.add(new Alien(500, 100, 0));
        missiles.add(new Missile(100, 100));

        controller.checkCollisionsWithMissile();
        controller.tick();

        assertEquals(1, controller.getComboMultiplier());
        assertEquals(GameSession.COMBO_DURATION_TICKS - 1, controller.getComboTicks());

        missiles.add(new Missile(500, 100));
        aliens.add(new Alien(800, 100, 0));
        controller.checkCollisionsWithMissile();

        assertEquals(2, controller.getComboMultiplier());
        assertEquals(GameSession.COMBO_DURATION_TICKS, controller.getComboTicks());
        assertEquals(30, controller.getScore());
    }

    @Test
    void sameTickMultiKillAdvancesComboOnceAndUsesSharedMultiplier() {
        List<Missile> missiles = new ArrayList<>();
        List<Alien> aliens = new ArrayList<>();
        GameController controller = newController(missiles, aliens);
        missiles.add(new Missile(100, 100));
        aliens.add(new Alien(100, 100));
        aliens.add(new Alien(900, 100));
        controller.checkCollisionsWithMissile();

        missiles.add(new Missile(200, 100));
        missiles.add(new Missile(300, 100));
        aliens.add(new Alien(200, 100));
        aliens.add(new Alien(300, 100));

        controller.checkCollisionsWithMissile();

        assertEquals(2, controller.getComboMultiplier());
        assertEquals(50, controller.getScore());
    }

    @Test
    void comboExpiresDuringPlayingTicksAndNextKillRestartsAtOne() {
        List<Missile> missiles = new ArrayList<>();
        List<Alien> aliens = new ArrayList<>();
        GameController controller = newController(missiles, aliens);
        startPlaying(controller);
        aliens.clear();
        aliens.add(new Alien(100, 100, 0));
        aliens.add(new Alien(500, 100, 0));
        missiles.add(new Missile(100, 100));
        controller.checkCollisionsWithMissile();

        for (int tick = 0; tick < GameSession.COMBO_DURATION_TICKS; tick++) {
            controller.tick();
        }

        assertEquals(1, controller.getComboMultiplier());
        assertEquals(0, controller.getComboTicks());

        missiles.add(new Missile(500, 100));
        controller.checkCollisionsWithMissile();

        assertEquals(1, controller.getComboMultiplier());
        assertEquals(20, controller.getScore());
    }

    @Test
    void missedShotDoesNotDirectlyResetCombo() {
        List<Missile> missiles = new ArrayList<>();
        List<Alien> aliens = new ArrayList<>();
        GameController controller = newController(missiles, aliens);
        missiles.add(new Missile(100, 100));
        aliens.add(new Alien(100, 100));
        aliens.add(new Alien(900, 100));
        controller.checkCollisionsWithMissile();
        missiles.add(new Missile(200, 100));
        aliens.add(new Alien(200, 100));
        controller.checkCollisionsWithMissile();
        int comboTicks = controller.getComboTicks();
        missiles.add(new Missile(500, -GameConstants.COMPONENT_SIZE - 1));

        controller.cleanupOffscreenObjects();
        controller.checkCollisionsWithMissile();

        assertEquals(2, controller.getComboMultiplier());
        assertEquals(comboTicks, controller.getComboTicks());
    }

    @Test
    void lifeLossAndWaveClearResetComboAfterScoring() {
        List<Missile> missiles = new ArrayList<>();
        List<Alien> aliens = new ArrayList<>();
        GameController controller = newController(missiles, aliens);
        startPlaying(controller);
        aliens.clear();
        missiles.add(new Missile(100, 100));
        aliens.add(new Alien(100, 100));
        aliens.add(new Alien(200, 100));
        controller.checkCollisionsWithMissile();
        missiles.add(new Missile(200, 100));
        controller.checkCollisionsWithMissile();

        assertEquals(30, controller.getScore());
        assertEquals(2, controller.getComboMultiplier());

        controller.tick();

        assertEquals(2, controller.getWave());
        assertEquals(30, controller.getScore());
        assertEquals(1, controller.getComboMultiplier());
        assertEquals(0, controller.getComboTicks());

        aliens.clear();
        missiles.add(new Missile(300, 100));
        aliens.add(new Alien(300, 100));
        aliens.add(new Alien(startX(), startY()));
        controller.checkCollisionsWithMissile();
        missiles.add(new Missile(400, 100));
        aliens.add(new Alien(400, 100));
        controller.checkCollisionsWithMissile();

        assertEquals(2, controller.getComboMultiplier());

        controller.checkCollisionsWithSpaceShip();

        assertEquals(1, controller.getComboMultiplier());
        assertEquals(0, controller.getComboTicks());
    }

    @Test
    void rapidFireDoesNotChangeComboProgressionRules() {
        Spaceship spaceship = new Spaceship(startX(), startY());
        List<Missile> missiles = new ArrayList<>();
        List<Alien> aliens = new ArrayList<>();
        List<RapidFirePowerUp> powerUps = new ArrayList<>();
        GameController controller = new GameController(
            spaceship,
            missiles,
            new ArrayList<>(),
            aliens,
            powerUps,
            null
        );
        startPlaying(controller);
        powerUps.add(new RapidFirePowerUp(startX(), startY()));
        controller.checkCollisionsWithRapidFirePowerUp();
        missiles.add(new Missile(100, 100));
        aliens.add(new Alien(100, 100));
        controller.checkCollisionsWithMissile();

        assertTrue(controller.isRapidFireActive());
        assertEquals(1, controller.getComboMultiplier());

        missiles.add(new Missile(200, 100));
        missiles.add(new Missile(300, 100));
        aliens.add(new Alien(200, 100));
        aliens.add(new Alien(300, 100));
        controller.checkCollisionsWithMissile();

        assertEquals(2, controller.getComboMultiplier());
    }

    @Test
    void comboTimerPausesOutsidePlayingAndRestartClearsCombo() {
        Spaceship spaceship = new Spaceship(startX(), startY());
        List<Missile> missiles = new ArrayList<>();
        List<Alien> aliens = new ArrayList<>();
        GameController controller = new GameController(spaceship, missiles, new ArrayList<>(), aliens, null);
        startPlaying(controller);
        aliens.clear();
        missiles.add(new Missile(100, 100));
        aliens.add(new Alien(100, 100));
        aliens.add(new Alien(200, 100));
        controller.checkCollisionsWithMissile();
        missiles.add(new Missile(200, 100));
        controller.checkCollisionsWithMissile();
        aliens.add(new Alien(100, GameConstants.PANEL_HEIGHT - GameConstants.COMPONENT_SIZE));
        controller.tick();
        int comboTicksAtGameOver = controller.getComboTicks();

        controller.tick();

        assertEquals(GameState.GAME_OVER, controller.getGameState());
        assertEquals(2, controller.getComboMultiplier());
        assertEquals(comboTicksAtGameOver, controller.getComboTicks());

        controller.handleKeyPressed(KeyEvent.VK_ENTER);

        assertEquals(GameState.PLAYING, controller.getGameState());
        assertEquals(1, controller.getComboMultiplier());
        assertEquals(0, controller.getComboTicks());
    }

    private GameController newController(List<Missile> missiles, List<Alien> aliens) {
        return newController(missiles, aliens, new ArrayList<>());
    }

    private GameController newController(List<Missile> missiles, List<Alien> aliens, List<AlienExplosion> explosions) {
        return new GameController(
            new Spaceship(500, 680),
            missiles,
            new ArrayList<>(),
            aliens,
            explosions,
            new ArrayList<>(),
            null
        );
    }

    private GameController newController(List<Missile> missiles, List<Alien> aliens, Random random) {
        return new GameController(
            new Spaceship(500, 680),
            missiles,
            new ArrayList<>(),
            aliens,
            null,
            random,
            new ArcadeSoundPlayer()
        );
    }

    private void startPlaying(GameController controller) {
        controller.handleKeyPressed(KeyEvent.VK_ENTER);
    }

    private int startX() {
        return (GameConstants.PANEL_WIDTH - GameConstants.COMPONENT_SIZE) / 2;
    }

    private int startY() {
        return GameConstants.PANEL_HEIGHT - GameConstants.COMPONENT_SIZE - GameConstants.SPACESHIP_START_BOTTOM_MARGIN;
    }

    private void enterGameOverBySpaceshipCollisions(
        GameController controller,
        List<Alien> aliens,
        Spaceship spaceship
    ) {
        aliens.clear();
        for (int hit = 0; hit < 3; hit++) {
            aliens.add(new Alien(spaceship.getX(), spaceship.getY()));
            controller.checkCollisionsWithSpaceShip();
        }
    }

    private static Rectangle alienArea(Alien alien) {
        return new Rectangle(
            alien.getX(),
            alien.getY(),
            GameConstants.COMPONENT_SIZE,
            GameConstants.COMPONENT_SIZE
        );
    }

    private static class AlwaysFireRandom extends Random {
        @Override
        public double nextDouble() {
            return 0.0;
        }

        @Override
        public int nextInt(int bound) {
            return 0;
        }
    }

    private static class CountingSoundPlayer extends ArcadeSoundPlayer {
        private int startBackgroundMusicCalls;
        private int stopBackgroundMusicCalls;

        @Override
        public synchronized void startBackgroundMusic() {
            startBackgroundMusicCalls++;
        }

        @Override
        public synchronized void stopBackgroundMusic() {
            stopBackgroundMusicCalls++;
        }
    }

    private static class CountingDropRandom extends Random {
        private final double value;
        private int doubleCalls;

        private CountingDropRandom(double value) {
            this.value = value;
        }

        @Override
        public double nextDouble() {
            doubleCalls++;
            return value;
        }
    }

    private static class ScriptedRandom extends Random {
        private final double[] doubles;
        private final int[] ints;
        private int doubleIndex;
        private int intIndex;

        private ScriptedRandom(double[] doubles, int[] ints) {
            this.doubles = doubles;
            this.ints = ints;
        }

        @Override
        public double nextDouble() {
            if (doubleIndex >= doubles.length) {
                return doubles[doubles.length - 1];
            }
            return doubles[doubleIndex++];
        }

        @Override
        public int nextInt(int bound) {
            if (intIndex >= ints.length) {
                return 0;
            }
            return ints[intIndex++];
        }
    }
}
