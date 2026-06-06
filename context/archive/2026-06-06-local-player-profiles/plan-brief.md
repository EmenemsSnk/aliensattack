# Local Player Profiles — Plan Brief

> Full plan: `context/changes/local-player-profiles/plan.md`

## What and Why

Add local player profiles to Aliens Attack so a player must create or select a profile before starting a session. This plan also includes persisted best scores: after Game Over, the selected profile's best score is saved only when the final score is greater than the stored best score.

## Starting Point

The game has a complete Swing arcade loop with `GameController` owning input/state transitions and `GamePanel` passively rendering pushed state. There is no persisted data today; the start menu only shows a title and `ENTER` prompt.

## Desired End State

The start screen supports profile creation with `N`, profile selection with left/right arrows, and gameplay start with `ENTER` only after a profile is selected. Profiles are stored in working-directory `profiles.tsv`, and corrupted or unavailable data falls back to an empty profile state instead of crashing. Game Over shows the selected profile and best score, updating the file only for strictly better scores.

## Key Decisions

| Decision | Choice | Why |
| --- | --- | --- |
| Empty first-run behavior | Require profile creation before start | The player wanted an intentional selected profile before gameplay. |
| Profile creation UI | Inline start-screen input mode via `N` | It stays inside the game window and fits the existing key-driven controller tests. |
| Name validation | Trimmed 1-16 chars; letters, digits, spaces, hyphen, underscore | Keeps names readable and makes the file format simple. |
| Profile selection | Left/right arrows in `START_MENU` | Natural selection behavior while preserving arrow movement during gameplay. |
| Storage location | Working-directory `profiles.tsv` | Easy to find during local development and selected by the user. |
| Storage failure behavior | Empty state plus `System.err` logging | Bad data must not crash the game; the player can create a fresh profile. |
| Best-score scope | Include best-score saving now | The user chose to fold the S-09 behavior into this plan. |
| Best-score update rule | Save only when `finalScore > bestScore` | Standard, predictable high-score semantics and resolves the PRD open question. |

## Scope

**In scope:**

- Local `PlayerProfile` model with name and best score
- `profiles.tsv` load/save using JDK file APIs
- Name validation and duplicate rejection
- Start-menu profile creation, profile selection, and profile-gated start
- One-time Game Over best-score update on strictly better final score
- Start-menu and Game Over profile rendering
- README updates and JUnit coverage

**Out of scope:**

- Authentication, passwords, online accounts, cloud sync, networking, or global leaderboard
- Profile delete/rename/import/export
- Save-state or gameplay persistence
- Separate high-score table UI
- New runtime dependencies or architecture rewrite

## Architecture / Approach

Add a small `profiles` package for model, validation, storage, and passive view state. `GameController` loads profiles, owns selected-profile/input-mode state, gates `START_MENU -> PLAYING`, and triggers best-score persistence once per Game Over transition. `GamePanel` receives profile view state through the existing update path and only renders it.

## Phases at a Glance

| Phase | Delivers | Key Risk |
| --- | --- | --- |
| 1. Profile Model and Storage | Validated profile model plus `profiles.tsv` load/save | File errors or malformed data accidentally blocking startup |
| 2. Controller and Session Integration | Profile-gated start, create/select flow, best-score update | Game Over save running repeatedly on non-playing ticks |
| 3. Start Menu, Game Over UI, and Documentation | Clear profile UX and README controls | Text crowding in the fixed-size Swing panel |
| 4. Full Regression and Manual Verification | Compile/test/manual smoke confirmation | Persistence changes regressing the existing arcade loop |

**Prerequisites:** No upstream `frame.md` or `research.md`; decisions were made during planning. Existing Java 21/Maven/JUnit setup remains unchanged.

**Estimated effort:** ~3-4 focused implementation sessions across 4 phases, plus manual desktop smoke testing.

## Open Risks and Assumptions

- Working-directory storage means profile data location depends on where the game is launched from.
- `profiles.tsv` is intentionally simple and human-readable; future profile fields may need a small format migration.
- Save failure messaging should be visible but not alarming enough to interrupt restart/gameplay.
- Folding best-score persistence into this change means the roadmap S-09 scope should be considered mostly consumed by this plan.

## Success Criteria Summary

- A fresh launch requires creating/selecting a profile before gameplay starts.
- Profiles and best scores persist through `profiles.tsv`, and malformed data does not crash startup.
- A profile's best score updates only when the completed session score is strictly higher.
- Existing movement, firing, pause/resume, scoring, waves, Game Over, restart, and safe audio behavior continue to work.
