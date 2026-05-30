package com.emenems.games.aliens.gamemachines;

public class Spaceship implements GameObject {
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
        x = Math.max(minX, Math.min(x, maxX));
        y = Math.max(minY, Math.min(y, maxY));
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

}
