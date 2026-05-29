# Build Tooling Baseline — Implementation Plan

## Overview

Turn the bare `pom.xml` (groupId/artifactId/version only) into a fully-pinned, reproducible, CI-verified Maven build with a working JUnit 5 test harness and an internally consistent documentation set. This executes the build/tooling decisions already locked in `context/foundation/prd.md` (*Constraints & Compatibility*) and drafted in `context/foundation/build-tooling-plan.md` — with **one correction**: the Java compiler level is pinned to **21**, not 25, because the active local toolchain is JDK 21.0.7 (JDK 25 is installed but not the default). Java 21 is LTS and already provides every modern language feature the PRD's rationale named (records, `var`, switch expressions, pattern matching, text blocks).

## Current State Analysis

- `pom.xml` declares only `modelVersion`, `groupId`, `artifactId`, `version`. No `<properties>`, no `<dependencies>`, no `<build>`. The Java level is unpinned → the build silently targets whatever JDK is active.
- No `src/test/` directory and no test dependency. The only verification loop today is "compile + run + observe by hand."
- No Maven wrapper (`mvnw`), no `.editorconfig`, no CI (`.github/workflows/`).
- Active toolchain: **JDK 21.0.7-oracle** on PATH (sdkman `current` → `21.0.7-oracle`), Maven 3.9.10. JDK 25.0.3-oracle is installed but is not the default.
- Source uses only Java-8-safe constructs (lambdas + streams). `Spaceship` movement methods are pure: `moveLeft/moveRight` shift x by ∓5, `moveUp/moveDown` shift y by ∓5; constructor stores x/y read back via `getX()`/`getY()` (`src/main/java/com/emenems/games/aliens/gamemachines/Spaceship.java:14-36`).
- Remote `git@…:EmenemsSnk/aliensattack.git` exists → GitHub Actions is viable.

## Desired End State

After this plan:
- `mvn clean compile`, `mvn test`, and `mvn exec:java -Dexec.mainClass="com.emenems.games.aliens.Main"` all succeed under JDK 21; the equivalents via `./mvnw` succeed identically.
- The build is reproducible: compiler level, encoding, plugin versions, the exec mainClass, and the Maven version are all pinned.
- A `SpaceshipTest` runs under JUnit 5 and passes, proving the harness works.
- A GitHub Actions workflow runs `clean compile` + `test` on every push/PR and is green.
- No documentation file asserts a build fact that contradicts reality (no stale "Java 8", "1.8", or "release=25" build claims).

Verification: the five-step "Verification" list at the bottom of `build-tooling-plan.md`, adapted to `release=21`.

### Key Findings

- `pom.xml` is a bare POM — `pom.xml:7-9` is the entire declared content (`<project>` body is otherwise empty). Everything is additive; nothing existing is removed.
- The `Spaceship` movement contract is exactly as the preliminary plan assumed — the harness-proving test needs no production code changes (`Spaceship.java:14-36`).
- **Active JDK is 21, not 25** (`java -version` → 21.0.7; `readlink ~/.sdkman/candidates/java/current` → `21.0.7-oracle`). This is why `release` is pinned to 21 — pinning 25 would fail the PRD's hard `mvn clean compile` guardrail.
- The Maven wrapper pins **Maven**, not the **JDK** — `./mvnw` still uses whatever `JAVA_HOME`/PATH `java` is active. Pinning the build to 21 (the active default) is what removes the toolchain foot-gun; no `.sdkmanrc` is needed for the 21 path.
- CI is independent of the local default — `actions/setup-java` provisions its own JDK (temurin 21).

## What we are NOT doing

- No gameplay logic (FR-001…FR-010) — that is the separate gameplay-implementation change.
- No View/Controller refactor — `GameController` stays the central node (PRD constraint).
- No Mockito/Spock or any non-test runtime dependency.
- No packaging/distribution (JAR, jpackage, fat-jar).
- No `.sdkmanrc` / global-JDK change — pinning `release=21` to match the active default makes that unnecessary.
- No CI lint/static-analysis stage beyond compile + test (no analyzer is in scope).

## Implementation Approach

Five incremental, independently verifiable phases, ordered so each builds on a green predecessor: establish the pinned `pom.xml` first (compile + run still work), prove the test harness, pin the toolchain via the wrapper, automate verification in CI, then reconcile the docs to the new reality last (so the docs describe a state that already exists). Versions for plugins and JUnit are pinned to the exact current releases looked up at execution time — not copied from any stale number in the preliminary plan.

## Critical Implementation Details

- **Surefire ↔ JUnit 5 coupling**: JUnit Jupiter is only auto-detected by `maven-surefire-plugin` **3.x**. If an older surefire is resolved, tests are silently not run. Pin surefire 3.x explicitly and confirm the test count is non-zero in Phase 2 output (do not treat "BUILD SUCCESS, 0 tests" as a pass).
- **`exec:java` must keep working unchanged**: the CLAUDE.md run command (`mvn exec:java -Dexec.mainClass="com.emenems.games.aliens.Main"`) must still launch the game after the plugin is pinned. Pinning `exec-maven-plugin` with a configured `<mainClass>` is additive — the `-Dexec.mainClass=…` override must continue to resolve identically.

---

## Phase 1: `pom.xml` baseline

### Overview

Make the build pinned and reproducible, and add the JUnit 5 test-scope dependency, without changing any runtime behavior.

### Changes Required:

#### 1. Pinned build configuration

**File**: `pom.xml`

**Purpose**: Pin the compiler level so generated/modified code stays within a known feature set and the build is reproducible across machines; declare encoding; add the JUnit 5 test dependency; pin the plugins the project relies on so `mvn test` and `mvn exec:java` are reproducible rather than resolved via default plugin groups.

**Contract**:
- `<properties>`: `maven.compiler.release=21` (single source of truth — use `release`, not the legacy `source`/`target` pair) and `project.build.sourceEncoding=UTF-8`.
- `<dependencies>`: `org.junit.jupiter:junit-jupiter` with `<scope>test</scope>`, version pinned to the current 5.x release.
- `<build><plugins>`: `maven-surefire-plugin` (current 3.x, runs JUnit 5) and `exec-maven-plugin` (current 3.x) configured with `<mainClass>com.emenems.games.aliens.Main</mainClass>`.
- Look up exact current versions at execution time; do not copy version numbers from `build-tooling-plan.md`.

### Success Criteria:

#### Automated Verification:

- [ ] Build compiles: `mvn clean compile`
- [ ] Build resolves all declared dependencies/plugins offline-or-online without error: `mvn -q dependency:resolve`

#### Manual Verification:

- [ ] Game still launches and is controllable: `mvn exec:java -Dexec.mainClass="com.emenems.games.aliens.Main"`
- [ ] No behavior change versus before (movement, firing, rendering unchanged)

**Implementation note**: After this phase passes automated verification, stop for human confirmation that the game still launches before proceeding to Phase 2.

---

## Phase 2: First test (harness proof)

### Overview

Add one deliberately thin test that proves JUnit 5 runs end-to-end. No production code changes.

### Changes Required:

#### 1. SpaceshipTest

**File**: `src/test/java/com/emenems/games/aliens/gamemachines/SpaceshipTest.java`

**Purpose**: Prove the JUnit 5 + surefire harness executes and that the standard Maven test layout is wired. Targets `Spaceship`'s pure movement methods, the only domain logic extractable without touching EDT-coupled `GameController`.

**Contract**: JUnit 5 (`org.junit.jupiter.api`) test class asserting against `Spaceship`:
- `moveLeft()` → `getX()` decreases by 5; `moveRight()` → `getX()` increases by 5.
- `moveUp()` → `getY()` decreases by 5; `moveDown()` → `getY()` increases by 5.
- Constructor args round-trip: `new Spaceship(x, y)` → `getX()==x`, `getY()==y`.

This is intentionally thin — it proves the harness, nothing more. Substantive tests (collision FR-004, scoring FR-007, wave progression FR-010, lives FR-003) ride along with the gameplay change and only to the extent formulas are extractable into pure methods without triggering the out-of-scope View/Controller refactor.

### Success Criteria:

#### Automated Verification:

- [ ] Tests run and pass with a non-zero test count: `mvn test`
- [ ] Surefire actually executed Jupiter (confirm "Tests run: N" with N>0 in output, not "0 tests")

---

## Phase 3: Maven wrapper + `.editorconfig`

### Overview

Pin the Maven version (reproducibility insurance for a project with no enforced Maven version) and lock editor formatting to match existing source.

### Changes Required:

#### 1. Maven wrapper

**File**: `mvnw`, `mvnw.cmd`, `.mvn/wrapper/` (generated)

**Purpose**: Pin the Maven version so the agent, CI, and any machine run an identical build invocation.

**Contract**: Run `mvn wrapper:wrapper`; commit the generated `mvnw`, `mvnw.cmd`, and `.mvn/` directory. Wrapper pins Maven only — it does not pin the JDK (the build already targets 21 via `release`).

#### 2. EditorConfig

**File**: `.editorconfig`

**Purpose**: Keep agent-generated formatting consistent with existing source (4-space Java indent, UTF-8, LF).

**Contract**: `root = true`; `[*]` → `charset=utf-8`, `end_of_line=lf`, `insert_final_newline=true`, `trim_trailing_whitespace=true`; `[*.java]` → `indent_style=space`, `indent_size=4`.

### Success Criteria:

#### Automated Verification:

- [ ] Wrapper builds: `./mvnw clean compile`
- [ ] Wrapper runs tests: `./mvnw test`
- [ ] Wrapper files are tracked: `git status` shows `mvnw`, `mvnw.cmd`, `.mvn/`, `.editorconfig` staged/committed

---

## Phase 4: CI workflow

### Overview

Automate the PRD's `mvn clean compile` guardrail (plus tests) on every push and PR via GitHub Actions.

### Changes Required:

#### 1. Build workflow

**File**: `.github/workflows/build.yml`

**Purpose**: Run the build and tests automatically so regressions are caught without relying on local runs.

**Contract**: Triggers on `push` and `pull_request`. Steps: `actions/checkout` → `actions/setup-java` (`distribution: temurin`, `java-version: 21` — matching the pinned `release`) → `./mvnw clean compile` → `./mvnw test`. Use current major versions of the actions at execution time.

### Success Criteria:

#### Automated Verification:

- [ ] Workflow file is valid YAML and present at `.github/workflows/build.yml`

#### Manual Verification:

- [ ] After pushing the branch, the GitHub Actions run goes green (compile + test both pass on temurin 21)

**Implementation note**: After this phase, stop for human confirmation that the Actions run is green before proceeding to Phase 5.

---

## Phase 5: Doc-consistency cascade

### Overview

Reconcile every doc that asserts a now-stale build fact. "Java 8 / no new deps / unpinned level / Java 25" is repeated across several files; updating one without the others creates contradictions. Because the level landed on **21** (not the 25 the upstream docs recorded), this cascade is wider than `build-tooling-plan.md` Step 5 anticipated — it also corrects `prd.md` and `build-tooling-plan.md` themselves.

### Changes Required:

#### 1. CLAUDE.md

**File**: `CLAUDE.md`

**Purpose**: Make the instruction file describe the real build.

**Contract**: "Write **Java 8** syntax — avoid `var`, records, switch expressions … until the level is pinned" → Java **21** (LTS), modern syntax allowed. "The Java compiler level is **not pinned**…" → pinned via `maven.compiler.release=21`. Build/verify commands → `./mvnw clean compile` + add `./mvnw test`. The "Do **not** add JUnit" rule → JUnit 5 is now an approved **test-scope** dependency; the no-new-runtime-dependency rule still governs all other libraries. Leave the existing Swing-EDT section intact.

#### 2. PRD

**File**: `context/foundation/prd.md`

**Purpose**: Correct the locked decision from 25 to 21 with rationale, so the PRD matches the implemented build.

**Contract**: In *Current System Overview* and *Constraints & Compatibility*, change "Java 25 LTS" / "`maven.compiler.release=25`" → "Java 21 LTS" / "`maven.compiler.release=21`". Update the rationale parenthetical to note 21 is the active toolchain and already provides the modern syntax cited. Keep the JUnit 5 / GitHub Actions / pinned-pom statements (those are unchanged).

#### 3. Build tooling plan

**File**: `context/foundation/build-tooling-plan.md`

**Purpose**: Align the preliminary execution doc with what was actually done and record the 25→21 correction.

**Contract**: Update the `decisions.java_level` frontmatter and the "Decision recap" table / Step 1 / Step 4 / Verification entries from `release=25` / java-version 25 → `release=21` / java-version 21. Note the active-JDK finding as the reason. Mark status as executed/superseded by `context/changes/build-tooling-baseline/plan.md`.

#### 4. Stack assessment

**File**: `context/foundation/stack-assessment.md`

**Purpose**: Resolve the recorded gaps.

**Contract**: Gap 1 (unpinned level) → resolved (`release 21`); paste-ready block showing `1.8` → `release 21`; Gap 2 / test-runner → JUnit 5 adopted; frontmatter `test_runner: null → JUnit 5`, `ci_provider: null → GitHub Actions`.

#### 5. Health check

**File**: `context/foundation/health-check.md`

**Purpose**: Resolve the recorded fixes.

**Contract**: Fix #2 (`1.8` → `release 21`); Fix #1 → tests decided (JUnit 5 added); Fix #3 (wrapper) / #4 (.editorconfig) → done; CI section + frontmatter (`ci_provider`, `test_runner_detected`) → GitHub Actions configured / true.

### Success Criteria:

#### Automated Verification:

- [ ] No stale build-level Java-8 / 1.8 assertions remain: `grep -rn "Java 8\|1\.8" CLAUDE.md context/foundation/` returns nothing referring to the compiler level
- [ ] No stale `release=25` / "Java 25" build claims remain: `grep -rn "release=25\|release 25\|Java 25\|java-version: 25" CLAUDE.md context/foundation/` returns nothing
- [ ] Full build still green after doc edits: `./mvnw clean compile && ./mvnw test`

#### Manual Verification:

- [ ] Read-through confirms no doc contradicts another on Java level, test runner, or CI

---

## Testing Strategy

### Unit tests

- `SpaceshipTest` — pure movement deltas (±5) and constructor round-trip. The single purpose is proving the harness runs; correctness of `Spaceship` is incidental.

### Manual testing steps

1. `mvn clean compile` → BUILD SUCCESS under JDK 21.
2. `mvn exec:java -Dexec.mainClass="com.emenems.games.aliens.Main"` → game launches and responds to arrow keys + space.
3. `mvn test` → `Tests run: N` with N>0, all passing.
4. `./mvnw clean compile && ./mvnw test` → identical results via the wrapper.
5. Push branch → GitHub Actions green.
6. `grep -rn "Java 8\|1\.8\|release=25\|Java 25" CLAUDE.md context/foundation/` → no stale build assertions.

## Performance Considerations

None — build tooling only; no runtime code path changes.

## Migration Notes

None — no persistent state, no external contracts. Existing developers gain `./mvnw` as an alternative to `mvn`; the bare `mvn` commands continue to work under JDK 21.

## References

- Decisions: `context/foundation/prd.md` (*Constraints & Compatibility*)
- Preliminary execution detail: `context/foundation/build-tooling-plan.md`
- Gaps confirmed: `context/foundation/stack-assessment.md`, `context/foundation/health-check.md`
- Test target: `src/main/java/com/emenems/games/aliens/gamemachines/Spaceship.java:14-36`
- Bare POM: `pom.xml:7-9`

## Progress

> Convention: `- [ ]` pending, `- [x]` done. Append ` — <commit sha>` when a step lands. Do not rename step titles. See `references/progress-format.md`.

### Phase 1: pom.xml baseline

#### Automated

- [x] 1.1 Build compiles: `mvn clean compile` — 3dd53ed
- [x] 1.2 Dependencies/plugins resolve: `mvn -q dependency:resolve` — 3dd53ed

#### Manual

- [x] 1.3 Game still launches via `mvn exec:java` — 3dd53ed
- [x] 1.4 No behavior change versus before — 3dd53ed

### Phase 2: First test (harness proof)

#### Automated

- [x] 2.1 Tests run and pass with non-zero count: `mvn test`
- [x] 2.2 Surefire executed Jupiter ("Tests run: N", N>0)

### Phase 3: Maven wrapper + .editorconfig

#### Automated

- [ ] 3.1 Wrapper builds: `./mvnw clean compile`
- [ ] 3.2 Wrapper runs tests: `./mvnw test`
- [ ] 3.3 Wrapper + .editorconfig files tracked in git

### Phase 4: CI workflow

#### Automated

- [ ] 4.1 `.github/workflows/build.yml` present and valid YAML

#### Manual

- [ ] 4.2 GitHub Actions run goes green on temurin 21

### Phase 5: Doc-consistency cascade

#### Automated

- [ ] 5.1 No stale Java-8 / 1.8 compiler assertions: `grep -rn "Java 8\|1\.8" CLAUDE.md context/foundation/`
- [ ] 5.2 No stale release=25 / Java 25 claims: `grep -rn "release=25\|release 25\|Java 25\|java-version: 25" CLAUDE.md context/foundation/`
- [ ] 5.3 Full build green after doc edits: `./mvnw clean compile && ./mvnw test`

#### Manual

- [ ] 5.4 Read-through confirms no doc contradicts another
