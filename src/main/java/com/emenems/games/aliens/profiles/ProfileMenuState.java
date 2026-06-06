package com.emenems.games.aliens.profiles;

public record ProfileMenuState(
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
}
