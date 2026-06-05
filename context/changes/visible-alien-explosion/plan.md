# Visible Alien Explosion Implementation Plan

## Overview

Add a short visible explosion at each alien's position when a player missile destroys that alien. The effect should use a dedicated `explosion.png` asset, support multiple simultaneous kills, last about 12 playing ticks, and follow the existing shared-list rendering architecture.

## Current State Analysis

The game already plays an explosion sound after missile kills, but the destroyed alien disappears immediately with no visual death feedback. `GameController.checkCollisionsWithMissile()` batches destroyed aliens before removing them, which is the right point to create explosion effects from their final positions. `GamePanel` renders visible gameplay objects from shared mutable lists created in `Main`, while scalar session state is pushed separately through `updateGameState`.

## Desired End State

When one or more aliens are destroyed by player missiles, each destroyed alien leaves a visible explosion at its last `x/y` position for about 12 ticks. The explosion is rendered from `src/main/resources/images/explosion.png`, disappears automatically, is cleared on restart, and does not appear for non-lethal hits on a special alien or for ship-alien collisions. Existing scoring, combo, rapid-fire drops, sound effects, wave advancement, Game Over, and restart behavior remain unchanged.

### Key Findings

- Missile kill resolution already has the exact destroyed-alien set before removal and sound playback. `src/main/java/com/emenems/games/aliens/controller/GameController.java:268`
- `Main` creates shared lists once and passes the same references to `GamePanel` and `GameController`; this is the established pattern for renderable mutable objects. `src/main/java/com/emenems/games/aliens/Main.java:21`
- `GamePanel` currently loads sprites from `/images/...` and renders each gameplay list directly in `paintComponent`. `src/main/java/com/emenems/games/aliens/gui/GamePanel.java:98`
- Controller tests already cover one-hit standard kills, non-lethal special hits, lethal special hits, and same-tick duplicate hit behavior. `src/test/java/com/emenems/games/aliens/controller/GameControllerTest.java:83`

## What We Are NOT Doing

- No procedural explosion-only approach; this plan uses a dedicated `explosion.png` asset as decided.
- No explosion for ship-alien collision, alien invasion, offscreen cleanup, or non-lethal first hits on special aliens.
- No particle system, animation framework, external dependency, audio change, scoring change, or broad rendering refactor.
- No pixel-snapshot Swing tests; deterministic model/controller/panel helper tests are enough, with manual gameplay verification for the visual.

## Implementation Approach

Introduce a small `AlienExplosion` gameplay object with immutable position and a tick countdown. Create one shared `List<AlienExplosion>` in `Main`, pass it to `GamePanel` and `GameController`, add explosions when `aliensToRemove` is resolved, tick and remove expired effects during playing ticks, and clear the list on restart. `GamePanel` loads `explosion.png` and draws every active explosion at the object's current position using the existing component size.

## Phase 1: Explosion Model, Wiring, Rendering, and Tests

### Overview

Deliver the full visible alien explosion feature in one small phase, preserving existing gameplay contracts and adding focused regression coverage around effect creation and expiration.

### Required Changes

#### 1. Explosion Model

**File**: `src/main/java/com/emenems/games/aliens/gamemachines/AlienExplosion.java`

**Purpose**: Represent one visible explosion with position and a short deterministic lifetime.

**Contract**: Add a final class implementing `GameObject` with `x`, `y`, a 12-tick lifetime, `tick()`, and `isExpired()`; position must remain fixed for the effect's full lifetime.

#### 2. Shared List Wiring

**Files**:

- `src/main/java/com/emenems/games/aliens/Main.java`
- `src/main/java/com/emenems/games/aliens/gui/GamePanel.java`
- `src/main/java/com/emenems/games/aliens/controller/GameController.java`

**Purpose**: Thread explosion state through the same shared-list boundary already used by aliens, missiles, alien missiles, and rapid-fire power-ups.

**Contract**: `Main` creates `List<AlienExplosion> alienExplosions`. `GamePanel` accepts and stores it, with backward-compatible constructor overloads where useful for tests. `GameController` accepts and stores it, with existing public/package-private constructors delegating to an empty list when older tests do not care about explosions.

#### 3. Controller Lifecycle

**File**: `src/main/java/com/emenems/games/aliens/controller/GameController.java`

**Purpose**: Create, age, remove, and reset explosion effects without changing kill side effects.

**Contract**: For every alien in `aliensToRemove`, add a new `AlienExplosion` at that alien's current position before or alongside `aliens.removeAll(aliensToRemove)`. Tick active explosions during `PLAYING` ticks and remove expired effects. Clear explosions in `resetSession()`. Non-lethal special-alien hits must not create an explosion because they are not in the destroyed set.

#### 4. Explosion Sprite Rendering

**Files**:

- `src/main/resources/images/explosion.png`
- `src/main/java/com/emenems/games/aliens/gui/GamePanel.java`

**Purpose**: Make each active explosion visible at gameplay speed.

**Contract**: Add `explosion.png` under `src/main/resources/images/`. Load it in `GamePanel.loadImages()` using the existing classpath resource pattern. Draw active explosions after aliens/missiles and before HUD/overlays, at `GameConstants.COMPONENT_SIZE` square size, so the effect is visible without hiding menu or Game Over overlays.

#### 5. Tests and Project Guidance

**Files**:

- `src/test/java/com/emenems/games/aliens/gamemachines/AlienExplosionTest.java`
- `src/test/java/com/emenems/games/aliens/controller/GameControllerTest.java`
- `src/test/java/com/emenems/games/aliens/gui/GamePanelTest.java`
- `CLAUDE.md`

**Purpose**: Lock the new effect lifecycle and document ownership for future changes.

**Contract**: Add model tests for fixed position and 12-tick expiration. Add controller tests proving standard kills create explosions, lethal special kills create exactly one explosion, non-lethal special hits do not, multiple same-tick kills create multiple effects, effects expire after ticks, and restart clears them. Add a lightweight panel/helper or resource-existence test for the explosion asset. Update project guidance to mention that visible transient renderable effects follow the shared-list pattern.

### Success Criteria

#### Automated Verification

- Explosion model tests pass: `./mvnw test -Dtest=AlienExplosionTest`
- Controller regression tests pass: `./mvnw test -Dtest=GameControllerTest`
- Panel/resource tests pass: `./mvnw test -Dtest=GamePanelTest`
- Full test suite passes: `./mvnw test`
- Clean compilation passes: `./mvnw clean compile`
- Explosion asset exists: `test -f src/main/resources/images/explosion.png`

#### Manual Verification

- In the running game, destroying a standard alien shows a visible explosion where the alien disappeared.
- Destroying the special alien on its second hit shows one explosion, while its first hit does not.
- Rapid fire or same-tick multiple kills can show multiple simultaneous explosions.
- Explosions disappear quickly and do not persist after restart or Game Over/restart.
- Existing HUD, hit feedback, rapid fire, combo, wave advancement, sound, and restart behavior show no visible regression.

**Implementation note**: Manual verification is not auto-checked; do not mark manual progress complete until a human confirms it.

---

## Testing Strategy

### Unit Tests

- `AlienExplosion` keeps its starting `x/y`, reports active state before 12 ticks, and reports expired after 12 ticks.
- `GameController.checkCollisionsWithMissile()` creates one explosion per destroyed alien and none for damaged-but-surviving special aliens.
- `GameController.tick()` ages active explosions during play and removes expired ones.
- `resetSession()` clears active explosions along with projectiles and power-ups.

### Integration Tests

- Existing controller collision tests are extended to include explosion side effects without weakening score/combo/drop assertions.
- `GamePanelTest` verifies the explosion resource can be resolved or that the extracted draw predicate/helper treats active explosion objects as renderable.

### Manual Testing Steps

1. Run `./mvnw exec:java -Dexec.mainClass="com.emenems.games.aliens.Main"`.
2. Start the game and destroy a standard alien; confirm a short explosion appears at the alien's position.
3. Reach wave 2, hit the special alien once; confirm no explosion appears on the non-lethal hit.
4. Destroy the special alien with the second hit; confirm exactly one explosion appears.
5. Use rapid fire if available and confirm multiple quick kills can display multiple explosions.
6. Restart after Game Over and confirm no old explosions remain.

## Performance Considerations

The explosion list is tiny and each effect lives for only 12 ticks. Per-tick aging, removal, and rendering are linear in the number of active explosions, which is bounded naturally by the six-alien wave size and existing projectile collision rules.

## Migration Notes

No persistent data exists. The only asset migration is adding `src/main/resources/images/explosion.png` to the packaged resources.

## References

- Change identity: `context/changes/visible-alien-explosion/change.md`
- Existing kill resolution: `src/main/java/com/emenems/games/aliens/controller/GameController.java:268`
- Shared list wiring: `src/main/java/com/emenems/games/aliens/Main.java:21`
- Sprite loading and rendering: `src/main/java/com/emenems/games/aliens/gui/GamePanel.java:98`
- Collision regression tests: `src/test/java/com/emenems/games/aliens/controller/GameControllerTest.java:83`
- Related completed feedback work: `context/archive/2026-05-29-wave-boundaries-and-hit-feedback/plan.md`

## Progress

> Convention: `- [ ]` pending, `- [x]` done. Add ` — <commit sha>` when a step is committed. See `references/progress-format.md`.

### Phase 1: Explosion Model, Wiring, Rendering, and Tests

#### Automated

- [x] 1.1 Explosion model tests pass: `./mvnw test -Dtest=AlienExplosionTest`
- [x] 1.2 Controller regression tests pass: `./mvnw test -Dtest=GameControllerTest`
- [x] 1.3 Panel/resource tests pass: `./mvnw test -Dtest=GamePanelTest`
- [x] 1.4 Full test suite passes: `./mvnw test`
- [x] 1.5 Clean compilation passes: `./mvnw clean compile`
- [x] 1.6 Explosion asset exists: `test -f src/main/resources/images/explosion.png`

#### Manual

- [x] 1.7 In the running game, destroying a standard alien shows a visible explosion where the alien disappeared
- [x] 1.8 Destroying the special alien on its second hit shows one explosion, while its first hit does not
- [x] 1.9 Rapid fire or same-tick multiple kills can show multiple simultaneous explosions
- [x] 1.10 Explosions disappear quickly and do not persist after restart or Game Over/restart
- [x] 1.11 Existing HUD, hit feedback, rapid fire, combo, wave advancement, sound, and restart behavior show no visible regression
