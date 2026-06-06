# Local Player Profiles Implementation Plan

## Overview

Add local, unprotected player profiles to Aliens Attack and extend the slice to persist each profile's best score. The player must create or select a profile on the start screen before play starts; profile data is stored in a working-directory file, and read/write failures degrade gracefully instead of interrupting gameplay.

## Current State Analysis

Aliens Attack is a single-process Java 21 Swing game with no runtime dependencies beyond the JDK. `GameController` owns input, state transitions, gameplay orchestration, and the scalar state push into `GamePanel`; `GamePanel` passively renders shared entity lists plus pushed scalar state. There is no local persistence today, and historical plans repeatedly treated persistence, profiles, and score history as out of scope until this roadmap slice.

The start screen is currently a simple passive overlay. In `START_MENU`, only `ENTER` starts the game; in `PLAYING`, arrow keys move the ship and `SPACE` fires. This change must add profile selection and creation only to the start menu so active gameplay controls remain unchanged.

## Desired End State

On launch, the game loads local profiles from `profiles.tsv` in the working directory. If profiles exist, the start screen shows the selected profile and its best score; left/right arrows cycle the selection, `N` enters a simple name-entry mode, and `ENTER` starts only when a profile is selected. If no profiles exist, `ENTER` does not start gameplay and the start screen instructs the player to create one.

When the session reaches Game Over, the selected profile's stored best score is updated only if `finalScore > bestScore`, then saved. Save failures are logged to `System.err` and shown as a non-blocking message; they never throw into the game loop or prevent restart.

### Key Findings:

- `GameController.handleKeyPressed(...)` is the state-specific input router for `START_MENU`, `GAME_OVER`, `PAUSED`, and `PLAYING`: `src/main/java/com/emenems/games/aliens/controller/GameController.java:209`.
- `GameController.updatePanelState()` is the established push point into rendering and already carries all menu/gameplay scalar state into `GamePanel.updateGameState(...)`: `src/main/java/com/emenems/games/aliens/controller/GameController.java:496`.
- `GamePanel.drawStartMenu(...)` currently renders only title, start prompt, and controls text, so profile UI belongs there without changing gameplay rendering ownership: `src/main/java/com/emenems/games/aliens/gui/GamePanel.java:275`.
- `GameSession.startOrRestart()` owns fresh-session scalar reset, while Game Over is entered inside `GameSession.loseLife()` and `enterAlienInvasionGameOver()`: `src/main/java/com/emenems/games/aliens/GameSession.java:75`.
- Project constraints allow Java 21 and JDK APIs but no new runtime libraries; `pom.xml` has only JUnit 5 as a test dependency.

## What We Are NOT Doing

- No online accounts, authentication, passwords, cloud sync, networking, or global leaderboard.
- No separate high-score table UI beyond showing the selected profile's best score.
- No profile deletion, rename, import/export, settings screen, or mouse-based menu.
- No save-state feature; gameplay state is not persisted.
- No new external runtime dependencies or JSON parser.
- No rewrite of the controller/panel architecture.

## Implementation Approach

Introduce a small `profiles` package for the durable profile model, name validation, and file storage. Keep profile selection and profile-input mode under `GameController`, because profile input affects when `START_MENU` can transition into `PLAYING`. Pass a compact immutable profile menu view object into `GamePanel` through the existing panel update path so rendering remains passive.

Persist profiles in a simple working-directory `profiles.tsv` file with one profile per line: `<name>\t<bestScore>`. The accepted profile-name validation excludes tabs and newlines, so this format stays human-readable and does not require escaping. Treat missing files as an empty profile list; treat malformed files as an empty list with a `System.err` diagnostic.

## Critical Implementation Details

Best-score persistence must be triggered exactly once per Game Over transition, not on every non-`PLAYING` tick after Game Over. Add a controller-owned guard such as `gameOverScoreHandled` that resets when a new game starts and flips after the selected profile is evaluated for update.

## Phase 1: Profile Model and Storage

### Overview

Create the profile data surface, validation rules, file format, and storage behavior before touching gameplay or rendering.

### Required Changes:

#### 1. Profile domain model

**File**: `src/main/java/com/emenems/games/aliens/profiles/PlayerProfile.java`

**Purpose**: Represent one local player profile with a display name and persisted best score.

**Contract**: Add an immutable Java 21 record `PlayerProfile(String name, int bestScore)` or equivalent immutable class. It must reject null/blank names and negative best scores at construction or through factory validation.

#### 2. Profile-name validation

**File**: `src/main/java/com/emenems/games/aliens/profiles/ProfileNameValidator.java`

**Purpose**: Centralize normalization and validation so UI input, tests, and storage never duplicate name rules.

**Contract**: Trim input and accept names of 1-16 characters containing only letters, digits, spaces, hyphens, and underscores. Reject blank names, control characters, tabs, newlines, and other punctuation. Expose a validation result that lets the controller render a short message instead of throwing for normal user mistakes.

#### 3. File-backed profile store

**File**: `src/main/java/com/emenems/games/aliens/profiles/ProfileStore.java`

**Purpose**: Load and save profiles using only JDK file APIs.

**Contract**: Default storage path is `Path.of("profiles.tsv")` relative to the process working directory. The format is one line per profile: `<name>\t<bestScore>`. Missing file returns an empty list. Malformed content or I/O read failures return an empty list and log to `System.err`. Save writes the complete profile list; save failures are reported to the caller and logged, but never thrown into gameplay.

#### 4. Storage tests

**Files**:

- `src/test/java/com/emenems/games/aliens/profiles/ProfileNameValidatorTest.java`
- `src/test/java/com/emenems/games/aliens/profiles/ProfileStoreTest.java`

**Purpose**: Lock validation, format, and graceful-degradation behavior before controller integration.

**Contract**: Tests use JUnit 5 `@TempDir` and an injected store path. Cover valid names, invalid names, missing file, round-trip profiles with best scores, malformed file fallback, duplicate handling, and save failure reporting if feasible without platform-specific assumptions.

### Success Criteria:

#### Automated Verification:

- `ProfileNameValidatorTest` covers accepted and rejected profile names.
- `ProfileStoreTest` covers missing, valid, malformed, and round-tripped `profiles.tsv` data.
- `./mvnw test -Dtest=ProfileNameValidatorTest,ProfileStoreTest` passes.
- `./mvnw clean compile` passes.

#### Manual Verification:

- A developer can inspect `profiles.tsv` and understand the profile names and best scores.

**Implementation Note**: After this phase passes automated verification, continue to Phase 2; manual inspection can happen after the first integrated run writes a real file.

---

## Phase 2: Controller and Session Integration

### Overview

Wire profiles into startup, start-menu input, selected-profile state, and one-time best-score saving at Game Over.

### Required Changes:

#### 1. Profile menu state

**File**: `src/main/java/com/emenems/games/aliens/profiles/ProfileMenuState.java`

**Purpose**: Carry passive profile UI data from controller to panel without making `GamePanel` query storage or mutate profile lists.

**Contract**: Immutable view state containing selected profile name, selected best score, total profile count, selected index, input-mode flag, draft input text, and a short status/error message. It must have a no-profile state for empty stores.

#### 2. Controller construction and loading

**File**: `src/main/java/com/emenems/games/aliens/controller/GameController.java`

**Purpose**: Let production use the default working-directory store while tests inject temporary stores.

**Contract**: Add constructor wiring for `ProfileStore` while preserving existing constructors for tests and call sites. On initialization, load profiles once before the first panel state push. If profiles load successfully and are non-empty, select the first profile. If not, stay in no-profile state.

#### 3. Start-menu input routing

**File**: `src/main/java/com/emenems/games/aliens/controller/GameController.java`

**Purpose**: Enforce profile selection before gameplay and support profile creation/selection only on the start screen.

**Contract**: In `START_MENU`, left/right arrows cycle profiles only when not entering a new name. `N` enters profile-name input mode. While entering a name, printable allowed characters update the draft, `BACK_SPACE` removes one character, `ENTER` validates and creates/selects the profile, and `ESCAPE` cancels input. `ENTER` starts the game only when not in input mode and a profile is selected.

#### 4. Duplicate and invalid profile handling

**File**: `src/main/java/com/emenems/games/aliens/controller/GameController.java`

**Purpose**: Keep profile creation predictable without blocking the app on user mistakes.

**Contract**: Duplicate names are rejected case-insensitively with a short status message. Invalid names show the validator message and keep the draft editable. Successful creation appends the profile with `bestScore = 0`, selects it, saves the full profile list, and leaves `START_MENU` visible until the player presses `ENTER` to start.

#### 5. Game Over best-score update

**Files**:

- `src/main/java/com/emenems/games/aliens/controller/GameController.java`
- `src/main/java/com/emenems/games/aliens/GameSession.java`

**Purpose**: Save a selected profile's best score after a completed session without changing scoring rules.

**Contract**: When gameplay first transitions into `GAME_OVER`, compare final score with the selected profile's `bestScore`. If `finalScore > bestScore`, update the selected profile in memory and save `profiles.tsv`. If save fails, keep gameplay usable, log to `System.err`, and show a non-blocking message. Do not update on ties or lower scores. Reset the one-time guard on each fresh `startOrRestart()`.

#### 6. Controller tests

**File**: `src/test/java/com/emenems/games/aliens/controller/GameControllerTest.java`

**Purpose**: Cover profile-gated start behavior, creation, selection, and best-score persistence through deterministic controller paths.

**Contract**: Add tests for `ENTER` not starting without a profile, creating a valid profile from start-menu input, rejecting invalid and duplicate names, cycling with left/right only in `START_MENU`, starting after selection, preserving gameplay arrow behavior during `PLAYING`, updating best score only when final score is greater, not updating on tie/lower score, and not repeating the save on later non-playing ticks.

### Success Criteria:

#### Automated Verification:

- Controller tests cover profile-gated start, create/select flow, invalid input, duplicate input, and left/right selection.
- Controller tests cover one-time best-score update on `finalScore > bestScore`.
- Controller tests cover no best-score update on equal or lower final score.
- Controller tests cover save failure not preventing Game Over or restart.
- `./mvnw test -Dtest=GameControllerTest` passes.
- `./mvnw clean compile` passes.

#### Manual Verification:

- On a fresh working directory with no `profiles.tsv`, pressing `ENTER` on the start screen does not start gameplay and prompts profile creation.
- Creating a valid profile selects it but does not auto-start until `ENTER` is pressed again.
- Left/right profile switching works on the start screen and does not affect in-game movement behavior.

**Implementation Note**: Stop here for manual confirmation that the start-menu interaction feels acceptable before proceeding to final UI polish.

---

## Phase 3: Start Menu, Game Over UI, and Documentation

### Overview

Render the profile workflow clearly and update player-facing docs so the new required start step is discoverable.

### Required Changes:

#### 1. Panel profile state rendering

**File**: `src/main/java/com/emenems/games/aliens/gui/GamePanel.java`

**Purpose**: Show profile selection, name input, status messages, and best score without owning profile logic.

**Contract**: Extend `updateGameState(...)` or add an adjacent panel setter to receive `ProfileMenuState`. In `drawStartMenu(...)`, render selected profile name, best score, current index/count, and instructions: left/right to select, `N` to create, `ENTER` to start when a profile is selected. In input mode, render the draft profile name and validation/status message. Keep text within the existing fixed 760x650 panel.

#### 2. Game Over profile result

**File**: `src/main/java/com/emenems/games/aliens/gui/GamePanel.java`

**Purpose**: Make profile scoring visible when the session ends.

**Contract**: In `drawGameOver(...)`, render selected profile name and current best score below or near the final score. If the finished score became a new best, show a short `New Best Score!` style message. If save failed, show a short non-blocking warning without hiding restart instructions.

#### 3. Panel tests

**File**: `src/test/java/com/emenems/games/aliens/gui/GamePanelTest.java`

**Purpose**: Keep profile UI predicates and formatting testable without screenshot assertions.

**Contract**: Add static helper tests if useful for input prompt visibility, best-score message visibility, and profile-count formatting. Avoid brittle pixel rendering tests.

#### 4. README controls update

**File**: `README.md`

**Purpose**: Document the required profile step and new start-menu controls.

**Contract**: Update the controls section to mention start-screen left/right profile selection, `N` to create a profile, `ENTER` to start with selected profile, and `profiles.tsv` local working-directory storage.

### Success Criteria:

#### Automated Verification:

- `GamePanelTest` covers profile UI helper behavior added in this phase.
- `./mvnw test -Dtest=GamePanelTest` passes.
- `./mvnw test` passes.
- `./mvnw clean compile` passes.

#### Manual Verification:

- Start screen clearly communicates the required profile workflow when no profiles exist.
- Start screen clearly shows selected profile and best score when profiles exist.
- Game Over shows final score, selected profile, stored best score, and restart instruction.
- Save-failure messaging is visible but does not prevent restart.

**Implementation Note**: After this phase, run a full manual desktop smoke test before considering the plan implemented.

---

## Phase 4: Full Regression and Manual Verification

### Overview

Verify that the new persistence and start-menu behavior did not regress the arcade loop.

### Required Changes:

#### 1. Full automated verification

**Files**: Existing production and test suite

**Purpose**: Validate that the profile slice still satisfies the project's compile/test guardrails.

**Contract**: Run the complete test suite and compile gate from the repository root.

#### 2. Manual desktop smoke test

**Files**: Running application via Maven

**Purpose**: Confirm the Swing UI, file persistence, and gameplay loop work together in the real desktop app.

**Contract**: Launch with `./mvnw exec:java -Dexec.mainClass="com.emenems.games.aliens.Main"`. Verify first-run profile creation, restart persistence by relaunching, profile switching, best-score update only on better final score, malformed `profiles.tsv` fallback, pause/resume, Game Over restart, and normal movement/fire controls.

### Success Criteria:

#### Automated Verification:

- `./mvnw clean compile` passes.
- `./mvnw test` passes.

#### Manual Verification:

- Fresh launch with no `profiles.tsv` requires profile creation before gameplay.
- Relaunch after creating profiles shows persisted profiles from `profiles.tsv`.
- A better completed score updates the selected profile's best score; equal or lower scores do not.
- Malformed `profiles.tsv` logs an error and lets the player create a new profile.
- Existing controls still work: arrows move during play, `SPACE` fires, `P` pauses/resumes, `ENTER` restarts on Game Over.
- Audio-unavailable behavior remains safe.

**Implementation Note**: Do not archive until the manual desktop smoke test has been completed.

---

## Testing Strategy

### Unit Tests:

- Profile-name validation for accepted, rejected, trimmed, too-long, and control-character inputs.
- Profile storage for missing file, valid file, malformed file, duplicate handling, save and reload.
- Controller start-menu behavior: no-profile start blocking, profile creation input, invalid/duplicate messages, selection cycling.
- Controller best-score behavior: update only on `finalScore > bestScore`, no update on tie/lower score, one save per Game Over transition.
- Panel helper behavior for profile display strings and best-score message predicates.

### Integration Tests:

- Controller plus injected temporary `ProfileStore` for create/select/start/save flows.
- Existing controller tests for gameplay input, pause, restart, scoring, waves, power-ups, combo, and special aliens must continue to pass.

### Manual Testing Steps:

1. Remove or rename `profiles.tsv`, launch the game, and confirm `ENTER` does not start until a profile is created.
2. Press `N`, type a valid profile, press `ENTER`, then press `ENTER` again to start gameplay.
3. Finish a session with a non-zero score and confirm Game Over shows the profile and new best score.
4. Relaunch and confirm the profile and best score persist.
5. Create a second profile, use left/right to switch profiles, and confirm each profile has independent best score data.
6. Try invalid names and duplicate names and confirm the game stays on the start screen with a clear message.
7. Corrupt `profiles.tsv`, relaunch, and confirm the game falls back to empty profile state without crashing.
8. Verify arrows, `SPACE`, `P`, Game Over restart, scoring, waves, rapid-fire, combo, and special aliens still behave normally during play.

## Performance Considerations

The profile list is small and loaded once at startup. Saving rewrites a tiny `profiles.tsv` file only on profile creation or a new best score. No per-tick file I/O should occur; all gameplay ticks remain in-memory.

## Migration Notes

There is no existing persisted data to migrate. The new `profiles.tsv` is introduced as version-1 local working-directory data. If malformed content is found, the first implementation should not attempt partial recovery unless it is trivial and well-tested; fallback to an empty profile list is acceptable per the PRD guardrail.

Rollback consists of removing the `profiles` package, profile fields from controller/panel updates, profile UI rendering, README additions, and `profiles.tsv` from the working directory. Existing saved files can be ignored by older builds.

## References

- PRD local profiles and guardrails: `context/foundation/prd.md`
- Roadmap S-08/S-09 split and selected scope expansion: `context/foundation/roadmap.md`
- Original profile decisions: `context/foundation/shape-notes.md`
- Controller state/input routing: `src/main/java/com/emenems/games/aliens/controller/GameController.java:209`
- Controller panel state push: `src/main/java/com/emenems/games/aliens/controller/GameController.java:496`
- Start/Game Over rendering: `src/main/java/com/emenems/games/aliens/gui/GamePanel.java:258`
- Session lifecycle and scoring state: `src/main/java/com/emenems/games/aliens/GameSession.java:75`

## Progress

> Convention: `- [ ]` pending, `- [x]` done. Append ` — <commit sha>` when a step lands. Do not rename step titles.

### Phase 1: Profile Model and Storage

#### Automated

- [x] 1.1 `ProfileNameValidatorTest` covers accepted and rejected profile names. — c2ab968
- [x] 1.2 `ProfileStoreTest` covers missing, valid, malformed, and round-tripped `profiles.tsv` data. — c2ab968
- [x] 1.3 `./mvnw test -Dtest=ProfileNameValidatorTest,ProfileStoreTest` passes. — c2ab968
- [x] 1.4 `./mvnw clean compile` passes. — c2ab968

#### Manual

- [x] 1.5 A developer can inspect `profiles.tsv` and understand the profile names and best scores. — c2ab968

### Phase 2: Controller and Session Integration

#### Automated

- [x] 2.1 Controller tests cover profile-gated start, create/select flow, invalid input, duplicate input, and left/right selection. — c2ab968
- [x] 2.2 Controller tests cover one-time best-score update on `finalScore > bestScore`. — c2ab968
- [x] 2.3 Controller tests cover no best-score update on equal or lower final score. — c2ab968
- [x] 2.4 Controller tests cover save failure not preventing Game Over or restart. — c2ab968
- [x] 2.5 `./mvnw test -Dtest=GameControllerTest` passes. — c2ab968
- [x] 2.6 `./mvnw clean compile` passes. — c2ab968

#### Manual

- [x] 2.7 On a fresh working directory with no `profiles.tsv`, pressing `ENTER` on the start screen does not start gameplay and prompts profile creation. — c2ab968
- [x] 2.8 Creating a valid profile selects it but does not auto-start until `ENTER` is pressed again. — c2ab968
- [x] 2.9 Left/right profile switching works on the start screen and does not affect in-game movement behavior. — c2ab968

### Phase 3: Start Menu, Game Over UI, and Documentation

#### Automated

- [x] 3.1 `GamePanelTest` covers profile UI helper behavior added in this phase. — c2ab968
- [x] 3.2 `./mvnw test -Dtest=GamePanelTest` passes. — c2ab968
- [x] 3.3 `./mvnw test` passes. — c2ab968
- [x] 3.4 `./mvnw clean compile` passes. — c2ab968

#### Manual

- [x] 3.5 Start screen clearly communicates the required profile workflow when no profiles exist. — c2ab968
- [x] 3.6 Start screen clearly shows selected profile and best score when profiles exist. — c2ab968
- [x] 3.7 Game Over shows final score, selected profile, stored best score, and restart instruction. — c2ab968
- [x] 3.8 Save-failure messaging is visible but does not prevent restart. — c2ab968

### Phase 4: Full Regression and Manual Verification

#### Automated

- [x] 4.1 `./mvnw clean compile` passes. — c2ab968
- [x] 4.2 `./mvnw test` passes. — c2ab968

#### Manual

- [x] 4.3 Fresh launch with no `profiles.tsv` requires profile creation before gameplay. — c2ab968
- [x] 4.4 Relaunch after creating profiles shows persisted profiles from `profiles.tsv`. — c2ab968
- [x] 4.5 A better completed score updates the selected profile's best score; equal or lower scores do not. — c2ab968
- [x] 4.6 Malformed `profiles.tsv` logs an error and lets the player create a new profile. — c2ab968
- [x] 4.7 Existing controls still work: arrows move during play, `SPACE` fires, `P` pauses/resumes, `ENTER` restarts on Game Over. — c2ab968
- [x] 4.8 Audio-unavailable behavior remains safe. — c2ab968
