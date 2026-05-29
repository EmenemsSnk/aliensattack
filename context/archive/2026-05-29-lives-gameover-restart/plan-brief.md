# Lives, Game Over, and Restart — Brief Plan

> Full plan: `context/changes/lives-gameover-restart/plan.md`

## What and Why

Implement the final must-have gameplay loop: 3 lives, Game Over after the last collision, final score display, and Space-to-restart. This closes the PRD session path: launch, play, score, lose lives, reach Game Over, and restart without closing the app.

## Starting Point

The game already has a 60 FPS Swing timer, scoring, wave progression, score/wave HUD, and missile-alien collision handling. The remaining gap is that spaceship-alien collision is a no-op and there is no game state beyond continuous play.

## Desired End State

The game starts in play with score 0, wave 1, and 3 lives. A spaceship collision removes one life and the colliding alien; after the third lost life, gameplay stops behind a Game Over overlay showing the final score and restart prompt. Pressing Space on Game Over resets all session state and starts a fresh wave 1 game.

## Key Decisions

| Decision | Choice | Why |
| --- | --- | --- |
| State model | Minimal `GameState` enum: `PLAYING`, `GAME_OVER` | Matches the requested slice without adding the nice-to-have start menu. |
| Lives model | Discrete controller-owned lives count, default 3 | PRD calls for 3 lives and HUD display; percentage health is unused legacy state. |
| Collision repeat handling | Remove the colliding alien after life loss | Prevents one overlap from draining all lives across 60 FPS ticks. |
| Restart key | Reuse Space only while `GAME_OVER` | Preserves Space-to-fire during play and matches the Game Over prompt. |
| Rendering ownership | Panel receives pushed score, wave, lives, and state | Follows the existing scalar HUD push pattern and avoids controller reads from UI. |

## Scope

**In scope:**

- 3 lives and life loss on spaceship-alien collision.
- Game Over state after the last life.
- Lives HUD and Game Over overlay with final score.
- Space restart from Game Over to a clean score 0, wave 1, lives 3 session.
- Unit coverage for life loss, Game Over gating, and restart.

**Out of scope:**

- Start menu.
- Sound, alien fire, persistence, networking, or score history.
- New runtime dependencies.
- View/Controller refactor.

## Architecture / Approach

`GameController` remains the central gameplay owner. It stores `lives` and `gameState`, gates `tick()` when Game Over is active, handles Space differently based on state, and pushes passive render data into `GamePanel`. `GamePanel` only stores display state and draws HUD/overlay.

## Phases at a Glance

| Phase | Delivers | Key Risk |
| --- | --- | --- |
| 1. Lives and Game State Rules | Life loss, alien removal on ship collision, Game Over gating | Avoiding repeated life drain from one collision. |
| 2. HUD Lives, Game Over Overlay, and Restart | Lives HUD, overlay, Space restart, reset tests | Preserving existing Space-to-fire and scoring/wave behavior. |

**Prerequisites:** S-01 smooth loop and S-02 score/wave progression are already implemented and archived.
**Estimated effort:** One short implementation session across 2 focused phases.

## Open Risks and Assumptions

- Manual verification still requires launching the Swing app; no screenshot/assertion harness exists.
- Test coverage will focus on controller rules, not pixel-perfect overlay rendering.

## Success Criteria Summary

- Player sees score, wave, and lives during play.
- Three distinct collisions lead to Game Over, not one instant drain.
- Game Over shows final score and Space restart restores a fresh playable session.
