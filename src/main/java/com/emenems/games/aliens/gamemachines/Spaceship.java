package com.emenems.games.aliens.gamemachines;

public final class Spaceship implements GameObject {
    private static final int MOVE_STEP = 5;

    private int x;
    private int y;

    public Spaceship(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void moveLeft() {
        x -= MOVE_STEP;
    }

    public void moveRight() {
        x += MOVE_STEP;
    }

    public void moveUp() {
        y -= MOVE_STEP;
    }

    public void moveDown() {
        y += MOVE_STEP;
    }

    public void clampToBounds(int minX, int minY, int maxX, int maxY) {
        x = Math.clamp(x, minX, maxX);
        y = Math.clamp(y, minY, maxY);
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

}
