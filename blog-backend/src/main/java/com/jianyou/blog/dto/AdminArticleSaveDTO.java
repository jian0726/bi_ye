package com.jianyou.blog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 后台文章新建/更新请求
 */
@Data
public class AdminArticleSaveDTO {

    @NotBlank(message = "标题不能为空")
    @Size(max = 100, message = "标题最长 100 字")
    private String title;

    private String summary;

    private String content;

    private String cover;

    private Long categoryId;

    /** 标签 id 集合 */
    private List<Long> tagIds;

    /** 0-草稿 1-发布 2-隐藏 */
    private Integer status;

    private Boolean isTop;
    private Boolean isRecommend;
}
