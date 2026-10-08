package com.jianyou.blog.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 标签视图
 */
@Data
@Builder
public class TagVO {

    private Long id;

    private String name;

    private String slug;

    private String color;

    /** 已发布文章数 */
    private Long articleCount;
}
