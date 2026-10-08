package com.jianyou.blog.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 登录/注册成功响应：token + 用户信息（不含密码）
 */
@Data
@Builder
public class LoginVO {

    private String token;

    private Long userId;
    private String nickname;
    private String avatar;
    /** 留言页自定义背景图（null = 使用默认夜空） */
    private String messageBg;
    private String role;
}
