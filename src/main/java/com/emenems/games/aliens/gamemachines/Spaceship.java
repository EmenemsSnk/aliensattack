package com.emenems.games.aliens.gamemachines;

public class Spaceship implements GameObject {
    private int health = 100;
    private int speed;
    private int x;
    private int y;

    public Spaceship(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void moveLeft(){
        x-=5;
    }

    public void moveRight(){
        x+=5;
    }

    public void moveUp(){
        y-=5;
    }

    public void moveDown(){
        y+=5;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public void decreaseHealth(int demage){
        health-=demage;
    }
}
