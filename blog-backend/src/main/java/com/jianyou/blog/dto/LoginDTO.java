package com.jianyou.blog.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求（账号 / 手机号 / 邮箱 任一 + 密码）
 */
@Data
public class LoginDTO {

    @NotBlank(message = "请输入账号")
    private String account;

    @NotBlank(message = "请输入密码")
    private String password;
}
