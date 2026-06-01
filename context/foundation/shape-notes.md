---
project: "Aliens Attack"
context_type: brownfield
product_type: desktop
target_scale:
  users: small
  qps: low
  data_volume: small
created: 2026-05-31
updated: 2026-05-31
checkpoint:
  current_phase: 8
  phases_completed: [1, 2, 3, 4, 5, 6, 7]
  gray_areas_resolved:
    - topic: "context type"
      decision: "brownfield; existing Java Swing arcade game with shipped MVP"
    - topic: "next PRD direction"
      decision: "Focus first on Refactor Pack to prepare a stronger base for future development; Replayability Pack follows later; Polish Pack, High Scores Pack, and Distribution Pack are deferred to a later version."
    - topic: "change category"
      decision: "Architecture improvement without gameplay changes."
    - topic: "primary persona"
      decision: "Developer/maintainer continuing development of the game."
    - topic: "phase 1 guardrail"
      decision: "Zero visible gameplay behavior changes."
    - topic: "why not done earlier"
      decision: "Central GameController was pragmatic for the MVP, but now constrains future development."
    - topic: "access control"
      decision: "No access control changes; local single-player desktop game with no login and no roles."
    - topic: "refactor MVP scope"
      decision: "Extract GameSession and GameRules without visible gameplay behavior changes."
    - topic: "blast radius"
      decision: "Limit the change to GameController, controller tests, and session/reset/scoring/wave flow."
    - topic: "delivery time"
      decision: "One week."
    - topic: "secondary success"
      decision: "Make later Replayability Pack work easier."
    - topic: "functional requirements set"
      decision: "Use the four-FR MVP set for GameSession, GameRules, preserved gameplay behavior, and preserved test confidence."
    - topic: "business logic change"
      decision: "Reorganize existing rules without changing domain behavior."
    - topic: "constraints and preserved behavior"
      decision: "Preserve Swing, current launch path, no new runtime dependencies, and existing player-visible behavior."
    - topic: "non-functional requirements"
      decision: "Game remains responsive as before; tests pass; no new runtime dependencies."
    - topic: "product framing"
      decision: "No product type change; existing Java Swing desktop game."
    - topic: "target scale"
      decision: "No user-base change; local single-player game for a small audience."
    - topic: "deadline"
      decision: "No hard deadline."
    - topic: "work mode"
      decision: "After-hours work."
    - topic: "non-goals"
      decision: "Do not add power-ups/new aliens, do not rewrite Swing rendering/GamePanel, do not add high scores/data persistence/JAR distribution, and do not change gameplay balance."
  frs_drafted: 4
  quality_check_status: accepted
timeline_budget:
  delivery_weeks: 1
  hard_deadline: null
  after_hours_only: true
---

# Shape Notes

## Initial Input

Source file: `ideas_2_0.md`

User-selected direction: focus first on the Refactor Pack to prepare a stronger foundation for future development. Replayability Pack should follow later. Polish Pack, High Scores Pack, and Distribution Pack remain outside this PRD and are deferred to a later version.

## Current System

Aliens Attack is an existing Java 21 + Maven + Swing desktop arcade game. Version 1.0.0 is a playable MVP with a complete loop: start menu, gameplay, waves, score, lives, Game Over, restart, basic audio feedback, and shooting aliens.

The current architecture is a single-process Swing application. `GameController` is the central node for input, ticks, collision logic, waves, scoring, lives, missiles, audio, and game state. `GamePanel` renders the game, and the controller pushes scalar HUD/game-state values into it each tick. Runtime dependencies remain limited to the JDK standard library.

Current users are players of the local desktop game. For this change, the primary affected persona is the developer/maintainer extending the game after the MVP.

## Vision & Problem Statement

The next change is an architecture improvement without visible gameplay changes. The immediate pain is that `GameController` was pragmatic for shipping the MVP, but its current scope makes future mechanics riskier and more expensive to add.

The motivation is to prepare the codebase for later Replayability Pack work by extracting a cleaner gameplay foundation first. This PRD should not add power-ups, new aliens, polish, high scores, or distribution work.

Must be preserved: the visible gameplay behavior of the existing MVP, Swing as the presentation layer, the current way of running the game, the zero external runtime dependency posture, and the existing test suite.

## User & Persona

Primary persona: developer/maintainer continuing development of Aliens Attack after the MVP.

The moment they feel the pain is when planning the next gameplay iteration, especially Replayability Pack mechanics, and seeing that adding more behavior directly to `GameController` would increase regression risk and make focused tests harder.

## Phase 1 Capture

Ból / luka: `GameController` centralizes too much gameplay responsibility for comfortable future development.

Obecny system: Aliens Attack v1.0.0 desktop arcade MVP.

Stos technologiczny: Java 21, Maven, Swing, JUnit 5 test-scope, JDK-only runtime.

Użytkownicy: players of the local desktop game; for this change, the developer/maintainer is the primary persona.

Musi zostać zachowane: zero visible gameplay behavior changes.

## Access Control

No access control changes are planned.

Current model preserved: Aliens Attack is a local single-player desktop game with no login, no accounts, and no role separation.

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

## Phase 3 Capture

Smallest incremental change: extract `GameSession` and `GameRules` without visible gameplay behavior changes.

Delta flow:

1. Developer runs the existing tests and has a green baseline.
2. Developer extracts testable classes for session state and gameplay rules from `GameController`.
3. The game still launches the same way.
4. Player sees the same behavior: start, movement, shooting, waves, score, lives, Game Over, and restart.
5. Tests cover scoring, wave/lives/reset behavior, and existing behavior continues to pass.

Blast radius: `GameController`, controller tests, and session/reset/scoring/wave flow.

Delivery budget: 1 week.

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

## Functional Requirements

- FR-001: Developer/maintainer can work with an extracted game session model covering score, wave, lives, game state, and reset. Priority: must-have. Change: new
  > Socratic: Considered counterargument: extracting session state may split the simple reset and game-state flow across too many places. Resolution: keep the FR, but the extraction must preserve a clear single reset/session boundary rather than scatter state ownership.
- FR-002: Developer/maintainer can work with extracted game rules for scoring and wave scaling. Priority: must-have. Change: new
  > Socratic: Considered counterargument: extracting scoring and wave-scaling rules could make later balancing harder if rules are locked behind a rigid abstraction too early. Resolution: keep the FR, but the extracted rules should stay small and focused on current behavior rather than over-designing for future mechanics.
- FR-003: Player can play Aliens Attack with the same visible behavior as before the refactor. Priority: must-have. Change: preserved
  > Socratic: No counterargument; remains as written.
- FR-004: Developer/maintainer can run the existing test suite and confirm no regression. Priority: must-have. Change: preserved
  > Socratic: Considered counterargument: the existing tests may not cover every player-visible behavior. Resolution: keep the FR, but do not treat green tests as the only confidence signal; a short manual smoke check remains necessary.

## Business Logic

The system preserves the existing arcade-session rules: score depends on wave, wave difficulty scales alien speed, and losing all lives or allowing invasion ends the session without changing visible gameplay.

This change reorganizes where the existing rules live; it does not add a new player-facing domain rule. The relevant inputs are the same player-visible session events as before: alien destruction, wave progression, life loss, reset, and Game Over triggers. The output remains the same score, wave, lives, and game-state behavior the player already sees.

## Constraints & Preserved Behavior

- Preserve Swing as the presentation layer.
- Preserve the current launch path and local desktop game model.
- Preserve the zero external runtime dependency posture.
- Preserve existing player-visible behavior: start, movement, shooting, wave progression, scoring, lives, Game Over, and restart.
- Preserve current test confidence and supplement it with a short manual smoke check because existing tests may not cover every visible behavior.
- No migration of player data is required for this refactor.
- No changes to external integrations are required; the game has no external runtime service integration.

## Non-Functional Requirements

- The game remains responsive to player input at the same perceived level as before the refactor.
- The automated test suite passes after the refactor.
- The runtime remains JDK-only with no new external runtime dependencies.

## Product Framing

Product type: no change. Aliens Attack remains an existing Java Swing desktop game.

Target scale: no change. The game remains a local single-player game for a small audience.

Timeline: 1 week, no hard deadline, after-hours work.

Additional brownfield constraints: this change should fit the current local development and verification workflow. It should not introduce a new release process, deployment target, CI/CD shape, or runtime platform.

## Non-Goals

- No power-ups or new alien types in this PRD; those belong to the later Replayability Pack.
- No rewrite of Swing rendering or `GamePanel`; this PRD focuses on gameplay foundation extraction, not presentation architecture.
- No high scores, data persistence, playable JAR distribution, or release packaging; those remain deferred to a later version.
- No gameplay balance changes; scoring, wave progression, lives, movement, shooting, and Game Over behavior should remain visibly unchanged.

## Quality cross-check

- Access control: present.
- Business logic: present.
- Project artifacts: present.
- Timeline cost acknowledgment: present; delivery budget is 1 week.
- Non-goals: present.
- Preserved behavior: present.
