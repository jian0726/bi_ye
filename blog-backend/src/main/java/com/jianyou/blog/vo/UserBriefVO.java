package com.jianyou.blog.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 用户简要信息（评论/留言/文章作者内嵌使用）
 */
@Data
@Builder
public class UserBriefVO {

    private Long id;

    private String nickname;

    private String avatar;
}
