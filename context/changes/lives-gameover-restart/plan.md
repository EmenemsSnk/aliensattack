# Lives, Game Over, and Restart Implementation Plan

## Overview

Implement the S-03 roadmap slice: the player has 3 lives, loses one life when the spaceship collides with an alien, reaches a Game Over screen after the last life, and can restart from a clean wave 1 session by pressing Space. This builds on the completed timer loop, scoring, HUD, and wave progression work without introducing a start menu or a View/Controller refactor.

## Current State Analysis

- `GameController` is the central gameplay owner and already owns score, wave, alien generation, the Swing timer, keyboard input, and collision checks.
- `GamePanel` renders sprites plus HUD score/wave values pushed from the controller through `updateHud(int score, int wave)`.
- `checkCollisionsWithSpaceShip()` already detects spaceship-alien overlap but returns without changing gameplay state.
- `Spaceship` has unused health fields/methods, but the PRD and roadmap now call for 3 discrete lives instead of percentage health.
- There is no game state machine yet; `GameState.java` is only a commented placeholder.
- `SPACE` currently always fires a missile, so restart behavior must be gated by game state to avoid firing while on Game Over.

## Desired End State

The game starts directly in playing state with score 0, wave 1, and 3 lives shown in the HUD. Each spaceship-alien collision removes exactly one life, clears the colliding alien so one overlap does not drain all lives across consecutive ticks, and keeps play running while lives remain. When lives reach 0, the timer-driven game remains responsive but gameplay mutation stops, the panel overlays `GAME OVER`, final score, and `Press SPACE to Restart`, and pressing Space resets score, wave, lives, pressed movement keys, missiles, and aliens to a fresh wave 1 session.

### Key Findings:

- `src/main/java/com/emenems/games/aliens/controller/GameController.java:92` is the per-tick pipeline and should gate gameplay updates by state.
- `src/main/java/com/emenems/games/aliens/controller/GameController.java:104` is the no-op spaceship collision branch to replace.
- `src/main/java/com/emenems/games/aliens/controller/GameController.java:57` handles `SPACE`, making it the correct restart integration point.
- `src/main/java/com/emenems/games/aliens/gui/GamePanel.java:30` already stores HUD scalars and should be extended rather than queried from the controller.
- `context/foundation/roadmap.md` defines S-03 as 3 lives, Game Over with final score, and Space restart.

## What We Are NOT Doing

- No start menu; the game still launches straight into play.
- No sound effects, alien fire, persistence, networking, or score history.
- No new runtime dependencies or game engine.
- No View/Controller refactor; `GameController` remains the central node.
- No percentage health UI; lives are represented as a discrete count of 3, matching the PRD.

## Implementation Approach

Keep gameplay state in `GameController`, because score and wave already live there and the controller owns the tick/input pipeline. Add a small `GameState` enum with `PLAYING` and `GAME_OVER`, push lives and state to the panel through an expanded HUD/state update method, and keep all rendering passive inside `GamePanel`. Use focused JUnit coverage for the life-loss, game-over, and restart rules without driving Swing rendering.

## Critical Implementation Details

- **Collision cooldown by removal**: remove the alien that hits the ship when a life is lost. Without this, the same overlapping alien would subtract a life every 16 ms until Game Over.
- **Game Over tick gating**: when state is `GAME_OVER`, `tick()` should skip movement, collisions, cleanup, and wave advancement, then keep pushing HUD/state and repainting so the overlay remains visible.

---

## Phase 1: Lives and Game State Rules

### Overview

Introduce discrete lives and a minimal game state, wire spaceship collision to life loss, and make Game Over stop gameplay mutation.

### Changes Required:

#### 1. Game state enum

**File**: `src/main/java/com/emenems/games/aliens/GameState.java`

**Purpose**: Replace the commented placeholder with the smallest explicit state machine needed for this slice.

**Contract**: Define `PLAYING` and `GAME_OVER`. No start menu state is included.

#### 2. Controller lives/state fields and accessors

**File**: `src/main/java/com/emenems/games/aliens/controller/GameController.java`

**Purpose**: Track player lives and game state in the same owner as score and wave.

**Contract**: Add a default lives constant of 3, `lives` initialized to 3, `gameState` initialized to `PLAYING`, and package-visible getters for tests.

#### 3. Spaceship collision side effects

**File**: `src/main/java/com/emenems/games/aliens/controller/GameController.java`

**Purpose**: Turn the existing collision detection branch into the PRD life-loss rule.

**Contract**: On the first spaceship-alien collision in a tick, remove that alien, decrement lives by 1, clear active movement keys if Game Over is reached, and set state to `GAME_OVER` when lives reaches 0.

#### 4. Playing-state tick gate

**File**: `src/main/java/com/emenems/games/aliens/controller/GameController.java`

**Purpose**: Prevent a Game Over session from continuing to move objects, spawn waves, or process collisions.

**Contract**: If the state is `GAME_OVER`, `tick()` only updates the panel state/HUD and returns before movement/collision/wave logic.

### Success Criteria:

#### Automated Verification:

- Project compiles: `./mvnw clean compile`
- Test suite passes: `./mvnw test`
- Unit tests verify one spaceship collision removes one life and the colliding alien.
- Unit tests verify the third collision enters `GAME_OVER`.
- Unit tests verify `tick()` does not advance gameplay objects while in `GAME_OVER`.

#### Manual Verification:

- While playing, colliding with an alien visibly reduces lives by one instead of ending immediately.
- After a collision, the same alien is gone so lives do not drain instantly from one overlap.

**Implementation note**: Manual verification is not auto-checked; do not mark manual progress complete until a human confirms it.

---

## Phase 2: HUD Lives, Game Over Overlay, and Restart

### Overview

Show lives in the HUD, render the Game Over overlay with final score and restart prompt, and wire Space to restart from a clean session.

### Changes Required:

#### 1. Expanded panel state push

**File**: `src/main/java/com/emenems/games/aliens/gui/GamePanel.java`

**Purpose**: Let rendering show lives and Game Over without owning gameplay rules.

**Contract**: Replace or extend the HUD update path so the controller can push score, wave, lives, and game state. HUD draws `Lives: <lives>` during play.

#### 2. Game Over overlay rendering

**File**: `src/main/java/com/emenems/games/aliens/gui/GamePanel.java`

**Purpose**: Make the terminal session state clear to the player.

**Contract**: When state is `GAME_OVER`, draw a readable centered overlay with `GAME OVER`, `Final Score: <score>`, and `Press SPACE to Restart`.

#### 3. Restart input handling

**File**: `src/main/java/com/emenems/games/aliens/controller/GameController.java`

**Purpose**: Reuse the existing Space key path for restart only when the game is over.

**Contract**: `SPACE` restarts only in `GAME_OVER`; in `PLAYING`, it keeps firing missiles. Restart resets score to 0, wave to 1, lives to 3, state to `PLAYING`, clears missiles and pressed movement keys, regenerates the initial alien row, updates panel state, and repaints.

#### 4. Restart and panel-adjacent tests

**File**: `src/test/java/com/emenems/games/aliens/controller/GameControllerTest.java`

**Purpose**: Lock restart behavior without relying on screenshot assertions.

**Contract**: Add tests that drive the controller into Game Over and assert Space resets score, wave, lives, state, missiles, and aliens. Existing movement, scoring, wave, and cleanup tests must keep passing.

### Success Criteria:

#### Automated Verification:

- Project compiles: `./mvnw clean compile`
- Full test suite passes: `./mvnw test`
- Unit tests verify Space fires missiles while playing but restarts when in `GAME_OVER`.
- Unit tests verify restart resets score, wave, lives, state, missiles, pressed input effects, and the alien wave.

#### Manual Verification:

- HUD shows `Score: 0`, `Wave: 1`, and `Lives: 3` at game start.
- Game Over overlay shows `GAME OVER`, final score, and `Press SPACE to Restart` after the last life is lost.
- Pressing Space on Game Over restarts to score 0, wave 1, lives 3, and a fresh alien row.
- Existing movement, firing, scoring, wave progression, sprite rendering, and HUD readability still work.

**Implementation note**: Manual verification is not auto-checked; do not mark manual progress complete until a human confirms it.

---

## Testing Strategy

### Unit Tests:

- `GameController.checkCollisionsWithSpaceShip()` removes one life and one colliding alien.
- Repeated collisions transition from `PLAYING` to `GAME_OVER` on the third life loss.
- `GameController.tick()` does not move aliens or missiles while game over.
- `GameController.handleKeyPressed(SPACE)` fires missiles only while playing and restarts while game over.
- Restart restores score, wave, lives, state, missile list, pressed movement key effects, and the 10-alien initial wave.

### Integration Tests:

- No automated Swing rendering integration tests in this change; rendering is verified manually in the local desktop app.

### Manual Testing Steps:

1. Run `./mvnw exec:java -Dexec.mainClass="com.emenems.games.aliens.Main"`.
2. Confirm the HUD starts with score 0, wave 1, and lives 3.
3. Let or move the ship into aliens and confirm one life is removed per collision.
4. Confirm Game Over appears after the third lost life with the final score and restart prompt.
5. Press Space and confirm score, wave, lives, aliens, missiles, and controls reset for a fresh session.
6. Confirm normal movement, shooting, scoring, wave progression, and sprite rendering still work.

## Performance Considerations

The added state checks and overlay drawing are constant-time and negligible inside the existing 16 ms Swing timer loop. Removing the colliding alien also prevents repeated collision work for the same overlap.

## Migration Notes

No persistent data exists, so there is nothing to migrate.

## References

- Product requirement: `context/foundation/prd.md`
- Roadmap slice: `context/foundation/roadmap.md`
- Controller tick/input/collisions: `src/main/java/com/emenems/games/aliens/controller/GameController.java`
- Panel HUD/rendering: `src/main/java/com/emenems/games/aliens/gui/GamePanel.java`
- State placeholder: `src/main/java/com/emenems/games/aliens/GameState.java`
- Prior scoring/wave pattern: `context/archive/2026-05-29-score-and-wave-progression/plan.md`

## Progress

> Convention: `- [ ]` pending, `- [x]` done. Add ` — <commit sha>` when a step is committed. See `references/progress-format.md`.

### Phase 1: Lives and Game State Rules

#### Automated

- [x] 1.1 Project compiles: `./mvnw clean compile`
- [x] 1.2 Test suite passes: `./mvnw test`
- [x] 1.3 Unit tests verify one spaceship collision removes one life and the colliding alien
- [x] 1.4 Unit tests verify the third collision enters `GAME_OVER`
- [x] 1.5 Unit tests verify `tick()` does not advance gameplay objects while in `GAME_OVER`

#### Manual

- [x] 1.6 While playing, colliding with an alien visibly reduces lives by one instead of ending immediately
- [x] 1.7 After a collision, the same alien is gone so lives do not drain instantly from one overlap

### Phase 2: HUD Lives, Game Over Overlay, and Restart

#### Automated

- [x] 2.1 Project compiles: `./mvnw clean compile`
- [x] 2.2 Full test suite passes: `./mvnw test`
- [x] 2.3 Unit tests verify Space fires missiles while playing but restarts when in `GAME_OVER`
- [x] 2.4 Unit tests verify restart resets score, wave, lives, state, missiles, pressed input effects, and the alien wave

#### Manual

- [x] 2.5 HUD shows `Score: 0`, `Wave: 1`, and `Lives: 3` at game start
- [x] 2.6 Game Over overlay shows `GAME OVER`, final score, and `Press SPACE to Restart` after the last life is lost
- [x] 2.7 Pressing Space on Game Over restarts to score 0, wave 1, lives 3, and a fresh alien row
- [x] 2.8 Existing movement, firing, scoring, wave progression, sprite rendering, and HUD readability still work
