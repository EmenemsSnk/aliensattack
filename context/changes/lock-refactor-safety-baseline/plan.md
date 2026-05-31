# Lock Refactor Safety Baseline Implementation Plan

## Overview

Lock a repeatable regression baseline before the next Refactor Pack slices extract `GameRules` and `GameSession` from `GameController`. This change makes the current safety signal explicit, fills a few targeted test gaps, and records the manual smoke path that future refactors must preserve.

## Current State Analysis

Aliens Attack is already a playable Java 21 + Maven + Swing MVP. `GameController` remains the central gameplay owner: it gates ticks by `GameState`, owns score, wave, lives, hit feedback, fire cooldown, collisions, alien fire, wave generation, restart, and the scalar state pushed into `GamePanel` (`src/main/java/com/emenems/games/aliens/controller/GameController.java:184`, `src/main/java/com/emenems/games/aliens/controller/GameController.java:342`). `Main` constructs the entity lists once and passes the same references to both `GamePanel` and `GameController` (`src/main/java/com/emenems/games/aliens/Main.java:20`).

The existing automated baseline is healthy. On 2026-05-31, `./mvnw clean compile` built 12 production source files successfully, and `./mvnw test` passed 37 tests across 3 test classes. CI already runs the same compile and test commands on push and pull request (`.github/workflows/build.yml:21`).

The gap is not infrastructure; it is an auditable definition of "safe to refactor". The PRD explicitly says green tests are required but not sufficient, because existing tests may not cover every visible behavior and a short manual smoke check remains necessary (`context/foundation/prd.md:86`). The roadmap makes this change S-01 and positions it before `extract-game-rules` and `extract-game-session` (`context/foundation/roadmap.md:55`).

## Desired End State

After this plan is implemented, the change folder contains a `baseline.md` artifact that records the current automated baseline, the manual smoke checklist, and the required future gate. Focused regression tests protect the session/rules behavior most likely to move during later extraction work. The canonical safety gate is `./mvnw clean compile` plus `./mvnw test` before and after each refactor slice, with manual smoke verification for visible gameplay.

### Key Findings:

- `context/foundation/roadmap.md:55` defines this change as the first Refactor Pack slice and links it to FR-004.
- `context/foundation/prd.md:86` requires existing tests to pass and warns that manual smoke verification is still needed.
- `.github/workflows/build.yml:21` already runs compile and test, so S-01 should align with the current CI gate rather than introduce new infrastructure.
- `src/test/java/com/emenems/games/aliens/controller/GameControllerTest.java:24` already covers most controller behavior and is the correct place for focused gap-fill tests.

## What We Are NOT Doing

- No extraction of `GameRules`, `GameSession`, or any new production gameplay abstraction.
- No production gameplay behavior changes.
- No Swing rendering rewrite or GUI automation harness.
- No new runtime or test dependencies.
- No CI workflow expansion beyond documenting the existing compile/test gate.
- No broad cleanup, formatting, or refactor work outside the baseline artifacts and focused tests.

## Implementation Approach

Keep S-01 as a safety slice. First create a change-scoped `baseline.md` that turns the current baseline into an artifact future agents can read. Then add only focused tests that lock behavior likely to be disturbed by later scoring/wave/session extraction. Finally run the canonical automated gate, update `baseline.md` with the final implementation evidence, and leave manual checklist completion for a human to confirm.

## Phase 1: Baseline Evidence Artifact

### Overview

Create the durable baseline artifact that defines what future refactor slices must preserve.

### Required Changes:

#### 1. Refactor safety baseline document

**File**: `context/changes/lock-refactor-safety-baseline/baseline.md`

**Purpose**: Make the safety baseline auditable and easy to archive with S-01.

**Contract**: Create a Markdown document with these sections: scope, current automated baseline, canonical safety gate, focused test coverage, manual smoke checklist, and evidence log. Record 2026-05-31 as the initial baseline date, list `./mvnw clean compile` and `./mvnw test` as passing, and record the current test count as 37 tests before implementation adds any new tests.

#### 2. Manual smoke checklist

**File**: `context/changes/lock-refactor-safety-baseline/baseline.md`

**Purpose**: Satisfy the PRD requirement that green tests are not the only confidence signal.

**Contract**: Include a short repeatable checklist covering launch, Enter start, movement/clamping, hold Space firing, scoring/wave progression, life loss/hit feedback, Game Over, and restart. Leave checklist status as pending unless a human confirms it during implementation.

### Success Criteria:

#### Automated Verification:

- `context/changes/lock-refactor-safety-baseline/baseline.md` exists.
- `baseline.md` names `./mvnw clean compile` and `./mvnw test` as the canonical automated gate.
- `baseline.md` records the pre-change baseline as 37 passing tests on 2026-05-31.
- `baseline.md` includes a pending manual smoke checklist.

#### Manual Verification:

- The checklist is specific enough for a future implementer to run without rereading the full PRD.
- The artifact clearly states that production gameplay refactoring is out of scope for S-01.

**Implementation note**: Do not mark manual smoke complete unless the game is actually launched and checked by a human.

---

## Phase 2: Focused Regression Tests

### Overview

Add a small number of JUnit tests that improve confidence for the next two refactor slices without changing production behavior.

### Required Changes:

#### 1. Session reset and held-input regression tests

**File**: `src/test/java/com/emenems/games/aliens/controller/GameControllerTest.java`

**Purpose**: Lock reset behavior that will be easy to disturb when session state moves out of the controller.

**Contract**: Add focused tests around reset/session boundaries that preserve current behavior: restart clears held fire state and fire cooldown so Space does not auto-fire after restart unless pressed again; restart keeps the player at the same current coordinates; restart clears missiles and alien missiles while spawning a fresh wave. Use existing package-private controller seams and helper factories, with no GUI or audio dependency.

#### 2. Active-wave scoring regression test

**File**: `src/test/java/com/emenems/games/aliens/controller/GameControllerTest.java`

**Purpose**: Lock the connection between current wave state and score calculation before `GameRules` extraction.

**Contract**: Add a test proving that after the controller advances to wave 2, a missile kill adds the wave-2 score value rather than the default wave-1 value. The test should drive existing public/package-private behavior and avoid reaching into private fields.

#### 3. Baseline document coverage update

**File**: `context/changes/lock-refactor-safety-baseline/baseline.md`

**Purpose**: Keep the baseline artifact consistent with the new regression tests.

**Contract**: Update the focused test coverage section to name the added behaviors and update the evidence log with the post-change total after `./mvnw test` is run in Phase 3.

### Success Criteria:

#### Automated Verification:

- Project compiles: `./mvnw clean compile`
- Full test suite passes: `./mvnw test`
- Added tests fail against a broken reset/held-fire or active-wave scoring implementation.
- No production source file under `src/main/java/` is changed for this phase.

#### Manual Verification:

- The added tests read as behavioral regression tests, not implementation-lock tests for private controller structure.
- Test additions are narrow enough that future valid refactors can keep them green without preserving the current monolithic controller internals.

**Implementation note**: The implementation may edit `GameControllerTest.java` and `baseline.md`; it should not modify `GameController.java` or other production logic.

---

## Phase 3: Verification Gate and Handoff

### Overview

Run the canonical safety gate, record final evidence, and hand off S-02/S-03 with a clear baseline contract.

### Required Changes:

#### 1. Final evidence update

**File**: `context/changes/lock-refactor-safety-baseline/baseline.md`

**Purpose**: Record the actual post-implementation baseline result.

**Contract**: Add a dated evidence entry for `./mvnw clean compile` and `./mvnw test`, including the final test count, pass/fail status, and whether manual smoke remains pending or has been completed by a human.

#### 2. Optional future-reference note

**File**: `CLAUDE.md`

**Purpose**: Make the baseline discoverable to future agents only if the implementer decides the archived S-01 artifact would otherwise be missed.

**Contract**: If edited, add one concise note pointing future refactor work to the archived `lock-refactor-safety-baseline` baseline artifact after S-01 is complete. Do not rewrite architecture guidance or duplicate the whole checklist.

### Success Criteria:

#### Automated Verification:

- Project compiles: `./mvnw clean compile`
- Full test suite passes: `./mvnw test`
- `git diff -- src/main/java` is empty or contains only an explicitly justified documentation-adjacent change; no production gameplay logic is modified.
- `baseline.md` contains final automated evidence with the post-change test count.

#### Manual Verification:

- Manual smoke checklist is either completed by a human or explicitly left pending with clear steps.
- The baseline handoff clearly says future refactor slices must run compile + full test before and after changes.

**Implementation note**: Stop after S-01 is locked. Do not begin `extract-game-rules` or `extract-game-session` in this change.

---

## Testing Strategy

### Unit Tests:

- Extend `GameControllerTest` with focused regression tests for held-fire/reset behavior and active-wave scoring.
- Preserve all existing controller tests for state gating, movement, collisions, alien fire, cleanup, restart, score, wave, and difficulty formulas.
- Do not add mocks or external test libraries; use the existing deterministic `Random` and package-private controller seams.

### Integration Tests:

- No automated Swing integration harness is introduced.
- CI remains the existing GitHub Actions compile + test workflow.

### Manual Testing Steps:

1. Run `./mvnw exec:java -Dexec.mainClass="com.emenems.games.aliens.Main"`.
2. Confirm the Swing window opens to the start menu.
3. Press Enter and confirm gameplay starts.
4. Move with arrow keys and confirm the ship remains within the board.
5. Hold Space and confirm repeated firing with cooldown.
6. Destroy aliens and confirm score/wave behavior appears unchanged.
7. Lose lives via alien or alien missile contact and confirm hit feedback/Game Over.
8. Press Enter on Game Over and confirm restart clears projectiles and starts a fresh session.

## Performance Considerations

This change should have no runtime performance impact. The only code changes are tests and baseline documentation. Avoid production instrumentation or per-tick logging.

## Migration Notes

No player data, saves, or external integrations exist. No migration is required.

## References

- Roadmap S-01: `context/foundation/roadmap.md:55`
- PRD FR-004 and manual-smoke warning: `context/foundation/prd.md:86`
- CI compile/test gate: `.github/workflows/build.yml:21`
- Controller behavior under test: `src/main/java/com/emenems/games/aliens/controller/GameController.java:184`
- Controller regression tests: `src/test/java/com/emenems/games/aliens/controller/GameControllerTest.java:24`
- Shared-list startup wiring: `src/main/java/com/emenems/games/aliens/Main.java:20`

## Progress

> Convention: `- [ ]` pending, `- [x]` done. Add ` — <commit sha>`, when a step is committed. Do not change step titles. See `references/progress-format.md`.

### Phase 1: Baseline Evidence Artifact

#### Automated

- [x] 1.1 `baseline.md` exists — 39ab235
- [x] 1.2 `baseline.md` names `./mvnw clean compile` and `./mvnw test` as the canonical automated gate — 39ab235
- [x] 1.3 `baseline.md` records the pre-change baseline as 37 passing tests on 2026-05-31 — 39ab235
- [x] 1.4 `baseline.md` includes a pending manual smoke checklist — 39ab235

#### Manual

- [x] 1.5 Checklist is specific enough for a future implementer — 39ab235
- [x] 1.6 Artifact states production gameplay refactoring is out of scope for S-01 — 39ab235

### Phase 2: Focused Regression Tests

#### Automated

- [x] 2.1 Project compiles: `./mvnw clean compile` — a915e80
- [x] 2.2 Full test suite passes: `./mvnw test` — a915e80
- [x] 2.3 Added tests fail against broken reset/held-fire or active-wave scoring behavior — a915e80
- [x] 2.4 No production source file under `src/main/java/` is changed — a915e80

#### Manual

- [x] 2.5 Added tests read as behavioral regression tests — a915e80
- [x] 2.6 Test additions do not lock private controller structure — a915e80

### Phase 3: Verification Gate and Handoff

#### Automated

- [x] 3.1 Project compiles: `./mvnw clean compile` — d75164e
- [x] 3.2 Full test suite passes: `./mvnw test` — d75164e
- [x] 3.3 No production gameplay logic is modified — d75164e
- [x] 3.4 `baseline.md` contains final automated evidence with the post-change test count — d75164e

#### Manual

- [x] 3.5 Manual smoke checklist is completed or explicitly left pending — d75164e
- [x] 3.6 Baseline handoff says future refactor slices must run compile + full test before and after changes — d75164e
