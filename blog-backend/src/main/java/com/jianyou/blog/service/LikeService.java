package com.jianyou.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.jianyou.blog.common.AuthContext;
import com.jianyou.blog.common.BusinessException;
import com.jianyou.blog.entity.Article;
import com.jianyou.blog.entity.LikeRecord;
import com.jianyou.blog.mapper.ArticleMapper;
import com.jianyou.blog.mapper.LikeRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 点赞服务：以「登录用户 id」为唯一维度，服务于文章点赞
 *
 * 设计要点（与数据库设计文档 3.7 like_record 一致）：
 * - t_like_record 上的 uk_user_article(user_id, target_id) 唯一索引是防重复点赞的最终保障，
 *   并发下即使两个请求同时通过存在性检查，也只有一个能插入成功
 * - 记录表是「我点过没」的真相来源，页面刷新后红心状态据此回显
 * - like_count 只作展示用的冗余计数，与记录表双写；取消点赞时用 GREATEST 兜底不为负
 * - 点赞是注册用户能力：未登录返回 401，由前端引导登录
 */
@Service
@RequiredArgsConstructor
public class LikeService {

    private final LikeRecordMapper likeRecordMapper;
    private final ArticleMapper articleMapper;

    /** 当前登录用户 id，未登录直接拒绝 */
    private Long requireUserId() {
        Long userId = AuthContext.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }
        return userId;
    }

    /** 点赞 / 取消点赞；返回操作后是否处于已点赞状态 */
    public boolean toggle(Long articleId, boolean add) {
        Long userId = requireUserId();
        validateTarget(articleId);

        if (add) {
            doLike(userId, articleId);
        } else {
            doUnlike(userId, articleId);
        }
        return add;
    }

    /** 当前用户是否已点赞（未登录返回 false，不报错） */
    public boolean isLiked(Long articleId) {
        Long userId = AuthContext.getUserId();
        if (userId == null) {
            return false;
        }
        return likeRecordMapper.selectCount(new LambdaQueryWrapper<LikeRecord>()
                .eq(LikeRecord::getUserId, userId)
                .eq(LikeRecord::getTargetId, articleId)) > 0;
    }

    /** 批量查询：当前用户在这批文章里点过赞的 id 集合（列表回显用，避免 N+1） */
    public Set<Long> likedIdsOf(Collection<Long> articleIds) {
        Long userId = AuthContext.getUserId();
        if (userId == null || articleIds == null || articleIds.isEmpty()) {
            return Set.of();
        }
        return likeRecordMapper.selectList(new LambdaQueryWrapper<LikeRecord>()
                        .eq(LikeRecord::getUserId, userId)
                        .in(LikeRecord::getTargetId, articleIds))
                .stream().map(LikeRecord::getTargetId).collect(Collectors.toSet());
    }

    /* ============================ 私有实现 ============================ */

    private void validateTarget(Long articleId) {
        Article article = articleMapper.selectById(articleId);
        if (article == null || article.getStatus() != Article.STATUS_PUBLISHED) {
            throw new BusinessException(404, "文章不存在或未发布");
        }
    }

    private void doLike(Long userId, Long articleId) {
        LikeRecord existing = likeRecordMapper.selectOne(new LambdaQueryWrapper<LikeRecord>()
                .eq(LikeRecord::getUserId, userId)
                .eq(LikeRecord::getTargetId, articleId)
                .last("LIMIT 1"));
        if (existing != null) {
            throw new BusinessException(400, "已经点过赞啦");
        }

        LikeRecord record = new LikeRecord();
        record.setUserId(userId);
        record.setTargetId(articleId);
        record.setCreateTime(LocalDateTime.now());
        try {
            likeRecordMapper.insert(record);
        } catch (DuplicateKeyException e) {
            // 并发重复提交：唯一索引兜底
            throw new BusinessException(400, "已经点过赞啦");
        }

        bumpCount(articleId, true);
    }

    private void doUnlike(Long userId, Long articleId) {
        int removed = likeRecordMapper.delete(new LambdaQueryWrapper<LikeRecord>()
                .eq(LikeRecord::getUserId, userId)
                .eq(LikeRecord::getTargetId, articleId));
        if (removed == 0) {
            throw new BusinessException(400, "还没有点过赞");
        }

        bumpCount(articleId, false);
    }

    /** 冗余计数双写（记录表为准，计数只用于展示） */
    private void bumpCount(Long articleId, boolean add) {
        String sql = add ? "like_count = like_count + 1" : "like_count = GREATEST(like_count - 1, 0)";
        articleMapper.update(null, new LambdaUpdateWrapper<Article>()
                .eq(Article::getId, articleId)
                .setSql(sql));
    }
}
