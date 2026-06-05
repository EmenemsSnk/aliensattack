# Distinct Alien Type - Plan Brief

> Full plan: `context/changes/distinct-alien-type/plan.md`

## What and Why

Add one special alien that changes how the player aims, dodges, and prioritizes targets without turning a normal wave into a boss encounter. The new enemy appears from wave 2 onward and is visibly distinct, tougher, faster, less predictable, and more aggressive than standard aliens.

## Starting Point

Every enemy is currently the same final `Alien` model: six identical aliens descend vertically, share one-hit durability, use one sprite, and are selected uniformly to fire. Collision handling immediately converts every hit into a kill with standard score, combo, and rapid-fire drop effects.

## Desired End State

Wave 1 remains unchanged. Every later six-alien wave contains one special alien with 2 HP, `1.5x` wave-relative vertical speed, horizontal movement with a `2%` per-tick random direction change, approximately `2x` firing frequency, and a dedicated sprite. Its first hit visibly damages it without awarding kill effects; its second hit behaves like a standard kill.

## Key Decisions

| Decision | Choice | Why |
| --- | --- | --- |
| Core identity | One hybrid special alien | Combines movement, durability, and aggression into a clearly distinct target |
| Availability | Exactly one per wave from wave 2 | Keeps wave 1 introductory and later waves predictably expose the feature |
| Durability | 2 HP | Makes armor noticeable without creating a boss |
| Vertical speed | `1.5x` standard wave speed | Adds pressure appropriate for a two-hit target |
| Lateral behavior | Horizontal movement with `2%` random direction change per tick | Creates unpredictable aiming without a fixed pattern |
| Boundary behavior | Reverse/clamp at panel edges | Prevents the special alien from escaping sideways |
| Firing frequency | Approximately `2x` each standard alien | Makes it more threatening while preserving one shared projectile type |
| Enemy missile cap | Increase shared cap from 2 to 5 | Allows the increased aggression to be visible |
| Kill rewards | Standard score, combo event, and drop chance | Avoids expanding scoring and rapid-fire balance |
| First hit | Consume missile and show damage; no kill effects | Preserves clear separation between damage and destruction |
| Visual identity | Dedicated sprite plus damaged-state treatment | Makes type and remaining durability readable |
| Architecture | Extend `Alien`; preserve shared `List<Alien>` | Fits the established controller/panel wiring without parallel models |
| Testing | Full model/controller tests plus manual playtest | Protects deterministic behavior while leaving feel and visuals to human review |

## Scope

**In scope:**

- Explicit standard/special alien identity inside the existing model
- Two-hit special durability and readable first-hit damage
- Faster vertical movement, random horizontal direction changes, and edge containment
- One special alien from wave 2 onward
- Approximately doubled special firing frequency and five-projectile shared cap
- Dedicated sprite, focused tests, full regression, and playtesting

**Out of scope:**

- Additional alien types, bosses, scaling special counts, health bars, or special projectiles
- Different scoring, combo, drop, collision damage, or invasion rules for the special alien
- New sounds, dependencies, broad architecture refactors, or pixel-snapshot tests

## Architecture / Approach

`Alien` gains explicit type, HP, movement state, and damage behavior while remaining in the existing shared `List<Alien>`. `GameController` creates later-wave composition, drives random movement with its injected `Random`, separates non-lethal hits from destroyed aliens, and uses weighted shooter selection. `GamePanel` chooses the correct sprite and damaged-state treatment directly from each alien.

## Phases at a Glance

| Phase | Delivers | Key Risk |
| --- | --- | --- |
| 1. Model and movement | Type, 2 HP, random bounded lateral movement, unit tests | Special movement accidentally changes standard behavior |
| 2. Gameplay integration | Wave composition, damage sequencing, weighted firing, five-shot cap | First hit incorrectly triggers score, combo, drop, or removal |
| 3. Rendering and regression | Dedicated sprite, damaged feedback, full verification | Combined speed, durability, and fire rate feels unfair |

**Prerequisites:** Existing compile/test baseline stays green; a dedicated transparent special-alien sprite must be created during phase 3.

**Estimated effort:** About 3 implementation sessions across 3 phases.

## Open Risks and Assumptions

- Random lateral movement plus `1.5x` descent and 2 HP may be too difficult; manual playtesting is required.
- Raising the projectile cap to five changes pressure from all shooters, not only the special alien.
- Shared injected randomness means tests must use purpose-built deterministic random stand-ins rather than incidental seeded sequences.
- Weighted firing must preserve roughly normal standard-alien firing frequency while making the special alien about twice as likely to shoot.

## Success Criteria Summary

- From wave 2 onward, the player consistently encounters exactly one clearly distinct special alien.
- The special alien requires two hits, moves unpredictably without leaving the panel, and fires more often while remaining fair.
- First-hit and kill effects are correctly separated, and all existing gameplay plus the full automated suite remain green.
