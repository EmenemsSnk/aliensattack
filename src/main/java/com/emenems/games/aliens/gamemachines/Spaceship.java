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
        moveLeft(MOVE_STEP);
    }

    public void moveRight() {
        moveRight(MOVE_STEP);
    }

    public void moveUp() {
        moveUp(MOVE_STEP);
    }

    public void moveDown() {
        moveDown(MOVE_STEP);
    }

    public void moveLeft(int step) {
        x -= step;
    }

    public void moveRight(int step) {
        x += step;
    }

    public void moveUp(int step) {
        y -= step;
    }

    public void moveDown(int step) {
        y += step;
    }

    public static int defaultMoveStep() {
        return MOVE_STEP;
    }

    public void clampToBounds(int minX, int minY, int maxX, int maxY) {
        x = Math.clamp(x, minX, maxX);
        y = Math.clamp(y, minY, maxY);
    }

    public void moveTo(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

}
