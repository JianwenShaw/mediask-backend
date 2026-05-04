# MediAsk Backend Agent Guide

## Snapshot
- Stack: Java 21, Spring Boot 3.5.x, Maven multi-module build.
- Architecture target: DDD + hexagonal modular monolith.

## Read
For any backend change, read:
- `docs/docs/01-OVERVIEW.md` — system architecture overview

Context-dependent:
- `docs/playbooks/00C-P0-BACKEND-TASKS.md` — when planning new P0 feature work or checking what's missing
- `docs/playbooks/00E-P0-BACKEND-ORDER-AND-DTOS.md` — when creating or modifying API endpoints, request/response DTOs, or assemblers
- `docs/docs/19-ERROR_EXCEPTION_RESPONSE_DESIGN.md` — when adding exceptions, modifying error handling, or changing API response format
- When changing AI APIs or SSE:
  - `docs/proposals/rag-python-service-design/02-integration-contract.md`
  - `docs/proposals/rag-python-service-design/03-java-boundary-and-owned-data.md`

Maven test note:
- For module-scoped tests from repo root, use `mvn -pl <module> -am ...`; plain `-pl <module>` can miss local reactor dependencies.
- Narrow test runs can use `mvn -pl mediask-api -am -Dtest=SomeTest test -Dsurefire.failIfNoSpecifiedTests=false`.
- `mediask-infra` tests must stay within existing test dependencies: do not assume Mockito; prefer manual stubs, simple test doubles, or JDK dynamic proxies.

## Architecture
Dependency direction:
```text
mediask-api -> mediask-application -> mediask-domain
mediask-api -> mediask-infra -> mediask-domain
mediask-worker -> mediask-application
mediask-worker -> mediask-infra
all modules -> mediask-common
```

- `mediask-domain` stays **framework-free**: no Spring, MyBatis, Redis, servlet APIs, or infrastructure code.
- `mediask-application` orchestrates use cases and transactions; domain rules belong in aggregates or domain services.
- `mediask-infra` implements ports defined by the domain.
- `mediask-api` and `mediask-worker` are entry-point adapters; controllers and jobs call application services, not repositories.
- Cross-context communication uses IDs, ports, and domain events, not direct aggregate references.
- **AI must preserve module purity: do not add imports or dependencies that violate the dependency direction, even if they seem convenient.**

## Java and Modeling
- Prefer constructor injection; in Spring classes, `@RequiredArgsConstructor` is the default Lombok pattern.
- Keep methods small and intention-revealing.
- Avoid comment noise; comment only non-obvious invariants or cross-system constraints.
- Use Java 21 features when they improve clarity, especially `record` for immutable value objects and event payloads.
- Prefer value objects over raw primitives for IDs, statuses, risk levels, types, and similar domain concepts.
- Keep aggregate behavior inside aggregates; avoid anemic setter-only models.
- Repository interfaces are defined per aggregate root.
- Use `Optional<T>` for absent aggregate lookups.
- Prefer `List` and `Set` in signatures unless a concrete collection is required.
- Do not pass transport DTOs or persistence DOs across layers.

## API and Errors
- Java JSON APIs must return `Result<T>`.
- `code = 0` means success.
- Every JSON response must include `requestId` and `timestamp`.
- SSE responses are not wrapped frame-by-frame in `Result<T>`.
- Response DTOs own JSON wire-format contracts. For business IDs and business date/time fields, declare serialization explicitly on the response DTO instead of relying on a global Jackson config to enforce the contract.
- Throw exceptions; do not manually build failure `Result` objects in controllers.
- Use `BizException` for expected business failures and `SysException` or centralized mapping for system failures.
- Do not leak stack traces, SQL, secrets, tokens, or internal hostnames in user-facing messages.
- Preserve and propagate `X-Request-Id` / `requestId` across Java, Python, logs, and audits.

## Logging
- Use SLF4J parameterized logging, never string concatenation.
- Log key business transitions at `INFO`, recoverable issues at `WARN`, unexpected failures at `ERROR`.
- Include stable identifiers such as `requestId`, aggregate IDs, and business IDs.
- Never log passwords, tokens, raw EMR content, or other sensitive payloads.

## Agent Working Rules
- Do not silently expand scope from `P0` into `P1` or `P2`.
- AI output must not drift into diagnosis conclusions, prescription advice, or dosage guidance.
