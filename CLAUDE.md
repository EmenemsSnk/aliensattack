# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Aliens Attack is a single-process 2D desktop arcade game (Space Invaders–like) on Java + Maven + Java Swing (`javax.swing`). The **MVP shipped at version `1.0.0`** — it is a playable game end-to-end (start menu → play → game over → restart), not a prototype. It has **no external runtime dependencies** — JDK standard library only, including sound (JUnit 5 is a `test`-scope dependency, so it is not part of the runtime artifact). The product spec lives in `context/foundation/prd.md` (brownfield PRD) with `roadmap.md` alongside; `stack-assessment.md` and `health-check.md` hold the agent-readiness analysis. Completed work is captured as per-change folders under `context/archive/` (the `/10x-*` workflow); new work starts a folder under `context/changes/`.

## Build, run, verify

```bash
./mvnw clean compile                                              # build — MUST pass at every stage (the project's only hard guardrail)
./mvnw test                                                       # run the JUnit 5 test suite
./mvnw test -Dtest=GameControllerTest                             # run one test class (-Dtest=GameControllerTest#someMethod for one method)
./mvnw exec:java -Dexec.mainClass="com.emenems.games.aliens.Main" # run the game (needs a display; opens a Swing window)
```

A committed Maven wrapper (`mvnw`) pins the Maven version, so the build is reproducible; bare `mvn` still works under the active JDK. The verification loop is **compile + test + run + observe behavior by hand**. The automated harness is **JUnit 5** (`test` scope only) — add tests under `src/test/java/`. JUnit 5 is the one deliberately approved test dependency; do **not** add any other external library (game engine, audio, JSON parser, Mockito/Spock, etc.) on your own — the PRD bars new libraries without an explicit decision. The shipped runtime stays zero-dependency (even the audio is hand-synthesized — see below).

The Java compiler level is **pinned** to `maven.compiler.release=21` in `pom.xml` (with `project.build.sourceEncoding=UTF-8`). Java 21 is LTS and is the active local toolchain. Modern syntax is allowed — `var`, records, switch expressions, pattern matching, and text blocks are all available.

## Architecture

Packages under `com.emenems.games.aliens`:
- `gamemachines` — domain objects implementing the sealed `GameObject` interface (`getX`/`getY`): `Spaceship`, `Alien`, `Missile` (player), `AlienMissile` (enemy fire), and `RapidFirePowerUp` (falling collectible). Plain mutable state plus a `move()` method; pixel coordinates use `GameConstants.COMPONENT_SIZE`. `Alien` tracks `y` as a `double` (sub-pixel speed) and rounds in `getY()`.
- `controller` — `GameController` holds the game loop, input handling, collision logic, and the **state machine**. Per the PRD this stays the central node; a View/Controller refactor is explicitly out of scope.
- `gui` — Swing presentation: `WindowFrame` (the `JFrame`) and `GamePanel` (the `JPanel`; all `paintComponent` rendering — entities, HUD, hit-flash overlay, start menu, game-over screen — plus image loading from `src/main/resources/images/`).
- `audio` — `ArcadeSoundPlayer` synthesizes all sound at runtime via `javax.sound.sampled` (no audio files, no library): square/sine PCM tone buffers for shoot/explosion SFX and a looping background melody. **All audio calls swallow exceptions** — headless/CI machines have no mixer, and gameplay must continue silently. Keep that contract: never let a missing audio device throw into the game loop.
- `GameState` (top level) — the enum `START_MENU | PLAYING | GAME_OVER`.
- `GameConstants` (top level) — shared board dimensions, component size, and startup placement constants. Gameplay logic and rendering both read these values from here, not from `GamePanel`.
- `GameRules` (top level) — pure gameplay rule calculations for scoring and wave-speed scaling. Keep score-per-alien and alien-speed tuning here, and unit-test changes directly in `GameRulesTest`.
- `GameSession` (top level) — scalar arcade-session state and lifecycle: score, wave, lives, `GameState`, hit-feedback state, game-over title, rapid-fire effect timer, score application, wave advancement, life loss, Game Over transitions, and scalar reset. Keep entity lists, input, spawning, audio, timer, and panel update orchestration out of this class.

**The load-bearing wiring (read `Main.java` first):** `Main` constructs the `Spaceship` and the four `List<Missile>` / `List<AlienMissile>` / `List<Alien>` / `List<RapidFirePowerUp>` collections once, then passes the **same references** to both `GamePanel` (which reads them to render) and `GameController` (which mutates them each tick). There is no separate model object — the shared mutable lists *are* the game state, and rendering stays in sync because both sides point at the same instances. UI construction is correctly wrapped in `EventQueue.invokeLater`.

**Scalar/HUD state is *not* shared by reference.** `score`, `wave`, `lives`, `gameState`, the hit-feedback flag, game-over title, and rapid-fire timer are owned by `GameSession`; the panel keeps its own copies and the controller pushes session values every tick via `gamePanel.updateGameState(...)`. So the rule is: **collections flow by shared reference, scalars flow through `GameSession` and then an explicit panel push.** Add any new per-tick scalar to `GameSession` and that push channel — don't reach into the panel's fields.

**The loop (`GameController`):** a single `javax.swing.Timer` at `TIMER_DELAY_MS = 16` (~60 FPS) drives `actionPerformed → tick() + repaint`. `tick()` is gated by `GameSession.getGameState()`: **outside `PLAYING` it only pushes panel state and returns** (no entities move on the menu / game-over screens). While `PLAYING` the ordered pipeline is: update hit-feedback & fire-cooldown timers → fire held player missile if ready → move ship (from the held-key set, then `clampToBounds`) → move aliens / player missiles / alien missiles → maybe fire an alien missile → `checkCollisions` → (bail if game over) → `checkAlienInvasion` (bail if game over) → `cleanupOffscreenObjects` → `advanceWaveIfCleared` → push panel state.

**Input & state transitions:** `keyPressed`/`keyReleased` only (no `keyTyped`). On `START_MENU`/`GAME_OVER`, `ENTER` starts/restarts (`resetSession`). While `PLAYING`, arrow keys feed a `pressedMovementKeys` set (held keys move every tick); `SPACE` is hold-to-fire, throttled by `PLAYER_FIRE_COOLDOWN_TICKS`.

**Combat & lives:** `checkCollisionsWithMissile()` uses dedup `Set`s + a single `removeAll`, so a hit counts once and one missile can't clear multiple aliens in a tick; it delegates score application to `GameSession.addAlienKills(...)`, which uses `GameRules.alienScoreForWave(wave)`, and plays the explosion SFX. `checkCollisionsWithSpaceShip()` (ship↔alien) and `checkCollisionsWithAlienMissile()` (ship↔enemy fire) each remove the offender and call `loseLife()`. `GameSession.loseLife()` triggers the hit-feedback flash and, at 0 lives, enters `"GAME OVER"`. `checkAlienInvasion()` ends the session with `"ALIENS WIN"` if any alien reaches the bottom edge. **Lives live on `GameSession` (`DEFAULT_LIVES = 3`), not on `Spaceship`** — see the vestigial note below.

**Difficulty tuning (pure `GameRules` methods — keep new scoring and wave-speed tuning logic here, they're unit-tested):** `GameRules.alienScoreForWave(wave)` = `wave * 10`; `GameRules.alienSpeedForWave(wave)` = `0.8 * 1.15^(wave-1)` capped at `2.8`. `GameController` delegates to these rules while still owning wave advancement and alien spawning. Each wave spawns `ALIEN_COUNT = 6` aliens in jittered lanes near the top; they descend straight down (no horizontal sweep).

**Test seams:** `GameController` has a package-private constructor that injects a seeded `Random` and a stand-in `ArcadeSoundPlayer`, accepts a `null` `GamePanel`, and exposes package-private getters (`getScore`, `getWave`, `getLives`, `getGameState`, ...). Tests drive `tick()`/`handleKeyPressed(...)` directly and assert on those — follow that pattern (deterministic `Random`, no real window/audio) for new controller tests.

**Swing threading rule (critical):** all rendering and timer-driven game logic must run on the Event Dispatch Thread (EDT). Drive the loop with `javax.swing.Timer` (its `ActionListener` callbacks fire on the EDT) — not a background thread. Never do blocking or long-running work inside an EDT callback. (Audio playback uses non-blocking `Clip` start/loop, so it doesn't violate this.)

## Removed vestiges

- `Spaceship` no longer carries health or speed state. Lives and damage live entirely in `GameController`; extend the controller's `lives` flow rather than adding parallel state to the ship.
- The old commented-out `Point.java` placeholder was deleted. Coordinates remain plain `int x`/`int y` through the `GameObject` contract.
