package com.jianyou.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文章
 */
@Data
@TableName("t_article")
public class Article {

    public static final int STATUS_DRAFT = 0;
    public static final int STATUS_PUBLISHED = 1;
    public static final int STATUS_HIDDEN = 2;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    /** 摘要（列表页展示） */
    private String summary;

    /** 正文 markdown（详情接口才返回） */
    private String content;

    /** 封面图 */
    private String cover;

    private Long categoryId;

    /** 作者（博主）用户 id */
    private Long authorId;

    /** 状态 0-草稿 1-已发布 2-隐藏 */
    private Integer status;

    /** 是否置顶 */
    private Boolean isTop;

    /** 是否推荐 */
    private Boolean isRecommend;

    private Long viewCount;
    private Long likeCount;
    private Long collectCount;

    /** 字数（统计写入） */
    private Integer wordCount;

    /** 预计阅读分钟数 */
    private Integer readMinutes;

    private LocalDateTime publishTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
