# Visible Alien Explosion — Brief Plan

> Full plan: `context/changes/visible-alien-explosion/plan.md`

## What And Why

Destroyed aliens currently vanish instantly while only the explosion sound communicates the kill. This change adds a short visible explosion at each alien's last position so combat feedback is readable during normal play and rapid-fire moments.

## Starting Point

`GameController.checkCollisionsWithMissile()` already batches destroyed aliens before removal and sound playback, which is the right moment to create visual effects. `Main` and `GamePanel` already use shared mutable lists for renderable gameplay objects, while tests avoid brittle Swing pixel snapshots.

## Desired End State

Every alien destroyed by a player missile creates one visible explosion for about 12 ticks. Multiple destroyed aliens can show multiple simultaneous explosions, non-lethal special-alien hits do not create an effect, and restart clears any active explosions.

## Key Decisions

| Decision | Choice | Why |
| --- | --- | --- |
| Visual form | Dedicated `explosion.png` asset | Gives the feature a classic arcade visual and matches the user's choice. |
| Duration | About 12 ticks | Visible at 60 FPS without lingering during rapid fire. |
| Trigger | Only missile kills | Matches the current explosion sound and avoids mixing alien death feedback with ship damage feedback. |
| Multiplicity | One active explosion per destroyed alien | Preserves feedback for same-tick and rapid-fire kills. |
| State ownership | Shared `List<AlienExplosion>` | Follows the existing renderable-object architecture used by aliens, missiles, and power-ups. |

## Scope

**In scope:**

- New `AlienExplosion` model with fixed position and 12-tick lifetime.
- Shared explosion list wired through `Main`, `GameController`, and `GamePanel`.
- `explosion.png` loaded from `src/main/resources/images/`.
- Controller lifecycle for create/tick/expire/reset.
- Focused model, controller, and panel/resource tests.

**Out of scope:**

- Procedural-only explosion rendering.
- Explosions for ship collision, invasion, offscreen cleanup, or non-lethal special hits.
- Particle systems, animation frameworks, audio changes, scoring changes, or broad rendering refactors.
- Pixel-snapshot Swing tests.

## Architecture / Approach

Create `List<AlienExplosion>` beside the existing object lists. The controller mutates it when kills happen and on each playing tick; the panel passively renders it using the new sprite; `Main` owns initial list creation and passes the same reference to both sides.

## Phases At A Glance

| Phase | Delivers | Key Risk |
| --- | --- | --- |
| 1. Explosion Model, Wiring, Rendering, and Tests | Complete visible explosion feature with regression coverage | Constructor/wiring churn can break existing tests if compatibility overloads are not kept. |

**Prerequisites:** A readable `src/main/resources/images/explosion.png` asset must be added during implementation.
**Estimated effort:** One focused implementation session plus manual gameplay verification.

## Open Risks And Assumptions

- The explosion asset must remain legible at the existing 42x42 render size against the space background.
- Existing constructor overloads should be preserved so current tests and simple callers do not need unrelated rewrites.
- Manual visual verification is still required because the project intentionally avoids pixel-level Swing tests.

## Success Criteria Summary

- Missile kills show short visible explosions at the alien's last position.
- Non-lethal special hits do not show explosions; lethal second hits do.
- Automated tests pass and restart/gameplay behavior does not regress.
