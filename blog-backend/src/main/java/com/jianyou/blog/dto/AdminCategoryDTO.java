package com.jianyou.blog.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 后台分类新建/更新请求
 */
@Data
public class AdminCategoryDTO {

    @NotBlank(message = "分类名不能为空")
    private String name;

    private String slug;
    private String description;
    private String icon;
    private Integer sortOrder;
}
