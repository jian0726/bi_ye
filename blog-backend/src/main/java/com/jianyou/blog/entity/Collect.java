package com.jianyou.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文章收藏（用户 × 文章 关系）
 * uk_user_article 联合唯一索引保证同一用户对同一文章只有一条记录
 */
@Data
@TableName("t_collect")
public class Collect {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 收藏人 */
    private Long userId;

    /** 被收藏文章 */
    private Long articleId;

    private LocalDateTime createTime;
}
