package com.jianyou.blog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改个人资料（昵称 / 简介；头像走上传接口单独改）
 */
@Data
public class ProfileUpdateDTO {

    @NotBlank(message = "请输入昵称")
    @Size(min = 2, max = 20, message = "昵称 2-20 个字符")
    private String nickname;

    @Size(max = 255, message = "简介最多 255 字")
    private String bio;
}
