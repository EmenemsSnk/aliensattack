# Distinct Alien Type Implementation Plan

## Overview

Add one visibly and behaviorally distinct special alien to every wave from wave 2 onward. The special alien moves faster and laterally, changes horizontal direction unpredictably, survives one non-lethal hit, and fires more often, while preserving the existing score, combo, drop, wave, life-loss, and restart contracts.

## Current State Analysis

The game represents every enemy with the final `Alien` class, which currently owns only position and vertical speed and always moves straight downward. `GameController` creates six identical aliens per wave, treats every alien uniformly during collision resolution and firing, and reduces a kill batch to a count before sending it to `GameSession`. `GamePanel` renders the shared `List<Alien>` using one image for every entry.

The existing architecture should remain intact: `Main` creates one shared alien list for controller mutation and panel rendering, scalar session state remains in `GameSession`, and the controller remains the orchestration boundary. The change therefore extends `Alien` with explicit type and combat/movement state rather than adding parallel enemy lists or a new controller hierarchy.

## Desired End State

Wave 1 remains an introductory wave of six standard aliens. From wave 2 onward, each six-alien wave contains exactly one special alien and five standard aliens. The special alien:

- Is clearly distinguishable through a dedicated sprite.
- Has 2 HP and is destroyed by the second player-missile hit.
- Moves downward at `1.5x` the normal wave-relative speed.
- Moves horizontally, changes direction with a `2%` chance per playing tick, and remains inside the panel.
- Is selected to fire at approximately twice the frequency of a standard alien.
- Participates in the shared global alien-missile cap, increased from 2 to 5.
- Uses the same kill score, combo event, rapid-fire drop chance, ship-collision damage, invasion rule, and wave-completion semantics as a standard alien.

The first, non-lethal hit consumes its missile and updates its damaged visual state, but does not award score, advance combo, roll a drop, play the destruction effect, or remove the alien. Automated tests cover deterministic model and controller behavior, and manual gameplay verification confirms that the result is readable, challenging, and not boss-like.

### Key Findings

- `Alien` is a final mutable entity with straight vertical movement and sub-pixel Y state; extending it preserves the current shared `List<Alien>` contract. `src/main/java/com/emenems/games/aliens/gamemachines/Alien.java:3`
- Wave composition and placement are centralized in `generateSpaceObjects()`, which creates six aliens using injected `Random`. `src/main/java/com/emenems/games/aliens/controller/GameController.java:125`
- Missile collision resolution currently converts destroyed aliens to a count before scoring, so damage must be resolved before the existing kill batch side effects. `src/main/java/com/emenems/games/aliens/controller/GameController.java:265`
- Alien firing currently uses one global chance, uniformly selects a shooter, and caps active missiles at two. `src/main/java/com/emenems/games/aliens/controller/GameController.java:499`
- Every alien currently uses the same loaded image; presentation tests favor deterministic helper rules plus manual visual verification rather than pixel snapshots. `src/main/java/com/emenems/games/aliens/gui/GamePanel.java:97`
- The injected `Random` is shared by spawning, firing, and drops, so new random calls can change seeded test sequences and must be explicitly controlled in tests. `src/main/java/com/emenems/games/aliens/controller/GameController.java:46`

## What We Are NOT Doing

- Adding more than one new alien type, bosses, variable special-alien counts, or type progression across later waves.
- Giving the special alien a different score, combo multiplier behavior, rapid-fire drop chance, projectile type, collision size, or life-loss effect.
- Adding health bars, damage numbers, new sound effects, or a broad HUD redesign.
- Introducing parallel alien lists, subclasses, a new enemy controller, external dependencies, or a general entity-component refactor.
- Changing rapid-fire, combo, player controls, wave size, or the standard alien's movement and durability.
- Adding pixel-snapshot Swing tests.

## Implementation Approach

Represent the distinction inside the existing `Alien` model with an explicit alien type and type-derived properties. Keep the existing constructors as standard-alien conveniences so current callers and tests remain readable, and provide a clear special-alien creation contract for generated waves and focused tests.

`Alien` owns its health, vertical/horizontal movement state, boundary-safe movement, random direction-change decision, and damage result. `GameController` owns wave composition, invokes movement with the injected random source and panel bounds, separates damaged aliens from destroyed aliens during collision resolution, and applies existing kill side effects only to the destroyed set.

For firing, preserve one global firing event per tick and the existing single missile type. Increase the active alien-missile cap to five, adjust the firing-event probability to account for the heavier special-alien weight, and select shooters with a `2:1` special-to-standard weight. This keeps one special alien approximately twice as likely to fire as each standard alien without independently rolling for every enemy or multiplying total fire rate by the full wave size.

`GamePanel` selects the dedicated sprite by alien type and uses a deterministic damaged-state presentation for a special alien after its first hit. No new scalar panel push or `GameSession` state is needed because type, HP, and position travel through the shared alien objects.

## Critical Implementation Details

### Timing and Lifecycle

Alien movement occurs before firing and collisions in each playing tick. The special alien's random direction decision and horizontal move must happen in that existing movement slot, and horizontal bounds must be enforced there so it cannot be removed by the generic offscreen cleanup before the player can fight it.

### State Sequencing

Collision resolution must consume a player missile for every hit, but only add an alien to the destroyed set after `takeHit()` reports destruction. Rapid-fire drop rolls, alien removal, combo progression, score application, and explosion sound remain one batch operation over destroyed aliens only; a non-lethal first hit triggers none of those effects.

### Debugging and Observability

Use deterministic `Random` stand-ins that distinguish direction-change, firing-event, shooter-selection, and drop calls. Tests must not depend on incidental call ordering from a generic seeded `Random` when asserting special-alien behavior.

## Phase 1: Special Alien Model and Movement

### Overview

Establish a type-aware alien model with bounded durability and deterministic special movement while preserving standard alien behavior and existing constructor compatibility.

### Required Changes

#### 1. Alien Type Contract

**File**: `src/main/java/com/emenems/games/aliens/gamemachines/AlienType.java`

**Purpose**: Define the two supported enemy identities so spawning, behavior, collision handling, rendering, and tests share one explicit vocabulary.

**Contract**: Add `STANDARD` and `SPECIAL`; type identity must be queryable from each `Alien` without introducing a second enemy list or subclass hierarchy.

#### 2. Type-Aware Alien State and Behavior

**File**: `src/main/java/com/emenems/games/aliens/gamemachines/Alien.java`

**Purpose**: Extend the existing entity with special-alien durability and movement while leaving standard alien behavior unchanged.

**Contract**: Existing constructors create standard aliens. A special-alien construction path sets 2 HP, wave speed multiplied by `1.5`, and horizontal movement state. Expose type, remaining-health/damaged-state queries, and a damage operation that reports whether the hit destroyed the alien. Standard aliens die on their first hit. Special movement changes horizontal direction when the injected random decision is below `0.02`, moves laterally while descending, and reverses/clamps at panel edges.

#### 3. Focused Alien Tests

**File**: `src/test/java/com/emenems/games/aliens/gamemachines/AlienTest.java`

**Purpose**: Lock the entity-level contracts independently from controller orchestration.

**Contract**: Cover unchanged standard movement and one-hit destruction; special `1.5x` vertical movement, two-hit destruction, damaged-state transition, horizontal movement, forced random direction change, and edge reversal without leaving bounds.

### Success Criteria

#### Automated Verification

- Special and standard alien model tests pass: `./mvnw test -Dtest=AlienTest`
- Existing domain tests remain green: `./mvnw test -Dtest=GameRulesTest,GameSessionTest,AlienMissileTest,RapidFirePowerUpTest,SpaceshipTest`
- Project compiles: `./mvnw clean compile`

#### Manual Verification

- Model constants and naming make the special alien's behavior understandable without inspecting controller code.
- The special alien remains a bounded non-boss enemy with exactly two hits of durability.

**Implementation note**: After this phase and successful automated verification, stop for human confirmation of the manual checks before continuing.

---

## Phase 2: Wave, Combat, and Firing Integration

### Overview

Integrate one special alien into later waves and ensure damage, destruction side effects, weighted firing, missile limits, and lifecycle behavior preserve existing gameplay contracts.

### Required Changes

#### 1. Special-Alien Rules

**File**: `src/main/java/com/emenems/games/aliens/GameRules.java`

**Purpose**: Centralize stable gameplay tuning values and pure calculations that affect special-alien speed, direction-change probability, wave eligibility, and firing weight.

**Contract**: Define or expose pure rules for special availability from wave 2, exactly one special slot per eligible six-alien wave, `1.5x` vertical speed, `2%` direction-change chance, and `2:1` firing weight. Preserve existing score and wave-speed formulas.

#### 2. Controller Tick and Wave Composition

**File**: `src/main/java/com/emenems/games/aliens/controller/GameController.java`

**Purpose**: Generate the agreed wave composition and drive special movement through the existing playing-tick pipeline.

**Contract**: Wave 1 contains six standard aliens; every later wave contains five standard aliens and one special alien. Type assignment must be deterministic under injected randomness without weakening existing non-overlap/top-area placement guarantees. Replace the uniform `Alien::move` call with movement orchestration that supplies the injected random source and panel bounds required by special movement.

#### 3. Damage-Aware Missile Collision Resolution

**File**: `src/main/java/com/emenems/games/aliens/controller/GameController.java`

**Purpose**: Consume missiles on both damaging and destroying hits while applying score, combo, drops, removal, and explosion sound only to actual kills.

**Contract**: Preserve the invariant that one missile hits at most one alien per collision pass. Track hit missiles separately from destroyed aliens. A first hit on a special alien leaves it active and damaged; the second hit enters the existing destroyed-alien batch. Same-tick duplicate hits may destroy the special alien but must score, drop, and advance combo only once.

#### 4. Weighted Alien Fire and Increased Missile Cap

**File**: `src/main/java/com/emenems/games/aliens/controller/GameController.java`

**Purpose**: Make the special alien fire approximately twice as often as each standard alien while allowing up to five active enemy projectiles.

**Contract**: Set the shared active alien-missile cap to `5`. Preserve one firing event and at most one new alien missile per tick. Select shooters using standard weight `1` and special weight `2`, and tune the global event chance so adding the weight does not accidentally reduce standard-alien firing frequency. All shooters continue creating the existing `AlienMissile`.

#### 5. Controller and Rule Regression Tests

**Files**:

- `src/test/java/com/emenems/games/aliens/GameRulesTest.java`
- `src/test/java/com/emenems/games/aliens/controller/GameControllerTest.java`

**Purpose**: Protect composition, movement integration, damage sequencing, weighted firing, and preserved replayability mechanics.

**Contract**: Cover no special alien in wave 1; exactly one from wave 2 onward; preserved total wave size and spawn invariants; first hit consumes a missile without score/combo/drop/removal; second and same-tick lethal hits apply kill effects once; standard aliens remain one-hit kills; special aliens use standard score/drop semantics; special movement remains bounded during ticks; shooter weighting favors special aliens; no tick creates more than one enemy missile; cap is five; restart creates the correct wave-1 composition.

### Success Criteria

#### Automated Verification

- Rule tests pass: `./mvnw test -Dtest=GameRulesTest`
- Controller integration tests pass: `./mvnw test -Dtest=GameControllerTest`
- Project compiles: `./mvnw clean compile`

#### Manual Verification

- Clearing wave 1 introduces exactly one special alien in wave 2 without interrupting wave progression.
- First-hit, second-hit, combo, rapid-fire drop, ship collision, invasion, and restart behavior match the agreed contracts.
- Five-projectile cap and increased special firing create pressure without making unavoidable projectile walls.

**Implementation note**: After this phase and successful automated verification, stop for human confirmation of the manual checks before continuing.

---

## Phase 3: Distinct Rendering and Full Regression

### Overview

Make the special alien immediately recognizable, communicate its damaged state, and verify the complete replayability slice through automated regression and gameplay observation.

### Required Changes

#### 1. Dedicated Special-Alien Sprite

**File**: `src/main/resources/images/special-alien.png`

**Purpose**: Give the special alien a clearly distinct visual identity at the existing `42x42` rendered size.

**Contract**: Add one transparent image asset that remains legible against the space background and visibly differs from `alien.png`. Do not add new asset libraries or change component dimensions.

#### 2. Type- and Damage-Aware Rendering

**File**: `src/main/java/com/emenems/games/aliens/gui/GamePanel.java`

**Purpose**: Render each alien using its type-specific visual and make the special alien's first non-lethal hit readable.

**Contract**: Load the dedicated special sprite and select the correct image from alien type. Add a simple deterministic damaged-state treatment for a special alien with 1 HP, such as a high-contrast overlay or outline, without adding scalar panel state or obscuring projectiles.

#### 3. Presentation Rule Tests

**File**: `src/test/java/com/emenems/games/aliens/gui/GamePanelTest.java`

**Purpose**: Lock deterministic type/damage visual-selection rules without brittle Swing pixel snapshots.

**Contract**: Test any extracted package-visible predicates or visual-selection helpers used to distinguish standard, healthy special, and damaged special aliens.

#### 4. Project Guidance

**File**: `CLAUDE.md`

**Purpose**: Keep future changes aligned with the new alien identity, damage, movement, firing, and rendering ownership boundaries.

**Contract**: Document that alien type/HP/movement state lives on `Alien`; special wave composition, weighted firing, and kill side effects remain controller concerns; and special visuals are selected by `GamePanel` from the shared alien list.

### Success Criteria

#### Automated Verification

- Presentation tests pass: `./mvnw test -Dtest=GamePanelTest`
- Full test suite passes: `./mvnw test`
- Clean compilation passes: `./mvnw clean compile`
- Both alien image assets exist: `test -f src/main/resources/images/alien.png && test -f src/main/resources/images/special-alien.png`

#### Manual Verification

- The special alien is immediately distinguishable from standard aliens at gameplay speed and its damaged state is readable after the first hit.
- Its `1.5x` descent, random lateral changes, two-hit durability, approximately doubled firing frequency, and five-projectile environment feel challenging but not boss-like.
- Standard aliens, controls, scoring, combo, rapid fire, lives, Game Over, restart, audio fallback, and wave progression show no regressions during a complete session.

**Implementation note**: After this phase and successful automated verification, stop for human confirmation of the manual checks.

---

## Testing Strategy

### Unit Tests

- Verify standard and special alien construction, movement, boundary handling, damage, destruction, and damaged state.
- Verify pure special-alien wave eligibility, speed multiplier, direction-change chance, and firing-weight calculations.
- Verify deterministic presentation predicates or visual selection without pixel snapshots.

### Integration Tests

- Verify wave 1 and later-wave composition while retaining six non-overlapping spawn positions.
- Verify non-lethal and lethal collision paths, including multiple missiles in one tick and one-score/one-drop/one-combo-event invariants.
- Verify weighted shooter selection, one-shot-per-tick behavior, and the five-missile cap.
- Verify special aliens remain compatible with ship collision, invasion, cleanup, wave advancement, rapid-fire drops, combo scoring, Game Over, and restart.

### Manual Testing Steps

1. Start a game and confirm wave 1 contains only standard aliens.
2. Clear wave 1 and confirm wave 2 contains exactly one visually distinct special alien.
3. Observe that the special alien descends faster, moves laterally unpredictably, remains inside the panel, and fires more often.
4. Hit it once and confirm the missile is consumed, the alien remains, damage is visible, and score/combo/drop do not change.
5. Hit it again and confirm normal kill score, combo progression, possible rapid-fire drop behavior, and wave completion.
6. Allow up to five enemy missiles onscreen and assess whether dodging remains fair.
7. Trigger ship collision, invasion Game Over, life loss, wave advancement, and restart; confirm existing behavior remains intact.
8. Complete a longer session with and without rapid fire and assess whether one special alien per wave is challenging but not boss-like.

## Performance Considerations

The change adds constant-time state and movement work for one special alien per wave. Weighted shooter selection may scan the six-alien list once per firing event, which is negligible. The increased missile cap remains bounded at five, and existing offscreen cleanup must continue to prevent projectile accumulation.

## Migration Notes

No persisted data or schema exists. Existing `Alien` constructors must remain compatible and continue creating standard aliens so current code and tests migrate incrementally. Restart always returns to wave 1 and therefore clears special-alien state through normal alien-list regeneration.

## References

- Product requirement: `context/foundation/prd.md`
- Roadmap slice: `context/foundation/roadmap.md`
- Existing alien model: `src/main/java/com/emenems/games/aliens/gamemachines/Alien.java:3`
- Wave generation and combat: `src/main/java/com/emenems/games/aliens/controller/GameController.java:125`
- Existing rendering: `src/main/java/com/emenems/games/aliens/gui/GamePanel.java:97`
- Similar shared-entity integration: `context/archive/2026-06-04-rapid-fire-power-up/plan.md`
- Similar score/combo collision constraints: `context/archive/2026-06-04-skill-based-score-combo/plan.md`

## Progress

> Convention: `- [ ]` pending, `- [x]` complete. Append ` — <commit sha>` when a step is implemented. Do not rename step titles.

### Phase 1: Special Alien Model and Movement

#### Automated

- [x] 1.1 Special and standard alien model tests pass
- [x] 1.2 Existing domain tests remain green
- [x] 1.3 Project compiles

#### Manual

- [x] 1.4 Model constants and naming make the special alien behavior understandable
- [x] 1.5 Special alien remains a bounded two-hit non-boss enemy

### Phase 2: Wave, Combat, and Firing Integration

#### Automated

- [x] 2.1 Rule tests pass
- [x] 2.2 Controller integration tests pass
- [x] 2.3 Project compiles

#### Manual

- [x] 2.4 Wave 2 introduces exactly one special alien without interrupting progression
- [x] 2.5 Combat, combo, drop, collision, invasion, and restart contracts are preserved
- [x] 2.6 Five-projectile cap and special firing pressure remain fair

### Phase 3: Distinct Rendering and Full Regression

#### Automated

- [x] 3.1 Presentation tests pass
- [x] 3.2 Full test suite passes
- [x] 3.3 Clean compilation passes
- [x] 3.4 Standard and special alien image assets exist

#### Manual

- [x] 3.5 Special identity and damaged state are immediately readable
- [x] 3.6 Special behavior is challenging but not boss-like
- [x] 3.7 Complete arcade session shows no regressions
