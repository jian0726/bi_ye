package com.jianyou.blog.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 发送验证码请求（找回密码用）
 */
@Data
public class SendCodeDTO {

    /** 注册时使用的手机号或邮箱 */
    @NotBlank(message = "请输入手机号或邮箱")
    private String account;

    /** 接收验证码的方式：phone / email */
    @NotBlank(message = "请选择接收验证码的方式")
    private String channel;
}
