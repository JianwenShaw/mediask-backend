# TODO

## Working Rules

- 一次只执行一个 `CURRENT` 任务。
- 当前任务未审查确认前，不进入下一个任务。
- 当前阶段不做 AI、SSE、排班增强、通知、字典。

## Status Legend

- `todo`: 未开始
- `doing`: 开发中
- `done`: 已完成并通过自查

## Known Issues

- `todo` 知识文档存储配置当前默认 `mode=LOCAL`，但非 `dev` 环境若未显式配置 `mediask.ai.knowledge-storage.local.base-dir`，AI 相关 Bean 可能在启动阶段失败。
- `todo` 知识文档导入流程虽然已将重复校验前移，但 `store()` 仍早于最终持久化成功；并发重复导入或后续 `save/prepare/index` 失败时，仍可能留下孤儿文件。
- `todo` 当前类型识别支持通过 `Content-Type` 推断 `MARKDOWN/DOCX/PDF`，但本地存储适配器仍要求原始文件名必须带扩展名，扩展名缺失时会在存储阶段报错。
- `todo` 领域模型当前要求 `KnowledgeDocument.sourceUri` 非空，但数据库表结构仍允许 `knowledge_document.source_uri` 为 `NULL`，旧数据或手工写入数据在仓储回填时可能失败。

## Phase 1 - 门诊挂号最小闭环

### T1 门诊场次列表查询

- ID: `T1`
- 状态: `done`
- 目标: 完成 `GET /api/v1/clinic-sessions`，提供最小可挂号的门诊场次列表。
- 完成标准:
  - 新增门诊场次查询用例与控制器接口。
  - 返回字段至少覆盖 `clinicSessionId`、`departmentId`、`departmentName`、`doctorId`、`doctorName`、`sessionDate`、`periodCode`、`clinicType`、`remainingCount`、`fee`。
  - 查询仅返回已开放挂号的 `OPEN` 场次。
  - 有主成功路径测试。
- 涉及接口/表:
  - `GET /api/v1/clinic-sessions`
  - `clinic_session`
  - `clinic_slot`

### T2 创建挂号订单

- ID: `T2`
- 状态: `done`
- 目标: 完成 `POST /api/v1/registrations`，创建最小挂号订单。
- 完成标准:
  - 写入 `registration_order`。
  - 校验所选场次与号源关系正确。
  - 返回 `registrationId`、`orderNo`、`status`。
  - 有主成功路径测试。
- 涉及接口/表:
  - `POST /api/v1/registrations`
  - `registration_order`
  - `clinic_session`
  - `clinic_slot`

### T3 患者查看我的挂号

- ID: `T3`
- 状态: `done`
- 目标: 完成 `GET /api/v1/registrations`，支持患者查看自己的挂号记录。
- 完成标准:
  - 只返回当前患者自己的挂号记录。
  - 返回最小列表字段与状态。
  - 有主成功路径测试。
- 涉及接口/表:
  - `GET /api/v1/registrations`
  - `registration_order`

### T4 医生接诊列表

- ID: `T4`
- 状态: `done`
- 目标: 完成 `GET /api/v1/encounters`，支持医生查看待接诊/已接诊列表。
- 完成标准:
  - 基于已预创建的 `visit_encounter` 返回最小列表信息。
  - 仅返回当前医生可见的接诊记录。
  - 挂号创建成功后预创建 `visit_encounter`，初始状态为 `SCHEDULED`。
  - 有主成功路径测试。
- 涉及接口/表:
  - `GET /api/v1/encounters`
  - `visit_encounter`
  - `registration_order`

### CURRENT - T5 接诊详情

- ID: `T5`
- 状态: `todo`
- 目标: 完成 `GET /api/v1/encounters/{id}`，支持医生查看单个接诊详情。
- 完成标准:
  - 返回 `encounterId`、`registrationId`、`patientSummary` 等最小字段。
  - 仅允许当前医生查看自己的接诊记录。
  - 有主成功路径测试。
- 涉及接口/表:
  - `GET /api/v1/encounters/{id}`
  - `visit_encounter`
  - `registration_order`

## Phase 2 - 病历与处方闭环

### T6 保存病历

- ID: `T6`
- 状态: `todo`
- 目标: 完成 `POST /api/v1/emr`，保存病历头、正文和诊断。
- 完成标准:
  - 写入 `emr_record`、`emr_record_content`、`emr_diagnosis`。
  - 请求和响应字段对齐文档最小 DTO。
  - 有主成功路径测试。
- 涉及接口/表:
  - `POST /api/v1/emr`
  - `emr_record`
  - `emr_record_content`
  - `emr_diagnosis`

### T7 查看病历

- ID: `T7`
- 状态: `todo`
- 目标: 完成 `GET /api/v1/emr/{encounterId}`，查看单次接诊对应病历。
- 完成标准:
  - 返回病历正文和诊断列表。
  - 只允许有权限的患者/医生访问。
  - 有主成功路径测试和拒绝路径测试。
- 涉及接口/表:
  - `GET /api/v1/emr/{encounterId}`
  - `emr_record`
  - `emr_record_content`
  - `emr_diagnosis`

### T8 保存处方

- ID: `T8`
- 状态: `todo`
- 目标: 完成 `POST /api/v1/prescriptions`，保存处方头和处方项。
- 完成标准:
  - 写入 `prescription_order`、`prescription_item`。
  - 返回 `prescriptionOrderId`、`status`。
  - 有主成功路径测试。
- 涉及接口/表:
  - `POST /api/v1/prescriptions`
  - `prescription_order`
  - `prescription_item`

### T9 查看处方

- ID: `T9`
- 状态: `todo`
- 目标: 完成 `GET /api/v1/prescriptions/{encounterId}`，查看单次接诊对应处方。
- 完成标准:
  - 返回处方头和处方项列表。
  - 只允许有权限的患者/医生访问。
  - 有主成功路径测试和拒绝路径测试。
- 涉及接口/表:
  - `GET /api/v1/prescriptions/{encounterId}`
  - `prescription_order`
  - `prescription_item`

## Phase 3 - 对象级授权

### T10 患者侧对象级授权

- ID: `T10`
- 状态: `todo`
- 目标: 患者只能访问自己的挂号、病历、处方。
- 完成标准:
  - 相关接口统一接入对象级授权。
  - 至少覆盖病历和处方读取拒绝路径。
  - 有授权失败测试。
- 涉及接口/表:
  - `GET /api/v1/registrations`
  - `GET /api/v1/emr/{encounterId}`
  - `GET /api/v1/prescriptions/{encounterId}`
  - `data_scope_rules`

### T11 医生侧对象级授权

- ID: `T11`
- 状态: `todo`
- 目标: 医生只能访问自己接诊范围内的数据。
- 完成标准:
  - 接诊、病历、处方接口接入授权判断。
  - 非本人接诊记录访问被拒绝。
  - 有授权失败测试。
- 涉及接口/表:
  - `GET /api/v1/encounters`
  - `GET /api/v1/encounters/{id}`
  - `GET /api/v1/emr/{encounterId}`
  - `GET /api/v1/prescriptions/{encounterId}`
  - `data_scope_rules`

### T12 管理员最小审计查询授权

- ID: `T12`
- 状态: `todo`
- 目标: 管理员只开放最小审计查询能力。
- 完成标准:
  - 审计查询接口仅管理员可访问。
  - 普通患者和医生访问被拒绝。
  - 有授权失败测试。
- 涉及接口/表:
  - `GET /api/v1/audit/events`
  - `GET /api/v1/audit/data-access`

## Phase 4 - 审计与访问留痕

### T13 挂号写审计事件

- ID: `T13`
- 状态: `todo`
- 目标: 挂号创建成功时写入 `audit.audit_event`。
- 完成标准:
  - `POST /api/v1/registrations` 成功后写审计。
  - 审计记录包含 `requestId`、操作人、动作码、资源类型。
  - 有测试覆盖。
- 涉及接口/表:
  - `POST /api/v1/registrations`
  - `audit.audit_event`

### T14 病历保存写审计事件

- ID: `T14`
- 状态: `todo`
- 目标: 病历保存成功时写入 `audit.audit_event`。
- 完成标准:
  - `POST /api/v1/emr` 成功后写审计。
  - 有测试覆盖。
- 涉及接口/表:
  - `POST /api/v1/emr`
  - `audit.audit_event`

### T15 处方保存写审计事件

- ID: `T15`
- 状态: `todo`
- 目标: 处方保存成功时写入 `audit.audit_event`。
- 完成标准:
  - `POST /api/v1/prescriptions` 成功后写审计。
  - 有测试覆盖。
- 涉及接口/表:
  - `POST /api/v1/prescriptions`
  - `audit.audit_event`

### T16 查看病历正文写访问日志

- ID: `T16`
- 状态: `todo`
- 目标: 查看病历正文时写入 `audit.data_access_log`。
- 完成标准:
  - `GET /api/v1/emr/{encounterId}` 读取正文时写访问日志。
  - 记录访问结果和资源标识。
  - 有测试覆盖。
- 涉及接口/表:
  - `GET /api/v1/emr/{encounterId}`
  - `audit.data_access_log`

### T17 审计查询接口

- ID: `T17`
- 状态: `todo`
- 目标: 完成最小审计查询接口。
- 完成标准:
  - 完成 `GET /api/v1/audit/events`。
  - 完成 `GET /api/v1/audit/data-access`。
  - 返回最小列表字段。
  - 有主成功路径测试。
- 涉及接口/表:
  - `GET /api/v1/audit/events`
  - `GET /api/v1/audit/data-access`
  - `audit.audit_event`
  - `audit.data_access_log`
