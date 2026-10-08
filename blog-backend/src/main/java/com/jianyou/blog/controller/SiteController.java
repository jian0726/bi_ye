package com.jianyou.blog.controller;

import com.jianyou.blog.common.Result;
import com.jianyou.blog.service.SiteService;
import com.jianyou.blog.vo.SiteConfigVO;
import com.jianyou.blog.vo.SiteStatsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 站点信息接口
 */
@RestController
@RequestMapping("/portal/site")
@RequiredArgsConstructor
public class SiteController {

    private final SiteService siteService;

    @GetMapping("/config")
    public Result<SiteConfigVO> config() {
        return Result.ok(siteService.get());
    }

    /** 站点统计：文章数 / 分类数 / 标签数 / 全站累计浏览量 */
    @GetMapping("/stats")
    public Result<SiteStatsVO> stats() {
        return Result.ok(siteService.stats());
    }
}
