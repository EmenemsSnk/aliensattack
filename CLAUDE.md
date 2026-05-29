# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Aliens Attack is a single-process 2D desktop arcade game (Space Invaders–like) on Java + Maven + Java Swing (`javax.swing`). It has **no external dependencies** — JDK standard library only. Active work is defined in `context/foundation/prd.md` (a brownfield PRD turning the current prototype into a playable game); `context/foundation/stack-assessment.md` and `context/foundation/health-check.md` hold the agent-readiness analysis.

## Build, run, verify

```bash
mvn clean compile                                                # build — MUST pass at every stage (the project's only hard guardrail)
mvn exec:java -Dexec.mainClass="com.emenems.games.aliens.Main"   # run the game
```

There is no test suite. The verification loop is **compile + run + observe behavior by hand** — there is no automated way to confirm a change. Do **not** add JUnit, TestNG, or any other dependency on your own: the PRD bars new external libraries without an explicit decision. If automated tests are wanted, raise it and get sign-off before editing `pom.xml`.

The Java compiler level is **not pinned** in `pom.xml` (no `maven-compiler-plugin`, no `maven.compiler.*` properties), so the build silently targets whatever local JDK is installed. Write **Java 8** syntax — avoid `var`, records, and switch expressions — until the level is pinned. Pinning it (`maven.compiler.source`/`target` = `1.8`) is a recommended fix.

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