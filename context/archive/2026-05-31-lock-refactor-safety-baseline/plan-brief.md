# Lock Refactor Safety Baseline - Brief Plan

> Full plan: `context/changes/lock-refactor-safety-baseline/plan.md`

## What And Why

Lock a repeatable safety baseline before the Refactor Pack starts moving rules and session state out of `GameController`. The goal is not to refactor production logic yet; it is to make "safe to refactor" concrete through evidence, focused tests, and a short manual smoke checklist.

## Starting Point

The current baseline is green: on 2026-05-31, `./mvnw clean compile` succeeded and `./mvnw test` passed 37 tests across 3 test classes. CI already runs compile and test, but the PRD warns that green tests must be supplemented with manual smoke verification.

## Desired End State

The change folder contains a `baseline.md` artifact that future refactor slices can use as a contract. A few focused controller tests lock reset/held-input and active-wave scoring behavior, and the handoff clearly says future slices must run `./mvnw clean compile` plus `./mvnw test` before and after refactoring.

## Key Decisions

| Decision | Choice | Why |
| --- | --- | --- |
| Baseline scope | Tests + checklist + evidence | Gives future refactors an auditable contract without starting extraction work. |
| Artifact location | Change-scoped `baseline.md` | Keeps S-01 self-contained and archiveable. |
| Test additions | Focused gap-fill tests | Protects likely `GameRules`/`GameSession` extraction risks without brittle broad scenarios. |
| Manual verification | Short repeatable checklist | Matches the PRD warning that tests alone are not enough. |
| Automated gate | `./mvnw clean compile` + `./mvnw test` | Matches existing docs and CI. |
| Evidence style | Dated command results in `baseline.md` | Makes the locked baseline easy to audit later. |
| Refactor guardrail | No production behavior refactor | Keeps S-01 independent from the refactor it protects. |

## Scope

**In scope:**

- Create `context/changes/lock-refactor-safety-baseline/baseline.md`.
- Record current baseline evidence and final post-implementation command results.
- Add focused `GameControllerTest` regression coverage for reset/held-fire and active-wave scoring.
- Document a short manual smoke checklist.

**Out of scope:**

- Extracting `GameRules` or `GameSession`.
- Production gameplay logic changes.
- GUI automation, new dependencies, CI redesign, or broad cleanup.

## Architecture / Approach

Keep the baseline as a test/documentation slice. The implementation touches the change folder and controller tests, runs the existing Maven gate, and records enough evidence that S-02/S-03 can proceed with a known regression contract.

## Phases In Brief

| Phase | Delivers | Key Risk |
| --- | --- | --- |
| 1. Baseline Evidence Artifact | `baseline.md` with gate, evidence, and manual checklist | Artifact becomes too vague to guide future refactors. |
| 2. Focused Regression Tests | Narrow controller tests for extraction-sensitive behavior | Tests accidentally lock private implementation shape. |
| 3. Verification Gate and Handoff | Final compile/test evidence and clear future-refactor guardrail | Someone starts extraction work before the baseline is locked. |

**Prerequisites:** Existing green compile/test baseline; no production refactor in progress.
**Estimated effort:** One focused implementation session across three small phases.

## Open Risks And Assumptions

- Manual smoke verification still requires a human with a display; CI cannot prove Swing gameplay feel.
- The baseline artifact is change-scoped, so future agents should look at the archived S-01 folder after completion.
- The implementation should not change production logic; if a test exposes a real bug, that should become a separate change unless it blocks locking the baseline.

## Success Criteria Summary

- `baseline.md` records initial and final command evidence, including the final test count.
- `./mvnw clean compile` and `./mvnw test` pass after the focused tests are added.
- S-01 ends with no production gameplay refactor and a clear handoff for future Refactor Pack slices.
