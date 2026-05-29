# Wave Boundaries and Hit Feedback — Brief Plan

> Full plan: `context/changes/wave-boundaries-and-hit-feedback/plan.md`

## What and Why

Implement the S-04 gameplay hardening slice: keep the ship inside the board, make wave spawn positions varied and non-overlapping, show a visible hit reaction when a life is lost, and end the game when aliens reach the bottom before being cleared. The MVP loop is already playable, so this plan tightens the rules that make the game feel fair and complete.

## Starting Point

The current controller already owns movement, collision, waves, lives, Game Over, and restart. The gaps are that ship movement is unbounded, alien waves spawn in one uniform row, offscreen cleanup can remove invading aliens before a loss condition is applied, and Game Over rendering has only one fixed title.

## Desired End State

The player cannot move the ship outside the visible panel. Every wave starts with 10 aliens in a lightly randomized varied layout within the top fifth of the board and without overlap. Ship hits create a short red visual feedback state, while uncleared aliens reaching the bottom end the game with `ALIENS WIN` and the existing Space restart flow.

## Key Decisions

| Decision | Choice | Why |
| --- | --- | --- |
| Ship bounds | Clamp after controller movement | Covers every held-key path while keeping existing movement methods simple. |
| Spawn variation | Safe-lane random jitter per wave | Gives visible variation between waves while preserving no-overlap/top-fifth guarantees. |
| Alien win condition | First alien touching bottom edge | Clear, easy to understand, and prevents cleanup from turning failure into a new wave. |
| Hit feedback | Controller-owned tick countdown pushed to panel | Keeps gameplay state centralized and avoids blocking the Swing EDT. |
| Game Over title | Push title string with panel state | Minimal change that supports both `GAME OVER` and `ALIENS WIN`. |

## Scope

**In scope:**

- Ship boundary clamp.
- Varied non-overlapping top-fifth alien spawn.
- Aliens-win loss condition.
- Visual hit feedback for life loss.
- Unit coverage for all new gameplay rules.

**Out of scope:**

- Fully free-form random spawn that can overlap or leave the top-fifth start area.
- Start Menu, audio, alien fire, persistence, networking, or score history.
- New runtime dependencies.
- View/Controller refactor.

## Architecture / Approach

`GameController` remains the gameplay owner. It clamps the ship after movement, generates aliens from safe lanes with bounded random jitter, detects bottom-edge invasion before cleanup, owns transient hit feedback state, and pushes passive render state to `GamePanel`. `GamePanel` only renders the pushed state: hit tint/border and the provided Game Over title.

## Phases at a Glance

| Phase | Delivers | Key Risk |
| --- | --- | --- |
| 1. Boundaries, Spawn Layout, Invasion Loss, and Hit Feedback | Full S-04 behavior plus unit tests | Tick ordering must check invasion before cleanup/wave advancement. |

**Prerequisites:** S-03 lives/gameover/restart is implemented and archived.
**Estimated effort:** One focused implementation session.

## Open Risks and Assumptions

- Manual verification still requires launching the Swing app; no screenshot harness exists.
- Spawn variation is randomized, but constrained by lanes so tests can still verify no overlap and top-fifth placement.
- `ALIENS WIN` is the chosen message for uncleared-wave failure.

## Success Criteria Summary

- Ship remains fully visible under all arrow-key movement.
- Aliens spawn varied, non-overlapping, and within the top fifth of the board.
- Collisions visibly signal life loss, and bottom-reaching aliens end the game with `ALIENS WIN`.
