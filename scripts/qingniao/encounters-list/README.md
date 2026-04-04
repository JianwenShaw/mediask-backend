# Encounters List

用于验证 T4 医生接诊列表最小链路：

1. 患者登录
2. 创建挂号
3. 医生登录
4. 医生查询 `GET /api/v1/encounters?status=SCHEDULED`
5. 医生查询不带 `status` 的全部可见接诊
6. 验证患者访问被拒绝
7. 验证非法状态参数返回 `1002`

## 运行

在当前目录执行：

```bash
cargo run --manifest-path /Users/catovo/dev/qingniao/Cargo.toml -- run encounters-list.toml --agent-output
```

## 前置条件

- `mediask-api` 已启动，默认地址 `http://localhost:8989`
- 本地数据库已加载 `sql/init-dev.sql`
- 种子数据包含：
  - 患者账号 `patient_li / patient123`
  - 医生账号 `doctor_zhang / doctor123`
  - `clinic_session.id = 4101`
  - `clinic_slot.id = 5102`
  - 日期 `2026-05-20`

## 已知风险

如果 `create registration for encounter` 因号源已占用失败，需要先重置本地数据库，或调整环境文件中的 `clinic_slot_id` 到当前仍可用的号源。
