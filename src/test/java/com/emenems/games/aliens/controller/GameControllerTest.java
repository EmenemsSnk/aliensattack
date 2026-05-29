package com.emenems.games.aliens.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.emenems.games.aliens.GameState;
import com.emenems.games.aliens.gamemachines.Alien;
import com.emenems.games.aliens.gamemachines.Missile;
import com.emenems.games.aliens.gamemachines.Spaceship;
import com.emenems.games.aliens.gui.GamePanel;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class GameControllerTest {

    @Test
    void cleanupRemovesMissilesAbovePanel() {
        List<Missile> missiles = new ArrayList<>();
        Missile offscreen = new Missile(100, -GamePanel.DEFAULT_COMPONENT_SIZE - 1);
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
        Alien belowPanel = new Alien(100, GamePanel.PANEL_HEIGHT + 1);
        Alien leftOfPanel = new Alien(-GamePanel.DEFAULT_COMPONENT_SIZE - 1, 100);
        Alien rightOfPanel = new Alien(GamePanel.PANEL_WIDTH + 1, 100);
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
        GameController controller = newController(missiles, aliens);

        controller.checkCollisionsWithMissile();

        assertEquals(0, missiles.size());
        assertEquals(0, aliens.size());
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
        Spaceship spaceship = new Spaceship(500, 680);
        List<Missile> missiles = new ArrayList<>();
        List<Alien> aliens = new ArrayList<>();
        GameController controller = new GameController(spaceship, missiles, aliens, null);

        controller.handleKeyPressed(KeyEvent.VK_RIGHT);
        controller.tick();
        controller.tick();
        controller.handleKeyReleased(KeyEvent.VK_RIGHT);
        controller.tick();

        assertEquals(510, spaceship.getX());
        assertEquals(680, spaceship.getY());
    }

    @Test
    void tickAdvancesWaveAndSpawnsAliensWhenWaveIsCleared() {
        List<Missile> missiles = new ArrayList<>();
        List<Alien> aliens = new ArrayList<>();
        GameController controller = newController(missiles, aliens);

        controller.tick();

        assertEquals(2, controller.getWave());
        assertEquals(10, aliens.size());
    }

    @Test
    void spaceshipAlienCollisionRemovesOneLifeAndCollidingAlien() {
        List<Missile> missiles = new ArrayList<>();
        List<Alien> aliens = new ArrayList<>();
        Alien collidingAlien = new Alien(500, 680);
        Alien otherAlien = new Alien(100, 100);
        aliens.add(collidingAlien);
        aliens.add(otherAlien);
        GameController controller = newController(missiles, aliens);

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

        aliens.add(new Alien(500, 680));
        controller.checkCollisionsWithSpaceShip();
        aliens.add(new Alien(500, 680));
        controller.checkCollisionsWithSpaceShip();
        aliens.add(new Alien(500, 680));
        controller.checkCollisionsWithSpaceShip();

        assertEquals(0, controller.getLives());
        assertEquals(GameState.GAME_OVER, controller.getGameState());
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
        GameController controller = new GameController(spaceship, missiles, aliens, null);

        aliens.add(new Alien(500, 680));
        controller.checkCollisionsWithSpaceShip();
        aliens.add(new Alien(500, 680));
        controller.checkCollisionsWithSpaceShip();
        aliens.add(new Alien(500, 680));
        controller.checkCollisionsWithSpaceShip();
        controller.tick();

        assertEquals(100, missile.getY());
        assertEquals(100, stationaryAlien.getY());
        assertEquals(GameState.GAME_OVER, controller.getGameState());
    }

    @Test
    void spaceFiresMissilesWhilePlayingButRestartsGameOver() {
        Spaceship spaceship = new Spaceship(500, 680);
        List<Missile> missiles = new ArrayList<>();
        List<Alien> aliens = new ArrayList<>();
        GameController controller = new GameController(spaceship, missiles, aliens, null);

        controller.handleKeyPressed(KeyEvent.VK_SPACE);

        assertEquals(1, missiles.size());

        aliens.add(new Alien(500, 680));
        controller.checkCollisionsWithSpaceShip();
        aliens.add(new Alien(500, 680));
        controller.checkCollisionsWithSpaceShip();
        aliens.add(new Alien(500, 680));
        controller.checkCollisionsWithSpaceShip();
        controller.handleKeyPressed(KeyEvent.VK_SPACE);

        assertEquals(GameState.PLAYING, controller.getGameState());
        assertEquals(0, missiles.size());
        assertEquals(10, aliens.size());
    }

    @Test
    void restartResetsSessionStateAndPressedMovementKeys() {
        Spaceship spaceship = new Spaceship(500, 680);
        List<Missile> missiles = new ArrayList<>();
        List<Alien> aliens = new ArrayList<>();
        GameController controller = new GameController(spaceship, missiles, aliens, null);
        missiles.add(new Missile(100, 100));
        aliens.add(new Alien(100, 100));
        controller.checkCollisionsWithMissile();
        controller.handleKeyPressed(KeyEvent.VK_RIGHT);

        aliens.add(new Alien(500, 680));
        controller.checkCollisionsWithSpaceShip();
        aliens.add(new Alien(500, 680));
        controller.checkCollisionsWithSpaceShip();
        aliens.add(new Alien(500, 680));
        controller.checkCollisionsWithSpaceShip();
        controller.handleKeyPressed(KeyEvent.VK_SPACE);
        controller.tick();

        assertEquals(GameState.PLAYING, controller.getGameState());
        assertEquals(0, controller.getScore());
        assertEquals(1, controller.getWave());
        assertEquals(3, controller.getLives());
        assertEquals(10, aliens.size());
        assertEquals(0, missiles.size());
        assertEquals(500, spaceship.getX());
    }

    @Test
    void calculateAlienScoreScalesWithWave() {
        assertEquals(10, GameController.calculateAlienScore(1));
        assertEquals(30, GameController.calculateAlienScore(3));
    }

    @Test
    void calculateAlienSpeedStartsAtBaseSpeed() {
        assertEquals(5, GameController.calculateAlienSpeed(1, 5, 10));
    }

    @Test
    void calculateAlienSpeedIncreasesWithWave() {
        assertEquals(6, GameController.calculateAlienSpeed(2, 5, 10));
    }

    @Test
    void calculateAlienSpeedNeverExceedsCap() {
        assertEquals(10, GameController.calculateAlienSpeed(20, 5, 10));
    }

    private GameController newController(List<Missile> missiles, List<Alien> aliens) {
        return new GameController(new Spaceship(500, 680), missiles, aliens, null);
    }
}
