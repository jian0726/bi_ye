package com.jianyou.blog.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 分类视图
 */
@Data
@Builder
public class CategoryVO {

    private Long id;

    private String name;

    private String slug;

    private String description;

    private String icon;

    private Integer sortOrder;

    /** 已发布文章数 */
    private Long articleCount;
}
