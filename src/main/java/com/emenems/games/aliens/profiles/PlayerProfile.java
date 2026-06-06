package com.emenems.games.aliens.profiles;

import java.util.Objects;

public record PlayerProfile(String name, int bestScore) {
    public PlayerProfile {
        Objects.requireNonNull(name, "name");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Profile name must not be blank");
        }
        if (bestScore < 0) {
            throw new IllegalArgumentException("Best score must not be negative");
        }
    }

    public PlayerProfile withBestScore(int newBestScore) {
        return new PlayerProfile(name, newBestScore);
    }
}
