package com.jianyou.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文章浏览记录（文章 × 独立访客 关系）
 *
 * uk_article_visitor(article_id, visitor_key) 联合唯一索引保证
 * 同一访客对同一篇文章只有一条记录，是浏览量的计数依据：
 * 插入成功（首次访问）才让 t_article.view_count + 1，
 * 重复访问被唯一索引挡下，浏览量不再随刷新增长。
 *
 * 访客标识 visitor_key 的取法见 util/VisitorKey：
 * 已登录 u:{userId}，游客 ip:{客户端IP}。
 */
@Data
@TableName("t_view_record")
public class ViewRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 被浏览文章ID */
    private Long articleId;

    /** 访客标识：登录用户 u:12；游客 ip:1.2.3.4 */
    private String visitorKey;

    /** 首次浏览时间 */
    private LocalDateTime createTime;
}
