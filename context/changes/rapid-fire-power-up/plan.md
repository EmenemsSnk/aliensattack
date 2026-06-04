# Rapid Fire Power-Up Implementation Plan

## Overview

Deliver roadmap slice S-01: destroyed aliens can drop a collectible rapid-fire power-up, collecting it temporarily accelerates held-space shooting, and the player can see the full effect lifecycle. The change extends the existing shared-list/controller/session/panel architecture without new dependencies or unrelated Replayability features.

## Current State Analysis

The game already supports held-space firing through a fixed 10-tick cooldown, deterministic controller tests through injected `Random`, shared entity lists wired from `Main` into `GameController` and `GamePanel`, scalar lifecycle state in `GameSession`, and passive HUD rendering in `GamePanel`. There is no collectible object type, drop list, power-up collision path, temporary firing modifier, or HUD status for temporary effects.

The baseline is healthy on 2026-06-04: `./mvnw clean compile` and `./mvnw test` pass, with 48 tests.

## Desired End State

Each alien destroyed by a player missile independently has a 12% chance to drop a visible rapid-fire collectible at the alien's last position. The drop falls down the board and disappears when missed. Ship contact removes the drop and activates rapid-fire for 180 playing ticks, reducing the player firing cooldown from 10 ticks to 4 ticks. Collecting another drop while active refreshes the timer to 180 ticks. Losing a life cancels the effect, while advancing a wave preserves it; starting or restarting a session clears the effect and all uncollected drops.

While rapid-fire is active, the HUD displays `RAPID FIRE: Ns` with a player-readable remaining-seconds value. The indicator disappears when the effect expires. Existing controls, scoring, waves, lives, Game Over, restart, audio safety, and projectile behavior continue to work.

### Key Findings:

- The controller's ordered playing-tick pipeline and fixed player cooldown are centralized in `src/main/java/com/emenems/games/aliens/controller/GameController.java:175` and `src/main/java/com/emenems/games/aliens/controller/GameController.java:398`.
- Alien kills are deduplicated before score and sound application in `src/main/java/com/emenems/games/aliens/controller/GameController.java:231`; drop generation must use those resolved kills rather than raw collision attempts.
- Shared rendered collections are constructed once and passed to controller and panel in `src/main/java/com/emenems/games/aliens/Main.java:20`.
- Scalar session lifecycle and reset behavior belong in `src/main/java/com/emenems/games/aliens/GameSession.java:3`.
- `GamePanel.updateGameState(...)` is the established scalar push channel for HUD state in `src/main/java/com/emenems/games/aliens/gui/GamePanel.java:46`.
- Controller tests already inject deterministic `Random` and drive ticks/input directly in `src/test/java/com/emenems/games/aliens/controller/GameControllerTest.java`.

## What We Are NOT Doing

- No score combo, new alien type, additional power-up family, inventory, permanent upgrade, or power-up stacking.
- No cancellation of rapid-fire on wave advancement.
- No change to missile movement, damage, score values, alien speed, alien fire, or existing controls.
- No new image, audio, runtime, or test dependency; the collectible uses Swing drawing primitives.
- No broad refactor of `GameController`, `GameSession`, shared-list wiring, or HUD layout.
- No pixel-perfect GUI automation.

## Implementation Approach

Model the falling collectible as a new `RapidFirePowerUp` `GameObject` held in a shared mutable list, matching the existing projectile/entity wiring. Keep drop spawning, movement, cleanup, collection collision, and firing-cooldown selection in `GameController`, because they are entity orchestration and gameplay sequencing concerns. Keep the active-effect timer in `GameSession`, because it is scalar session lifecycle state that must reset with the session and flow to the panel through the established scalar push channel.

Use the controller's injected `Random` for the 12% drop decision, allowing deterministic tests without new test infrastructure. Generate drops from the resolved `aliensToRemove` set before those aliens are removed, so each actual kill receives exactly one independent chance and duplicate collision attempts cannot create duplicate drops.

## Critical Implementation Details

### Timing and Lifecycle

Tick the rapid-fire duration once per `PLAYING` tick alongside hit feedback, before firing is evaluated. With this ordering, a newly collected power-up becomes usable from the next firing opportunity, survives wave advancement, and does not expire while the game is outside `PLAYING`. Life loss and session reset must clear the timer, while controller reset must clear the drop list.

### State Sequencing

When selecting a cooldown for a fired missile, use the effect state at the moment the missile is created. Activating rapid-fire must not retroactively shorten an already-running normal cooldown; the next successful shot receives the rapid-fire cooldown. This preserves the existing cooldown invariant and avoids special-case mutation on collection.

## Phase 1: Power-Up Model and Session Effect

### Overview

Add the falling collectible model and direct unit coverage for the temporary scalar effect before integrating either into controller orchestration.

### Required Changes:

#### 1. Rapid-Fire Power-Up Model

**Files**:
- `src/main/java/com/emenems/games/aliens/gamemachines/RapidFirePowerUp.java`
- `src/main/java/com/emenems/games/aliens/gamemachines/GameObject.java`
- `src/test/java/com/emenems/games/aliens/gamemachines/RapidFirePowerUpTest.java`

**Purpose**: Represent a collectible that falls down the board and can participate in the existing coordinate/collision conventions.

**Contract**: Add a final `RapidFirePowerUp` implementing `GameObject`, with constructor-provided x/y coordinates, stable x, downward movement at a fixed speed, and standard getters. Extend the sealed `GameObject` permits list. Unit tests lock its movement direction and coordinate behavior.

#### 2. Rapid-Fire Session Lifecycle

**Files**:
- `src/main/java/com/emenems/games/aliens/GameSession.java`
- `src/test/java/com/emenems/games/aliens/GameSessionTest.java`

**Purpose**: Make `GameSession` the scalar owner of rapid-fire activation, remaining duration, expiration, and reset.

**Contract**: Add a 180-tick rapid-fire duration and behavior methods to activate/refresh and tick the effect, plus read methods for active state and remaining ticks. Activation always sets the remaining duration to 180 ticks. Wave advancement leaves it unchanged; life loss and `startOrRestart()` clear it.

### Success Criteria:

#### Automated Verification:

- Project compiles: `./mvnw clean compile`
- Focused model/session tests pass: `./mvnw test -Dtest=RapidFirePowerUpTest,GameSessionTest`
- Unit tests verify the collectible moves downward.
- Unit tests verify activation starts at 180 ticks and expires after 180 playing ticks.
- Unit tests verify repeated activation refreshes the duration to 180 ticks.
- Unit tests verify wave advancement preserves the effect and life loss cancels it.
- Unit tests verify session start/restart clears the effect.

#### Manual Verification:

- Code review confirms `RapidFirePowerUp` contains only position and movement state.
- Code review confirms rapid-fire scalar lifecycle is owned by `GameSession`, with no Swing, random, audio, or entity-list dependency.

**Implementation note**: After automated verification passes, continue to Phase 2; no standalone visible behavior exists yet.

---

## Phase 2: Controller Drop, Collection, and Firing Integration

### Overview

Wire the full gameplay lifecycle through the controller: resolved alien kills can create drops, drops move and clean up, ship contact activates the effect, and firing uses the active cooldown.

### Required Changes:

#### 1. Shared Drop List and Controller Wiring

**Files**:
- `src/main/java/com/emenems/games/aliens/Main.java`
- `src/main/java/com/emenems/games/aliens/controller/GameController.java`
- `src/test/java/com/emenems/games/aliens/controller/GameControllerTest.java`

**Purpose**: Introduce the power-up collection without replacing the established shared-list model.

**Contract**: Construct one `List<RapidFirePowerUp>` in `Main` and pass the same instance to controller and panel. Extend controller constructors and test helpers with this list. Controller reset clears uncollected drops.

#### 2. Drop Generation from Resolved Alien Kills

**Files**:
- `src/main/java/com/emenems/games/aliens/controller/GameController.java`
- `src/test/java/com/emenems/games/aliens/controller/GameControllerTest.java`

**Purpose**: Give each actual player-missile alien kill one independent 12% chance to create a collectible.

**Contract**: During `checkCollisionsWithMissile()`, evaluate the injected `Random` once for each alien in the resolved removal set and spawn a drop at that alien's last coordinates when the roll is below 0.12. A missed roll creates nothing; one alien cannot create multiple drops from overlapping missiles.

#### 3. Drop Movement, Cleanup, and Collection

**Files**:
- `src/main/java/com/emenems/games/aliens/controller/GameController.java`
- `src/test/java/com/emenems/games/aliens/controller/GameControllerTest.java`

**Purpose**: Complete the collectible lifecycle during active play.

**Contract**: Move drops once per playing tick, remove drops after they pass below the board, and add ship/drop collision handling to the collision pipeline. Collection removes one colliding drop and activates or refreshes rapid-fire. Drops and the effect remain through wave advancement; losing a life cancels the active effect without removing uncollected drops.

#### 4. Dynamic Player Fire Cooldown

**Files**:
- `src/main/java/com/emenems/games/aliens/controller/GameController.java`
- `src/test/java/com/emenems/games/aliens/controller/GameControllerTest.java`

**Purpose**: Make the collected effect immediately meaningful while preserving held-space firing semantics.

**Contract**: Keep the normal cooldown at 10 ticks and add a rapid-fire cooldown of 4 ticks. When a missile is successfully created, set the cooldown according to the current session rapid-fire state. Tick the session effect once per playing tick before held firing evaluation. Preserve the current behavior that pressing Space fires immediately when no cooldown is active.

### Success Criteria:

#### Automated Verification:

- Focused controller tests pass: `./mvnw test -Dtest=GameControllerTest`
- Deterministic tests verify a successful 12% roll creates one drop at the killed alien's position.
- Deterministic tests verify a failed roll creates no drop and one resolved kill receives only one roll.
- Tests verify drops move down and missed drops are cleaned up.
- Tests verify ship contact removes the drop and activates rapid-fire.
- Tests verify a second collection refreshes the duration rather than stacking it.
- Tests verify held Space fires at a 4-tick cooldown while active and returns to 10 ticks after expiration.
- Tests verify wave advancement preserves rapid-fire and life loss cancels it.
- Tests verify restart clears active rapid-fire and uncollected drops.

#### Manual Verification:

- Code review confirms drop generation uses resolved alien kills, not raw collision pairs.
- Code review confirms no power-up state is stored on `Spaceship` or `GamePanel`.
- Code review confirms the shared drop list flows by reference from `Main` to controller and panel.

**Implementation note**: Continue to Phase 3 after focused tests pass. Manual gameplay validation belongs to the rendered end-to-end phase.

---

## Phase 3: Rendering, HUD, and Regression Gate

### Overview

Render the collectible and active-effect countdown, then run the full automated and manual regression gates.

### Required Changes:

#### 1. Collectible Rendering

**File**: `src/main/java/com/emenems/games/aliens/gui/GamePanel.java`

**Purpose**: Make falling rapid-fire drops clearly visible and distinguishable from both projectile types.

**Contract**: Accept and retain the shared rapid-fire drop list. Render each drop with Swing drawing primitives in a distinct high-contrast color and compact symbol/shape, within the standard component-sized collision area. Draw drops with active gameplay objects before overlays.

#### 2. Rapid-Fire HUD State

**Files**:
- `src/main/java/com/emenems/games/aliens/controller/GameController.java`
- `src/main/java/com/emenems/games/aliens/gui/GamePanel.java`

**Purpose**: Show the player that rapid-fire is active and when it will end.

**Contract**: Extend `updateGameState(...)` with rapid-fire active/remaining state from `GameSession`. While active, display `RAPID FIRE: Ns` in the HUD using a ceiling-style conversion from remaining ticks and the existing approximately 60 FPS loop, so the label starts at 3 seconds and does not show 0 while still active. Hide the indicator when inactive.

#### 3. Architecture Guidance and Final Verification

**Files**:
- `CLAUDE.md`
- `src/test/java/com/emenems/games/aliens/controller/GameControllerTest.java`

**Purpose**: Preserve the new ownership rules and prove the slice does not regress the shipped arcade loop.

**Contract**: Document the new shared drop-list wiring and `GameSession` ownership of the rapid-fire timer. Complete controller coverage for the end-to-end lifecycle and run the canonical compile/test gate.

### Success Criteria:

#### Automated Verification:

- Project compiles: `./mvnw clean compile`
- Full test suite passes: `./mvnw test`
- Existing 48-test baseline remains green alongside the new power-up tests.
- No new runtime or test dependency is added.

#### Manual Verification:

- Destroying aliens eventually produces visible, distinguishable falling rapid-fire drops.
- Moving the ship into a drop removes it and shows `RAPID FIRE: 3s` in the HUD.
- Holding Space visibly fires faster during the effect and returns to normal after the indicator expires.
- Collecting another drop while active refreshes the HUD countdown to 3 seconds.
- Losing a life cancels rapid-fire; clearing a wave does not.
- Missing a drop lets it fall offscreen without lingering.
- Game Over and Enter-to-restart clear the indicator and all drops.
- Existing movement, scoring, waves, lives, alien fire, hit feedback, audio safety, Game Over, and restart remain usable.

**Implementation note**: Do not mark manual progress complete until a human runs the game with a display and confirms the checklist.

---

## Testing Strategy

### Unit Tests:

- `RapidFirePowerUpTest` covers fixed downward movement and coordinate access.
- `GameSessionTest` covers activation, refresh, expiration, persistence across life/wave changes, and reset.
- `GameControllerTest` covers deterministic drop rolls, resolved-kill semantics, movement/cleanup, collection, dynamic cooldown, timer sequencing, life/wave persistence, and restart cleanup.

### Integration Tests:

- Focused controller/session tests verify the complete mechanic without a Swing window.
- `./mvnw test` remains the project-wide regression gate.
- Rendering and real-time feel are verified manually because the project has no GUI automation harness.

### Manual Testing Steps:

1. Run `./mvnw exec:java -Dexec.mainClass="com.emenems.games.aliens.Main"`.
2. Start gameplay with Enter and destroy aliens until a rapid-fire drop appears.
3. Confirm the drop is visually distinct, falls downward, and disappears if missed.
4. Collect a drop and confirm the HUD starts at `RAPID FIRE: 3s`.
5. Hold Space and compare the active firing cadence with the normal cadence after expiration.
6. Collect another drop while active and confirm the countdown refreshes to 3 seconds.
7. Clear a wave while active and confirm rapid-fire remains active; then lose a life and confirm it is cancelled.
8. Reach Game Over and restart; confirm all drops and active rapid-fire state are cleared.
9. Smoke-test movement, scoring, wave progression, alien projectiles, audio-safe behavior, and Game Over/restart.

## Performance Considerations

The drop list is naturally bounded by six aliens per wave and falling cleanup. Movement, collision, and rendering add small linear passes over tiny lists. The 4-tick cooldown can create more player missiles, but existing offscreen cleanup bounds the list; manual testing should confirm sustained rapid-fire does not visibly affect the EDT-driven loop.

## Migration Notes

No persistent player data or schema exists for this slice. Starting or restarting always clears transient rapid-fire state and drops, so no migration or compatibility path is required.

## References

- Product requirement: `context/foundation/prd.md` → FR-003, FR-010
- Roadmap slice: `context/foundation/roadmap.md` → S-01
- Existing controller pipeline: `src/main/java/com/emenems/games/aliens/controller/GameController.java:175`
- Existing collision deduplication: `src/main/java/com/emenems/games/aliens/controller/GameController.java:231`
- Existing scalar session lifecycle: `src/main/java/com/emenems/games/aliens/GameSession.java:3`
- Existing shared-list wiring: `src/main/java/com/emenems/games/aliens/Main.java:20`
- Existing scalar panel push: `src/main/java/com/emenems/games/aliens/gui/GamePanel.java:46`
- Regression baseline: `context/archive/2026-05-31-lock-refactor-safety-baseline/baseline.md`

## Progress

> Convention: `- [ ]` pending, `- [x]` completed. Add ` — <commit sha>` when a step is implemented. Do not rename step titles.

### Phase 1: Power-Up Model and Session Effect

#### Automated

- [x] 1.1 Project compiles: `./mvnw clean compile`
- [x] 1.2 Focused model/session tests pass: `./mvnw test -Dtest=RapidFirePowerUpTest,GameSessionTest`
- [x] 1.3 Unit tests verify the collectible moves downward
- [x] 1.4 Unit tests verify activation starts at 180 ticks and expires after 180 playing ticks
- [x] 1.5 Unit tests verify repeated activation refreshes the duration to 180 ticks
- [x] 1.6 Unit tests verify wave advancement preserves the effect and life loss cancels it
- [x] 1.7 Unit tests verify session start/restart clears the effect

#### Manual

- [x] 1.8 Code review confirms `RapidFirePowerUp` contains only position and movement state
- [x] 1.9 Code review confirms rapid-fire scalar lifecycle is owned by `GameSession`, with no Swing, random, audio, or entity-list dependency

### Phase 2: Controller Drop, Collection, and Firing Integration

#### Automated

- [x] 2.1 Focused controller tests pass: `./mvnw test -Dtest=GameControllerTest`
- [x] 2.2 Deterministic tests verify a successful 12% roll creates one drop at the killed alien's position
- [x] 2.3 Deterministic tests verify a failed roll creates no drop and one resolved kill receives only one roll
- [x] 2.4 Tests verify drops move down and missed drops are cleaned up
- [x] 2.5 Tests verify ship contact removes the drop and activates rapid-fire
- [x] 2.6 Tests verify a second collection refreshes the duration rather than stacking it
- [x] 2.7 Tests verify held Space fires at a 4-tick cooldown while active and returns to 10 ticks after expiration
- [x] 2.8 Tests verify wave advancement preserves rapid-fire and life loss cancels it
- [x] 2.9 Tests verify restart clears active rapid-fire and uncollected drops

#### Manual

- [x] 2.10 Code review confirms drop generation uses resolved alien kills, not raw collision pairs
- [x] 2.11 Code review confirms no power-up state is stored on `Spaceship` or `GamePanel`
- [x] 2.12 Code review confirms the shared drop list flows by reference from `Main` to controller and panel

### Phase 3: Rendering, HUD, and Regression Gate

#### Automated

- [x] 3.1 Project compiles: `./mvnw clean compile`
- [x] 3.2 Full test suite passes: `./mvnw test`
- [x] 3.3 Existing 48-test baseline remains green alongside the new power-up tests
- [x] 3.4 No new runtime or test dependency is added

#### Manual

- [x] 3.5 Destroying aliens eventually produces visible, distinguishable falling rapid-fire drops
- [x] 3.6 Moving the ship into a drop removes it and shows `RAPID FIRE: 3s` in the HUD
- [x] 3.7 Holding Space visibly fires faster during the effect and returns to normal after the indicator expires
- [x] 3.8 Collecting another drop while active refreshes the HUD countdown to 3 seconds
- [x] 3.9 Losing a life cancels rapid-fire; clearing a wave does not
- [x] 3.10 Missing a drop lets it fall offscreen without lingering
- [x] 3.11 Game Over and Enter-to-restart clear the indicator and all drops
- [x] 3.12 Existing movement, scoring, waves, lives, alien fire, hit feedback, audio safety, Game Over, and restart remain usable
