
/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
DROP TABLE IF EXISTS `t_article`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `t_article_tag`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_article_tag` (
  `article_id` bigint NOT NULL COMMENT '文章ID',
  `tag_id` bigint NOT NULL COMMENT '标签ID',
  PRIMARY KEY (`article_id`,`tag_id`),
  KEY `idx_tag` (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章标签关联表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `t_category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `t_collect`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `t_file`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='上传资源表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `t_friend_link`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `t_like_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_like_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '点赞ID',
  `user_id` bigint NOT NULL COMMENT '点赞人ID',
  `target_id` bigint NOT NULL COMMENT '被点赞文章ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_article` (`user_id`,`target_id`),
  KEY `idx_target` (`target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章点赞记录表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `t_message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `t_music`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='背景音乐表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `t_photo`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `t_site_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_site_config` (
  `config_key` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '配置键（与 site.* kebab-case 一致，如 site-name）',
  `config_value` varchar(2000) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '配置值',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='站点配置表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `t_tag`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_tag` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '标签ID',
  `name` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '标签名',
  `slug` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'URL别名',
  `color` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '展示颜色',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='标签表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `t_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `t_view_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_view_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '浏览记录ID',
  `article_id` bigint NOT NULL COMMENT '被浏览文章ID',
  `visitor_key` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '访客标识：登录用户 u:{userId}；游客 ip:{客户端IP}',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '首次浏览时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_article_visitor` (`article_id`,`visitor_key`),
  KEY `idx_article` (`article_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章浏览记录表（独立访客去重）';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `t_visit_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_visit_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `ip` varchar(45) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '客户端IP',
  `province` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '未知' COMMENT '省份（IP归属地；本机/内网为「本地」）',
  `path` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '访问路径',
  `user_agent` varchar(300) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '浏览器UA',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '访问时间',
  PRIMARY KEY (`id`),
  KEY `idx_visit_time` (`create_time`),
  KEY `idx_visit_province` (`province`,`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=97 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='访问日志埋点表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

