package com.emenems.games.aliens.profiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProfileStoreTest {
    @TempDir
    private Path tempDir;

    @Test
    void missingFileLoadsAsEmptyProfiles() {
        ProfileStore store = new ProfileStore(tempDir.resolve("profiles.tsv"));

        assertTrue(store.loadProfiles().isEmpty());
    }

    @Test
    void savesAndLoadsProfilesInTabSeparatedFormat() throws IOException {
        Path path = tempDir.resolve("profiles.tsv");
        ProfileStore store = new ProfileStore(path);

        ProfileStore.SaveResult result = store.saveProfiles(List.of(
            new PlayerProfile("Player One", 120),
            new PlayerProfile("Ace_2", 40)
        ));

        assertTrue(result.success());
        assertEquals(List.of("Player One\t120", "Ace_2\t40"), Files.readAllLines(path));
        assertEquals(
            List.of(new PlayerProfile("Player One", 120), new PlayerProfile("Ace_2", 40)),
            store.loadProfiles()
        );
    }

    @Test
    void malformedFilesLoadAsEmptyProfiles() throws IOException {
        Path path = tempDir.resolve("profiles.tsv");
        Files.writeString(path, "Player\t10\nBrokenRow\n");
        ProfileStore store = new ProfileStore(path);

        assertTrue(store.loadProfiles().isEmpty());
    }

    @Test
    void invalidScoresAndNamesLoadAsEmptyProfiles() throws IOException {
        Path path = tempDir.resolve("profiles.tsv");
        Files.writeString(path, "Bad.Name\t10\n");
        ProfileStore store = new ProfileStore(path);

        assertTrue(store.loadProfiles().isEmpty());

        Files.writeString(path, "Player\t-1\n");

        assertTrue(store.loadProfiles().isEmpty());
    }

    @Test
    void duplicateNamesLoadAsEmptyProfiles() throws IOException {
        Path path = tempDir.resolve("profiles.tsv");
        Files.writeString(path, "Player\t10\nplayer\t20\n");
        ProfileStore store = new ProfileStore(path);

        assertTrue(store.loadProfiles().isEmpty());
    }

    @Test
    void saveFailureIsReportedWithoutThrowing() {
        Path path = tempDir.resolve("missing-parent").resolve("profiles.tsv");
        ProfileStore store = new ProfileStore(path);

        ProfileStore.SaveResult result = store.saveProfiles(List.of(new PlayerProfile("Player", 10)));

        assertFalse(result.success());
        assertFalse(result.message().isBlank());
    }

    @Test
    void invalidSaveDataIsReportedWithoutThrowing() {
        ProfileStore store = new ProfileStore(tempDir.resolve("profiles.tsv"));

        ProfileStore.SaveResult result = store.saveProfiles(List.of(
            new PlayerProfile("Player", 10),
            new PlayerProfile("player", 20)
        ));

        assertFalse(result.success());
        assertFalse(result.message().isBlank());
    }
}
