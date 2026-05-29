# Build Tooling Baseline — Plan Brief

> Full plan: `context/changes/build-tooling-baseline/plan.md`
> Decisions: `context/foundation/prd.md` (*Constraints & Compatibility*)
> Preliminary execution detail: `context/changes/build-tooling-baseline/build-tooling-plan.md`

## What & why

Turn the bare `pom.xml` into a fully-pinned, reproducible, CI-verified Maven build with a working JUnit 5 test harness and consistent docs. The current build silently targets whatever JDK is active, has no tests, no Maven wrapper, and no CI — so an agent can't tell which language features are legal and can't verify a change beyond "does it compile."

## Starting point

`pom.xml` is bare (groupId/artifactId/version only). No `src/test/`, no `mvnw`, no `.editorconfig`, no `.github/workflows/`. The active toolchain is **JDK 21.0.7** (Maven 3.9.10). Source uses only Java-8-safe syntax; `Spaceship` has pure movement methods ideal for a first test.

## Desired end state

`mvn`/`./mvnw` `clean compile`, `test`, and `exec:java` all succeed under JDK 21; one `SpaceshipTest` proves the JUnit 5 harness; a GitHub Actions workflow runs compile + test green on every push/PR; and no doc still asserts a stale build fact (Java 8, unpinned level, or release=25).

## Key decisions made

| Decision | Choice | Why | Source |
|---|---|---|---|
| Java compiler level | **`release=21`** | Active JDK is 21 (25 installed but not default); pinning 25 would fail the PRD's `mvn clean compile` guardrail. 21 is LTS and already provides every modern feature the PRD's rationale cited. | Plan (corrects PRD) |
| Test framework | JUnit 5, `test` scope | Shipped game stays zero-runtime-dependency; domain logic is pure, no mocking needed. | PRD |
| CI | GitHub Actions (temurin 21) | Remote exists; automates the compile + test guardrail. | PRD |
| Maven version | Pinned via `mvn wrapper:wrapper` | Reproducible build command across machines/CI. | Plan |
| Doc cascade scope | Includes `prd.md` + `build-tooling-plan.md` | Those assert 25; the 25→21 correction must propagate or docs contradict the build. | Plan |

## Scope

**In scope:** pinned `pom.xml` (release=21, encoding, JUnit 5, surefire, exec plugin), `SpaceshipTest`, Maven wrapper, `.editorconfig`, GitHub Actions workflow, doc-consistency cascade.

**Out of scope:** gameplay logic (FR-001…FR-010), View/Controller refactor, Mockito/Spock, packaging/distribution, `.sdkmanrc`/global-JDK change, CI lint/static-analysis stage.

## Architecture / approach

Five incremental, independently verifiable phases, each building on a green predecessor: pin `pom.xml` (compile + run still work) → prove the test harness → pin Maven via the wrapper → automate in CI → reconcile docs last (so docs describe a state that already exists). Plugin/JUnit versions are looked up fresh at execution time, not copied from the preliminary plan.

## Phases at a glance

| Phase | Delivers | Key risk |
|---|---|---|
| 1. pom.xml baseline | Pinned, reproducible build + JUnit 5 dep | `exec:java` must keep launching the game unchanged |
| 2. First test | `SpaceshipTest` green under JUnit 5 | Old surefire silently runs 0 tests — must confirm count >0 |
| 3. Wrapper + .editorconfig | `./mvnw` works; formatting locked | Wrapper pins Maven, not JDK (mitigated by release=21) |
| 4. CI | Actions runs compile + test on push/PR | First-run YAML/setup-java config errors |
| 5. Doc cascade | All docs match the real build | Wider than originally planned (also prd.md + build-tooling-plan.md) |

**Prerequisites:** Active JDK 21 (current default); push access to the GitHub remote (present).
**Estimated effort:** ~1 session, 5 small phases; mostly config + docs, one trivial test.

## Open risks & assumptions

- Assumes JDK 21 stays the active default; if the developer later switches to 25, `release=21` still compiles fine (21 ≤ 25), so no breakage — only a missed-features choice they can revisit.
- Exact current plugin/JUnit versions must be looked up at execution time; stale numbers in `build-tooling-plan.md` are not authoritative.

## Success criteria (summary)

- `./mvnw clean compile`, `./mvnw test`, and `mvn exec:java …` all succeed under JDK 21.
- GitHub Actions is green on push/PR.
- No documentation file contradicts another (or the build) on Java level, test runner, or CI.
