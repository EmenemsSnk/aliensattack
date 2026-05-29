# Smooth Playable Loop — Implementation Plan

## Overview

Deliver roadmap slice S-01: replace the prototype's 1 FPS background-thread loop with an EDT-safe Swing timer, keep keyboard controls responsive without duplicate actions, remove off-screen objects, and make missile-alien collisions resolve exactly once. This deliberately stops before score, waves, lives, Game Over, and restart; those are planned as later slices S-02 and S-03.

## Current State Analysis

The game already has the right coarse shape: `Main` creates one `Spaceship`, one `List<Missile>`, and one `List<Alien>`, then passes the same mutable references to `GamePanel` for rendering and `GameController` for mutation. That shared-state wiring is the core model for this change.

The current loop and collision implementation make the prototype unplayable:
- `GameController.initialize()` starts a background `Thread` before generating aliens, while the unused `Timer` field is never initialized or started (`src/main/java/com/emenems/games/aliens/controller/GameController.java:23`, `src/main/java/com/emenems/games/aliens/controller/GameController.java:32`).
- `GameController.run()` sleeps for 1000 ms, mutates game state off the Swing EDT, and repaints from that background thread (`src/main/java/com/emenems/games/aliens/controller/GameController.java:141`).
- `actionPerformed()` already contains the intended per-tick movement/collision/repaint shape, but no timer calls it (`src/main/java/com/emenems/games/aliens/controller/GameController.java:79`).
- `keyTyped()` and `keyPressed()` both call `makeAction()`, so one player action can be handled twice (`src/main/java/com/emenems/games/aliens/controller/GameController.java:37`).
- Missile collisions remove missiles during stream evaluation, then run a second collision-removal loop afterward (`src/main/java/com/emenems/games/aliens/controller/GameController.java:107`).
- Off-screen cleanup exists only as a commented TODO and uses the wrong shape for this requirement (`src/main/java/com/emenems/games/aliens/controller/GameController.java:136`).

The baseline verification path is healthy: `./mvnw test` passes 5 JUnit tests. No new runtime dependencies are allowed.

## Desired End State

After this plan, launching the game starts immediate PLAYING behavior with a smooth timer-driven loop. Arrow keys move the ship once per key event, Space fires one missile per press, missiles and aliens advance every timer tick on the EDT, missile-alien collisions remove exactly one resolved pair per collision event, and off-screen missiles/aliens are removed so lists do not grow indefinitely.

Verification combines automated tests for pure/controller logic with manual play testing: compile and JUnit must pass, and a human should observe responsive movement, firing, collision removal, and no visible 1 FPS stutter.

### Key Findings:

- `javax.swing.Timer` is already imported and `GameController` already implements `ActionListener`; the replacement path is local to the controller.
- The rendering contract depends on mutating the existing shared `missiles` and `aliens` list instances, not replacing them with new list objects.
- `GamePanel` has unused imports from old experimentation, but rendering itself is straightforward and should not need behavioral changes.
- `Alien.move()` and `Missile.move()` both move 5 px per tick; at 16 ms this can feel fast, but S-01 accepts gameplay-feel calibration only within this slice and defers wave/difficulty logic to S-02.

## What We Are NOT Doing

- No score, HUD, wave progression, speed cap, or scoring formula; those belong to S-02.
- No lives, spaceship-hit consequences, Game Over screen, or Space-to-restart; those belong to S-03.
- No Start Menu.
- No View/Controller rewrite and no separate model object.
- No new runtime libraries, game engines, audio, serialization, or mocking libraries.
- No hard FPS benchmarking; success is compile/test plus manual smoothness observation.

## Implementation Approach

Keep the architecture intact and make the minimum controller-centered change that turns the prototype into a smooth loop. First expose the tick and collision behavior in a testable way without changing runtime behavior. Then replace the background `Runnable` loop with a `javax.swing.Timer` using a 16 ms delay and clean key handling. Finally add cleanup and collision tests, then verify manually in the running Swing game.

## Critical Implementation Details

The timer callback must be the only autonomous game-loop path. Do not leave `Runnable`, `Thread`, or `Thread.sleep()` in `GameController`, because that would keep mutating Swing-rendered state off the EDT. When cleaning lists, mutate the existing list instances in place; `GamePanel` renders the same list objects created in `Main`.

---

## Phase 1: Controller Loop and Input

### Overview

Move the game loop onto the Swing EDT and remove duplicate keyboard handling while preserving the existing shared-state architecture.

### Required Changes:

#### 1. Timer-backed loop

**File**: `src/main/java/com/emenems/games/aliens/controller/GameController.java`

**Purpose**: Replace the 1 FPS background thread with a Swing timer that drives the existing movement/collision/repaint sequence at roughly 60 FPS.

**Contract**: `GameController` no longer implements `Runnable`, no longer owns or starts a `Thread`, and contains no `Thread.sleep()` loop. `initialize()` generates aliens, registers input, creates `new Timer(16, this)`, and starts it. `actionPerformed()` remains the single per-frame entrypoint and runs movement, collision, cleanup, and repaint on the EDT.

#### 2. Single-path keyboard input

**File**: `src/main/java/com/emenems/games/aliens/controller/GameController.java`

**Purpose**: Prevent one physical action from firing twice and keep controls compatible with the existing simple movement model.

**Contract**: `KeyAdapter` handles gameplay actions from `keyPressed()` only. `keyTyped()` must not call `makeAction()`. Space creates one missile from the ship's current coordinates per key-press event; arrow keys apply the existing `Spaceship` movement methods.

### Success Criteria:

#### Automated Verification:

- Project compiles: `./mvnw clean compile`
- Existing tests pass: `./mvnw test`
- Static check by inspection: `GameController` has no `implements Runnable`, no `Thread` field, and no `Thread.sleep`

#### Manual Verification:

- Game launches with `./mvnw exec:java -Dexec.mainClass="com.emenems.games.aliens.Main"`
- Aliens and missiles move smoothly rather than once per second
- One Space press creates one visible missile
- Arrow-key movement remains responsive and does not visibly double-step from one event

**Implementation note**: After this phase and the automated checks pass, stop for human confirmation that the loop feels smooth before proceeding.

---

## Phase 2: Object Cleanup and Collision Semantics

### Overview

Make the per-frame update stable over time: remove off-screen objects and resolve missile-alien collisions once without mutating lists from inside stream pipelines.

### Required Changes:

#### 1. Off-screen cleanup

**File**: `src/main/java/com/emenems/games/aliens/controller/GameController.java`

**Purpose**: Prevent unbounded growth from missiles and aliens that leave the visible play area.

**Contract**: After movement and before repaint, the controller removes missiles whose bottom edge is above the panel and aliens whose top edge is below the panel or whose horizontal bounds are fully outside the panel. Cleanup mutates the existing `missiles` and `aliens` list instances in place and uses `GamePanel.PANEL_WIDTH`, `GamePanel.PANEL_HEIGHT`, and `GamePanel.DEFAULT_COMPONENT_SIZE` for bounds.

#### 2. Single-pass missile collision resolution

**File**: `src/main/java/com/emenems/games/aliens/controller/GameController.java`

**Purpose**: Replace the double-removal stream logic with deterministic collision handling that removes each resolved missile/alien pair once.

**Contract**: Collision detection uses `Rectangle` overlap as it does today, but does not remove from a list while streaming that same collision search. A missile that hits an alien is removed, the alien it hit is removed, and the same missile cannot remove additional aliens in the same tick. Existing spaceship collision detection remains a no-op for this slice except that it must not break the tick.

#### 3. Testable controller logic

**File**: `src/main/java/com/emenems/games/aliens/controller/GameController.java`

**Purpose**: Allow JUnit coverage for collision and cleanup without launching a Swing window or adding a mocking dependency.

**Contract**: Extract package-private methods for one tick's non-UI logic and for cleanup/collision helpers where needed. These methods must operate on the controller's existing lists and avoid requiring a visible `JFrame`. Public runtime behavior stays controlled by `initialize()` and `actionPerformed()`.

### Success Criteria:

#### Automated Verification:

- Unit tests cover off-screen missile removal
- Unit tests cover off-screen alien removal
- Unit tests cover one missile hitting one alien removes both
- Unit tests cover one missile overlapping multiple aliens does not remove multiple aliens in one tick
- Full suite passes: `./mvnw test`
- Compile passes: `./mvnw clean compile`

#### Manual Verification:

- Repeated firing for at least 30 seconds does not make old off-screen missiles reappear or visibly slow the game
- Shooting aliens removes the hit alien and missile without flicker or double-removal behavior
- Existing ship rendering and movement still work after collision cleanup changes

**Implementation note**: After this phase and automated checks pass, stop for manual verification of sustained firing and collision behavior.

---

## Phase 3: Rendering Cleanup and Final Verification

### Overview

Clean up incidental code left by the old prototype and run the full verification loop for S-01.

### Required Changes:

#### 1. Remove dead imports and obsolete TODOs

**File**: `src/main/java/com/emenems/games/aliens/gui/GamePanel.java`

**Purpose**: Keep the presentation class focused on rendering and avoid carrying unused controller/collision imports that make future changes harder to read.

**Contract**: Remove unused imports only. Do not change image loading, dimensions, drawing order, or the shared-list rendering contract.

**File**: `src/main/java/com/emenems/games/aliens/controller/GameController.java`

**Purpose**: Remove obsolete commented-out cleanup code once real cleanup exists.

**Contract**: Delete the old `checkOutOfBoarder` TODO and any imports made unused by the new implementation.

#### 2. Final S-01 verification pass

**File**: `context/changes/smooth-playable-loop/plan.md`

**Purpose**: Ensure the implementer records completion through the canonical Progress section after code lands.

**Contract**: Do not edit step titles in `## Progress`; implementation tooling should flip checkboxes as steps complete.

### Success Criteria:

#### Automated Verification:

- Full test suite passes: `./mvnw test`
- Clean compile passes: `./mvnw clean compile`
- No non-test runtime dependencies are added to `pom.xml`

#### Manual Verification:

- Game launches from the documented command
- Gameplay is subjectively smooth at ~60 FPS
- Controls remain responsive with arrows and Space
- Missiles and aliens leaving the screen do not accumulate visibly
- Scope remains limited to S-01; no score/HUD/lives/Game Over behavior is introduced

---

## Testing Strategy

### Unit Tests:

- Controller cleanup: off-screen missiles are removed when they leave above the panel.
- Controller cleanup: aliens outside the visible board are removed according to the S-01 bounds contract.
- Controller collision: a missile-alien rectangle overlap removes both objects.
- Controller collision: one missile cannot clear multiple aliens in one tick.
- Existing `SpaceshipTest` remains unchanged and passing.

### Integration Tests:

- No automated Swing integration test is required in this slice; UI behavior is verified manually because the project has no GUI test harness and no new dependencies are allowed.

### Manual Testing Steps:

1. Run `./mvnw exec:java -Dexec.mainClass="com.emenems.games.aliens.Main"`.
2. Confirm alien movement updates smoothly rather than once per second.
3. Press Space once and confirm one missile appears.
4. Hold or repeatedly press arrow keys and confirm the ship remains responsive.
5. Fire repeatedly for at least 30 seconds and confirm the game does not visibly slow from accumulated off-screen missiles.
6. Shoot several aliens and confirm each visible hit removes the missile and one alien.

## Performance Considerations

The timer delay target is 16 ms for ~60 FPS. The object counts in S-01 are small, so simple in-place list iteration/removal is acceptable. Avoid blocking work in the timer callback; image loading must remain outside the tick path.

## Migration Notes

No data migration. This is a local desktop game with no persisted state.

## References

- Roadmap slice: `context/foundation/roadmap.md` → `S-01: smooth-playable-loop`
- PRD requirements: `context/foundation/prd.md` → FR-001, FR-002, FR-004
- Controller loop and collision source: `src/main/java/com/emenems/games/aliens/controller/GameController.java`
- Rendering shared lists: `src/main/java/com/emenems/games/aliens/gui/GamePanel.java`

## Progress

> Convention: `- [ ]` pending, `- [x]` done. Append ` — <commit sha>` when a step lands. Do not rename step titles. See `references/progress-format.md`.

### Phase 1: Controller Loop and Input

#### Automated

- [x] 1.1 Project compiles
- [x] 1.2 Existing tests pass
- [x] 1.3 No background Thread loop remains

#### Manual

- [x] 1.4 Game launches
- [x] 1.5 Aliens and missiles move smoothly
- [x] 1.6 One Space press creates one visible missile
- [x] 1.7 Arrow-key movement remains responsive

### Phase 2: Object Cleanup and Collision Semantics

#### Automated

- [x] 2.1 Unit tests cover off-screen missile removal
- [x] 2.2 Unit tests cover off-screen alien removal
- [x] 2.3 Unit tests cover one missile hitting one alien
- [x] 2.4 Unit tests cover one missile cannot remove multiple aliens
- [x] 2.5 Full suite passes
- [x] 2.6 Compile passes

#### Manual

- [x] 2.7 Sustained firing does not visibly slow the game
- [x] 2.8 Shooting aliens removes the hit alien and missile
- [x] 2.9 Existing ship rendering and movement still work

### Phase 3: Rendering Cleanup and Final Verification

#### Automated

- [x] 3.1 Full test suite passes
- [x] 3.2 Clean compile passes
- [x] 3.3 No non-test runtime dependencies are added

#### Manual

- [x] 3.4 Game launches from the documented command
- [x] 3.5 Gameplay is subjectively smooth at about 60 FPS
- [x] 3.6 Controls remain responsive with arrows and Space
- [x] 3.7 Off-screen objects do not accumulate visibly
- [x] 3.8 Scope remains limited to S-01
