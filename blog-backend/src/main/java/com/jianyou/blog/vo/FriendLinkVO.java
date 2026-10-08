package com.jianyou.blog.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 友链视图
 */
@Data
@Builder
public class FriendLinkVO {

    private Long id;
    private String name;
    private String url;
    private String logo;
    private String description;
}
