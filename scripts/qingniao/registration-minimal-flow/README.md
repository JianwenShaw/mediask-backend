# Registration Minimal Flow

用于验证当前最小挂号链路：

1. 患者登录
2. 查询固定种子数据对应的门诊场次
3. 创建挂号
4. 查询我的挂号
5. 验证非法状态参数返回 `1002`

## 运行

在当前目录执行：

```bash
cargo run --manifest-path /Users/catovo/dev/qingniao/Cargo.toml -- run registration-minimal-flow.toml --agent-output
```

## 前置条件

- `mediask-api` 已启动，默认地址 `http://localhost:8989`
- 本地数据库已加载 `sql/init-dev.sql`
- 种子数据包含：
  - `clinic_session.id = 4101`
  - `clinic_slot.id = 5101`
  - 日期 `2026-05-20`

## 已知风险

如果 `create registration` 失败，且数据库报 `registration_order.patient_id` 外键相关错误，说明当前运行环境里挂号写库使用的 `patientId` 口径与表约束仍不一致。这属于业务实现问题，不是这套 qingniao 用例本身的问题。
