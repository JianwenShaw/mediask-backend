# Canonical files

Use these files as the first stop when MediAsk API contract work changes behavior visible to callers.

## Repo rules

- `AGENTS.md`
- `docs/docs/00A-P0-BASELINE.md`
- `docs/playbooks/00B-P0-DEVELOPMENT-CHECKLIST.md`
- `docs/playbooks/00C-P0-BACKEND-TASKS.md`
- `docs/playbooks/00E-P0-BACKEND-ORDER-AND-DTOS.md`

## API contract docs

- `docs/docs/10A-JAVA_AI_API_CONTRACT.md` for AI-facing browser APIs and related JSON contract rules
- `docs/playbooks/00G-P0-CURRENT-API-CONTRACT.md` when the current P0 backend API list or DTO contract summary needs to stay aligned

## Serialization reminders

- Business IDs in response JSON are strings.
- Business `LocalDate` fields use `yyyy-MM-dd`.
- Business `OffsetDateTime` fields use `yyyy-MM-dd'T'HH:mm:ssXXX`.
- `Result.timestamp` remains a numeric millisecond timestamp.
- Response DTO annotations own these guarantees; global Jackson config is not the contract source.
