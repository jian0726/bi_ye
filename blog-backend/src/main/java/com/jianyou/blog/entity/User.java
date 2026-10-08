package com.jianyou.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户（博主与评论/留言用户）
 */
@Data
@TableName("t_user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 手机号（与邮箱至少一项，唯一，可用于登录） */
    private String phone;

    /** 邮箱（与手机号至少一项，唯一，可用于登录） */
    private String email;

    /** 密码（BCrypt 密文，任何接口不回传） */
    private String password;

    /** 昵称 */
    private String nickname;

    /** 头像地址 */
    private String avatar;

    /** 留言页自定义背景图地址（null = 使用默认夜空） */
    private String messageBg;

    /** 简介 */
    private String bio;

    /** 性别 0-未知 1-男 2-女 */
    private Integer gender;

    /** 角色 USER / ADMIN */
    private String role;

    /** 状态 0-禁用 1-正常 */
    private Integer status;

    private LocalDateTime createTime;
}
