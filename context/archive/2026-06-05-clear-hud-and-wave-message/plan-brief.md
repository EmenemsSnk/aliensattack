# Clear HUD and wave message — Plan brief

> Full plan: `context/changes/clear-hud-and-wave-message/plan.md`

## What and Why

Polish the in-game presentation so the player can read session state more easily and gets a clear signal when a new wave starts. This delivers roadmap slice S-05 without changing the underlying arcade rules, controls, or progression.

## Starting Point

The game already has a passive HUD rendered by `GamePanel`, controller-owned wave progression in `GameController`, and session-owned short-lived scalar state in `GameSession`. What is missing is a cleaner HUD treatment and any state/rendering path for a temporary wave-start message.

## Desired End State

During play, score, wave, lives, and temporary effects remain visible but are grouped into a cleaner top-left HUD. When wave 1 starts or a later wave is reached, the player briefly sees a top-centered `WAVE <n>` banner that disappears automatically while gameplay continues underneath.

## Key Decisions

| Decision | Choice | Why |
| --- | --- | --- |
| HUD scope | Cleanup existing information, not a full redesign | Improves readability while minimizing regression risk |
| Wave banner placement | Top-centered overlay | Highly visible without covering the ship’s main action area |
| Start rule | Show on wave 1 and every later wave | Keeps the rule simple and consistent |
| Lifetime | Short timer in playing ticks, non-blocking | Fits the current Swing timer model and preserves gameplay flow |
| State ownership | `GameSession` | Matches existing ownership of transient scalar feedback |
| Render path | Push new scalar state through `GameController` into `GamePanel` | Reuses the established passive-panel architecture |
| Test depth | Session lifecycle, controller sequencing, panel helpers, plus full smoke regression | Protects both timing rules and presentation wiring |

## Scope

**In scope:**

- A cleaner grouped HUD for score, wave, lives, rapid fire, and combo
- A temporary top-centered wave-start banner for wave 1 and later waves
- Session-owned banner timer state with controller ticking and panel rendering
- Focused test additions plus compile/test/manual regression checks

**Out of scope:**

- Gameplay rebalance, pause, or control changes
- New assets, dependencies, or broad MVC refactors
- A blocking full-screen overlay or animation-heavy presentation pass
- Changes to scoring, life loss, wave speed, alien behavior, Game Over, or restart rules

## Architecture / Approach

Add minimal wave-message lifecycle state to `GameSession`, activate it from `startOrRestart()` and `advanceWave()`, and tick it only during the existing `PLAYING` loop in `GameController`. Extend the current scalar push into `GamePanel`, where the HUD is redrawn as a compact translucent card and the banner is rendered near the top center while the timer is active.

## Phases at a Glance

| Phase | Delivers | Key Risk |
| --- | --- | --- |
| 1. Session-owned wave message lifecycle | Deterministic timer state, activation paths, and lifecycle tests | Off-by-one timing or banner state drifting from the real wave |
| 2. HUD cleanup and wave banner rendering | Cleaner panel presentation and full regression verification | Readability improvements accidentally obscuring gameplay |

**Prerequisites:** S-04 is already complete, and the current compile/test baseline is green.
**Estimated effort:** About 1-2 implementation sessions across 2 phases.

## Open Risks and Assumptions

- The exact banner duration is a UX tuning choice and needs quick manual confirmation in live play.
- Tick-based timing should behave like rapid-fire/combo timing and should not advance outside `PLAYING`.
- The cleaned-up HUD must stay readable even when rapid-fire and combo labels are both visible.

## Success Criteria Summary

- The player can read core session state more easily without losing any existing HUD information.
- A visible `WAVE <n>` banner appears at the start of wave 1 and each later wave, then disappears automatically without blocking play.
- Existing start, combat, temporary effects, Game Over, and restart flows continue to work without regression.
