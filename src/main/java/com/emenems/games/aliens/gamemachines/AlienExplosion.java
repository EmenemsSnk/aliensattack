package com.emenems.games.aliens.gamemachines;

public final class AlienExplosion implements GameObject {
    static final int DEFAULT_LIFETIME_TICKS = 12;

    private final int x;
    private final int y;
    private int ticksRemaining;

    public AlienExplosion(int x, int y) {
        this(x, y, DEFAULT_LIFETIME_TICKS);
    }

    AlienExplosion(int x, int y, int ticksRemaining) {
        this.x = x;
        this.y = y;
        this.ticksRemaining = ticksRemaining;
    }

    public void tick() {
        if (ticksRemaining > 0) {
            ticksRemaining--;
        }
    }

    public boolean isExpired() {
        return ticksRemaining <= 0;
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
