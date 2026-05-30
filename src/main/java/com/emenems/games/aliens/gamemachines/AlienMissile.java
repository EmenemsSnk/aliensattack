package com.emenems.games.aliens.gamemachines;

public final class AlienMissile implements GameObject {
    private static final int DEFAULT_SPEED = 4;

    private final int x;
    private int y;
    private final int speed;

    public AlienMissile(int x, int y) {
        this(x, y, DEFAULT_SPEED);
    }

    public AlienMissile(int x, int y, int speed) {
        this.x = x;
        this.y = y;
        this.speed = speed;
    }

    public void move() {
        y += speed;
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
