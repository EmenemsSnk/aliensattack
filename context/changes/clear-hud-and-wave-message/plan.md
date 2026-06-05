# Clear HUD and wave message implementation plan

## Overview

Implement roadmap slice S-05 by making the in-game HUD easier to scan and by showing a short, non-blocking wave-start message. The change must preserve the existing gameplay loop, controls, scoring, wave progression, lives, Game Over flow, and temporary-effect semantics while fitting the current `GameSession -> GameController -> GamePanel` scalar state push.

## Current State Analysis

The game already has a complete playable loop with passive rendering in `GamePanel`, controller-owned gameplay mutation in `GameController`, and scalar session state in `GameSession`. HUD information is visible today, but it is rendered as an ungrouped vertical text stack in the top-left corner, and there is no wave-start message state or rendering path.

### Key findings:

- `GamePanel.drawHud(...)` draws `Score`, `Wave`, `Lives`, plus optional `RAPID FIRE` and `COMBO` labels directly on the playfield with no grouping or backdrop today. [`src/main/java/com/emenems/games/aliens/gui/GamePanel.java:140`](/Users/michaldziedzic/Priv/aliensattack/src/main/java/com/emenems/games/aliens/gui/GamePanel.java:140)
- `GamePanel.updateGameState(...)` is the established scalar push channel from controller/session into rendering, and recent features already extend it rather than letting the panel query gameplay state directly. [`src/main/java/com/emenems/games/aliens/gui/GamePanel.java:81`](/Users/michaldziedzic/Priv/aliensattack/src/main/java/com/emenems/games/aliens/gui/GamePanel.java:81)
- `GameController.tick()` advances only `PLAYING` timers and pushes panel state at the end of each tick, which is the right place to tick a temporary wave banner without affecting menus or Game Over. [`src/main/java/com/emenems/games/aliens/controller/GameController.java:277`](/Users/michaldziedzic/Priv/aliensattack/src/main/java/com/emenems/games/aliens/controller/GameController.java:277)
- Wave transitions already have one canonical trigger in `advanceWaveIfCleared()`, which increments the session wave and regenerates aliens. [`src/main/java/com/emenems/games/aliens/controller/GameController.java:455`](/Users/michaldziedzic/Priv/aliensattack/src/main/java/com/emenems/games/aliens/controller/GameController.java:455)
- `GameSession` already owns short-lived scalar feedback state such as hit feedback, rapid-fire timing, combo timing, and restart lifecycle resets, making it the natural owner for wave-message timing. [`src/main/java/com/emenems/games/aliens/GameSession.java:4`](/Users/michaldziedzic/Priv/aliensattack/src/main/java/com/emenems/games/aliens/GameSession.java:4)
- The roadmap defines S-05 as “clearer HUD and wave-start message” and warns that presentation changes must stay readable without obscuring gameplay. [`context/foundation/roadmap.md:126`](/Users/michaldziedzic/Priv/aliensattack/context/foundation/roadmap.md:126)

## Desired End State

During play, the player sees the same session information as today, but presented in a cleaner grouped HUD with improved contrast and spacing. When a new session starts at wave 1 or when the player clears into a later wave, a short-lived `WAVE <n>` banner appears near the top center of the board, then fades away on its own without pausing gameplay, consuming input, or changing wave timing.

The end state is correct when:

- The HUD remains readable on the existing 760x650 board and still shows score, wave, lives, and active rapid-fire/combo information.
- Starting a session and advancing to later waves both activate a visible top-centered wave banner.
- The banner expires automatically during `PLAYING` ticks only and is cleared by restart/Game Over lifecycle resets as appropriate.
- Existing automated tests remain green and manual play confirms the new presentation does not cover critical action.

## What We Are Not Doing

- No gameplay rebalance, pause mechanics, new controls, or changes to score/wave/life rules.
- No new images, fonts, external UI libraries, or asset pipeline changes.
- No broad refactor of `GameController`, `GameSession`, or rendering ownership.
- No full-screen overlay that blocks play or hides aliens, missiles, or the ship.
- No animation-heavy polish beyond a simple short-lived message lifecycle.

## Implementation Approach

Keep the existing ownership split. Add wave-message lifecycle state to `GameSession`, activate it from the existing start/restart and wave-advance paths in `GameController`, and pass the new scalar state through `GamePanel.updateGameState(...)`. In `GamePanel`, replace the loose text stack with a compact translucent HUD card in the top-left that preserves the same information hierarchy, then render a lightweight top-centered `WAVE <n>` banner while the session-owned timer is active.

## Critical Implementation Details

### State sequencing

Prime the wave-message timer inside the same session methods that establish the active wave (`startOrRestart()` for wave 1 and `advanceWave()` for later waves), then tick that timer only from the existing `PLAYING` branch in `GameController.tick()`. This keeps the banner aligned with the real current wave, prevents it from draining during `START_MENU` or `GAME_OVER`, and avoids adding special-case render logic outside the established scalar push path.

## Phase 1: Session-owned wave message lifecycle

### Overview

Introduce the minimal state needed to represent a temporary wave-start banner and wire it into the existing start/restart and wave-advance paths.

### Required Changes:

#### 1. Session wave-message state and lifecycle

**File**: `src/main/java/com/emenems/games/aliens/GameSession.java`

**Purpose**: Make wave-banner visibility part of the same scalar lifecycle that already owns hit feedback and temporary effect timers.

**Contract**: Add a small constant duration in ticks, fields/getters describing whether the wave message is active and how many ticks remain, activate that state in `startOrRestart()` for wave 1 and `advanceWave()` for later waves, decrement it with a dedicated tick method, and clear it on fresh lifecycle resets alongside the rest of session state.

#### 2. Controller timer and state push integration

**File**: `src/main/java/com/emenems/games/aliens/controller/GameController.java`

**Purpose**: Advance the new timer only while gameplay is active and expose it to rendering through the established push channel.

**Contract**: In the `PLAYING` branch of `tick()`, advance the wave-message timer alongside existing session timers before panel state is pushed. Extend `updatePanelState()` and the panel call site to pass the additional wave-message scalar state without changing rendering ownership or wave progression timing.

#### 3. Session and controller lifecycle tests

**Files**:
- `src/test/java/com/emenems/games/aliens/GameSessionTest.java`
- `src/test/java/com/emenems/games/aliens/controller/GameControllerTest.java`

**Purpose**: Lock the new lifecycle behavior before presentation changes are layered on top.

**Contract**: Add focused tests proving that a fresh start shows a wave-1 message, advancing a cleared wave shows the next-wave message, the timer expires after its configured number of `PLAYING` ticks, and non-playing states do not advance gameplay timers or corrupt the banner lifecycle.

### Success Criteria:

#### Automated verification:

- `GameSessionTest` covers wave-message activation on start/restart and wave advancement.
- `GameControllerTest` covers `PLAYING`-tick countdown behavior and cleared-wave activation.
- `./mvnw test`

#### Manual verification:

- Starting the game from the menu visibly triggers a wave-1 banner once play begins.
- Clearing a wave triggers the next-wave banner without pausing movement or firing.

**Implementation note**: After this phase and its automated checks pass, stop for human confirmation that the banner timing feels readable before moving to the final presentation pass.

---

## Phase 2: HUD cleanup and wave banner rendering

### Overview

Render the cleaner HUD and top-centered wave message using the new scalar state while preserving existing gameplay readability.

### Required Changes:

#### 1. HUD card layout and grouped rendering

**File**: `src/main/java/com/emenems/games/aliens/gui/GamePanel.java`

**Purpose**: Improve legibility without changing what information is available to the player.

**Contract**: Replace the loose left-aligned text stack with a compact top-left HUD treatment that groups score, wave, lives, and active temporary effects on a readable translucent backdrop with consistent spacing and contrast. Preserve the existing labels and conditional visibility rules for rapid fire and combo.

#### 2. Wave banner state ingestion and rendering

**File**: `src/main/java/com/emenems/games/aliens/gui/GamePanel.java`

**Purpose**: Make the start of each wave visible without blocking gameplay.

**Contract**: Extend `updateGameState(...)` and panel fields to retain the new wave-message scalar state. While active, draw a top-centered `WAVE <wave>` banner in the upper playfield using a readable non-fullscreen treatment that stays clear of the ship’s default action area and does not suppress sprite rendering or overlays.

#### 3. Panel-focused rendering helper tests

**File**: `src/test/java/com/emenems/games/aliens/gui/GamePanelTest.java`

**Purpose**: Keep the new presentation logic deterministic where practical without introducing fragile pixel-perfect tests.

**Contract**: Add pure/helper-level tests for any new HUD or wave-banner visibility/conversion helpers introduced as part of the panel cleanup, following the existing `rapidFireSecondsRemaining(...)`, `comboSecondsRemaining(...)`, and predicate-style test pattern.

#### 4. Full regression gate

**Files**:
- `src/test/java/com/emenems/games/aliens/GameSessionTest.java`
- `src/test/java/com/emenems/games/aliens/controller/GameControllerTest.java`
- `src/test/java/com/emenems/games/aliens/gui/GamePanelTest.java`

**Purpose**: Confirm the polish slice does not regress the shipped arcade loop.

**Contract**: Run the full compile/test gate and manually smoke-test menu start, normal play, wave clear, rapid-fire/combo HUD coexistence, hit feedback, Game Over, and restart with the updated presentation.

### Success Criteria:

#### Automated verification:

- `GamePanelTest` covers new helper behavior introduced for the HUD/banner presentation.
- Existing `GameSessionTest`, `GameControllerTest`, and `GamePanelTest` remain green with the new panel state signature.
- `./mvnw clean compile`
- `./mvnw test`

#### Manual verification:

- The HUD is easier to read at a glance and still shows score, wave, lives, rapid-fire, and combo status correctly.
- The wave banner appears near the top center for wave 1 and later waves, then disappears on its own while gameplay continues.
- The new presentation does not cover critical action during normal firing, alien movement, rapid fire, hit feedback, Game Over, or restart flows.

**Implementation note**: After this phase and its automated checks pass, stop for human confirmation that the revised HUD and banner feel readable in live play before considering the change complete.

---

## Testing Strategy

### Unit tests:

- `GameSessionTest` for wave-message activation, reset, and expiry lifecycle.
- `GameControllerTest` for countdown sequencing during `PLAYING` ticks and next-wave activation after a clear.
- `GamePanelTest` for any new pure helpers used to decide banner/HUD display behavior.

### Integration tests:

- Controller tests proving wave-clear progression still advances to the next wave while also priming the banner state.
- Full Maven compile/test gate after the panel state signature changes.

### Manual test steps:

1. Launch the game and press `ENTER`; confirm wave 1 begins with a visible top-centered banner.
2. Observe the top-left HUD during normal movement and firing; confirm base stats remain readable.
3. Trigger rapid fire and a combo; confirm those labels still appear correctly within the cleaned-up HUD.
4. Clear a wave; confirm the next wave spawns normally and the new banner appears briefly without pausing play.
5. Lose a life, then reach Game Over, then restart; confirm hit feedback, Game Over overlay, restart, and the new wave-1 banner all behave correctly.

## Performance Notes

This change adds only constant-time timer bookkeeping and a few extra draw calls per frame. The main risk is visual clutter rather than EDT load, so manual verification should focus on readability and whether the new HUD/banner treatment obscures active gameplay.

## Migration Notes

No data migration or compatibility work is required. This is an in-memory presentation and session-state enhancement inside the current desktop game process.

## References

- Roadmap slice definition: [`context/foundation/roadmap.md:126`](/Users/michaldziedzic/Priv/aliensattack/context/foundation/roadmap.md:126)
- Session lifecycle owner: [`src/main/java/com/emenems/games/aliens/GameSession.java:4`](/Users/michaldziedzic/Priv/aliensattack/src/main/java/com/emenems/games/aliens/GameSession.java:4)
- Tick sequencing and wave progression: [`src/main/java/com/emenems/games/aliens/controller/GameController.java:277`](/Users/michaldziedzic/Priv/aliensattack/src/main/java/com/emenems/games/aliens/controller/GameController.java:277)
- Existing panel state push and HUD rendering: [`src/main/java/com/emenems/games/aliens/gui/GamePanel.java:81`](/Users/michaldziedzic/Priv/aliensattack/src/main/java/com/emenems/games/aliens/gui/GamePanel.java:81)
- Prior temporary-effect HUD extension pattern: [`context/archive/2026-06-04-rapid-fire-power-up/plan.md`](/Users/michaldziedzic/Priv/aliensattack/context/archive/2026-06-04-rapid-fire-power-up/plan.md)
- Prior combo HUD extension pattern: [`context/archive/2026-06-04-skill-based-score-combo/plan.md`](/Users/michaldziedzic/Priv/aliensattack/context/archive/2026-06-04-skill-based-score-combo/plan.md)

## Progress

> Convention: `- [ ]` pending, `- [x]` completed. Add ` — <commit sha>` when a step lands. Do not rename step titles.

### Phase 1: Session-owned wave message lifecycle

#### Automated

- [x] 1.1 `GameSessionTest` covers wave-message activation on start/restart and wave advancement. — 54ae300
- [x] 1.2 `GameControllerTest` covers `PLAYING`-tick countdown behavior and cleared-wave activation. — 54ae300
- [x] 1.3 `./mvnw test` — 54ae300

#### Manual

- [x] 1.4 Starting the game from the menu visibly triggers a wave-1 banner once play begins. — 54ae300
- [x] 1.5 Clearing a wave triggers the next-wave banner without pausing movement or firing. — 54ae300

### Phase 2: HUD cleanup and wave banner rendering

#### Automated

- [x] 2.1 `GamePanelTest` covers new helper behavior introduced for the HUD/banner presentation. — 54ae300
- [x] 2.2 Existing session, controller, and panel tests remain green with the new panel state signature. — 54ae300
- [x] 2.3 `./mvnw clean compile` — 54ae300
- [x] 2.4 `./mvnw test` — 54ae300

#### Manual

- [x] 2.5 The HUD is easier to read at a glance and still shows score, wave, lives, rapid-fire, and combo status correctly. — 54ae300
- [x] 2.6 The wave banner appears near the top center for wave 1 and later waves, then disappears on its own while gameplay continues. — 54ae300
- [x] 2.7 The new presentation does not cover critical action during normal firing, alien movement, rapid fire, hit feedback, Game Over, or restart flows. — 54ae300
