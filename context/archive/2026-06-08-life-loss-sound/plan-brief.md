# Life loss sound - brief plan

> Full plan: `context/changes/life-loss-sound/plan.md`

## What and Why

Add a dedicated sound effect for losing a life in Aliens Attack. The goal is to make damage feedback clearer during live play without changing any gameplay rules or breaking the existing “audio may safely fail” contract.

## Starting Point

The game already has procedural sound for shooting, explosions, and background music, all implemented in `ArcadeSoundPlayer` with silent fallback when audio cannot play. Real life loss is already centralized in `GameController.loseLife()`, while shield consumption and alien invasion Game Over use neighboring but different paths.

## Desired End State

Any real, unshielded life loss plays one short, clearly distinct sound, whether it comes from an alien missile or direct alien contact. The same sound also plays on the final life, while shielded hits and alien invasion remain silent with respect to this new effect.

## Key Decisions Made

| Decision | Choice | Why |
| --- | --- | --- |
| Trigger point | Central `GameController.loseLife()` helper | It is the one place that already means “a life was truly lost,” so both current damage sources inherit the sound automatically. |
| Coverage scope | Every real life loss, including last life | This matches the product wording and keeps behavior consistent across the whole life system. |
| Non-triggers | Shielded hits and alien invasion stay silent | They do not represent actual life loss, so triggering here would blur gameplay semantics. |
| Sound style | Short, lower procedural tone | It stays inside the current zero-dependency audio model and is easier to distinguish from shoot/explosion sounds. |
| Test strategy | Controller trigger tests plus manual listening | The important contract is when the sound fires, while perceived quality still requires a real-world listen. |

## Scope

**In scope:**

- Add a new life-loss event method to `ArcadeSoundPlayer`
- Trigger it from the centralized controller life-loss path
- Extend controller audio test seam
- Add positive and negative controller coverage for sound triggers
- Manually verify that the sound is distinct and non-regressive

**Out of scope:**

- New audio files, libraries, or runtime dependencies
- New Game Over, shield, or pause sound effects
- Changes to life rules, hit feedback, waves, score, or controls
- Moving audio logic into `GameSession`
- README control changes

## Architecture / Approach

This slice is intentionally narrow. The audio layer gains one more event-style public method that reuses the existing procedural tone generator, and `GameController.loseLife()` becomes the sole place that triggers it. Tests stay at the controller level by extending the existing counting sound double rather than trying to validate waveform bytes.

## Phases at a Glance

| Phase | What it Delivers | Key Risk |
| --- | --- | --- |
| 1. Add the life-loss sound contract | New audio event and centralized controller trigger | Triggering from the wrong place could miss one damage source or fire during non-life-loss events. |
| 2. Lock trigger semantics with tests | Sound seam expansion, positive coverage, negative coverage | Easy-to-miss regressions around shield consumption, last-life Game Over, or invasion behavior. |

**Prerequisites:** The S-05/S-06 polish work is already complete; no upstream artifact blocks this slice.
**Estimated effort:** ~1 small implementation session plus manual listening.

## Open Risks and Assumptions

- The exact tone still needs human tuning by ear; tests only prove trigger semantics.
- Background music stop on Game Over may occur immediately after the last-life sound starts; this is acceptable as long as the effect remains audible enough in practice.
- Audio-unavailable environments must remain silent and stable.

## Success Criteria Summary

- Real life loss produces one distinct sound, including the hit that ends the session.
- Shielded hits and alien invasion do not trigger the new effect.
- Existing shooting, explosion, pause/resume, and Game Over audio behavior remain intact.
