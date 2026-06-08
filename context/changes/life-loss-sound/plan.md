# Life loss sound implementation plan

## Overview

Implement roadmap slice S-07 by adding a distinct procedural sound when the player actually loses a life. The sound should play for both alien-contact and alien-missile damage, including the final life, while preserving the existing zero-dependency audio model and silent fallback when audio is unavailable.

## Current State Analysis

The game already has the right structural seam for this slice: life loss is funneled through one controller helper, while audio remains a separate event-style API. What is missing is only a dedicated life-loss sound method plus focused controller tests that prove the trigger happens exactly on real life loss and not on shield consumption or alien invasion.

### Key findings:

- Roadmap S-07 defines the desired outcome as a separate life-loss sound that does not break safe silent gameplay without audio. `context/foundation/roadmap.md:157`
- The PRD marks FR-009 as a nice-to-have polish item: “The player can hear a distinct sound when losing a life.” `context/foundation/prd.md:99`
- `ArcadeSoundPlayer` currently exposes only `playShoot()`, `playExplosion()`, and background music methods; short effects are synthesized procedurally through the shared `playTone(...)` helper and all audio failures are swallowed. `src/main/java/com/emenems/games/aliens/audio/ArcadeSoundPlayer.java:13`
- Real life loss currently flows through `GameController.loseLife()`, which delegates to `session.loseLife()` and then handles Game Over cleanup if lives reach zero. `src/main/java/com/emenems/games/aliens/controller/GameController.java:857`
- Ship-vs-alien collision removes the alien and calls `loseLife()` only when shield is not consumed. `src/main/java/com/emenems/games/aliens/controller/GameController.java:430`
- Alien-missile collision removes the missile and calls `loseLife()` only when shield is not consumed. `src/main/java/com/emenems/games/aliens/controller/GameController.java:497`
- Alien invasion enters Game Over through a separate path without spending a life, so it should not trigger the new sound. `src/main/java/com/emenems/games/aliens/controller/GameController.java:737`
- The test suite already uses a package-local `CountingSoundPlayer` seam for controller audio assertions, but it only counts background music start/stop calls today. `src/test/java/com/emenems/games/aliens/controller/GameControllerTest.java:2481`

## Desired End State

Whenever the player loses a life because an unshielded hit lands, the game plays a short, clearly distinct life-loss sound. This applies to both alien body collision and alien missile collision, including the hit that reduces lives to zero and immediately transitions to Game Over.

Shield consumption, alien invasion Game Over, start menu, pause/resume, restart, shooting, explosion sounds, and background music behavior remain unchanged. If Java Sound is unavailable, the game still runs silently with no exception escaping into gameplay.

### Key discoveries:

- The right trigger point is the existing controller-owned `loseLife()` helper, not the two individual collision sites. `src/main/java/com/emenems/games/aliens/controller/GameController.java:857`
- `GameSession` is intentionally pure session state and should not grow audio responsibilities. `src/main/java/com/emenems/games/aliens/GameSession.java:1`
- The audio layer already has the exact extension pattern needed: one public event method per effect delegating to `playTone(...)`. `src/main/java/com/emenems/games/aliens/audio/ArcadeSoundPlayer.java:13`

## What We Are Not Doing

- No new audio assets, files, external libraries, mixers, or runtime dependencies.
- No new Game Over sting, shield-break sound, pause sound, or broader audio redesign.
- No changes to life rules, hit feedback timing, Game Over conditions, scoring, waves, or controls.
- No changes to README controls; the current README already documents `P` for pause/resume and needs no update for this slice.
- No direct audio work inside `GameSession`.

## Implementation Approach

Extend `ArcadeSoundPlayer` with one new public event method for life loss that synthesizes a lower, distinct short tone using the existing PCM generation path. Trigger that method exactly once from `GameController.loseLife()` after the session has actually consumed a life, so both existing damage sources inherit the sound automatically and shielded hits remain silent.

Keep verification controller-centric. Expand the existing test sound seam to count life-loss sound calls, then add focused tests proving the trigger fires for unshielded alien-body and alien-missile hits, including Game Over on the last life, while remaining absent for shield consumption and alien invasion.

## Critical Implementation Details

### Audio sequencing

Keep the new trigger in `GameController.loseLife()` rather than in `GameSession` or the individual collision methods. That preserves the current ownership split and guarantees a single source of truth for “a life was really lost,” including the last-life path where Game Over cleanup stops background music immediately afterward.

## Phase 1: Add the life-loss sound contract

### Overview

Introduce the new sound effect in the audio layer and wire it into the one existing life-loss controller path.

### Required Changes:

#### 1. Distinct life-loss audio event

**File**: `src/main/java/com/emenems/games/aliens/audio/ArcadeSoundPlayer.java`

**Purpose**: Add a dedicated sound event for life loss without changing the current runtime model.

**Contract**: Add a new public method for life-loss playback that uses the existing procedural `playTone(...)` path, stays clearly distinct from `playShoot()` and `playExplosion()`, and preserves the current silent fallback when audio cannot play.

#### 2. Centralized life-loss trigger

**File**: `src/main/java/com/emenems/games/aliens/controller/GameController.java`

**Purpose**: Ensure every real life loss triggers the sound exactly once, regardless of which current damage source caused it.

**Contract**: Trigger the new sound from the controller-owned `loseLife()` helper, after the session spends a life and before or alongside existing Game Over cleanup logic. Shielded hits must remain silent because they never reach `loseLife()`. Alien invasion must remain silent because it bypasses life loss entirely.

### Success Criteria:

#### Automated verification:

- `./mvnw clean compile`
- Existing audio call sites still compile with the new `ArcadeSoundPlayer` API.

#### Manual verification:

- During normal play, taking an unshielded hit produces a distinct sound that is audibly different from shooting and alien-destruction sounds.
- The final hit that ends the session still produces the life-loss sound before the Game Over screen takes over.
- Running without a working audio device still leaves gameplay functional and exception-free.

**Implementation note**: After this phase and its automated checks pass, stop for human confirmation that the new tone is distinct enough and not overly harsh in the live Swing app.

---

## Phase 2: Lock trigger semantics with tests

### Overview

Expand the existing controller audio seam and prove the new sound fires only on true life loss.

### Required Changes:

#### 1. Counting sound seam extension

**File**: `src/test/java/com/emenems/games/aliens/controller/GameControllerTest.java`

**Purpose**: Make controller tests able to assert the new sound effect directly.

**Contract**: Extend `CountingSoundPlayer` with a life-loss counter by overriding the new audio method. Existing background music counters remain intact.

#### 2. Positive controller coverage

**File**: `src/test/java/com/emenems/games/aliens/controller/GameControllerTest.java`

**Purpose**: Prove both current damage sources inherit the new sound automatically.

**Contract**: Add focused tests showing one life-loss sound call for an unshielded alien missile hit, one call for an unshielded ship-vs-alien collision, and one call when the hit also causes Game Over on the last life.

#### 3. Negative controller coverage

**File**: `src/test/java/com/emenems/games/aliens/controller/GameControllerTest.java`

**Purpose**: Prevent false positives on nearby but semantically different events.

**Contract**: Add focused tests showing no life-loss sound call when shield absorbs an alien missile hit, when shield absorbs a ship-vs-alien collision, and when alien invasion ends the session without spending a life.

### Success Criteria:

#### Automated verification:

- `GameControllerTest` covers life-loss sound on unshielded alien missile damage.
- `GameControllerTest` covers life-loss sound on unshielded ship-vs-alien damage.
- `GameControllerTest` covers life-loss sound on the last-life Game Over hit.
- `GameControllerTest` covers no life-loss sound when shield absorbs damage.
- `GameControllerTest` covers no life-loss sound on alien invasion Game Over.
- `./mvnw test`

#### Manual verification:

- Repeated unshielded hits produce one sound per life lost, not duplicate sounds for a single event.
- Shielded hits still feel silent apart from existing visual feedback.
- Shooting, explosions, pause/resume audio, and Game Over music stop behavior show no audible regression.

**Implementation note**: After this phase and its automated checks pass, stop for human confirmation that the trigger semantics match expected gameplay feedback in a full session.

---

## Testing Strategy

### Unit tests:

- Use `GameControllerTest` as the primary safety net because the new behavior is controller-owned orchestration rather than session math.
- Reuse deterministic, no-window, no-real-audio controller tests with injected `CountingSoundPlayer`.
- Avoid adding brittle tests against synthesized PCM sample contents; the behavior contract is trigger/no-trigger, not waveform shape.

### Integration tests:

- Run full Maven compile and test gates because the audio API surface changes and controller tests cover the wiring.

### Manual test steps:

1. Launch the game with `./mvnw exec:java -Dexec.mainClass="com.emenems.games.aliens.Main"`.
2. Start a session and deliberately take an alien missile hit without a shield; confirm one distinct life-loss sound plays.
3. Start another case with direct ship-vs-alien contact; confirm the same life-loss sound plays.
4. Reach the final remaining life and take a hit; confirm the life-loss sound still plays before Game Over.
5. Collect a shield and take one hit; confirm life is not lost and the life-loss sound does not play.
6. Let aliens invade the bottom edge; confirm Game Over occurs without the life-loss sound.
7. Fire missiles, destroy aliens, pause/resume, and restart; confirm shoot, explosion, and background music behavior still feels unchanged.

## Performance Notes

This slice adds one more short synthesized tone on infrequent damage events. The cost is negligible relative to the existing procedural shoot/explosion effects, and the implementation should continue to avoid blocking the EDT or introducing new long-lived audio resources.

## Migration Notes

No data or save migration is required. The change is confined to in-memory gameplay orchestration and procedural audio playback.

## References

- Roadmap slice: `context/foundation/roadmap.md:157`
- PRD requirement: `context/foundation/prd.md:99`
- Audio event API and silent fallback: `src/main/java/com/emenems/games/aliens/audio/ArcadeSoundPlayer.java:13`
- Ship collision life-loss path: `src/main/java/com/emenems/games/aliens/controller/GameController.java:430`
- Alien missile life-loss path: `src/main/java/com/emenems/games/aliens/controller/GameController.java:497`
- Central life-loss helper: `src/main/java/com/emenems/games/aliens/controller/GameController.java:857`
- Alien invasion Game Over path: `src/main/java/com/emenems/games/aliens/controller/GameController.java:737`
- Existing counting sound seam: `src/test/java/com/emenems/games/aliens/controller/GameControllerTest.java:2481`

## Progress

> Convention: `- [ ]` pending, `- [x]` completed. Add ` — <commit sha>` when a step lands. Do not rename step titles. See `references/progress-format.md`.

### Phase 1: Add the life-loss sound contract

#### Automated

- [x] 1.1 `./mvnw clean compile`
- [x] 1.2 Existing audio call sites still compile with the new `ArcadeSoundPlayer` API.

#### Manual

- [x] 1.3 Unshielded hits produce a distinct life-loss sound during play.
- [x] 1.4 The final hit still produces the life-loss sound before Game Over.
- [ ] 1.5 Gameplay remains functional and silent when no audio device is available.

### Phase 2: Lock trigger semantics with tests

#### Automated

- [x] 2.1 `GameControllerTest` covers life-loss sound on unshielded alien missile damage.
- [x] 2.2 `GameControllerTest` covers life-loss sound on unshielded ship-vs-alien damage.
- [x] 2.3 `GameControllerTest` covers life-loss sound on the last-life Game Over hit.
- [x] 2.4 `GameControllerTest` covers no life-loss sound when shield absorbs damage.
- [x] 2.5 `GameControllerTest` covers no life-loss sound on alien invasion Game Over.
- [x] 2.6 `./mvnw test`

#### Manual

- [x] 2.7 Repeated unshielded hits produce one sound per life lost.
- [x] 2.8 Shielded hits stay silent aside from existing visual feedback.
- [ ] 2.9 Shooting, explosions, pause/resume audio, and Game Over music behavior show no audible regression.
