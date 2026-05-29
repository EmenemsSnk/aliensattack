package com.emenems.games.aliens.gamemachines;

public class Alien implements GameObject {
    private int x;
    private double y;
    private double speed;

    public Alien(int x, int y) {
        this(x, y, 5);
    }

    public Alien(int x, int y, double speed) {
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
        return (int) Math.round(y);
    }
}
