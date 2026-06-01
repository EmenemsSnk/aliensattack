---
project: "Aliens Attack"
version: 1
status: draft
created: 2026-05-31
context_type: brownfield
product_type: desktop
target_scale:
  users: small
  qps: low
  data_volume: small
timeline_budget:
  delivery_weeks: 1
  hard_deadline: null
  after_hours_only: true
---

# Aliens Attack Refactor Pack PRD

## Current System Overview

Aliens Attack is an existing Java 21 + Maven + Swing desktop arcade game.

The current architecture is a single-process Swing application. Version 1.0.0 is a playable MVP with a complete loop: start menu, gameplay, waves, score, lives, Game Over, restart, basic audio feedback, and shooting aliens. `GameController` is the central node for input, ticks, collision logic, waves, scoring, lives, missiles, audio, and game state. `GamePanel` renders the game, and the controller pushes scalar HUD/game-state values into it each tick. Runtime dependencies remain limited to the JDK standard library.

Current users are players of the local desktop game. For this change, the primary affected persona is the developer/maintainer extending the game after the MVP.

## Problem Statement & Motivation

The next change is an architecture improvement without visible gameplay changes. The immediate pain is that `GameController` was pragmatic for shipping the MVP, but its current scope makes future mechanics riskier and more expensive to add.

The motivation is to prepare the codebase for later Replayability Pack work by extracting a cleaner gameplay foundation first. This PRD should not add power-ups, new aliens, polish, high scores, or distribution work.

The current workaround is to keep adding behavior directly to `GameController`, which increases regression risk and makes focused tests harder as the next gameplay iteration approaches.

## User & Persona

Primary persona: developer/maintainer continuing development of Aliens Attack after the MVP.

The moment they feel the pain is when planning the next gameplay iteration, especially Replayability Pack mechanics, and seeing that adding more behavior directly to `GameController` would increase regression risk and make focused tests harder.

Players remain affected indirectly because their existing game experience must not change during this refactor.

## Success Criteria

### Primary

- Developer/maintainer can extract `GameSession` and `GameRules` from `GameController` while preserving visible gameplay behavior.
- The game still supports the existing player-visible loop: start, movement, shooting, wave progression, scoring, lives, Game Over, and restart.

### Secondary

- The extracted foundation makes the later Replayability Pack easier to add.

### Guardrails

- Zero visible gameplay behavior changes.
- Keep the blast radius focused on `GameController`, controller tests, and session/reset/scoring/wave flow.
- Existing tests continue to pass.
- The game remains responsive to player input at the same perceived level as before the refactor.
- The runtime remains JDK-only with no new external runtime dependencies.

## User Stories

### US-01: Developer extracts gameplay foundation without changing player-visible behavior

- **Given** the existing Aliens Attack MVP with `GameController` owning session state and gameplay rules
- **When** the developer/maintainer performs the refactor
- **Then** session state and game-rule calculations are available through focused classes while the player-visible game loop remains unchanged

#### Acceptance Criteria

- Score, wave, lives, game state, and reset behavior are represented outside the monolithic controller.
- Scoring and wave-speed calculations are represented as focused game-rule behavior.
- The game still supports start, movement, shooting, wave progression, scoring, lives, Game Over, and restart.
- Existing tests continue to pass.

## Scope of Change

- [new] FR-001: Developer/maintainer can work with an extracted game session model covering score, wave, lives, game state, and reset. Priority: must-have.
  > Socratic: Considered counterargument: extracting session state may split the simple reset and game-state flow across too many places. Resolution: keep the FR, but the extraction must preserve a clear single reset/session boundary rather than scatter state ownership.
- [new] FR-002: Developer/maintainer can work with extracted game rules for scoring and wave scaling. Priority: must-have.
  > Socratic: Considered counterargument: extracting scoring and wave-scaling rules could make later balancing harder if rules are locked behind a rigid abstraction too early. Resolution: keep the FR, but the extracted rules should stay small and focused on current behavior rather than over-designing for future mechanics.
- [preserved] FR-003: Player can play Aliens Attack with the same visible behavior as before the refactor. Priority: must-have.
  > Socratic: No counterargument; remains as written.
- [preserved] FR-004: Developer/maintainer can run the existing test suite and confirm no regression. Priority: must-have.
  > Socratic: Considered counterargument: the existing tests may not cover every player-visible behavior. Resolution: keep the FR, but do not treat green tests as the only confidence signal; a short manual smoke check remains necessary.

## Constraints & Compatibility

- Preserve the existing presentation layer.
- Preserve the current launch path and local desktop game model.
- Preserve the zero external runtime dependency posture.
- Preserve existing player-visible behavior: start, movement, shooting, wave progression, scoring, lives, Game Over, and restart.
- Preserve current test confidence and supplement it with a short manual smoke check because existing tests may not cover every visible behavior.
- No migration of player data is required for this refactor.
- No changes to external integrations are required; the game has no external runtime service integration.

## Business Logic Changes

The system preserves the existing arcade-session rules: score depends on wave, wave difficulty scales alien speed, and losing all lives or allowing invasion ends the session without changing visible gameplay.

This change reorganizes where the existing rules live; it does not add a new player-facing domain rule. The relevant inputs are the same player-visible session events as before: alien destruction, wave progression, life loss, reset, and Game Over triggers. The output remains the same score, wave, lives, and game-state behavior the player already sees.

## Access Control Changes

No access control changes are planned.

Current model preserved: Aliens Attack is a local single-player desktop game with no login, no accounts, and no role separation.

## Non-Goals

- No power-ups or new alien types in this PRD; those belong to the later Replayability Pack.
- No presentation rendering rewrite; this PRD focuses on gameplay foundation extraction, not presentation architecture.
- No high scores, data persistence, playable artifact distribution, or release packaging; those remain deferred to a later version.
- No gameplay balance changes; scoring, wave progression, lives, movement, shooting, and Game Over behavior should remain visibly unchanged.

## Open Questions

- None.
