# MediAsk Backend

## 容器快速开始

当前项目使用 Docker Compose 运行本地基础设施：

- `postgres`：PostgreSQL 17 + `pgvector`
- `redis`：Redis 7

启动前请先确保这些环境变量已经在 shell 或 `.env` 中提供：

- `PG_PASSWORD`
- `REDIS_PASSWORD`

首次构建自定义 PostgreSQL 镜像：

```bash
docker compose build postgres
```

启动全部容器：

```bash
docker compose up -d
```

查看状态：

```bash
docker compose ps
```

查看日志：

```bash
docker compose logs -f postgres
docker compose logs -f redis
```

停止容器但保留数据：

```bash
docker compose down
```

## `build` 是什么

`docker compose build postgres` 会重新构建 `docker/postgres/Dockerfile` 定义的 PostgreSQL 镜像。

只有在以下情况才需要重新 `build`：

- `docker/postgres/Dockerfile` 变了
- `docker-compose.yml` 里 PostgreSQL 镜像相关配置变了
- 你想强制重建一个干净镜像

以下情况通常不需要重新 `build`：

- 只改了 `sql/` 里的 SQL 文件
- 只改了 Java 代码
- 只是想重新启动容器

日常开发里最常见的启动命令通常就是：

```bash
docker compose up -d
```

## PostgreSQL 初始化机制

PostgreSQL 使用官方 entrypoint 初始化机制。

- `docker/postgres/initdb/00-init-dev.sql` 是唯一顶层入口文件
- 它会加载 `/mediask-init/sql/init-dev.sql`
- 完整初始化 SQL 位于项目根目录 `sql/`

需要特别注意：初始化 SQL 只会在 PostgreSQL 数据目录为空时自动执行。

这意味着：

- 第一次使用全新数据卷启动时，会自动跑初始化 SQL
- 之后如果继续沿用同一个数据卷，重启容器不会再次自动跑初始化 SQL

当前 Docker 命名卷：

- PostgreSQL：`postgres-data`
- Redis：`redis-data`

## 什么时候重启、重建、删卷

使用 `docker compose restart` 的场景：

- 只是想重启正在运行的容器
- 运行中的服务需要简单重启

使用 `docker compose down && docker compose up -d` 的场景：

- 想完整重启容器
- 怀疑容器网络或启动状态有问题
- 改了 compose 运行参数，但没有改镜像构建逻辑

使用 `docker compose build postgres && docker compose up -d postgres` 的场景：

- 改了 PostgreSQL Dockerfile
- 改了 `pgvector` 或 PostgreSQL 镜像层安装方式

使用 `docker compose down -v` 的场景：

- 想把 PostgreSQL 和 Redis 数据全部清空重来
- 想让 PostgreSQL 初始化 SQL 从头自动再执行一遍
- 改了 schema，而且是破坏式调整，希望本地重新初始化

不要使用 `docker compose down -v` 的场景：

- 你还想保留本地数据库数据
- 你只改了应用代码
- 你只是想简单重启服务

## 常见工作流

### 1. 第一次本地启动

```bash
docker compose build postgres
docker compose up -d
```

### 2. 保留数据的情况下重启服务

```bash
docker compose down
docker compose up -d
```

### 3. 改了 PostgreSQL Dockerfile 后重建镜像

```bash
docker compose build postgres
docker compose up -d postgres
```

### 4. 完全重置本地数据库并重新执行初始化 SQL

```bash
docker compose down -v
docker compose up -d
```

## 数据库检查

进入 PostgreSQL 容器内的 `psql`：

```bash
docker compose exec postgres psql -U mediask -d mediask_dev
```

常用检查命令：

```sql
\dn
\dt public.*
\dt audit.*
\dx
```

正常情况下应重点看到：

- schema：`public`、`audit`、`event`
- 扩展：`vector`
- `audit` schema 下的审计表

## Redis 检查

进入 Redis CLI：

```bash
docker compose exec redis redis-cli -a "$REDIS_PASSWORD"
```

基础连通性检查：

```bash
docker compose exec redis redis-cli -a "$REDIS_PASSWORD" ping
```

## 说明

- 修改 `sql/` 后，不会自动重新应用到一个已经存在的 PostgreSQL 数据卷
- 如果你改了初始化 SQL，且希望从头生效，最直接的方式是重置卷再启动
- 目前直接使用容器内的 `psql` 就够了，不一定需要在 macOS 本机额外安装 PostgreSQL 客户端
