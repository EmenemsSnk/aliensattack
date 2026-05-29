package com.emenems.games.aliens.gamemachines;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Harness-proving test for the pure {@link Spaceship} movement methods.
 * Its purpose is to prove the JUnit 5 + surefire wiring works end-to-end;
 * the correctness of {@code Spaceship} is incidental.
 */
class SpaceshipTest {

    @Test
    void constructorRoundTripsCoordinates() {
        Spaceship ship = new Spaceship(100, 200);
        assertEquals(100, ship.getX());
        assertEquals(200, ship.getY());
    }

    @Test
    void moveLeftDecreasesXByFive() {
        Spaceship ship = new Spaceship(100, 200);
        ship.moveLeft();
        assertEquals(95, ship.getX());
        assertEquals(200, ship.getY());
    }

    @Test
    void moveRightIncreasesXByFive() {
        Spaceship ship = new Spaceship(100, 200);
        ship.moveRight();
        assertEquals(105, ship.getX());
        assertEquals(200, ship.getY());
    }

    @Test
    void moveUpDecreasesYByFive() {
        Spaceship ship = new Spaceship(100, 200);
        ship.moveUp();
        assertEquals(195, ship.getY());
        assertEquals(100, ship.getX());
    }

    @Test
    void moveDownIncreasesYByFive() {
        Spaceship ship = new Spaceship(100, 200);
        ship.moveDown();
        assertEquals(205, ship.getY());
        assertEquals(100, ship.getX());
    }
}
