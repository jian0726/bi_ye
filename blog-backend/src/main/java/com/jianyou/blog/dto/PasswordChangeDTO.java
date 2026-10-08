package com.jianyou.blog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改密码（需校验旧密码）
 */
@Data
public class PasswordChangeDTO {

    @NotBlank(message = "请输入当前密码")
    private String oldPassword;

    @NotBlank(message = "请输入新密码")
    @Size(min = 6, max = 32, message = "新密码 6-32 位")
    private String newPassword;
}
