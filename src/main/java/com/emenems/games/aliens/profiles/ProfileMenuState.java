package com.emenems.games.aliens.profiles;

import java.util.List;
import java.util.Objects;

public record ProfileMenuState(
    String selectedProfileName,
    int selectedBestScore,
    int profileCount,
    int selectedIndex,
    boolean inputMode,
    String draftName,
    String message,
    boolean saveFailed,
    boolean newBestScore,
    List<LeaderboardEntry> topProfiles
) {
    public ProfileMenuState {
        topProfiles = List.copyOf(Objects.requireNonNull(topProfiles, "topProfiles"));
    }

    public ProfileMenuState(
        String selectedProfileName,
        int selectedBestScore,
        int profileCount,
        int selectedIndex,
        boolean inputMode,
        String draftName,
        String message,
        boolean saveFailed,
        boolean newBestScore
    ) {
        this(
            selectedProfileName,
            selectedBestScore,
            profileCount,
            selectedIndex,
            inputMode,
            draftName,
            message,
            saveFailed,
            newBestScore,
            List.of()
        );
    }

    public static ProfileMenuState empty() {
        return new ProfileMenuState("", 0, 0, -1, false, "", "Create a profile with N", false, false);
    }

    public boolean hasSelectedProfile() {
        return !selectedProfileName.isBlank();
    }

    public String profileCounterText() {
        if (profileCount <= 0 || selectedIndex < 0) {
            return "No profiles";
        }
        return "Profile " + (selectedIndex + 1) + " of " + profileCount;
    }

    public String bestScoreText() {
        return hasSelectedProfile() ? "Best: " + selectedBestScore : "Best: -";
    }

    public String startPromptText() {
        return hasSelectedProfile() && !inputMode ? "Press ENTER to Start" : "Create a profile to start";
    }

    public record LeaderboardEntry(int rank, String name, int bestScore) {
    }
}
