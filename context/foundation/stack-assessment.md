---
project: "Aliens Attack"
assessed_at: 2026-05-28T20:49:20Z
agent_readiness: ready-with-compensation
context_type: brownfield
stack_components:
  language: Java
  framework: Java Swing (javax.swing)
  build_tool: Maven
  test_runner: JUnit 5
  package_manager: Maven Central
  ci_provider: GitHub Actions
  deployment_target: null
gates_passed: 8
gates_failed: 1
---

## Stack Components

> **Resolution (2026-05-29, via `build-tooling-baseline`)**: the two gaps below are now closed. The compiler level is pinned to `maven.compiler.release=21` and a JUnit 5 (`test`-scope) harness plus GitHub Actions CI are in place. The original assessment text is retained below for provenance; resolution notes are inlined.

- **Language — Java.** Statically typed by the language; the compiler enforces input/output shapes at build time, so an agent can reason about types from source without running the program. The language level is now pinned to `maven.compiler.release=21` (Java 21 LTS). *At assessment time it was unpinned — `pom.xml` declared no `maven-compiler-plugin` and no `maven.compiler.source`/`target`/`release`.*
- **Framework — Java Swing (`javax.swing`).** JDK standard-library GUI toolkit, no version of its own (ships with the JDK). Confirmed by `javax.swing` usage in `src/main/java/com/emenems/games/aliens/gui/GamePanel.java` and `WindowFrame.java`. Swing is a UI toolkit, not an application framework — it imposes a component/container/event model and the Event Dispatch Thread (EDT) threading rule, but no opinion on application architecture, packaging, or file layout.
- **Build tool — Maven.** `pom.xml` present (`com.emenems.games.aliens:aliens-attack:1.0-SNAPSHOT`). The POM is minimal: groupId/artifactId/version only, no `<build>`, no plugin management, no `<properties>`. No Maven wrapper (`mvnw`) is committed, so the build relies on the developer's locally installed Maven version.
- **Test runner — JUnit 5.** A `test`-scope `org.junit.jupiter:junit-jupiter` dependency and `src/test/java/` harness are now in place (`maven-surefire-plugin` runs them). *At assessment time none existed.*
- **Package manager — Maven Central** (Maven's default dependency resolution; no third-party repos declared).
- **CI/CD — not detected.** No `.github/workflows/`, `.gitlab-ci.yml`, `Jenkinsfile`, or `.circleci/`.
- **Deployment — not detected.** No `Dockerfile`/`docker-compose.yml`; this is a local desktop app launched via `mvn exec:java` (which resolves through Maven's default plugin groups and works without a POM declaration).
- **Instruction files — `CLAUDE.md` present.** Documents build/run commands, code style, and a "Project Architecture" section naming the `gamemachines` / `controller` / `gui` packages.

## Quality Gate Assessment

| Component    | Typed | Convention | Training Data | Documented | Verdict          |
|--------------|-------|------------|---------------|------------|------------------|
| Language     | ✓     | —          | —             | —          | pass             |
| Framework    | —     | ~          | ✓             | ✓          | pass (with note) |
| Build tool   | —     | ✓          | ✓             | ✓          | pass             |
| Test runner  | —     | —          | —             | —          | absent           |

Legend: ✓ = pass, ✗ = fail, ~ = partial, — = not applicable

### Gate Details

**Language — type safety: ✓**
Java is statically typed at the language level; every `.java` file under `src/main/java/` is compile-time type-checked. This holds regardless of language level. *Resolved:* the level is now pinned to `maven.compiler.release=21`, so the build encodes which features (`var`, switch expressions, records, pattern matching, text blocks) are available — see Gaps below (now closed).

**Framework (Swing) — convention: ~ (partial)**
Swing itself is unopinionated about application structure — it dictates no folder layout, routing, or config conventions, only the component/EDT model. On its own this would fail the convention gate. It is scored **partial → pass** because the project documents its own conventions in `CLAUDE.md` ("Project Architecture": `gamemachines` = domain objects, `controller` = game loop/input, `gui` = Swing components). The one convention Swing *does* impose and that is load-bearing here — all rendering and timer-driven logic must run on the EDT — is named in the PRD but not yet in `CLAUDE.md`. See compensation.

**Framework (Swing) — training data: ✓**
Within the Java language family, Swing is among the most heavily represented UI technologies in training corpora: decades of Oracle tutorials, textbooks, and Stack Overflow answers. An agent generates idiomatic Swing (JPanel/paintComponent, JFrame, Timer, KeyListener) reliably.

**Framework (Swing) — documented: ✓**
Official, stable, version-pinned documentation exists: Oracle's "Creating a GUI With Swing" trail and the Java SE API Javadoc, both tied to the JDK release. Swing is in maintenance mode, so the docs are dated but accurate and unlikely to drift.

**Build tool (Maven) — convention / training / documented: ✓ ✓ ✓**
Maven enforces the standard directory layout, which this project follows (`src/main/java`, `src/main/resources` both present). It is the dominant Java build tool (deep training-data coverage) and has official, versioned documentation. *Caveat (not a gate failure):* the POM is minimal — no pinned plugin or compiler versions — which is a reproducibility gap, not a convention gap. See Gaps below.

**Test runner — absent**
No test component exists to score, so no ✓/✗ is marked against the gates. The absence itself is a real agent-workflow gap and is captured in Gaps & Compensation rather than as a failing gate on a nonexistent component.

## Gaps & Compensation

### Gap 1 — Java language level was unpinned (highest-impact) — ✅ RESOLVED

**Status:** Resolved by `build-tooling-baseline` — `pom.xml` now pins `maven.compiler.release=21` (with `project.build.sourceEncoding=UTF-8`), and `CLAUDE.md` restates the Java 21 level. **Original finding (at assessment time):** `pom.xml` declared no compiler configuration, so the target Java level was undefined in the build; the agent couldn't tell which language features were legal and the PRD guardrail ("`mvn clean compile` must pass at every stage") was at risk. The compensation — pin the level in `pom.xml` and restate it in `CLAUDE.md` — has been applied.

### Gap 2 — No automated test harness — ✅ RESOLVED

**Status:** Resolved by `build-tooling-baseline` — JUnit 5 was adopted as an explicit, signed-off decision (`org.junit.jupiter:junit-jupiter`, `test` scope; `maven-surefire-plugin` runs it; `SpaceshipTest` is the first test). The shipped runtime stays zero-dependency because the test dependency is not part of the artifact. **Original finding (at assessment time):** no `src/test/`, no test dependency; the only feedback loop was "does it compile" plus manual play-testing. The verification loop in `CLAUDE.md` is now **compile + test + run**; any *other* new external library still requires an explicit decision per the PRD.

### Gap 3 — Swing EDT convention not in the instruction file

**What:** `CLAUDE.md` documents the package layout but not the threading rule. **Why it matters for an agent:** the PRD requires all rendering and timer-driven game-loop logic to run on the EDT and forbids blocking the EDT (the current `Thread.sleep(1000)` loop is exactly the anti-pattern being removed). An agent without this rule in front of it may reintroduce a blocking loop or do Swing work off-thread. **Compensation:** add an explicit EDT-and-game-loop convention block to `CLAUDE.md`.

### Recommended Instruction File Additions

The following are paste-ready blocks for `CLAUDE.md`.

> *These blocks have been applied (in updated form) to `CLAUDE.md` by `build-tooling-baseline`. They are kept here as a record of the recommendation, reflecting the implemented `release=21` + JUnit 5 decision.*

```markdown
## Java Language Level
* Java 21 LTS, pinned via `maven.compiler.release=21` (the active local toolchain).
* `pom.xml` declares the level explicitly:
  ```xml
  <properties>
    <maven.compiler.release>21</maven.compiler.release>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
  </properties>
  ```
* Modern syntax is allowed — `var`, records, switch expressions, pattern matching, and text blocks are all available.
```

```markdown
## Verification Loop
* The verification path for any change is: `./mvnw clean compile` must pass, then `./mvnw test`, then run the
  game with `./mvnw exec:java -Dexec.mainClass="com.emenems.games.aliens.Main"` and confirm behavior by hand.
* `./mvnw clean compile` must pass at every stage of a change (PRD guardrail). Never leave the tree non-compiling.
* JUnit 5 (`test` scope) is the approved test harness — add tests under `src/test/java/`. Do NOT add any *other*
  new external library on your own — the PRD forbids new libraries (game engine, audio, JSON, Mockito/Spock, etc.)
  without an explicit decision. The shipped runtime stays zero-dependency.
```

```markdown
## Swing Threading & Game Loop (EDT)
* All rendering and all timer-driven game logic must run on the Swing Event Dispatch Thread (EDT).
* Drive the game loop with `javax.swing.Timer` (its callbacks fire on the EDT) — NOT with a background
  thread and `Thread.sleep`. The old `Thread.sleep(1000)` loop blocked the EDT and is being removed; do not
  reintroduce a blocking loop.
* Never perform long-running or blocking work inside an EDT callback.
* Keep game logic and input handling in `GameController` (central node — View/Controller refactor is out of scope per PRD).
```

## Summary

**Overall: ready-with-compensation.** The core of this stack is genuinely agent-friendly: Java is statically typed, Maven brings a conventional and well-trodden project layout, and Swing — while not an application framework — is one of the best-represented and most stably documented UI technologies in the Java ecosystem. An agent can navigate and extend this codebase with confidence.

**Key strengths:** compile-time type safety; standard Maven layout already in place; high training-data coverage for both Swing and Maven; a `CLAUDE.md` already exists and documents the package architecture.

**Key gaps identified at assessment time — status after `build-tooling-baseline` (2026-05-29):**
1. ✅ The Java language level is now pinned via `maven.compiler.release=21` in `pom.xml`, restated in `CLAUDE.md`.
2. ✅ Automated test harness adopted — JUnit 5 (`test` scope) + `maven-surefire-plugin`; verification loop in `CLAUDE.md` is now compile + test + run.
3. The EDT threading rule is documented in `CLAUDE.md` (Swing threading section) — guards against reintroducing a blocking game loop.

A GitHub Actions CI pipeline (`./mvnw clean compile` + `./mvnw test` on push/PR) now enforces the compile guardrail automatically.
