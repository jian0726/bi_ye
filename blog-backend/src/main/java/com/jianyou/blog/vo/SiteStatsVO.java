package com.jianyou.blog.vo;

import lombok.Data;

/**
 * 站点统计（前台刊底/关于页展示，与后台仪表盘同口径）
 *
 * viewCount = SUM(t_article.view_count)，即全站累计浏览量，
 * 不再由前端把「当页文章列表」的浏览数求和，避免首页与后台数字互相打架。
 */
@Data
public class SiteStatsVO {

    /** 已发布文章数 */
    private long articleCount;

    /** 分类数 */
    private long categoryCount;

    /** 标签数 */
    private long tagCount;

    /** 全站累计浏览量 */
    private long viewCount;
}
