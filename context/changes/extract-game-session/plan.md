# Extract Game Session Implementation Plan

## Overview

Extract the scalar arcade-session state from `GameController` into a focused `GameSession` model without changing visible gameplay. This is roadmap slice S-03: score, wave, lives, game state, reset, hit feedback, and game-over title become session-owned, while the controller remains the orchestrator for input, entity lists, collisions, spawning, audio, timer, and panel updates.

## Current State Analysis

`GameController` currently owns both orchestration and scalar session state. It stores `score`, `wave`, `lives`, `gameState`, `hitFeedbackTicks`, and `gameOverTitle` directly, mutates them from collision/reset/tick paths, and pushes their current values into `GamePanel` each tick. The entity collections are still shared by reference between `Main`, `GamePanel`, and `GameController`, and that shared-list model is intentionally preserved.

S-01 locked the refactor safety baseline, and S-02 already extracted pure scoring and wave-speed formulas into `GameRules`. This plan should build on those boundaries rather than move rule formulas again.

## Desired End State

`GameSession` is the single owner of scalar session state and exposes small domain operations for reset/start, scoring alien kills, advancing waves, losing lives, entering Game Over, and ticking hit feedback. `GameController` delegates scalar state changes to `GameSession`, continues to mutate the shared entity lists, and still pushes the same values to `GamePanel`. Existing controller behavior remains green, and new session tests cover the extracted state transitions directly.

### Key Findings:

- Roadmap S-03 scopes this slice to `GameSession` covering score, wave, lives, game state, and reset while preserving gameplay: `context/foundation/roadmap.md`.
- `GameController` currently owns scalar session fields directly: `src/main/java/com/emenems/games/aliens/controller/GameController.java:48`.
- The tick loop gates all gameplay work on `gameState` and then pushes scalar state into the panel: `src/main/java/com/emenems/games/aliens/controller/GameController.java:183`.
- Reset currently mixes scalar reset, projectile cleanup, input cleanup, wave spawn, and panel update in one controller method: `src/main/java/com/emenems/games/aliens/controller/GameController.java:365`.
- `Main` wires shared mutable lists into both controller and panel; this plan must not replace that model: `src/main/java/com/emenems/games/aliens/Main.java:20`.
- Existing regression tests already cover reset, held input, wave advancement, scoring, lives, Game Over, and restart behavior through controller seams: `src/test/java/com/emenems/games/aliens/controller/GameControllerTest.java:386`.
- `GameRules` now owns score-per-alien and alien speed formulas; `GameSession` should call `GameRules.alienScoreForWave(wave)` rather than duplicating score math: `src/main/java/com/emenems/games/aliens/GameRules.java:9`.

## What We Are NOT Doing

- No changes to player-visible gameplay balance, scoring values, wave-speed values, lives count, hit-feedback duration, firing cooldown, alien spawn count, or Game Over titles.
- No movement of entity lists into `GameSession`; `spaceship`, `missiles`, `alienMissiles`, and `aliens` stay controller/panel shared references.
- No Swing rendering rewrite, panel state pull model, observer pattern, or background game loop.
- No new runtime or test dependencies.
- No Replayability Pack features such as power-ups, new alien types, high scores, persistence, or release packaging.
- No broad cleanup of unrelated controller responsibilities such as collision geometry, alien fire, movement, or audio.

## Implementation Approach

Use a small mutable model object instead of a static utility. `GameSession` should live in the root `com.emenems.games.aliens` package next to `GameRules`, because it is domain state rather than controller infrastructure. It should own only scalar session values and expose behavior-named methods. The controller should keep all list mutation and side effects, then read session values for tick gating, collision decisions, spawning, and panel updates.

Recommended assumptions applied:

- `GameSession` is a `public final` class with package-simple mutable state and no dependency on Swing, controller, panel, audio, random, or entity lists.
- `GameSession` owns `DEFAULT_LIVES`, `HIT_FEEDBACK_TICKS`, `DEFAULT_GAME_OVER_TITLE`, and `ALIENS_WIN_TITLE` as session constants because those constants describe scalar session lifecycle.
- `GameController` keeps `PLAYER_FIRE_COOLDOWN_TICKS`, alien spawning constants, alien missile constants, and input key state because those remain controller/orchestration concerns.
- Reset is split deliberately: `GameSession.startOrRestart()` resets scalar state; `GameController.resetSession()` clears projectiles/input, regenerates aliens, and pushes panel state.
- Controller package-private getters should remain for tests, but delegate to `GameSession`; do not expose the session object just to satisfy tests.

## Critical Implementation Details

The main sequencing risk is reset. `GameSession.startOrRestart()` must run before `generateSpaceObjects()` so generated alien speed reads wave 1, and `GameController.resetSession()` must still clear projectiles, held movement, held space, and fire cooldown before the first post-restart tick. Do not move shared entity lists into `GameSession`; doing so would break the current panel/controller shared-reference architecture.

## Phase 1: Extract Scalar Session Model

### Overview

Create `GameSession` with direct unit tests for the scalar lifecycle currently embedded in `GameController`.

### Required Changes:

#### 1. Game Session Class

**File**: `src/main/java/com/emenems/games/aliens/GameSession.java`

**Purpose**: Provide a focused owner for score, wave, lives, game state, hit-feedback state, and game-over title.

**Contract**: Add a `public final class GameSession` with default construction in `START_MENU`, score `0`, wave `1`, lives `3`, inactive hit feedback, and default title `"GAME OVER"`. Expose read methods for score, wave, lives, state, hit-feedback active, and game-over title. Expose behavior methods for starting/restarting, adding alien kills, advancing wave, losing a life, entering alien-invasion Game Over, and ticking hit feedback.

#### 2. Session Unit Tests

**File**: `src/test/java/com/emenems/games/aliens/GameSessionTest.java`

**Purpose**: Lock the extracted scalar behavior directly before rewiring the controller.

**Contract**: Cover initial state, start/restart reset values, score accumulation using the current wave rule, wave advancement, life loss with temporary hit feedback, third life loss entering default Game Over, alien-invasion Game Over title, and restart clearing hit feedback/title while restoring score/wave/lives.

### Success Criteria:

#### Automated Verification:

- `./mvnw clean compile` passes.
- `./mvnw test -Dtest=GameSessionTest` passes.

#### Manual Verification:

- Code review confirms `GameSession` owns only scalar session state and does not reference Swing, controller, audio, random, or entity lists.

**Implementation Note**: Continue to Phase 2 after this phase unless the extracted session tests reveal a behavior mismatch with the current controller tests.

---

## Phase 2: Wire Controller Through GameSession

### Overview

Replace controller-owned scalar fields and scalar lifecycle logic with a `GameSession` instance while preserving controller behavior and test seams.

### Required Changes:

#### 1. Controller Session Delegation

**File**: `src/main/java/com/emenems/games/aliens/controller/GameController.java`

**Purpose**: Keep `GameController` focused on orchestration and side effects while delegating scalar session lifecycle to `GameSession`.

**Contract**: Add a `private final GameSession session` initialized by the existing constructors. Remove controller fields for `score`, `wave`, `lives`, `gameState`, `hitFeedbackTicks`, and `gameOverTitle`. Replace state checks, scoring, wave advancement, life loss, Game Over entry, hit-feedback ticking, and panel updates with calls to `session`. Keep projectile cleanup, input cleanup, fire cooldown reset, alien generation, audio, timer, and repaint behavior in the controller.

#### 2. Controller Getters and Tick Flow

**File**: `src/main/java/com/emenems/games/aliens/controller/GameController.java`

**Purpose**: Preserve existing controller tests and behavior while hiding the session object.

**Contract**: Keep package-private `getScore()`, `getWave()`, `getLives()`, `getGameState()`, `isHitFeedbackActive()`, and `getGameOverTitle()` methods, but delegate them to `session`. Ensure `tick()` still does no entity movement outside `PLAYING`, still updates hit feedback before collisions while playing, still bails immediately after Game Over transitions, and still pushes the same scalar values to `GamePanel.updateGameState(...)`.

#### 3. Controller Test Alignment

**File**: `src/test/java/com/emenems/games/aliens/controller/GameControllerTest.java`

**Purpose**: Keep behavior-level coverage around controller integration with the extracted session.

**Contract**: Update only tests that fail because internals moved. Preserve behavior assertions for start menu gating, restart state, held-space reset, spaceship coordinate preservation, projectile cleanup, score on current wave, wave advancement, lives, hit feedback, alien-invasion title, and Game Over/restart.

### Success Criteria:

#### Automated Verification:

- `./mvnw clean compile` passes.
- `./mvnw test -Dtest=GameSessionTest,GameControllerTest` passes.

#### Manual Verification:

- Code review confirms `GameController` no longer owns scalar session fields directly.
- Code review confirms shared entity lists still flow by reference from `Main` into both `GamePanel` and `GameController`.
- Code review confirms reset remains one clear controller boundary: scalar reset in `GameSession`, collection/input/spawn reset in `GameController`.

**Implementation Note**: If the controller becomes harder to read after delegation, prefer small private adapter methods such as `isPlaying()` or `currentWave()` over exposing the session object broadly.

---

## Phase 3: Documentation and Regression Gate

### Overview

Update agent guidance and run the full refactor safety gate from S-01.

### Required Changes:

#### 1. Agent Architecture Guidance

**File**: `CLAUDE.md`

**Purpose**: Prevent future work from treating `GameController` as the owner of scalar session state after this extraction.

**Contract**: Update the architecture section so it states that `GameSession` owns score, wave, lives, game state, hit feedback, game-over title, and scalar reset lifecycle. Keep the existing guidance that `GameController` owns orchestration, input, collisions, shared entity list mutation, audio, timer, and panel updates. Keep the `GameRules` guidance intact.

#### 2. Final Verification

**File**: no source file; command verification only

**Purpose**: Confirm this refactor preserved the locked baseline and did not change visible gameplay behavior.

**Contract**: Run `./mvnw clean compile` and `./mvnw test` after implementation. Hand off or run the S-01 manual smoke checklist because this slice affects start, restart, score, wave, lives, Game Over, and panel HUD state.

### Success Criteria:

#### Automated Verification:

- `./mvnw clean compile` passes.
- `./mvnw test` passes.

#### Manual Verification:

- Run or hand off the S-01 smoke checklist.
- Confirm gameplay is visibly unchanged for start, movement, firing, scoring, wave progression, life loss, Game Over, alien-invasion Game Over, and restart.
- Confirm `CLAUDE.md` accurately describes `GameSession`, `GameRules`, and `GameController` ownership after the change.

---

## Testing Strategy

### Unit Tests:

- `GameSessionTest` covers scalar state transitions independently from controller and Swing.
- Existing `GameRulesTest` continues to cover scoring and alien speed formulas.
- Existing `GameControllerTest` remains the behavior-level integration suite for tick gating, score application, wave advancement, reset, held input, lives, hit feedback, alien invasion, projectile cleanup, and restart.

### Integration Tests:

- `./mvnw test -Dtest=GameSessionTest,GameControllerTest` is the focused integration gate after rewiring.
- Full `./mvnw test` remains the final project-wide gate.

### Manual Test Steps:

1. Run `./mvnw exec:java -Dexec.mainClass="com.emenems.games.aliens.Main"`.
2. Confirm the Swing window opens to the start menu.
3. Press Enter and confirm gameplay starts with score 0, wave 1, and 3 lives.
4. Move with arrow keys and confirm the ship remains within the board.
5. Hold Space and confirm repeated firing with cooldown.
6. Destroy aliens and confirm score increases according to the current wave.
7. Clear a wave and confirm the next wave spawns with the HUD wave incremented.
8. Lose a life and confirm the red hit feedback still appears.
9. Lose all lives and confirm `"GAME OVER"` appears with final score.
10. Let an alien reach the bottom and confirm `"ALIENS WIN"` appears.
11. Press Enter on Game Over and confirm score, wave, lives, projectiles, held input, and Game Over title reset for a fresh session.

## Performance Considerations

No meaningful runtime impact is expected. `GameSession` methods are constant-time scalar operations that replace direct field writes in the controller.

## Migration Notes

No player data, save files, external integrations, or deployment artifacts exist. This is an internal refactor in a local desktop game.

## References

- Roadmap S-03: `context/foundation/roadmap.md`
- PRD FR-001, FR-003, FR-004: `context/foundation/prd.md`
- Safety baseline: `context/archive/2026-05-31-lock-refactor-safety-baseline/baseline.md`
- Prior rule extraction: `context/archive/2026-05-31-extract-game-rules/plan.md`
- Current scalar controller fields: `src/main/java/com/emenems/games/aliens/controller/GameController.java:48`
- Current reset boundary: `src/main/java/com/emenems/games/aliens/controller/GameController.java:365`
- Current shared-list wiring: `src/main/java/com/emenems/games/aliens/Main.java:20`
- Current controller regression tests: `src/test/java/com/emenems/games/aliens/controller/GameControllerTest.java:386`

## Progress

> Convention: `- [ ]` pending, `- [x]` done. Add ` — <commit sha>` when a step is completed. Do not change step titles.

### Phase 1: Extract Scalar Session Model

#### Automated

- [x] 1.1 `./mvnw clean compile` passes.
- [x] 1.2 `./mvnw test -Dtest=GameSessionTest` passes.

#### Manual

- [x] 1.3 Code review confirms `GameSession` owns only scalar session state and no Swing/controller/audio/random/entity-list concerns.

### Phase 2: Wire Controller Through GameSession

#### Automated

- [ ] 2.1 `./mvnw clean compile` passes.
- [ ] 2.2 `./mvnw test -Dtest=GameSessionTest,GameControllerTest` passes.

#### Manual

- [ ] 2.3 Code review confirms `GameController` no longer owns scalar session fields directly.
- [ ] 2.4 Code review confirms shared entity lists still flow by reference from `Main` into both `GamePanel` and `GameController`.
- [ ] 2.5 Code review confirms reset remains one clear boundary split between scalar session reset and controller collection/input/spawn reset.

### Phase 3: Documentation and Regression Gate

#### Automated

- [ ] 3.1 `./mvnw clean compile` passes.
- [ ] 3.2 `./mvnw test` passes.

#### Manual

- [ ] 3.3 Run or hand off the S-01 smoke checklist.
- [ ] 3.4 Confirm gameplay is visibly unchanged for start, movement, firing, scoring, wave progression, life loss, Game Over, alien-invasion Game Over, and restart.
- [ ] 3.5 Confirm `CLAUDE.md` accurately documents `GameSession`, `GameRules`, and `GameController` ownership.
