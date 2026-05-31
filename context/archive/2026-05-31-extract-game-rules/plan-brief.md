# Extract Game Rules — Brief Plan

> Full plan: `context/changes/extract-game-rules/plan.md`

## What and Why

Extract the current scoring and alien wave-speed calculations from `GameController` into a focused `GameRules` class. This delivers roadmap slice S-02 and makes the gameplay foundation easier to extend without changing what the player sees.

## Starting Point

`GameController` currently owns orchestration, collisions, input, session state, and the pure formulas for score and alien speed. The roadmap says this slice should extract only scoring and wave rules; session extraction is explicitly the next slice.

## Desired End State

Developers can work with scoring and wave-speed rules through `GameRules`. `GameController` delegates to that class, tests cover the rules directly, and visible gameplay remains unchanged.

## Key Decisions

| Decision | Choice | Why |
| --- | --- | --- |
| Scope | Score and alien speed only | Matches roadmap S-02 and avoids pulling session state into this small refactor. |
| Class shape | `public final GameRules` with static pure methods | Current rules are stateless formulas; this avoids premature object design. |
| Package | Root `com.emenems.games.aliens` | Keeps core game concepts near `GameConstants` and `GameState`. |
| Tuning constants | Move base speed and max speed into `GameRules` | The constants define the extracted speed rule and should live with it. |
| Old controller seams | Remove static formula methods from `GameController` | Keeping wrappers would preserve the coupling this slice is meant to eliminate. |
| Verification | Compile, targeted rule test, full test suite, manual smoke handoff | Follows the S-01 baseline and covers both pure rules and visible behavior. |

## Scope

**In scope:**

- Add `GameRules` for current alien score and wave-speed calculations.
- Add `GameRulesTest` for direct rule coverage.
- Update `GameController` to delegate scoring and wave-speed calculation.
- Remove controller-owned pure rule methods and stale tests.
- Update `CLAUDE.md` guidance so future agents use the new rule boundary.

**Out of scope:**

- `GameSession` extraction or moving score/wave/lives/game-state ownership.
- Gameplay tuning, new rules, new enemies, power-ups, or balancing changes.
- UI, audio, input, persistence, CI, or dependency changes.

## Architecture / Approach

Create a small pure-rule boundary first, then wire the existing controller to it. The controller still owns the tick pipeline, collisions, state transitions, shared mutable lists, panel updates, audio calls, and session reset; only score and alien speed formulas move.

## Phases in Brief

| Phase | What it Delivers | Key Risk |
| --- | --- | --- |
| 1. Extract Pure Rule Surface | `GameRules` plus direct rule tests | Accidentally changing the speed formula or score value. |
| 2. Wire Controller to Extracted Rules | Controller delegates to `GameRules` and old formula seams disappear | Breaking existing controller tests that relied on package-private static methods. |
| 3. Align Documentation and Regression Evidence | `CLAUDE.md` reflects the new boundary and canonical checks run | Stale guidance could send later agents back to `GameController`. |

**Prerequisites:** S-01 safety baseline is archived; worktree changes are limited to this change folder before implementation.
**Estimated effort:** One focused implementation session across three small phases.

## Open Risks and Assumptions

- Assumption: the user intended `extract-game-rules`; `cextract-game-rules` was treated as a typo because the existing change folder is `context/changes/extract-game-rules/`.
- Assumption: no manual Q&A is needed because the recommended path is already defined by PRD and roadmap.
- Risk: green tests do not prove visible gameplay feel, so the manual S-01 smoke checklist remains required after implementation.

## Success Criteria Summary

- `GameRules` is the only owner of alien score and wave-speed formulas.
- `./mvnw clean compile` and `./mvnw test` pass after implementation.
- Manual smoke confirms scoring, wave progression, Game Over, and restart are visibly unchanged.
