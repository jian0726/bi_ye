# 移除留言回复功能 + Docker 库对齐 —— 备份与迁移说明

变更日期：2026-10-05。本目录存放改动前的备份、比对证据与回滚脚本。

## 一、备份内容

| 文件 | 内容 |
|---|---|
| `init.sql.blog-backend` | 变更前代码版建表脚本（`blog-backend/src/main/resources/db/init.sql`） |
| `init.sql.docs` | 变更前文档版建表脚本（`docs/02-设计文档/init.sql`） |
| `t_message-3306-schema.sql` | 宿主库（3306）`t_message` 结构，仍含 `reply` / `reply_time` |
| `t_message-3306-data.sql` | 宿主库 `t_message` 全量数据（7 条，其中 6 条带博主回复） |
| `t_message-3306-after.txt` | 宿主库删列后的 `SHOW CREATE TABLE` 快照 |
| `t_message-3307-schema.sql` / `t_message-3307-data.sql` | Docker 库（3307）`t_message` 改动前的结构与数据 |
| `full-backup-3307-before-update.sql` | **Docker 库整体备份**（15 张表结构 + 全部数据，含已废弃的 `t_comment` 与 6 条评论） |

采集时的告警日志 `dump-3306*.err` / `dump-3307*.err` 一并保留，内容仅为
「命令行传密码不安全」与「缺 PROCESS 权限无法导出 tablespace」，不影响导出结果。

## 二、Docker 库（3307）的同步结果

3307 停留在「评论功能还在」的旧版本，本次以 `init.sql` 为唯一基准对齐，共处置 5 项：

| # | 处置 | 说明 |
|---|---|---|
| 1 | `DROP TABLE t_comment` | 评论功能已整体移除（无表、无接口、无页面、无文档），该表及 6 条评论数据仅存于备份 |
| 2 | `t_article` 删 `allow_comment` / `comment_count` | 评论遗留列 |
| 3 | `t_like_record` 重建 | 旧结构带 `target_type` + `uk_user_target`，与 `entity/LikeRecord.java` 不符；改为文章维度 `uk_user_article(user_id, target_id)`（旧表无数据，重建无损失） |
| 4 | 补建 `t_view_record` | 文章 × 独立访客的浏览量去重表 |
| 5 | 修复 `t_collect` 乱码注释 + `t_user` 注释口径 | `t_collect` 的列与表注释被早期 latin1 连接写成了 `?`，用 `MODIFY` 重写注释（不丢数据）；`phone` / `email` 注释同步为「与邮箱 / 手机号至少一项」 |

随后用修订后的 `init.sql` 重导数据，使 3307 成为干净的演示库（17 篇文章 / 7 条留言 / 12 张照片 /
4 条友链，互动计数与点赞、收藏记录均为 0，与宿主库「已归零」口径一致）。

同步脚本：`blog-backend/src/main/resources/db/upgrade-align-to-init-2026-10-05.sql`
（该脚本已于 2026-10-07 的「db 目录合并为单一 init.sql」中删除，变更内容已并入 `init.sql`）。

**校验结果**：`diff` 归一化后结构与基准完全一致；乱码注释计数为 0；站长与普通用户均可按手机号 / 邮箱查到。

### 与宿主库（3306）仍存在的差异（均为运行数据，非结构问题）

| 表 | 3306 | 3307 | 原因 |
|---|---|---|---|
| `t_user` | 9 | 8 | 3306 多一条真实注册用户（非种子） |
| `t_music` | 3 | 5 | 3306 只保留了 3 首启用曲目，3307 为 `init.sql` 种子全量 |
| `t_file` | 3 | 0 | 3306 有真实上传记录，3307 为干净种子 |
| `t_visit_log` | 920 | 18 | 3306 累计了真实访问埋点 |

## 三、比对证据

| 文件 | 用途 |
|---|---|
| `full-schema-3306.sql` | 宿主库全库结构（对齐时的参照物之一） |
| `full-schema-3307.sql` / `-after.sql` / `-final.sql` | Docker 库在「同步前 / 删列后 / 重导后」三个时点的结构 |
| `ref-schema.sql` | 用 `init.sql` 建参照库后导出的结构（权威基准） |
| `schema-diff.txt` | 同步前的完整结构差异（86 行 diff） |
| `n*.sql` | 上述快照去掉 `AUTO_INCREMENT` 与空格噪声后的归一化版本，供 `diff` 直接比较 |

## 四、回滚方式

### 单表回滚（宿主库 3306）

```bash
mysql --host=127.0.0.1 --port=3306 -ujianyou -p jianyou_blog < t_message-3306-schema.sql
mysql --host=127.0.0.1 --port=3306 -ujianyou -p jianyou_blog < t_message-3306-data.sql
```

> 两条脚本都带 `DROP TABLE IF EXISTS t_message`，会**清空当前留言表**并恢复被删的回复列与回复内容。
> 生产数据请勿直接执行，先自行导出当前数据。

### 整体回滚（Docker 库 3307）

```bash
mysql --host=127.0.0.1 --port=3307 -uroot -proot2026 jianyou_blog < full-backup-3307-before-update.sql
```

> 该备份含 `DROP TABLE` 与建表语句，会覆盖当前 15 张表，执行前请确认。

### 代码与文档

用 Git 回滚对应提交；若未提交，参照 `docs/02-设计文档/缺陷修复记录.md` 6.5 节逐层还原。
