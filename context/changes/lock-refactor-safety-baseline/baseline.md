# Lock Refactor Safety Baseline

## Scope

This artifact locks the S-01 safety baseline for the Aliens Attack refactor pack.
It exists to make later `extract-game-rules` and `extract-game-session` work
auditable before production gameplay code is moved.

In scope:

- Record the current automated regression baseline.
- Define the canonical automated gate for future refactor slices.
- Track focused regression coverage added during S-01.
- Preserve a repeatable manual smoke checklist for visible gameplay behavior.
- Record dated evidence from automated and manual verification.

Out of scope for S-01:

- Extracting `GameRules`, `GameSession`, or any other production gameplay
  abstraction.
- Changing production gameplay behavior, scoring, wave progression, movement,
  lives, Game Over, restart, rendering, or audio.
- Adding GUI automation, runtime dependencies, or CI workflow changes.

## Current Automated Baseline

Initial baseline date: 2026-05-31.

Before S-01 implementation adds any new tests:

| Command | Result | Notes |
| --- | --- | --- |
| `./mvnw clean compile` | Passing | Existing compile gate succeeds. |
| `./mvnw test` | Passing | 37 tests passed across 3 test classes. |

The existing CI workflow already runs the same compile and test commands on
push and pull request.

## Canonical Safety Gate

Before and after each future refactor slice, run:

```bash
./mvnw clean compile
./mvnw test
```

Both commands must pass before the slice is treated as safe to continue or
archive. Green automated checks are required but not sufficient: visible
gameplay must also be smoke-checked by a human when behavior could be affected.

## Focused Test Coverage

Pre-implementation coverage already includes controller regression tests for:

- Tick gating outside `PLAYING`.
- Missile, alien, alien missile, and spaceship collision behavior.
- Score calculation for a wave-1 missile kill.
- Wave advancement and generated alien placement.
- Held movement and movement clamping.
- Hold-to-fire cooldown behavior.
- Life loss, hit feedback, Game Over, and restart basics.
- Alien missile cap and cleanup.

S-01 adds focused regression coverage for:

- Restart clearing held fire state and player fire cooldown.
- Restart preserving the spaceship's current coordinates.
- Restart clearing player missiles and alien missiles while spawning a fresh wave.
- Active-wave scoring after advancing to wave 2.

## Manual Smoke Checklist

Status: pending human verification.

Run:

```bash
./mvnw exec:java -Dexec.mainClass="com.emenems.games.aliens.Main"
```

Checklist:

- [ ] Launch opens the Swing window to the start menu.
- [ ] Pressing Enter starts gameplay.
- [ ] Arrow-key movement works and the ship remains within the board.
- [ ] Holding Space repeatedly fires missiles with a visible cooldown.
- [ ] Destroying aliens increases score and clearing a wave advances to the next wave.
- [ ] Alien or alien missile contact removes lives and shows hit feedback.
- [ ] Losing all lives reaches Game Over.
- [ ] Pressing Enter on Game Over restarts with projectiles cleared and a fresh session.

Do not mark this checklist complete unless the game is actually launched and
checked by a human with a display.

## Evidence Log

| Date | Evidence | Result | Notes |
| --- | --- | --- | --- |
| 2026-05-31 | `./mvnw clean compile` | Passing | Initial baseline before S-01 test additions. |
| 2026-05-31 | `./mvnw test` | Passing | Initial baseline: 37 tests across 3 test classes before S-01 test additions. |
