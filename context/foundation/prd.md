---
project: "Aliens Attack"
version: 1
status: draft
created: 2026-06-04
context_type: brownfield
product_type: desktop
target_scale:
  users: small
  qps: low
  data_volume: small
timeline_budget:
  delivery_weeks: 6
  hard_deadline: null
  after_hours_only: true
---

## Current System Overview

Aliens Attack is an existing single-process 2D desktop arcade game built with Java 21, Maven, and Java Swing. MVP version 1.0.0 provides a complete playable loop: start menu, gameplay, waves, score, lives, Game Over, restart, audio feedback, and aliens that shoot.

The game is used locally by its author. It has no external runtime dependencies.

## Problem Statement & Motivation

The earlier MVP intentionally focused on delivering a simple, complete game. The next change expands it through three equally important feature packs: Replayability, Polish, and High Scores.

The packs will be delivered as three independent playable releases. Each release must add its intended value without changing the game's genre or breaking the existing controls and general playability.

# TODO: current workaround and its cost — see Open Questions

## User & Persona

The primary persona is the author playing Aliens Attack locally.

The author reaches for the game for a complete arcade session and should gain, across the three independent releases, less predictable gameplay, a more finished game feel, and a persistent reason to improve previous results.

## Success Criteria

### Primary

- Replayability release: the player can build a score combo, collect a temporary rapid-fire power-up, and encounter a new alien type while completing a playable arcade session.
- Polish release: the player sees explosions, a clearer HUD, and wave-start messages during a playable arcade session.
- High Scores release: the player can select or create a local profile, and the profile's best score is saved and displayed after Game Over.

### Secondary

- The player can pause and resume gameplay.
- Losing a life produces a distinct sound.

### Guardrails

- Existing controls continue to work.
- Scoring, waves, and difficulty scaling continue to work.
- Lives, Game Over, and restart continue to work.
- Gameplay continues safely when audio is unavailable.
- Each release remains independently playable.

## User Stories

### US-01: Complete enhanced game session

- **Given** the player has selected or created a local profile
- **When** the player completes a full session using the new Replayability mechanics and Polish elements
- **Then** the player can finish the session without regressions, and the profile's best score is saved and displayed after Game Over

#### Acceptance Criteria

- The existing controls and arcade loop remain usable throughout the session.
- Replayability and Polish features are observable during the session.
- The selected profile's best score is saved and displayed after Game Over.

## Scope of Change

### Local Profiles and High Scores

- [new] FR-001: The player can select or create an unprotected local profile from the start screen. Priority: must-have.
  > Socrates: Considered counterargument: profile names or data may be invalid and require additional error handling. Resolution: retained; invalid profile input or data must not prevent the game from starting.
- [new] FR-002: The player can have the selected profile's best score saved and displayed. Priority: must-have.
  > Socrates: No counterargument selected; remains as written.

### Replayability

- [new] FR-003: The player can collect a temporary rapid-fire power-up during gameplay. Priority: must-have.
  > Socrates: No counterargument selected; remains as written.
- [new] FR-004: The player can build a score combo during gameplay. Priority: must-have.
  > Socrates: Considered counterargument: rapid-fire may reward the power-up more than player skill. Resolution: retained; combo behavior must preserve a meaningful skill component.
- [new] FR-005: The player can encounter a new alien type during gameplay. Priority: must-have.
  > Socrates: No counterargument selected; remains as written.

### Polish

- [new] FR-006: The player can see an explosion after destroying an alien. Priority: must-have.
  > Socrates: No counterargument selected; remains as written.
- [modified] FR-007: The player can see a clearer HUD and a message when a wave starts. Priority: must-have.
  > Socrates: No counterargument selected; remains as written.
- [new] FR-008: The player can pause and resume gameplay. Priority: nice-to-have.
  > Socrates: Considered counterargument: pause may be unnecessary for short sessions. Resolution: lowered to nice-to-have.
- [new] FR-009: The player can hear a distinct sound when losing a life. Priority: nice-to-have.
  > Socrates: Considered counterargument: an additional sound may add little beyond existing feedback. Resolution: lowered to nice-to-have.

### Preserved Gameplay

- [preserved] FR-010: The player can continue using the existing controls and complete the existing arcade loop with scoring, waves, difficulty scaling, lives, Game Over, restart, and safe behavior when audio is unavailable, except where a new FR deliberately changes a named rule. Priority: must-have.
  > Socrates: Considered counterargument: a broad guardrail could prevent deliberate improvements to existing rules. Resolution: retained with an explicit exception for changes required by new FRs.

## Constraints & Compatibility

- Existing controls must continue to work.
- The game must remain playable after each independent release.
- Existing scoring, waves, difficulty scaling, lives, Game Over, restart, and safe audio behavior must not regress.
- The game remains a local desktop application.
- Valid local profile and best-score data remains available after restarting the game.
- Missing or corrupted profile data does not prevent the game from starting; the player can continue with an empty profile state.
- A failure to save profile or score data does not interrupt an active game session.
- The game remains playable when audio is unavailable.

# TODO: data migration and rollback needs — see Open Questions

# TODO: existing integrations that must remain compatible — see Open Questions

## Business Logic Changes

Destroyed aliens have a small random chance to drop a rapid-fire power-up; ship contact activates it, and the effect expires after a fixed number of ticks.

Quick consecutive hits increase the score multiplier. A delay between hits or losing a life resets the combo.

The exact rule for the new alien type and the condition for replacing a profile's stored best score remain open.

## Access Control Changes

The current game has no authentication or roles. This change adds unprotected local player profiles: on the start screen, a player can select an existing profile or create a new one.

Any local player can select any profile. Profiles have no password or PIN and separate only each player's best score.

## Non-Goals

- No multiplayer or networking; the releases remain focused on local single-player play.
- No online accounts, cloud synchronization, or global leaderboard; profiles and best scores remain local.
- No additional power-up families, alien types, or bosses beyond the explicitly selected scope; this prevents mechanic expansion from delaying the three releases.
- No rewrite of the existing game architecture and no change away from the desktop product surface; the work extends the shipped game.

## Open Questions

1. What behavior differentiates the new alien type from the standard alien? Owner: user. By: before planning the Replayability release.
2. Under exactly what condition does a completed session replace the selected profile's stored best score? Owner: user. By: before planning the High Scores release.
3. What is the current workaround for limited replayability, polish, and score persistence, and what does it cost the player? Owner: user. By: before PRD review.
4. Does introducing persistent local profiles require migration or rollback behavior for any existing data? Owner: user. By: before planning the High Scores release.
5. Are there existing integrations beyond local audio behavior that must remain compatible? Owner: user. By: before implementation planning.
