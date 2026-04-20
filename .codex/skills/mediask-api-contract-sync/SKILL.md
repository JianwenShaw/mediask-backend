---
name: mediask-api-contract-sync
description: "Keep MediAsk backend API changes and repo docs in sync. Use when changing mediask-api controllers, request/response DTOs, assemblers, API tests, or JSON contracts, especially for AI APIs. Enforce that the same turn updates the matching docs under docs/, keeps business ID fields serialized as strings, applies the repo business date/time wire-format rules on response DTOs, and avoids relying on a global Jackson config for API contracts."
---

# MediAsk API Contract Sync

Use this skill when a change affects the external JSON contract of `mediask-api`.

## Workflow

1. Read `AGENTS.md` and the relevant repo docs before changing code.
2. Change code and docs in the same turn. Do not leave API behavior updated while `docs/` still describes the old contract.
3. Validate the real wire format with controller/web tests when possible. Do not trust only a hand-built `ObjectMapper` test.

## Pick The Contract Docs

Update the smallest authoritative doc set that matches the change.

- For AI APIs or SSE-related API work, read and update `docs/docs/10A-JAVA_AI_API_CONTRACT.md`.
- For broader backend API workflow or DTO conventions, also check `docs/playbooks/00E-P0-BACKEND-ORDER-AND-DTOS.md` and `docs/playbooks/00G-P0-CURRENT-API-CONTRACT.md` when relevant.
- Search `docs/` by endpoint path, DTO name, or business term before deciding that no doc change is needed.
- If the change only alters implementation and not the external contract, add or update the smallest rule note needed to prevent future confusion.

## Wire-Format Rules

Treat response DTOs as the source of truth for JSON wire format.

- Business ID fields exposed in JSON must serialize as strings. Use explicit DTO-level serialization such as `@JsonSerialize(using = ToStringSerializer.class)` on response DTO fields.
- Business date fields exposed in JSON must use `@JsonFormat(pattern = "yyyy-MM-dd")` on response DTO fields.
- Business date-time fields exposed in JSON must use `@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")` on response DTO fields.
- `Result.timestamp` stays a numeric millisecond timestamp. Do not convert it to a string.
- Do not rely on a global Jackson config or a Boot Jackson customizer as the only guarantee of the external API contract.

## Checks

Before finishing, confirm all of the following.

- Code and matching `docs/` entries are both updated.
- Example JSON and rule text still match the current response DTO annotations.
- ID fields that represent business keys are strings in responses.
- Business date and business date-time fields match the repo format rules.
- Tests assert real response JSON on critical endpoints when the change is non-trivial.

## References

Read `references/contract-files.md` for the canonical file locations that currently carry these rules.
