# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Aliens Attack is a single-process 2D desktop arcade game (Space Invaders–like) on Java + Maven + Java Swing (`javax.swing`). The shipped game has **no external runtime dependencies** — JDK standard library only (JUnit 5 is a `test`-scope dependency, so it is not part of the runtime artifact). Active work is defined in `context/foundation/prd.md` (a brownfield PRD turning the current prototype into a playable game); `context/foundation/stack-assessment.md` and `context/foundation/health-check.md` hold the agent-readiness analysis.

## Build, run, verify

```bash
./mvnw clean compile                                              # build — MUST pass at every stage (the project's only hard guardrail)
./mvnw test                                                       # run the JUnit 5 test suite
./mvnw test -Dtest=GameControllerTest                             # run one test class (-Dtest=GameControllerTest#missileAlienCollisionRemovesBothObjects for one method)
./mvnw exec:java -Dexec.mainClass="com.emenems.games.aliens.Main" # run the game
```

A committed Maven wrapper (`mvnw`) pins the Maven version, so the build is reproducible; bare `mvn` still works under the active JDK. The verification loop is **compile + test + run + observe behavior by hand**. The automated harness is **JUnit 5** (`test` scope only) — add tests under `src/test/java/`. JUnit 5 is the one deliberately approved test dependency; do **not** add any other external library (game engine, audio, JSON parser, Mockito/Spock, etc.) on your own — the PRD bars new libraries without an explicit decision. The shipped runtime stays zero-dependency.

The Java compiler level is **pinned** to `maven.compiler.release=21` in `pom.xml` (with `project.build.sourceEncoding=UTF-8`). Java 21 is LTS and is the active local toolchain. Modern syntax is allowed — `var`, records, switch expressions, pattern matching, and text blocks are all available.

## Architecture

Three packages under `com.emenems.games.aliens`:
- `gamemachines` — domain objects implementing the `GameObject` interface (`getX`/`getY`): `Spaceship`, `Alien`, `Missile`. Plain mutable state plus movement methods; pixel coordinates, `25px` component size.
- `controller` — `GameController` holds the game loop, input handling, and collision logic. Per the PRD this stays the central node; a View/Controller refactor is explicitly out of scope.
- `gui` — Swing presentation: `WindowFrame` (the `JFrame`) and `GamePanel` (the `JPanel`; all `paintComponent` rendering, plus image loading from `src/main/resources/images/`).

**The load-bearing wiring (read `Main.java` first):** `Main` constructs the `Spaceship` and the `List<Missile>` / `List<Alien>` collections once, then passes the **same references** to both `GamePanel` (which reads them to render) and `GameController` (which mutates them each tick). There is no separate model object — the shared mutable lists *are* the game state, and rendering stays in sync because both sides point at the same instances. UI construction is correctly wrapped in `EventQueue.invokeLater`.

**Scalar HUD state is *not* shared by reference.** `score` and `wave` are owned by `GameController`; the panel has its own copies and the controller pushes them each tick via `gamePanel.updateHud(score, wave)` (drawn by `drawHud`). So the rule is: collections flow by shared reference, scalars flow by an explicit push call. Keep new per-tick scalars on this same push channel rather than reaching into the panel's fields.

**The loop (`GameController`):** a single `javax.swing.Timer` at `TIMER_DELAY_MS = 16` (~60 FPS) drives `actionPerformed → tick() + repaint`. `tick()` is the ordered pipeline: move ship (from the held-key set) → move aliens/missiles → `checkCollisions` → `cleanupOffscreenObjects` → `advanceWaveIfCleared` → `updateHud`. Input uses `keyPressed`/`keyReleased` feeding a `pressedMovementKeys` set (held keys move every tick); `SPACE` fires a missile on press. Difficulty scales per wave: `calculateAlienScore` (= `wave * 10`) and `calculateAlienSpeed` (`1.1^(wave-1)`, capped at `MAX_ALIEN_SPEED`) — both `static` and unit-tested, so keep tuning logic in pure static methods.

**Swing threading rule (critical):** all rendering and timer-driven game logic must run on the Event Dispatch Thread (EDT). Drive the loop with `javax.swing.Timer` (its `ActionListener` callbacks fire on the EDT) — not a background thread. Never do blocking or long-running work inside an EDT callback.

## Current state / known issues

The code is becoming a playable game but is not finished; `context/foundation/prd.md` and `context/foundation/roadmap.md` define remaining work. Already done (don't "fix" these — they were the old known issues, now resolved):
- The loop is a single EDT `javax.swing.Timer` (~60 FPS). The old background `Thread`/`while(true){ sleep }` and the unwired `actionPerformed` are gone.
- Offscreen cleanup exists (`cleanupOffscreenObjects`); lists no longer grow unbounded.
- `checkCollisionsWithMissile()` uses dedup `Set`s + single `removeAll`, so a hit is counted once and one missile can't clear multiple aliens in a tick.
- Input is `keyPressed`/`keyReleased` only (no `keyTyped`), so a keypress fires one action.
- Scoring and wave progression work end-to-end (HUD shows them).

Still open — a change will likely touch:
- **`checkCollisionsWithSpaceShip()` is still a no-op:** it detects the ship↔alien overlap but the hit branch is a bare `return`. No life loss, no game over.
- **No state machine.** There is no start / playing / game-over flow; the game just runs. `GameState.java` and `Point.java` are empty commented-out placeholders.
- **`Spaceship` carries unused `health`/`decreaseHealth`/`speed`** (health is never read; movement uses hardcoded `±5`). Wire health into the spaceship-collision branch when adding lives/game-over.
