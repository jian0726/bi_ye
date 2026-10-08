"""合成 db/init.sql —— 从线上真实库 dump 生成唯一的初始化脚本。

背景：原 init.sql 存在 `'ip:N'` 脏列值（dump 产物缺陷，导入必报错），
且配套 10 个 upgrade-*.sql 分散维护。本脚本以**线上库结构为唯一基准**重新生成。

种子策略（对齐原 init.sql 的设计意图）：
  带种子：t_user / t_category / t_tag / t_article / t_article_tag /
          t_message / t_friend_link / t_photo / t_music
  不带种子（运行态数据，重建后从 0 起算）：
          t_collect / t_like_record / t_view_record / t_visit_log /
          t_file / t_site_config

t_article 的 like_count / collect_count / view_count 一律归零，
与「互动计数由真实互动累计」的口径一致。
"""
import re
import subprocess
import sys

CONTAINER = "jianyou-mysql"
USER = "jianyou"
PWD = "jianyou2026"
DB = "jianyou_blog"

SEED_TABLES = {
    "t_user", "t_category", "t_tag", "t_article", "t_article_tag",
    "t_message", "t_friend_link", "t_photo", "t_music",
}
ALL_TABLES = [
    "t_user", "t_category", "t_tag", "t_article", "t_article_tag",
    "t_collect", "t_like_record", "t_view_record", "t_message",
    "t_friend_link", "t_file", "t_visit_log", "t_site_config",
    "t_photo", "t_music",
]

HEADER = """-- ============================================================
-- 简柚个人博客系统 - 数据库初始化脚本（唯一脚本）
-- ------------------------------------------------------------
-- 说明：
--   1. 本文件是全项目唯一的建库脚本，已合并历史上全部 upgrade-*.sql 的变更；
--      在空库上执行即可得到与当前代码完全一致的结构 + 演示种子数据。
--   2. 由 docker-compose 首次启动 MySQL 时自动执行
--      （挂载到 /docker-entrypoint-initdb.d/，库由 MYSQL_DATABASE=jianyou_blog
--       创建并选中，因此脚本内不含 CREATE DATABASE / USE）。
--   3. 手动导入：mysql -ujianyou -p jianyou_blog < init.sql
--   4. 结构基准为线上真实库（mysqldump --no-data 结果），
--      与 docs/02-设计文档/数据库设计文档.md 保持一致，共 15 张表。
--
-- 种子数据策略：
--   · 带种子：用户、分类、标签、文章、文章标签关联、留言、友链、相册、音乐
--   · 不带种子（运行态数据，重建后从 0 起算）：
--     收藏 t_collect / 点赞 t_like_record / 浏览 t_view_record /
--     访问日志 t_visit_log / 上传资源 t_file / 站点配置 t_site_config
--   · 文章互动计数（view/like/collect_count）一律归零，
--     与「计数由真实互动累计、按独立访客去重」的口径一致。
--
-- 版本：2026-10-07 合并版（替代 init.sql + 10 个 upgrade-*.sql）
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

"""

FOOTER = """
SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 初始化完成：15 张表 + 演示种子数据
-- ============================================================
"""

TABLE_COMMENT = {
    "t_user": "用户表（登录标识 = 手机号 / 邮箱，无用户名字段）",
    "t_category": "分类表（仅 记录 / 游记 / 随笔 三类，无「技术」分类）",
    "t_tag": "标签表",
    "t_article": "文章表",
    "t_article_tag": "文章-标签 关联表",
    "t_collect": "文章收藏表（用户 × 文章 联合唯一，无种子数据）",
    "t_like_record": "文章点赞记录表（用户 × 文章 联合唯一，无种子数据）",
    "t_view_record": "文章浏览记录表（独立访客去重，无种子数据）",
    "t_message": "留言表（无 reply 列——留言板定位为「一次性留言」）",
    "t_friend_link": "友链表",
    "t_file": "上传资源表（MinIO 对象记录，无种子数据）",
    "t_visit_log": "访问日志埋点表（含 visitor_key 独立访客去重键，无种子数据）",
    "t_site_config": "站点配置表（键值对，DB 值覆盖 application.yml 的 site.* 默认值）",
    "t_photo": "相册照片表（url 为空时前台用 tone 渐变 + emoji 占位）",
    "t_music": "背景音乐表（全站 BGM 歌单）",
}


def dump(args: str) -> str:
    cmd = (
        f'docker exec {CONTAINER} mysqldump -u{USER} -p{PWD} '
        f"--default-character-set=utf8mb4 --skip-comments --no-tablespaces "
        f"--single-transaction --skip-add-locks --set-gtid-purged=OFF "
        f"--skip-add-drop-table --skip-extended-insert {args} {DB}"
    )
    r = subprocess.run(cmd, shell=True, capture_output=True, text=True, encoding="utf-8")
    if r.returncode != 0:
        print("mysqldump 失败：", r.stderr[:500], file=sys.stderr)
        sys.exit(1)
    return r.stdout


def clean_dump(sql: str) -> str:
    """去掉 dump 头尾的环境设置语句，保留 CREATE TABLE / INSERT。"""
    lines = []
    skip_patterns = [
        r"^/\*!\d+ SET @",
        r"^/\*!\d+ SET (NAMES|TIME_ZONE|SQL_MODE|UNIQUE_CHECKS|FOREIGN_KEY_CHECKS|SQL_NOTES)",
        r"^-- Dump completed",
        r"^-- MySQL dump",
        r"^-- Host:",
        r"^-- Server version",
    ]
    for ln in sql.splitlines():
        if any(re.match(p, ln) for p in skip_patterns):
            continue
        lines.append(ln)
    return "\n".join(lines).strip()


def extract_table(dump_sql: str, table: str) -> tuple[str, str]:
    """从 dump 文本里抽出某表的 CREATE TABLE 与 INSERT 语句。"""
    create = ""
    inserts = []
    # CREATE TABLE：从 "CREATE TABLE `t_x`" 到匹配的 ");"
    m = re.search(
        rf"CREATE TABLE `{table}` \(.*?\n\) ENGINE=[^;]*;",
        dump_sql, re.S,
    )
    if m:
        create = m.group(0)
    # INSERT：可能多行（--skip-extended-insert 时一行一条）
    for im in re.finditer(rf"INSERT INTO `{table}` VALUES .*?;", dump_sql, re.S):
        inserts.append(im.group(0))
    return create, "\n".join(inserts)


def add_drop(create: str, table: str) -> str:
    """在 CREATE TABLE 前补 DROP TABLE IF EXISTS，保证脚本可重复执行。"""
    return f"DROP TABLE IF EXISTS `{table}`;\n{create}"


def main():
    struct_sql = dump("--no-data")
    data_sql = dump("")

    out = [HEADER]
    for t in ALL_TABLES:
        create, _ = extract_table(struct_sql, t)
        if not create:
            print(f"⚠ 未取到 {t} 的建表语句", file=sys.stderr)
            continue

        out.append("-- " + "-" * 58)
        out.append(f"-- {TABLE_COMMENT.get(t, t)}")
        out.append("-- " + "-" * 58)
        out.append(add_drop(create, t))

        if t in SEED_TABLES:
            _, ins = extract_table(data_sql, t)
            if ins:
                out.append("")
                out.append(ins)
        else:
            out.append("")
            out.append("-- 不预置种子数据（运行态数据，重建后从 0 起算）")
        out.append("")

    # 文章互动计数归零
    out.append("-- 互动计数不从种子预置：点赞数 / 收藏数 / 浏览量由真实互动累计")
    out.append("UPDATE `t_article` SET like_count = 0, collect_count = 0, view_count = 0;")
    out.append(FOOTER)

    text = "\n".join(out)
    path = "blog-backend/src/main/resources/db/init.sql"
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write(text)
    print(f"已生成 {path}")
    print(f"  行数 {len(text.splitlines())}，字符 {len(text)}")
    print("  含 CREATE TABLE：", text.count("CREATE TABLE"))
    print("  含 INSERT INTO：", text.count("INSERT INTO"))


if __name__ == "__main__":
    main()
