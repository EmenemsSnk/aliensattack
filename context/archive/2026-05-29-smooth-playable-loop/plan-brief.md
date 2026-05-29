# Smooth Playable Loop — Plan Brief

> Full plan: `context/changes/smooth-playable-loop/plan.md`
> Roadmap: `context/foundation/roadmap.md`
> PRD: `context/foundation/prd.md`

## What & why

Make the current Aliens Attack prototype actually playable for the first roadmap slice. The goal is not the full MVP loop yet; it is the smallest useful core: smooth movement, firing, one-time missile hits, and no unbounded object buildup.

## Starting point

`GameController` currently starts a background `Thread` that sleeps for 1000 ms, mutates game state off the Swing EDT, and repaints roughly once per second. A Swing `Timer` path exists in shape only, input can be processed twice, missile collisions remove objects through overlapping passes, and off-screen cleanup is still a TODO.

## Desired end state

The game starts directly in PLAYING behavior with a `javax.swing.Timer` ticking at 16 ms. Arrow keys and Space stay responsive, missiles and aliens move smoothly, a missile hit removes exactly one missile/alien pair, and missiles/aliens that leave the board are removed from the existing shared lists.

## Key decisions made

| Decision | Choice | Why |
|---|---|---|
| Scope | S-01 only | Keeps this change aligned to the roadmap and avoids mixing in scoring, lives, or Game Over. |
| Loop | `javax.swing.Timer` at 16 ms | Matches Swing EDT rules and the PRD's ~60 FPS goal. |
| Input | `keyPressed()` only | Removes duplicate action handling while preserving the existing simple input model. |
| Cleanup | Remove off-screen missiles and aliens | Satisfies FR-002 and prevents list growth during sustained play. |
| Tests | Package-private testable logic | Allows JUnit coverage without GUI automation or new libraries. |

## Scope

**In scope:**
- Replace `Thread.sleep(1000)` loop with EDT-safe Swing timer.
- Remove duplicate key handling.
- Add off-screen cleanup for missiles and aliens.
- Make missile-alien collision resolution deterministic and single-pass.
- Add focused JUnit tests for cleanup and collision logic.
- Clean unused imports/TODOs related to the old prototype loop.

**Out of scope:**
- Score, HUD, wave progression, and speed scaling.
- Lives, Game Over, restart, and Start Menu.
- View/Controller refactor or separate model object.
- New runtime dependencies or GUI test framework.
- Hard FPS benchmark/profiling.

## Architecture / approach

Keep the current shared-state architecture: `Main` creates the lists, `GamePanel` renders them, and `GameController` mutates them. The controller becomes the only game-loop owner: `initialize()` starts the timer, `actionPerformed()` performs one tick, helper methods handle collision and cleanup in place, and rendering continues through `gamePanel.repaint()`.

## Phases at a glance

| Phase | Delivers | Key risk |
|---|---|---|
| 1. Controller loop and input | EDT timer + single-path keyboard actions | Movement may feel too fast at 16 ms and needs manual feel check. |
| 2. Cleanup and collision semantics | Off-screen removal + deterministic hit handling + tests | Must mutate existing list instances, not replace shared state. |
| 3. Rendering cleanup and final verification | Dead-code cleanup + full manual/automated S-01 check | Avoid slipping into S-02/S-03 features. |

**Prerequisites:** Current `./mvnw test` baseline is green; no upstream frame/research artifact is required.
**Estimated effort:** ~1 focused implementation session in 3 phases.

## Open risks & assumptions

- At 16 ms, current 5 px/tick movement may feel fast; calibration is allowed only insofar as S-01 remains smooth and playable.
- Manual verification is required for perceived smoothness because the project has no GUI automation harness.
- If aliens leave the board before S-02 wave progression exists, the board can become empty; that is acceptable for this slice.

## Success criteria (summary)

- `./mvnw clean compile` and `./mvnw test` pass.
- The game runs smoothly via `./mvnw exec:java -Dexec.mainClass="com.emenems.games.aliens.Main"`.
- Space and arrow controls remain responsive without duplicate actions.
- Missile hits remove one missile and one alien, and off-screen objects stop accumulating.
