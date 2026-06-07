package com.emenems.games.aliens.gamemachines;

public final class PowerUp implements GameObject {
    private static final int FALL_SPEED = 3;

    private final PowerUpType type;
    private final int x;
    private int y;

    public PowerUp(PowerUpType type, int x, int y) {
        this.type = type;
        this.x = x;
        this.y = y;
    }

    public PowerUpType getType() {
        return type;
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
