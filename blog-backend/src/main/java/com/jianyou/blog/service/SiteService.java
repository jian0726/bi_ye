package com.jianyou.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jianyou.blog.config.SiteProperties;
import com.jianyou.blog.dto.SiteConfigUpdateDTO;
import com.jianyou.blog.entity.Article;
import com.jianyou.blog.entity.SiteConfig;
import com.jianyou.blog.mapper.ArticleMapper;
import com.jianyou.blog.mapper.CategoryMapper;
import com.jianyou.blog.mapper.SiteConfigMapper;
import com.jianyou.blog.mapper.TagMapper;
import com.jianyou.blog.vo.SiteConfigVO;
import com.jianyou.blog.vo.SiteStatsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 站点信息服务
 * 配置链路：application.yml 的 site.* 提供默认值，
 * 数据库 t_site_config 键值对按需覆盖（管理后台「网站设置」写入 DB）。
 */
@Service
@RequiredArgsConstructor
public class SiteService {

    private final SiteProperties properties;
    private final SiteConfigMapper siteConfigMapper;
    private final ArticleMapper articleMapper;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;

    public SiteConfigVO get() {
        Map<String, String> overrides = loadOverrides();
        SiteConfigVO vo = new SiteConfigVO();
        vo.setSiteName(resolve(overrides, "site-name", properties.getSiteName()));
        vo.setSiteSubtitle(resolve(overrides, "site-subtitle", properties.getSiteSubtitle()));
        vo.setSiteLogo(resolve(overrides, "site-logo", properties.getSiteLogo()));
        vo.setSiteDescription(resolve(overrides, "site-description", properties.getSiteDescription()));
        vo.setSiteKeywords(resolve(overrides, "site-keywords", properties.getSiteKeywords()));
        vo.setSiteAuthor(resolve(overrides, "site-author", properties.getSiteAuthor()));
        vo.setAuthorAvatar(resolve(overrides, "author-avatar", properties.getAuthorAvatar()));
        vo.setAuthorBio(resolve(overrides, "author-bio", properties.getAuthorBio()));
        vo.setIcpNumber(resolve(overrides, "icp-number", properties.getIcpNumber()));
        vo.setPoliceNumber(resolve(overrides, "police-number", properties.getPoliceNumber()));
        vo.setCopyright(resolve(overrides, "copyright", properties.getCopyright()));
        vo.setGithubUrl(resolve(overrides, "github-url", properties.getGithubUrl()));
        vo.setEmailAddress(resolve(overrides, "email-address", properties.getEmailAddress()));
        vo.setAboutContent(resolve(overrides, "about-content", properties.getAboutContent()));
        return vo;
    }

    /** 保存配置：非 null 字段逐键 upsert 到 t_site_config（键与 site.* kebab-case 一致） */
    @Transactional(rollbackFor = Exception.class)
    public void update(SiteConfigUpdateDTO dto) {
        upsert("site-name", dto.getSiteName());
        upsert("site-subtitle", dto.getSiteSubtitle());
        upsert("site-logo", dto.getSiteLogo());
        upsert("site-description", dto.getSiteDescription());
        upsert("site-keywords", dto.getSiteKeywords());
        upsert("site-author", dto.getSiteAuthor());
        upsert("author-avatar", dto.getAuthorAvatar());
        upsert("author-bio", dto.getAuthorBio());
        upsert("icp-number", dto.getIcpNumber());
        upsert("police-number", dto.getPoliceNumber());
        upsert("copyright", dto.getCopyright());
        upsert("github-url", dto.getGithubUrl());
        upsert("email-address", dto.getEmailAddress());
        upsert("about-content", dto.getAboutContent());
    }

    /* ============================ 站点统计 ============================ */

    /**
     * 站点统计（前台刊底「共 X 次阅读」与后台仪表盘同口径）
     *
     * viewCount 直接对 t_article.view_count 求和，不再由前端把「当页文章列表」的
     * 浏览数相加——那样首页只统计到当前这一页，与后台数字对不上。
     */
    public SiteStatsVO stats() {
        SiteStatsVO vo = new SiteStatsVO();
        vo.setArticleCount(articleMapper.selectCount(new LambdaQueryWrapper<Article>()
                .eq(Article::getStatus, Article.STATUS_PUBLISHED)));
        vo.setCategoryCount(categoryMapper.selectCount(null));
        vo.setTagCount(tagMapper.selectCount(null));

        List<Object> sum = articleMapper.selectObjs(
                new QueryWrapper<Article>().select("IFNULL(SUM(view_count), 0)"));
        vo.setViewCount(sum.isEmpty() || sum.get(0) == null ? 0L : Long.parseLong(sum.get(0).toString()));
        return vo;
    }

    /* ============================ 私有工具 ============================ */

    private Map<String, String> loadOverrides() {
        return siteConfigMapper.selectList(null).stream()
                .filter(c -> c.getConfigValue() != null)
                .collect(Collectors.toMap(SiteConfig::getConfigKey, SiteConfig::getConfigValue, (a, b) -> b));
    }

    /** DB 覆盖值优先，缺省回落 yml */
    private String resolve(Map<String, String> overrides, String key, String fallback) {
        String value = overrides.get(key);
        return value != null ? value : fallback;
    }

    private void upsert(String key, String value) {
        if (value == null) {
            return;
        }
        SiteConfig config = siteConfigMapper.selectById(key);
        if (config == null) {
            config = new SiteConfig();
            config.setConfigKey(key);
            config.setConfigValue(value);
            config.setUpdateTime(LocalDateTime.now());
            siteConfigMapper.insert(config);
        } else {
            config.setConfigValue(value);
            config.setUpdateTime(LocalDateTime.now());
            siteConfigMapper.updateById(config);
        }
    }
}
