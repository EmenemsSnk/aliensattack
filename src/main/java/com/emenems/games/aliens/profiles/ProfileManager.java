package com.emenems.games.aliens.profiles;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class ProfileManager {
    private final ProfileStore profileStore;
    private List<PlayerProfile> profiles = new ArrayList<>();
    private int selectedProfileIndex = -1;
    private boolean inputMode;
    private String draftName = "";
    private String statusMessage = "Create a profile with N";
    private boolean saveFailed;
    private boolean newBestScore;
    private boolean gameOverScoreHandled;

    public ProfileManager(ProfileStore profileStore) {
        this.profileStore = profileStore;
    }

    public void loadProfiles() {
        profiles = new ArrayList<>(profileStore.loadProfiles());
        selectedProfileIndex = profiles.isEmpty() ? -1 : 0;
        statusMessage = profiles.isEmpty()
            ? "Create a profile with N"
            : "Selected profile: " + profiles.get(selectedProfileIndex).name();
        saveFailed = false;
    }

    public void replaceProfilesForTesting(List<PlayerProfile> testProfiles) {
        profiles = new ArrayList<>(testProfiles);
        selectedProfileIndex = profiles.isEmpty() ? -1 : 0;
        inputMode = false;
        draftName = "";
        statusMessage = profiles.isEmpty()
            ? "Create a profile with N"
            : "Selected profile: " + profiles.get(selectedProfileIndex).name();
        saveFailed = false;
        newBestScore = false;
    }

    public boolean hasSelectedProfile() {
        return selectedProfile() != null;
    }

    public boolean isInputMode() {
        return inputMode;
    }

    public ProfileMenuState buildMenuState() {
        PlayerProfile selected = selectedProfile();
        return new ProfileMenuState(
            selected == null ? "" : selected.name(),
            selected == null ? 0 : selected.bestScore(),
            profiles.size(),
            selectedProfileIndex,
            inputMode,
            draftName,
            statusMessage,
            saveFailed,
            newBestScore,
            topProfiles()
        );
    }

    public void clearNewBestScore() {
        newBestScore = false;
    }

    public void onNewGameStarted() {
        gameOverScoreHandled = false;
        newBestScore = false;
        saveFailed = false;
    }

    public void onGameOverScoreUpdate(int score) {
        if (gameOverScoreHandled) {
            return;
        }
        gameOverScoreHandled = true;
        PlayerProfile selected = selectedProfile();
        if (selected == null || score <= selected.bestScore()) {
            newBestScore = false;
            return;
        }

        PlayerProfile updated = selected.withBestScore(score);
        profiles.set(selectedProfileIndex, updated);
        ProfileStore.SaveResult saveResult = profileStore.saveProfiles(profiles);
        saveFailed = !saveResult.success();
        newBestScore = true;
        statusMessage = saveResult.success()
            ? "New best score for " + updated.name()
            : "New best score kept, but save failed";
    }

    public void beginProfileInput() {
        inputMode = true;
        draftName = "";
        statusMessage = "Type a profile name";
    }

    public void cancelProfileInput() {
        inputMode = false;
        draftName = "";
        statusMessage = hasSelectedProfile() ? "Profile creation cancelled" : "Create a profile with N";
    }

    public void handleProfileInputKeyPressed(int keyCode, char keyChar) {
        if (keyCode == java.awt.event.KeyEvent.VK_ENTER) {
            createProfileFromDraft();
        } else if (keyCode == java.awt.event.KeyEvent.VK_ESCAPE) {
            cancelProfileInput();
        } else if (keyCode == java.awt.event.KeyEvent.VK_BACK_SPACE) {
            if (!draftName.isEmpty()) {
                draftName = draftName.substring(0, draftName.length() - 1);
            }
            statusMessage = "Type a profile name";
        } else if (keyChar != java.awt.event.KeyEvent.CHAR_UNDEFINED && ProfileNameValidator.isAllowedCharacter(keyChar)) {
            if (draftName.length() < ProfileNameValidator.MAX_NAME_LENGTH) {
                draftName += keyChar;
                statusMessage = "Press ENTER to save";
            } else {
                statusMessage = ProfileNameValidator.TOO_LONG_MESSAGE;
            }
        }
    }

    public void selectPreviousProfile() {
        if (profiles.isEmpty()) {
            statusMessage = "Create a profile with N";
            return;
        }
        selectedProfileIndex = Math.floorMod(selectedProfileIndex - 1, profiles.size());
        statusMessage = "Selected profile: " + profiles.get(selectedProfileIndex).name();
    }

    public void selectNextProfile() {
        if (profiles.isEmpty()) {
            statusMessage = "Create a profile with N";
            return;
        }
        selectedProfileIndex = Math.floorMod(selectedProfileIndex + 1, profiles.size());
        statusMessage = "Selected profile: " + profiles.get(selectedProfileIndex).name();
    }

    public void setStatusMessage(String message) {
        statusMessage = message;
    }

    private void createProfileFromDraft() {
        ProfileNameValidator.ValidationResult result = ProfileNameValidator.validate(draftName);
        if (!result.valid()) {
            statusMessage = result.message();
            return;
        }
        if (profileNameExists(result.normalizedName())) {
            statusMessage = "Profile already exists";
            return;
        }

        PlayerProfile profile = new PlayerProfile(result.normalizedName(), 0);
        profiles.add(profile);
        selectedProfileIndex = profiles.size() - 1;
        inputMode = false;
        draftName = "";
        ProfileStore.SaveResult saveResult = profileStore.saveProfiles(profiles);
        saveFailed = !saveResult.success();
        statusMessage = saveResult.success()
            ? "Created profile: " + profile.name()
            : "Profile created, but save failed";
    }

    private boolean profileNameExists(String name) {
        String key = name.toLowerCase(Locale.ROOT);
        return profiles.stream()
            .map(p -> p.name().toLowerCase(Locale.ROOT))
            .anyMatch(key::equals);
    }

    private PlayerProfile selectedProfile() {
        if (selectedProfileIndex < 0 || selectedProfileIndex >= profiles.size()) {
            return null;
        }
        return profiles.get(selectedProfileIndex);
    }

    private List<ProfileMenuState.LeaderboardEntry> topProfiles() {
        List<PlayerProfile> ranked = profiles.stream()
            .sorted(
                Comparator.comparingInt(PlayerProfile::bestScore)
                    .reversed()
                    .thenComparing(PlayerProfile::name)
            )
            .limit(5)
            .toList();
        List<ProfileMenuState.LeaderboardEntry> entries = new ArrayList<>();
        for (int i = 0; i < ranked.size(); i++) {
            PlayerProfile p = ranked.get(i);
            entries.add(new ProfileMenuState.LeaderboardEntry(i + 1, p.name(), p.bestScore()));
        }
        return entries;
    }
}
