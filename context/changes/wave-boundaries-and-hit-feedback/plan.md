# Wave Boundaries and Hit Feedback Implementation Plan

## Overview

Implement the S-04 roadmap slice: keep the player ship inside the board, generate each alien wave with varied non-overlapping positions in the top fifth of the map, show a visible hit reaction when the ship loses a life, and end the game with an aliens-win message when an alien reaches the bottom of the board before the wave is cleared.

## Current State Analysis

- `GameController` owns the tick pipeline, input, collision rules, wave spawning, score, wave, lives, and game state.
- `GamePanel` passively renders shared object lists plus scalar state pushed from the controller through `updateGameState`.
- `Spaceship` movement methods directly mutate coordinates by 5 px and can move the ship outside the visible panel.
- Alien spawning is a fixed single row: `ALIEN_START_Y = 30` and `ALIEN_START_X_VALUES`, so each wave has identical spacing and height.
- Offscreen alien cleanup currently removes aliens below the panel; if the whole wave leaves the board, `advanceWaveIfCleared()` can treat that as a cleared wave.
- Game Over rendering has one fixed title, so it cannot distinguish player death from aliens winning.

## Desired End State

The ship remains fully visible inside `GamePanel.PANEL_WIDTH` x `GamePanel.PANEL_HEIGHT` regardless of held movement keys. A new wave contains 10 aliens with lightly randomized X/Y start positions, no overlapping rectangles, and all start Y values at or above the top fifth boundary. When a ship-alien collision costs a life, the player sees a short visual flash/tint. If any alien reaches the bottom edge of the board, gameplay stops in Game Over and the overlay says `ALIENS WIN` while preserving final score and Space restart.

### Key Findings:

- `src/main/java/com/emenems/games/aliens/controller/GameController.java:101` is the single movement integration point for held keys.
- `src/main/java/com/emenems/games/aliens/controller/GameController.java:64` is the wave generation point used by initialize, wave advancement, and restart.
- `src/main/java/com/emenems/games/aliens/controller/GameController.java:122` defines the tick ordering; alien invasion must be checked before offscreen cleanup can remove the alien.
- `src/main/java/com/emenems/games/aliens/gui/GamePanel.java:46` is the state push boundary to extend for hit feedback and Game Over title.
- `src/test/java/com/emenems/games/aliens/controller/GameControllerTest.java:16` already covers controller rules and is the right place for boundary, spawn, invasion, and feedback unit tests.

## What We Are NOT Doing

- No fully free-form random spawn; randomization stays constrained to safe lanes so tests can prove the no-overlap/top-fifth invariants.
- No sound effects, Start Menu, alien fire, persistence, networking, or score history.
- No new runtime dependencies.
- No View/Controller refactor; `GameController` remains the central gameplay node.
- No pixel-perfect Swing rendering tests; visual behavior is supported by state tests and manual play.

## Implementation Approach

Keep gameplay decisions in `GameController` and rendering passive in `GamePanel`. Add a small clamping method to `Spaceship` so the controller can apply panel bounds after movement without making the model depend on Swing. Replace the single-row alien spawn constants with lane-based random jitter that changes each wave while preserving no-overlap/top-fifth invariants. Add controller-owned transient hit-feedback ticks and a controller-owned Game Over title, both pushed to `GamePanel` with the existing scalar state.

## Critical Implementation Details

- **Tick ordering**: check for alien invasion after alien movement and before offscreen cleanup. Otherwise cleanup can remove the last alien and accidentally advance to the next wave.
- **Hit feedback lifetime**: set feedback ticks on life loss, decrement once per playing tick, and push `hitFeedbackActive` to the panel. Do not sleep, schedule a second timer, or block the EDT.
- **Bottom-edge rule**: aliens win when `alien.getY() + DEFAULT_COMPONENT_SIZE >= PANEL_HEIGHT`; this means the first alien touching the bottom edge ends the wave.

---

## Phase 1: Boundaries, Spawn Layout, Invasion Loss, and Hit Feedback

### Overview

Deliver the full S-04 gameplay hardening in one tightly scoped phase so the already playable MVP is improved without spreading state changes across multiple partial commits.

### Changes Required:

#### 1. Spaceship clamping

**File**: `src/main/java/com/emenems/games/aliens/gamemachines/Spaceship.java`

**Purpose**: Keep the ship fully inside the visible board while preserving existing movement methods.

**Contract**: Add a bounds-clamp method that constrains `x` and `y` between minimum coordinates and maximum coordinates supplied by the controller.

#### 2. Controller movement bounds

**File**: `src/main/java/com/emenems/games/aliens/controller/GameController.java`

**Purpose**: Apply board bounds after held-key movement so no input path can leave the ship outside the panel.

**Contract**: After processing pressed movement keys, clamp the ship to `0..PANEL_WIDTH - DEFAULT_COMPONENT_SIZE` and `0..PANEL_HEIGHT - DEFAULT_COMPONENT_SIZE`.

#### 3. Varied non-overlapping wave spawn

**File**: `src/main/java/com/emenems/games/aliens/controller/GameController.java`

**Purpose**: Replace the uniform one-row spawn with a varied, lightly randomized layout that changes between waves.

**Contract**: Generate 10 aliens from safe horizontal lanes with bounded random X/Y jitter. All spawn Y values must satisfy `y <= PANEL_HEIGHT / 5`, no two spawn rectangles of `DEFAULT_COMPONENT_SIZE` overlap, and consecutive waves should not reuse the exact same positions.

#### 4. Alien invasion Game Over

**File**: `src/main/java/com/emenems/games/aliens/controller/GameController.java`

**Purpose**: End the game when a wave is not cleared before aliens reach the bottom of the board.

**Contract**: During `tick()`, after aliens move and before cleanup/wave advancement, enter `GAME_OVER`, clear pressed movement keys, and set the Game Over title to `ALIENS WIN` if any alien reaches the bottom edge.

#### 5. Hit feedback state

**File**: `src/main/java/com/emenems/games/aliens/controller/GameController.java`

**Purpose**: Expose a short-lived visual state after ship-alien collision costs a life.

**Contract**: Set hit feedback active on each ship collision that removes a life, decrement it on subsequent playing ticks, reset it on restart, and expose package-visible state for tests.

#### 6. Passive rendering updates

**File**: `src/main/java/com/emenems/games/aliens/gui/GamePanel.java`

**Purpose**: Render the new transient hit feedback and aliens-win title without owning gameplay rules.

**Contract**: Extend the controller state push to include `hitFeedbackActive` and `gameOverTitle`. Draw a brief visible red tint/border while feedback is active. Draw the provided Game Over title instead of always drawing `GAME OVER`.

#### 7. Unit coverage

**Files**:
- `src/test/java/com/emenems/games/aliens/gamemachines/SpaceshipTest.java`
- `src/test/java/com/emenems/games/aliens/controller/GameControllerTest.java`

**Purpose**: Lock the new edge cases so future gameplay changes do not regress them.

**Contract**: Add tests for spaceship clamping, movement clamping through `tick()`, varied non-overlapping top-fifth spawns, aliens-win Game Over, hit feedback activation/expiry, and restart reset.

### Success Criteria:

#### Automated Verification:

- Project compiles: `./mvnw clean compile`
- Full test suite passes: `./mvnw test`
- Unit tests verify ship movement cannot leave the board through held keys.
- Unit tests verify generated aliens are varied, non-overlapping, start within the top fifth of the board, and change positions between waves.
- Unit tests verify an alien reaching the bottom edge enters `GAME_OVER` with `ALIENS WIN`.
- Unit tests verify hit feedback activates after life loss, expires after a short duration, and resets on restart.

#### Manual Verification:

- In the running game, holding each arrow key keeps the ship fully visible inside the board.
- Fresh waves show aliens at varied positions/heights and no visible overlap.
- Ship-alien collision shows a visible life-loss effect.
- Letting an alien reach the bottom ends the game with an aliens-win message and Space restart still works.

**Implementation note**: Manual verification is not auto-checked; do not mark manual progress complete until a human confirms it.

---

## Testing Strategy

### Unit Tests:

- `Spaceship.clampToBounds` clamps lower and upper coordinates.
- `GameController.tick()` clamps held-key movement to panel bounds.
- Wave generation creates 10 aliens, all in top fifth, with varied Y positions, no overlapping rectangles, and different positions between consecutive waves.
- Alien invasion transitions to `GAME_OVER`, clears movement, and uses the `ALIENS WIN` title.
- Hit feedback is active immediately after life loss, then expires after its configured tick window.
- Restart clears hit feedback and restores the default Game Over title for future player-death losses.

### Integration Tests:

- No automated Swing integration harness exists. Controller state is tested directly; rendering is verified manually by running the desktop app.

### Manual Testing Steps:

1. Run `./mvnw exec:java -Dexec.mainClass="com.emenems.games.aliens.Main"`.
2. Hold Left, Right, Up, and Down long enough to confirm the ship remains fully visible.
3. Observe a fresh wave and confirm aliens have varied spacing/heights without visible overlap.
4. Collide with an alien and confirm a short red visual hit effect appears while lives decrease by one.
5. Avoid shooting until an alien reaches the bottom and confirm the overlay says `ALIENS WIN`.
6. Press Space after Game Over and confirm the game restarts cleanly.

## Performance Considerations

The new checks are O(number of aliens) inside an already small list of 10 aliens. Hit feedback is a simple integer countdown and one conditional render, so it is negligible inside the existing 16 ms Swing timer.

## Migration Notes

No persistent data exists, so there is nothing to migrate.

## References

- Roadmap slice: `context/foundation/roadmap.md`
- Product constraints: `context/foundation/prd.md`
- Controller tick/input/spawn/collision code: `src/main/java/com/emenems/games/aliens/controller/GameController.java`
- Panel rendering/state push: `src/main/java/com/emenems/games/aliens/gui/GamePanel.java`
- Spaceship movement model: `src/main/java/com/emenems/games/aliens/gamemachines/Spaceship.java`
- Existing controller test harness: `src/test/java/com/emenems/games/aliens/controller/GameControllerTest.java`

## Progress

> Convention: `- [ ]` pending, `- [x]` done. Add ` — <commit sha>` when a step is committed. See `references/progress-format.md`.

### Phase 1: Boundaries, Spawn Layout, Invasion Loss, and Hit Feedback

#### Automated

- [x] 1.1 Project compiles: `./mvnw clean compile`
- [x] 1.2 Full test suite passes: `./mvnw test`
- [x] 1.3 Unit tests verify ship movement cannot leave the board through held keys
- [x] 1.4 Unit tests verify generated aliens are varied, non-overlapping, start within the top fifth of the board, and change positions between waves
- [x] 1.5 Unit tests verify an alien reaching the bottom edge enters `GAME_OVER` with `ALIENS WIN`
- [x] 1.6 Unit tests verify hit feedback activates after life loss, expires after a short duration, and resets on restart

#### Manual

- [x] 1.7 In the running game, holding each arrow key keeps the ship fully visible inside the board
- [x] 1.8 Fresh waves show aliens at varied positions/heights and no visible overlap
- [x] 1.9 Ship-alien collision shows a visible life-loss effect
- [x] 1.10 Letting an alien reach the bottom ends the game with an aliens-win message and Space restart still works
