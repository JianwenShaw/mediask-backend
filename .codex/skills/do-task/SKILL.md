---
name: do-task
description: finish project's unfinished task which was defined in a lark/feishu doc; use local lark-related skills first to read the task doc, then implement strictly against repo docs
---

# Do Task

Start by reading the task document in Feishu/Lark to understand the current requirement:

- Task doc: [Feishu document](https://www.feishu.cn/docx/A0bFdW8rKoSGaWxv2rqcwIgXnWg)

## Required workflow

1. First use local Lark/Feishu-related skills and tools to read the document content directly instead of treating the URL as a normal web page.
2. Prefer these skills when available:
   - `lark-doc`: use it to locate and read the Feishu doc content
   - other `lark-*` skills only when they are clearly needed by the task
3. After reading the task doc, identify the exact current task, scope, acceptance criteria, and whether it depends on any external system.
4. Then consult the documentation in the repository `docs/` directory with specific questions in mind, so the implementation stays strictly consistent with the documented baseline.
5. If the repo documentation appears unreasonable, outdated, or in conflict with the task doc, stop and communicate the conflict before making an implementation plan or code change.

## Constraints

- Do not guess the current task from TODOs, old snapshots, or partial repo state when the Feishu/Lark doc has not actually been read.
- Do not assume a web browser can access the document content directly.
- If the Lark/Feishu doc still cannot be read after using the available lark skills/tools, state that clearly and ask the user to provide the task content or grant the missing access.

**Note**: Do not make assumptions about anything uncertain. Either ask for clarification or find evidence within the documentation. Also, please note that lark-cli must be used in a non-sandbox environment. You can apply for access to this CLI tool directly.
