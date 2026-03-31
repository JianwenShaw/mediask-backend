---
name: mediask-mybatis-plus
description: Use when working on persistence in mediask-backend with MyBatis-Plus, especially when adding or reviewing DO/Mapper/RepositoryAdapter code, deciding whether manual SQL is justified, preserving optimistic locking and audit fields, or avoiding N+1 query regressions.
---

# Mediask MyBatis Plus

Use this skill for persistence changes in `mediask-backend` that touch `mediask-infra` DOs, mappers, repository adapters, or query adapters.

## What This Skill Guards

- Keep persistence code aligned with the repo baseline: `mediask-infra` implements ports, `mapper` stays as the MyBatis boundary, and upper layers do not call mappers directly.
- Prefer MyBatis-Plus built-ins for ordinary single-table CRUD.
- Preserve project-level behaviors already wired into MyBatis-Plus, especially optimistic locking and audit field fill.
- Prevent query-shape regressions such as obvious N+1 access patterns on list or batch reads.

## Workflow

1. Confirm the change belongs in `mediask-infra`, not controller/use case/domain code.
2. Read the relevant DO, mapper, repository adapter, and the nearest existing adapter with similar behavior.
3. Decide whether the access pattern is ordinary CRUD or genuinely needs custom SQL.
4. Check whether the write path touches entities that inherit `BaseDO`; if yes, preserve `version`, `updatedAt`, and soft-delete semantics.
5. Check whether the read path loops over rows and issues secondary selects; if yes, collapse the query shape before implementing.
6. Add or update regression tests for the specific persistence behavior being changed.

## Default Rules

### 1. Prefer BaseMapper CRUD for ordinary work

Use `selectById`, `selectOne`, `selectList`, `insert`, `updateById`, and `deleteById` when the operation is a straightforward single-table CRUD operation.

Prefer lambda wrappers such as `Wrappers.lambdaQuery(...)` and `Wrappers.lambdaUpdate(...)` over string column names when a wrapper is needed.

Do not handwrite SQL or XML just because a query has a few predicates. Ordinary filtered CRUD should stay in MyBatis-Plus.

### 2. Handwritten SQL needs a real reason

Custom mapper SQL is justified when at least one of these is true:

- The query needs a join or projection row that is not a single DO.
- The query must fetch a batch shape specifically to avoid N+1.
- The query needs aggregation, grouping, windowing, or database-specific SQL that wrappers do not express clearly.
- A read model is intentionally denormalized and does not map cleanly to a single aggregate DO.

If none of those are true, stay with MyBatis-Plus CRUD.

### 3. Never bypass optimistic locking on `BaseDO`

`BaseDO` carries `@Version`, `createdAt`, `updatedAt`, and `deletedAt`. In this repo, that means updates on `BaseDO` descendants must preserve:

- optimistic locking via the entity `version`
- `updatedAt` fill via `MetaObjectHandler`
- soft-delete filtering via `deletedAt`

For updates to a specific existing row:

- First load the current effective row with the business predicates.
- Then update by entity, usually via `updateById(...)`.
- Carry `id` and current `version` into the update entity.

Do not use `mapper.update(null, UpdateWrapper...)` on `BaseDO` entities for normal business updates. That pattern can bypass the version plugin and audit fill.

If an update affects `0` rows after loading an existing row, treat it as a possible optimistic-lock conflict and map it to an explicit business conflict instead of a generic system error.

### 3.1 Be explicit when a field must be clearable to `NULL`

MyBatis-Plus `updateById(...)` will typically skip `null` properties under the default field strategy. That means "user cleared this optional field" can silently fail unless the write path is designed for it.

When a business field must support clearing to `NULL`:

- Prefer field-level `@TableField(updateStrategy = FieldStrategy.ALWAYS)` on the specific nullable column, not a global strategy change.
- Keep using `updateById(...)` with entity `id` and `version` so optimistic locking and `updatedAt` fill still work.
- Do not switch to `mapper.update(null, wrapper)` just to force `NULL` writes on `BaseDO` entities.
- Add a regression test that captures the entity passed to `updateById(...)` and asserts the cleared field is `null`.

Important repo pitfall:

- Upstream normalization such as `blankToNull(...)` does **not** make the database column clearable by itself.
- If a request accepts an empty string or blank string as "clear this field", and the draft/command converts that value to `null`, the corresponding DO field still needs `@TableField(updateStrategy = FieldStrategy.ALWAYS)`.
- Otherwise the repository test can show `null` reaching `updateById(...)` while the database row still keeps the old value, because MyBatis-Plus drops that column from the generated `UPDATE`.

Use this only for fields that are intentionally clearable. Do not blanket-apply `ALWAYS` to unrelated columns.

### 4. Keep “not found” and “conflict” distinct

For write paths:

- “No effective row exists” is a business not-found condition.
- “The row existed when read, but update matched 0 rows” is a concurrency conflict.

Do not collapse both into `SYSTEM_ERROR`.

### 5. Soft delete is part of the effective-row predicate

When reading or updating active business data, include the repo’s effective-row predicates, typically:

- `deletedAt IS NULL`
- any business status predicate such as `status = ACTIVE` where the adapter already treats that as the effective row

Match the existing read adapter semantics. Write adapters should not silently broaden them.

### 6. N+1 is not acceptable on list or batch flows

Watch for code shaped like:

- query a list of primary rows
- loop that list
- perform one or more mapper selects per row

That is acceptable only for single-record reads where the cardinality is known to stay tiny and the code path is not a list/batch API.

For list or batch flows, replace N+1 with one of:

- a join query in mapper SQL
- a batched `IN (...)` fetch plus in-memory assembly
- a purpose-built projection row mapper

When reviewing, treat “works for now” as insufficient if the query shape is structurally N+1.

## Repo-Specific Patterns

### Good default pattern for single-row reads

- Use `selectOne(Wrappers.lambdaQuery(...))`
- Include `deletedAt IS NULL`
- Include active status predicates when the business object only exists in active state
- Convert DO to domain object inside the repository adapter

### Good default pattern for updates on profile-like rows

- Read the current effective row first
- Throw the existing `*_NOT_FOUND` business error if missing
- Create an update DO carrying `id`, `version`, and changed fields
- Call `updateById(...)`
- If affected rows is `0`, throw an explicit conflict error
- Invalidate cache only after the database write succeeds

### Smells that should trigger a redesign

- Repository adapter uses raw string column names without a need
- `update(null, wrapper)` on a DO that extends `BaseDO`
- Use case or controller directly depends on a mapper
- List assembly performs repeated secondary selects per row
- New XML SQL is added for plain filtered CRUD that BaseMapper already handles

## Files To Check First

- `mediask-infra/src/main/java/me/jianwen/mediask/infra/persistence/base/BaseDO.java`
- `mediask-infra/src/main/java/me/jianwen/mediask/infra/persistence/base/MybatisPlusConfig.java`
- `mediask-infra/src/main/java/me/jianwen/mediask/infra/persistence/base/AuditFieldMetaObjectHandler.java`
- `docs/docs/02-CODE_STANDARDS.md`
- `docs/docs/03A-JAVA_CONFIG.md`

## Output Expectations

When using this skill, explain persistence decisions in concrete terms:

- why MyBatis-Plus CRUD is sufficient, or why custom SQL is necessary
- how optimistic locking and `updatedAt` are preserved
- how the chosen query shape avoids N+1
- what regression tests prove the behavior
