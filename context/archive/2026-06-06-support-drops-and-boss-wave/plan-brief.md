# Support drops and boss wave — brief plan

> Full plan: `context/changes/support-drops-and-boss-wave/plan.md`

## What and why

Add a larger replayability slice: rare support drops during regular waves and a boss-only milestone wave every fifth wave. The goal is to give the player more survival tools and a memorable periodic encounter without replacing the current local arcade loop.

## Starting point

The game already has rapid-fire drops, combo scoring, special aliens, explosions, a clearer HUD, pause/resume, and local profiles. The key existing patterns are shared renderable lists in `Main`, controller-owned entity orchestration, session-owned scalar effect state, and passive rendering in `GamePanel`.

## Desired end state

Regular alien kills can rarely drop rapid-fire, extra life, shield, or speed boost. Extra life caps at 5 lives, shield blocks one incoming damage event, and speed boost lasts 3 seconds. Every fifth wave contains one 20-hit horizontally patrolling boss that fires bursts, shows a health bar, awards bonus points, and drops a guaranteed life-or-shield reward when defeated.

## Key decisions

| Decision | Choice | Why |
| --- | --- | --- |
| Boss wave composition | Boss only | Makes every fifth wave a clear milestone and avoids mixed-wave edge cases. |
| Boss durability | 20 hits | Makes the boss feel like a real encounter rather than a tougher special alien. |
| Boss threat | Burst shots | The boss threatens through missiles, not contact or invasion. |
| Boss movement | Horizontal top-lane patrol | Preserves player space and matches the chosen missile-only threat model. |
| Boss reward | Life-or-shield drop plus bonus points | Gives a milestone reward without adding all support types to boss payout. |
| Shield | One damage event | Easy to understand and test, and it limits runaway protection. |
| Extra life | Cap at 5 lives | Prevents long-session survivability from scaling without bound. |
| Speed boost | 3 seconds | Short, readable aid for dodging bursts. |
| Ordinary drops | One rare roll, then weighted type selection | Controls total drop volume while supporting multiple drop types. |
| HUD | Compact active-effect rows | Extends the current HUD pattern without a larger layout rewrite. |

## Scope

**In scope:**
- Generalize rapid-fire drops into a `PowerUp` / `PowerUpType` system.
- Add extra-life, shield, and 3-second speed-boost effects.
- Add a boss identity, boss-only wave cadence, 20 HP, horizontal patrol, burst firing, boss health bar, bonus score, and guaranteed life-or-shield reward.
- Update controller, session, rules, panel, tests, and `CLAUDE.md` guidance.

**Out of scope:**
- Multiple boss types, boss phases, angled projectiles, new projectile classes, inventory, permanent upgrades, new dependencies, persistence changes, or architecture rewrite.

## Architecture / approach

Use the existing ownership split. Renderable support drops and boss aliens stay in shared lists; controller owns spawning, movement, collision, burst firing, and reward sequencing; `GameSession` owns lives, shield, speed boost, rapid-fire, and timers; `GameRules` owns tuning; `GamePanel` receives scalar state and draws the HUD/boss health bar.

## Phases at a glance

| Phase | Delivers | Key risk |
| --- | --- | --- |
| 1. Shared support-drop model and session effects | Generalized power-up model, life cap, shield, speed boost | Accidentally duplicating rapid-fire behavior or storing timed state in the wrong class |
| 2. Support-drop controller integration | Rare weighted drops, collection, shield interception, speed movement | Drop volume and shield sequencing around life loss |
| 3. Boss model and boss-only wave | 20-hit boss every fifth wave, bonus score, guaranteed reward | Boss defeat ordering before wave advancement |
| 4. Boss burst attack and shield-safe damage | Burst firing under cap and shield-safe damage behavior | Unfair projectile density |
| 5. Rendering, HUD, guidance, and full regression | Drop visuals, active-effect HUD, boss health bar, docs, full verification | Visual clutter in the small Swing board |

**Prerequisites:** Existing S-01 rapid-fire and S-05 HUD/wave-message work are already archived and implemented.
**Estimated effort:** Large change, about 4-5 implementation sessions across 5 phases plus manual gameplay tuning.

## Open risks and assumptions

- Boss score bonus still needs exact numeric tuning inside `GameRules`; the plan requires a tested boss-specific bonus, not a hard-coded collision-side value.
- Boss burst size/cap should start conservative and be adjusted by manual play.
- Replacing `RapidFirePowerUp` with generalized `PowerUp` is a migration risk because it touches constructors, tests, panel rendering, and controller helpers.

## Success criteria summary

- Regular waves still feel like the current game, but support drops occasionally add survival/mobility options.
- Wave 5 is a readable 20-hit boss fight with burst pressure, health bar, bonus score, and guaranteed life-or-shield reward.
- `./mvnw clean compile`, `./mvnw test`, and manual Swing gameplay checks pass without regressions to profiles, score, combo, rapid-fire, pause, Game Over, restart, or audio-safe behavior.
