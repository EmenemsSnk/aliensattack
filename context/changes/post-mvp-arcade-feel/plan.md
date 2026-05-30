# Post-MVP Arcade Feel Implementation Plan

## Overview

Implement the S-05 roadmap slice: add a start menu, retro sound feedback for shooting/explosions, and alien-fired projectiles that make the completed MVP feel more like an arcade game. The change stays inside the existing Java/Swing architecture and keeps the runtime dependency-free.

## Current State Analysis

The game currently launches directly into `PLAYING`, runs a `javax.swing.Timer` loop in `GameController`, renders passive state in `GamePanel`, and shares the same spaceship/missile/alien lists between controller and panel. The player can move, shoot, score, clear waves, lose lives, see hit feedback, reach Game Over, and restart with Space. There is no start state, no audio path, and only player missiles exist.

## Desired End State

Launching the app shows a readable start overlay with the current HUD hidden from active play expectations, and Enter starts the first session. While playing, holding Space fires player missiles at a tuned cadence and emits a short retro shoot sound. Destroying an alien emits a short explosion sound. Aliens periodically fire downward projectiles at a tuned, capped rate; those projectiles collide with the ship, cost one life using the same life-loss path as ship-alien collision, and are cleaned up when offscreen. Game Over still stops gameplay and Enter restarts into a fresh playing session.

### Key Findings:

- `src/main/java/com/emenems/games/aliens/GameState.java` currently has only `PLAYING` and `GAME_OVER`, so start menu support belongs there.
- `src/main/java/com/emenems/games/aliens/controller/GameController.java` owns input, per-tick sequencing, collision rules, scalar state, and restart.
- `src/main/java/com/emenems/games/aliens/gui/GamePanel.java` already receives pushed scalar state and draws overlays, so start/menu text and alien projectiles should remain passive rendering.
- `src/main/java/com/emenems/games/aliens/gamemachines/Missile.java` is a player-upward projectile; alien fire should not invert that class in a way that breaks existing tests.
- `context/foundation/roadmap.md` defines `post-mvp-arcade-feel` as Start Menu, retro sounds, and alien fire, with no new runtime dependencies as the default assumption.

## What We Are NOT Doing

- No external audio, game engine, asset, JSON, or media libraries.
- No persisted settings, high scores, menus beyond the start overlay, pause mode, or difficulty selector.
- No alien AI targeting, pathfinding, formations, or per-alien weapon types.
- No View/Controller refactor; `GameController` remains the gameplay owner and `GamePanel` remains passive rendering.
- No automated pixel-perfect Swing screenshot assertions.

## Implementation Approach

Keep the slice incremental. Add `START_MENU` to the state enum and initialize the controller in that state. Add a small standard-library audio helper that generates short tones at runtime and degrades silently when audio output is unavailable. Add an `AlienMissile` domain object with downward movement and a separate shared list passed to `GamePanel` and `GameController`, avoiding semantic overload of player `Missile`. Extend the existing collision/life-loss pipeline so alien projectiles reuse the same life and Game Over rules.

## Critical Implementation Details

- **Tick gating**: `START_MENU` should behave like `GAME_OVER` for gameplay mutation: update panel state and return before movement, firing, collisions, cleanup, and wave advancement.
- **Shared list wiring**: `Main` must construct the alien missile list once and pass the same reference to both controller and panel, matching the existing spaceship/missile/alien wiring.
- **Audio failure mode**: Java Sound can be unavailable on CI/headless machines. Sound playback must catch runtime/audio failures and must never fail tests, compile, or gameplay.

---

## Phase 1: Start Menu, Audio Feedback, and Alien Fire

### Overview

Deliver the full post-MVP arcade-feel slice in one phase because the features are small and share the same tick/input/rendering surfaces.

### Changes Required:

#### 1. Start menu state and input flow

**Files**:
- `src/main/java/com/emenems/games/aliens/GameState.java`
- `src/main/java/com/emenems/games/aliens/controller/GameController.java`
- `src/main/java/com/emenems/games/aliens/gui/GamePanel.java`

**Purpose**: Let the player explicitly start the session with Enter while preserving Enter-to-restart on Game Over.

**Contract**: Add `START_MENU`. Controller initializes in `START_MENU`, `initialize()` still spawns the first wave for immediate readiness, and Enter transitions to a fresh `PLAYING` session. `tick()` does not mutate gameplay while in `START_MENU`. `GamePanel` draws a centered start overlay with the game title and an Enter prompt when state is `START_MENU`.

#### 2. Retro sound feedback

**Files**:
- `src/main/java/com/emenems/games/aliens/controller/GameController.java`
- `src/main/java/com/emenems/games/aliens/audio/ArcadeSoundPlayer.java`

**Purpose**: Provide immediate arcade feedback for player shooting and alien destruction without adding dependencies or binary assets.

**Contract**: Add a package-local or public sound helper that exposes shoot and explosion methods. It uses Java standard library audio only, generates very short tones in memory, and no-ops safely when audio cannot play. Controller calls shoot sound when Space fires during `PLAYING` and explosion sound when one or more aliens are removed by player missile collisions.

#### 3. Alien projectile model and wiring

**Files**:
- `src/main/java/com/emenems/games/aliens/Main.java`
- `src/main/java/com/emenems/games/aliens/gamemachines/AlienMissile.java`
- `src/main/java/com/emenems/games/aliens/controller/GameController.java`
- `src/main/java/com/emenems/games/aliens/gui/GamePanel.java`

**Purpose**: Add downward alien projectiles while preserving the existing player missile behavior.

**Contract**: Add `AlienMissile` implementing `GameObject`, with downward movement and stable x/y accessors. `Main` constructs `List<AlienMissile>` and passes it to both controller and panel. `GamePanel` renders alien missiles distinctly from player missiles using existing drawing primitives or existing missile imagery. Controller clears alien missiles on restart and cleanup removes them once they leave the bottom of the panel.

#### 4. Alien firing rules and collision behavior

**File**: `src/main/java/com/emenems/games/aliens/controller/GameController.java`

**Purpose**: Make aliens an active threat without overwhelming the player.

**Contract**: During `PLAYING` ticks, aliens may fire only when aliens exist, using a low random chance per tick and a cap on active alien missiles. Spawn from a randomly selected alien near its bottom edge. Alien missile collision with the ship removes the projectile, costs exactly one life, activates hit feedback, and enters Game Over if lives reach 0. It must not remove aliens or score points.

#### 5. Unit coverage

**Files**:
- `src/test/java/com/emenems/games/aliens/controller/GameControllerTest.java`
- `src/test/java/com/emenems/games/aliens/gamemachines/AlienMissileTest.java`

**Purpose**: Lock the new state and projectile rules without relying on Swing rendering or real audio.

**Contract**: Add tests for start menu tick gating, Space start behavior, Space still firing while playing, alien missile downward movement, alien missile cleanup, alien missile life loss/Game Over behavior, alien firing cap, and restart clearing alien missiles.

### Success Criteria:

#### Automated Verification:

- Project compiles: `./mvnw clean compile`
- Full test suite passes: `./mvnw test`
- Unit tests verify `START_MENU` does not move gameplay objects until Enter starts play.
- Unit tests verify Enter starts from menu, Space fires while playing, and Enter restarts from Game Over.
- Unit tests verify alien missiles move downward and are removed offscreen.
- Unit tests verify alien missile collision removes one life, removes the projectile, activates hit feedback, and can enter Game Over.
- Unit tests verify alien firing respects the configured active-projectile cap.

#### Manual Verification:

- Running the game shows a start overlay; pressing Enter starts play.
- Shooting plays a short retro sound and destroyed aliens play a distinct explosion sound when audio output is available.
- Aliens visibly fire downward projectiles at a playable rate.
- Alien projectile hits reduce lives, show hit feedback, and Game Over/restart behavior still works.
- Existing movement, player shooting, scoring, wave progression, bottom invasion loss, and HUD readability still work.

**Implementation note**: Manual verification is not auto-checked; do not mark manual progress complete until a human confirms it.

---

## Testing Strategy

### Unit Tests:

- Controller starts in `START_MENU`; ticks in menu push state but do not move missiles/aliens.
- Enter transitions from `START_MENU` to `PLAYING`; Space in `PLAYING` still creates player missiles.
- `AlienMissile` moves downward at a fixed speed.
- Offscreen alien missiles are cleaned up.
- Alien missile collision removes the projectile and one life, activates hit feedback, and can enter `GAME_OVER`.
- Alien firing respects the active alien missile cap and does not fire without aliens.
- Restart clears alien missiles and returns to a fresh `PLAYING` session.

### Integration Tests:

- No automated Swing rendering or sound-device integration tests. Rendering and real audio are verified manually by running the desktop app.

### Manual Testing Steps:

1. Run `./mvnw exec:java -Dexec.mainClass="com.emenems.games.aliens.Main"`.
2. Confirm the start overlay appears and gameplay does not begin until Enter is pressed.
3. Press Enter and confirm the game starts, then hold Space and confirm the ship fires.
4. Confirm shoot and explosion sounds are audible when local audio output is available.
5. Let aliens fire and confirm downward projectiles are visible and do not flood the screen.
6. Let an alien projectile hit the ship and confirm one life is removed with hit feedback.
7. Confirm Game Over and Space restart still reset score, wave, lives, aliens, player missiles, alien missiles, and controls.

## Performance Considerations

Alien missile lists are capped and use the same small-object update pattern as player missiles, so per-tick work remains tiny. Generated tones are short and should be cached or generated with minimal work inside the sound helper; audio playback must not block or throw through the EDT.

---

## Phase 2: Manual Feedback Tuning

### Overview

Apply the manual feedback from the first arcade-feel pass: use Enter for start/restart, make the game easier to read and less frantic, reduce repeated Space tapping, and make the sound layer less harsh with a quiet space-like background loop.

### Changes Required:

#### 1. Enter for session transitions

**Files**:
- `src/main/java/com/emenems/games/aliens/controller/GameController.java`
- `src/main/java/com/emenems/games/aliens/gui/GamePanel.java`
- `src/test/java/com/emenems/games/aliens/controller/GameControllerTest.java`

**Purpose**: Match the requested control contract: Enter starts and restarts, Space only fires during active play.

**Contract**: `ENTER` transitions `START_MENU -> PLAYING` and `GAME_OVER -> PLAYING`. `SPACE` does not start or restart sessions.

#### 2. Larger, calmer playfield

**Files**:
- `src/main/java/com/emenems/games/aliens/Main.java`
- `src/main/java/com/emenems/games/aliens/controller/GameController.java`
- `src/main/java/com/emenems/games/aliens/gui/GamePanel.java`
- `src/test/java/com/emenems/games/aliens/controller/GameControllerTest.java`

**Purpose**: Make sprites more readable and reduce the constant pressure caused by a very wide board with too many enemies.

**Contract**: Reduce panel width, enlarge component size, reduce initial alien count, reduce alien speed/cap, and reduce active alien projectile pressure while preserving wave progression and collision tests.

#### 3. Hold-to-fire

**Files**:
- `src/main/java/com/emenems/games/aliens/controller/GameController.java`
- `src/test/java/com/emenems/games/aliens/controller/GameControllerTest.java`

**Purpose**: Avoid requiring constant repeated Space tapping during normal play.

**Contract**: Pressing Space fires immediately during `PLAYING`; holding Space continues firing on a short cooldown. Releasing Space stops auto-fire.

#### 4. Softer sound and background loop

**Files**:
- `src/main/java/com/emenems/games/aliens/audio/ArcadeSoundPlayer.java`
- `src/main/java/com/emenems/games/aliens/controller/GameController.java`

**Purpose**: Make the audio feel less abrasive and add a simple space-like atmosphere without new assets or dependencies.

**Contract**: Lower the volume/harshness of shoot/explosion tones. Start a looping generated background pattern when play begins or restarts, and stop it on Game Over. Audio must still no-op safely when unavailable.

### Success Criteria:

#### Automated Verification:

- Project compiles: `./mvnw clean compile`
- Full test suite passes: `./mvnw test`
- Unit tests verify Enter starts from menu and restarts from Game Over while Space does not restart.
- Unit tests verify holding Space auto-fires with cooldown.
- Unit tests verify tuned alien count/speed/cap expectations remain coherent with wave and firing rules.

#### Manual Verification:

- Start and restart happen on Enter, not Space.
- Sprites are easier to see and the board feels less overly wide.
- The first wave has fewer aliens and less immediate pressure.
- Holding Space is enough for repeated shooting; repeated tapping is not required.
- The new audio feels less harsh, and the background loop improves the space mood without being distracting.

**Implementation note**: Manual verification is not auto-checked; do not mark manual progress complete until a human confirms it.

## Migration Notes

No persistent data exists, so there is nothing to migrate.

## References

- Roadmap slice: `context/foundation/roadmap.md`
- Product constraints: `context/foundation/prd.md`
- Controller tick/input/collision code: `src/main/java/com/emenems/games/aliens/controller/GameController.java`
- Panel rendering/state push: `src/main/java/com/emenems/games/aliens/gui/GamePanel.java`
- Existing projectile model: `src/main/java/com/emenems/games/aliens/gamemachines/Missile.java`
- Existing controller tests: `src/test/java/com/emenems/games/aliens/controller/GameControllerTest.java`

## Progress

> Convention: `- [ ]` pending, `- [x]` done. Add ` — <commit sha>` when a step is committed. See `references/progress-format.md`.

### Phase 1: Start Menu, Audio Feedback, and Alien Fire

#### Automated

- [x] 1.1 Project compiles: `./mvnw clean compile` — d67030a
- [x] 1.2 Full test suite passes: `./mvnw test` — d67030a
- [x] 1.3 Unit tests verify `START_MENU` does not move gameplay objects until Enter starts play — d67030a
- [x] 1.4 Unit tests verify Enter starts from menu, Space fires while playing, and Enter restarts from Game Over — d67030a
- [x] 1.5 Unit tests verify alien missiles move downward and are removed offscreen — d67030a
- [x] 1.6 Unit tests verify alien missile collision removes one life, removes the projectile, activates hit feedback, and can enter Game Over — d67030a
- [x] 1.7 Unit tests verify alien firing respects the configured active-projectile cap — d67030a

#### Manual

- [x] 1.8 Running the game shows a start overlay; pressing Enter starts play — d67030a
- [x] 1.9 Shooting plays a short retro sound and destroyed aliens play a distinct explosion sound when audio output is available — d67030a
- [x] 1.10 Aliens visibly fire downward projectiles at a playable rate — d67030a
- [x] 1.11 Alien projectile hits reduce lives, show hit feedback, and Game Over/restart behavior still works — d67030a
- [x] 1.12 Existing movement, player shooting, scoring, wave progression, bottom invasion loss, and HUD readability still work — d67030a

### Phase 2: Manual Feedback Tuning

#### Automated

- [x] 2.1 Project compiles: `./mvnw clean compile` — d67030a
- [x] 2.2 Full test suite passes: `./mvnw test` — d67030a
- [x] 2.3 Unit tests verify Enter starts from menu and restarts from Game Over while Space does not restart — d67030a
- [x] 2.4 Unit tests verify holding Space auto-fires with cooldown — d67030a
- [x] 2.5 Unit tests verify tuned alien count/speed/cap expectations remain coherent with wave and firing rules — d67030a

#### Manual

- [x] 2.6 Start and restart happen on Enter, not Space — d67030a
- [x] 2.7 Sprites are easier to see and the board feels less overly wide — d67030a
- [x] 2.8 The first wave has fewer aliens and less immediate pressure — d67030a
- [x] 2.9 Holding Space is enough for repeated shooting; repeated tapping is not required — d67030a
- [x] 2.10 The new audio feels less harsh, and the background loop improves the space mood without being distracting — d67030a
