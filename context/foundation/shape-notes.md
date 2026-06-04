---
project: "Aliens Attack"
context_type: brownfield
created: 2026-06-04
updated: 2026-06-04
product_type: desktop
target_scale:
  users: small
  qps: low
  data_volume: small
timeline_budget:
  delivery_weeks: 6
  hard_deadline: null
  after_hours_only: true
checkpoint:
  current_phase: 8
  phases_completed: [1, 2, 3, 4, 5, 6, 7]
  gray_areas_resolved:
    - topic: context type
      decision: brownfield
    - topic: change category
      decision: three significant feature packs delivered as independent playable releases
    - topic: primary persona
      decision: author playing locally
    - topic: priority between packs
      decision: Replayability, Polish, and High Scores are equally important
    - topic: local profile access
      decision: player selects or creates an unprotected local profile on the start screen
    - topic: profile data separation
      decision: only the best score is separate for each profile
    - topic: delivery scope
      decision: full scope across three independent playable releases
    - topic: delivery estimate
      decision: 6 weeks with no hard deadline; sustained-effort cost accepted
    - topic: secondary polish scope
      decision: pause and a distinct life-loss sound
    - topic: regression guardrails
      decision: preserve controls, scoring, waves, difficulty scaling, lives, Game Over, restart, and safe audio behavior
    - topic: functional requirement priority
      decision: all captured capabilities are must-have except pause and life-loss sound, which are nice-to-have
    - topic: rapid-fire rule
      decision: small random drop chance after destroying an alien; collected by ship contact; expires after a fixed number of ticks
    - topic: combo rule
      decision: quick consecutive hits increase the multiplier; a delay or life loss resets it
    - topic: missing or corrupted profile data
      decision: game starts normally with an empty profile state; read or write failures do not interrupt gameplay
    - topic: preserved technical contracts
      decision: local desktop product, no new external runtime dependencies, safe without audio, each release compiles, passes tests, and remains playable
    - topic: product framing
      decision: remains a local desktop app for the author or a handful of local players, developed after hours
    - topic: explicit non-goals
      decision: no multiplayer/networking, online accounts/cloud/global leaderboard, additional mechanic families beyond scope, or architecture/platform rewrite
  frs_drafted: 10
  quality_check_status: accepted
---

## Source Notes

- Initial idea source: `ideas_2_0.md`

## Current System

Aliens Attack is an existing single-process 2D desktop arcade game built with Java 21, Maven, and Java Swing. MVP version 1.0.0 provides a complete playable loop: start menu, gameplay, waves, score, lives, Game Over, restart, audio feedback, and aliens that shoot.

The game is used locally by its author. It has no external runtime dependencies.

## Vision & Problem Statement

The earlier MVP intentionally focused on delivering a simple, complete game. The next change expands it through three equally important feature packs: Replayability, Polish, and High Scores.

The packs will be delivered as three independent playable releases. Each release must add its intended value without changing the game's genre or breaking the existing controls and general playability.

## User & Persona

The primary persona is the author playing Aliens Attack locally.

The author reaches for the game for a complete arcade session and should gain, across the three independent releases, less predictable gameplay, a more finished game feel, and a persistent reason to improve previous results.

## Access Control

The current game has no authentication or roles. This change adds unprotected local player profiles: on the start screen, a player can select an existing profile or create a new one.

Any local player can select any profile. Profiles have no password or PIN and separate only each player's best score.

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

## Functional Requirements

### Local Profiles and High Scores

- FR-001: The player can select or create an unprotected local profile from the start screen. Priority: must-have. Change: new
  > Socrates: Considered counterargument: profile names or data may be invalid and require additional error handling. Resolution: retained; invalid profile input or data must not prevent the game from starting.
- FR-002: The player can have the selected profile's best score saved and displayed. Priority: must-have. Change: new
  > Socrates: No counterargument selected; remains as written.

### Replayability

- FR-003: The player can collect a temporary rapid-fire power-up during gameplay. Priority: must-have. Change: new
  > Socrates: No counterargument selected; remains as written.
- FR-004: The player can build a score combo during gameplay. Priority: must-have. Change: new
  > Socrates: Considered counterargument: rapid-fire may reward the power-up more than player skill. Resolution: retained; combo behavior must preserve a meaningful skill component.
- FR-005: The player can encounter a new alien type during gameplay. Priority: must-have. Change: new
  > Socrates: No counterargument selected; remains as written.

### Polish

- FR-006: The player can see an explosion after destroying an alien. Priority: must-have. Change: new
  > Socrates: No counterargument selected; remains as written.
- FR-007: The player can see a clearer HUD and a message when a wave starts. Priority: must-have. Change: modified
  > Socrates: No counterargument selected; remains as written.
- FR-008: The player can pause and resume gameplay. Priority: nice-to-have. Change: new
  > Socrates: Considered counterargument: pause may be unnecessary for short sessions. Resolution: lowered to nice-to-have.
- FR-009: The player can hear a distinct sound when losing a life. Priority: nice-to-have. Change: new
  > Socrates: Considered counterargument: an additional sound may add little beyond existing feedback. Resolution: lowered to nice-to-have.

### Preserved Gameplay

- FR-010: The player can continue using the existing controls and complete the existing arcade loop with scoring, waves, difficulty scaling, lives, Game Over, restart, and safe behavior when audio is unavailable, except where a new FR deliberately changes a named rule. Priority: must-have. Change: preserved
  > Socrates: Considered counterargument: a broad guardrail could prevent deliberate improvements to existing rules. Resolution: retained with an explicit exception for changes required by new FRs.

## User Stories

### US-01: Complete enhanced game session

- **Given** the player has selected or created a local profile
- **When** the player completes a full session using the new Replayability mechanics and Polish elements
- **Then** the player can finish the session without regressions, and the profile's best score is saved and displayed after Game Over

#### Acceptance Criteria

- The existing controls and arcade loop remain usable throughout the session.
- Replayability and Polish features are observable during the session.
- The selected profile's best score is saved and displayed after Game Over.

## Business Logic

Destroyed aliens have a small random chance to drop a rapid-fire power-up; ship contact activates it, and the effect expires after a fixed number of ticks.

Quick consecutive hits increase the score multiplier. A delay between hits or losing a life resets the combo.

The exact rule for the new alien type and the condition for replacing a profile's stored best score remain open.

## Non-Functional Requirements

- Valid local profile and best-score data remains available after restarting the game.
- Missing or corrupted profile data does not prevent the game from starting; the player can continue with an empty profile state.
- A failure to save profile or score data does not interrupt an active game session.
- The game remains playable when audio is unavailable.
- Each independent release compiles, passes its automated tests, and remains playable.

## Constraints & Preserved Behavior

- Existing controls must continue to work.
- The game must remain playable after each independent release.
- Existing scoring, waves, difficulty scaling, lives, Game Over, restart, and safe audio behavior must not regress.
- The game remains a local desktop application.
- No new external runtime dependencies are introduced.
- Missing audio hardware or audio failures must not interrupt gameplay.
- Missing or corrupted profile data must not prevent the game from starting.

## Non-Goals

- No multiplayer or networking; the releases remain focused on local single-player play.
- No online accounts, cloud synchronization, or global leaderboard; profiles and best scores remain local.
- No additional power-up families, alien types, or bosses beyond the explicitly selected scope; this prevents mechanic expansion from delaying the three releases.
- No rewrite of the existing game architecture and no change away from the desktop product surface; the work extends the shipped game.

## Open Questions

1. What behavior differentiates the new alien type from the standard alien? Owner: user. By: before planning the Replayability release.
2. Under exactly what condition does a completed session replace the selected profile's stored best score? Owner: user. By: before planning the High Scores release.

## Timeline acknowledgment

Acknowledged on 2026-06-04: a 6-week change requires sustained effort; the user accepted.

## Quality cross-check

- Access control: present.
- Business logic: present.
- Project artifact and checkpoint: present.
- Timeline cost acknowledgment: present.
- Non-Goals: present.
- Preserved behavior: present.
