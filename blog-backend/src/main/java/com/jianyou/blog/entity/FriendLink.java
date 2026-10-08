package com.jianyou.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 友情链接
 */
@Data
@TableName("t_friend_link")
public class FriendLink {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String url;

    private String logo;

    private String description;

    /** 0-待审核 1-已上架 2-下架 */
    private Integer status;

    private Integer sortOrder;

    private LocalDateTime createTime;
}
