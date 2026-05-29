# Score and Wave Progression Implementation Plan

## Overview

Implement the S-02 roadmap slice: scoring, live HUD score/wave display, and automatic wave progression after all aliens are cleared. This change builds on the fresh-master S-01 timer loop and keeps the remaining scope focused on score and wave state while fitting the current `GameController`-centered architecture.

## Current State Analysis

- `GameController` owns the shared mutable `spaceship`, `missiles`, and `aliens` lists and mutates them from the game loop and keyboard input.
- `GameController.generateSpaceObjects()` currently seeds one fixed row of 10 aliens with hard-coded coordinates.
- `GameController.checkCollisionsWithMissile()` removes hit aliens but has no score side effect and does not trigger a next wave.
- `Alien.move()` always advances by a hard-coded 5 pixels; the unused `speed` field means wave speed cannot be varied yet.
- `GamePanel.paintComponent()` draws the background, spaceship, aliens, and missiles, but no HUD text.
- Roadmap S-02 depends on S-01. The branch was initially planned on stale master, then synchronized with the fresh master that contains S-01 before implementation was corrected.

## Desired End State

After this plan, destroying an alien awards `10 * currentWave` points, the current score and wave are visible in a HUD during play, and clearing all aliens immediately spawns the next 10-alien wave. Each new wave moves faster according to `baseSpeed * 1.1^(wave - 1)`, capped at 2x the base speed.

### Key Findings:

- `src/main/java/com/emenems/games/aliens/controller/GameController.java:51` is the current wave seed point.
- `src/main/java/com/emenems/games/aliens/controller/GameController.java:107` is the only existing missile-vs-alien removal path.
- `src/main/java/com/emenems/games/aliens/gamemachines/Alien.java:13` hard-codes alien movement; S-02 makes that speed configurable per wave.
- `src/main/java/com/emenems/games/aliens/gui/GamePanel.java:60` is the rendering hook for a simple HUD overlay.
- `context/foundation/roadmap.md:87` defines S-02 as score, HUD, and faster-wave progression.

## What We Are NOT Doing

- No additional `javax.swing.Timer` conversion, FPS fix, or EDT cleanup beyond the S-01 code already on master.
- No additional missile/alien off-screen cleanup beyond the S-01 code already on master.
- No Game Over, lives, restart, or final score screen from S-03.
- No start menu, sound, alien fire, persistence, or external dependencies.
- No View/Controller architecture refactor.

## Implementation Approach

Keep state in `GameController`, because that is the established central node. Add a small immutable gameplay-state surface on `GamePanel` through primitive setters so rendering can show score and wave without owning game rules. Extract score and wave-speed formulas into static package-visible methods on `GameController` so JUnit can verify the rules without driving Swing.

## Critical Implementation Details

- **S-01 dependency risk**: this change assumes the fresh-master S-01 timer loop is present. Implementing it on stale master gives misleading manual behavior.
- **Wave generation timing**: spawn the next wave after collision/removal and off-screen cleanup have finished and only when the alien list is empty. This catches both successful clears and the S-01 cleanup path.

---

## Phase 1: Domain State and Wave Rules

### Overview

Add score/wave state to the controller, make aliens configurable by speed, and make wave generation reusable.

### Changes Required:

#### 1. Alien movement speed

**File**: `src/main/java/com/emenems/games/aliens/gamemachines/Alien.java`

**Purpose**: Allow later waves to move faster without changing every tick loop.

**Contract**: `Alien` keeps its existing two-argument constructor behavior as base speed, adds a three-argument constructor accepting `speed`, and `move()` advances `y` by the instance speed.

#### 2. GameController score and wave fields

**File**: `src/main/java/com/emenems/games/aliens/controller/GameController.java`

**Purpose**: Track current score, current wave, base speed, and capped wave speed in the central game logic owner.

**Contract**: Add fields for `score`, `wave`, base alien speed `1` for the S-01 60 FPS timer loop, wave speed cap multiplier `2`, and wave size/positions matching the current 10-alien row.

#### 3. Reusable wave generation

**File**: `src/main/java/com/emenems/games/aliens/controller/GameController.java`

**Purpose**: Replace the fixed startup-only generation with reusable generation for wave 1 and later waves.

**Contract**: `generateSpaceObjects()` clears/refills the shared `aliens` list with 10 aliens at the existing x/y positions using the current wave speed.

### Success Criteria:

#### Automated Verification:

- Project compiles: `./mvnw clean compile`
- Existing tests pass: `./mvnw test`

#### Manual Verification:

- Launching the game still shows the initial row of 10 aliens.
- Aliens still move downward with no rendering regression.

**Implementation note**: Manual verification is not auto-checked; do not mark manual progress complete until a human confirms it.

---

## Phase 2: Score, HUD, and Next-Wave Progression

### Overview

Award points when aliens are destroyed, update the HUD, and spawn the next wave when the current wave is cleared.

### Changes Required:

#### 1. Scoring side effect on alien destruction

**File**: `src/main/java/com/emenems/games/aliens/controller/GameController.java`

**Purpose**: Make missile hits affect player progress, not only object lists.

**Contract**: When `checkCollisionsWithMissile()` identifies aliens removed by missile collisions, add `calculateAlienScore(wave)` for each removed alien.

#### 2. Next-wave trigger

**File**: `src/main/java/com/emenems/games/aliens/controller/GameController.java`

**Purpose**: Turn clearing a wave into continued play.

**Contract**: After collision resolution, if `aliens` is empty, increment `wave`, regenerate the 10-alien row using capped wave speed, and push the updated score/wave to the panel before repaint.

#### 3. HUD rendering

**File**: `src/main/java/com/emenems/games/aliens/gui/GamePanel.java`

**Purpose**: Make score and wave visible while playing.

**Contract**: Add `score` and `wave` fields plus an `updateHud(int score, int wave)` method. `paintComponent()` draws a readable HUD over the background before or after sprites, with text `Score: <score>` and `Wave: <wave>`.

### Success Criteria:

#### Automated Verification:

- Project compiles: `./mvnw clean compile`
- Existing tests pass: `./mvnw test`

#### Manual Verification:

- HUD shows `Score: 0` and `Wave: 1` when the game starts.
- Destroying an alien increases score by 10 on wave 1.
- Clearing all aliens advances HUD to wave 2 and spawns a new row.

**Implementation note**: Manual verification is not auto-checked; do not mark manual progress complete until a human confirms it.

---

## Phase 3: Formula Tests and Final Verification

### Overview

Add focused JUnit coverage for the pure scoring and wave-speed rules, then run the full automated verification loop.

### Changes Required:

#### 1. GameController formula tests

**File**: `src/test/java/com/emenems/games/aliens/controller/GameControllerTest.java`

**Purpose**: Lock the PRD formulas without testing Swing rendering or driving the game loop.

**Contract**: Test `calculateAlienScore(wave)` for waves 1 and 3, and `calculateAlienSpeed(wave, baseSpeed, maxSpeed)` for wave 1, wave 2, and capped higher waves.

### Success Criteria:

#### Automated Verification:

- Project compiles: `./mvnw clean compile`
- Full test suite passes with non-zero test count: `./mvnw test`

#### Manual Verification:

- No visible regression in movement, firing, alien rendering, missile rendering, or HUD readability after the test-backed formula extraction.

**Implementation note**: Manual verification is not auto-checked; do not mark manual progress complete until a human confirms it.

---

## Testing Strategy

### Unit Tests:

- `GameController.calculateAlienScore(int wave)` returns `wave * 10`.
- `GameController.calculateAlienSpeed(int wave, int baseSpeed, int maxSpeed)` returns base speed for wave 1, increases from wave 2 onward, and never exceeds the cap.

### Integration Tests:

- No automated Swing integration tests in this change; the project currently uses only JUnit for pure logic and manual game observation for UI.

### Manual Testing Steps:

1. Run `./mvnw exec:java -Dexec.mainClass="com.emenems.games.aliens.Main"`.
2. Confirm the HUD starts at `Score: 0` and `Wave: 1`.
3. Fire at aliens and confirm score increments by 10 per destroyed alien on wave 1.
4. Clear the row and confirm a new row appears with `Wave: 2`.
5. Confirm existing movement, firing, and sprite rendering still work.

## Performance Considerations

The added work is constant-time HUD drawing and small list regeneration for 10 aliens per wave. The current game-loop performance limitation is the existing S-01 1 FPS thread loop, which this change leaves untouched.

## Migration Notes

No persistent data or saved game state exists, so there is nothing to migrate.

## References

- Product requirement: `context/foundation/prd.md`
- Roadmap slice: `context/foundation/roadmap.md`
- Controller state and collisions: `src/main/java/com/emenems/games/aliens/controller/GameController.java:51`
- HUD rendering hook: `src/main/java/com/emenems/games/aliens/gui/GamePanel.java:60`
- Alien movement: `src/main/java/com/emenems/games/aliens/gamemachines/Alien.java:13`

## Progress

> Convention: `- [ ]` pending, `- [x]` done. Add ` — <commit sha>` when a step is committed. See `references/progress-format.md`.

### Phase 1: Domain State and Wave Rules

#### Automated

- [x] 1.1 Project compiles: `./mvnw clean compile`
- [x] 1.2 Existing tests pass: `./mvnw test`

#### Manual

- [x] 1.3 Launching the game still shows the initial row of 10 aliens
- [x] 1.4 Aliens still move downward with no rendering regression

### Phase 2: Score, HUD, and Next-Wave Progression

#### Automated

- [x] 2.1 Project compiles: `./mvnw clean compile`
- [x] 2.2 Existing tests pass: `./mvnw test`

#### Manual

- [x] 2.3 HUD shows `Score: 0` and `Wave: 1` when the game starts
- [x] 2.4 Destroying an alien increases score by 10 on wave 1
- [x] 2.5 Clearing all aliens advances HUD to wave 2 and spawns a new row

### Phase 3: Formula Tests and Final Verification

#### Automated

- [x] 3.1 Project compiles: `./mvnw clean compile`
- [x] 3.2 Full test suite passes with non-zero test count: `./mvnw test`

#### Manual

- [x] 3.3 No visible regression in movement, firing, alien rendering, missile rendering, or HUD readability
