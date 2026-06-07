package com.emenems.games.aliens.gamemachines;

import com.emenems.games.aliens.GameConstants;
import com.emenems.games.aliens.GameRules;
import java.util.Random;

public final class Alien implements GameObject {
    private static final int SPECIAL_HORIZONTAL_STEP = GameConstants.COMPONENT_SIZE / 6;
    private static final double BOSS_SPEED_VARIANCE_FLOOR = 0.65;
    private static final double BOSS_SPEED_VARIANCE_RANGE = 0.7;

    private int x;
    private double y;
    private final double speed;
    private final AlienType type;
    private int health;
    private int horizontalDirection;
    private int lastHorizontalDelta;

    public Alien(int x, int y) {
        this(x, y, 5);
    }

    public Alien(int x, int y, double speed) {
        this(x, y, speed, AlienType.STANDARD);
    }

    public Alien(int x, int y, double speed, AlienType type) {
        this.x = x;
        this.y = y;
        this.type = type;
        this.speed = switch (type) {
            case SPECIAL -> speed * GameRules.specialAlienSpeedMultiplier();
            case BOSS -> GameRules.bossHorizontalSpeed();
            case STANDARD -> speed;
        };
        this.health = switch (type) {
            case SPECIAL -> 2;
            case BOSS -> GameRules.bossHealth();
            case STANDARD -> 1;
        };
        this.horizontalDirection = 1;
    }

    public void move() {
        y += speed;
    }

    public void move(Random random, int minX, int maxX) {
        if (type == AlienType.BOSS) {
            moveBoss(random, minX, maxX);
            return;
        }

        move();
        if (type != AlienType.SPECIAL) {
            return;
        }

        if (random.nextDouble() < GameRules.specialAlienDirectionChangeChance()) {
            horizontalDirection *= -1;
        }

        moveHorizontallyWithinBounds(minX, maxX);
    }

    public static Alien special(int x, int y, double speed) {
        return new Alien(x, y, speed, AlienType.SPECIAL);
    }

    public static Alien boss(int x, int y) {
        return new Alien(x, y, 0, AlienType.BOSS);
    }

    public AlienType getType() {
        return type;
    }

    public int getHealth() {
        return health;
    }

    public boolean isDamaged() {
        return switch (type) {
            case SPECIAL -> health == 1;
            case BOSS -> health < GameRules.bossHealth();
            case STANDARD -> false;
        };
    }

    public boolean takeHit() {
        health--;
        return health <= 0;
    }

    public boolean isSpecial() {
        return type == AlienType.SPECIAL;
    }

    public boolean isBoss() {
        return type == AlienType.BOSS;
    }

    public void reverseHorizontalDirection() {
        horizontalDirection *= -1;
    }

    public void undoHorizontalMove() {
        x -= lastHorizontalDelta;
        lastHorizontalDelta = 0;
    }

    public void separateFrom(Alien otherAlien, int minX, int maxX) {
        reverseHorizontalDirection();

        if (getY() <= otherAlien.getY()) {
            y = otherAlien.getY() - GameConstants.COMPONENT_SIZE;
        } else if (getY() >= otherAlien.getY()) {
            y = otherAlien.getY() + GameConstants.COMPONENT_SIZE;
        }

        int preferredX = horizontalDirection < 0
            ? otherAlien.getX() - GameConstants.COMPONENT_SIZE
            : otherAlien.getX() + GameConstants.COMPONENT_SIZE;
        int fallbackX = horizontalDirection < 0
            ? otherAlien.getX() + GameConstants.COMPONENT_SIZE
            : otherAlien.getX() - GameConstants.COMPONENT_SIZE;

        x = clampWithinBounds(preferredX, minX, maxX);
        if (Math.abs(x - otherAlien.getX()) < GameConstants.COMPONENT_SIZE) {
            x = clampWithinBounds(fallbackX, minX, maxX);
        }
        lastHorizontalDelta = 0;
    }

    private void moveHorizontallyWithinBounds(int minX, int maxX) {
        lastHorizontalDelta = horizontalDirection * SPECIAL_HORIZONTAL_STEP;
        x += lastHorizontalDelta;
        if (x < minX) {
            x = minX;
            horizontalDirection = 1;
        } else if (x > maxX) {
            x = maxX;
            horizontalDirection = -1;
        }
    }

    private void moveBoss(Random random, int minX, int maxX) {
        if (random.nextDouble() < GameRules.bossDirectionChangeChance()) {
            horizontalDirection *= -1;
        }

        double speedVariance = BOSS_SPEED_VARIANCE_FLOOR + random.nextDouble() * BOSS_SPEED_VARIANCE_RANGE;
        lastHorizontalDelta = (int) Math.round(horizontalDirection * speed * speedVariance);
        if (lastHorizontalDelta == 0) {
            lastHorizontalDelta = horizontalDirection;
        }

        x += lastHorizontalDelta;
        if (x < minX) {
            x = minX;
            horizontalDirection = 1;
        } else if (x > maxX) {
            x = maxX;
            horizontalDirection = -1;
        }
    }

    private int clampWithinBounds(int value, int minX, int maxX) {
        return Math.max(minX, Math.min(maxX, value));
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
