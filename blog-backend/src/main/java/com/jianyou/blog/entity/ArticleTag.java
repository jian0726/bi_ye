package com.jianyou.blog.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 文章-标签 关联
 */
@Data
@TableName("t_article_tag")
public class ArticleTag {

    private Long articleId;

    private Long tagId;
}
