# TODO

## Working Rules

- 一次只执行一个 `CURRENT` 任务。
- 当前任务未审查确认前，不进入下一个任务。
- 本文件只记录当前 `Java` 仓库需要完成的任务。
- 若某个 Java 任务依赖 Python AI 服务，本文件只注明“依赖 Python 提供什么”，不把 Python 实现本身当作本仓库任务。
- 只围绕 `P0` 主线推进：`认证 -> AI 问诊 -> RAG 引用 -> 导诊结果 -> 挂号 -> 接诊 -> 病历/处方 -> 对象级授权 -> 审计留痕`。
- 不进入 `P1/P2`：不做排班增强、通知、字典、复杂观测、事件总线、治理型能力。

## Status Legend

- `todo`: 未开始
- `doing`: 开发中
- `done`: 已完成并通过自查
- `blocked`: 当前 Java 任务已明确，但被外部依赖阻塞

## Current Position

- `done` 公共协议与请求上下文：`Result<T>`、全局异常、`requestId`、JWT、Java `health/readiness/liveness`、对外 `SSE` 转发骨架、结构化日志。
- `done` 基础业务底座：管理员患者管理、患者/医生本人资料、门诊场次查询、挂号创建、我的挂号、挂号后预创建 `visit_encounter`、医生接诊列表。
- `done` 知识库 Java 侧底座：知识库管理、知识文档导入/列表/删除、Java 调 Python `prepare`、`knowledge_document`/`knowledge_chunk` 持久化、Java 调 Python `index`。
- `todo` AI 问诊 Java 对外主链：`/api/v1/ai/chat`、`/api/v1/ai/chat/stream`、会话回看、导诊结果、挂号承接。
- `todo` 医生接诊闭环：接诊详情、AI 摘要、病历、处方。
- `todo` 安全合规闭环：`AI_SESSION` / `EMR_RECORD` 对象级授权、`audit_event`、`data_access_log`、最小审计查询。

## External Dependency Notes

- `blocked` 当前 `6001` 属于 Java -> Python AI 联调问题。在 Python `/health`、`/ready`、`/api/v1/chat` 未稳定前，Java AI 主链无法完成验收。
- `todo` Java 侧知识文档导入链路已具备，但最终 RAG 可用性仍依赖 Python 侧 `prepare/index/search` 的真实可用性。
- `todo` Java 侧会话、引用、导诊结果可以先按契约落地，但最终联调仍依赖 Python 返回稳定的 `risk_level`、`guardrail_action`、`citations` 等字段。

## Phase 0 - 已完成基础能力

### B0 公共协议与认证基线

- ID: `B0`
- 状态: `done`
- 说明: `Result<T>`、错误处理、`requestId`、JWT 登录/刷新/登出/当前用户、Java 健康检查、`SSE` 转发骨架、结构化日志基线已完成。

### B1 挂号与接诊最小底座

- ID: `B1`
- 状态: `done`
- 说明: `GET /api/v1/clinic-sessions`、`POST /api/v1/registrations`、`GET /api/v1/registrations`、`GET /api/v1/encounters` 已完成，且挂号成功后预创建 `visit_encounter`。

### B2 知识库 Java 侧底座

- ID: `B2`
- 状态: `done`
- 说明: 知识库/知识文档后台接口已完成，Java 已具备 `prepare -> chunk 持久化 -> index` 的调用链骨架。

## Phase 1 - Java AI 问诊主链

### CURRENT - T1 非流式 AI 问诊接口

- ID: `T1`
- 状态: `done`
- 目标: 完成 `POST /api/v1/ai/chat`，让 Java 对外提供 AI 问诊主入口。
- Java 完成标准:
  - 完成 Controller / UseCase / DTO / 持久化链路。
  - Java 预创建 `ai_model_run`。
  - Java 持久化 `ai_session`、`ai_turn`、`ai_turn_content`。
  - Java 调 Python 时透传 `model_run_id` 与 `requestId`。
  - 返回 `sessionId`、`turnId`、`answer`、`triageResult`。
  - `triageResult` 至少包含 `riskLevel`、`guardrailAction`、`nextAction`、`recommendedDepartments`、`careAdvice`、`citations`。
- 依赖 Python 提供:
  - `POST /api/v1/chat`
  - 稳定返回 `answer`、`risk_level`、`guardrail_action`、`chief_complaint_summary`、`recommended_departments`、`care_advice`、`citations`
  - 失败时稳定返回 `code`、`msg`、`requestId`、`timestamp`
  - 基于 `model_run_id` 写入 `ai_run_citation`
- 涉及接口/表:
  - `POST /api/v1/ai/chat`
  - `ai_session`
  - `ai_turn`
  - `ai_turn_content`
  - `ai_model_run`
  - `ai_guardrail_event`
  - `ai_run_citation`

### T2 流式 AI 问诊接口

- ID: `T2`
- 状态: `done`
- 目标: 完成 `POST /api/v1/ai/chat/stream`，由 Java 对外统一暴露 SSE。
- Java 完成标准:
  - Java 继续作为浏览器唯一 AI 入口。
  - 流式协议稳定输出 `message / meta / end / error`。
  - `meta` 中带 `sessionId`、`turnId`、`triageResult`。
  - 异常结束时仍可通过 `requestId` 串联日志。
  - Java 预创建并持久化 `ai_session`、`ai_turn`、`ai_turn_content`、`ai_model_run`。
  - 流式成功结束后回填 `ai_guardrail_event`、会话摘要与运行状态。
- 依赖 Python 提供:
  - `POST /api/v1/chat/stream`
  - 稳定的流式消息与结束事件
  - 流式异常时可映射为稳定错误
- 涉及接口/表:
  - `POST /api/v1/ai/chat/stream`
  - `ai_session`
  - `ai_turn`
  - `ai_turn_content`
  - `ai_model_run`
  - `ai_guardrail_event`

### T3 AI 会话回看与导诊结果

- ID: `T3`
- 状态: `done`
- 目标: 完成会话详情与导诊结果查询，支撑患者结果页和后续挂号承接。
- Java 完成标准:
  - 完成 `GET /api/v1/ai/sessions/{sessionId}`。
  - 完成 `GET /api/v1/ai/sessions/{sessionId}/triage-result`。
  - 导诊结果可展示 `riskLevel`、`nextAction`、`recommendedDepartments`、`citations`。
  - 高风险分支不继续普通问答，返回明确下一步动作。
- 当前实现边界:
  - 当前仅支持患者本人回看自己的 AI 会话。
  - 医生侧 AI 内容查看仍由后续 `T8` 的 `GET /api/v1/encounters/{encounterId}/ai-summary` 承接。
- 依赖 Python 提供:
  - 已在问诊阶段返回并写入可回放的 `risk_level`、`guardrail_action`、`citations`
  - `ai_run_citation` 数据完整可查
- 涉及接口/表:
  - `GET /api/v1/ai/sessions/{sessionId}`
  - `GET /api/v1/ai/sessions/{sessionId}/triage-result`
  - `ai_session`
  - `ai_turn`
  - `ai_turn_content`
  - `ai_guardrail_event`
  - `ai_run_citation`
- 紧急待办:
  - 当前 `GET /api/v1/ai/sessions/{sessionId}` 已返回 AI 原文，但尚未写入 `data_access_log`
  - 在继续推进验收或上线前，必须补齐 AI 原文查看的 `data_access_log` 写入链路和测试

### T4 验证 Java 侧 RAG 接入闭环

- ID: `T4`
- 状态: `todo`
- 目标: 验证当前 Java 知识导入链路与 AI 问诊链路已能实际接入可检索知识。
- Java 完成标准:
  - 至少一套知识文档可通过现有后台导入接口入库。
  - `knowledge_document`、`knowledge_chunk` 数据完整。
  - Java 侧可完成 `prepare -> chunk 持久化 -> index` 整体调用。
  - 有最小联调验证记录。
- 依赖 Python 提供:
  - `POST /api/v1/knowledge/prepare`
  - `POST /api/v1/knowledge/index`
  - `POST /api/v1/knowledge/search`
  - `knowledge_chunk_index` 真实写入并可检索命中
- 涉及接口/表:
  - `POST /api/v1/admin/knowledge-documents/import`
  - `knowledge_document`
  - `knowledge_chunk`
  - `knowledge_chunk_index`

## Phase 2 - Java AI 到挂号承接

### T5 导诊结果承接挂号

- ID: `T5`
- 状态: `todo`
- 目标: 完成 `POST /api/v1/ai/sessions/{sessionId}/registration-handoff`，由 Java 把 AI 结果转换成挂号入口参数。
- Java 完成标准:
  - 返回推荐科室、主诉摘要、建议就诊类型、挂号查询参数。
  - AI 结果可直接带出挂号检索所需 `departmentId` 等最小信息。
  - 仅允许当前患者访问自己的 AI 会话承接结果。
- 依赖 Python 提供:
  - 在问诊结果中稳定返回推荐科室与主诉摘要
- 涉及接口/表:
  - `POST /api/v1/ai/sessions/{sessionId}/registration-handoff`
  - `ai_session`
  - `registration_order`

### T6 挂号写入 AI 来源

- ID: `T6`
- 状态: `todo`
- 目标: 让 Java 挂号链路具备 AI 来源追溯能力。
- Java 完成标准:
  - 从 AI 导诊结果进入挂号时真实写入 `registration_order.source_ai_session_id`。
  - 现有 `POST /api/v1/registrations` 保持最小入参，不引入额外复杂度。
  - AI 来源可在后续接诊与审计链路中追溯。
- 依赖 Python 提供:
  - 无新增接口依赖，仅依赖前置 AI 会话已创建成功
- 涉及接口/表:
  - `POST /api/v1/registrations`
  - `registration_order`
  - `ai_session`

## Phase 3 - Java 医生接诊、病历、处方闭环

### T7 接诊详情

- ID: `T7`
- 状态: `todo`
- 目标: 完成 `GET /api/v1/encounters/{encounterId}`，支持医生查看单个接诊详情。
- Java 完成标准:
  - 返回 `encounterId`、`registrationId`、`patientSummary` 等最小字段。
  - 仅允许当前医生查看自己的接诊记录。
  - 有主成功路径测试。
- 依赖 Python 提供:
  - 无
- 涉及接口/表:
  - `GET /api/v1/encounters/{encounterId}`
  - `visit_encounter`
  - `registration_order`

### T8 医生侧 AI 摘要

- ID: `T8`
- 状态: `todo`
- 目标: 完成 `GET /api/v1/encounters/{encounterId}/ai-summary`，让医生接诊前看到 AI 摘要而非原文。
- Java 完成标准:
  - 返回 `encounterId`、`sessionId`、`chiefComplaintSummary`、`structuredSummary`、`riskLevel`、`latestCitations`。
  - 默认不暴露 AI 原文。
  - 能从 `source_ai_session_id` 或等价链路追溯到 AI 会话。
- 依赖 Python 提供:
  - 前置 AI 问诊结果中有稳定的摘要、风险等级、引用数据
- 涉及接口/表:
  - `GET /api/v1/encounters/{encounterId}/ai-summary`
  - `visit_encounter`
  - `registration_order`
  - `ai_session`
  - `ai_turn`

### T9 保存病历

- ID: `T9`
- 状态: `todo`
- 目标: 完成 `POST /api/v1/emr`，保存病历头、正文和诊断。
- Java 完成标准:
  - 写入 `emr_record`、`emr_record_content`、`emr_diagnosis`。
  - 请求和响应字段对齐最小 DTO。
  - 有主成功路径测试。
- 依赖 Python 提供:
  - 无
- 涉及接口/表:
  - `POST /api/v1/emr`
  - `emr_record`
  - `emr_record_content`
  - `emr_diagnosis`

### T10 查看病历

- ID: `T10`
- 状态: `todo`
- 目标: 完成 `GET /api/v1/emr/{encounterId}`，查看单次接诊对应病历。
- Java 完成标准:
  - 返回病历正文和诊断列表。
  - 正文查询与索引查询分层，列表不直接暴露正文。
  - 只允许有权限的患者/医生访问。
  - 有主成功路径测试和拒绝路径测试。
- 依赖 Python 提供:
  - 无
- 涉及接口/表:
  - `GET /api/v1/emr/{encounterId}`
  - `emr_record`
  - `emr_record_content`
  - `emr_diagnosis`

### T11 保存处方

- ID: `T11`
- 状态: `todo`
- 目标: 完成 `POST /api/v1/prescriptions`，保存处方头和处方项。
- Java 完成标准:
  - 写入 `prescription_order`、`prescription_item`。
  - 返回 `prescriptionOrderId`、`status`。
  - 有主成功路径测试。
- 依赖 Python 提供:
  - 无
- 涉及接口/表:
  - `POST /api/v1/prescriptions`
  - `prescription_order`
  - `prescription_item`

### T12 查看处方

- ID: `T12`
- 状态: `todo`
- 目标: 完成 `GET /api/v1/prescriptions/{encounterId}`，查看单次接诊对应处方。
- Java 完成标准:
  - 返回处方头和处方项列表。
  - 只允许有权限的患者/医生访问。
  - 有主成功路径测试和拒绝路径测试。
- 依赖 Python 提供:
  - 无
- 涉及接口/表:
  - `GET /api/v1/prescriptions/{encounterId}`
  - `prescription_order`
  - `prescription_item`

## Phase 4 - Java 对象级授权与审计

### T13 AI 会话与病历对象级授权

- ID: `T13`
- 状态: `todo`
- 目标: 补齐 `EMR_RECORD` / `AI_SESSION` 的对象级资源解析与授权闭环。
- Java 完成标准:
  - `ResourceReferenceAssemblerPort` / `ResourceAccessResolverPort` 有实际实现。
  - 患者不能读取他人病历与 AI 会话。
  - 医生不能读取超出本人接诊范围的病历与 AI 会话。
  - 有拒绝路径测试。
- 依赖 Python 提供:
  - 无新增依赖，仅依赖前置 AI 会话数据存在
- 涉及接口/表:
  - `GET /api/v1/ai/sessions/{sessionId}`
  - `GET /api/v1/ai/sessions/{sessionId}/triage-result`
  - `GET /api/v1/emr/{encounterId}`
  - `GET /api/v1/prescriptions/{encounterId}`
  - `data_scope_rules`

### T14 敏感访问留痕

- ID: `T14`
- 状态: `todo`
- 目标: 查看病历正文和 AI 会话敏感内容时写入 `data_access_log`。
- Java 完成标准:
  - `GET /api/v1/emr/{encounterId}` 查看正文时写 `data_access_log`。
  - AI 会话敏感查看链路写 `data_access_log`。
  - 记录访问人、资源类型、资源标识、访问结果、`requestId`。
  - 有测试覆盖。
- 依赖 Python 提供:
  - 无
- 涉及接口/表:
  - `GET /api/v1/emr/{encounterId}`
  - `GET /api/v1/ai/sessions/{sessionId}`
  - `data_access_log`

### T15 关键业务动作审计

- ID: `T15`
- 状态: `todo`
- 目标: 关键业务成功动作写入 `audit_event`。
- Java 完成标准:
  - 登录、挂号、病历保存、处方保存至少写审计事件。
  - 记录稳定的 `requestId`、操作人、动作码、资源类型、资源标识。
  - 有测试覆盖。
- 依赖 Python 提供:
  - 无
- 涉及接口/表:
  - `POST /api/v1/auth/login`
  - `POST /api/v1/registrations`
  - `POST /api/v1/emr`
  - `POST /api/v1/prescriptions`
  - `audit_event`

### T16 最小审计查询接口

- ID: `T16`
- 状态: `todo`
- 目标: 完成管理员最小审计查询能力。
- Java 完成标准:
  - 完成 `GET /api/v1/audit/events`。
  - 完成 `GET /api/v1/audit/data-access`。
  - 仅管理员可访问。
  - 返回最小列表字段。
  - 有主成功路径测试和拒绝路径测试。
- 依赖 Python 提供:
  - 无
- 涉及接口/表:
  - `GET /api/v1/audit/events`
  - `GET /api/v1/audit/data-access`
  - `audit_event`
  - `data_access_log`
