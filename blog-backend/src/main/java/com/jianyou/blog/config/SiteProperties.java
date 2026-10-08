package com.jianyou.blog.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 站点配置（application.yml 中 site.* 前缀）
 */
@Data
@Component
@ConfigurationProperties(prefix = "site")
public class SiteProperties {

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
