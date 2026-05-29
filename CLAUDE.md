# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Aliens Attack is a single-process 2D desktop arcade game (Space Invaders–like) on Java + Maven + Java Swing (`javax.swing`). The shipped game has **no external runtime dependencies** — JDK standard library only (JUnit 5 is a `test`-scope dependency, so it is not part of the runtime artifact). Active work is defined in `context/foundation/prd.md` (a brownfield PRD turning the current prototype into a playable game); `context/foundation/stack-assessment.md` and `context/foundation/health-check.md` hold the agent-readiness analysis.

## Build, run, verify

```bash
./mvnw clean compile                                              # build — MUST pass at every stage (the project's only hard guardrail)
./mvnw test                                                       # run the JUnit 5 test suite
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

**Swing threading rule (critical):** all rendering and timer-driven game logic must run on the Event Dispatch Thread (EDT). Drive the loop with `javax.swing.Timer` (its `ActionListener` callbacks fire on the EDT) — not a background thread. Never do blocking or long-running work inside an EDT callback.

## Current state / known issues

The code is a prototype; `context/foundation/prd.md` defines the work to make it playable. A change will likely touch:
- `GameController` declares **two** competing update mechanisms: a background `Thread` running `while(true){ Thread.sleep(1000); ... }` (started in `initialize()`, ~1 FPS, and it mutates lists + repaints **off the EDT**), plus an unused `javax.swing.Timer` field and an `actionPerformed` body that is never wired up. The Timer-on-EDT path is the intended replacement for the thread.
- Missiles/aliens leaving the screen are never removed (`checkOutOfBoarder` is a commented-out TODO) → unbounded list growth.
- `checkCollisionsWithSpaceShip()` detects the hit but is a no-op (bare `return`) — no life loss / game over.
- `checkCollisionsWithMissile()` removes missiles both via a filter side-effect and again in a follow-up loop — collisions can be counted twice.
- `keyTyped` and `keyPressed` both call `makeAction`, so one keypress can trigger an action twice.
- `Spaceship` carries an unused `health`/`decreaseHealth`; `GameState` is an empty commented-out file. No state machine (start / playing / game-over) exists yet.
