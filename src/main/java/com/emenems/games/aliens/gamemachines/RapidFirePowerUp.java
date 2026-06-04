package com.emenems.games.aliens.gamemachines;

public final class RapidFirePowerUp implements GameObject {
    private static final int FALL_SPEED = 3;

    private final int x;
    private int y;

    public RapidFirePowerUp(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void move() {
        y += FALL_SPEED;
    }

    @Override
    public int getX() {
        return x;
    }

    @Override
    public int getY() {
        return y;
    }
}
