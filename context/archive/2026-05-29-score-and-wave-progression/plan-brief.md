# Score and Wave Progression — Brief Plan

> Full plan: `context/changes/score-and-wave-progression/plan.md`

## What and Why

Add the S-02 gameplay slice: score points for destroyed aliens, show score and wave in the HUD, and spawn faster waves after clearing the current row. This turns alien destruction into visible player progress while keeping scope away from the larger game-loop and Game Over work.

## Starting Point

The current controller seeds one fixed 10-alien row, removes aliens on missile collision, and repaints sprites, but it has no score, no wave state, no HUD, and no reusable wave generation. The roadmap marks S-02 as dependent on S-01; this implementation is corrected to run on the fresh master that contains S-01.

## Desired End State

The game starts at score 0 and wave 1. Destroying an alien adds `10 * wave` points; clearing all aliens increments the wave and spawns a new 10-alien row whose downward speed follows a capped `1.1^(wave - 1)` multiplier.

## Key Decisions

| Decision | Choice | Why |
| --- | --- | --- |
| Scope boundary | Implement S-02 only, leave S-01/S-03 out | User requested this change-id specifically and the repo has separate roadmap slices |
| Score formula | `10 * currentWave` | Matches PRD FR-007 and roadmap S-02 |
| Wave speed | Base 1 px/tick, multiplier 1.1, cap 2 px/tick | Matches the fresh-master 60 FPS timer loop while preserving the ~2x cap |
| Wave shape | Reuse the existing 10 x-coordinate row | Smallest change that preserves current rendering/gameplay structure |
| Test target | Pure formula methods on `GameController` | Avoids brittle Swing tests while covering the business rules |

## Scope

**In scope:**

- Score state in `GameController`
- Wave state and reusable alien generation
- Configurable alien movement speed
- HUD text for score and wave in `GamePanel`
- JUnit tests for score and wave speed formulas

**Out of scope:**

- 60 FPS Timer conversion
- Off-screen cleanup and collision-loop cleanup
- Lives, Game Over, restart, final score screen
- Start menu, audio, alien fire, persistence, new libraries

## Architecture / Approach

`GameController` remains the source of gameplay state. It calculates score and wave speed, mutates the shared alien list, and pushes primitive HUD values into `GamePanel`; `GamePanel` only renders those values.

## Phases at a Glance

| Phase | Delivers | Key Risk |
| --- | --- | --- |
| 1. Domain State and Wave Rules | Speed-aware aliens and reusable wave state | Touches controller state before scoring is visible |
| 2. Score, HUD, and Next-Wave Progression | Visible score/wave and next wave spawning | Must trigger after both collision removal and off-screen cleanup |
| 3. Formula Tests and Final Verification | Regression tests for score/speed formulas | Swing behavior still needs manual observation |

**Prerequisites:** Build tooling baseline and S-01 smooth playable loop are present from fresh `origin/master`.
**Estimated Effort:** One compact implementation session across 3 phases.

## Open Risks and Assumptions

- The exact gameplay feel of speed increases may need later tuning after more playtesting.
- Manual UI verification is still required because there is no Swing UI test harness.

## Success Criteria Summary

- HUD shows current score and wave.
- Destroyed aliens add the correct wave-scaled points.
- Clearing a wave spawns the next faster wave without adding dependencies or refactoring the architecture.
