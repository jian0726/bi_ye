package com.jianyou.blog.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 后台用户列表 VO（不回传密码）
 */
@Data
@Builder
public class AdminUserVO {

    private Long id;

    /** 手机号（可空） */
    private String phone;

    /** 邮箱（可空） */
    private String email;

    private String nickname;

    private String avatar;

    /** USER / ADMIN */
    private String role;

    /** 0-禁用 1-正常 */
    private Integer status;

    private LocalDateTime createTime;
}
