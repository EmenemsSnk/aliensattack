# Extract Game Rules Implementation Plan

## Overview

Extract the current scoring and alien wave-speed calculations out of `GameController` into a focused `GameRules` class without changing gameplay. This is roadmap slice S-02 and prepares the codebase for the later `extract-game-session` slice while keeping the refactor intentionally small.

## Current State Analysis

`GameController` currently owns both orchestration and domain rule calculations. The score awarded per alien is calculated through a package-private static method, and alien speed is calculated from controller-private tuning constants during wave generation. Controller tests already cover these rules, but the tests are coupled to `GameController` instead of a focused rule surface.

The safety baseline from S-01 is already archived and defines the canonical guardrail for this refactor: run `./mvnw clean compile` and `./mvnw test` before and after the slice, with manual smoke testing still required for visible gameplay confidence.

## Desired End State

`GameRules` exposes the current scoring and wave-speed behavior as focused, testable domain rules. `GameController` delegates scoring and generated alien speed to `GameRules`, existing visible gameplay remains unchanged, and tests prove the extracted rule behavior independently from the controller.

### Key Findings:

- Roadmap S-02 explicitly scopes this slice to "extracted scoring and wave rules" and marks session extraction as the next slice, not this one: `context/foundation/roadmap.md:67`.
- `GameController.generateSpaceObjects()` currently calculates alien speed from `wave`, `BASE_ALIEN_SPEED`, and `MAX_ALIEN_SPEED`: `src/main/java/com/emenems/games/aliens/controller/GameController.java:104`.
- Missile hits add `aliensToRemove.size() * calculateAlienScore(wave)` to the controller-owned score: `src/main/java/com/emenems/games/aliens/controller/GameController.java:260`.
- The current rule methods live at the bottom of `GameController` and are already covered by tests: `src/main/java/com/emenems/games/aliens/controller/GameController.java:348`, `src/test/java/com/emenems/games/aliens/controller/GameControllerTest.java:523`.
- The locked baseline requires compile and full tests as the automated gate for future refactor slices: `context/archive/2026-05-31-lock-refactor-safety-baseline/baseline.md:39`.

## What We Are NOT Doing

- No `GameSession` extraction, reset-state ownership changes, or movement of `score`, `wave`, `lives`, or `gameState`.
- No gameplay tuning: score values, speed curve, cap, wave advancement, alien spawning, movement, lives, Game Over, and restart behavior stay the same.
- No UI, rendering, audio, input, data persistence, CI, or dependency changes.
- No broad architecture rewrite or dependency injection framework.

## Implementation Approach

Use the smallest useful extraction: create a `GameRules` class in the root `com.emenems.games.aliens` package next to `GameConstants` and `GameState`, move only the current scoring and alien-speed formulas there, and update `GameController` to delegate. Move focused rule tests into a new `GameRulesTest`, while keeping controller integration tests that prove score application and wave behavior still work through the game loop.

Recommended assumptions applied:

- `GameRules` should be a `public final` utility-style class with a private constructor, because the current rules are pure calculations and the project currently avoids unnecessary object graphs.
- Rule method names should describe domain intent, not implementation: `alienScoreForWave(int wave)` and `alienSpeedForWave(int wave)`.
- The base speed and max speed tuning constants should move with the speed rule so future tuning has one obvious owner.
- The old `GameController.calculateAlienScore` and `calculateAlienSpeed` test seam should be removed after tests move, because keeping wrappers would preserve the coupling this slice is meant to eliminate.

## Phase 1: Extract Pure Rule Surface

### Overview

Introduce `GameRules` and move the pure scoring and alien speed behavior into it while preserving formulas exactly.

### Required Changes:

#### 1. Game Rules Class

**File**: `src/main/java/com/emenems/games/aliens/GameRules.java`

**Purpose**: Provide a focused home for current score and wave-speed calculations so future gameplay work does not need to reach into `GameController` for rule behavior.

**Contract**: Add a `public final class GameRules` with a private constructor and two public static methods: `alienScoreForWave(int wave)` returns `wave * 10`; `alienSpeedForWave(int wave)` returns `0.8 * Math.pow(1.15, wave - 1)` capped at `2.8`.

#### 2. Independent Rule Tests

**File**: `src/test/java/com/emenems/games/aliens/GameRulesTest.java`

**Purpose**: Prove the extracted rule behavior directly and keep rule regressions isolated from controller integration tests.

**Contract**: Move the assertions currently covering score scaling and alien speed scaling/capping from `GameControllerTest` into this new test class, updated to call `GameRules`.

### Success Criteria:

#### Automated Verification:

- `./mvnw clean compile` passes.
- `./mvnw test -Dtest=GameRulesTest` passes.

#### Manual Verification:

- Code review confirms `GameRules` contains only current pure rule calculations and no session state.

**Implementation Note**: After completing this phase and automated verification, continue directly to Phase 2 unless the extraction reveals a behavior mismatch.

---

## Phase 2: Wire Controller to Extracted Rules

### Overview

Replace controller-owned rule formulas with calls to `GameRules`, then remove the old controller static rule methods.

### Required Changes:

#### 1. Controller Delegation

**File**: `src/main/java/com/emenems/games/aliens/controller/GameController.java`

**Purpose**: Keep `GameController` focused on orchestration while delegating scoring and wave-speed calculations to `GameRules`.

**Contract**: Import `GameRules`; replace alien speed generation with `GameRules.alienSpeedForWave(wave)`; replace score accumulation with `GameRules.alienScoreForWave(wave)`; remove `BASE_ALIEN_SPEED`, `MAX_ALIEN_SPEED`, `calculateAlienScore`, and `calculateAlienSpeed` from the controller.

#### 2. Controller Test Cleanup

**File**: `src/test/java/com/emenems/games/aliens/controller/GameControllerTest.java`

**Purpose**: Keep controller tests focused on integration behavior rather than pure rule formulas.

**Contract**: Remove tests that call `GameController.calculateAlienScore` and `GameController.calculateAlienSpeed`; keep tests proving score is applied through missile collisions and wave-two scoring remains correct.

### Success Criteria:

#### Automated Verification:

- `./mvnw clean compile` passes.
- `./mvnw test` passes.

#### Manual Verification:

- Code review confirms `GameController` no longer owns scoring or alien-speed formulas.
- Code review confirms controller behavior tests still cover score application and wave advancement through public/package-private game behavior.

**Implementation Note**: After completing this phase and automated verification, continue to Phase 3 for documentation alignment.

---

## Phase 3: Align Documentation and Regression Evidence

### Overview

Update agent-facing documentation so future work follows the new rule boundary, then run the canonical refactor gate.

### Required Changes:

#### 1. Agent Guidance

**File**: `CLAUDE.md`

**Purpose**: Prevent future agents from following stale guidance that says difficulty tuning should remain in `GameController`.

**Contract**: Update the architecture guidance to state that scoring and wave-speed calculations live in `GameRules`, while `GameController` still owns orchestration, input, collisions, lives, game state, and panel updates.

#### 2. Final Verification

**File**: no source file; command verification only

**Purpose**: Confirm the slice preserved the locked refactor safety baseline.

**Contract**: Run the canonical commands from the S-01 baseline after implementation: `./mvnw clean compile` and `./mvnw test`.

### Success Criteria:

#### Automated Verification:

- `./mvnw clean compile` passes.
- `./mvnw test` passes.

#### Manual Verification:

- Run or hand off the S-01 smoke checklist because this refactor touches gameplay code that affects scoring and wave difficulty.
- Confirm gameplay is visibly unchanged: start, shoot aliens, score increases by wave, wave clears spawn the next wave, and Game Over/restart still work.

---

## Testing Strategy

### Unit Tests:

- `GameRulesTest` covers score per wave, speed starts at base, speed increases across waves, and speed never exceeds the cap.
- Existing `GameControllerTest` keeps behavior-level coverage for score application after missile collisions and wave-two scoring.

### Integration Tests:

- Full `./mvnw test` remains the integration safety net for controller, object movement, collisions, restart, and wave behavior.

### Manual Test Steps:

1. Run `./mvnw exec:java -Dexec.mainClass="com.emenems.games.aliens.Main"`.
2. Press Enter to start and confirm the window begins gameplay.
3. Hold Space to destroy at least one alien and confirm score increases.
4. Clear a wave or use normal play to observe that the next wave spawns and alien speed still feels progressively faster.
5. Lose all lives or let aliens reach the bottom, then press Enter to restart and confirm a fresh session.

## Performance Considerations

No meaningful performance impact is expected. The extracted methods are the same constant-time calculations currently executed by the controller.

## Migration Notes

No data migration or player-state migration is required. This is an internal refactor in a local desktop game.

## References

- Roadmap S-02: `context/foundation/roadmap.md:67`
- PRD FR-002: `context/foundation/prd.md`
- Safety baseline: `context/archive/2026-05-31-lock-refactor-safety-baseline/baseline.md:39`
- Current controller rule usage: `src/main/java/com/emenems/games/aliens/controller/GameController.java:104`, `src/main/java/com/emenems/games/aliens/controller/GameController.java:260`
- Current rule tests: `src/test/java/com/emenems/games/aliens/controller/GameControllerTest.java:523`

## Progress

> Convention: `- [ ]` pending, `- [x]` done. Add ` — <commit sha>` when a step is completed. Do not change step titles.

### Phase 1: Extract Pure Rule Surface

#### Automated

- [x] 1.1 `./mvnw clean compile` passes. — a4b1876
- [x] 1.2 `./mvnw test -Dtest=GameRulesTest` passes. — a4b1876

#### Manual

- [x] 1.3 Code review confirms `GameRules` contains only current pure rule calculations and no session state. — a4b1876

### Phase 2: Wire Controller to Extracted Rules

#### Automated

- [x] 2.1 `./mvnw clean compile` passes. — ffe570f
- [x] 2.2 `./mvnw test` passes. — ffe570f

#### Manual

- [x] 2.3 Code review confirms `GameController` no longer owns scoring or alien-speed formulas. — ffe570f
- [x] 2.4 Code review confirms controller behavior tests still cover score application and wave advancement. — ffe570f

### Phase 3: Align Documentation and Regression Evidence

#### Automated

- [x] 3.1 `./mvnw clean compile` passes. — 7573b16
- [x] 3.2 `./mvnw test` passes. — 7573b16

#### Manual

- [x] 3.3 Run or hand off the S-01 smoke checklist. — 7573b16
- [x] 3.4 Confirm gameplay is visibly unchanged for start, scoring, wave progression, Game Over, and restart. — 7573b16
