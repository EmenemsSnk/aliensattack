---
project: "Aliens Attack"
created: 2026-05-29
status: planned
type: build-tooling-plan
relates_to:
  - context/foundation/prd.md            # decisions recorded here originate
  - context/foundation/stack-assessment.md
  - context/foundation/health-check.md
decisions:
  java_level: "Java 25 LTS (maven.compiler.release=25)"
  tests: "JUnit 5 only, test scope"
  ci: "GitHub Actions"
---

# Build & Tooling Plan — preliminary (execution detail)

This is the **"how"** behind the build/tooling decisions recorded in `prd.md` *Constraints & Compatibility* (which closed the former Open Questions Q3–Q5 and the missing-CI gap). The PRD holds the **decisions**; this file holds the **execution steps** so a later roadmap / implementation-plan pass can pick them up. **Nothing here is applied yet** — it is planned work.

## Decision recap (from PRD)

| Item | Decision | Rationale |
|---|---|---|
| Java level | **Java 25 LTS**, pinned via `maven.compiler.release=25` | Local JDK is 25.0.3 LTS; game is local / single-user / no-distribution → zero portability cost; unlocks modern syntax. Existing source (lambdas + streams only) is unaffected. |
| Tests | **JUnit 5 only** (Jupiter + surefire), `test` scope | Shipped game stays zero-runtime-dependency; pure domain logic is the target. Mockito/Spock rejected for MVP — logic is pure, no mocking needed. |
| CI | **GitHub Actions** | Remote exists (`github.com/EmenemsSnk/aliensattack`); automates the PRD `mvn clean compile` guardrail. |

Verified context: JDK 25.0.3 can target `--release` 8–25; current source uses only Java-8-safe constructs; 10 files / ~403 LOC; `Point.java` is commented out; `Spaceship` movement methods are pure.

---

## Step 1 — `pom.xml` (currently bare: groupId/artifactId/version only)

Add:

- `<properties>`:
  - `<maven.compiler.release>25</maven.compiler.release>` — **single source of truth** (use `release`, NOT the legacy `source`/`target` pair).
  - `<project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>`.
- `<dependencies>`: `org.junit.jupiter:junit-jupiter` with `<scope>test</scope>`.
- `<build><plugins>`:
  - `maven-surefire-plugin` — runs JUnit 5.
  - `exec-maven-plugin` — **pinned explicitly** with `<mainClass>com.emenems.games.aliens.Main</mainClass>`, so `mvn exec:java` is reproducible (today it resolves via default plugin groups, unpinned). Keep the run command in CLAUDE.md working unchanged.

**Versions:** pin exact *current* releases at execution time (≈ JUnit Jupiter 5.x, surefire 3.x, exec-maven-plugin 3.x). Verify against latest — do not copy stale numbers.

## Step 2 — Maven wrapper + `.editorconfig`

- `mvn wrapper:wrapper` → commit `mvnw`, `mvnw.cmd`, `.mvn/`. (Pins Maven version; agent/CI run an identical build.)
- `.editorconfig`: `charset=utf-8`, `end_of_line=lf`, `insert_final_newline=true`, `trim_trailing_whitespace=true`; `[*.java]` → `indent_style=space`, `indent_size=4` (matches existing source).

## Step 3 — First test

`src/test/java/com/emenems/games/aliens/gamemachines/SpaceshipTest.java`:

- `moveLeft` → x decreases by 5; `moveRight` → x +5; `moveUp` → y −5; `moveDown` → y +5.
- Constructor args round-trip through `getX()` / `getY()`.

**Honest scope:** this is deliberately thin — it proves the harness runs, nothing more. The substantive tests (collision FR-004, scoring FR-007, wave progression FR-010, lives FR-003) target logic that **is not written yet** and lives in EDT-coupled `GameController`. Those tests ride along with the gameplay-implementation task, and only to the extent the formulas are extractable into **pure** methods — extraction must not trigger the out-of-scope View/Controller refactor (PRD constraint).

## Step 4 — CI: `.github/workflows/build.yml`

- Trigger: `push` + `pull_request`.
- Steps: `actions/checkout` → `actions/setup-java` (distribution `temurin`, java-version **25**, matching the pinned level) → `./mvnw clean compile` → `./mvnw test`.

## Step 5 — Doc-consistency cascade (do NOT skip when executing)

"Java 8" / "no new deps" is asserted in multiple docs; updating one without the others creates contradictions:

- **`CLAUDE.md`**: "Write **Java 8** syntax — avoid `var`, records, switch expressions … until the level is pinned" → Java 25 LTS, modern syntax allowed. "The Java compiler level is **not pinned**…" → pinned via `maven.compiler.release=25`. Build/verify commands → `./mvnw` + add `./mvnw test`. "Do **not** add JUnit" rule → JUnit 5 is an approved **test-scope** dependency; the no-new-dependency rule still governs all *other* libraries. (The "Swing threading rule" section already exists — leave it.)
- **`context/foundation/stack-assessment.md`**: Gap 1 → resolved (`release 25`); paste-ready block `1.8` → `release 25`; Gap 2 / test-runner → JUnit 5 adopted; frontmatter `test_runner: null → JUnit 5`, `ci_provider: null → GitHub Actions`.
- **`context/foundation/health-check.md`**: Fix #2 `1.8` → `release 25`; Fix #1 → tests decided (JUnit 5 added); Fix #3 (wrapper) / #4 (.editorconfig) → done; CI section + frontmatter (`ci_provider`, `test_runner_detected`) → GitHub Actions configured.
- After edits: `grep -rn "Java 8\|1\.8" CLAUDE.md context/foundation/` → expect no stale assertions.

## Verification (when this plan is executed)

1. `./mvnw clean compile` → BUILD SUCCESS (proves `release=25` + wrapper).
2. `./mvnw test` → `SpaceshipTest` passes (proves JUnit 5 wired).
3. `./mvnw exec:java -Dexec.mainClass="com.emenems.games.aliens.Main"` → game still launches.
4. Push on a branch → GitHub Actions goes green (compile + test).
5. Grep docs for "Java 8" / "1.8" → none remain.

## Out of scope

- Gameplay logic (FR-001…FR-010); View/Controller refactor; Mockito/Spock; packaging/distribution (JAR/jpackage).
