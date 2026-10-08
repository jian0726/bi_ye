package com.jianyou.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 分类
 */
@Data
@TableName("t_category")
public class Category {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 分类名（记录 / 游记 / 随笔） */
    private String name;

    /** URL 别名 */
    private String slug;

    /** 分类描述 */
    private String description;

    /** 图标标识 */
    private String icon;

    /** 排序权重，越小越靠前 */
    private Integer sortOrder;

    private LocalDateTime createTime;
}
