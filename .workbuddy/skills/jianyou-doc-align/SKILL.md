---
name: jianyou-doc-align
description: 把「简柚个人博客」毕业设计文档与真实代码/数据库对齐。当需要核对或修订 docs/ 下的需求分析、数据库设计、系统架构、功能结构图、选题方案，或用户抱怨「文档和系统对不上」「答辩怕被问」，或要写正文/PPT 需要真值时使用。
agent_created: true
---

# 简柚毕设文档 ↔ 实现对齐

## 铁律

1. **唯一基准是线上真实表结构与代码**，不是文档之间互相引用。任何"设计里有、代码里没有"的功能，
   一律移入未落地清单并写取舍理由，**不隐藏、不虚构**。
2. **`docs/01-选题与方案/任务书.txt` / `.docx` 是学院已签发的一式三份官方文件，绝不改动**。
   与它的差异只在 `选题方案与技术方案.md` 5.6 与 `需求分析.md` 9.2 里说明，两份清单必须逐项对齐。
3. `docs/02-设计文档/init.sql` 是 `blog-backend/src/main/resources/db/init.sql` 的副本，
   **改一处必须同步另一处**（用 Copy-Item，改完比对行数）。
4. 文档里出现的每个技术名词都要能在代码里 grep 到；grep 不到就标注为「未实现 / 未采用 / 预留」。

## 第一步：取真值（不要凭记忆写）

```powershell
# 真实表结构（唯一权威）
& 'D:\mysql\bin\mysqldump.exe' --host=127.0.0.1 --port=3306 -ujianyou -pjianyou2026 `
  --no-data --skip-comments --compact jianyou_blog | Out-String `
  | Set-Content shots/_schema-real.sql -Encoding utf8

# 真实目录树（后端/前端/项目根）
$root='...\blog-backend\src\main\java\com\jianyou\blog'
foreach ($d in @('common','config','interceptor','controller','service','mapper','entity','dto','vo','util')) { ... }
```

取真值要点：
- 控制器真实路径与 HTTP 方法：grep `@RequestMapping|@(Get|Post|Put|Delete)Mapping`
- 依赖真实版本：读 `pom.xml` 的 `spring-boot-starter-parent` 版本、`blog-web/package.json` 的 dependencies
- 错误码真实分支：读 `GlobalExceptionHandler` + `BusinessException` 用法
- 关键"事实点"必须逐个 grep 确认，别猜：搜索是 `LIKE` 还是 ngram？浏览量是 Redis 累加还是直接 `+1`？
  JWT 有效期几小时？留言墙游客提交落不落库？

## 第二步：按真值改写，逐处标注

- 技术栈表：**把实际未引入的组件单独列一条「未引入 + 理由」**，比悄悄删掉更有说服力（答辩能用）。
- 架构图：类名必须真实存在（本项目**没有** `AdminInterceptor`、`JwtAuthenticationFilter`、
  `OperationLogAspect`；只有 `AuthInterceptor` + `VisitLogInterceptor`，见 `WebConfig`）。
- 目录结构：整棵树照抄真实结构，别写"规划中"的目录（本项目**没有** `blog-admin/`、`deploy/`；
  后台是 `blog-web/src/views/admin/` + `AdminLayout.vue`）。
- 功能清单：已实现 / 未落地分列，**不用"部分实现"含糊带过**。

## 第三步：查残留（必做）

改完必须全量扫描 `docs/`，确认没有过时表述：

```
grep -rn "Apple 风格|毛玻璃|collect_record|view_log|article_history|sensitive_word|operation_log|
blog-admin|blog-server|Druid|Knife4j|Hutool|双 Token|429|ngram|IP 去重|验证码" docs/
```

注意：
- `docs/02-设计文档/UI设计图/*.html` 里的 ngram / Refresh Token 是**演示文章的正文内容**，不是设计声明，不要改。
- `任务书.txt` 里的 ngram / Redis 累加是**任务书原文**，不要改。

## 权限与互动的真实口径（易错，勿回退）

| 动作 | 是否需要登录 | 说明 |
|---|---|---|
| 发表评论 | 否 | 游客填昵称，`t_comment.user_id = NULL`，落库 |
| 留言墙发弹幕 | **否，但不落库** | 游客提交只在前端本页飘一次（`MessageView` 未登录分支）；登录才调 `POST /portal/messages` |
| 申请友链 | 否 | `FriendLinkService.apply` 不校验登录，`status = 0` 待审 |
| 点赞（文章 / 评论） | **是** | `t_like_record` 唯一索引去重，游客 401 → 前端跳登录 |
| 收藏 | **是** | `t_collect` 唯一索引，游客 401 |

## 未落地清单（13 项，两份文档都要有）

ngram 全文搜索 · 浏览量 Redis 缓冲+定时落库 · 评论敏感词过滤 · 文章版本历史与回滚 ·
后台操作日志 · 短信/邮箱验证码 · 双 Token 与退出黑名单 · 文章定时发布 · 接口限流 ·
相册分组 · 评论先审后发开关 · 用户重置密码 · 数据看板图表

出处：`docs/01-选题与方案/选题方案与技术方案.md` 5.6、`docs/02-设计文档/需求分析.md` 9.2。

## 输出规范

- 每次改完在 `docs/` 涉及的文件头更新版本号与「修订要点」。
- 改完在工作日志追加一段，写清**基准来源、改了哪几份、发现了哪些真值与旧文档不符**。
