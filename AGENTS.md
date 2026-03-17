# MediAsk Backend Agent Guide

This file is for coding agents working in `mediask-backend`.

## Repository Snapshot
- Stack: Java 21, Spring Boot 3.5.x, Maven multi-module build.
- Modules: `mediask-api`, `mediask-application`, `mediask-domain`, `mediask-infra`, `mediask-common`, `mediask-worker`.
- Architecture target: DDD + hexagonal modular monolith.
- The codebase is still sparse; for conventions, trust the docs baseline more than current code volume.

## Read First
For non-trivial work, read these first:
1. `docs/CLAUDE.md`
2. `docs/docs/00A-P0-BASELINE.md`
3. `docs/playbooks/00B-P0-DEVELOPMENT-CHECKLIST.md`
4. `docs/playbooks/AI-CODE-REVIEW-CHECKLIST.md`

For backend changes, also read:
5. `docs/playbooks/00C-P0-BACKEND-TASKS.md`
6. `docs/playbooks/00E-P0-BACKEND-ORDER-AND-DTOS.md`
7. `docs/docs/01-OVERVIEW.md`
8. `docs/docs/02-CODE_STANDARDS.md`
9. `docs/docs/05-TESTING.md`
10. `docs/docs/06-DDD_DESIGN.md`
11. `docs/docs/19-ERROR_EXCEPTION_RESPONSE_DESIGN.md`
12. `docs/docs/10A-JAVA_AI_API_CONTRACT.md` when changing AI APIs or SSE.

## Cursor / Copilot Rules
- No `.cursor/rules/` directory was found.
- No `.cursorrules` file was found.
- No `.github/copilot-instructions.md` file was found.
- This `AGENTS.md` is the repository-local rule file for agents.

## Platform Rule
Before running any `mvn` command, check the OS with `./scripts/os_detect.py`.
- macOS: use `./scripts/m21.sh` instead of `mvn`.
- Linux/Windows: use `mvn` normally.

For this repo on macOS, prefer:
```bash
./scripts/m21.sh <goals>
```

## Build / Run Commands
Run from the repository root unless there is a clear reason not to.
- Full build: `./scripts/m21.sh clean verify`
- Compile only: `./scripts/m21.sh clean compile`
- Package without tests: `./scripts/m21.sh clean package -DskipTests`
- Run all tests: `./scripts/m21.sh test`
- Build one module and dependencies: `./scripts/m21.sh -pl mediask-api -am clean verify`
- Run module tests and dependencies: `./scripts/m21.sh -pl mediask-api -am test`
- Start API: `./scripts/m21.sh spring-boot:run -pl mediask-api`
- Start worker: `./scripts/m21.sh spring-boot:run -pl mediask-worker`

If plugin prefix resolution fails, use:
```bash
./scripts/m21.sh -pl mediask-api org.springframework.boot:spring-boot-maven-plugin:run
```

## Test Commands
The root `pom.xml` does not currently configure custom Surefire, Failsafe, JaCoCo, Checkstyle, or Spotless plugins. Use standard Maven commands.

- All tests: `./scripts/m21.sh test`
- Single module: `./scripts/m21.sh -pl mediask-domain -am test`
- Single test class: `./scripts/m21.sh -pl mediask-domain -am -Dtest=RegistrationOrderTest test`
- Single test method: `./scripts/m21.sh -pl mediask-domain -am -Dtest=RegistrationOrderTest#confirm_WhenInvalidState_ThrowException test`
- Multiple test classes: `./scripts/m21.sh -pl mediask-domain -am -Dtest=RegistrationOrderTest,ClinicSessionTest test`
- Package while skipping tests: `./scripts/m21.sh package -DskipTests`

Notes:
- `-pl <module> -am` is the safest default in this multi-module repo.
- Run single tests from the smallest relevant module, usually `mediask-domain`, `mediask-application`, or `mediask-api`.
- If no tests exist yet in the touched area, add focused tests near the changed code.

## Lint / Formatting State
- No `.editorconfig` exists.
- No linter or formatter is wired into the build yet.
- Treat `./scripts/m21.sh test` and `./scripts/m21.sh verify` as the current quality gates.
- If you introduce lint or format tooling, wire it into `verify` and update this file.

## Architecture Rules
Dependency direction:
```text
mediask-api -> mediask-application -> mediask-domain
mediask-api -> mediask-infra -> mediask-domain
mediask-worker -> mediask-application
mediask-worker -> mediask-infra
all modules -> mediask-common
```

Hard rules:
- `mediask-domain` stays framework-free: no Spring, MyBatis, Redis, servlet APIs, or infrastructure code.
- `mediask-application` orchestrates use cases and transactions; domain rules belong in aggregates or domain services.
- `mediask-infra` implements ports defined by the domain.
- `mediask-api` and `mediask-worker` are entry-point adapters; controllers and jobs call application services, not repositories.
- Cross-context communication uses IDs, ports, and domain events, not direct aggregate references.

## Package and Naming Conventions
- Base package: `me.jianwen.mediask`.
- Organize by bounded context first, then by layer.
- API: `*Controller`, `*Request`, `*Response`, `*VO`, `*Assembler`
- Application: `*UseCase`, `*Command`, `*Query`
- Domain: entities use business nouns, value objects often use `*Id`, `*Status`, `*Type`, domain services use `*DomainService`, ports use `*Repository` or `*Port`, events use past-tense `*Event`
- Infra: `*RepositoryImpl`, `*DO`, `*Mapper`, `*Converter`, `*Client`
- Common: `*Exception`, `ErrorCode`, constants, utility classes
- Enums should not use an `Enum` suffix.

## Java Style
- Prefer explicit, business-meaningful names; avoid pinyin, vague abbreviations, and placeholders like `tmp`, `obj`, `dto1`.
- Prefer constructor injection; in Spring classes, `@RequiredArgsConstructor` is the default Lombok pattern.
- Keep imports explicit; avoid wildcard imports.
- Follow normal Java formatting: 4-space indentation, braces on the same line, one top-level class per file.
- Keep methods small and intention-revealing.
- Avoid comment noise; comment only non-obvious invariants or cross-system constraints.
- Do not add framework annotations to `mediask-domain`.

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

## Testing Expectations
- Follow AAA: Arrange, Act, Assert.
- Prefer scenario-oriented test names like `confirm_WhenInvalidState_ThrowException`.
- Add regression tests for bug fixes.
- For transaction-sensitive logic, assert both outcome and absence of invalid side effects.
- Do not rely on real LLM output or unstable external services in tests.
- For permission-sensitive flows, test both allowed and denied paths.

## Agent Working Rules
- Prefer changing code to match the docs baseline instead of casually changing docs.
- Do not silently expand scope from `P0` into `P1` or `P2`.
- Browser traffic must go to Java only, not directly to Python.
- Python-owned persistence is limited to `knowledge_chunk_index` and `ai_run_citation` unless the baseline changes.
- AI output must not drift into diagnosis conclusions, prescription advice, or dosage guidance.

## When Updating This File
Update `AGENTS.md` whenever build, run, test, single-test, lint, formatting, architecture, naming, exception/response, Cursor, or Copilot rules change.
