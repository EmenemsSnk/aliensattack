# Production Code Refactor — Brief Plan

> Full plan: `context/changes/refactor-plan/plan.md`
> Draft reviewed: `context/changes/refactor-plan/draft.md`

## What And Why

Refactor the Java 21 MVP codebase after the `v1.0.0` release to remove prototype-era vestiges and clarify core architecture without changing gameplay. The target is operationally boring: same behavior, cleaner contracts, fewer misleading APIs.

## Starting Point

The game is playable end-to-end and has 37 passing tests. The code still has dead placeholders, unused spaceship fields, `GamePanel` owning gameplay constants, constructor telescoping in `GameController`, and stale documentation that says those vestiges still exist.

## Desired End State

Dead code is gone, game-world constants live outside Swing UI, constructor APIs make shared-list wiring explicit, and `GameObject` is a sealed Java 21 hierarchy. Tests and manual gameplay confirm no regression from the MVP.

## Key Decisions

| Decision | Choice | Why |
| --- | --- | --- |
| Refactor scope | Production cleanup only | Preserves the MVP and avoids a View/Controller rewrite. |
| Constants owner | New `GameConstants` class | Controller/game rules should not depend on `GamePanel` for board dimensions. |
| Constructor API | One production constructor, one test constructor | Makes shared mutable lists explicit and removes hidden list creation. |
| Java 21 usage | `sealed GameObject` and switch input handling | Adds clarity where contracts benefit; avoids broad cosmetic modernizations. |
| Dependency policy | No new libraries | Matches zero-runtime-dependency project constraint. |

## Scope

**In scope:**

- Remove `Point.java`, dead spaceship state, deprecated `show()`, no-op `KeyAdapter` calls, unused constants/overloads.
- Extract board/entity constants to `GameConstants`.
- Collapse `GameController` and `GamePanel` compatibility APIs.
- Seal `GameObject`, name selected magic numbers, and update stale docs.

**Out of scope:**

- Gameplay tuning, new features, rendering redesign, audio changes, or performance optimization.
- View/Controller separation, DI framework, module conversion, Lombok, or other dependencies.
- Broad `var` rewrite or formatting-only churn outside touched files.

## Architecture / Approach

Use four small phases with compile/test gates after each: safe deletion, constants extraction, constructor/API collapse, then focused modernization plus docs. This keeps the highest-risk changes, especially world constants and constructors, isolated and easy to review.

## Phases In Brief

| Phase | Delivers | Key Risk |
| --- | --- | --- |
| 1. Safe Dead Code and Deprecated Call Cleanup | Removes obvious vestiges and deprecated/no-op calls | Accidentally deleting a test seam if usage is missed. |
| 2. Extract Game-World Constants from Swing Panel | `GameConstants` owns dimensions and size | Wide reference update across controller, panel, and tests. |
| 3. Collapse Constructors and Compatibility Shims | Explicit production/test construction only | Breaking deterministic tests or shared-list wiring. |
| 4. Focused Java 21 Modernization and Documentation | Sealed object hierarchy, clearer input state, updated docs | Sealed hierarchy must match actual implementors exactly. |

**Prerequisites:** Clean worktree or deliberate handling of the existing draft file; `./mvnw test` green before implementation.
**Estimated effort:** One to two focused sessions across four commits/phases.

## Open Risks And Assumptions

- The original draft is preserved as `context/changes/refactor-plan/draft.md`; implementation should not overwrite it unless intentionally marking it superseded.
- Manual verification is still required because rendering and Swing window behavior are not covered by automated tests.
- Constructor cleanup must preserve the shared mutable list contract documented in `CLAUDE.md`.

## Success Criteria Summary

- `./mvnw clean compile` and `./mvnw test` pass after every phase.
- Gameplay is visually and manually unchanged from v1.0.0.
- Future agents no longer see stale APIs, dead placeholders, or misleading architecture guidance.
