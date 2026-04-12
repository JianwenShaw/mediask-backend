# AI Chat Non-Stream

用于验证 `POST /api/v1/ai/chat` 的最小链路：

1. 患者登录
2. 创建非流式 AI 问诊会话
3. 使用返回的 `sessionId` 续写同一会话
4. 验证 `useStream=true` 会被拒绝并返回 `1002`
5. 患者登出

## 运行

在当前目录执行：

```bash
cargo run --manifest-path /Users/catovo/dev/qingniao/Cargo.toml -- run ai-chat-non-stream.toml --agent-output
```

## 前置条件

- `mediask-api` 已启动，默认地址 `http://localhost:8989`
- 已正确配置：
  - `MEDIASK_AI_BASE_URL`
  - `MEDIASK_AI_API_KEY`
  - `MEDIASK_ENCRYPTION_KEY`
- Python AI 服务 `/api/v1/chat` 可用
- 本地用户种子包含：
  - `username = patient_li`
  - `password = patient123`

## 说明

- `department_id` 默认写成种子数据里的 `3103`，如果你本地不是这个值，直接改 `qingniao-env.toml`
- 本套件只断言 `T1` 契约中的关键字段，不绑定具体回答文本
