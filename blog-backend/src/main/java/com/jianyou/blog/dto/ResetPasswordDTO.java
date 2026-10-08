package com.jianyou.blog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 找回密码第二步：验证码 + 新密码（无需登录）
 */
@Data
public class ResetPasswordDTO {

    /** 注册时使用的手机号或邮箱 */
    @NotBlank(message = "请输入手机号或邮箱")
    private String account;

    /** 接收验证码的通道：phone / email */
    @NotBlank(message = "请选择接收验证码的方式")
    private String channel;

    @NotBlank(message = "请输入验证码")
    private String code;

    @NotBlank(message = "请输入新密码")
    @Size(min = 6, max = 32, message = "密码 6-32 位")
    private String newPassword;
}
