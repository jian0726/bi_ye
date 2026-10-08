package com.jianyou.blog.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 后台标签新建/更新请求
 */
@Data
public class AdminTagDTO {

    @NotBlank(message = "标签名不能为空")
    private String name;

    private String slug;
    private String color;
}
