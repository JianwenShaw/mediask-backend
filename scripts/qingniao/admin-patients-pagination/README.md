# Admin Patient Pagination QN Suite

这个目录放管理员患者分页接口的 `qingniao` 手工验证文件。

运行前提：

- 本地 `mediask-api` 已启动在 `http://localhost:8989`
- 可用管理员账号为 `admin / admin123`
- PostgreSQL、Redis 已就绪

运行方式：

```bash
cd scripts/qingniao/admin-patients-pagination
cargo run --manifest-path /Users/catovo/dev/qingniao/Cargo.toml -- run admin-patients-pagination.toml
```

当前 suite 覆盖：

- 登录获取 token
- `GET /api/v1/admin/patients` 默认分页
- `GET /api/v1/admin/patients?pageNum=1&pageSize=2` 显式分页参数
- `GET /api/v1/admin/patients?pageSize=101` 非法分页参数返回 `400`
- 登出清理登录态
