package com.emenems.games.aliens.gamemachines;

public class Alien implements GameObject {
    private int x;
    private int y;
    private int speed;

    public Alien(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void move() {
        y+=5;
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
