---
name: qingniao-api-test
description: Use when validating mediask-backend HTTP APIs with the local qingniao CLI, especially to create or update TOML suites under scripts/qingniao, run login-dependent request flows, and verify JSON contract assertions against a locally running mediask-api service.
---

# Qingniao API Test

Use this skill when the task is to verify `mediask-backend` APIs through real HTTP requests with the local `qingniao` CLI.

## When To Use It

- The user asks to create or update `qingniao` TOML suites.
- The user asks to manually verify an API instead of relying only on unit tests.
- A review or bugfix needs request-level evidence from a running local service.

## Default Conventions

- Put repo-local suites under `scripts/qingniao/<scenario>/`.
- Keep each scenario self-contained with:
  - `qingniao-env.toml`
  - one or more suite TOML files
- Reuse `/Users/catovo/dev/qingniao/Cargo.toml` as the CLI entrypoint unless the user says otherwise.
- Prefer focused suites for one API or one workflow instead of one giant shared suite.

## Workflow

1. Check whether a matching suite already exists under `scripts/qingniao/`.
2. If no suite exists, create a new scenario directory with a local `qingniao-env.toml` and a focused suite TOML.
3. Read `references/qingniao-cli-usage.md` first for the current CLI behavior and TOML rules. This reference is expected to track the source `qingniao` project instead of becoming a stale copy.
4. Before running the suite, verify the target service is reachable, usually `http://localhost:8989/actuator/health`.
5. If the service is down, report that clearly instead of claiming verification.
6. Run the suite from its scenario directory with:

```bash
cargo run --manifest-path /Users/catovo/dev/qingniao/Cargo.toml -- run <suite>.toml --agent-output
```

7. Summarize the important request results in the response, especially:
  - HTTP status
  - `body.code`
  - contract fields that were asserted
  - any request IDs or error payloads that matter

## Suite Design Rules

- Always include the smallest login/setup step needed for authenticated endpoints.
- Add explicit assertions for both HTTP status and `body.code`.
- For success cases, assert the fields that matter to the change being verified.
- For validation/error cases, assert the expected error code and `requestId`.
- Keep assertions targeted; do not overfit the suite to volatile data unless the user wants exact payload checks.

## Mediask-Specific Notes

- `mediask-api` defaults to `http://localhost:8989`.
- For authenticated admin APIs, the usual flow is:
  - login
  - save `accessToken` and `refreshToken`
  - call the target API with `Authorization: Bearer {{run.token}}`
  - logout if cleanup is useful
- Current repo-local example: `scripts/qingniao/admin-patients-pagination/`.

## Output Expectations

- Say whether verification was actually executed or only prepared.
- If execution was blocked, state the blocker concretely, such as service not running or missing credentials.
- When a suite fails, report the failing request name and the key response details instead of pasting noisy output.
