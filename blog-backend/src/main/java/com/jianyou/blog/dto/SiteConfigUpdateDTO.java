package com.jianyou.blog.dto;

import lombok.Data;

/**
 * 站点配置更新（管理后台「网站设置」表单；字段为 null 时不覆盖）
 */
@Data
public class SiteConfigUpdateDTO {

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
