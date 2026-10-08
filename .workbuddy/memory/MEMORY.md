# 项目长期记忆 — 毕业设计「简柚个人博客系统」

## 项目定位

| 项 | 值 |
|---|---|
| 题目 | 简柚个人博客系统的设计与实现 |
| 学生 | 简之航　学号 202420330504　班级 软件技术2-2405 |
| 学校 | 长沙南方职业学院 人工智能学院 |
| 选题类型 | A、产品设计 |
| 指导教师 | 待定（参考资料案例为陈将） |

## 技术栈（已锁定，勿擅改）

**后端**：Spring Boot 3.5.5、JDK 21、MyBatis-Plus 3.5.7、MySQL 8.0、Redis 7.x、
**无 Spring Security**（仅引 `spring-security-crypto` 用 BCrypt；鉴权 = `AuthInterceptor` + `AuthContext`，
越权响应为 **HTTP 200 + code=401/403**）、JJWT 0.12.x、MinIO、Hutool、Lombok

**前端**：前台 + 后台**同仓** `blog-web/`（Vue 3.4 + Vite 5 + TS + Tailwind 3.4；
后台页面在 `src/views/admin/`，Element Plus 2.7，**无独立 `blog-admin/`**）；
共享 `styles/tokens.css` 设计令牌层

**DevOps**：仓库内只有 **`docker-compose.yml`**（mysql/redis/minio/backend/frontend 五服务）。
Jenkins、registry:5000、Prometheus/Grafana、Loki、certbot 属**后续规划**，仓库无对应文件

## 关键业务约定

- **定位：个人成长记录本**（非作品集）——措辞用中性记录式（"最近写的""按时间翻"），禁推荐/精选/营销类文案
- **注册登录（2026-09-22 定稿，勿回退）**：**全站无用户名字段**（`t_user` 无 username 列）；
  唯一登录标识 = 手机号 / 邮箱（至少填一个，uk_phone/uk_email 唯一），`LoginDTO.account` 只匹配 phone OR email；
  注册 = 手机号/邮箱 + 昵称 + 密码；站长 `13800000001` / `jianyou@local.dev`（DataLoader 以手机号/邮箱为锚点）；无 OAuth
- **会话：双 Token（2026-10-07 落地，勿回退成单 Token）**：
  access 2h（`jwt.access-hours`，带 `role`）+ refresh 7d（`jwt.refresh-days`，带 `jti`、不带角色）；
  两者带 `type` claim **不可混用**，请求凭证**只认 access**（`AuthInterceptor` 用 `parseAccess`）；
  refresh 的 jti 登记 Redis `blog:auth:refresh:{jti}`（TTL 7d，**删除即撤销**），未在册 jti 一律拒绝；
  `POST /auth/refresh` **轮换制**（撤销旧 jti 再发新对）且**回库读最新 `role`/`status`**（降权/禁用立即生效）；
  `POST /auth/logout` 撤销；**不做 access 黑名单**（靠 2h 短效期收敛）。
  前端 `request.ts` 401 单飞刷新（模块级 Promise）+ 重放（`_retried` 防循环），**成功分支必须覆盖 localStorage 的新 refresh**
- **权限三级**：游客 / 注册用户 / 管理员（`user.role='ADMIN'`，无独立表）
- **全站无评论功能**（无表、无 Controller、无页面），互动由留言板承担
- **用户数据隔离**：收藏 `t_collect`（uk_user_article）+ `AuthContext.getUserId()`；
  接口 `POST/DELETE /portal/articles/{id}/collect`、`GET /{id}/collected`、`GET /portal/articles/me/collections`；未登录 401
- **点赞按账号落库（2026-09-23，勿回退成 IP 方案）**：`t_like_record`（user_id, target_id, target_type,
  uk_user_target + idx_target）+ `LikeService`（DuplicateKeyException 兜底，like_count 双写）；
  VO 带 `liked` 一次回显（未登录恒 false），刷新后红心保持；**点赞需登录**（游客 401 → 引导登录）；
  原 Redis `blog:like:*` / `blog:comment:like:*` 已废弃
- **留言板无回复能力（2026-10-05 定稿，勿回退）**：`t_message` 无 `reply`/`reply_time` 列，后端无回复接口/DTO；
  后台筛选参数是 **`ipLocation`**（不是 `replied`）。定位「一次性留言」
- **留言**：后端不要求登录（`user_id` 恒 NULL，IP 属地入 `ip_location`）；但**前端未登录时不调接口**——
  只在本页飘落一次，刷新即散。昵称缺省「访客」（页面无昵称/邮箱输入框）；
  content ≤300（前端 maxlength=60）/ 昵称 ≤20 / 邮箱 `@Email`；无限流
- **分类仅 3 个**：记录=1、游记=2、随笔=3（**无「技术」分类**）；`/articles` 重定向首页
- **浏览量（2026-10-05 晚改造）**：口径 = **独立访客数**——先 `INSERT IGNORE` 写 `t_view_record`
  （`uk_article_visitor(article_id, visitor_key)`），**影响行数为 1 才** `view_count + 1` 原子自增；
  访客标识 = 登录 `u:{userId}` / 游客 `ip:{ClientIP}`（`util/VisitorKey`）；
  **同一访客对同一文章终身只计一次**。三处浏览数已统一为 `GET /portal/site/stats` 的 `SUM(view_count)`
  （后台仪表盘、首页刊底、关于页）
- **Redis 两类用途（2026-10-07 起）**：①找回密码验证码 `blog:verify:*`；②refresh 撤销登记 `blog:auth:refresh:{jti}`。
  浏览量/热门/限流/access 黑名单**均不经 Redis**。
  ⚠ 排查统计异常时先看历史残留键 `blog:like:*` / `blog:comment:like:*` / `blog:collect:*`（代码已改走 MySQL）
- **IP 归属地与访问统计（2026-10-06 生产化改造，勿回退）**：
  - IP → 省份走 **ip2region 3.3.7 离线 xdb**（`resources/ip2region/ip2region_v4.xdb` 10.6MB 内置，
    启动载入内存二分查找，零外部请求）；埋点与留言属地共用
  - **「今日访问」= 当日独立访客数**（`COUNT(DISTINCT visitor_key)`，勿当请求数）；省份分布同口径，各省之和 = 总数；
    `t_visit_log.visitor_key VARCHAR(64)`（3307 已于 2026-10-07 补齐 + 32 行历史回填 `CONCAT('ip:', ip)`）
  - **XFF 可信代理白名单**：`app.trusted-proxies`（默认 `127.0.0.1,::1`；生产改反代内网，`APP_TRUSTED_PROXIES` 覆盖）；
    非白名单来源的 XFF 一律忽略
  - `t_visit_log` 定期清理：`app.visit-log.retention-days: 90` + `purge-cron`（每日 4 点），`VisitLogPurgeTask` 分批删
  - 埋点执行器为**有界队列（2000）+ 丢弃计数**，宁可丢埋点不阻塞业务
- 列表 `size` 超上限**静默裁剪**为 50；`page=abc` → 400
- **上传体积口径（勿再出现 25MB）**：图片 ≤5MB、头像 ≤2MB、音频（mp3/flac）≤1028MB；
  容器层 `max-file-size: 1028MB`、`max-request-size: 2056MB`（**2056 是请求体上限，不是音频上限**）。
  三处同步：`application.yml`、`StorageService.MAX_AUDIO_SIZE` + 错误文案、前端 `MusicManageView.vue`
- 全文检索用 `title` + `summary` 的 `LIKE`（不含正文，ngram 未落地）；图片存储用 MinIO
- 文章版本历史未实现（`t_article_history` 未建）

## 部署与数据库

- **`db/init.sql` 是唯一建库脚本（2026-10-07 合并定稿）**：已并入历史全部 10 个 `upgrade-*.sql` 的变更，
  每表前带 `DROP TABLE IF EXISTS` → **可重复执行**；**393 行 / 15 表 / 91 INSERT**。
  原 upgrade 脚本已删除，备份在 `db/_backup-before-merge/`（**勿删**）。
  两份副本（`blog-backend/src/main/resources/db/` 与 `docs/02-设计文档/`）改一处必须同步另一处。
  **对已存在的库要重建只能清空数据卷**：`docker compose down -v && docker compose up -d`
  （compose 只挂 `/docker-entrypoint-initdb.d/`，空卷才执行）
- **Docker 库（3307）状态**：结构与 3306 完全一致（106 列 + 43 索引 diff 为空），
  差异仅运行数据（`t_view_record` / `t_visit_log` 不预置）。
  `t_visit_log` 含 `visitor_key`；`t_message` 已无 reply/reply_time；15 表。
  - **一键部署（2026-10-07 落地，可用）**：`scripts/deploy.ps1` / `scripts/deploy.sh`
    （内置 `BACKEND_PORT=8081 FRONTEND_PORT=5174`，支持 `-Rebuild`/`--rebuild`、`-Down`/`--down`，
    起后轮询 `/api/portal/site/stats` 直到 200）。也可直接
    `BACKEND_PORT=8081 FRONTEND_PORT=5174 docker compose up -d`
  - 镜像：`bi_ye_she_ji-backend:latest` / `bi_ye_she_ji-frontend:latest`（**2026-10-07 已重建为最新代码**，
    含双 Token + VisitorKey + ip2region + VisitLogPurgeTask + ClientIpConfig；
    **容器内 jar 与镜像层 jar md5 一致**，此前「docker cp 进去、镜像里是旧的」状态已消除）
  - 跑 Docker 必须带端口：`BACKEND_PORT=8081 FRONTEND_PORT=5174 docker compose up -d --build`
    ——`--force-recreate` **不继承环境变量**，漏写则回退 8080（被其他项目 Jenkins 占）且 backend 被连带重建，
    容器内 jar 会回退成镜像内的版本（`docker cp` 的改动全部丢失）⚠
  - `blog-web/nginx.conf` 已补 `client_max_body_size 2056m`（对齐后端 max-request-size，
    原缺该项时 nginx 默认 1m → Docker 下 >1MB 上传全 413，实测 3MB 上传修复后 200）
  - **自定义网络 `jianyou-net`（subnet `172.28.0.0/24`）五服务钉死固定 IP**：
    backend `.2` / frontend `.3` / mysql `.11` / redis `.12` / minio `.13`。
    用途：后端 XFF 白名单只能认「前端 nginx 那一个 IP」`172.28.0.3`
  - **⚠ 规律 R27：Docker 里 XFF 白名单绝不能写网段**——Docker 网关（`172.x.0.1`）必在网段内，
    而宿主机经端口转发直连后端时 `remoteAddr` 就是网关 IP → 写网段 = 对宿主机开放 XFF 伪造
    （实测直连 8081 发 `XFF: 8.8.8.8` 被采信写库）。正解：钉固定 IP（`IpUtils` 也支持 CIDR 写法备用）
  - backend 生产化环境变量已写进 compose：`APP_TRUSTED_PROXIES=127.0.0.1,::1,172.28.0.3`、
    `VERIFY_DEV_MODE=false`、`JWT_SECRET`、`VISIT_LOG_RETENTION_DAYS/PURGE_CRON`
- 访问方式：`docker exec jianyou-mysql mysql -uroot -p<env> ...`；root 密码 `docker exec jianyou-mysql printenv MYSQL_ROOT_PASSWORD`（= `root2026`），
  库用户密码 `printenv MYSQL_PASSWORD`（= `jianyou2026`）——**凭据一律 printenv 读，勿猜**

## 前台视觉（已定稿，勿回退成"安全"样式）

- 用户否掉两版：v1 Apple 风玻璃卡片（"撞车模板"）、v2 浅色杂志风（"不惊艳"）
- **定稿 = 墨色刊头 + 暖纸正文**：首屏全屏深墨绿 #101714（`--ink-*`）+ 反白衬线大字 + 竖排幽灵刊名视差；
  横滑"最近落笔"卡片带；浅色区 暖纸白 #faf9f7 + 柚橙 #d95d18 主色 + 墨绿 #2f5d50 辅色；
  `--font-display` 衬线（Noto Serif SC），标题/序号/月份衬线、正文无衬线
- 默认**浅色主题**（用户明确不要黑色基调），暗色仅手动切换
- 首页三组件 `MagazineHero.vue` / `FeaturedEditorial.vue` / `TimelineList.vue`
- **版心宽度不变量：`--shell-max` 下限必须是绝对长度 `1120px`，绝不可写 `100%`**
  （`clamp(100%, 88vw, 1560px)` 时结果恒 ≥ 视口宽，`max-width` 被架空 → 版心塌成全宽、留白消失）。
  正解：`clamp(1120px, 88vw, 1560px)`

## 前台信息架构

- 导航 8 项**纯文字无图标无 emoji**（用户两次要求），顺序即代码顺序：
  今日(/) / 记录(/category/1) / 游记(/category/2) / 随笔(/category/3) / 相册(/album) / 百宝箱(/toolbox) / 家(/about) / 留言(/message)
- **游记/随笔/记录是三个独立页面**（TravelView/EssayView/RecordView），版式各异，绝不共用列表模板；
  `/category/:id` 兜底走 CategoryView；旧链接 `/articles` 重定向首页
- **全站统一节奏 = 墨色刊头（PageMasthead）+ 暖纸正文**；用户明确「不存在特色页，风格不一就是不统一」
- ArticleCard 无封面时自动生成渐变题图（四组杂志色 + 衬线大字）
- 相册 = CSS columns 瀑布流 + Teleport 大图弹层；百宝箱 = 工具卡三组 + 友链并入
- 留言板 = **墨绿夜空 + canvas 文字雨**（2026-10-04 定稿，`MessageView.vue` 730 行）：
  纯 CSS+SVG 墨绿夜空；整句字幅缓缓飘落，随机横位/落速/延迟，淡入淡出，飘出后回收；
  **鼠标左右移动控制风向**（带惯性微倾）。用户否掉列表式(v1)/便签墙(v2)/黄昏弹幕墙(v3)
  ——**做参考站必须先真实渲染确认设计再动手**
- 留言页 100dvh 不滚动：DefaultLayout 对 /message 隐藏 Footer/BackToTop，AppHeader overInk 恒亮
- 异步数据页面（Home/Message/Toolbox）加载后都要 `await nextTick(); requestAnimationFrame(refresh)` 重观察 .reveal

## Windows 调试规律（重要）

- **cmd.exe 会剥掉参数中双引号、`&&`、`||`、`<`、`>`**：浏览器 eval 须只用单引号 + `.split(' ').forEach`
  + 三元嵌套；写成 .mjs 用 Node spawnSync 单进程执行（`scripts/shot.mjs` 路线，
  agent-browser daemon 每次调用后被杀，不可跨调用复用）
- **Vue 页面模板必须单根**：根级注释或多根 fragment 会让 `<Transition mode="out-in">` 交接卡死
  → 切页后全站空白（留言页/搜索页都栽过）
- **沙箱会向子进程注入随机 `SERVER_PORT`**（Spring relaxed binding 使其优先于 jar 内 yml）→
  后端报「Tomcat initialized with port 62969 / already in use」。**解法：命令行参数优先级最高**：
  `D:\Java21\bin\java.exe -Dfile.encoding=UTF-8 -jar target/blog-backend.jar --server.port=8080`
- **沙箱禁止嵌套 spawn**：`spawnSync` 拉 npx/node 报 `EBUSY`，截图类工具在本环境不可用
- **沙箱对认证类操作会触发安全审批**：命令行出现密码、或调用注册/登录接口 → `SENSITIVE_APPROVAL=TIMED_OUT`，
  **超时后禁止重试**。做认证验证应优先**产出可交付脚本**（`scripts/smoke-double-token.sh|ps1`），而非反复直接执行
- **同一 docx 文件禁止并发读写**：两条命令同时 `Document(P)` + `save()` 会导致 zip 损坏（`phys_pkg.blob_for` 报错），必须串行
- **删 SQL/脚本后必须全仓 grep 脚本名**：本次合成 init.sql 后残留 7 处引用（compose 注释、目录树、
  审计报告、两份 README）；且**审计报告里的 `init.sql:行号` 会全部失效**，需重定位
- **`docker compose --force-recreate` 不继承环境变量**，且会连带重建 `depends_on` 的下游容器；
  只要用过端口覆盖，之后每条 compose 命令都必须重复写全（漏写→端口回退默认→服务 502）
- **判断容器内 jar 是否最新**：比对 mtime + 字节数，不要相信「刚 build 过」；
  `docker cp` 改过的容器一旦被 recreate 立即回退成镜像内版本（本次导致 refresh 接口 404）
- **合成/重建 SQL 必须以线上真实库 `mysqldump` 产物为基准**，不得在旧文件上手改
  （旧 init.sql 含 `'ip:N'` 脏列值，列数与值个数不匹配，导入必报错）。
  校验三件套：**列/索引 diff + 逐表行数对比 + 幂等二次执行**
- JDK 必须用 `D:\Java21\bin\java.exe`（默认 `java` 是 8，class 52 → UnsupportedClassVersionError）

## 文档规范（学院要求）

- **文档基准 = 线上真实表结构与代码**：数据库设计文档以 `mysqldump --no-data` 为准；
  「设计里有、代码里没有」的功能一律移入未落地清单并写取舍理由
- **未落地清单唯一出处**：`选题方案与技术方案.md` 5.6 与 `需求分析.md` 9.2，
  两份必须逐项对齐（**2026-10-07 起均为 12 项**——「双 Token」已实现移出）
- **`docs/01-选题与方案/任务书.txt` / `.docx` 是学院已签发官方文件，不得改动**
- 正文字体：正文标题宋体小三加粗居中；一级标题宋体小四加粗；正文宋体五号；
  表格内容五号宋体；图/表标题五号黑体
- 正文结构：1.背景 → 2.设计思路 → 3.设计内容 → 4.小结 → 参考文献 → 附录；
  第 3 章须含功能模块图、系统流程图、数据库分析（含 ER 图）、表设计、逐功能截图 + 关键代码；
  参考文献 ≥5 篇（近 3 年）；正文约 1.5 万字
- **正文现状**：`docs/03-正文/毕业设计正文-简柚个人博客系统的设计与实现.docx`
  = **526 段 + 15 表 + 27 图**（图3.1~3.27 连续）；后台 11 项全有小节（3.3.13~3.3.23）；
  图3.3 ER 图、图3.19 留言管理界面均已换新版；备份 4 份（修订前 / 热门清理前 / 移除留言回复前 / **双Token同步前**）。
  **2026-10-07 已同步双 Token**：段 91（架构总述）、317（登录页）、318（由「无状态、不保存会话」重写为
  「短效 access + 长效 refresh + 服务端登记 + 轮换 + 换发回库读角色」）、319（新增有效期取舍段）、
  455/460（代码节选改 `jwtUtil.parseAccess`）；脚本 `scripts/patch-thesis-double-token.py`（幂等）。
  ⚠ 待做：Word 中 **Ctrl+A → F9** 更新目录页码；
  ⚠ 未同步：docx 版设计文档（需求分析/选题方案/系统架构/数据库设计）md 已改、docx 待重新生成
- **`测试用例.md` v3.5（2026-10-07）**：**14 模块 TC-A~TC-N 共 207 条**（正常 110 / 异常 56 / 边界 41）；
  统计数字全部由 `scripts/verify-testcase-stats.py` 从正文行反算，不得手填；
  文首「编写依据」逐条标注代码出处，「全局事实」表列关键口径（无 Spring Security / HTTP 200+code /
  双 Token / 密码 6–32 / Redis 两类用途 / 无技术分类）；末尾章列与旧版差异。
  **教训：写用例必须逐条核对 DTO 注解与 Service 实现（并搜「有没有第二个同类实现」），
  不得从需求分析转写或凭常识补全**
- **⚠ 工作区可能有多会话并发写同一批文档**：写完必须回读 mtime + 内容校验；
  发现被覆盖**不要盲目重写**（会互相覆盖），先核对现状再做增量校准
- **docx 增量修订操作规程**（踩过坑）：
  - 模板段落定位必须排除目录区：`p.style.name.startswith("toc")` 过滤
  - `docx.paragraphs` 每次访问都重建列表，**禁止对其调 `.index()`**，先缓存 `_cache = d.paragraphs`
  - 插入类操作**必须先 `d.save()` 落盘，再重新 `Document(P)` 打开复查**
  - 标题段：`Normal` + 宋体 + 152400（小四）；正文段：`Normal` + 宋体 + 133350（五号）+ 首行缩进 266700；
    图题：`Normal` + 黑体 + 133350

## 工作空间与工具

- 目录：`docs/01-选题与方案/`、`02-设计文档/`、`03-正文/`、`04-答辩/`、`05-源代码/`、`06-生产化审计/`
- 环境：Windows + Git Bash（ls/cat 等基础命令不可用，须用 PowerShell 或专用工具）；提取 .doc 用 pywin32 + Word COM
- **Python 路径**：`C:/Users/32916/.workbuddy/binaries/python/versions/3.13.12/python.exe`
- **JDK 21** `D:\Java21`（JAVA_HOME 已设）；**Maven 3.9.9** `D:\apache-maven-3.9.9`（已配 aliyun 镜像）
- 后端编译：`cd blog-backend; mvn compile -DskipTests`（输出落盘再 Read，PowerShell stdout 不稳）
- Docker Desktop 已装且**可正常使用**（2026-10-07 验证：Docker 29.7.2，五个 jianyou-* 容器在跑）

## 交付物清单

1. 选题方案与技术方案 ✅
2. 毕业设计任务书（txt + docx）✅
3. 数据库设计文档（含 ER 图 + **15 张表**）✅
4. 功能结构图与流程图 ✅
5. 毕业设计正文 — 初稿完成（526 段 + 15 表 + 27 图；剩目录页码 F9 更新与终校）
6. 答辩 PPT — 待办
7. 完整源代码（前后台同仓 + 服务端 + docker-compose）— 在仓内，待终验

## 生产化审计（2026-10-06 产出，2026-10-07 部分整改）

- **报告**：`docs/06-生产化审计/生产化差距审计报告.md`（P0×6 / P1×10 / P2×11 / 已达标×13 + 三批整改路线）
- **P0 六项未整改**：①Redis 无密码暴露 ②MySQL/MinIO 端口暴露 ③密钥明文进仓库 ④**无 git** ⑤nginx 无
  `client_max_body_size`（Docker 下 >1MB 上传全 413）⑥验证码 dev-mode 回传明文
- **P1-1 已整改**（双 Token 会话，见上文）；其余 P1 未动：登录无锁定/无限流、无 HTTPS 与安全头、
  CORS `*` 无 profile 开关、无可观测性（Actuator/healthcheck/日志轮转）、无容器资源上限与 JVM 内存参数、
  零备份、迁移无 Flyway、上传无魔数校验
- 毕设衔接：报告第七节给了「不足与展望」认领口径，答辩可引用
