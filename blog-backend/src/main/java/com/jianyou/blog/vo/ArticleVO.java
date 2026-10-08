package com.jianyou.blog.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文章视图（列表接口不含 content；详情接口含）
 */
@Data
public class ArticleVO {

    private Long id;
    private String title;
    private String summary;

    /** 正文 markdown，仅详情接口填充 */
    private String content;

    private String cover;
    private Long categoryId;
    private CategoryVO category;
    private List<TagVO> tags;
    private UserBriefVO author;

    private Integer status;
    private Boolean isTop;
    private Boolean isRecommend;

    private Long viewCount;
    private Long likeCount;
    private Long collectCount;

    /** 当前登录用户是否已点赞（未登录恒为 false） */
    private Boolean liked;

    /** 当前登录用户是否已收藏（未登录恒为 false） */
    private Boolean collected;

    private Integer wordCount;
    private Integer readMinutes;

    private LocalDateTime publishTime;
    private LocalDateTime createTime;
}
