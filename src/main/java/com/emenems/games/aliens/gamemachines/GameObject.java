package com.emenems.games.aliens.gamemachines;

public sealed interface GameObject permits Alien, AlienExplosion, AlienMissile, Missile, RapidFirePowerUp, Spaceship {
    int getX();
    int getY();
}
