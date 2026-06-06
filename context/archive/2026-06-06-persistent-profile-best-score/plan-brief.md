# Persistent Profile Best Score and Top 5 Ranking — Brief

> Full plan: `context/changes/persistent-profile-best-score/plan.md`

## What and Why

Finish the profile best-score slice by adding the missing comparative view: a local Top 5 profile ranking shown on Game Over. The selected profile's best score already persists; this change lets the player see how that result ranks against other local profiles.

## Starting Point

The branch already contains local profiles, `profiles.tsv`, selected-profile best-score display, and one-time best-score updates on Game Over. The roadmap still says S-09 is blocked by the update rule, but the rule is now decided: update only when `finalScore > bestScore`.

## Desired End State

Game Over shows final score, selected profile result, and a compact Top 5 ranking. The ranking is derived from existing profile best scores, updates immediately after a new best score, persists through `profiles.tsv`, and does not add a new screen or storage format.

## Key Decisions

| Decision | Choice | Why |
| --- | --- | --- |
| Ranking model | Top 5 profiles by stored `bestScore` | Reuses the current profile data model and avoids session-history storage. |
| Update timing | Refresh ranking after in-memory best-score update | The player immediately sees their new rank on Game Over. |
| Tie order | Score descending, then profile name ascending | Deterministic and easy to test. |
| UI placement | Compact list on Game Over | Makes ranking visible without introducing another game state. |
| Roadmap handling | Reconcile stale S-09 blocker | Prevents future agents from re-planning an already-decided rule. |

## Scope

**In scope:**

- Add a Top 5 ranking view to `ProfileMenuState`.
- Derive ranking in `GameController` from loaded profiles.
- Render compact ranking rows in `GamePanel.drawGameOver(...)`.
- Add controller/panel tests for sorting, limit, refresh, and helper formatting.
- Update README and roadmap wording.

**Out of scope:**

- Session-history leaderboard.
- New file format or migration.
- Online/global leaderboard, accounts, passwords, or cloud sync.
- Separate leaderboard screen.
- Changes to scoring, waves, lives, or profile creation.

## Architecture / Approach

`GameController` owns the profile list and derives a passive Top 5 view whenever it builds `ProfileMenuState`. `GamePanel` receives that state through the existing `updateGameState(...)` push and only renders it. `ProfileStore` remains unchanged because the ranking is computed from existing `profiles.tsv` rows.

## Phases at a Glance

| Phase | What It Delivers | Key Risk |
| --- | --- | --- |
| 1. Ranking state and controller derivation | Top 5 rows available in profile UI state | Wrong ordering or stale ranking after Game Over update. |
| 2. Game Over rendering and docs | Visible compact ranking plus README note | Fixed-size panel may become crowded. |
| 3. Roadmap and verification | Stale blocker removed, full compile/test/manual QA | Documentation can overclaim completion before manual smoke test. |

**Prerequisites:** Existing local profile implementation remains in place.
**Estimated effort:** ~1 focused implementation session plus manual desktop smoke test.

## Open Risks and Assumptions

- Assumption: Top 5 means profiles by stored best score, not individual sessions.
- Assumption: Showing in-memory ranking after a save failure is acceptable as long as the save warning remains visible.
- Risk: Game Over text may need smaller font/spacing to avoid overlap.

## Success Criteria Summary

- Game Over shows a stable Top 5 local profile ranking.
- New best scores update the selected profile and ranking immediately, then persist across relaunch.
- Existing profile persistence, restart, pause, movement, firing, and save-failure behavior do not regress.
