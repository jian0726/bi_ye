package com.jianyou.blog.vo;

import lombok.Data;

/**
 * 站点配置视图（与前端 SiteConfig 接口字段一致）
 */
@Data
public class SiteConfigVO {

    private String siteName;
    private String siteSubtitle;
    private String siteLogo;
    private String siteDescription;
    private String siteKeywords;
    private String siteAuthor;
    private String authorAvatar;
    private String authorBio;
    private String icpNumber;
    private String policeNumber;
    private String copyright;
    private String githubUrl;
    private String emailAddress;
    private String aboutContent;
}
