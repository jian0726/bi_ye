-- ============================================================
-- 清理本次冒烟测试产生的一次性数据
-- 依据：create_time >= '2026-10-13 00:00:00'（今日测试）
-- 保留：id<=8 的用户（演示数据）、id<=7 的留言（演示数据）
-- ============================================================

SELECT '=== 清理前快照 ===' AS s;

SELECT '消息(测试)' AS tbl, COUNT(*) AS cnt FROM t_message WHERE id >= 16
UNION ALL SELECT '用户(测试)', COUNT(*) FROM t_user WHERE id >= 13
UNION ALL SELECT '点赞记录(测试)', COUNT(*) FROM t_like_record WHERE create_time >= '2026-10-13 00:00:00'
UNION ALL SELECT '收藏记录(测试)', COUNT(*) FROM t_collect WHERE create_time >= '2026-10-13 00:00:00'
UNION ALL SELECT '文章3计数', like_count FROM t_article WHERE id = 3;

-- 1) 删除测试留言
DELETE FROM t_message WHERE id >= 16 AND create_time >= '2026-10-13 00:00:00';

-- 2) 删除测试点赞记录
DELETE FROM t_like_record WHERE create_time >= '2026-10-13 00:00:00';

-- 3) 删除测试收藏记录
DELETE FROM t_collect WHERE create_time >= '2026-10-13 00:00:00';

-- 4) 删除测试用户（先确认无外键引用）
DELETE FROM t_user WHERE id >= 13 AND create_time >= '2026-10-13 00:00:00';

-- 5) 修正文章计数：按真实记录数重算 like_count / collect_count
UPDATE t_article a
SET a.like_count = (SELECT COUNT(*) FROM t_like_record l WHERE l.target_id = a.id)
WHERE a.id IN (1, 2, 3);

UPDATE t_article a
SET a.collect_count = (SELECT COUNT(*) FROM t_collect c WHERE c.article_id = a.id)
WHERE a.id IN (1, 2, 3);

SELECT '=== 清理后核对 ===' AS s;

SELECT '剩余用户数' AS item, COUNT(*) AS v FROM t_user
UNION ALL SELECT '剩余留言数', COUNT(*) FROM t_message
UNION ALL SELECT '剩余点赞记录', COUNT(*) FROM t_like_record
UNION ALL SELECT '剩余收藏记录', COUNT(*) FROM t_collect;

SELECT id, view_count, like_count, collect_count FROM t_article WHERE id IN (1,2,3);

SELECT id, nickname, LEFT(content,20) AS c, create_time FROM t_message ORDER BY id;
