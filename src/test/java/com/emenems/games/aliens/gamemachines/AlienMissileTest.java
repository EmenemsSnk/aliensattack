package com.emenems.games.aliens.gamemachines;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AlienMissileTest {

    @Test
    void moveTravelsDownward() {
        AlienMissile missile = new AlienMissile(100, 200, 6);

        missile.move();

        assertEquals(100, missile.getX());
        assertEquals(206, missile.getY());
    }
}
