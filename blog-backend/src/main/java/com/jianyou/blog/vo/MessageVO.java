package com.jianyou.blog.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 留言视图
 */
@Data
public class MessageVO {

    private Long id;

    /** 留言人（登录用户）；游客为 null */
    private UserBriefVO user;

    /** 游客昵称 */
    private String nickname;

    private String content;

    private String ipLocation;
    private LocalDateTime createTime;
}
