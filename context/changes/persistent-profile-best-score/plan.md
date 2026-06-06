# Persistent Profile Best Score and Top 5 Ranking Implementation Plan

## Overview

Complete the `persistent-profile-best-score` roadmap slice by reconciling the already-implemented per-profile best-score flow and adding a compact Top 5 local profile ranking on the Game Over screen. The ranking uses the existing `profiles.tsv` data model: one profile, one stored best score.

## Current State Analysis

The best-score persistence work is already present in the branch. `ProfileStore` persists `PlayerProfile(name, bestScore)` rows in `profiles.tsv`, `GameController` loads/selects profiles and updates the selected profile once per Game Over transition, and `GamePanel` already renders the selected profile's best score and new-best/save-failure messages.

The remaining product gap is visibility across profiles. A player can see the selected profile's best score, but cannot see how that score compares with other local profiles after a game ends. The roadmap also still marks S-09 as blocked even though the update rule has now been decided and implemented.

## Desired End State

After Game Over, the screen shows the final score, selected profile result, and a compact `TOP 5` ranking derived from all loaded local profiles sorted by `bestScore` descending, then profile name ascending for ties. If the just-finished score becomes a new best, the ranking reflects that in-memory update immediately; if saving fails, the existing non-blocking warning remains visible.

The roadmap no longer presents S-09 as blocked by an unresolved best-score update rule. The plan remains compatible with the current local profile format and does not introduce session history or a separate leaderboard file.

### Key Findings:

- `ProfileStore` already stores each profile's best score in `profiles.tsv` using JDK file APIs only: `src/main/java/com/emenems/games/aliens/profiles/ProfileStore.java:14`.
- `GameController.getProfileMenuState()` is the existing controller-to-panel profile state boundary: `src/main/java/com/emenems/games/aliens/controller/GameController.java:528`.
- Game Over best-score persistence already updates only when `session.getScore() > selectedProfile.bestScore()` and is guarded by `gameOverScoreHandled`: `src/main/java/com/emenems/games/aliens/controller/GameController.java:861`.
- `GamePanel.drawGameOver(...)` has the current result layout and is the right place to render a compact ranking without adding a new state: `src/main/java/com/emenems/games/aliens/gui/GamePanel.java:270`.
- Existing tests already cover per-profile best score update, no update on tie/lower score, save failure, profile display formatting, and storage round-trip behavior.
- `context/foundation/roadmap.md:180` still marks S-09 as blocked by an update-rule question that has now been answered.

## What We Are NOT Doing

- No session-history leaderboard. The ranking is Top 5 profiles by stored best score, not Top 5 individual game runs.
- No new storage format, migration, JSON parser, database, cloud sync, accounts, passwords, or global leaderboard.
- No separate leaderboard screen or menu navigation state.
- No profile deletion, rename, export, or score reset.
- No changes to scoring rules, combo rules, waves, lives, or game difficulty.
- No controller/panel architecture rewrite.

## Implementation Approach

Keep profile persistence as-is and add a small immutable ranking view derived from the controller's in-memory `profiles` list. Extend `ProfileMenuState` to carry up to five leaderboard rows into `GamePanel`, because the panel should remain passive and should not sort profiles or read storage.

The ranking should be recomputed whenever `getProfileMenuState()` is built or through an equivalent controller helper. Sort by `bestScore` descending and then `name` ascending so ties are deterministic and easy to test. `handleGameOverScoreUpdate()` already updates the selected profile in memory before pushing panel state, so the same source list can drive both the selected best score and ranking.

## Phase 1: Ranking State and Controller Derivation

### Overview

Add the data contract for Top 5 ranking rows and derive the ranking from existing in-memory profiles without changing persistence.

### Required Changes:

#### 1. Profile menu state ranking contract

**File**: `src/main/java/com/emenems/games/aliens/profiles/ProfileMenuState.java`

**Purpose**: Carry the Top 5 ranking to the panel through the existing passive profile UI state object.

**Contract**: Add an immutable leaderboard field, such as `List<LeaderboardEntry> topProfiles`, where each row exposes `rank`, `name`, and `bestScore`. Provide a compact nested record or package-level record if that better matches local style. `ProfileMenuState.empty()` must return an empty ranking list. Defensive copying is required so callers cannot mutate panel state through the list.

#### 2. Controller ranking derivation

**File**: `src/main/java/com/emenems/games/aliens/controller/GameController.java`

**Purpose**: Keep ranking derivation with the controller-owned profile list and avoid putting sorting logic in Swing rendering.

**Contract**: Extend `getProfileMenuState()` to include the Top 5 profiles sorted by `bestScore` descending and `name` ascending. The selected profile's newly improved score must appear in this ranking immediately after `handleGameOverScoreUpdate()` mutates `profiles`.

#### 3. Controller tests for ranking behavior

**File**: `src/test/java/com/emenems/games/aliens/controller/GameControllerTest.java`

**Purpose**: Lock ranking sort order, limit, tie behavior, and post-Game Over refresh.

**Contract**: Add tests that verify only five rows are exposed, rows sort by score descending then name ascending, and a new best score can move the selected profile into the Top 5 after Game Over. Existing best-score tests should continue to prove one-time save and no update on equal/lower scores.

### Success Criteria:

#### Automated Verification:

- `GameControllerTest` covers Top 5 ordering, tie ordering, list limit, and refresh after a new best score.
- `./mvnw test -Dtest=GameControllerTest` passes.
- `./mvnw clean compile` passes.

#### Manual Verification:

- With more than five profiles in `profiles.tsv`, only the five highest best scores are shown after Game Over.
- Equal scores appear in deterministic alphabetical order.

**Implementation Note**: Continue directly to Phase 2 after the controller tests pass; the manual UI check happens after rendering is wired.

---

## Phase 2: Game Over Rendering and Documentation

### Overview

Render the ranking on the Game Over screen and document that local profiles now form a local Top 5 ranking.

### Required Changes:

#### 1. Game Over Top 5 UI

**File**: `src/main/java/com/emenems/games/aliens/gui/GamePanel.java`

**Purpose**: Make the local ranking visible at the moment it matters, without adding a new screen.

**Contract**: In `drawGameOver(...)`, render a compact `TOP 5` section below the selected profile/new-best area and above or around the restart instruction. Each row should fit the fixed `760x650` panel and use a short format such as `1. Player  120`. Preserve visibility of `New Best Score!`, save-failure warning, and `Press ENTER to Restart`.

#### 2. Panel helper tests

**File**: `src/test/java/com/emenems/games/aliens/gui/GamePanelTest.java`

**Purpose**: Keep rendering-adjacent ranking behavior testable without brittle screenshot tests.

**Contract**: Add helper coverage for ranking visibility/formatting if helpers are introduced. Avoid pixel-position assertions; verify pure formatting or predicate behavior only.

#### 3. README update

**File**: `README.md`

**Purpose**: Tell players that local profiles keep best scores and the Game Over screen displays a Top 5 ranking.

**Contract**: Add a short note near the existing `profiles.tsv` explanation. Keep it accurate: this is a local profile ranking, not an online/global leaderboard.

### Success Criteria:

#### Automated Verification:

- `GamePanelTest` covers any new ranking helper behavior.
- `./mvnw test -Dtest=GamePanelTest` passes.
- `./mvnw test` passes.
- `./mvnw clean compile` passes.

#### Manual Verification:

- Game Over shows final score, selected profile, best score, Top 5 ranking, and restart instruction without text overlap.
- A new best score immediately appears in the ranking after Game Over.
- Save-failure messaging remains visible and does not prevent restart.

**Implementation Note**: If the existing Game Over layout becomes crowded, reduce font sizes and vertical spacing before considering a separate screen. A new state is out of scope for this plan.

---

## Phase 3: Roadmap Reconciliation and Final Verification

### Overview

Align foundation documentation with the actual S-09 decision and run the full verification loop.

### Required Changes:

#### 1. Roadmap S-09 status

**File**: `context/foundation/roadmap.md`

**Purpose**: Remove stale blocker language so future planning agents do not re-plan the same best-score decision.

**Contract**: Update S-09 to reflect the decided rule: a completed session replaces the selected profile's stored best score only when `finalScore > bestScore`. Change readiness/status language from blocked to the appropriate post-implementation state after code verification. If this change is not archived yet, avoid claiming archival completion in advance.

#### 2. Full automated verification

**Files**: Production and test suite

**Purpose**: Verify that ranking changes did not regress existing arcade, profile, and persistence behavior.

**Contract**: Run the complete test suite and compile gate from the repository root.

#### 3. Manual desktop smoke test

**Files**: Running application via Maven

**Purpose**: Confirm the real Swing screen and local `profiles.tsv` persistence behave together.

**Contract**: Launch with `./mvnw exec:java -Dexec.mainClass="com.emenems.games.aliens.Main"`. Use a local `profiles.tsv` containing at least six profiles with varied and tied scores. Finish a session that creates a new best score and confirm the ranking updates immediately; relaunch and confirm persisted order.

### Success Criteria:

#### Automated Verification:

- `./mvnw test` passes.
- `./mvnw clean compile` passes.
- `context/foundation/roadmap.md` no longer marks S-09 as blocked by the resolved update-rule question.

#### Manual Verification:

- Top 5 ranking appears on Game Over and fits the fixed panel.
- Ranking uses stored profile best scores, not individual session history.
- New best score updates the selected profile and ranking immediately.
- Relaunch preserves the same Top 5 from `profiles.tsv`.
- Existing controls still work: arrows move during play, `SPACE` fires, `P` pauses/resumes, `ENTER` restarts on Game Over.

**Implementation Note**: Do not archive until the manual desktop smoke test has been completed.

---

## Testing Strategy

### Unit Tests:

- Profile ranking derivation: score descending, name ascending for ties, maximum five rows.
- Controller Game Over path: new selected best score can move into the ranking and save is still one-time.
- Panel helper behavior for any ranking formatting/predicates.
- Existing profile validation and storage tests remain unchanged unless a helper type requires test updates.

### Integration Tests:

- Controller with injected `ProfileStore` and deterministic profile lists for ranking.
- Existing controller tests for gameplay input, pause, restart, scoring, waves, power-ups, combo, special aliens, and best-score persistence must continue to pass.

### Manual Testing Steps:

1. Prepare `profiles.tsv` with at least six valid profiles and at least one tied score.
2. Launch the game and select a profile that can enter or move within the Top 5 after a better score.
3. Finish a session with a new best score and confirm the Game Over Top 5 updates immediately.
4. Relaunch and confirm the same ranking persists from `profiles.tsv`.
5. Finish an equal or lower score and confirm the ranking does not change.
6. Confirm Game Over still shows restart instructions, selected profile result, new-best message when applicable, and save-failure warning when applicable.

## Performance Considerations

The profile list is small and already held in memory. Sorting on profile-state creation is acceptable for local profile counts, but the implementation should sort only the in-memory list and avoid file I/O during rendering or per-frame panel code.

## Migration Notes

No file migration is required. The ranking is derived from existing `profiles.tsv` rows. Rollback consists of removing the ranking field/type, controller derivation, Game Over rendering, README note, tests, and roadmap wording changes; existing profile files remain valid.

## References

- Current profile storage: `src/main/java/com/emenems/games/aliens/profiles/ProfileStore.java:14`
- Profile state push boundary: `src/main/java/com/emenems/games/aliens/controller/GameController.java:528`
- Panel state push: `src/main/java/com/emenems/games/aliens/controller/GameController.java:609`
- Best-score update guard and rule: `src/main/java/com/emenems/games/aliens/controller/GameController.java:861`
- Current Game Over layout: `src/main/java/com/emenems/games/aliens/gui/GamePanel.java:270`
- Profile UI tests: `src/test/java/com/emenems/games/aliens/gui/GamePanelTest.java:67`
- S-09 stale roadmap status: `context/foundation/roadmap.md:180`

## Progress

> Convention: `- [ ]` pending, `- [x]` done. Append ` — <commit sha>` when a step lands. Do not rename step titles. See `references/progress-format.md`.

### Phase 1: Ranking State and Controller Derivation

#### Automated

- [x] 1.1 `GameControllerTest` covers Top 5 ordering, tie ordering, list limit, and refresh after a new best score.
- [x] 1.2 `./mvnw test -Dtest=GameControllerTest` passes.
- [x] 1.3 `./mvnw clean compile` passes.

#### Manual

- [x] 1.4 With more than five profiles in `profiles.tsv`, only the five highest best scores are shown after Game Over.
- [x] 1.5 Equal scores appear in deterministic alphabetical order.

### Phase 2: Game Over Rendering and Documentation

#### Automated

- [x] 2.1 `GamePanelTest` covers any new ranking helper behavior.
- [x] 2.2 `./mvnw test -Dtest=GamePanelTest` passes.
- [x] 2.3 `./mvnw test` passes.
- [x] 2.4 `./mvnw clean compile` passes.

#### Manual

- [x] 2.5 Game Over shows final score, selected profile, best score, Top 5 ranking, and restart instruction without text overlap.
- [x] 2.6 A new best score immediately appears in the ranking after Game Over.
- [x] 2.7 Save-failure messaging remains visible and does not prevent restart.

### Phase 3: Roadmap Reconciliation and Final Verification

#### Automated

- [x] 3.1 `./mvnw test` passes.
- [x] 3.2 `./mvnw clean compile` passes.
- [x] 3.3 `context/foundation/roadmap.md` no longer marks S-09 as blocked by the resolved update-rule question.

#### Manual

- [x] 3.4 Top 5 ranking appears on Game Over and fits the fixed panel.
- [x] 3.5 Ranking uses stored profile best scores, not individual session history.
- [x] 3.6 New best score updates the selected profile and ranking immediately.
- [x] 3.7 Relaunch preserves the same Top 5 from `profiles.tsv`.
- [x] 3.8 Existing controls still work: arrows move during play, `SPACE` fires, `P` pauses/resumes, `ENTER` restarts on Game Over.
