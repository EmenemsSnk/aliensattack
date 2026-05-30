# Production Code Refactor Implementation Plan

## Overview

Refactor the Java 21 MVP codebase to remove vestigial code, clarify ownership of game-world constants, simplify constructor/API surfaces, and apply small modern Java improvements without changing gameplay behavior. The refactor is intentionally incremental: each phase keeps the game runnable and the JUnit suite green.

## Current State Analysis

The MVP is playable and tagged `v1.0.0`, but several implementation details still reflect the prototype path. `GameController` is the central gameplay node and owns state, input, collisions, scoring, wave progression, and audio triggers. `GamePanel` passively renders shared entity lists plus pushed scalar state. The shared-list contract is load-bearing: `Main` creates the lists once and passes the same references to both controller and panel.

The draft in `context/changes/refactor-plan/draft.md` is mostly accurate on dead code, constructor telescoping, stale constants, deprecated `JFrame.show()`, and formatting. I am tightening it in three ways:

- Keep the refactor behavior-preserving and test-first; no View/Controller rewrite, no new library, no architectural split.
- Move game-world dimensions out of `GamePanel`, but do that as its own phase because it touches controller, panel, tests, and startup wiring.
- Apply Java 21 modernization only where it makes contracts clearer (`sealed GameObject`, switch-based state input), not as a broad `var` or style sweep.

## Desired End State

Production code has no dead placeholders or unused compatibility shims. Game-world dimensions and sprite size live in a shared non-GUI constants holder, not on `GamePanel`. `GameController` has one production constructor and one package-private deterministic test constructor. `GamePanel` exposes only the constructor and state update path actually used by the application. `GameObject` explicitly defines its permitted concrete implementations. Existing gameplay behavior, controls, visuals, audio failure tolerance, and tests remain unchanged.

### Key Findings:

- `src/main/java/com/emenems/games/aliens/Main.java` still calls deprecated/redundant `windowFrame.show()` even though `WindowFrame.initUI()` already calls `setVisible(true)`.
- `src/main/java/com/emenems/games/aliens/Point.java` is only a commented-out placeholder.
- `src/main/java/com/emenems/games/aliens/gamemachines/Spaceship.java` has unused `health`, `speed`, and `decreaseHealth(int demage)`; lives are correctly owned by `GameController`.
- `src/main/java/com/emenems/games/aliens/gui/GamePanel.java` still owns game-world constants used by controller/tests and has unused constants/overloads.
- `src/main/java/com/emenems/games/aliens/controller/GameController.java` has constructor telescoping and depends heavily on `GamePanel` constants for non-rendering rules.
- `./mvnw test` currently passes with 37 tests, so the refactor has a green baseline.

## What We Are NOT Doing

- No behavior changes to movement, collision, scoring, waves, alien fire, audio, start/restart flow, or rendering.
- No View/Controller separation rewrite; `GameController` remains the central gameplay owner.
- No new runtime or test dependencies.
- No Lombok, DI framework, Spring, game engine, annotation processor, or module-system conversion.
- No broad `var` conversion or cosmetic-only rewrite across every local variable.
- No performance tuning beyond preserving current O(n) small-list behavior.

## Implementation Approach

Proceed in four small phases. First remove the safest dead code and deprecated/no-op calls while preserving public behavior. Then extract game-world constants into a root-level `GameConstants` class and update all references. Next collapse constructor overloads and test factories so hidden list creation cannot violate the shared-reference contract. Finally apply focused Java 21 modernization and documentation reconciliation. Each phase runs `./mvnw clean compile` and `./mvnw test`.

## Critical Implementation Details

- **Shared-list contract**: `Main` must still pass the exact same `List<Missile>`, `List<AlienMissile>`, and `List<Alien>` instances to `GamePanel` and `GameController`. Any constructor cleanup must make this harder to break, not easier.
- **Constants migration**: move dimensions to a non-instantiable `GameConstants` class. Update controller/tests to consume it directly. `GamePanel` should use the same constants for rendering dimensions, avoiding duplicate values.
- **Sealed hierarchy sequencing**: `GameObject` and all current implementors are in `com.emenems.games.aliens.gamemachines`, so a sealed interface is feasible. Do it after dead-code/API cleanup so the permits list is final.

---

## Phase 1: Safe Dead Code and Deprecated Call Cleanup

### Overview

Remove code that is demonstrably unused or misleading, without changing any gameplay rule or test seam.

### Required Changes:

#### 1. Delete empty placeholder

**File**: `src/main/java/com/emenems/games/aliens/Point.java`

**Purpose**: Remove a commented-out placeholder that suggests a coordinate abstraction exists when it does not.

**Contract**: Delete the file. No replacement is introduced.

#### 2. Remove vestigial spaceship state

**File**: `src/main/java/com/emenems/games/aliens/gamemachines/Spaceship.java`

**Purpose**: Make it clear that player lives are owned by `GameController`, not `Spaceship`.

**Contract**: Remove `health`, `speed`, and `decreaseHealth(int)`. Extract the hardcoded movement delta into `private static final int MOVE_STEP = 5`; existing movement tests must still pass.

#### 3. Remove deprecated/redundant frame visibility call

**File**: `src/main/java/com/emenems/games/aliens/Main.java`

**Purpose**: Eliminate the deprecated `JFrame.show()` call and the duplicate visibility side effect.

**Contract**: `WindowFrame` remains visible via `WindowFrame.initUI()`. `Main` constructs the controller and initializes it after frame creation as today.

#### 4. Remove no-op KeyAdapter super calls and local formatting noise

**Files**:
- `src/main/java/com/emenems/games/aliens/controller/GameController.java`
- `src/main/java/com/emenems/games/aliens/Main.java`
- `src/main/java/com/emenems/games/aliens/gui/WindowFrame.java`
- `src/main/java/com/emenems/games/aliens/gui/GamePanel.java`

**Purpose**: Remove noise that obscures the important control flow.

**Contract**: Remove `super.keyPressed(e)` / `super.keyReleased(e)` from the anonymous `KeyAdapter`; normalize obvious spacing around braces, commas, and assignments in touched files only.

### Success Criteria:

#### Automated Verification:

- Project compiles: `./mvnw clean compile`
- Full test suite passes: `./mvnw test`
- `grep -RIn "decreaseHealth\\|private int health\\|private int speed\\|windowFrame.show\\|super.keyPressed\\|super.keyReleased" src/main/java` returns no stale production references.
- `src/main/java/com/emenems/games/aliens/Point.java` no longer exists.

#### Manual Verification:

- Running the game still opens the Swing window normally.
- Movement, start/restart, shooting, alien fire, Game Over, and audio behavior appear unchanged.

**Implementation note**: Manual verification is not auto-checked; do not mark manual progress complete until a human confirms it.

---

## Phase 2: Extract Game-World Constants from Swing Panel

### Overview

Move board dimensions and entity size out of `GamePanel` so gameplay logic no longer depends on a Swing component for world rules.

### Required Changes:

#### 1. Add shared constants holder

**File**: `src/main/java/com/emenems/games/aliens/GameConstants.java`

**Purpose**: Provide one non-GUI owner for board width, board height, component size, and any startup margin used by `Main`.

**Contract**: Add a `final` class with a private constructor and public static final constants matching current values:
- `PANEL_WIDTH = 760`
- `PANEL_HEIGHT = 650`
- `COMPONENT_SIZE = 42`
- `SPACESHIP_START_BOTTOM_MARGIN = 20`

#### 2. Update production references

**Files**:
- `src/main/java/com/emenems/games/aliens/Main.java`
- `src/main/java/com/emenems/games/aliens/controller/GameController.java`
- `src/main/java/com/emenems/games/aliens/gui/GamePanel.java`

**Purpose**: Decouple controller/startup rules from `GamePanel`.

**Contract**: Replace controller and `Main` usages of `GamePanel.PANEL_WIDTH`, `GamePanel.PANEL_HEIGHT`, and `GamePanel.DEFAULT_COMPONENT_SIZE` with `GameConstants`. `GamePanel` uses `GameConstants` for its preferred/min/max dimensions and sprite render sizes.

#### 3. Remove unused GamePanel constants

**File**: `src/main/java/com/emenems/games/aliens/gui/GamePanel.java`

**Purpose**: Remove unused or misleading constants from the rendering component.

**Contract**: Remove `MINIMUM_BORDER_VALUE` and `DEFAULT_DIMENSION`. Remove the public width/height/component-size constants from `GamePanel` after all code/tests are migrated.

#### 4. Update test references

**File**: `src/test/java/com/emenems/games/aliens/controller/GameControllerTest.java`

**Purpose**: Keep tests aligned with the domain owner of dimensions.

**Contract**: Tests use `GameConstants` for board limits and component size. Assertions remain semantically identical.

### Success Criteria:

#### Automated Verification:

- Project compiles: `./mvnw clean compile`
- Full test suite passes: `./mvnw test`
- `grep -RIn "GamePanel\\.PANEL_WIDTH\\|GamePanel\\.PANEL_HEIGHT\\|GamePanel\\.DEFAULT_COMPONENT_SIZE\\|DEFAULT_DIMENSION\\|MINIMUM_BORDER_VALUE" src/main/java src/test/java` returns no matches.
- The generated wave, boundary clamp, collision, and cleanup tests still prove the same behavior.

#### Manual Verification:

- Board size, sprite size, HUD layout, start overlay, and game-over overlay appear unchanged.
- Ship bounds and alien bottom-loss behavior remain unchanged in play.

**Implementation note**: Manual verification is not auto-checked; do not mark manual progress complete until a human confirms it.

---

## Phase 3: Collapse Constructors and Compatibility Shims

### Overview

Reduce API surface so production and tests use explicit shared-list wiring, with deterministic injection available only where tests need it.

### Required Changes:

#### 1. Collapse GameController constructors

**File**: `src/main/java/com/emenems/games/aliens/controller/GameController.java`

**Purpose**: Remove constructor telescoping that silently creates hidden list instances and obscures the shared-reference contract.

**Contract**: Keep exactly:
- one public production constructor accepting `Spaceship`, `List<Missile>`, `List<AlienMissile>`, `List<Alien>`, `GamePanel`;
- one package-private test constructor accepting the same plus `Random` and `ArcadeSoundPlayer`.

Remove all intermediate overloads. Existing package-private state getters may remain for tests.

#### 2. Update controller tests to use canonical factories

**File**: `src/test/java/com/emenems/games/aliens/controller/GameControllerTest.java`

**Purpose**: Preserve deterministic tests without relying on deleted convenience overloads.

**Contract**: Test helper factories create and pass explicit `List<AlienMissile>` instances. Seeded-random tests use the 7-argument constructor. Tests that do not care about alien missiles still pass an empty mutable list.

#### 3. Remove GamePanel compatibility overloads

**File**: `src/main/java/com/emenems/games/aliens/gui/GamePanel.java`

**Purpose**: Avoid a dead 3-argument constructor that supplies an immutable `List.of()` for alien missiles.

**Contract**: Keep only the constructor used by `Main`: `Spaceship`, player missiles, alien missiles, aliens. Keep only the 6-argument `updateGameState(...)` method currently called by `GameController`; remove `updateHud(...)` and the 4-argument overload.

### Success Criteria:

#### Automated Verification:

- Project compiles: `./mvnw clean compile`
- Full test suite passes: `./mvnw test`
- `grep -RIn "new GameController" src/main/java src/test/java` shows only the production constructor in `Main` and explicit test-helper/test-injection call sites.
- `grep -RIn "updateHud\\|updateGameState(int score, int wave, int lives, GameState" src/main/java src/test/java` returns no stale overload declarations/usages.

#### Manual Verification:

- Start menu, play, Game Over, and restart still work.
- Alien missiles still render, move, collide, and clear correctly, confirming shared-list wiring survived.

**Implementation note**: Manual verification is not auto-checked; do not mark manual progress complete until a human confirms it.

---

## Phase 4: Focused Java 21 Modernization and Documentation Reconciliation

### Overview

Apply modernization that clarifies contracts and update local agent documentation so it no longer describes removed vestiges.

### Required Changes:

#### 1. Seal the GameObject hierarchy

**Files**:
- `src/main/java/com/emenems/games/aliens/gamemachines/GameObject.java`
- `src/main/java/com/emenems/games/aliens/gamemachines/Spaceship.java`
- `src/main/java/com/emenems/games/aliens/gamemachines/Alien.java`
- `src/main/java/com/emenems/games/aliens/gamemachines/Missile.java`
- `src/main/java/com/emenems/games/aliens/gamemachines/AlienMissile.java`

**Purpose**: Make the closed set of renderable/gameplay objects explicit.

**Contract**: `GameObject` becomes a sealed interface permitting the four current implementors. The four implementors declare the required `final` or `non-sealed` modifier; prefer `final` unless a class has a concrete extension need, which none currently do.

#### 2. Simplify state input with switch

**File**: `src/main/java/com/emenems/games/aliens/controller/GameController.java`

**Purpose**: Make state-dependent input behavior easier to audit.

**Contract**: Rewrite `handleKeyPressed(int keyCode)` using a Java 21 switch over `gameState` or a small dispatch structure. Preserve exact behavior: Enter starts/restarts in menu/game-over; Space fires only while playing; movement keys only affect playing state.

#### 3. Name hitbox/render magic numbers

**Files**:
- `src/main/java/com/emenems/games/aliens/controller/GameController.java`
- `src/main/java/com/emenems/games/aliens/gui/GamePanel.java`

**Purpose**: Make alien missile collision/render widths auditable and keep controller hitbox choices distinct from presentation choices.

**Contract**: Extract named constants for alien missile hitbox x-offset/width and render x-offset/width. Preserve current numeric values unless manual testing in the phase explicitly approves a change.

#### 4. Reconcile documentation

**Files**:
- `CLAUDE.md`
- `context/changes/refactor-plan/draft.md`

**Purpose**: Keep future agents from following stale notes after vestigial code is removed.

**Contract**: Update `CLAUDE.md` to remove statements saying `Point.java` remains or `Spaceship` still has unused health/speed/decreaseHealth. Add a brief note that game-world constants live in `GameConstants`. Mark `context/changes/refactor-plan/draft.md` as superseded by `context/changes/refactor-plan/plan.md`, without rewriting its historical analysis.

### Success Criteria:

#### Automated Verification:

- Project compiles: `./mvnw clean compile`
- Full test suite passes: `./mvnw test`
- `grep -RIn "Point.java remains\\|unused.*health\\|decreaseHealth\\|DEFAULT_COMPONENT_SIZE = 42px" CLAUDE.md` returns no stale guidance.
- `grep -RIn "getX()\\|getY()" src/main/java/com/emenems/games/aliens/gamemachines/GameObject.java` confirms the sealed interface still exposes the same coordinate contract.

#### Manual Verification:

- No visible gameplay or UI regression after the modernization phase.
- The code remains easy to navigate: constants, object hierarchy, and input-state behavior are clearer than before.

**Implementation note**: Manual verification is not auto-checked; do not mark manual progress complete until a human confirms it.

---

## Testing Strategy

### Unit Tests:

- Preserve all existing controller tests as regression coverage for state gating, movement, scoring, wave generation, collisions, alien fire, cleanup, restart, and speed/score formulas.
- Preserve `SpaceshipTest` and `AlienMissileTest`; adjust only imports/constant references as required by constants extraction.
- Do not add mock frameworks. If audio injection remains needed, use the existing package-private constructor seam with a harmless `ArcadeSoundPlayer` instance.

### Integration Tests:

- No automated Swing integration harness exists. Use `./mvnw exec:java -Dexec.mainClass="com.emenems.games.aliens.Main"` for manual validation after phases that touch `Main`, `GamePanel`, or constants.

### Manual Testing Steps:

1. Run `./mvnw exec:java -Dexec.mainClass="com.emenems.games.aliens.Main"`.
2. Confirm the window opens once, without deprecated `show()` behavior causing double visibility effects.
3. Confirm Enter starts/restarts and Space hold-to-fire still works.
4. Confirm movement clamps to the same board bounds.
5. Confirm collisions, alien fire, scoring, wave progression, audio fallback, and Game Over still behave as in v1.0.0.

## Performance Considerations

This refactor should be behavior- and allocation-neutral in the game loop. Constants extraction and constructor cleanup should not add per-tick allocations. Sealing `GameObject` is compile-time metadata and should not affect runtime performance. Avoid introducing streams or abstractions into hot per-tick collision paths unless they are already present and measured equivalent.

## Migration Notes

No player data, saves, or external APIs exist, so there is no data migration. This is source-level cleanup only. Downstream compatibility is not a concern beyond this repository because the game is not published as a library.

## References

- Draft analysis: `context/changes/refactor-plan/draft.md`
- Local architecture rules: `CLAUDE.md`
- Main wiring: `src/main/java/com/emenems/games/aliens/Main.java`
- Controller loop/input/collisions: `src/main/java/com/emenems/games/aliens/controller/GameController.java`
- Rendering/state push: `src/main/java/com/emenems/games/aliens/gui/GamePanel.java`
- Entity model: `src/main/java/com/emenems/games/aliens/gamemachines/`
- Regression tests: `src/test/java/com/emenems/games/aliens/`

## Progress

> Convention: `- [ ]` pending, `- [x]` done. Add ` — <commit sha>`, when a step is committed. Do not change step titles. See `references/progress-format.md`.

### Phase 1: Safe Dead Code and Deprecated Call Cleanup

#### Automated

- [x] 1.1 Project compiles: `./mvnw clean compile` — a5d7503
- [x] 1.2 Full test suite passes: `./mvnw test` — a5d7503
- [x] 1.3 Stale dead-code/deprecated references are removed by grep — a5d7503
- [x] 1.4 `Point.java` no longer exists — a5d7503

#### Manual

- [x] 1.5 Running the game still opens the Swing window normally — a5d7503
- [x] 1.6 Movement, start/restart, shooting, alien fire, Game Over, and audio behavior appear unchanged — a5d7503

### Phase 2: Extract Game-World Constants from Swing Panel

#### Automated

- [x] 2.1 Project compiles: `./mvnw clean compile`
- [x] 2.2 Full test suite passes: `./mvnw test`
- [x] 2.3 No stale `GamePanel` world-constant references remain
- [x] 2.4 Wave, boundary, collision, and cleanup tests still prove equivalent behavior

#### Manual

- [x] 2.5 Board size, sprite size, HUD layout, start overlay, and game-over overlay appear unchanged
- [x] 2.6 Ship bounds and alien bottom-loss behavior remain unchanged in play

### Phase 3: Collapse Constructors and Compatibility Shims

#### Automated

- [ ] 3.1 Project compiles: `./mvnw clean compile`
- [ ] 3.2 Full test suite passes: `./mvnw test`
- [ ] 3.3 Controller constructor call sites use explicit production/test wiring
- [ ] 3.4 Dead `GamePanel` state-update and constructor overloads are removed

#### Manual

- [ ] 3.5 Start menu, play, Game Over, and restart still work
- [ ] 3.6 Alien missiles still render, move, collide, and clear correctly

### Phase 4: Focused Java 21 Modernization and Documentation Reconciliation

#### Automated

- [ ] 4.1 Project compiles: `./mvnw clean compile`
- [ ] 4.2 Full test suite passes: `./mvnw test`
- [ ] 4.3 `CLAUDE.md` has no stale vestigial-code guidance
- [ ] 4.4 `GameObject` is sealed while preserving the `getX()` / `getY()` coordinate contract

#### Manual

- [ ] 4.5 No visible gameplay or UI regression after modernization
- [ ] 4.6 Constants, object hierarchy, and input-state behavior are clearer than before
