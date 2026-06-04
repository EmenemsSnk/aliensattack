package com.emenems.games.aliens.gamemachines;

public sealed interface GameObject permits Alien, AlienMissile, Missile, RapidFirePowerUp, Spaceship {
    int getX();
    int getY();
}
