package com.jianyou.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jianyou.blog.common.AuthContext;
import com.jianyou.blog.common.BusinessException;
import com.jianyou.blog.common.PageResult;
import com.jianyou.blog.entity.Article;
import com.jianyou.blog.entity.Collect;
import com.jianyou.blog.mapper.ArticleMapper;
import com.jianyou.blog.mapper.CollectMapper;
import com.jianyou.blog.vo.ArticleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * 收藏服务：以「登录用户 id」为唯一维度
 *
 * 设计要点（区别于点赞）：
 * - 点赞按 IP 去重即可（不需要"我赞过哪些"列表）；收藏必须落库成用户×文章关系，才能回答"我的收藏"
 * - t_collect 上的 uk_user_article 联合唯一索引是防重复收藏的最终保障，
 *   并发下即使两个请求同时通过存在性检查，也只有一个能插入成功
 * - collect_count 只作列表展示用的冗余计数，真相以 t_collect 为准
 */
@Service
@RequiredArgsConstructor
public class CollectService {

    private final CollectMapper collectMapper;
    private final ArticleMapper articleMapper;

    /** 当前登录用户 id，未登录直接拒绝 */
    private Long requireUserId() {
        Long userId = AuthContext.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }
        return userId;
    }

    /** 收藏 / 取消收藏；返回操作后是否处于已收藏状态 */
    public boolean toggle(Long articleId, boolean add) {
        Long userId = requireUserId();

        Article article = articleMapper.selectById(articleId);
        if (article == null || article.getStatus() != Article.STATUS_PUBLISHED) {
            throw new BusinessException(404, "文章不存在或未发布");
        }

        if (add) {
            doCollect(userId, articleId);
        } else {
            doUncollect(userId, articleId);
        }
        return add;
    }

    private void doCollect(Long userId, Long articleId) {
        Collect existing = collectMapper.selectOne(new LambdaQueryWrapper<Collect>()
                .eq(Collect::getUserId, userId)
                .eq(Collect::getArticleId, articleId)
                .last("LIMIT 1"));
        if (existing != null) {
            throw new BusinessException(400, "已经收藏过啦");
        }

        Collect collect = new Collect();
        collect.setUserId(userId);
        collect.setArticleId(articleId);
        collect.setCreateTime(LocalDateTime.now());
        try {
            collectMapper.insert(collect);
        } catch (DuplicateKeyException e) {
            // 并发重复提交：唯一索引兜底
            throw new BusinessException(400, "已经收藏过啦");
        }

        articleMapper.update(null, new LambdaUpdateWrapper<Article>()
                .eq(Article::getId, articleId)
                .setSql("collect_count = collect_count + 1"));
    }

    private void doUncollect(Long userId, Long articleId) {
        int removed = collectMapper.delete(new LambdaQueryWrapper<Collect>()
                .eq(Collect::getUserId, userId)
                .eq(Collect::getArticleId, articleId));
        if (removed == 0) {
            throw new BusinessException(400, "还没有收藏");
        }

        articleMapper.update(null, new LambdaUpdateWrapper<Article>()
                .eq(Article::getId, articleId)
                .setSql("collect_count = GREATEST(collect_count - 1, 0)"));
    }

    /** 当前用户是否收藏了某篇文章（未登录返回 false，不报错） */
    public boolean isCollected(Long articleId) {
        Long userId = AuthContext.getUserId();
        if (userId == null) {
            return false;
        }
        return collectMapper.selectCount(new LambdaQueryWrapper<Collect>()
                .eq(Collect::getUserId, userId)
                .eq(Collect::getArticleId, articleId)) > 0;
    }

    /** 批量查询：当前用户在这批文章里收藏了哪些（列表页标记用，避免 N+1） */
    public Set<Long> collectedIdsOf(List<Long> articleIds) {
        Long userId = AuthContext.getUserId();
        if (userId == null || articleIds == null || articleIds.isEmpty()) {
            return Set.of();
        }
        return collectMapper.selectList(new LambdaQueryWrapper<Collect>()
                        .eq(Collect::getUserId, userId)
                        .in(Collect::getArticleId, articleIds))
                .stream().map(Collect::getArticleId).collect(java.util.stream.Collectors.toSet());
    }

    /**
     * 我的收藏列表（按收藏时间倒序，只返回仍处于已发布状态的文章）
     * 分页在关系表上做，再按 articleId 回查文章详情
     */
    public PageResult<ArticleVO> myCollections(long page, long size, ArticleService articleService) {
        Long userId = requireUserId();
        page = Math.max(page, 1);
        size = Math.min(Math.max(size, 1), 50);

        Page<Collect> collectPage = collectMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<Collect>()
                        .eq(Collect::getUserId, userId)
                        .orderByDesc(Collect::getCreateTime));

        List<Long> articleIds = collectPage.getRecords().stream()
                .map(Collect::getArticleId).toList();
        if (articleIds.isEmpty()) {
            return new PageResult<>(List.of(), 0, page, size, 0);
        }

        // 按收藏顺序还原文章顺序（selectBatchIds 不保证顺序）
        List<ArticleVO> vos = articleService.listPublishedByIds(articleIds);

        return new PageResult<>(vos, collectPage.getTotal(), page, size, collectPage.getPages());
    }
}
