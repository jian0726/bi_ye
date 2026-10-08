package com.jianyou.blog.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.util.StringUtils;

/**
 * 注册请求（手机号 / 邮箱至少填一个 + 昵称 + 密码）
 * 个人博客不接入短信/邮件服务，手机号与邮箱仅做格式校验与唯一性约束；两者均可用于登录
 * 唯一的登录标识就是手机号 / 邮箱，不再设独立的用户名字段
 */
@Data
public class RegisterDTO {

    /** 手机号：与邮箱必须至少填一个；填了则须为合法大陆手机号 */
    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /** 邮箱：与手机号必须至少填一个；填了则须为合法邮箱 */
    @Pattern(regexp = "^$|^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", message = "邮箱格式不正确")
    private String email;

    /** 跨字段校验：手机号与邮箱不能同时为空 */
    @AssertTrue(message = "请填写手机号或邮箱")
    public boolean isContactPresent() {
        return StringUtils.hasText(phone) || StringUtils.hasText(email);
    }

    @NotBlank(message = "请输入昵称")
    @Size(min = 2, max = 20, message = "昵称 2-20 个字符")
    private String nickname;

    @NotBlank(message = "请输入密码")
    @Size(min = 6, max = 32, message = "密码 6-32 位")
    private String password;
}
