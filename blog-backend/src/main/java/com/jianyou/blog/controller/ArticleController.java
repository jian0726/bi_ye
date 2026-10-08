package com.jianyou.blog.controller;

import com.jianyou.blog.common.PageResult;
import com.jianyou.blog.common.Result;
import com.jianyou.blog.service.ArticleService;
import com.jianyou.blog.service.CollectService;
import com.jianyou.blog.service.LikeService;
import com.jianyou.blog.vo.ArchiveGroupVO;
import com.jianyou.blog.vo.ArticleVO;
import com.jianyou.blog.util.VisitorKey;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 文章接口（路径与前端 api/index.ts 契约一一对应）
 */
@RestController
@RequestMapping("/portal/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;
    private final CollectService collectService;
    private final LikeService likeService;

    /** 文章列表：分页 / 分类 / 标签 / 关键词（排序固定为置顶优先 + 发布时间倒序） */
    @GetMapping
    public Result<PageResult<ArticleVO>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long tagId,
            @RequestParam(required = false) String keyword) {
        return Result.ok(articleService.page(page, size, categoryId, tagId, keyword));
    }

    /** 归档（按年分组） */
    @GetMapping("/archive")
    public Result<List<ArchiveGroupVO>> archive() {
        return Result.ok(articleService.archive());
    }

    /** 我的收藏（登录用户维度，按收藏时间倒序）—— 放在 /{id} 之前避免路径歧义 */
    @GetMapping("/me/collections")
    public Result<PageResult<ArticleVO>> myCollections(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return Result.ok(collectService.myCollections(page, size, articleService));
    }

    /** 文章详情（浏览量按独立访客去重，同一访客终身只计一次） */
    @GetMapping("/{id}")
    public Result<ArticleVO> detail(@PathVariable Long id, HttpServletRequest request) {
        return Result.ok(articleService.detail(id, VisitorKey.of(request)));
    }

    /** 点赞（按登录用户 id 落库到 t_like_record，未登录返回 401） */
    @PostMapping("/{id}/like")
    public Result<Void> like(@PathVariable Long id) {
        likeService.toggle(id, true);
        return Result.ok();
    }

    /** 取消点赞 */
    @DeleteMapping("/{id}/like")
    public Result<Void> unlike(@PathVariable Long id) {
        likeService.toggle(id, false);
        return Result.ok();
    }

    /** 收藏（按登录用户 id 落库到 t_collect，未登录返回 401） */
    @PostMapping("/{id}/collect")
    public Result<Void> collect(@PathVariable Long id) {
        collectService.toggle(id, true);
        return Result.ok();
    }

    /** 取消收藏 */
    @DeleteMapping("/{id}/collect")
    public Result<Void> uncollect(@PathVariable Long id) {
        collectService.toggle(id, false);
        return Result.ok();
    }

    /** 当前用户是否已收藏该文章（未登录返回 false，供详情页初始化按钮状态） */
    @GetMapping("/{id}/collected")
    public Result<Boolean> collected(@PathVariable Long id) {
        return Result.ok(collectService.isCollected(id));
    }
}
