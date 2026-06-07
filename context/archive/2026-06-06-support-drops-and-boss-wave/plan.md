# Support drops and boss wave implementation plan

## Overview

Implement roadmap slice S-10 by extending the existing rapid-fire drop pattern into a small support-drop system and adding a boss-only milestone wave every fifth wave. The player can collect rare extra-life, shield, or speed-boost drops during regular waves; every fifth wave replaces the normal alien group with one 20-hit boss that patrols horizontally, fires bursts, shows a health bar, and pays out extra score plus a guaranteed life-or-shield reward when destroyed.

## Current State Analysis

The game already has the exact architectural seams this feature should use: shared mutable lists for renderable objects, controller-owned entity orchestration, session-owned scalar effects, pure rule calculations in `GameRules`, and passive HUD rendering in `GamePanel`. Rapid-fire is currently implemented as a dedicated falling entity plus scalar timer, while special aliens already prove that alien type, health, damage state, and movement can live on the shared `Alien` model without introducing a parallel enemy list.

The current baseline is green on 2026-06-06: `./mvnw clean compile` passes, and `./mvnw test` passes with 135 tests.

## Desired End State

Regular waves continue to spawn six aliens using the current standard/special composition. Destroyed non-boss aliens have one rare support-drop roll; if the roll succeeds, the drop type is selected from weighted `RAPID_FIRE`, `EXTRA_LIFE`, `SHIELD`, and `SPEED_BOOST` choices. Extra life adds one life up to a maximum of 5, shield blocks one incoming damage event, and speed boost increases ship movement for 3 seconds. Rapid-fire keeps its existing duration, cooldown effect, wave-preservation, life-loss reset, and HUD semantics.

Every fifth wave is a boss-only wave. The boss has 20 HP, patrols horizontally in the upper board, does not descend into the player and does not use the existing alien-invasion lose condition, fires burst shots using existing alien missiles, and is defeated only by enough player-missile hits. On defeat, the boss awards an additional boss score bonus and spawns one guaranteed life-or-shield drop. The boss has a visible health bar, and active support effects are shown as compact HUD rows.

### Key Findings

- `GameController.tick()` owns the playing-only pipeline, including effect timers, ship movement, alien movement, projectile movement, collisions, cleanup, wave advancement, and panel state push (`src/main/java/com/emenems/games/aliens/controller/GameController.java:371`).
- Player missile collision already deduplicates hit missiles and destroyed aliens before applying destruction side effects (`src/main/java/com/emenems/games/aliens/controller/GameController.java:433`).
- Current rapid-fire drops are spawned from resolved destroyed aliens, moved and cleaned up by the controller, and collected through ship collision (`src/main/java/com/emenems/games/aliens/controller/GameController.java:484`, `src/main/java/com/emenems/games/aliens/controller/GameController.java:752`).
- `GameSession` owns lives, wave, rapid-fire, combo, hit feedback, wave message, and reset lifecycle (`src/main/java/com/emenems/games/aliens/GameSession.java:3`).
- `GamePanel.updateGameState(...)` is the scalar push channel for HUD-visible state (`src/main/java/com/emenems/games/aliens/gui/GamePanel.java:92`).
- `Alien` already carries type, health, damaged state, and type-specific movement for special aliens (`src/main/java/com/emenems/games/aliens/gamemachines/Alien.java:7`).
- `GameObject` is sealed and currently permits `RapidFirePowerUp`, so a generalized power-up class will need an explicit permits update (`src/main/java/com/emenems/games/aliens/gamemachines/GameObject.java:3`).
- The roadmap defines S-10 as rare life/shield/speed drops plus a boss every fifth wave, with balance and readability as the main risks (`context/foundation/roadmap.md:194`).

## What We Are NOT Doing

- No additional boss types, multiple bosses, multi-phase boss AI, angled projectiles, new projectile classes, or full bullet-hell patterns.
- No boss body collision damage, boss invasion Game Over, or downward boss approach; the boss threatens the player only through missiles.
- No permanent ship upgrades, inventory, player-selected power-ups, or stacking shield charges.
- No new external runtime dependency, game engine, mocking library, image library, or broad architecture refactor.
- No persistence changes for profiles or best scores beyond the existing score value increasing from boss bonus points.
- No pixel-perfect Swing screenshot tests; rendering remains covered by helper tests plus manual gameplay verification.

## Implementation Approach

Preserve the existing architecture and generalize only where this feature creates real duplication. Replace the dedicated `RapidFirePowerUp` concept with a generalized falling `PowerUp` plus `PowerUpType`, while keeping compatibility through focused migration of existing rapid-fire tests and controller constructors. Let `GameSession` own scalar effects: life cap, shield availability, speed boost timer, and existing rapid-fire timer. Let `Spaceship` expose movement with an explicit speed multiplier or step choice but not own timed effect state.

Represent the boss as an explicit supported alien identity rather than a separate enemy list. The lowest-risk path is extending `AlienType` with `BOSS` and extending `Alien` with boss health and horizontal patrol behavior, while keeping boss-specific wave composition, burst firing, scoring, guaranteed reward, and health-bar state under controller/rules/panel ownership. `GameRules` should hold the pure tuning contracts: boss wave cadence, boss HP, boss score bonus, support-drop chance and weights, max lives, shield capacity, speed duration, and boss burst parameters.

## Critical Implementation Details

### State sequencing

Boss defeat must apply in this order: consume the lethal hit, remove the boss, award normal or boss-specific score exactly once, create explosion/visual defeat feedback, spawn the guaranteed reward, then advance the wave on the next cleared-wave check. If the boss is removed before bonus/drop logic runs, the wave-advance path may regenerate the next wave and hide the reward.

### Timing and lifecycle

Speed boost and rapid-fire timers should tick only in the existing `PLAYING` branch. Shield has no countdown; it is consumed by the next damage event. Boss waves should reuse `session.advanceWave()` and the existing wave banner, but `generateSpaceObjects()` must choose boss-only composition for waves where `GameRules.isBossWave(wave)` is true.

### User experience specification

The boss never reaches the ship by movement. It should remain in an upper horizontal patrol lane, so any player damage during boss waves comes from alien missiles. The health bar should make 20 HP legible without adding damage numbers or a large overlay.

## Phase 1: Shared support-drop model and session effects

### Overview

Create a generalized support-drop model and add session-owned life, shield, and speed-boost behavior before touching boss logic.

### Required Changes

#### 1. Power-up identity and falling model

**Files**:
- `src/main/java/com/emenems/games/aliens/gamemachines/PowerUpType.java`
- `src/main/java/com/emenems/games/aliens/gamemachines/PowerUp.java`
- `src/main/java/com/emenems/games/aliens/gamemachines/GameObject.java`
- `src/test/java/com/emenems/games/aliens/gamemachines/PowerUpTest.java`

**Purpose**: Replace the single-purpose rapid-fire collectible with a shared model for all falling support drops.

**Contract**: Add `PowerUpType` values `RAPID_FIRE`, `EXTRA_LIFE`, `SHIELD`, and `SPEED_BOOST`. Add final `PowerUp` implementing `GameObject`, with constructor-provided type/x/y, stable x, downward movement at the existing drop speed, and getters for type and coordinates. Update the sealed `GameObject` permits list. Migrate or remove `RapidFirePowerUp` only after all references use `PowerUp`.

#### 2. Session-owned support effects

**Files**:
- `src/main/java/com/emenems/games/aliens/GameSession.java`
- `src/main/java/com/emenems/games/aliens/GameRules.java`
- `src/test/java/com/emenems/games/aliens/GameSessionTest.java`
- `src/test/java/com/emenems/games/aliens/GameRulesTest.java`

**Purpose**: Keep support-effect lifecycle with the scalar session state that already owns lives, rapid-fire, combo, and wave messages.

**Contract**: Add max lives of 5, shield-active boolean, and 3-second speed-boost timer. Extra life increments lives only while below 5. Shield activation sets one available shield charge; repeated shield collection refreshes to one charge, not a stack. Speed boost activation refreshes its timer to 180 playing ticks. Life loss and start/restart clear rapid-fire, shield, speed boost, and combo; wave advancement preserves active rapid-fire, shield, and speed boost unless tests reveal an existing contrary invariant.

#### 3. Ship movement contract for speed boost

**Files**:
- `src/main/java/com/emenems/games/aliens/gamemachines/Spaceship.java`
- `src/main/java/com/emenems/games/aliens/controller/GameController.java`
- `src/test/java/com/emenems/games/aliens/gamemachines/SpaceshipTest.java`

**Purpose**: Make temporary speed boost affect movement without storing timed gameplay state on the ship.

**Contract**: Preserve the normal movement step. Add a movement path that allows the controller to apply a boosted step while `GameSession` reports speed boost active. Bounds clamping remains unchanged and still happens after all held-key movement for a tick.

### Success Criteria

#### Automated Verification

- Project compiles: `./mvnw clean compile`
- Focused tests pass: `./mvnw test -Dtest=PowerUpTest,GameSessionTest,GameRulesTest,SpaceshipTest`
- Tests verify all power-up types retain type and move downward.
- Tests verify extra life respects the 5-life cap.
- Tests verify shield activation, one-event consumption, restart reset, and no stacking beyond one charge.
- Tests verify speed boost activates for 180 ticks, refreshes, expires, and resets on life loss/restart.
- Tests verify boosted movement uses a larger step but still clamps to board bounds.

#### Manual Verification

- Code review confirms timed support effect state is not stored in `Spaceship` or `GamePanel`.
- Code review confirms `RapidFirePowerUp` has been fully migrated or intentionally retained only as a compatibility wrapper with no duplicate behavior.

**Implementation note**: After automated verification passes, stop for human confirmation before integrating support drops into live collision flow.

---

## Phase 2: Support-drop controller integration

### Overview

Wire the generalized support drops through resolved alien kills, movement, cleanup, collection, cooldown choice, shield damage interception, and HUD state push.

### Required Changes

#### 1. Shared support-drop list wiring

**Files**:
- `src/main/java/com/emenems/games/aliens/Main.java`
- `src/main/java/com/emenems/games/aliens/controller/GameController.java`
- `src/main/java/com/emenems/games/aliens/gui/GamePanel.java`
- `src/test/java/com/emenems/games/aliens/controller/GameControllerTest.java`

**Purpose**: Keep support drops in the established shared-list model used by aliens, missiles, explosions, and rapid-fire drops.

**Contract**: Replace `List<RapidFirePowerUp>` wiring with `List<PowerUp>` in `Main`, controller constructors, panel constructors, and test helpers. Reset clears uncollected support drops. Movement and cleanup process the generalized list once per playing tick.

#### 2. Rare weighted support-drop spawning

**Files**:
- `src/main/java/com/emenems/games/aliens/GameRules.java`
- `src/main/java/com/emenems/games/aliens/controller/GameController.java`
- `src/test/java/com/emenems/games/aliens/GameRulesTest.java`
- `src/test/java/com/emenems/games/aliens/controller/GameControllerTest.java`

**Purpose**: Add one controlled source of support-drop randomness without increasing drop volume unpredictably.

**Contract**: Each destroyed non-boss alien receives one rare drop roll. If successful, select a type from weighted support-drop rules. The ordinary pool includes rapid-fire, extra life, shield, and speed boost. Drop spawning must use the resolved destroyed-alien set, so non-lethal hits, duplicate same-tick hits, and raw collision pairs cannot create extra drops.

#### 3. Collection effects and damage interception

**Files**:
- `src/main/java/com/emenems/games/aliens/controller/GameController.java`
- `src/test/java/com/emenems/games/aliens/controller/GameControllerTest.java`

**Purpose**: Make each support drop meaningful during play while preserving existing life-loss and cooldown semantics.

**Contract**: Ship/power-up collision removes one collected drop and activates the effect by type. Rapid-fire keeps the existing cooldown behavior. Extra life applies through `GameSession`. Shield intercepts one incoming damage event from an alien missile or ship-alien collision, removes the damaging object where appropriate, triggers no life loss, and clears shield. Speed boost affects controller-applied movement while active and does not retroactively modify positions or cooldowns.

#### 4. Panel state push for new effects

**Files**:
- `src/main/java/com/emenems/games/aliens/controller/GameController.java`
- `src/main/java/com/emenems/games/aliens/gui/GamePanel.java`
- `src/test/java/com/emenems/games/aliens/gui/GamePanelTest.java`

**Purpose**: Expose active shield and speed boost state to rendering through the existing scalar channel.

**Contract**: Extend `GamePanel.updateGameState(...)` with shield active, speed boost active, and speed boost ticks. Preserve existing score, wave, lives, rapid-fire, combo, profile, and pause/game-over state. Add helper tests for effect visibility and speed seconds remaining.

### Success Criteria

#### Automated Verification

- Focused controller/panel tests pass: `./mvnw test -Dtest=GameControllerTest,GamePanelTest`
- Tests verify successful and failed drop rolls, weighted type selection, and one roll per resolved non-boss kill.
- Tests verify each drop type activates the correct session/controller behavior.
- Tests verify shield blocks exactly one alien missile damage event and exactly one ship-alien collision damage event.
- Tests verify shield does not block alien invasion from non-boss aliens.
- Tests verify speed boost changes held-key movement only while active.
- Tests verify rapid-fire behavior remains compatible with the generalized drop list.
- Tests verify drops and active effects reset on restart and do not tick outside `PLAYING`.

#### Manual Verification

- During normal waves, all four drop types are visually collectible and produce understandable effects.
- Lives never exceed 5.
- Shield consumption is visible enough to understand why a hit did not reduce lives.
- Speed boost feels helpful without making movement uncontrollable.

**Implementation note**: Keep the ordinary support-drop chance conservative; the exact tuning can live in `GameRules`, but the implementation must make it easy to adjust without editing collision code.

---

## Phase 3: Boss model and boss-only wave

### Overview

Add a boss alien identity, boss-only wave generation every fifth wave, 20-hit durability, horizontal patrol, boss scoring, and guaranteed life-or-shield reward.

### Required Changes

#### 1. Boss rules

**Files**:
- `src/main/java/com/emenems/games/aliens/GameRules.java`
- `src/test/java/com/emenems/games/aliens/GameRulesTest.java`

**Purpose**: Centralize boss cadence and balance values.

**Contract**: Add pure rules for boss wave cadence (`wave % 5 == 0`), boss HP `20`, boss score bonus, boss horizontal movement tuning, boss burst tuning placeholders, and guaranteed boss reward type selection limited to extra life or shield. Existing score, combo, special-alien, and ordinary alien-speed rules remain unchanged unless explicitly covered by tests.

#### 2. Boss alien state and movement

**Files**:
- `src/main/java/com/emenems/games/aliens/gamemachines/AlienType.java`
- `src/main/java/com/emenems/games/aliens/gamemachines/Alien.java`
- `src/test/java/com/emenems/games/aliens/gamemachines/AlienTest.java`

**Purpose**: Represent the boss in the same shared alien list while preserving standard and special alien behavior.

**Contract**: Add boss identity with 20 HP, damage state, and horizontal patrol movement constrained to the upper board. Boss movement does not descend toward the player after spawn. Boss gets hit by player missiles one hit at a time; only the lethal hit reports destruction. Existing standard and special alien constructors/behavior remain compatible.

#### 3. Boss-only wave generation and completion

**Files**:
- `src/main/java/com/emenems/games/aliens/controller/GameController.java`
- `src/test/java/com/emenems/games/aliens/controller/GameControllerTest.java`

**Purpose**: Turn every fifth wave into a clear milestone encounter without mixing regular wave semantics.

**Contract**: `generateSpaceObjects()` spawns exactly one boss on boss waves and no regular/special aliens. Non-boss waves keep the current six-alien composition. Boss waves still activate the wave banner. Boss does not trigger `checkAlienInvasion()` while patrolling horizontally. Clearing the boss advances to the next normal wave through the existing cleared-wave path.

#### 4. Boss defeat score and guaranteed reward

**Files**:
- `src/main/java/com/emenems/games/aliens/controller/GameController.java`
- `src/main/java/com/emenems/games/aliens/GameSession.java`
- `src/test/java/com/emenems/games/aliens/controller/GameControllerTest.java`
- `src/test/java/com/emenems/games/aliens/GameSessionTest.java`

**Purpose**: Make boss defeat materially rewarding without merging boss rewards into ordinary random drops.

**Contract**: The lethal boss hit awards a boss-specific bonus in addition to any normal kill score contract selected in `GameRules`. It spawns one guaranteed `EXTRA_LIFE` or `SHIELD` drop at the boss position using a deterministic random selection. Boss defeat must not also receive an ordinary rare drop roll unless explicitly encoded as a separate tested rule; the default plan excludes ordinary random boss drops to keep reward volume controlled.

### Success Criteria

#### Automated Verification

- Focused tests pass: `./mvnw test -Dtest=AlienTest,GameRulesTest,GameSessionTest,GameControllerTest`
- Tests verify waves 1-4 keep normal composition and wave 5 contains exactly one boss.
- Tests verify boss has 20 HP, requires 20 player-missile hits, and is removed only on the lethal hit.
- Tests verify boss patrols horizontally and remains in the upper board.
- Tests verify boss does not trigger alien invasion.
- Tests verify boss defeat awards the boss score bonus once.
- Tests verify boss defeat spawns exactly one guaranteed extra-life-or-shield drop.
- Tests verify clearing boss wave advances to wave 6 with normal composition.

#### Manual Verification

- Wave 5 clearly feels like a boss-only milestone, not a normal wave with a hidden special case.
- The 20-hit fight is readable and does not stall due to movement or hit feedback ambiguity.
- The guaranteed reward and bonus score are visible in gameplay outcomes.

**Implementation note**: Stop after this phase for human confirmation that the 20-hit boss duration feels acceptable before tuning burst firing and final rendering polish.

---

## Phase 4: Boss burst attack and shield-safe damage

### Overview

Give the boss its burst-shot pressure while keeping projectiles readable, bounded, and compatible with shield/life-loss rules.

### Required Changes

#### 1. Boss burst firing

**Files**:
- `src/main/java/com/emenems/games/aliens/GameRules.java`
- `src/main/java/com/emenems/games/aliens/controller/GameController.java`
- `src/test/java/com/emenems/games/aliens/GameRulesTest.java`
- `src/test/java/com/emenems/games/aliens/controller/GameControllerTest.java`

**Purpose**: Make the boss dangerous through missiles rather than body contact or descent.

**Contract**: Boss firing uses existing `AlienMissile` objects. A boss firing event creates a small burst, recommended as 2-3 vertically falling missiles with horizontal offsets, subject to a boss-aware active alien-missile cap. Bursts must not create angled projectiles or a new missile class. Boss burst timing should be tunable through `GameRules`.

#### 2. Projectile cap and fairness constraints

**Files**:
- `src/main/java/com/emenems/games/aliens/controller/GameController.java`
- `src/test/java/com/emenems/games/aliens/controller/GameControllerTest.java`

**Purpose**: Keep burst shots from producing unavoidable projectile walls on the 760x650 board.

**Contract**: Preserve the normal enemy missile cap for non-boss waves or define a separate tested boss cap. No tick may add a burst that exceeds the cap. Burst missile x positions must be clamped inside the board and spaced enough to be visually separable at component size.

#### 3. Shield and life-loss regression around bursts

**Files**:
- `src/main/java/com/emenems/games/aliens/controller/GameController.java`
- `src/test/java/com/emenems/games/aliens/controller/GameControllerTest.java`

**Purpose**: Ensure burst pressure interacts cleanly with one-event shield behavior and existing Game Over flow.

**Contract**: If a shielded player is hit by a burst missile, exactly one colliding missile is removed and the shield is consumed without losing a life. Later missiles can damage normally. Game Over score handling and profile-best-score update still happen once when lives reach zero.

### Success Criteria

#### Automated Verification

- Boss burst tests pass: `./mvnw test -Dtest=GameRulesTest,GameControllerTest`
- Tests verify boss firing creates the expected burst count when under cap.
- Tests verify burst creation respects the active missile cap.
- Tests verify burst missiles remain inside the board and use existing alien missile movement/cleanup.
- Tests verify shield blocks one burst hit and later hits reduce lives normally.
- Tests verify boss projectile deaths still enter Game Over and profile score handling once.

#### Manual Verification

- Boss burst pressure is clearly different from normal alien fire.
- Dodging remains possible at normal speed and noticeably easier during speed boost.
- Shield consumption during boss bursts is understandable from HUD/visual feedback.

**Implementation note**: Do not add new projectile physics in this phase; if vertical bursts are not expressive enough, that is a later scoped change.

---

## Phase 5: Rendering, HUD, guidance, and full regression

### Overview

Make support drops, active effects, boss identity, boss health, and reward outcomes readable, then run the full automated and manual regression gates.

### Required Changes

#### 1. Support drop and effect rendering

**Files**:
- `src/main/java/com/emenems/games/aliens/gui/GamePanel.java`
- `src/test/java/com/emenems/games/aliens/gui/GamePanelTest.java`

**Purpose**: Let the player distinguish support drops and active effects without adding assets or clutter.

**Contract**: Render each `PowerUpType` with distinct Swing primitive color/symbol treatment inside the normal component-sized area. HUD shows compact active-effect rows for rapid-fire and speed boost with seconds remaining, plus shield active state. HUD height calculation accounts for all visible rows without overlapping the playfield labels.

#### 2. Boss rendering and health bar

**Files**:
- `src/main/java/com/emenems/games/aliens/gui/GamePanel.java`
- `src/test/java/com/emenems/games/aliens/gui/GamePanelTest.java`

**Purpose**: Make the boss and its remaining health immediately legible.

**Contract**: Render boss distinctly from standard and special aliens, using either a larger primitive treatment or a clearly different overlay around the existing alien sprite. Draw a boss health bar while a boss is alive, based on boss current HP and max HP. Health-bar helpers should be deterministic and tested without pixel snapshots.

#### 3. Project guidance

**File**: `CLAUDE.md`

**Purpose**: Preserve the new ownership and sequencing rules for future agents.

**Contract**: Document generalized support drops, session-owned shield/speed/life-cap effects, boss-only wave cadence, boss threat model, and the rule that boss rewards and score must be applied before wave advancement regenerates the next wave.

#### 4. Full regression

**Files**:
- `src/test/java/com/emenems/games/aliens/GameRulesTest.java`
- `src/test/java/com/emenems/games/aliens/GameSessionTest.java`
- `src/test/java/com/emenems/games/aliens/controller/GameControllerTest.java`
- `src/test/java/com/emenems/games/aliens/gui/GamePanelTest.java`

**Purpose**: Prove this large replayability slice does not regress the shipped arcade loop.

**Contract**: Run focused tests, then the canonical `./mvnw clean compile` and `./mvnw test` gates. Manual testing must cover normal waves, support drops, boss wave, boss reward, profiles/best score, pause/resume, Game Over, and restart.

### Success Criteria

#### Automated Verification

- Presentation helpers pass: `./mvnw test -Dtest=GamePanelTest`
- Core focused tests pass: `./mvnw test -Dtest=GameRulesTest,GameSessionTest,GameControllerTest`
- Project compiles: `./mvnw clean compile`
- Full test suite passes: `./mvnw test`
- No new dependency is added to `pom.xml`.
- Existing profile, pause, combo, rapid-fire, special alien, explosion, score, wave, life, restart, and audio-safe behavior tests remain green.

#### Manual Verification

- Destroying regular aliens can produce rare rapid-fire, life, shield, and speed drops.
- Extra life respects the 5-life cap.
- Shield blocks exactly one damage event and then disappears from HUD.
- Speed boost lasts 3 seconds and makes dodging boss bursts easier without breaking bounds.
- Wave 5 is boss-only, the boss has a readable health bar, takes 20 hits, and never descends into contact/invasion.
- Boss burst shots are readable and fair.
- Killing the boss awards extra points and creates one guaranteed life-or-shield drop.
- Wave 6 resumes normal wave composition.
- Start menu, profile selection, scoring, combo, rapid-fire, pause/resume, Game Over, best score, restart, and safe audio behavior still work.

**Implementation note**: Do not mark manual progress complete until a human runs the Swing game with a display and confirms the checklist.

---

## Testing Strategy

### Unit Tests

- `PowerUpTest`: type identity, coordinates, and falling movement.
- `AlienTest`: boss construction, 20 HP damage lifecycle, horizontal patrol, standard/special regression.
- `GameRulesTest`: max lives, support-drop chance/weights, speed duration, boss cadence, boss HP, boss bonus, boss reward selection, burst tuning.
- `GameSessionTest`: extra-life cap, shield activation/consumption/reset, speed boost timer, rapid-fire regression, life loss and restart reset behavior.
- `GamePanelTest`: HUD row visibility, seconds conversion, support-drop visual helpers, boss health-bar percentage helpers, boss visibility predicates.

### Integration Tests

- `GameControllerTest`: resolved-kill support drops, collection effects, shield interception, speed movement, boss wave generation, boss combat, boss reward, boss scoring, burst firing, wave advancement, restart cleanup, and interaction with profiles/Game Over.
- Full `./mvnw test` remains the project-wide regression gate.

### Manual Testing Steps

1. Run `./mvnw exec:java -Dexec.mainClass="com.emenems.games.aliens.Main"`.
2. Start a profile-backed session and play regular waves until support drops appear.
3. Collect each drop type and verify rapid-fire, extra-life cap, one-hit shield, and 3-second speed boost.
4. Build combo and collect rapid-fire/support drops together; verify HUD rows remain readable.
5. Clear to wave 5 and verify it contains exactly one horizontally patrolling boss.
6. Fight the boss; verify 20 hits are required, the health bar updates, burst shots are readable, and the boss never descends into the ship.
7. Verify shield blocks one boss missile hit and speed boost helps dodge without leaving bounds.
8. Kill the boss; verify bonus points, guaranteed life-or-shield drop, and transition to wave 6.
9. Lose all lives after boss and confirm Game Over, profile best score, restart, and reset behavior.
10. Smoke-test pause/resume, normal special aliens, alien explosions, rapid-fire, combo, and audio-safe play.

## Performance Considerations

All new lists remain tiny: support drops are bounded by wave size and cleanup, and boss waves contain one alien. Burst firing can increase active alien missiles, so cap tests and manual play should verify that projectile count remains bounded and the EDT-driven Swing loop stays responsive. Rendering adds a few primitives and a health bar per frame, which is negligible compared with the existing sprite rendering.

## Migration Notes

There is no persistent data migration. This is an in-memory gameplay change. The only code migration is replacing `RapidFirePowerUp` references with generalized `PowerUp` references while preserving the existing rapid-fire behavior.

## References

- Roadmap S-10: `context/foundation/roadmap.md:194`
- Existing rapid-fire drop pattern: `context/archive/2026-06-04-rapid-fire-power-up/plan.md`
- Existing special alien pattern: `context/archive/2026-06-04-distinct-alien-type/plan.md`
- Existing HUD/wave message pattern: `context/archive/2026-06-05-clear-hud-and-wave-message/plan.md`
- Controller tick pipeline: `src/main/java/com/emenems/games/aliens/controller/GameController.java:371`
- Missile collision sequencing: `src/main/java/com/emenems/games/aliens/controller/GameController.java:433`
- Support-effect scalar owner: `src/main/java/com/emenems/games/aliens/GameSession.java:3`
- Panel state push: `src/main/java/com/emenems/games/aliens/gui/GamePanel.java:92`
- Shared list wiring: `src/main/java/com/emenems/games/aliens/Main.java:22`

## Progress

> Convention: `- [ ]` pending, `- [x]` complete. Append ` — <commit sha>` when a step is implemented. Do not rename step titles.

### Phase 1: Shared support-drop model and session effects

#### Automated

- [x] 1.1 Project compiles
- [x] 1.2 Focused model/session/rules/ship tests pass
- [x] 1.3 Tests verify all power-up types retain type and move downward
- [x] 1.4 Tests verify extra life respects the 5-life cap
- [x] 1.5 Tests verify shield activation, one-event consumption, restart reset, and no stacking beyond one charge
- [x] 1.6 Tests verify speed boost activates for 180 ticks, refreshes, expires, and resets on life loss/restart
- [x] 1.7 Tests verify boosted movement uses a larger step but still clamps to board bounds

#### Manual

- [x] 1.8 Code review confirms timed support effect state is not stored in `Spaceship` or `GamePanel`
- [x] 1.9 Code review confirms `RapidFirePowerUp` migration has no duplicate behavior

### Phase 2: Support-drop controller integration

#### Automated

- [x] 2.1 Focused controller/panel tests pass
- [x] 2.2 Tests verify successful and failed drop rolls, weighted type selection, and one roll per resolved non-boss kill
- [x] 2.3 Tests verify each drop type activates the correct behavior
- [x] 2.4 Tests verify shield blocks exactly one alien missile damage event and exactly one ship-alien collision damage event
- [x] 2.5 Tests verify shield does not block alien invasion from non-boss aliens
- [x] 2.6 Tests verify speed boost changes held-key movement only while active
- [x] 2.7 Tests verify rapid-fire behavior remains compatible with the generalized drop list
- [x] 2.8 Tests verify drops and active effects reset on restart and do not tick outside `PLAYING`

#### Manual

- [x] 2.9 All four drop types are visually collectible and produce understandable effects
- [x] 2.10 Lives never exceed 5
- [x] 2.11 Shield consumption is visible enough to understand why a hit did not reduce lives
- [x] 2.12 Speed boost feels helpful without making movement uncontrollable

### Phase 3: Boss model and boss-only wave

#### Automated

- [x] 3.1 Focused alien/rules/session/controller tests pass
- [x] 3.2 Tests verify waves 1-4 keep normal composition and wave 5 contains exactly one boss
- [x] 3.3 Tests verify boss has 20 HP, requires 20 player-missile hits, and is removed only on the lethal hit
- [x] 3.4 Tests verify boss patrols horizontally and remains in the upper board
- [x] 3.5 Tests verify boss does not trigger alien invasion
- [x] 3.6 Tests verify boss defeat awards the boss score bonus once
- [x] 3.7 Tests verify boss defeat spawns exactly one guaranteed extra-life-or-shield drop
- [x] 3.8 Tests verify clearing boss wave advances to wave 6 with normal composition

#### Manual

- [x] 3.9 Wave 5 clearly feels like a boss-only milestone
- [x] 3.10 The 20-hit fight is readable and does not stall due to movement or hit feedback ambiguity
- [x] 3.11 The guaranteed reward and bonus score are visible in gameplay outcomes

### Phase 4: Boss burst attack and shield-safe damage

#### Automated

- [x] 4.1 Boss burst tests pass
- [x] 4.2 Tests verify boss firing creates the expected burst count when under cap
- [x] 4.3 Tests verify burst creation respects the active missile cap
- [x] 4.4 Tests verify burst missiles remain inside the board and use existing alien missile movement/cleanup
- [x] 4.5 Tests verify shield blocks one burst hit and later hits reduce lives normally
- [x] 4.6 Tests verify boss projectile deaths still enter Game Over and profile score handling once

#### Manual

- [x] 4.7 Boss burst pressure is clearly different from normal alien fire
- [x] 4.8 Dodging remains possible at normal speed and noticeably easier during speed boost
- [x] 4.9 Shield consumption during boss bursts is understandable from HUD/visual feedback

### Phase 5: Rendering, HUD, guidance, and full regression

#### Automated

- [x] 5.1 Presentation helpers pass
- [x] 5.2 Core focused tests pass
- [x] 5.3 Project compiles
- [x] 5.4 Full test suite passes
- [x] 5.5 No new dependency is added to `pom.xml`
- [x] 5.6 Existing profile, pause, combo, rapid-fire, special alien, explosion, score, wave, life, restart, and audio-safe behavior tests remain green

#### Manual

- [x] 5.7 Destroying regular aliens can produce rare rapid-fire, life, shield, and speed drops
- [x] 5.8 Extra life respects the 5-life cap
- [x] 5.9 Shield blocks exactly one damage event and then disappears from HUD
- [x] 5.10 Speed boost lasts 3 seconds and makes dodging boss bursts easier without breaking bounds
- [x] 5.11 Wave 5 is boss-only, the boss has a readable health bar, takes 20 hits, and never descends into contact/invasion
- [x] 5.12 Boss burst shots are readable and fair
- [x] 5.13 Killing the boss awards extra points and creates one guaranteed life-or-shield drop
- [x] 5.14 Wave 6 resumes normal wave composition
- [x] 5.15 Start menu, profile selection, scoring, combo, rapid-fire, pause/resume, Game Over, best score, restart, and safe audio behavior still work
