# Post-MVP Arcade Feel — Brief Plan

> Full plan: `context/changes/post-mvp-arcade-feel/plan.md`

## What And Why

Add the three post-MVP arcade-feel improvements from the roadmap: start menu, retro sound effects, and alien fire. The MVP loop is already playable; this change makes it feel more like a complete arcade session without changing the project architecture.

## Starting Point

The game starts directly in `PLAYING`, has a working HUD/Game Over/restart loop, and uses shared mutable lists for player missiles and aliens. There is no start state, no audio path, and no alien projectile model.

## Desired End State

The app opens on a start overlay and begins play when the player presses Enter. Holding Space fires repeatedly on a cooldown. Shooting and alien destruction produce softer retro sounds, with a quiet generated background loop during play. Aliens fire capped downward projectiles that can hit the ship, reduce lives, trigger feedback, and participate cleanly in Game Over/restart.

## Key Decisions

| Decision | Choice | Why |
| --- | --- | --- |
| Runtime dependencies | None | The repo rules and roadmap default keep the shipped runtime standard-library only. |
| Audio | Generate short Java Sound tones | Avoids binary assets and external libraries while still adding feedback. |
| Alien fire model | Separate `AlienMissile` list | Keeps player `Missile` semantics unchanged and matches existing shared-list wiring. |
| Session keys | Enter starts/restarts, Space fires | Keeps session transitions distinct from the high-frequency shooting action. |
| Playfield tuning | Smaller board, bigger sprites, fewer aliens | Responds to manual feedback that the initial pass was too wide, small, and frantic. |
| Scope | One implementation phase | The work is small and touches the same controller/panel surfaces. |
| Start behavior | `START_MENU` before `PLAYING` | Adds the postponed FR-005 without introducing pause/settings/menu complexity. |

## Scope

**In scope:**

- Start overlay and Space-to-start flow.
- Shoot, explosion, and generated background sound feedback.
- Alien projectile model, rendering, movement, cleanup, firing cap, and ship collision.
- Manual-feedback tuning for scale, pressure, controls, and audio feel.
- Unit tests for controller/projectile rules.

**Out of scope:**

- Pause menu, settings, high scores, difficulty picker, or persistence.
- External audio/game libraries or new binary assets.
- Alien targeting AI, formations, or multiple enemy weapon types.
- View/Controller refactor.

## Architecture / Approach

Keep `GameController` as the gameplay owner and `GamePanel` as passive rendering. Extend `GameState`, wire a new shared `List<AlienMissile>` through `Main`, add a standard-library `ArcadeSoundPlayer`, and reuse existing life-loss/Game Over rules for projectile hits.

## Phases In Brief

| Phase | Delivers | Key Risk |
| --- | --- | --- |
| 1. Start Menu, Audio Feedback, and Alien Fire | Full S-05 slice | Audio APIs may be unavailable locally/CI, so playback must no-op safely. |
| 2. Manual Feedback Tuning | Enter start/restart, larger sprites, calmer pressure, hold-to-fire, background loop | Needs subjective manual play testing for feel. |

**Prerequisites:** Completed S-04 MVP gameplay loop.
**Estimated effort:** One focused implementation session plus manual play verification and a short tuning pass.

## Open Risks And Assumptions

- Audio output may be unavailable in some environments; the game must still run silently.
- Alien firing parameters require light calibration, but a low chance plus active-projectile cap is sufficient for this slice.
- Manual verification is still needed for audio, visual projectile readability, and overall feel.

## Success Criteria Summary

- Player sees a start overlay, starts with Space, and still uses Space to shoot/restart in the correct states.
- Sounds provide feedback without adding dependencies or breaking headless/test environments.
- Alien fire is visible, capped, dangerous, and compatible with existing lives/Game Over/restart behavior.
