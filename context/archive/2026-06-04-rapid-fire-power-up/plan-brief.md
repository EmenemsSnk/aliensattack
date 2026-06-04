# Rapid Fire Power-Up — Plan Brief

> Full plan: `context/changes/rapid-fire-power-up/plan.md`

## What and Why

Deliver the first Replayability slice: aliens can drop a temporary rapid-fire collectible, and the player can collect it, feel the faster firing cadence, see its remaining duration, and observe its expiration. This validates the new power-up direction while preserving the shipped arcade loop.

## Starting Point

The game already has deterministic collision handling, held-space firing with a fixed 10-tick cooldown, shared rendered entity lists, scalar session state, and a passive HUD. It has no collectibles, drop lifecycle, temporary firing modifier, or temporary-effect HUD state.

## Desired End State

Each resolved alien kill has a 12% chance to create a falling rapid-fire drop. Collection activates rapid-fire for 180 playing ticks, reducing firing cooldown to 4 ticks; another collection refreshes the duration. The HUD shows `RAPID FIRE: Ns`, while life loss cancels the effect and missed drops, expiration, and restart complete the visible lifecycle.

## Key Decisions

| Decision | Choice | Why |
| --- | --- | --- |
| Drop chance | 12% per resolved alien kill | Makes the mechanic observable without making it constant |
| Firing strength | 4-tick cooldown instead of 10 | Produces a clearly felt but bounded acceleration |
| Duration | 180 playing ticks, about 3 seconds | Manual playtesting found the original 5-second window too long |
| Repeat collection | Refresh to 180 ticks | Avoids unbounded stacking while rewarding collection |
| Life loss | Effect is cancelled | Manual playtesting found keeping the advantage after damage too forgiving |
| Missed drop | Falls offscreen and disappears | Preserves active collection and existing cleanup conventions |
| Player feedback | HUD label with remaining seconds | Makes activation and expiration unambiguous |
| Test depth | Rules plus full controller lifecycle | Protects timing, reset, collision, and cooldown behavior |

## Scope

**In scope:**

- Falling `RapidFirePowerUp` entity and shared drop list
- 12% deterministic drop generation from resolved player-missile kills
- Ship collection, 180-tick activation/refresh, life-loss cancellation, and expiration
- 4-tick rapid-fire cooldown versus normal 10-tick cooldown
- HUD countdown, restart cleanup, tests, and architecture guidance

**Out of scope:**

- Score combo, new alien type, other power-ups, stacking, inventory, or permanent upgrades
- New image/audio assets or dependencies
- Cancelling rapid-fire on wave advancement
- Broad controller, session, or HUD refactors

## Architecture / Approach

`Main` creates one shared `List<RapidFirePowerUp>` for `GameController` mutation and `GamePanel` rendering. `GameController` owns drop spawning, movement, cleanup, collection, and cooldown selection. `GameSession` owns the scalar active-effect timer and reset lifecycle. `GamePanel` receives timer state through the existing scalar push and renders the drop plus HUD indicator.

## Phases at a Glance

| Phase | Delivers | Key Risk |
| --- | --- | --- |
| 1. Model and session effect | Falling collectible and tested 180-tick scalar lifecycle | Putting state in the wrong ownership boundary |
| 2. Controller integration | Drop, collection, refresh, cleanup, and faster held fire | Tick/collision ordering causing duplicate or inconsistent behavior |
| 3. Rendering and regression | Visible drop, HUD countdown, full verification | Effect feels unclear or rapid-fire adds visible EDT pressure |

**Prerequisites:** Existing compile/test baseline remains green.

**Estimated effort:** About 2-3 implementation sessions across 3 phases.

## Open Risks and Assumptions

- A 12% random drop rate can still produce streaks; manual playtesting must confirm the mechanic feels present but not dominant.
- Rapid-fire creates more missiles; existing cleanup should bound the list, but sustained play needs manual verification.
- HUD seconds use the existing approximately 60 FPS tick rate, so the display is gameplay-relative rather than wall-clock precise.

## Success Criteria Summary

- The player can see, collect, use, refresh, and outlast a rapid-fire drop during a normal session.
- Rapid-fire visibly accelerates held-space shooting and the HUD accurately communicates its lifecycle.
- Existing gameplay and the full automated suite remain green; restart clears all transient power-up state.
