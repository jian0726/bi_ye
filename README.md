# 简柚博客网站系统

一个个人向的内容记录站：文章、相册、音乐、留言板。前台是访客视角的阅读界面，后台在同一套前端里提供完整管理能力。

> 毕业设计项目 · 长沙南方职业学院 人工智能学院 软件技术专业

---

## 目录

- [技术栈](#技术栈)
- [功能概览](#功能概览)
- [快速开始](#快速开始)
- [配置说明](#配置说明)
- [项目结构](#项目结构)
- [数据库](#数据库)
- [接口约定](#接口约定)
- [脚本说明](#脚本说明)
- [部署](#部署)
- [已知限制](#已知限制)

---

## 技术栈

### 后端

| 类别 | 技术 | 版本 |
|---|---|---|
| 框架 | Spring Boot | 3.5.5 |
| 语言 | Java | 21 |
| ORM | MyBatis-Plus | 3.5.7 |
| 数据库 | MySQL | 8.0 |
| 缓存 | Redis | 7 |
| 认证 | JJWT（JWT） | 0.12.x |
| 对象存储 | MinIO | latest |
| 工具库 | Hutool / Lombok | — |

**没有使用 Spring Security**。鉴权由 `AuthInterceptor` + `AuthContext` 手工实现，JWT 由 `JwtUtil` 签发与校验；密码哈希单独引入 `spring-security-crypto` 使用 BCrypt。

### 前端

| 类别 | 技术 | 版本 |
|---|---|---|
| 框架 | Vue | 3.5 |
| 构建 | Vite | 5.4 |
| 语言 | TypeScript | 5.6 |
| 样式 | Tailwind CSS | 3.4 |
| 路由 | Vue Router | 4.4 |
| 状态 | Pinia | 2.2 |
| 请求 | Axios | 1.7 |
| 后台组件 | Element Plus | 2.14 |
| Markdown | markdown-it + highlight.js | 14 / 11 |

前台与后台**在同一个前端工程内**，后台页面位于 `src/views/admin/`，没有独立的管理端项目。

---

## 功能概览

### 前台（访客）

| 路由 | 页面 | 说明 |
|---|---|---|
| `/` | 首页 | 刊头 Hero + 精选 + 时间线列表 |
| `/articles` | 文章列表 | 分页、分类筛选 |
| `/article/:id` | 文章详情 | Markdown 渲染、阅读进度、点赞、收藏 |
| `/category/1` | 记录 | 独立版式 |
| `/category/2` | 游记 | 独立版式 |
| `/category/3` | 随笔 | 独立版式 |
| `/archive` | 归档 | 按年份时间线 |
| `/tag/:id` | 标签 | 按标签聚合 |
| `/search` | 搜索 | 标题与摘要的 LIKE 检索 |
| `/album` | 相册 | 瀑布流 + 大图弹层 |
| `/toolbox` | 百宝箱 | 音乐播放器等 |
| `/about` | 家 | 博主介绍 |
| `/message` | 留言 | 墨绿夜空 + 文字雨效果 |
| `/login` `/register` `/forgot` | 登录 / 注册 / 找回密码 | 手机号或邮箱登录 |
| `/profile` | 个人中心 | 资料、收藏、密码修改 |

### 后台（管理员）

`/admin` 下共 11 个模块：仪表盘、文章管理、文章编辑、分类、标签、留言、友链、文件、用户、相册、音乐。

---

## 快速开始

### 方式一：Docker 一键启动（推荐）

```bash
# 1. 准备环境变量
cp .env.example .env
# 打开 .env，把 CHANGE_ME_* 改成你自己的密码

# 2. 启动
bash scripts/deploy.sh                 # Linux / macOS / Git Bash
powershell -File scripts/deploy.ps1    # Windows PowerShell
```

启动后：

- 前台：http://localhost:5174
- 后端 API：http://localhost:8081/api
- MinIO 控制台：http://localhost:9001

首次启动会自动执行 `init.sql` 建表并灌入演示数据（17 篇文章、相册、留言、友链）。

#### ⚠ 从旧部署升级时：改了密码必须清数据卷

MySQL / Redis / MinIO 的密码只在**数据卷为空时**生效：

- MySQL 的 `MYSQL_ROOT_PASSWORD` / `MYSQL_PASSWORD` 只在 `/var/lib/mysql` 为空时写入账号
- MinIO 的 `MINIO_ROOT_USER` 只在 `/data` 为空时建立 root 账号
- Redis 的密码每次启动都生效（命令行参数），不需要重建

所以如果之前用旧密码跑过、现在改了 `.env`，后端会一直重启并报：

```
java.sql.SQLException: Access denied for user 'jianyou'@'172.28.0.2' (using password: YES)
```

两种解法：

```bash
# 解法一：丢弃旧数据，按新密码重建（演示/毕设场景推荐）
docker compose down -v && docker compose up -d

# 解法二：保留数据，把旧卷里的账号密码改成新值
docker exec -it jianyou-mysql mysql -uroot -p<旧root密码>
  ALTER USER 'jianyou'@'%' IDENTIFIED BY '<新密码>';
  ALTER USER 'root'@'%'    IDENTIFIED BY '<新root密码>';
  FLUSH PRIVILEGES;
# MinIO 只能用旧账号登录控制台后新建/改密，或同样清 /data
```

### 方式二：本地开发

需要本机已装好 JDK 21、Maven 3.9、Node 20+、MySQL 8、Redis 7（MinIO 可选，不传图片可不启）。

```bash
# 后端
cd blog-backend
mvn spring-boot:run

# 前端
cd blog-web
npm install
npm run dev          # http://localhost:5173，已配置 /api 与 /files 代理
```

本地开发无需配置环境变量，`application.yml` 中的默认值可直接使用。

---

## 配置说明

所有敏感配置通过环境变量注入，仓库中**不含任何真实密码**。

| 变量 | 必填 | 说明 |
|---|---|---|
| `MYSQL_ROOT_PASSWORD` | 是 | MySQL root 密码 |
| `MYSQL_PASSWORD` | 是 | 业务账号 `jianyou` 的密码 |
| `REDIS_PASSWORD` | 是 | Redis 访问密码（容器内已开启 `requirepass`） |
| `MINIO_ROOT_USER` | 是 | MinIO 账号 |
| `MINIO_ROOT_PASSWORD` | 是 | MinIO 密码 |
| `JWT_SECRET` | 是 | JWT 签名密钥，**至少 32 字符** |
| `VERIFY_DEV_MODE` | 否 | 默认 `false`。设为 `true` 时找回密码验证码会随接口返回，仅本地调试可用 |
| `BACKEND_PORT` | 否 | 后端对外端口，默认 8081 |
| `FRONTEND_PORT` | 否 | 前端对外端口，默认 5174 |

### 生产环境安全校验

容器以 `SPRING_PROFILES_ACTIVE=prod` 启动时，`SecurityGuard` 会在启动阶段校验配置，出现以下任一情况**直接拒绝启动**：

- `JWT_SECRET` 仍是默认值，或长度不足 32 字符
- `MYSQL_PASSWORD` / `MINIO_SECRET_KEY` 仍是默认值
- `VERIFY_DEV_MODE` 仍为 `true`

这是为了防止漏配时静默使用仓库里公开的弱口令。

### 端口暴露策略

MySQL（3307）、Redis（6380）、MinIO（9000/9001）均绑定在 `127.0.0.1`，仅本机可访问；容器之间通过 `jianyou-net` 内网互通。对外只暴露前端端口，API 与对象存储由 nginx 反向代理。

---

## 项目结构

```
.
├── blog-backend/                    后端
│   ├── src/main/java/com/jianyou/blog/
│   │   ├── common/                  统一响应、异常处理
│   │   ├── config/                  配置类（含 SecurityGuard 生产校验）
│   │   ├── controller/              10 个 Controller
│   │   ├── dto/ entity/ vo/         数据传输对象 / 实体 / 视图对象
│   │   ├── interceptor/             鉴权拦截器
│   │   ├── mapper/                  MyBatis-Plus Mapper
│   │   ├── service/                 业务逻辑
│   │   ├── task/                    定时任务（访问日志清理）
│   │   └── util/                    工具类（JWT、IP 归属地、访客标识）
│   ├── src/main/resources/
│   │   ├── application.yml          主配置
│   │   ├── db/init.sql              唯一建库脚本（15 表，可重复执行）
│   │   └── ip2region/               离线 IP 归属地库
│   └── Dockerfile
│
├── blog-web/                        前端（前台 + 后台同一工程）
│   ├── src/
│   │   ├── api/                     接口封装
│   │   ├── components/              通用组件
│   │   ├── composables/             组合式函数
│   │   ├── router/                  路由与守卫
│   │   ├── stores/                  Pinia 状态
│   │   ├── styles/                  设计令牌与全局样式
│   │   ├── views/                   页面（admin/ 为后台）
│   │   └── utils/
│   ├── nginx.conf                   生产环境反向代理配置
│   └── Dockerfile
│
├── docs/                            设计文档
│   ├── 01-选题与方案/               任务书、选题方案
│   ├── 02-设计文档/                 需求分析、系统架构、数据库设计、测试用例
│   └── 03-正文/                     毕业设计正文
│
├── scripts/                         运维与验证脚本
├── docker-compose.yml               五服务编排
└── .env.example                     环境变量模板
```

---

## 数据库

- **唯一建库脚本**：`blog-backend/src/main/resources/db/init.sql`（`docs/02-设计文档/init.sql` 为同步副本）
- 15 张表，所有表前带 `DROP TABLE IF EXISTS`，**可重复执行**
- 首次启动由 MySQL 容器自动执行一次

**重建数据库**（会清空所有数据）：

```bash
docker compose down -v
docker compose up -d
```

主要表：

| 表 | 说明 |
|---|---|
| `t_user` | 用户（手机号 / 邮箱唯一，无用户名字段） |
| `t_article` | 文章 |
| `t_category` / `t_tag` | 分类 / 标签 |
| `t_collect` | 收藏（用户 × 文章唯一） |
| `t_like_record` | 点赞（用户 × 目标唯一） |
| `t_message` | 留言 |
| `t_photo` / `t_music` | 相册 / 音乐 |
| `t_friend_link` | 友链 |
| `t_visit_log` | 访问日志（保留 90 天） |
| `t_view_record` | 浏览记录（访客 × 文章唯一，用于独立访客计数） |

---

## 接口约定

### 统一响应

```json
{ "code": 200, "message": "success", "data": {}, "timestamp": 1700000000000 }
```

### 状态码

| 场景 | HTTP 状态 | 响应 code |
|---|---|---|
| 成功 | 200 | 200 |
| 参数校验失败 | 200 | 400 |
| 未登录 / token 失效 | **200** | **401** |
| 无权限 | **200** | **403** |
| 服务端异常 | 500 | 500 |

> 鉴权失败返回的是 **HTTP 200 + 业务 code 401/403**，前端拦截器依据 `code` 判断，而不是 HTTP 状态码。

### 认证

双 Token 机制：

- `access`：2 小时，每次请求通过 `Authorization: Bearer <token>` 携带
- `refresh`：7 天，仅用于 `/auth/refresh` 换发新 access，jti 登记在 Redis，登出即撤销

---

## 脚本说明

| 脚本 | 用途 |
|---|---|
| `scripts/deploy.sh` / `deploy.ps1` | Docker 一键部署 |
| `scripts/tunnel-dev.ps1` | 把本地 Vite（5173）暴露到公网 |
| `scripts/tunnel-docker.ps1` | 把 Docker 前端（5174）暴露到公网 |
| `scripts/_ngrok-precheck.ps1` | 隧道脚本的前置检查（被上面两个点源引用） |
| `scripts/sync-ngrok-host.py` | 把 ngrok 域名同步进 `vite.config.ts` 的 allowedHosts |
| `scripts/test-readonly.py` | 只读接口测试（无需登录） |
| `scripts/test-authed.py` | 需登录接口测试 |
| `scripts/test-retest.py` | 回归测试 |
| `scripts/verify-def01.py` | 验证缺陷 DEF-01 已修复 |
| `scripts/verify-testcase-stats.py` | 反算测试用例统计 |
| `scripts/make-toc-dynamic.py` | 重建正文动态目录 |
| `scripts/replace-shots.py` | 替换正文截图 |
| `scripts/_cleanup-run.sql` | 数据清理 |
| `scripts/run-backend.cmd` | Windows 本地启动后端 |

---

## 部署

### 内网穿透（本机对外演示）

```powershell
# 先启动前后端，再开隧道
.\scripts\tunnel-docker.ps1
```

首次使用需先配置 ngrok token：`ngrok config add-authtoken <你的token>`。

> 若报 `ERR_NGROK_9009`，说明当前终端带有 `http_proxy` 等代理环境变量，而 ngrok 免费版禁止走代理。隧道脚本已内置清理逻辑，直接用脚本运行即可。

### 常规服务器部署

1. 准备 `.env`（参考 `.env.example`）
2. `docker compose up -d --build`
3. 前置一层 nginx 或云负载均衡处理 HTTPS，转发到前端端口

---

## 已知限制

- **全站无评论功能**，互动由留言板承担（留言不支持回复）
- **找回密码未接入真实短信/邮件通道**，生产环境下该功能实际不可用
- 全文检索为 `title + summary` 的 LIKE 匹配，不支持全文索引
- 文章版本历史未实现
- 后端暂无单元测试（`src/test` 为空），验证依赖 `scripts/` 下的接口测试脚本与 GitHub Actions 构建校验
- 登录接口无失败锁定与限流；CORS 仍为 `*`；未内置 HTTPS 证书

## 自动化检查

`.github/workflows/ci.yml` 在每次 push / PR 时跑三项：

| Job | 内容 |
|---|---|
| Backend | JDK 21 + Maven 打包（`target/blog-backend.jar`） |
| Frontend | Node 22 + `npm ci` + `vue-tsc` 类型检查 + `vite build` |
| Docker build check | 两个 Dockerfile 在干净 Ubuntu 环境中构建（不推送） |

三项全绿才认为合并安全。
