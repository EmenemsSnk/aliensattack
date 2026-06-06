# Pause and resume - brief plan

> Full plan: `context/changes/pause-and-resume/plan.md`

## What and Why

Add an in-game pause/resume feature for Aliens Attack. The player should be able to freeze an active session with `P`, step away without gameplay changing, and resume the same board state later.

## Starting Point

The game already has a state-machine-driven tick gate: gameplay mutates only while `GameState.PLAYING`. Existing overlays and scalar UI state are rendered passively by `GamePanel` from values pushed by `GameController`.

## Desired End State

Pressing `P` during play enters a new `PAUSED` state, stops background music, clears held input, and shows a centered paused overlay over the frozen board. Pressing `P` again resumes `PLAYING` and starts background music again. While paused, `ENTER`, arrows, and `SPACE` do nothing, and gameplay timers and objects do not advance.

## Key Decisions Made

| Decision | Choice | Why |
| --- | --- | --- |
| Pause key | `P` | It is a common game convention and avoids conflicts with `ENTER` start/restart and `SPACE` fire. |
| Pause feedback | Centered overlay | It is clear and matches the existing menu/game-over overlay pattern. |
| Music behavior | Stop during pause, start on resume | It makes pause feel like a true suspension while preserving the current safe audio API. |
| Held input | Clear on pause | It prevents surprise movement or firing immediately after resume. |
| Timer behavior | Freeze exactly | It matches the roadmap requirement that object/session state not change while paused. |
| Paused controls | `P` resumes only | It keeps paused state simple and prevents accidental restart or firing. |
| Documentation | README only | This updates user-facing controls without expanding persistent agent guidance for a small slice. |

## Scope

**In scope:**

- Add `PAUSED` to the game state machine.
- Add session pause/resume transitions.
- Toggle pause/resume with `P`.
- Clear held movement/fire input and fire cooldown intent on pause.
- Stop background music while paused and start it on resume.
- Render a centered paused overlay.
- Update README controls.
- Add focused session, controller, and panel tests.

**Out of scope:**

- Pause menu, settings, quit, restart shortcut, save-state, or profiles.
- Gameplay balance or scoring/wave/life rule changes.
- New assets, libraries, or audio files.
- Exact background-music clip-position resume.
- Updates to `CLAUDE.md`.

## Architecture / Approach

Use the existing state-machine pattern instead of stopping the Swing timer. `GameSession` owns the scalar `PLAYING <-> PAUSED` transition, `GameController` owns key handling, held-input clearing, and music orchestration, and `GamePanel` passively renders paused state from the existing `updateGameState(...)` push.

## Phases at a Glance

| Phase | What it Delivers | Key Risk |
| --- | --- | --- |
| 1. State machine and input contract | `PAUSED` state, `P` toggle, input clearing, music stop/start | Accidentally allowing movement, fire, restart, or timer drift while paused. |
| 2. Rendering and documentation | Centered paused overlay and README control update | Overlay could be unclear or confused with Game Over/start menu. |
| 3. Regression coverage | Full compile/test gate and manual gameplay smoke test | Pause could subtly regress existing start, restart, Game Over, or temporary-effect behavior. |

**Prerequisites:** S-05 HUD/wave-message work is complete and archived.

**Estimated effort:** ~1-2 implementation sessions across 3 small phases.

## Open Risks and Assumptions

- Resuming background music may restart the generated loop rather than resume the exact clip position; this is accepted for this slice.
- Manual verification is needed for overlay readability and perceived audio behavior in the real Swing app.
- Tests should avoid real windows and real audio, following the existing controller test seam.

## Success Criteria Summary

- The player can pause with `P`, see a clear paused overlay, and resume with `P`.
- No gameplay state changes while paused: entities, cooldowns, rapid-fire, combo, wave message, and explosions stay frozen.
- Existing start menu, Game Over, restart, movement, firing, HUD, and audio-failure safety continue to work.
