# Skill-Based Score Combo Implementation Plan

## Overview

Implement the roadmap's skill-based score combo on top of the existing score, session, collision, rapid-fire, and HUD architecture. Quick consecutive scoring ticks build an immediately applied multiplier from x1 to x5. A 90-playing-tick delay, life loss, wave advancement, or restart resets the chain, while missed shots have no direct reset effect.

## Current State Analysis

- `GameSession` owns score, wave, lives, hit feedback, rapid-fire timing, and reset lifecycle. Its current `addAlienKills(int count)` applies the flat per-wave score formula without any temporal state (`src/main/java/com/emenems/games/aliens/GameSession.java:10-70`).
- `GameRules.alienScoreForWave(int wave)` is the existing pure scoring rule and natural home for bounded combo score calculations (`src/main/java/com/emenems/games/aliens/GameRules.java:10-12`).
- `GameController.tick()` advances playing-only timers before movement and collision resolution, then advances the wave after cleanup (`src/main/java/com/emenems/games/aliens/controller/GameController.java:205-234`).
- Missile collisions deduplicate aliens into one `Set`, then award one batch score through `session.addAlienKills(aliensToRemove.size())`. This is the correct event boundary for limiting combo growth to once per tick (`src/main/java/com/emenems/games/aliens/controller/GameController.java:264-291`).
- All life-loss paths funnel through `GameSession.loseLife()`, and all start/restart paths funnel through `GameSession.startOrRestart()`, giving combo resets clear ownership (`src/main/java/com/emenems/games/aliens/controller/GameController.java:243-261`, `293-310`, `416-442`, `510-515`).
- `GamePanel.updateGameState(...)` is the established scalar push channel. The HUD currently shows score, wave, lives, and rapid-fire time (`src/main/java/com/emenems/games/aliens/gui/GamePanel.java:63-81`, `114-128`).
- The existing automated baseline is green: `./mvnw clean compile` succeeds and `./mvnw test` passes 60 tests.

## Desired End State

The first resolved player-missile kill batch starts a 90-tick combo window and scores at x1. Each later non-empty kill batch while the window is active immediately increases the multiplier by one, capped at x5, refreshes the window, and scores every alien in that batch with the resulting shared multiplier. Because collision resolution submits one batch per tick, multiple same-tick kills never increase the multiplier more than once.

The combo timer advances only during `PLAYING`. If it expires before a later kill batch, that later batch begins a new x1 chain. Life loss, wave advancement, and start/restart reset both multiplier and timer. Missed shots do not directly affect the combo. From x2 onward, the HUD displays `COMBO xN: Ns`.

## Key Decisions

| Area | Decision | Rationale |
| --- | --- | --- |
| Timing | 90 playing ticks, approximately 1.5 seconds | Requires quick follow-up hits but remains achievable without rapid fire |
| Progression | One multiplier step per non-empty scoring tick | Aligns combo growth with meaningful resolved-hit events |
| Cap | x5 | Bounds score growth and rapid-fire amplification |
| Immediate reward | A qualifying follow-up batch uses its newly increased multiplier | Makes progression intuitive and rewarding |
| Same-tick kills | All kills share one multiplier; multiplier increases at most once | Avoids ordering ambiguity and batch-driven level skipping |
| Reset lifecycle | Timeout, life loss, wave advancement, and start/restart reset | Creates clear, testable scoring attempts |
| Missed shots | No direct reset | The shrinking time window already penalizes misses |
| Visibility | Show multiplier plus seconds remaining from x2 | Keeps x1 HUD noise out while making active combo actionable |

## What We Are NOT Doing

- No combo reset on firing or missing a shot.
- No score persistence, high-score table, leaderboard, achievements, or analytics.
- No separate combo entity, event bus, or broad state-management refactor.
- No new audio, animations, image assets, dependencies, or settings.
- No changes to rapid-fire drop chance, duration, or firing cooldown.
- No changes to base wave score or alien speed progression.

## Implementation Approach

Keep pure calculations in `GameRules`, temporal/scalar combo state in `GameSession`, collision-event orchestration in `GameController`, and display state in `GamePanel`. Extend the existing `addAlienKills(int count)` session boundary so the controller continues to submit only resolved kill counts and does not own multiplier math.

Treat each non-empty `addAlienKills` call as one scoring event. If no combo window is active, it starts at x1. If a window is active, the multiplier increments once up to x5 before calculating the batch score. Every scoring event refreshes the timer to 90 ticks. Calls with zero kills are no-ops and must neither start nor refresh combo state.

Tick combo before collision resolution, matching rapid fire. This means a timer with one tick remaining expires before collisions in that tick and the next resolved kill starts a new chain. Reset combo inside `GameSession.advanceWave()`, `loseLife()`, and `startOrRestart()` so lifecycle invariants remain valid regardless of controller caller.

## Critical Implementation Details

- **Batch semantics:** `HashSet` iteration has no stable scoring order. The plan deliberately gives every kill in a resolved tick batch the same multiplier and progresses the combo only once.
- **Wave-clear ordering:** The final kill receives its valid combo score, then `advanceWave()` resets combo before the next panel state push.
- **Timer visibility:** Combo is considered active for scoring while its timer is greater than zero. HUD visibility is stricter: show only when multiplier is at least x2 and timer is greater than zero.
- **No-op batches:** Existing collision code calls `addAlienKills(0)` on ticks without kills. The session method must preserve timer/multiplier state for zero counts; normal ticking, not a no-op score call, controls expiration.
- **Playing-time clock:** `GameController.tick()` returns early outside `PLAYING`, so the combo timer must be advanced only after that guard.

---

## Phase 1: Combo Rules and Session State

### Overview

Define the bounded scoring formula and add deterministic combo progression, timing, and reset behavior to the session owner.

### Changes Required

#### 1. Pure combo score rule

**File:** `src/main/java/com/emenems/games/aliens/GameRules.java`

**Purpose:** Keep combo score math independently testable and prevent callers from applying unbounded multipliers.

**Contract:** Add a public maximum combo multiplier of `5` and a pure score calculation that returns `killCount * alienScoreForWave(wave) * boundedMultiplier`. The rule must clamp the supplied multiplier to the supported x1-x5 range and produce zero for zero kills.

#### 2. Session combo state and scoring-event progression

**File:** `src/main/java/com/emenems/games/aliens/GameSession.java`

**Purpose:** Make combo timing and progression part of the same scalar lifecycle that already owns score, wave, lives, and rapid fire.

**Contract:** Add a public `COMBO_DURATION_TICKS` constant of `90`, combo multiplier initialized to x1, combo ticks initialized to zero, getters for both values, and a visibility/active-state query as needed by callers. Change `addAlienKills(int count)` so a positive count starts or advances one combo step, immediately scores the whole batch with the resulting multiplier, and refreshes the timer; a zero count is a no-op.

#### 3. Session timer and reset invariants

**File:** `src/main/java/com/emenems/games/aliens/GameSession.java`

**Purpose:** Ensure all session lifecycle paths consistently end or preserve combo according to the agreed rules.

**Contract:** Add a combo tick method that decrements a positive timer and restores x1 when the timer reaches zero. Reset combo on `startOrRestart()`, `loseLife()`, and `advanceWave()`. Rapid-fire activation/expiration and missed shots must not alter combo state.

#### 4. Pure rule tests

**File:** `src/test/java/com/emenems/games/aliens/GameRulesTest.java`

**Purpose:** Lock down score multiplication and cap behavior independently of temporal session logic.

**Contract:** Cover base x1 scoring, increased multipliers, multiple kills in one batch, the x5 cap, and zero kills.

#### 5. Session lifecycle tests

**File:** `src/test/java/com/emenems/games/aliens/GameSessionTest.java`

**Purpose:** Verify the complete combo state machine and its interaction with existing session resets.

**Contract:** Cover first kill at x1, qualifying follow-up at x2 with immediate score effect, one increment per batch regardless of kill count, refresh behavior, x5 cap, timeout reset, zero-count no-op, life-loss reset, wave reset, start/restart reset, and independence from rapid fire.

### Success Criteria

#### Automated Verification

- `./mvnw clean compile` succeeds.
- `./mvnw test -Dtest=GameRulesTest,GameSessionTest` passes with non-zero test counts.
- Session tests prove a positive scoring event refreshes to 90 ticks while a zero-count call does not.
- Session tests prove multiplier and timer reset together on every agreed reset path.

#### Manual Verification

- No manual gameplay verification is required in this phase because combo is not yet connected to the controller or HUD.

---

## Phase 2: Controller Collision and Lifecycle Integration

### Overview

Connect the session combo clock and scoring event to the existing tick-level collision batch, preserving the agreed same-tick and reset semantics.

### Changes Required

#### 1. Playing-tick combo countdown

**File:** `src/main/java/com/emenems/games/aliens/controller/GameController.java`

**Purpose:** Advance combo time on the same gameplay-relative clock as other temporary session effects.

**Contract:** Tick combo once per `PLAYING` controller tick before collision resolution. Non-playing ticks must not consume combo time.

#### 2. Batch scoring integration

**File:** `src/main/java/com/emenems/games/aliens/controller/GameController.java`

**Purpose:** Preserve the existing deduplicated resolved-kill boundary while enabling combo-aware scores.

**Contract:** Continue passing exactly one kill count from `aliensToRemove.size()` to `GameSession` per missile-collision pass. Do not iterate destroyed aliens for score progression. Multiple destroyed aliens in the same pass must produce one multiplier step and one shared multiplier for their total score.

#### 3. Controller combo state exposure

**File:** `src/main/java/com/emenems/games/aliens/controller/GameController.java`

**Purpose:** Support focused integration tests and the existing scalar state push without moving ownership out of the session.

**Contract:** Add package-visible combo multiplier/tick getters following the existing rapid-fire getter pattern, and extend `updatePanelState()` with combo multiplier and timer values.

#### 4. Controller integration tests

**File:** `src/test/java/com/emenems/games/aliens/controller/GameControllerTest.java`

**Purpose:** Protect collision batching, timer ordering, and lifecycle integration that unit-level session tests cannot prove.

**Contract:** Cover consecutive kill ticks increasing and refreshing combo, a delayed kill after expiration restarting at x1, multiple same-tick kills increasing only once and sharing one multiplier, no-kill ticks not directly resetting before timeout, missed/offscreen missiles not directly resetting combo, life loss resetting combo, wave clear scoring the final batch before resetting combo, and rapid-fire state not changing combo rules.

### Success Criteria

#### Automated Verification

- `./mvnw clean compile` succeeds.
- `./mvnw test -Dtest=GameControllerTest` passes with non-zero test count.
- Integration tests prove two same-tick kills cannot advance more than one multiplier level.
- Integration tests prove final-wave kills receive combo score before wave advancement resets state.
- Existing rapid-fire controller tests remain green.

#### Manual Verification

- During a temporary diagnostic run or debugger inspection, quick kills increase score according to x1, x2, and later multipliers.
- Holding fire without hitting an alien does not directly cancel combo; only elapsed playing ticks do.

---

## Phase 3: Combo HUD and Full Regression Verification

### Overview

Expose the active combo to the player, test countdown conversion, and verify the complete feature with and without rapid fire.

### Changes Required

#### 1. Combo state push and HUD rendering

**File:** `src/main/java/com/emenems/games/aliens/gui/GamePanel.java`

**Purpose:** Make the score mechanic visible enough for players to understand and intentionally maintain it.

**Contract:** Extend `updateGameState(...)` with combo multiplier and combo ticks. Draw `COMBO xN: Ns` below the existing rapid-fire HUD area only while multiplier is at least x2 and combo ticks are positive. Use ceiling division at the existing 60 ticks-per-second display rate so a positive partial second remains visible as `1s`.

#### 2. HUD conversion tests

**File:** `src/test/java/com/emenems/games/aliens/gui/GamePanelTest.java`

**Purpose:** Lock the countdown's boundary behavior and visibility rule without brittle pixel-level Swing assertions.

**Contract:** Cover combo seconds conversion for full and partial seconds and the predicate/state rule that hides x1 or expired combo and shows an active x2+ combo.

#### 3. Full regression verification

**Files:** Existing production and test suite

**Purpose:** Confirm the score combo does not regress rapid fire, base scoring, wave progression, life loss, restart, or rendering.

**Contract:** Run the full compile/test suite, then manually play representative sequences with normal fire and rapid fire, including same-tick/multi-missile opportunities, timeout, damage, and wave clear.

### Success Criteria

#### Automated Verification

- `./mvnw clean compile` succeeds.
- `./mvnw test` passes with a non-zero test count and no regressions.
- Panel tests prove positive partial seconds round up and the combo indicator is limited to active x2+ state.
- Existing score, wave, life, restart, and rapid-fire tests remain green.

#### Manual Verification

- A first kill awards base score and does not add noisy x1 combo text to the HUD.
- A quick second kill awards x2 score and displays `COMBO x2` with a readable countdown.
- Continued quick kills visibly progress up to x5 but never beyond it.
- Waiting about 1.5 seconds resets the multiplier; the next kill awards x1 score.
- Losing a life or advancing a wave immediately removes the combo indicator and resets scoring to x1.
- Missed shots do not directly cancel combo.
- Rapid fire helps create follow-up opportunities but same-tick kills cannot skip multiplier levels.

## Testing Strategy

### Unit Tests

- `GameRulesTest`: combo-aware score calculation, multiple-kill batch math, lower/upper bounds, and zero kills.
- `GameSessionTest`: first event, immediate multiplier progression, refresh, cap, timeout, no-op batch, and lifecycle resets.
- `GamePanelTest`: countdown ceiling conversion and active x2+ visibility rule.

### Integration Tests

- `GameControllerTest`: playing-tick countdown order, deduplicated collision batch semantics, same-tick multi-kills, timeout/restart behavior, life loss, wave clear, missed shots, and rapid-fire coexistence.

### Manual Testing Steps

1. Run `./mvnw exec:java -Dexec.mainClass="com.emenems.games.aliens.Main"`.
2. Start a game and destroy one alien; confirm base score and no combo indicator.
3. Destroy another alien within about 1.5 seconds; confirm x2 score and visible countdown.
4. Continue quick kills; confirm progression stops at x5 and each scoring tick refreshes the countdown.
5. Fire and miss while maintaining a valid time window; confirm the miss does not directly reset combo.
6. Wait for the timer to expire; confirm the indicator disappears and the next kill scores at x1.
7. Lose a life and clear a wave while combo is active; confirm both reset it.
8. Collect rapid fire and create multiple missile hits; confirm combo remains controllable and cannot jump multiple levels in one tick.

## Performance Considerations

The feature adds constant-time scalar updates and score arithmetic per playing tick or scoring event. It creates no entities, collections, persistence, or additional rendering loops. The same-tick batch contract uses the already deduplicated collision result and avoids per-kill combo state transitions.

## Migration and Rollback

No persisted data or external contract exists, so no migration is required. Rollback consists of removing combo state/rules, restoring flat `addAlienKills` scoring, removing the two combo values from the panel state push, and deleting the associated tests.

## References

- Product requirements: `context/foundation/prd.md`
- Roadmap replayability slice: `context/foundation/roadmap.md`
- Previous base score plan: `context/archive/2026-05-29-score-and-wave-progression/plan.md`
- Rapid-fire dependency and patterns: `context/archive/2026-06-04-rapid-fire-power-up/plan.md`
- Session state: `src/main/java/com/emenems/games/aliens/GameSession.java`
- Pure gameplay rules: `src/main/java/com/emenems/games/aliens/GameRules.java`
- Tick and collision integration: `src/main/java/com/emenems/games/aliens/controller/GameController.java`
- HUD state and rendering: `src/main/java/com/emenems/games/aliens/gui/GamePanel.java`

## Progress

> Convention: `- [ ]` pending, `- [x]` done. Add ` — <commit sha>` when a step is committed.

### Phase 1: Combo Rules and Session State

#### Automated

- [x] 1.1 Pure combo score rules and bounds are covered.
- [x] 1.2 Session progression, timer, cap, no-op batch, and reset lifecycle are covered.
- [x] 1.3 Project compiles and focused rule/session tests pass.

#### Manual

- [x] 1.4 No manual verification required; combo is not yet connected to gameplay.

### Phase 2: Controller Collision and Lifecycle Integration

#### Automated

- [x] 2.1 Controller ticks combo only during play and uses one scoring event per collision batch.
- [x] 2.2 Same-tick multi-kill, timeout, life-loss, wave-clear, missed-shot, and rapid-fire integration tests pass.
- [x] 2.3 Project compiles and focused controller tests pass.

#### Manual

- [x] 2.4 Quick kills increase score according to the expected multiplier.
- [x] 2.5 Holding fire without hits does not directly cancel combo.

### Phase 3: Combo HUD and Full Regression Verification

#### Automated

- [x] 3.1 Combo HUD countdown and visibility rules are covered.
- [x] 3.2 Full compile and test suite pass with non-zero test count.

#### Manual

- [x] 3.3 HUD clearly shows active x2-x5 combo and countdown.
- [x] 3.4 Timeout, life loss, wave advancement, and restart visibly reset combo.
- [x] 3.5 Rapid fire does not allow same-tick kills to skip combo levels.
