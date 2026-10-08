package com.jianyou.blog.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 登录/注册/刷新成功响应：双 token + 用户信息（不含密码）
 */
@Data
@Builder
public class LoginVO {

    /** 短效 access token（默认 2h），每次请求携带 */
    private String token;

    /** 长效 refresh token（默认 7 天），access 过期后静默换发用；服务端删除即撤销 */
    private String refreshToken;

    private Long userId;
    private String nickname;
    private String avatar;
    /** 留言页自定义背景图（null = 使用默认夜空） */
    private String messageBg;
    private String role;
}
