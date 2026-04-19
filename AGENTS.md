# MediAsk Backend Agent Guide

## Attention

You can't use `lark-cli` in `sandbox`!

## Repository Snapshot
- Stack: Java 21, Spring Boot 3.5.x, Maven multi-module build.
- Architecture target: DDD + hexagonal modular monolith.

## Read

### Must read

For non-trivial work, read these first:
1. `docs/docs/00A-P0-BASELINE.md`
2. `docs/playbooks/00B-P0-DEVELOPMENT-CHECKLIST.md`

For backend changes, also read:
3. `docs/playbooks/00C-P0-BACKEND-TASKS.md`
4. `docs/playbooks/00E-P0-BACKEND-ORDER-AND-DTOS.md`
5. `docs/docs/01-OVERVIEW.md`
6. `docs/docs/02-CODE_STANDARDS.md`
7. `docs/docs/06-DDD_DESIGN.md`
8. `docs/docs/19-ERROR_EXCEPTION_RESPONSE_DESIGN.md`

### when you start a code review

`docs/playbooks/AI-CODE-REVIEW-CHECKLIST.md`

### when you begin to test
`docs/docs/05-TESTING.md`

Additional Maven testing note:

- This repo is a Maven multi-module build. When running tests for a specific module from the repo root, prefer `mvn -pl <module> -am ...` so Maven also builds required local reactor dependencies.
- Do not assume `mvn -pl <module> ...` alone will work. It can fail with `Could not find artifact me.jianwen:mediask-...:1.0-SNAPSHOT` when dependent local modules have not been built into the local repository.
- Recommended pattern:
  - `mvn -pl mediask-api -am -Dtest=SomeTest test`
  - add `-Dsurefire.failIfNoSpecifiedTests=false` when targeting a narrow test set across the reactor and upstream modules have no matching tests.
- `mediask-infra` tests should not assume Mockito is available. Prefer manual stubs, lightweight test doubles, or JDK dynamic proxies unless the module already has the needed test dependency.
- Do not add test dependencies just to simplify a small repository/adapter test. Stay within the repo’s existing dependency set.

### when changing AI APIs or SSE.
`docs/docs/10A-JAVA_AI_API_CONTRACT.md` 

## Architecture Rules
Dependency direction:
```text
mediask-api -> mediask-application -> mediask-domain
mediask-api -> mediask-infra -> mediask-domain
mediask-worker -> mediask-application
mediask-worker -> mediask-infra
all modules -> mediask-common
```

## Hard rules:
- `mediask-domain` stays **framework-free**: no Spring, MyBatis, Redis, servlet APIs, or infrastructure code.
- `mediask-application` orchestrates use cases and transactions; domain rules belong in aggregates or domain services.
- `mediask-infra` implements ports defined by the domain.
- `mediask-api` and `mediask-worker` are entry-point adapters; controllers and jobs call application services, not repositories.
- Cross-context communication uses IDs, ports, and domain events, not direct aggregate references.
- **AI must preserve module purity: do not add imports or dependencies that violate the dependency direction, even if they seem convenient.**

## Package and Naming Conventions
- Organize by bounded context first, then by layer.
- API: `*Controller`, `*Request`, `*Response`, `*VO`, `*Assembler`
- Application: `*UseCase`, `*Command`, `*Query`
- Domain: entities use business nouns, value objects often use `*Id`, `*Status`, `*Type`, domain services use `*DomainService`, ports use `*Repository` or `*Port`, events use past-tense `*Event`
- Infra: `*RepositoryImpl`, `*DO`, `*Mapper`, `*Converter`, `*Client`
- Common: `*Exception`, `ErrorCode`, constants, utility classes
- Enums should not use an `Enum` suffix.

## Java Style
- Prefer constructor injection; in Spring classes, `@RequiredArgsConstructor` is the default Lombok pattern.
- Keep imports explicit; avoid wildcard imports.
- Keep methods small and intention-revealing.
- Avoid comment noise; comment only non-obvious invariants or cross-system constraints.

## Type and Modeling Rules
- Use Java 21 features when they improve clarity, especially `record` for immutable value objects and event payloads.
- Prefer value objects over raw primitives for IDs, statuses, risk levels, types, and similar domain concepts.
- Keep aggregate behavior inside aggregates; avoid anemic setter-only models.
- Repository interfaces are defined per aggregate root.
- Use `Optional<T>` for absent aggregate lookups.
- Prefer `List` and `Set` in signatures unless a concrete collection is required.
- Do not pass transport DTOs or persistence DOs across layers.

## Error Handling and API Contract
- Java JSON APIs must return `Result<T>`.
- `code = 0` means success.
- Every JSON response must include `requestId` and `timestamp`.
- SSE responses are not wrapped frame-by-frame in `Result<T>`.
- Throw exceptions; do not manually build failure `Result` objects in controllers.
- Use `BizException` for expected business failures and `SysException` or centralized mapping for system failures.
- Do not leak stack traces, SQL, secrets, tokens, or internal hostnames in user-facing messages.
- Preserve and propagate `X-Request-Id` / `requestId` across Java, Python, logs, and audits.

## Logging Rules
- Use SLF4J parameterized logging, never string concatenation.
- Log key business transitions at `INFO`, recoverable issues at `WARN`, unexpected failures at `ERROR`.
- Include stable identifiers such as `requestId`, aggregate IDs, and business IDs.
- Never log passwords, tokens, raw EMR content, or other sensitive payloads.

## Agent Working Rules
- Do not silently expand scope from `P0` into `P1` or `P2`.
- AI output must not drift into diagnosis conclusions, prescription advice, or dosage guidance.

## External Collaborative Document Rules
- These rules apply only to external collaborative documents such as Feishu/Lark docs, online task boards, and similar repository-external documents. Repo-local Markdown and code files still follow normal repository editing rules.
- Before modifying an external collaborative document, the agent must first read the current document content with the appropriate dedicated tool. Do not edit based only on memory, old snapshots, or partial excerpts from earlier turns.
- Do not use full-document overwrite unless the agent has already fetched and checked the complete latest document content. Prefer scoped updates that modify only the intended section.
- Before writing an external collaborative document, the agent must keep a local recoverable snapshot of the current content being changed so the document can be restored quickly if the update goes wrong.
- Keep external document edits minimal. If the user asked to update a status, conclusion, or short section, do not restructure or rewrite unrelated parts of the document.
- After every external document write, the agent must read back the updated document and verify the key sections for truncation, duplicated headings, missing sections, list corruption, or contradictory status updates.
- If an external document update causes damage or inconsistency, the agent must explicitly acknowledge it and repair the document before moving on to any other task.
