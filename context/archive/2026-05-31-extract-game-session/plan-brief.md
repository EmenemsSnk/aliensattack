# Extract Game Session — Brief Plan

> Full plan: `context/changes/extract-game-session/plan.md`

## What and Why

Extract the scalar arcade-session state from `GameController` into a focused `GameSession` model. This keeps the existing player-visible loop unchanged while giving future gameplay work a clearer place for score, wave, lives, game state, reset, hit feedback, and Game Over title behavior.

## Starting Point

`GameController` currently owns scalar state, input, collisions, spawning, audio, entity-list mutation, timer callbacks, and panel updates. S-02 already moved scoring and wave-speed formulas into `GameRules`, so this plan builds on that by moving session lifecycle state without touching gameplay tuning.

## Desired End State

`GameSession` owns scalar session lifecycle and exposes behavior methods for start/restart, alien kill scoring, wave advancement, life loss, hit-feedback ticking, and Game Over. `GameController` still coordinates shared entity lists, input, collisions, spawning, audio, repainting, and panel state pushes. Players see the same start, movement, firing, scoring, wave, lives, Game Over, and restart behavior.

## Key Decisions

| Decision | Choice | Why |
| --- | --- | --- |
| Session boundary | Scalar state only | Preserves the existing shared-list wiring between `Main`, `GamePanel`, and `GameController`. |
| Class shape | Mutable `GameSession` domain object | Session lifecycle is stateful; a utility class would only move static helpers, not ownership. |
| Package | Root `com.emenems.games.aliens` | Matches `GameRules`, `GameState`, and `GameConstants` as core domain concepts. |
| Reset split | Scalar reset in `GameSession`, list/input/spawn reset in controller | Keeps one clear controller reset boundary while avoiding entity-list ownership drift. |
| Testing | Add `GameSessionTest`, preserve `GameControllerTest` behavior tests | Direct unit coverage catches session regressions; controller tests prove integration stayed unchanged. |

## Scope

**In scope:**

- New `GameSession` class and focused session unit tests.
- Controller delegation for score, wave, lives, game state, hit feedback, and game-over title.
- Existing controller regression tests kept green and adjusted only where internals moved.
- `CLAUDE.md` architecture guidance updated after extraction.
- Compile, focused tests, full tests, and manual smoke handoff.

**Out of scope:**

- Moving `spaceship`, `missiles`, `alienMissiles`, or `aliens` into `GameSession`.
- Changing gameplay balance, spawn count, speeds, score values, lives count, or Game Over titles.
- UI/rendering rewrite, observer pattern, data persistence, high scores, power-ups, or new alien types.
- New runtime or test dependencies.

## Architecture / Approach

`GameSession` becomes the scalar state model. `GameController` asks it whether the game is playing, delegates scalar transitions to it, and reads values back for spawning, scoring, and `GamePanel.updateGameState(...)`. Entity collections remain shared mutable lists, so rendering stays synchronized exactly as it does today.

## Phases At A Glance

| Phase | Delivers | Key Risk |
| --- | --- | --- |
| 1. Extract scalar session model | `GameSession` plus direct unit tests | Accidentally duplicating or changing current reset/scoring/lives semantics. |
| 2. Wire controller through session | Controller no longer owns scalar fields directly | Reset order or tick gating could drift and affect visible behavior. |
| 3. Documentation and regression gate | Updated agent guidance plus full compile/test gate | Automated tests may miss a visible Swing gameplay regression. |

**Prerequisites:** S-01 safety baseline and S-02 `GameRules` extraction are complete and archived.
**Estimated effort:** ~2-3 implementation sessions across 3 phases.

## Open Risks and Assumptions

- Assumption: `GameSession` should not own shared entity lists; preserving current panel/controller wiring is safer.
- Risk: Reset behavior mixes scalar state, input state, and collection cleanup today; implementation must keep the visible reset outcome identical.
- Risk: Green JUnit tests are not sufficient for this refactor, so the S-01 manual smoke checklist remains required or explicitly handed off.

## Success Criteria Summary

- `GameSession` owns score, wave, lives, game state, hit feedback, game-over title, and scalar reset lifecycle.
- `GameController` behavior tests and full test suite pass after delegation.
- Manual smoke confirms start, movement, firing, scoring, wave progression, life loss, Game Over, alien-invasion Game Over, and restart still behave the same.
