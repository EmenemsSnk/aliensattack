---
project: "Aliens Attack"
assessed_at: 2026-05-28T20:49:20Z
agent_readiness: ready-with-compensation
context_type: brownfield
stack_components:
  language: Java
  framework: Java Swing (javax.swing)
  build_tool: Maven
  test_runner: null
  package_manager: Maven Central
  ci_provider: null
  deployment_target: null
gates_passed: 8
gates_failed: 1
---

## Stack Components

- **Language — Java.** Statically typed by the language; the compiler enforces input/output shapes at build time, so an agent can reason about types from source without running the program. The exact language level is **not pinned** — `pom.xml` declares no `maven-compiler-plugin` and no `maven.compiler.source`/`target` (or `release`) properties. The PRD targets "Java 8+", but nothing in the build encodes that.
- **Framework — Java Swing (`javax.swing`).** JDK standard-library GUI toolkit, no version of its own (ships with the JDK). Confirmed by `javax.swing` usage in `src/main/java/com/emenems/games/aliens/gui/GamePanel.java` and `WindowFrame.java`. Swing is a UI toolkit, not an application framework — it imposes a component/container/event model and the Event Dispatch Thread (EDT) threading rule, but no opinion on application architecture, packaging, or file layout.
- **Build tool — Maven.** `pom.xml` present (`com.emenems.games.aliens:aliens-attack:1.0-SNAPSHOT`). The POM is minimal: groupId/artifactId/version only, no `<build>`, no plugin management, no `<properties>`. No Maven wrapper (`mvnw`) is committed, so the build relies on the developer's locally installed Maven version.
- **Test runner — not detected.** No `src/test/` directory, no JUnit/TestNG dependency in `pom.xml`. There is no automated verification harness in the repo today.
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
Java is statically typed at the language level; every `.java` file under `src/main/java/` is compile-time type-checked. This holds regardless of language level. *Caveat (not a type-safety failure):* the language level is unpinned in `pom.xml`, so an agent doesn't know from the build whether `var`, lambdas, switch expressions, or records are available — see Gaps below.

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

### Gap 1 — Java language level is unpinned (highest-impact)

**What:** `pom.xml` declares no compiler configuration, so the target Java level is undefined in the build. **Why it matters for an agent:** when generating or modifying code, the agent can't tell which language features are legal — it may emit `record`, `var`, or newer switch syntax that won't compile under the developer's actual JDK, or it may stay needlessly conservative. The PRD's guardrail ("`mvn clean compile` must pass at every stage") makes this directly relevant: an unpinned level invites compile breaks. **Compensation:** pin the compiler level in `pom.xml` and restate it in `CLAUDE.md` so the constraint is visible both to the build and to the agent. Tie it to the PRD's "Java 8+".

### Gap 2 — No automated test harness

**What:** no `src/test/`, no test dependency. **Why it matters for an agent:** the agent has no programmatic way to verify a change preserves behavior; the only feedback loop is "does it compile" plus manual play-testing. **Compensation, constraint-aware:** the PRD bars new external dependencies "without an explicit decision," and JUnit *is* a new dependency — so the primary compensation is the **zero-dependency verification loop** (compile + manual run), documented in `CLAUDE.md` so the agent knows that is the expected verification path. Adding JUnit is offered only as an **optional** step that requires the developer's explicit sign-off, not as a default recommendation.

### Gap 3 — Swing EDT convention not in the instruction file

**What:** `CLAUDE.md` documents the package layout but not the threading rule. **Why it matters for an agent:** the PRD requires all rendering and timer-driven game-loop logic to run on the EDT and forbids blocking the EDT (the current `Thread.sleep(1000)` loop is exactly the anti-pattern being removed). An agent without this rule in front of it may reintroduce a blocking loop or do Swing work off-thread. **Compensation:** add an explicit EDT-and-game-loop convention block to `CLAUDE.md`.

### Recommended Instruction File Additions

The following are paste-ready blocks for `CLAUDE.md`.

```markdown
## Java Language Level
* Target Java 8+ (per PRD). The build must pin this so generated code stays within the supported feature set.
* `pom.xml` must declare the compiler level explicitly:
  ```xml
  <properties>
    <maven.compiler.source>1.8</maven.compiler.source>
    <maven.compiler.target>1.8</maven.compiler.target>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
  </properties>
  ```
* Do not use language features newer than the pinned level (e.g. no `record`, no `var`, no switch expressions if pinned to 1.8). When in doubt, prefer Java 8 syntax.
```

```markdown
## Verification Loop (no new test dependency without sign-off)
* The verification path for any change is: `mvn clean compile` must pass, then run the game with
  `mvn exec:java -Dexec.mainClass="com.emenems.games.aliens.Main"` and confirm the affected behavior by hand.
* `mvn clean compile` must pass at every stage of a change (PRD guardrail). Never leave the tree non-compiling.
* Do NOT add JUnit, TestNG, or any new dependency on your own — the PRD forbids new external libraries
  without an explicit decision. If automated tests are wanted, raise it first and get sign-off before
  editing `pom.xml`.
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

**Key gaps, all compensable via instruction-file edits (no stack change needed):**
1. The Java language level is unpinned — pin it in `pom.xml` and `CLAUDE.md` (highest impact for keeping `mvn clean compile` green).
2. No automated test harness — document the compile-plus-manual-run verification loop; treat JUnit as an opt-in needing sign-off, per the PRD's no-new-dependencies rule.
3. The EDT threading rule isn't in `CLAUDE.md` — add it so the agent doesn't reintroduce a blocking game loop.

**Recommended next step:** run `/10x-health-check` to dig into code-level health (the unpinned config, the EDT loop, and the missing-cleanup memory issue called out in the PRD are natural focus areas).
