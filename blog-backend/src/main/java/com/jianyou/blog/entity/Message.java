package com.jianyou.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 留言板留言
 */
@Data
@TableName("t_message")
public class Message {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 留言人；游客为 null，用 nickname 列 */
    private Long userId;

    /** 游客昵称 */
    private String nickname;

    /** 游客邮箱（不回显到前端列表，仅存档） */
    private String email;

    private String content;

    private String ipLocation;

    private LocalDateTime createTime;
}
