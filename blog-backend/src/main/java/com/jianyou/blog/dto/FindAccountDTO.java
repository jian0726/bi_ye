package com.jianyou.blog.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 找回密码第一步：用手机号或邮箱探账号（无需登录）
 */
@Data
public class FindAccountDTO {

    /** 注册时使用的手机号或邮箱 */
    @NotBlank(message = "请输入手机号或邮箱")
    private String account;
}
