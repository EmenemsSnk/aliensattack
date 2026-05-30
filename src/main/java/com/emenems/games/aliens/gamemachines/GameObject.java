package com.emenems.games.aliens.gamemachines;

public sealed interface GameObject permits Alien, AlienMissile, Missile, Spaceship {
    int getX();
    int getY();
}
