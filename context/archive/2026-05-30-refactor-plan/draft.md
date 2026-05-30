# Refactor Plan — Aliens Attack (Java 21)

> Superseded by `context/changes/refactor-plan/plan.md`. Kept for historical analysis only.
>
> **Date:** 2026-05-30
> **Scope:** `src/main/java` — all production code
> **Constraint:** Zero new libraries (only JUnit 5 test-scope exists). No behavioral change.

---

## 1. Dead Code & Vestigial Fields

| Location | Issue | Action |
|----------|-------|--------|
| `Spaceship.health` | Never read anywhere — lives managed by `GameController` | **Remove field** |
| `Spaceship.speed` | Declared but never assigned/read — movement is hardcoded ±5 | **Remove field** |
| `Spaceship.decreaseHealth(int)` | Dead method — no callers | **Remove method** |
| `Point.java` | Entire file is commented out (placeholder) | **Delete file** |
| `GamePanel.MINIMUM_BORDER_VALUE` | Constant declared, never referenced by any code | **Remove constant** |
| `GamePanel.DEFAULT_DIMENSION` | `new Dimension(42,42)` constant declared, never used | **Remove constant** |
| `GamePanel(Spaceship, List<Missile>, List<Alien>)` (3-arg ctor) | Legacy overload — `Main` uses the 4-arg ctor; the 3-arg passes `List.of()` (immutable!) for `alienMissiles`, which would crash on `add`. Effectively dead/dangerous. | **Remove constructor** |
| `GamePanel.updateHud(int, int)` | Thin delegation to `updateGameState` — only used as backward-compat shim, no callers remain | **Remove method** |
| `GamePanel.updateGameState(int, int, int, GameState)` (4-arg) | Same — intermediate overload with no external callers | **Remove method** |
| `GameController(Spaceship, List<Missile>, List<Alien>, GamePanel)` (4-arg public) | Creates `new ArrayList<>()` for `alienMissiles` — test-only relic | **Remove or merge into canonical test constructor** |
| `GameController(…, Random)` (5-arg pkg-private without `alienMissiles`) | Same test-convenience duplication | **Remove — tests should use the 7-arg canonical** |

---

## 2. Code Smells & Bad Patterns

### 2.1 Magic Numbers
| File | Example | Recommendation |
|------|---------|----------------|
| `Spaceship` | `x -= 5` / `x += 5` | Extract `private static final int MOVE_STEP = 5` |
| `AlienMissile` | `alienMissile.getX() + 9`, width `7` in `alienMissileArea` (controller) | Extract constants `ALIEN_MISSILE_HITBOX_OFFSET_X = 9`, `ALIEN_MISSILE_HITBOX_WIDTH = 7` |
| `GamePanel.drawAlienMissiles` | `missile.getX() + 17`, width `8` | Extract `ALIEN_MISSILE_RENDER_OFFSET_X = 17`, `ALIEN_MISSILE_RENDER_WIDTH = 8` |
| `GamePanel.drawHitFeedback` | `2, 5, 70, 220` etc. | Group into named constants or keep as literal (presentation concern — lower priority) |

### 2.2 Formatting / Style
- **Inconsistent whitespace:** `missiles =  missiles` (double space before `missiles`), `new GameController(…,gamePanel)` missing space after comma in `Main.java`.
- **Missing braces spaces:** `main(String[] args){` → `main(String[] args) {`
- **Javadoc:** No Javadoc on public API anywhere (low priority for a game, but worth a single-sentence on `GameController.initialize()`).

### 2.3 Encapsulation / Coupling
| Issue | Detail |
|-------|--------|
| `GamePanel` constants used as game-world physics (`PANEL_WIDTH`, `DEFAULT_COMPONENT_SIZE`) in `GameController`, `Main`, entity spawning | These belong to a shared `GameConstants` class/interface — the panel shouldn't own game-world dimensions |
| `Spaceship` is mutable with no encapsulation of position | Consider extracting position logic, but per CLAUDE.md the shared-mutable-list contract is intentional, so leave mutable — just clean fields |
| `WindowFrame.show()` called in `Main` | `JFrame.show()` is deprecated since JDK 1.5 — use `setVisible(true)`. Actually `initUI()` already calls `setVisible(true)`, so the `show()` call is redundant **and** deprecated. **Remove the call.** |

### 2.4 Redundant / Duplicated Code
| Location | Detail |
|----------|--------|
| `startGame()` vs `restartGame()` | Bodies are **identical** — extract to single method or just call one from the other |
| `super.keyPressed(e)` / `super.keyReleased(e)` in `KeyAdapter` | `KeyAdapter` methods are no-ops — `super` call is useless noise | **Remove** |

### 2.5 Typo
- `Spaceship.decreaseHealth(int demage)` — param name `demage` → `damage` (dead code anyway, will be removed)

---

## 3. Wrong Package / Structural Issues

| Item | Current | Proposed |
|------|---------|----------|
| `GameState.java` | Root package `com.emenems.games.aliens` | Fine semantically (shared enum). No move needed. |
| `Point.java` | Root package — **empty placeholder** | **Delete** |
| Game-world constants (`PANEL_WIDTH`, `PANEL_HEIGHT`, `DEFAULT_COMPONENT_SIZE`) | Live on `GamePanel` (a Swing widget) | Extract to `com.emenems.games.aliens.GameConstants` (or an interface) so controller & entities reference game-world sizes without depending on the GUI package |

---

## 4. Java 21 Modernization Opportunities

| Opportunity | Detail |
|-------------|--------|
| `sealed` interface for `GameObject` | `GameObject` permits `Spaceship`, `Alien`, `Missile`, `AlienMissile` — makes the hierarchy explicit and enables exhaustive `switch` |
| `switch` expression in `handleKeyPressed` | The if-chain on `gameState` can become a switch expression (readability gain) |
| Pattern matching `instanceof` | Not directly applicable yet (no `instanceof` usage), but worth noting for future |
| Text blocks | No multi-line strings present — N/A |
| `var` for local variables | Applicable in `GameController.tick()` internals, `loadImages`, etc. — minor clarity gain |

---

## 5. Useless Constructor Telescope

`GameController` has **6 constructors** forming a telescope. Only 2 are actually needed:
1. **Production** (5-arg: spaceship, missiles, alienMissiles, aliens, gamePanel) — creates `Random` + `ArcadeSoundPlayer` internally.
2. **Test** (7-arg: all above + `Random` + `ArcadeSoundPlayer`) — full injection for deterministic tests.

The other 4 (3-arg, 4-arg, 5-arg-with-random, 6-arg-without-soundPlayer) are **intermediary convenience overloads that create partial defaults**. They make the API confusing and some produce hidden `new ArrayList<>()` instances that break the shared-reference contract documented in CLAUDE.md.

**Action:** Collapse to 2 constructors. Update tests to use the canonical 7-arg test constructor.

---

## 6. Performance & Thread-Safety Notes

| Area | Observation |
|------|-------------|
| `ArcadeSoundPlayer` — `synchronized` on `startBackgroundMusic` / `stopBackgroundMusic` | Both are called only from the EDT (via `Timer` → `tick()` → `startGame/enterGameOver`). The `synchronized` is unnecessary but harmless for now. Document that audio methods are EDT-only. |
| No off-EDT work | All game logic runs on the EDT via `javax.swing.Timer` — correct per CLAUDE.md. No changes needed. |

---

## 7. Proposed Execution Order (Safe Increments)

1. **Delete dead code** — `Point.java`, dead fields/methods on `Spaceship`, dead constructors on `GamePanel` / `GameController`, dead constants.
2. **Fix deprecated call** — remove `windowFrame.show()` in `Main.java`.
3. **Extract `GameConstants`** — move `PANEL_WIDTH`, `PANEL_HEIGHT`, `DEFAULT_COMPONENT_SIZE` there; update all references.
4. **Extract magic numbers** — `Spaceship.MOVE_STEP`, alien-missile hitbox/render constants.
5. **Collapse `GameController` constructors** — keep production + test only.
6. **Merge `startGame`/`restartGame`** into single private method.
7. **Apply `sealed` to `GameObject`** interface.
8. **Apply `switch` expression** in `handleKeyPressed`.
9. **Formatting pass** — consistent spacing, remove `super.keyPressed/Released` no-ops.
10. **Verify** — `./mvnw clean test` must pass green at each step.

---

## 8. Risks & Assumptions

- **Shared-mutable-list contract** (CLAUDE.md): any refactoring must not break the pattern where `Main` creates collections passed by reference to both `GamePanel` and `GameController`.
- **No Lombok:** Adding Lombok was considered (e.g., `@Getter` on game-object fields), but the project is zero-dependency at runtime and entities are mutable with domain logic — Lombok adds marginal value here. **Decision: skip Lombok.**
- **Test compatibility:** Existing tests use package-private constructor overloads and getters. The collapse to 2 constructors must be accompanied by updating test code in lock-step.
- **`sealed` interface change:** Requires all implementors to be in the same module (or declared `permits`). Since all are in the same module, this is safe.

---

## 9. Out of Scope (per CLAUDE.md / PRD)

- View/Controller separation refactor
- Introducing new external libraries
- Changing the `Timer`-driven game loop architecture
- Adding Spring, DI framework, or any build-time annotation processors
