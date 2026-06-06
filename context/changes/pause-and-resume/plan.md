# Pause and resume implementation plan

## Overview

Implement roadmap slice S-06 by adding an in-game pause state that freezes the active arcade session and resumes it on demand. The player toggles pause with `P`, sees a centered paused overlay, and background music stops while paused and starts again on resume.

## Current State Analysis

The game already has the right shape for pause: `GameController.tick()` mutates gameplay only while the session state is `PLAYING`, and simply pushes panel state for every other game state. The missing piece is a first-class `PAUSED` state plus explicit input, rendering, audio, and documentation behavior around it.

### Key findings:

- The state machine currently has only `START_MENU`, `PLAYING`, and `GAME_OVER`, so pause needs a new enum value rather than overloading an existing state. `src/main/java/com/emenems/games/aliens/GameState.java:3`
- `GameController.tick()` already freezes movement, firing, collisions, cleanup, wave advancement, and session timers outside `PLAYING`. `src/main/java/com/emenems/games/aliens/controller/GameController.java:277`
- Input handling is state-based in `handleKeyPressed(...)`; `ENTER` starts/restarts today, and `PLAYING` delegates to movement/fire handling. `src/main/java/com/emenems/games/aliens/controller/GameController.java:209`
- `resetSession()` clears held movement keys, held fire, fire cooldown, entity lists, and scalar session state, which is the local pattern to follow for clearing held input on pause. `src/main/java/com/emenems/games/aliens/controller/GameController.java:526`
- `GamePanel.updateGameState(...)` is the established scalar push path for rendering overlays and HUD state. `src/main/java/com/emenems/games/aliens/gui/GamePanel.java:90`
- The panel already renders full-screen overlays for start menu and game over after drawing the frozen playfield. `src/main/java/com/emenems/games/aliens/gui/GamePanel.java:136`
- Background music currently exposes `startBackgroundMusic()` and `stopBackgroundMusic()`, and both audio failure paths keep gameplay safe in headless or audio-less environments. `src/main/java/com/emenems/games/aliens/audio/ArcadeSoundPlayer.java:21`
- Roadmap S-06 defines the desired outcome as pausing and resuming without object state changing during pause, and names accidental movement, firing, or timer changes as the main risk. `context/foundation/roadmap.md:141`

## Desired End State

While playing, pressing `P` enters `PAUSED`. The current board remains visible under a centered translucent overlay reading `PAUSED` and `Press P to Resume`; held movement/fire input is cleared; background music stops; and no gameplay objects or timers advance while the game is paused.

Pressing `P` again resumes the same session from the frozen state and starts background music again. In paused state, `ENTER`, `SPACE`, arrows, and other keys do not start, restart, move, or fire. Start menu and Game Over retain their existing `ENTER` behavior.

## What We Are Not Doing

- No pause menu, settings screen, quit action, restart shortcut, save-state feature, or profile behavior.
- No changes to score, wave, lives, rapid-fire, combo, alien spawning, collision, or difficulty rules.
- No new audio assets, external libraries, or Swing threading changes.
- No exact audio clip-position resume requirement; current generated background music may restart when resumed.
- No updates to persistent agent guidance beyond the plan artifacts; user-facing README controls are enough for this slice.

## Implementation Approach

Add `PAUSED` to `GameState`, keep session-owned state transitions in `GameSession`, and let `GameController` orchestrate the `P` key, held-input clearing, and background music stop/start. Keep the Swing timer running so repaint and panel state pushes continue, but rely on the existing non-`PLAYING` tick gate to freeze gameplay. Render pause passively in `GamePanel` using the same overlay style as start and game-over screens, and document `P` in the README controls.

## Critical Implementation Details

### State sequencing

Pause must not call `resetSession()` or touch entity collections. The transition only changes session state from `PLAYING` to `PAUSED`, clears controller-held input/cooldown intent, stops background music, updates the panel state, and repaints; resume changes `PAUSED` back to `PLAYING`, starts background music, updates panel state, and repaints.

### Audio safety

Use the existing `ArcadeSoundPlayer.startBackgroundMusic()` and `stopBackgroundMusic()` contract unless implementation discovers a cleaner local helper is needed. Do not introduce an audio exception path from pause/resume into gameplay; the current audio class intentionally swallows unavailable-device failures.

## Phase 1: State machine and input contract

### Overview

Add pause as a first-class game state and wire the `P` key so gameplay can enter and leave that state safely.

### Required Changes:

#### 1. Game state enum

**File**: `src/main/java/com/emenems/games/aliens/GameState.java`

**Purpose**: Represent pause explicitly instead of inferring it from menu or game-over state.

**Contract**: Add `PAUSED` as a distinct enum value. Existing state names remain unchanged.

#### 2. Session pause/resume lifecycle

**File**: `src/main/java/com/emenems/games/aliens/GameSession.java`

**Purpose**: Keep scalar state transitions owned by the session, consistent with start/restart and game-over lifecycle.

**Contract**: Add methods that pause only from `PLAYING` and resume only from `PAUSED`. These methods must not reset score, wave, lives, timers, combo, rapid-fire, hit feedback, or game-over title. `startOrRestart()` still returns to a fresh `PLAYING` state.

#### 3. Controller key handling and audio orchestration

**File**: `src/main/java/com/emenems/games/aliens/controller/GameController.java`

**Purpose**: Let the player toggle pause with `P` while preserving existing start/restart and fire/move controls.

**Contract**: In `handleKeyPressed(...)`, route `P` during `PLAYING` to pause and during `PAUSED` to resume. While paused, ignore `ENTER`, `SPACE`, arrows, and all other keys. Pausing clears `pressedMovementKeys`, `spacePressed`, and `playerFireCooldownTicks`, stops background music, updates panel state, and repaints. Resuming starts background music, updates panel state, and repaints.

#### 4. Controller state exposure for tests

**File**: `src/main/java/com/emenems/games/aliens/controller/GameController.java`

**Purpose**: Keep the existing package-private test seam useful for pause assertions.

**Contract**: Reuse existing getters where possible; add only minimal package-private accessors if a pause-specific assertion cannot be made through current getters and collections.

### Success Criteria:

#### Automated verification:

- `GameSessionTest` covers pause only from `PLAYING`, resume only from `PAUSED`, and timer/scalar preservation across pause/resume.
- `GameControllerTest` covers `P` toggling from `PLAYING` to `PAUSED` and back to `PLAYING`.
- `GameControllerTest` covers `ENTER`, `SPACE`, and movement keys being ignored while paused.
- `GameControllerTest` covers held movement/fire input and player fire cooldown being cleared on pause.
- `GameControllerTest` covers missiles, alien missiles, aliens, power-ups, alien explosions, rapid-fire ticks, combo ticks, wave-message ticks, and fire cooldown not advancing during paused ticks where applicable.
- `./mvnw test`

#### Manual verification:

- Pressing `P` during gameplay pauses immediately.
- Pressing arrows or holding space while paused does not move or fire.
- Pressing `P` resumes the same session without a restart.

**Implementation note**: After this phase and its automated checks pass, stop for human confirmation that pause/resume preserves the active board state correctly before moving to presentation polish.

---

## Phase 2: Rendering and documentation

### Overview

Make paused state visible to the player and document the new control.

### Required Changes:

#### 1. Pause overlay rendering

**File**: `src/main/java/com/emenems/games/aliens/gui/GamePanel.java`

**Purpose**: Give clear visual feedback that gameplay is intentionally frozen.

**Contract**: Add a centered paused overlay rendered when `gameState == GameState.PAUSED`. Draw it after gameplay sprites/HUD/wave message and before menu/game-over overlays. The overlay should use a translucent full-board dim plus centered `PAUSED` text and `Press P to Resume`, matching the existing simple Swing drawing style.

#### 2. Panel helper tests

**File**: `src/test/java/com/emenems/games/aliens/gui/GamePanelTest.java`

**Purpose**: Cover deterministic pause presentation logic without fragile pixel-perfect rendering tests.

**Contract**: Add a small helper predicate if useful, such as `isPausedOverlayVisible(GameState gameState)`, and test that it is true only for `PAUSED`.

#### 3. README controls

**File**: `README.md`

**Purpose**: Keep user-facing controls complete.

**Contract**: Add `P` to the “How to Play” section as the pause/resume control. Preserve existing controls and build/run instructions.

### Success Criteria:

#### Automated verification:

- `GamePanelTest` covers paused overlay visibility helper behavior if a helper is introduced.
- `./mvnw clean compile`
- `./mvnw test`

#### Manual verification:

- Paused overlay is centered, readable, and clearly distinct from start menu and Game Over.
- The frozen board remains visible enough for the player to understand what will resume.
- README lists `P` as pause/resume.

**Implementation note**: After this phase and its automated checks pass, stop for human confirmation that the overlay is readable and not confused with Game Over or the start menu.

---

## Phase 3: Regression coverage

### Overview

Run the full regression gate and manually smoke-test the complete pause flow against existing lifecycle behavior.

### Required Changes:

#### 1. Full automated regression

**Files**:
- `src/test/java/com/emenems/games/aliens/GameSessionTest.java`
- `src/test/java/com/emenems/games/aliens/controller/GameControllerTest.java`
- `src/test/java/com/emenems/games/aliens/gui/GamePanelTest.java`

**Purpose**: Confirm pause does not regress the shipped arcade loop or existing temporary-effect semantics.

**Contract**: Existing tests for start menu, Game Over, restart, rapid-fire, combo, wave message, hit feedback, alien explosions, and input continue to pass with the new `PAUSED` state.

#### 2. Manual gameplay smoke test

**File**: `README.md`

**Purpose**: Validate the feature in the actual Swing app where overlay readability and audio behavior matter.

**Contract**: Launch with the documented Maven command and test start, pause, ignored paused controls, resume, firing, movement, wave message, rapid-fire/combo if practical, Game Over, and restart.

### Success Criteria:

#### Automated verification:

- `./mvnw clean compile`
- `./mvnw test`

#### Manual verification:

- Start menu still starts with `ENTER`.
- During play, `P` pauses and stops background music.
- During pause, entities and all visible timers remain frozen across several seconds.
- During pause, `ENTER`, arrows, and `SPACE` do not restart, move, or fire.
- Pressing `P` resumes play and background music starts again.
- Game Over still stops music and `ENTER` restart still works.

---

## Testing Strategy

### Unit tests:

- `GameSessionTest` for pause/resume state transitions and scalar/timer preservation.
- `GameControllerTest` for key handling, held-input clearing, paused tick freeze, and existing lifecycle regressions.
- `GamePanelTest` for any pause overlay visibility helper.

### Integration tests:

- Controller tests should drive `handleKeyPressed(...)` and `tick()` directly, following the current deterministic, no-window/no-audio pattern.
- Full Maven compile/test gate after adding `PAUSED`, because switch expressions over `GameState` will force all call sites to acknowledge the new enum value.

### Manual test steps:

1. Launch the game with `./mvnw exec:java -Dexec.mainClass="com.emenems.games.aliens.Main"`.
2. Press `ENTER`; confirm the game starts normally.
3. Hold an arrow key and/or `SPACE`, then press `P`; confirm the game pauses, music stops, and the overlay appears.
4. While paused, wait several seconds and press arrows, `SPACE`, and `ENTER`; confirm nothing moves, fires, expires, or restarts.
5. Press `P`; confirm the same board state resumes, music starts, and fresh movement/fire input is required.
6. Continue to Game Over and press `ENTER`; confirm restart still works.

## Performance Notes

Pause adds only a new enum branch, a few scalar transitions, and one overlay draw path. Keeping the Swing timer running preserves the existing EDT model and avoids background-thread or timer lifecycle risk.

## Migration Notes

No data migration is required. This is an in-memory state-machine and rendering change inside the existing desktop process.

## References

- Roadmap slice: `context/foundation/roadmap.md:141`
- Game state enum: `src/main/java/com/emenems/games/aliens/GameState.java:3`
- Controller tick gate: `src/main/java/com/emenems/games/aliens/controller/GameController.java:277`
- Controller input handling: `src/main/java/com/emenems/games/aliens/controller/GameController.java:209`
- Controller reset/input clearing pattern: `src/main/java/com/emenems/games/aliens/controller/GameController.java:526`
- Panel scalar push and overlays: `src/main/java/com/emenems/games/aliens/gui/GamePanel.java:90`
- Audio background music contract: `src/main/java/com/emenems/games/aliens/audio/ArcadeSoundPlayer.java:21`
- README controls: `README.md:18`

## Progress

> Convention: `- [ ]` pending, `- [x]` completed. Add ` — <commit sha>` when a step lands. Do not rename step titles. See `references/progress-format.md`.

### Phase 1: State machine and input contract

#### Automated

- [x] 1.1 `GameSessionTest` covers pause only from `PLAYING`, resume only from `PAUSED`, and timer/scalar preservation across pause/resume. — 6288bd4
- [x] 1.2 `GameControllerTest` covers `P` toggling from `PLAYING` to `PAUSED` and back to `PLAYING`. — 6288bd4
- [x] 1.3 `GameControllerTest` covers `ENTER`, `SPACE`, and movement keys being ignored while paused. — 6288bd4
- [x] 1.4 `GameControllerTest` covers held movement/fire input and player fire cooldown being cleared on pause. — 6288bd4
- [x] 1.5 `GameControllerTest` covers gameplay objects and timers not advancing during paused ticks where applicable. — 6288bd4
- [x] 1.6 `./mvnw test` — 6288bd4

#### Manual

- [x] 1.7 Pressing `P` during gameplay pauses immediately. — 6288bd4
- [x] 1.8 Pressing arrows or holding space while paused does not move or fire. — 6288bd4
- [x] 1.9 Pressing `P` resumes the same session without a restart. — 6288bd4

### Phase 2: Rendering and documentation

#### Automated

- [x] 2.1 `GamePanelTest` covers paused overlay visibility helper behavior if a helper is introduced. — 6288bd4
- [x] 2.2 `./mvnw clean compile` — 6288bd4
- [x] 2.3 `./mvnw test` — 6288bd4

#### Manual

- [x] 2.4 Paused overlay is centered, readable, and clearly distinct from start menu and Game Over. — 6288bd4
- [x] 2.5 The frozen board remains visible enough for the player to understand what will resume. — 6288bd4
- [x] 2.6 README lists `P` as pause/resume. — 6288bd4

### Phase 3: Regression coverage

#### Automated

- [x] 3.1 `./mvnw clean compile` — 6288bd4
- [x] 3.2 `./mvnw test` — 6288bd4

#### Manual

- [x] 3.3 Start menu still starts with `ENTER`. — 6288bd4
- [x] 3.4 During play, `P` pauses and stops background music. — 6288bd4
- [x] 3.5 During pause, entities and all visible timers remain frozen across several seconds. — 6288bd4
- [x] 3.6 During pause, `ENTER`, arrows, and `SPACE` do not restart, move, or fire. — 6288bd4
- [x] 3.7 Pressing `P` resumes play and background music starts again. — 6288bd4
- [x] 3.8 Game Over still stops music and `ENTER` restart still works. — 6288bd4
