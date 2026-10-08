-- ============================================================
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


-- ----------------------------------------------------------
-- 用户表（登录标识 = 手机号 / 邮箱，无用户名字段）
-- ----------------------------------------------------------
DROP TABLE IF EXISTS `t_user`;
CREATE TABLE `t_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '手机号（与邮箱至少一项，唯一，可用于登录）',
  `email` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '邮箱（与手机号至少一项，唯一，可用于登录）',
  `password` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '密码（BCrypt 密文，由应用启动时写入）',
  `nickname` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '昵称',
  `avatar` varchar(300) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '头像地址',
  `message_bg` varchar(300) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '留言页自定义背景图地址（NULL=默认夜空）',
  `bio` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '简介',
  `gender` tinyint NOT NULL DEFAULT '0' COMMENT '性别 0未知 1男 2女',
  `role` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'USER' COMMENT '角色 USER/ADMIN',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态 0禁用 1正常',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_phone` (`phone`),
  UNIQUE KEY `uk_email` (`email`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

INSERT INTO `t_user` VALUES (1,'13800000001','jianyou@local.dev','$2a$10$hJtqRVARlKhxW8U8jsMtJOfCDNGKJuZYBvSmHmvCCRIJUDuND7OQu','简之航','',NULL,'软件技术专业在读。不太会说话，所以写下来。',0,'ADMIN',1,'2026-03-01 09:00:00');
INSERT INTO `t_user` VALUES (2,'13800000002','linshen@local.dev','$2a$10$hJtqRVARlKhxW8U8jsMtJOfCDNGKJuZYBvSmHmvCCRIJUDuND7OQu','林深','',NULL,'爱写点代码和生活',0,'USER',1,'2026-05-11 14:20:00');
INSERT INTO `t_user` VALUES (3,'13800000003','aqi@local.dev','$2a$10$hJtqRVARlKhxW8U8jsMtJOfCDNGKJuZYBvSmHmvCCRIJUDuND7OQu','阿七','',NULL,'记录校园的日常',0,'USER',1,'2026-06-02 10:15:00');
INSERT INTO `t_user` VALUES (4,'13800000004','chenmo@local.dev','$2a$10$hJtqRVARlKhxW8U8jsMtJOfCDNGKJuZYBvSmHmvCCRIJUDuND7OQu','陈默','',NULL,NULL,0,'USER',1,'2026-06-18 20:40:00');
INSERT INTO `t_user` VALUES (5,'13800000005','xiaoman@local.dev','$2a$10$hJtqRVARlKhxW8U8jsMtJOfCDNGKJuZYBvSmHmvCCRIJUDuND7OQu','小满','',NULL,NULL,0,'USER',1,'2026-07-01 16:30:00');
INSERT INTO `t_user` VALUES (6,'13800000006','fenghuowuhen@local.dev','$2a$10$hJtqRVARlKhxW8U8jsMtJOfCDNGKJuZYBvSmHmvCCRIJUDuND7OQu','风过无痕','',NULL,'一个拍照片的人',0,'USER',1,'2026-07-20 11:00:00');
INSERT INTO `t_user` VALUES (7,'13800000007','zoe@local.dev','$2a$10$hJtqRVARlKhxW8U8jsMtJOfCDNGKJuZYBvSmHmvCCRIJUDuND7OQu','Zoe','',NULL,'画画、手帐和好天气',0,'USER',1,'2026-08-05 09:45:00');
INSERT INTO `t_user` VALUES (8,'13800000008','zhouyu@local.dev','$2a$10$hJtqRVARlKhxW8U8jsMtJOfCDNGKJuZYBvSmHmvCCRIJUDuND7OQu','周屿','',NULL,NULL,0,'USER',1,'2026-08-22 22:10:00');

-- ----------------------------------------------------------
-- 分类表（仅 记录 / 游记 / 随笔 三类，无「技术」分类）
-- ----------------------------------------------------------
DROP TABLE IF EXISTS `t_category`;
CREATE TABLE `t_category` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `name` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '分类名',
  `slug` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'URL别名',
  `description` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '分类描述',
  `icon` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '图标标识',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序权重，越小越靠前',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分类表';

INSERT INTO `t_category` VALUES (1,'记录','record','大学生活的每一步，写下就算数','book',1,'2026-09-19 01:09:34');
INSERT INTO `t_category` VALUES (2,'游记','travel','走过的地方，看过的东西','map',2,'2026-09-19 01:09:34');
INSERT INTO `t_category` VALUES (3,'随笔','essay','想到哪写到哪，不必有结论','feather',3,'2026-09-19 01:09:34');

-- ----------------------------------------------------------
-- 标签表
-- ----------------------------------------------------------
DROP TABLE IF EXISTS `t_tag`;
CREATE TABLE `t_tag` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '标签ID',
  `name` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '标签名',
  `slug` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'URL别名',
  `color` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '展示颜色',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='标签表';

INSERT INTO `t_tag` VALUES (1,'校园','campus','#2F5D50','2026-09-19 01:09:34');
INSERT INTO `t_tag` VALUES (2,'长沙','changsha','#D95D18','2026-09-19 01:09:34');
INSERT INTO `t_tag` VALUES (3,'读书','reading','#A855F7','2026-09-19 01:09:34');
INSERT INTO `t_tag` VALUES (4,'骑行','cycling','#2496ED','2026-09-19 01:09:34');
INSERT INTO `t_tag` VALUES (5,'日出','sunrise','#E76F00','2026-09-19 01:09:34');
INSERT INTO `t_tag` VALUES (6,'美食','food','#F7B32B','2026-09-19 01:09:34');
INSERT INTO `t_tag` VALUES (7,'雨天','rainy-day','#64748B','2026-09-19 01:09:34');
INSERT INTO `t_tag` VALUES (8,'夜晚','night','#1E3A5F','2026-09-19 01:09:34');
INSERT INTO `t_tag` VALUES (9,'成长','growth','#42B883','2026-09-19 01:09:34');
INSERT INTO `t_tag` VALUES (10,'日常','daily','#DC382D','2026-09-19 01:09:34');

-- ----------------------------------------------------------
-- 文章表
-- ----------------------------------------------------------
DROP TABLE IF EXISTS `t_article`;
CREATE TABLE `t_article` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '文章ID',
  `title` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '标题',
  `summary` varchar(300) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '摘要',
  `content` mediumtext COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '正文 markdown',
  `cover` varchar(300) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '封面图',
  `category_id` bigint DEFAULT NULL COMMENT '分类ID',
  `author_id` bigint NOT NULL COMMENT '作者用户ID',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态 0草稿 1已发布 2隐藏',
  `is_top` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否置顶',
  `is_recommend` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否推荐',
  `view_count` bigint NOT NULL DEFAULT '0' COMMENT '浏览量',
  `like_count` bigint NOT NULL DEFAULT '0' COMMENT '点赞数',
  `collect_count` bigint NOT NULL DEFAULT '0' COMMENT '收藏数',
  `word_count` int NOT NULL DEFAULT '0' COMMENT '字数',
  `read_minutes` int NOT NULL DEFAULT '1' COMMENT '预计阅读分钟数',
  `publish_time` datetime DEFAULT NULL COMMENT '发布时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category_id`),
  KEY `idx_status_publish` (`status`,`publish_time`),
  KEY `idx_view` (`view_count`)
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章表';

INSERT INTO `t_article` VALUES (1,'开学第一周：把书桌变成能待住的地方','新学期从收拾桌子开始。清空、贴墙、装软木板，十分钟的发呆值回全部体力。','## 九月的第一周\n\n开学第一周，课还没上到正题，人已经忙得团团转：领书、拆暑假寄回来的箱子、把新课表抄进本子。\n\n## 把书桌重新布置了一遍\n\n暑假就想好了：这学期要把书桌变成「能待住的地方」。\n\n- 桌面清空，只留台灯、水杯和每天都用的东西\n- 左边墙上贴了五月自己在岳麓山拍的日出\n- 加了两块软木板，票根和便签终于有地方挂了\n\n> 收拾完坐在椅子上发了十分钟呆。原来环境真的会影响心情。\n\n## 一点小感受\n\n以前总觉得宿舍只是睡觉的地方。现在慢慢觉得，把生活的角落收拾好，也是一种对自己的交代。\n\n新学期，从一张干净的桌子开始。',NULL,1,1,1,0,1,1,0,0,380,2,'2026-09-15 10:30:00','2026-09-15 10:30:00','2026-10-05 22:36:57');
INSERT INTO `t_article` VALUES (2,'食堂三楼的新窗口','开学偷偷新开的窗口，粉丝煲给得实在。阿姨手不抖，这件事很重要。','## 一个重大发现\n\n三楼靠东边新开了一个窗口，做砂锅粉丝煲。开学第二周才发现它，是我的失误。\n\n## 实测报告\n\n- 粉丝给得实在，锅底还有配菜\n- 汤是热的，端上来还在冒泡\n- 阿姨手不抖，肉是完整落下来的\n\n> 评语：午餐 / 晚餐都合适，饭点前十五分钟去不用排。\n\n## 记一笔\n\n其实对食堂没什么大期待，热乎、实在、不用等太久，就够好了。\n\n这家窗后要是能撑过这个学期，我打算把菜单挨个吃一遍。',NULL,1,1,1,0,0,0,0,0,260,2,'2026-09-11 12:40:00','2026-09-11 12:40:00','2026-09-23 15:13:08');
INSERT INTO `t_article` VALUES (3,'图书馆四楼，靠窗第三个位置','占位试遍全馆之后，我找到了最优解。附一份不严肃的位置测评。','## 位置测评\n\n开学两周，把图书馆的区域试了个遍。结论先说：**四楼靠窗，从左数第三个位置最好。**\n\n| 区域 | 优点 | 缺点 |\n| --- | --- | --- |\n| 一楼大厅 | 离门口近 | 人来人往，心静不下来 |\n| 三楼角落 | 绝对安静 | 有点闷 |\n| 四楼靠窗 | 光线好，抬头是树 | 下午两点后偏晒 |\n\n## 附近的邻居\n\n靠窗这一排有几个熟面孔：总穿蓝卫衣的考研人，翻书永远很轻；还有一位大叔，每天带一个保温杯和一份报纸。\n\n谁也不认识谁，但每天在同一排座位坐下，莫名安心。\n\n> 上午坐窗边，下午挪去背光那侧——这份攻略我用了两周，屡试不爽。',NULL,1,1,1,0,0,0,0,0,350,2,'2026-09-04 16:45:00','2026-09-04 16:45:00','2026-10-05 22:09:23');
INSERT INTO `t_article` VALUES (4,'把「以后再说」的事做完了','备忘录里躺了一个学期的那页清单，一个下午清完。原来所谓的麻烦，动手只要五分钟。','## 那页清单\n\n备忘录里有个叫「以后再说」的页面，记着各种拖着没做的小事。今天下午把它清空了。\n\n- 修那把松了半年的椅子 ✔\n- 把上学期拍的照片导进硬盘 ✔\n- 给家里寄的明信片补上邮编 ✔\n- 预约了拖了一个月的牙医 ✔\n\n## 一个发现\n\n每一件事，实际做起来都不超过二十分钟。**麻烦的不是事情本身，是「想起来还没做」的那种悬着的感觉。**\n\n> 清单清空的那一刻，比打游戏赢了还爽。\n\n以后这条规则写进本子：两分钟内能做完的事，立刻做；做不完的，写下来，别占脑子。',NULL,1,1,1,0,0,0,0,0,300,2,'2026-08-30 21:10:00','2026-08-30 21:10:00',NULL);
INSERT INTO `t_article` VALUES (5,'一个人去看了场早场电影','周日上午九点半的影厅，一共四个人。灯光暗下来的那一刻，舒服得叹了口气。','## 早场的好处\n\n周日上午九点半看电影，是这次学到的：票便宜一半，影厅里一共四个人，安静得能听见空调声。\n\n## 看的是什么\n\n一部讲小镇夏天的片子，没有反转，没有拯救世界。散场的时候前排的女生在擦眼泪，我倒没有，只是走到商场门口，突然不想马上回去。\n\n## i 人快乐清单\n\n在街边吃了碗粉，沿江边走了一段，拍了两张没什么意义的照片，下午回宿舍睡了个午觉。\n\n> 一个人出门不孤独，反而自由。不用迁就场次，不用讨论剧情，看完的感受完完全全属于自己。\n\n下周还有一部想看的，还订早场。',NULL,1,1,1,0,0,0,0,0,320,2,'2026-08-26 14:20:00','2026-08-26 14:20:00','2026-09-19 16:29:44');
INSERT INTO `t_article` VALUES (6,'宿舍楼下的猫，我叫它柚子','一只不怕人的橘猫，每天傍晚蹲在快递柜顶上。给它起了名字，它不知道，但我开心。','## 关于柚子\n\n宿舍楼下有只橘猫，圆，且不怕人。每天傍晚准时蹲在快递柜顶上，看着人来人往，像个值班的。\n\n某天路过的时候它冲我喵了一声，从那天起我就认定：这猫归我了，叫柚子。\n\n## 日常\n\n- 柚子不接受抚摸，但接受蹲下来对视十秒\n- 有一次带了根火腿肠，它吃完就走了，非常功利\n- 下雨天它不出勤，快递柜顶上空空的\n\n> 没养过猫，不知道养猫是什么感觉。但每天下楼看一眼柚子在不在，已经成了固定节目。\n\n它大概有好多个人给它起名字。没关系，柚子不知道，我开心就好。',NULL,1,1,1,0,0,0,0,0,300,2,'2026-08-21 18:30:00','2026-08-21 18:30:00','2026-09-23 10:28:57');
INSERT INTO `t_article` VALUES (7,'班级聚会：不说话也挺好','两个小时的活动，我大概说了二十分钟的话。但散场时发现，这样也很好。','## 聚会报告\n\n学期末的班级聚会，火锅店，两桌人。作为 i 人，如实记录我的参与情况：\n\n- 到场：全程两小时\n- 主动发言：约二十分钟\n- 添茶倒水：若干次\n- 认真听讲：全程\n\n## 我的位置\n\n坐在靠墙那侧，听大家聊暑假去了哪、实习怎么样。轮到我，就简单说了两句。没人觉得我奇怪，我也没觉得尴尬。\n\n> 以前总觉得聚会里不活跃就是不合群。现在接受了一件事：我可以是那个话不多、但一直在认真听的人。\n\n## 散场\n\n走的时候和几个同学顺路了一段，聊了几句最近的事。夏天的晚上，风是热的，心情是松的。\n\n不说话也挺好。说了的每一句，也都没白说。',NULL,1,1,1,0,0,0,0,0,330,2,'2026-06-30 22:00:00','2026-06-30 22:00:00','2026-09-19 14:36:44');
INSERT INTO `t_article` VALUES (8,'拿到奖学金那天，请自己吃了顿好的','国家励志奖学金到账的那天中午，我去吃了平时舍不得点的那家。不是炫耀，是想记住这种努力被看见的感觉。','## 中午的一顿饭\n\n名单公布那天中午，我去吃了学校旁边那家一直舍不得的单人小火锅。加了肥牛，加了虾滑。\n\n## 想起这一年\n\n说实话，上学期过得不算轻松。图书馆四楼的那些早上，自习室里熬的那些晚上，一度怀疑有没有用。\n\n名单出来那一刻，答案有了。\n\n> 不是炫耀，是想把这种「努力被看见」的感觉记下来。以后再撑不下去的时候，翻回这篇看看。\n\n## 给自己的一句话\n\n饭是自己挣的，路也是自己走的。慢慢来，比较快。',NULL,1,1,1,0,0,0,0,0,280,2,'2026-08-15 12:00:00','2026-08-15 12:00:00','2026-10-05 22:09:23');
INSERT INTO `t_article` VALUES (9,'查分那天','刷新页面之前手心全是汗。分数出来那一刻，一个夏天悬着的事终于落了地。','## 上午十点\n\n出成绩的上午，我把手机拿起来又放下三次。\n\n十点整，页面刷出来之前，手心全是汗。旁边的室友装作不在意，其实俩人都伸着脖子。\n\n## 然后\n\n分数比预想的好。没有顶到天花板，但每一分都对得起那些四楼靠窗的早晨。\n\n## 后来的事\n\n给家里打了个电话，我妈在那头说了句「那就好」，我听得出她也松了口气。\n\n> 查分这种事，熬的时候觉得漫长，回头看就一瞬。记录一下这一瞬。\n\n中午和室友去吃了顿好的，算是给这学期收尾。',NULL,1,1,1,0,0,0,0,0,270,2,'2026-07-28 10:30:00','2026-07-28 10:30:00','2026-09-19 14:36:10');
INSERT INTO `t_article` VALUES (10,'暑假没回家：留校的三十天','校园空了一半，食堂只开一楼。但傍晚去江边吹风的那半个小时，值回整个暑假。','## 空了一半的校园\n\n留校的理由很实际：暑假想把自己再打磨打磨。校园一下子空了，食堂只开一楼，操场傍晚才有人。\n\n## 三十天怎么过的\n\n- 上午：图书馆四楼，老位置\n- 下午：上课 / 自习\n- 傍晚：去湘江边走半个小时，这是每天最盼的一段\n- 夜里：楼下小卖部的绿豆冰棒，两块五，没涨价\n\n## 江边\n\n江风把白天的燥气都吹掉。对岸的灯一盏盏亮起来，有人跑步，有人遛狗，有人什么也不干就站着。\n\n> 我属于最后一种。站着站着，就觉得这个暑假没白留。\n\n三十天，说长不长。但比在家吹空调的那个假期，多了些能留下的东西。',NULL,1,1,1,0,0,0,0,0,340,2,'2026-07-20 21:00:00','2026-07-20 21:00:00','2026-09-19 14:36:14');
INSERT INTO `t_article` VALUES (11,'给家里打了个很长的电话','从菜价说到将来，一个半小时。挂了电话，心里满满当当的。','## 起因很小\n\n本来只想问一句家里的天气，结果打了一个半小时。\n\n## 都聊了什么\n\n从院子里的辣椒讲到隔壁修路，从我吃饭讲到将来想去哪。中间我妈说了一句：\n\n> 「在外面照顾好自己，别的都不急。」\n\n我嗯了一声，没接话。有些话接了就要谈感受，而我在忍眼泪这件事上，向来不擅长。\n\n## 挂了之后\n\n走出去在阳台上站了一会儿。晚风很软，楼下的柚子（猫）在叫。\n\n> 离家上学之后才懂，所谓牵挂，就是有人把你的事当成天大的事。\n\n下周再打。这次记得提前想好要说的事，别又只问天气。',NULL,1,1,1,0,0,0,0,0,280,2,'2026-07-12 21:40:00','2026-07-12 21:40:00','2026-09-19 14:36:20');
INSERT INTO `t_article` VALUES (12,'换了个台灯，宿舍都暖了','六十块钱的暖光灯。开灯的那一刻，整个宿舍的气质都变了。','## 缘起\n\n原来的台灯是白光，亮是亮，像自习室。晚上回宿舍一开灯，紧迫感扑面而来。\n\n## 新灯\n\n六十块，暖黄光，三档亮度，能充电。\n\n开灯那一刻的感受，怎么形容呢——**像把自习室换成了自家客厅。**\n\n## 灯下做的事\n\n- 看了半本《人类简史》\n- 给软木板上添了两张票根\n- 什么也没干，就坐着发了会儿呆\n\n> 有些钱花得很值。六十块买到的东西，叫「愿意在宿舍多待一会儿」。\n\n室友看了第二天也下单了一个。现在我们宿舍晚上是暖的。',NULL,1,1,1,0,0,0,0,0,240,1,'2026-07-05 22:30:00','2026-07-05 22:30:00','2026-09-19 14:36:33');
INSERT INTO `t_article` VALUES (13,'清晨五点半的岳麓山','为了看一次日出，四点半爬起来。上山的人比想象中多，山顶的风比想象中凉。太阳跳出来那几分钟，都值了。','## 四点半的闹钟\n\n前一晚查了日出时间：5:58。闹钟定在 4:30，纠结了三秒钟要不要改回 7:00。\n\n出门的时候天还是深蓝色，路上已经有人往山方向走了，人比想象中多。\n\n## 上山\n\n石阶湿凉，两边树很密，路灯隔很远才一盏。爬到一半回头，能看见城市的灯还亮着一小片。\n\n爱晚亭那段路，风一下子大了，把困意全吹跑了。\n\n## 山顶\n\n5:40 坐定，先等来的是风，凉得人清醒。然后天从灰蓝变橙，云的边缘开始发亮。\n\n**5:58，太阳从云层后面整个跳了出来，就几秒钟的事。**\n\n> 山顶的人都在举手机，我拍了两张，后来决定只留一张。剩下的用眼睛看，更清楚。\n\n下山买了杯豆浆，回宿舍补了一觉。这个五月，值了。',NULL,2,1,1,0,1,0,0,0,380,2,'2026-05-02 08:00:00','2026-05-02 08:00:00','2026-10-05 22:09:23');
INSERT INTO `t_article` VALUES (14,'骑过橘子洲：十二公里的江风','租了辆共享单车，从洲头骑到洲尾再折返。湘江的风、放风筝的人、桥上的落日，记一篇流水账，留给以后看。','## 出发\n\n四月中的一个周六，天晴。地铁到橘子洲站，出站第一件事：扫码开一辆单车。\n\n## 一路的流水账\n\n- 洲头广场：毛泽东青年艺术雕塑就在那，仰头看了很久\n- 江堤边：有人在放风筝，风筝挂树上了，主人不着急，坐着等风\n- 樱花尾季：剩下的几棵还开着，风一过飘一阵花瓣\n- 桥下：火车过桥的声音被江风裹着传过来，闷闷的\n\n## 洲尾\n\n骑到洲尾折返，掉头那一刻正对落日。整个江面是金色的，我停在原地看了五分钟。\n\n> 来回十二公里，两小时。腿是酸的，心里是松的。\n\n## 记一笔\n\n回程把车还了，沿江边慢慢走回地铁口。这种日子没什么剧情，但过完一整周都会想起来。',NULL,2,1,1,0,0,0,0,0,320,2,'2026-04-12 19:30:00','2026-04-12 19:30:00','2026-09-23 11:32:35');
INSERT INTO `t_article` VALUES (15,'期末周的深夜自习室','凌晨的自习室只剩几个人。有人在赶论文，有人在刷题，我盯着窗外发呆的十分钟，反而是那几天最清醒的时刻。','## 凌晨的自习室\n\n期末周，自习室。十二点之后，人陆陆续续走了，剩下四五个灯下的影子。\n\n我左边是赶论文的女生，键盘声很密；对面的人在做题，笔尖一直在动，偶尔停很久。\n\n## 我的十分钟\n\n背了两个小时的提纲，脑子开始像浆糊。索性合上书，盯着窗外发了十分钟呆。\n\n对面教学楼只剩两个亮着的窗户，路灯下面有只猫慢慢走过去。\n\n**奇怪的是，这十分钟反而是那几天最清醒的时刻。**\n\n> 明白了：脑子不是机器，熄灯不是休息。留白也是复习的一部分。\n\n## 收尾\n\n一点半，收拾东西。回宿舍的路上，风把白天的热气吹干净了。\n\n那学期最后考得不错。但记住的不是分数，是那十分钟。',NULL,3,1,1,0,0,0,0,0,320,2,'2026-06-18 23:50:00','2026-06-18 23:50:00','2026-10-05 22:09:23');
INSERT INTO `t_article` VALUES (16,'我的 i 路程：为什么决定开始记录','这个博客真正的起点。不太会说话的人，想用文字把自己留在日子里——与其都堵在心里，不如一行行写下来。','## 先说清楚「i 路程」是什么\n\n我是个典型的 i 人。课堂上被点名发言，心跳会快半拍；班群里打好一段话，看看又删掉。想说的事其实不少，只是都堵在半路。\n\n所以有了这里。**打字，是我最擅长的说话方式。**\n\n## 为什么是现在\n\n大三了。前两年过得挺模糊：上课、交作业、考试，日子一张张翻过去，回头却想不起哪一页写了什么。\n\n九月的一个晚上，我在备忘录里写了一句「好像该留下点什么」，写完自己盯着看了很久。\n\n于是开始。\n\n## 这里会写什么\n\n- 上课、自习、考试，这些日常的小事\n- 偶尔出去走的路：山、江、桥\n- 读书、电影、天气，以及半夜突然冒出来的念头\n\n> 不写教程，不输出观点，只记录。等毕业那天回头看，希望这一页页都是真的。\n\n第一篇，写给自己。',NULL,1,1,1,1,1,0,0,0,520,3,'2026-09-02 21:00:00','2026-09-02 21:00:00','2026-10-05 22:09:23');
INSERT INTO `t_article` VALUES (17,'一场雨和半本《人类简史》','下雨天适合读书。读到「想象的共同体」那一章，突然想明白了很多事为什么让人安心。','## 雨天限定\n\n三月的雨下了一整天。这种天气出门是麻烦，留在屋里就成了正当理由。\n\n泡了茶，把搁置很久的《人类简史》翻出来，一下午读了半本。\n\n## 读到的一章\n\n「想象的共同体」那一章说：国家、公司、货币这些概念，本质上都是人们共同相信的故事。\n\n合上书想了一会儿，突然明白了几件事：\n\n- 为什么升旗的时候心里会动——那是同一个故事里的人站在一起\n- 为什么宿舍夜谈能聊那么久——我们正在共享同一段十八九岁\n- 为什么这个博客让我安心——它是我讲给自己听的故事\n\n> 书没读完，剩下的留给下个雨天。好东西要省着用。\n\n## 记一笔\n\n雨停的时候天已经暗了。楼下柚子不知从哪儿钻出来，抖了抖毛。书里的故事很大，窗外的故事很小，都挺好。',NULL,3,1,1,0,0,0,0,0,330,2,'2026-03-21 16:10:00','2026-03-21 16:10:00','2026-09-20 09:54:18');

-- ----------------------------------------------------------
-- 文章-标签 关联表
-- ----------------------------------------------------------
DROP TABLE IF EXISTS `t_article_tag`;
CREATE TABLE `t_article_tag` (
  `article_id` bigint NOT NULL COMMENT '文章ID',
  `tag_id` bigint NOT NULL COMMENT '标签ID',
  PRIMARY KEY (`article_id`,`tag_id`),
  KEY `idx_tag` (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章标签关联表';

INSERT INTO `t_article_tag` VALUES (1,1);
INSERT INTO `t_article_tag` VALUES (2,1);
INSERT INTO `t_article_tag` VALUES (3,1);
INSERT INTO `t_article_tag` VALUES (7,1);
INSERT INTO `t_article_tag` VALUES (15,1);
INSERT INTO `t_article_tag` VALUES (5,2);
INSERT INTO `t_article_tag` VALUES (10,2);
INSERT INTO `t_article_tag` VALUES (13,2);
INSERT INTO `t_article_tag` VALUES (14,2);
INSERT INTO `t_article_tag` VALUES (17,3);
INSERT INTO `t_article_tag` VALUES (14,4);
INSERT INTO `t_article_tag` VALUES (13,5);
INSERT INTO `t_article_tag` VALUES (2,6);
INSERT INTO `t_article_tag` VALUES (8,6);
INSERT INTO `t_article_tag` VALUES (17,7);
INSERT INTO `t_article_tag` VALUES (15,8);
INSERT INTO `t_article_tag` VALUES (4,9);
INSERT INTO `t_article_tag` VALUES (8,9);
INSERT INTO `t_article_tag` VALUES (9,9);
INSERT INTO `t_article_tag` VALUES (10,9);
INSERT INTO `t_article_tag` VALUES (11,9);
INSERT INTO `t_article_tag` VALUES (16,9);
INSERT INTO `t_article_tag` VALUES (5,10);
INSERT INTO `t_article_tag` VALUES (6,10);
INSERT INTO `t_article_tag` VALUES (12,10);

-- ----------------------------------------------------------
-- 文章收藏表（用户 × 文章 联合唯一，无种子数据）
-- ----------------------------------------------------------
DROP TABLE IF EXISTS `t_collect`;
CREATE TABLE `t_collect` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '收藏ID',
  `user_id` bigint NOT NULL COMMENT '收藏人ID',
  `article_id` bigint NOT NULL COMMENT '被收藏文章ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_article` (`user_id`,`article_id`),
  KEY `idx_user_time` (`user_id`,`create_time`),
  KEY `idx_article` (`article_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章收藏表';

-- 不预置种子数据（运行态数据，重建后从 0 起算）

-- ----------------------------------------------------------
-- 文章点赞记录表（用户 × 文章 联合唯一，无种子数据）
-- ----------------------------------------------------------
DROP TABLE IF EXISTS `t_like_record`;
CREATE TABLE `t_like_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '点赞ID',
  `user_id` bigint NOT NULL COMMENT '点赞人ID',
  `target_id` bigint NOT NULL COMMENT '被点赞文章ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_article` (`user_id`,`target_id`),
  KEY `idx_target` (`target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章点赞记录表';

-- 不预置种子数据（运行态数据，重建后从 0 起算）

-- ----------------------------------------------------------
-- 文章浏览记录表（独立访客去重，无种子数据）
-- ----------------------------------------------------------
DROP TABLE IF EXISTS `t_view_record`;
CREATE TABLE `t_view_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '浏览记录ID',
  `article_id` bigint NOT NULL COMMENT '被浏览文章ID',
  `visitor_key` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '访客标识：登录用户 u:{userId}；游客 ip:{客户端IP}',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '首次浏览时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_article_visitor` (`article_id`,`visitor_key`),
  KEY `idx_article` (`article_id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章浏览记录表（独立访客去重）';

-- 不预置种子数据（运行态数据，重建后从 0 起算）

-- ----------------------------------------------------------
-- 留言表（无 reply 列——留言板定位为「一次性留言」）
-- ----------------------------------------------------------
DROP TABLE IF EXISTS `t_message`;
CREATE TABLE `t_message` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '留言ID',
  `user_id` bigint DEFAULT NULL COMMENT '留言人，游客为 NULL',
  `nickname` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '游客昵称',
  `email` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '游客邮箱（不回显）',
  `content` varchar(400) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '留言内容',
  `ip_location` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'IP属地',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_create` (`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='留言表';

INSERT INTO `t_message` VALUES (1,2,NULL,NULL,'博客做得很清爽，读起来不累。加油！','广东','2026-09-15 22:30:00');
INSERT INTO `t_message` VALUES (2,6,NULL,NULL,'你好呀，楼下的猫还常出没吗？下学期去看一眼。','江苏','2026-09-14 19:20:00');
INSERT INTO `t_message` VALUES (3,NULL,'路过的人',NULL,'从一个朋友的朋友圈点进来的，一口气看了好几篇，很舒服的网站。','山东','2026-09-12 21:45:00');
INSERT INTO `t_message` VALUES (4,7,NULL,NULL,'岳麓山日出那篇，看完就想定闹钟了。下一趟打算去哪？','上海','2026-09-10 16:05:00');
INSERT INTO `t_message` VALUES (5,NULL,'同样在熬的人',NULL,'从「记录」那一类翻到这里的，看到你写期末周自习室那段，突然有点想哭。我也是大三。','四川','2026-09-16 01:05:00');
INSERT INTO `t_message` VALUES (6,2,NULL,NULL,'起始页那个墨绿色的刊头太好看了，看着很安静。','广东','2026-09-16 23:18:00');
INSERT INTO `t_message` VALUES (7,8,NULL,NULL,'期末周自习室那篇看得我眼眶发热，还有一年我也大四了。一起熬，都加油。','湖南','2026-09-17 20:42:00');

-- ----------------------------------------------------------
-- 友链表
-- ----------------------------------------------------------
DROP TABLE IF EXISTS `t_friend_link`;
CREATE TABLE `t_friend_link` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '友链ID',
  `name` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '站点名称',
  `url` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '站点地址',
  `logo` varchar(300) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '站点图标',
  `description` varchar(60) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '站点描述',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0待审核 1已上架 2下架',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序权重',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='友链表';

INSERT INTO `t_friend_link` VALUES (1,'林深的小站','https://example.com',NULL,'写点生活和学习',1,1,'2026-09-19 01:09:34');
INSERT INTO `t_friend_link` VALUES (2,'Zoe 的手帐','https://example.com',NULL,'画画、手帐和好天气',1,2,'2026-09-19 01:09:34');
INSERT INTO `t_friend_link` VALUES (3,'风过无痕','https://example.com',NULL,'一个拍照片的人',1,3,'2026-09-19 01:09:34');
INSERT INTO `t_friend_link` VALUES (4,'阿七的日志','https://example.com',NULL,'记录校园的日常',1,4,'2026-09-19 01:09:34');

-- ----------------------------------------------------------
-- 上传资源表（MinIO 对象记录，无种子数据）
-- ----------------------------------------------------------
DROP TABLE IF EXISTS `t_file`;
CREATE TABLE `t_file` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '资源ID',
  `name` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '原文件名',
  `object_key` varchar(300) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '对象键',
  `url` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '访问地址',
  `type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'MIME 类型',
  `size` bigint NOT NULL DEFAULT '0' COMMENT '文件大小（字节）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  PRIMARY KEY (`id`),
  KEY `idx_file_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='上传资源表';

-- 不预置种子数据（运行态数据，重建后从 0 起算）

-- ----------------------------------------------------------
-- 访问日志埋点表（含 visitor_key 独立访客去重键，无种子数据）
-- ----------------------------------------------------------
DROP TABLE IF EXISTS `t_visit_log`;
CREATE TABLE `t_visit_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `ip` varchar(45) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '客户端IP',
  `visitor_key` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT 'è®¿å®¢æ ‡è¯†ï¼šç™»å½• u:{userId} / æ¸¸å®¢ ip:{IP}ï¼ˆç‹¬ç«‹è®¿å®¢åŽ»é‡é”®ï¼‰',
  `province` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '未知' COMMENT '省份（IP归属地；本机/内网为「本地」）',
  `path` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '访问路径',
  `user_agent` varchar(300) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '浏览器UA',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '访问时间',
  PRIMARY KEY (`id`),
  KEY `idx_visit_time` (`create_time`),
  KEY `idx_visit_province` (`province`,`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=34 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='访问日志埋点表';

-- 不预置种子数据（运行态数据，重建后从 0 起算）

-- ----------------------------------------------------------
-- 站点配置表（键值对，DB 值覆盖 application.yml 的 site.* 默认值）
-- ----------------------------------------------------------
DROP TABLE IF EXISTS `t_site_config`;
CREATE TABLE `t_site_config` (
  `config_key` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '配置键（与 site.* kebab-case 一致，如 site-name）',
  `config_value` varchar(2000) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '配置值',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='站点配置表';

-- 不预置种子数据（运行态数据，重建后从 0 起算）

-- ----------------------------------------------------------
-- 相册照片表（url 为空时前台用 tone 渐变 + emoji 占位）
-- ----------------------------------------------------------
DROP TABLE IF EXISTS `t_photo`;
CREATE TABLE `t_photo` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '照片ID',
  `title` varchar(60) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '标题',
  `url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '图片地址（MinIO）；空=渐变占位',
  `location` varchar(60) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '拍摄地点',
  `taken_date` date DEFAULT NULL COMMENT '拍摄日期',
  `ratio` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '1/1' COMMENT '宽高比（如 3/4），瀑布流占位用',
  `tone` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '占位渐变色（url 为空时显示）',
  `emoji` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '占位 emoji',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序权重，越小越靠前',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态 1显示 2隐藏',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_photo_sort` (`status`,`sort_order`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='相册照片表';

INSERT INTO `t_photo` VALUES (1,'岳麓山日出',NULL,'长沙 · 岳麓山','2026-05-02','3/4','linear-gradient(160deg,#f5a15f,#b8440e)','🌄',1,1,'2026-09-22 09:12:35');
INSERT INTO `t_photo` VALUES (2,'橘子洲的江风',NULL,'长沙 · 橘子洲','2026-04-12','4/3','linear-gradient(160deg,#5da291,#234439)','🌾',2,1,'2026-09-22 09:12:35');
INSERT INTO `t_photo` VALUES (3,'深夜自习室','http://localhost:9000/jianyou-blog/2026/09/22/211c64af7016460281f6e784d0687ced.png','长沙 · 图书馆','2026-06-18','1085/498','linear-gradient(160deg,#3a3f6b,#1b1e38)','🌙',3,1,'2026-09-22 09:12:35');
INSERT INTO `t_photo` VALUES (4,'宿舍窗外的雨',NULL,'长沙 · 学校','2026-03-21','4/5','linear-gradient(160deg,#7b8fa1,#39434e)','🌧️',4,1,'2026-09-22 09:12:35');
INSERT INTO `t_photo` VALUES (5,'食堂三楼的糖醋排骨',NULL,'长沙 · 食堂','2026-03-08','1/1','linear-gradient(160deg,#d97757,#8c3b22)','🍛',5,1,'2026-09-22 09:12:35');
INSERT INTO `t_photo` VALUES (6,'第一次跑完五公里',NULL,'长沙 · 操场','2026-02-27','4/3','linear-gradient(160deg,#e8c98d,#9c7a3c)','🏃',6,1,'2026-09-22 09:12:35');
INSERT INTO `t_photo` VALUES (7,'冰箱贴收藏',NULL,'长沙 · 宿舍','2026-01-30','3/4','linear-gradient(160deg,#9db4c0,#4e6572)','🧲',7,1,'2026-09-22 09:12:35');
INSERT INTO `t_photo` VALUES (8,'寒假回家的高铁',NULL,'长沙南 · 站台','2026-01-12','4/5','linear-gradient(160deg,#c0a9bd,#5d4a66)','🚄',8,1,'2026-09-22 09:12:35');
INSERT INTO `t_photo` VALUES (9,'课桌上的多肉',NULL,'长沙 · 教室','2025-12-19','1/1','linear-gradient(160deg,#a3c4a8,#3d6b4a)','🪴',9,1,'2026-09-22 09:12:35');
INSERT INTO `t_photo` VALUES (10,'晚自习的天空',NULL,'长沙 · 天台','2025-12-05','4/3','linear-gradient(160deg,#5f7fa8,#2a3d5c)','🌆',10,1,'2026-09-22 09:12:35');
INSERT INTO `t_photo` VALUES (11,'球鞋洗得很白',NULL,'长沙 · 水房','2025-11-16','3/4','linear-gradient(160deg,#d6d2c4,#8a8574)','👟',11,1,'2026-09-22 09:12:35');
INSERT INTO `t_photo` VALUES (12,'调试成功那晚的月亮',NULL,'长沙 · 宿舍','2025-11-02','1/1','linear-gradient(160deg,#2c3e50,#0f1a26)','🌕',12,1,'2026-09-22 09:12:35');

-- ----------------------------------------------------------
-- 背景音乐表（全站 BGM 歌单）
-- ----------------------------------------------------------
DROP TABLE IF EXISTS `t_music`;
CREATE TABLE `t_music` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '音乐ID',
  `title` varchar(60) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '曲名',
  `artist` varchar(60) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '作者/来源',
  `url` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '音频直链或 MinIO 地址',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序权重，越小越靠前',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态 1启用 2停用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_music_sort` (`status`,`sort_order`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='背景音乐表';

INSERT INTO `t_music` VALUES (1,'Outfoxing the Fox','Jan Morgenstern · CC BY 3.0','https://fastly.jsdelivr.net/gh/mdn/webaudio-examples@main/audio-basics/outfoxing.mp3',1,1,'2026-09-22 09:12:35');
INSERT INTO `t_music` VALUES (2,'Song No.1','SoundHelix · 免费示例曲','https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3',2,2,'2026-09-22 09:12:35');
INSERT INTO `t_music` VALUES (3,'Song No.2','SoundHelix · 免费示例曲','https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3',3,2,'2026-09-22 09:12:35');
INSERT INTO `t_music` VALUES (4,'Song No.3','SoundHelix · 免费示例曲','https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3',4,2,'2026-09-22 09:12:35');
INSERT INTO `t_music` VALUES (8,'周杰伦 - 明明就','本地-周杰伦','http://localhost:9000/jianyou-blog/2026/09/22/5f397dcf03f147e182dc57c774c2d5d1.flac',5,1,'2026-09-22 09:38:55');

-- 互动计数不从种子预置：点赞数 / 收藏数 / 浏览量由真实互动累计
UPDATE `t_article` SET like_count = 0, collect_count = 0, view_count = 0;

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 初始化完成：15 张表 + 演示种子数据
-- ============================================================
