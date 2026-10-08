package com.jianyou.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文章点赞记录（用户 × 文章 关系）
 *
 * uk_user_article(user_id, target_id) 联合唯一索引保证
 * 同一用户对同一篇文章只有一条点赞记录，同时也是「我点过没」的查询依据。
 */
@Data
@TableName("t_like_record")
public class LikeRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 点赞人 */
    private Long userId;

    /** 被点赞文章ID */
    private Long targetId;

    private LocalDateTime createTime;
}
