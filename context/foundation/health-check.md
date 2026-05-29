---
project: "Aliens Attack"
checked_at: 2026-05-28T20:55:00Z
health_status: needs-attention
context_type: brownfield
language_family: java
stack_assessment_available: true
checks_run:
  - lockfile
  - dependency_audit
  - outdated_deps
  - test_runner
  - ci_cd
  - configuration
audit_findings:
  critical: 0
  high: 0
  moderate: 0
  low: 0
test_runner_detected: false
ci_provider: null
recommended_fixes: 4
---

## Dependency Health

The project has **zero external dependencies** — `pom.xml` declares no `<dependencies>` block at all. The runtime surface is the JDK standard library plus `javax.swing`, both shipped with the JDK. This is the cleanest possible dependency posture: there is no third-party supply chain to pin, audit, or keep current.

### Lockfile

```
Status: not applicable — zero external dependencies declared in pom.xml
Package manager: Maven (Maven Central; no third-party repositories declared)
```

Maven has no first-class lockfile by default, and with zero declared dependencies there is nothing to pin. If external libraries are added later, consider the `maven-dependency-plugin` / a reproducible-build setup at that point. No action needed now.

### Security Audit

```
Tool: skipped — no built-in audit tool for java
Recommended external tool: OWASP Dependency-Check (org.owasp:dependency-check-maven) — only relevant once external dependencies are introduced
Summary: 0 CRITICAL, 0 HIGH, 0 MODERATE, 0 LOW
Direct vs transitive: not applicable — no dependencies to classify
```

With no third-party dependencies, the vulnerability surface from libraries is empty. The audit is effectively moot until the first dependency is added.

### Outdated Dependencies

```
Packages with major version gaps: 0 (no external dependencies)
```

Not applicable.

## Test Suite

```
Test runner: not detected
Tests found: not applicable (no src/test/ directory, no test dependency)
Test execution: not attempted
Build verification: mvn clean compile → BUILD SUCCESS (exit 0, 9 classes compiled on JDK 25)
```

⚠ No test runner detected. The agent cannot verify its own changes programmatically — the only feedback loop is "does it compile" plus manual play-testing.

Note the PRD context: the PRD's only verification guardrail is "`mvn clean compile` must pass at every stage," and it bars new external dependencies (JUnit included) "without an explicit decision." So the recommendation is staged: rely on the compile-plus-manual-run loop now, and treat adding JUnit as an explicit opt-in (see Recommended Fixes). The build itself is healthy — it compiles clean.

## CI/CD

```
Provider: not detected
Configuration: not found
```

| Stage      | Status | Notes            |
|------------|--------|------------------|
| Lint       | ✗      | not configured   |
| Test       | ✗      | not configured   |
| Build      | ✗      | not configured   |
| Type check | ✗      | not configured (Java is compile-time type-checked locally) |
| Security   | ✗      | not configured   |

ℹ No CI/CD configuration detected. You'll set this up in the infrastructure and deployment lesson. For now, the local `mvn clean compile` loop is sufficient for agent collaboration.

## Configuration

### High severity

- **`pom.xml` — Java compiler level is unpinned.** No `maven-compiler-plugin` config and no `maven.compiler.source`/`target`/`release` properties. The build currently succeeds because the locally installed JDK (25) supplies a default, but the *target language level is undefined in the build*. An agent doesn't know which language features are legal, and the build is non-reproducible across machines with different JDKs. Fix: pin the compiler level (see Recommended Fixes #2).

### Medium severity

- **Maven wrapper (`mvnw`, `mvnw.cmd`) — missing.** The build depends on whatever Maven the developer has installed locally (here 3.9.10). A committed wrapper pins the Maven version so the build is reproducible and the agent can run a known build command. Fix: `mvn wrapper:wrapper`.

### Low severity

- **`.editorconfig` — missing.** Without it, formatting (indentation, line endings, charset) can drift between the developer's editor and agent-generated code. Fix: add a minimal `.editorconfig` for Java.

### Present and correct

- **`.gitignore`** — present and well-formed; `/target/`, IDE files, and `/.claude/` are ignored. Confirmed `target/` is not tracked.
- **`CLAUDE.md`** — present (build/run commands, code style, project architecture).

## Stack Assessment Cross-Reference

```
Stack assessment: context/foundation/stack-assessment.md
Agent readiness (from stack-assess): ready-with-compensation
```

| Quality Gate Gap                          | Health-Check Finding                                                                 | Status      |
|-------------------------------------------|-------------------------------------------------------------------------------------|-------------|
| Unpinned Java language level              | Confirmed: zero compiler config in `pom.xml`; build relies on JDK 25 default         | Reinforced  |
| No automated test harness                 | Confirmed: no `src/test/`, no test dependency; only compile + manual verification    | Reinforced  |
| EDT threading convention not in CLAUDE.md | Not an operational finding — instruction-file content gap; carried forward as-is     | Carried     |

Both operational gaps confirm the stack assessment's top two compensation items. The unpinned compiler level is now doubly important: it is both an agent-friendliness gap (stack-assess) and a build-reproducibility gap (health-check), and there is no CI type-check or build stage to catch a regression. Fixing it in `pom.xml` closes both at once.

## Recommended Fixes

### Fix before agent work (Category A)

### 1. No automated test harness

**Impact**: The agent cannot programmatically verify that a change preserves behavior; verification is limited to compilation and manual play-testing.
**Severity**: medium (elevated for agent workflows, but the PRD explicitly defers automated tests and the build compiles clean)
**Effort**: quick (document the loop) / significant (if JUnit is later approved)
**Fix**:

Document the zero-dependency verification loop in `CLAUDE.md` as the expected path (per the stack-assessment's paste-ready block):

```
mvn clean compile   # must pass at every stage (PRD guardrail)
mvn exec:java -Dexec.mainClass="com.emenems.games.aliens.Main"   # then verify behavior by hand
```

Do **not** add JUnit unilaterally — the PRD bars new dependencies without an explicit decision. If you want automated tests, decide explicitly first, then add `org.junit.jupiter:junit-jupiter` and the `maven-surefire-plugin` to `pom.xml` and create `src/test/java/`.

### 2. Pin the Java compiler level in pom.xml

**Impact**: Removes the highest-impact ambiguity for the agent (which language features are legal) and makes the build reproducible across JDKs — directly protecting the PRD's "`mvn clean compile` must always pass" guardrail.
**Severity**: high
**Effort**: quick (< 5 min)
**Fix**:

Add to `pom.xml` (tie to the PRD's "Java 8+"):

```xml
<properties>
  <maven.compiler.source>1.8</maven.compiler.source>
  <maven.compiler.target>1.8</maven.compiler.target>
  <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
</properties>
```

Then restate the level in `CLAUDE.md` so the constraint is visible to the agent, not just the build.

### 3. Add a Maven wrapper

**Impact**: Pins the Maven version so the agent and any machine run an identical, known build — reproducibility insurance for a project with no CI.
**Severity**: medium
**Effort**: quick (< 5 min)
**Fix**:

```
mvn wrapper:wrapper
```

Commit the generated `mvnw`, `mvnw.cmd`, and `.mvn/` directory.

### 4. Add a minimal .editorconfig

**Impact**: Keeps agent-generated code formatting consistent with the existing source (indentation, charset, line endings).
**Severity**: low
**Effort**: quick (< 5 min)
**Fix**:

Create `.editorconfig`:

```ini
root = true

[*]
charset = utf-8
end_of_line = lf
insert_final_newline = true
trim_trailing_whitespace = true

[*.java]
indent_style = space
indent_size = 4
```

### Addressed in upcoming lessons (Category B)

### No CI/CD pipeline

**Lesson**: [Sprint Zero z Agentem: infrastruktura, walking skeleton i pierwszy deploy (M1L5)](https://platforma.przeprogramowani.pl/external/10xdevs-3/m1-l5)
**What you'll do there**: Stand up a pipeline (build + the compile check, and optionally a lint/test stage) so changes are verified automatically rather than only locally.

## Summary

```
Health status: needs-attention
```

This is an unusually clean brownfield project: zero external dependencies (so no security or supply-chain risk), a passing `mvn clean compile`, a correct `.gitignore`, and an existing `CLAUDE.md`. The gaps are about agent *enablement*, not breakage — chiefly the unpinned Java compiler level (a quick, high-value `pom.xml` fix that also closes a stack-assessment gap) and the absence of a test harness (real for agent workflows, but explicitly deferred by the PRD, so handled as a documented compile-plus-manual loop with JUnit as an opt-in). The Maven wrapper and `.editorconfig` are quick reproducibility/consistency wins.

Next step: apply Category A fixes #2–#4 (all quick) and document the verification loop from #1, then proceed to agent onboarding. CI is the only Category B item and is expected at this stage.
