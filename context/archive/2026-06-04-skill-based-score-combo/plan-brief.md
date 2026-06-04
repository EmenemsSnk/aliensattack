# Skill-Based Score Combo — Plan Brief

> Full plan: `context/changes/skill-based-score-combo/plan.md`

## What and Why

Add a score combo that rewards quick consecutive alien kills while preserving aim and timing as the source of higher scores. A short delay, life loss, or wave transition ends the chain, so rapid fire helps create opportunities without automatically replacing player skill.

## Starting Point

The game already awards `10 * wave` points per resolved alien kill, owns scalar gameplay state in `GameSession`, resolves all missile kills for a tick as one deduplicated batch, and pushes score/effect state to a passive HUD. Rapid fire increases shot frequency for three seconds but currently has no interaction with scoring beyond creating more kill opportunities.

## Desired End State

The first kill starts a 1.5-second combo window at x1. Each later scoring tick inside that window immediately increases the multiplier by one, capped at x5, and refreshes the window; all kills resolved in the same tick use one shared multiplier and can increase it only once. The HUD shows `COMBO xN` and remaining time from x2 onward, while timeout, life loss, wave advancement, and restart reset the combo.

## Key Decisions

| Decision | Choice | Why |
| --- | --- | --- |
| Combo window | 90 playing ticks, about 1.5 seconds | Rewards quick follow-up hits without requiring rapid fire |
| Progression | Increase by one per consecutive scoring tick | Keeps the mechanic simple and readable |
| Maximum multiplier | x5 | Bounds score inflation and rapid-fire amplification |
| Score timing | New multiplier applies immediately | Makes the rewarding hit visibly valuable |
| Same-tick kills | One multiplier increase; all kills share it | Prevents a rapid-fire collision batch from skipping multiplier levels |
| Wave transition | Reset combo | Makes each wave a fresh scoring challenge |
| Missed shots | Do not reset combo | Lost time is already the penalty and rapid fire remains useful |
| HUD | Show multiplier and remaining time from x2 | Makes the mechanic understandable and actionable |
| Test depth | Rules, session lifecycle, controller integration, and HUD conversion | Protects scoring math, reset paths, and rapid-fire interaction |

## Scope

**In scope:**

- 90-tick combo timer, x1-x5 multiplier, and combo-aware score calculation
- One combo progression event per tick-level resolved kill batch
- Timeout, life-loss, wave-transition, and start/restart resets
- Combo multiplier and countdown HUD from x2 onward
- Unit and controller integration tests, including multiple same-tick kills

**Out of scope:**

- Resetting combo on missed shots
- Persisted high scores, leaderboards, achievements, or score history
- New sound, animation, image assets, or dependencies
- Changes to rapid-fire duration, drop rate, or firing cooldown
- Broad controller, session, or HUD refactors

## Architecture / Approach

`GameRules` defines the bounded multiplier and combo-adjusted score formula. `GameSession` owns combo multiplier/timer state, scoring-event progression, and all reset lifecycle behavior. `GameController` ticks the timer once per playing tick and submits one deduplicated kill count per collision batch. `GamePanel` receives combo state through the existing scalar state push and renders the actionable HUD indicator.

## Phases at a Glance

| Phase | Delivers | Key Risk |
| --- | --- | --- |
| 1. Rules and session state | Deterministic scoring, timer, progression, and resets | Off-by-one timing or applying the wrong multiplier to the scoring hit |
| 2. Controller integration | One combo event per resolved tick batch and wave-reset behavior | Same-tick multi-kills or tick ordering inflating scores |
| 3. HUD and regression | Visible combo countdown and full verification | HUD ambiguity or rapid fire making combo too easy |

**Prerequisites:** The archived rapid-fire power-up change is implemented and the existing compile/test baseline remains green.

**Estimated effort:** About 2-3 implementation sessions across 3 phases.

## Open Risks and Assumptions

- The 1.5-second window and x5 cap are balance choices that require manual playtesting with and without rapid fire.
- Timer behavior is based on playing ticks, consistent with rapid fire; paused/non-playing states do not consume the window.
- A wave clear resets combo immediately even when the final kill refreshed the timer.

## Success Criteria Summary

- Quick consecutive kills visibly raise the multiplier and award the expected increased score, capped at x5.
- Waiting, losing a life, changing wave, or restarting resets the combo; missed shots do not.
- Multiple kills in one tick cannot skip multiplier levels, and rapid fire remains helpful without automatically maximizing combo.
