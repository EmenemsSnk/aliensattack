package com.emenems.games.aliens.profiles;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ProfileStore {
    private static final Path DEFAULT_PATH = Path.of("profiles.tsv");

    private final Path path;

    public ProfileStore() {
        this(DEFAULT_PATH);
    }

    public ProfileStore(Path path) {
        this.path = path;
    }

    public List<PlayerProfile> loadProfiles() {
        try {
            if (!Files.exists(path)) {
                return List.of();
            }

            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            List<PlayerProfile> profiles = new ArrayList<>();
            Set<String> normalizedNames = new HashSet<>();
            for (String line : lines) {
                if (line.isBlank()) {
                    continue;
                }
                String[] parts = line.split("\t", -1);
                if (parts.length != 2) {
                    return malformed("expected two tab-separated columns");
                }

                ProfileNameValidator.ValidationResult nameResult = ProfileNameValidator.validate(parts[0]);
                if (!nameResult.valid()) {
                    return malformed("invalid profile name");
                }

                int bestScore;
                try {
                    bestScore = Integer.parseInt(parts[1]);
                } catch (NumberFormatException exception) {
                    return malformed("invalid best score");
                }
                if (bestScore < 0) {
                    return malformed("negative best score");
                }

                String duplicateKey = nameResult.normalizedName().toLowerCase(Locale.ROOT);
                if (!normalizedNames.add(duplicateKey)) {
                    return malformed("duplicate profile name");
                }

                profiles.add(new PlayerProfile(nameResult.normalizedName(), bestScore));
            }
            return List.copyOf(profiles);
        } catch (NoSuchFileException exception) {
            return List.of();
        } catch (IOException | SecurityException exception) {
            System.err.println("Could not load profiles from " + path + ": " + exception.getMessage());
            return List.of();
        }
    }

    public SaveResult saveProfiles(List<PlayerProfile> profiles) {
        try {
            Files.write(path, serialize(profiles), StandardCharsets.UTF_8);
            return SaveResult.ok();
        } catch (IOException | IllegalArgumentException | SecurityException exception) {
            String message = "Could not save profiles to " + path + ": " + exception.getMessage();
            System.err.println(message);
            return SaveResult.failed(message);
        }
    }

    public Path path() {
        return path;
    }

    private List<String> serialize(List<PlayerProfile> profiles) {
        List<String> lines = new ArrayList<>();
        Set<String> normalizedNames = new HashSet<>();
        for (PlayerProfile profile : profiles) {
            ProfileNameValidator.ValidationResult nameResult = ProfileNameValidator.validate(profile.name());
            if (!nameResult.valid()) {
                throw new IllegalArgumentException("Invalid profile name: " + profile.name());
            }
            String duplicateKey = nameResult.normalizedName().toLowerCase(Locale.ROOT);
            if (!normalizedNames.add(duplicateKey)) {
                throw new IllegalArgumentException("Duplicate profile name: " + profile.name());
            }
            lines.add(nameResult.normalizedName() + "\t" + profile.bestScore());
        }
        return lines;
    }

    private List<PlayerProfile> malformed(String reason) {
        System.err.println("Ignoring malformed profiles file " + path + ": " + reason);
        return List.of();
    }

    public record SaveResult(boolean success, String message) {
        private static SaveResult ok() {
            return new SaveResult(true, "");
        }

        private static SaveResult failed(String message) {
            return new SaveResult(false, message);
        }
    }
}
