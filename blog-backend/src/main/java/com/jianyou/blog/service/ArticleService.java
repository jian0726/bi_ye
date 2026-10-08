package com.jianyou.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jianyou.blog.common.BusinessException;
import com.jianyou.blog.common.PageResult;
import com.jianyou.blog.entity.Article;
import com.jianyou.blog.entity.ArticleTag;
import com.jianyou.blog.entity.Category;
import com.jianyou.blog.entity.LikeRecord;
import com.jianyou.blog.entity.Tag;
import com.jianyou.blog.entity.User;
import com.jianyou.blog.mapper.ArticleMapper;
import com.jianyou.blog.mapper.ArticleTagMapper;
import com.jianyou.blog.mapper.CategoryMapper;
import com.jianyou.blog.mapper.TagMapper;
import com.jianyou.blog.mapper.UserMapper;
import com.jianyou.blog.mapper.ViewRecordMapper;
import com.jianyou.blog.vo.ArchiveGroupVO;
import com.jianyou.blog.vo.ArchiveItemVO;
import com.jianyou.blog.vo.ArticleVO;
import com.jianyou.blog.vo.CategoryVO;
import com.jianyou.blog.vo.TagVO;
import com.jianyou.blog.vo.UserBriefVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 文章服务：列表（分页/筛选/排序）、详情、热门、归档、点赞与收藏
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleService {

    private final ArticleMapper articleMapper;
    private final ArticleTagMapper articleTagMapper;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    private final UserMapper userMapper;
    private final LikeService likeService;
    private final CollectService collectService;
    private final ViewRecordMapper viewRecordMapper;

    /* ============================ 列表 ============================ */

    public PageResult<ArticleVO> page(long page, long size, Long categoryId, Long tagId,
                                      String keyword) {
        page = Math.max(page, 1);
        size = Math.min(Math.max(size, 1), 50);

        // tagId 筛选：先查出带该标签的文章 id 集合
        List<Long> tagArticleIds = null;
        if (tagId != null) {
            tagArticleIds = articleTagMapper.selectList(
                            new LambdaQueryWrapper<ArticleTag>().eq(ArticleTag::getTagId, tagId))
                    .stream().map(ArticleTag::getArticleId).toList();
            if (tagArticleIds.isEmpty()) {
                return new PageResult<>(List.of(), 0, page, size, 0);
            }
        }

        // 置顶优先，再按发布时间倒序
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<Article>()
                .eq(Article::getStatus, Article.STATUS_PUBLISHED)
                .eq(categoryId != null, Article::getCategoryId, categoryId)
                .in(tagArticleIds != null, Article::getId, tagArticleIds)
                .and(StringUtils.hasText(keyword), w -> w
                        .like(Article::getTitle, keyword)
                        .or().like(Article::getSummary, keyword))
                .orderByDesc(Article::getIsTop)
                .orderByDesc(Article::getPublishTime);

        Page<Article> result = articleMapper.selectPage(new Page<>(page, size), wrapper);
        List<ArticleVO> vos = fillVOs(result.getRecords(), false);
        return new PageResult<>(vos, result.getTotal(), page, size, result.getPages());
    }

    /* ============================ 详情 ============================ */

    /**
     * 文章详情（浏览量按「独立访客」去重）
     *
     * 计数规则：同一访客对同一篇文章终身只计一次。
     * t_view_record 的 uk_article_visitor 唯一索引是最终保障——
     * 并发下即使多个请求同时通过，也只有一个 INSERT 成功，浏览量不会重复累加。
     */
    public ArticleVO detail(Long id, String visitorKey) {
        Article article = articleMapper.selectById(id);
        if (article == null || article.getStatus() != Article.STATUS_PUBLISHED) {
            throw new BusinessException(404, "文章不存在或未发布");
        }
        // 首次访问才 +1（重复访问返回 0，展示值也不变）
        if (viewRecordMapper.insertIgnore(id, visitorKey) > 0) {
            articleMapper.update(null, new LambdaUpdateWrapper<Article>()
                    .eq(Article::getId, id)
                    .setSql("view_count = view_count + 1"));
            article.setViewCount(article.getViewCount() + 1);
        }

        return fillVOs(List.of(article), true).get(0);
    }

    /**
     * 按 id 集合取已发布文章（供收藏列表回查），并保持传入 id 的顺序
     * 未发布/已删除的 id 会被自动跳过（收藏后文章被下架的情况）
     */
    public List<ArticleVO> listPublishedByIds(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return List.of();
        }
        List<Article> articles = articleMapper.selectList(new LambdaQueryWrapper<Article>()
                .in(Article::getId, ids)
                .eq(Article::getStatus, Article.STATUS_PUBLISHED));

        Map<Long, ArticleVO> voById = fillVOs(articles, false).stream()
                .collect(Collectors.toMap(ArticleVO::getId, Function.identity()));

        return ids.stream().map(voById::get).filter(Objects::nonNull).toList();
    }

    /* ============================ 归档 ============================ */

    public List<ArchiveGroupVO> archive() {
        List<Article> list = articleMapper.selectList(new LambdaQueryWrapper<Article>()
                .eq(Article::getStatus, Article.STATUS_PUBLISHED)
                .select(Article::getId, Article::getTitle, Article::getCreateTime, Article::getPublishTime));

        Map<Integer, List<Article>> byYear = list.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getPublishTime() != null ? a.getPublishTime().getYear() : a.getCreateTime().getYear(),
                        LinkedHashMap::new,
                        Collectors.toList()));

        return byYear.entrySet().stream()
                .sorted(Map.Entry.<Integer, List<Article>>comparingByKey().reversed())
                .map(entry -> {
                    List<ArchiveItemVO> items = entry.getValue().stream()
                            .map(a -> ArchiveItemVO.builder()
                                    .id(a.getId()).title(a.getTitle())
                                    .createTime(a.getCreateTime()).publishTime(a.getPublishTime())
                                    .build())
                            .sorted(Comparator.comparing(ArchiveItemVO::getPublishTime,
                                    Comparator.nullsFirst(Comparator.naturalOrder())).reversed())
                            .toList();
                    return new ArchiveGroupVO(entry.getKey(), (long) items.size(), items);
                })
                .toList();
    }

    /* ============================ VO 组装 ============================ */

    /**
     * 批量组装 ArticleVO：分类、标签、作者各一次批量查询，避免 N+1
     */
    private List<ArticleVO> fillVOs(List<Article> articles, boolean withContent) {
        if (CollectionUtils.isEmpty(articles)) {
            return List.of();
        }

        // 分类
        Set<Long> categoryIds = articles.stream()
                .map(Article::getCategoryId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Category> categoryMap = categoryIds.isEmpty() ? Map.of()
                : categoryMapper.selectBatchIds(categoryIds).stream()
                        .collect(Collectors.toMap(Category::getId, Function.identity()));

        // 标签（article_tag → tag）
        List<Long> articleIds = articles.stream().map(Article::getId).toList();
        Map<Long, List<Long>> tagIdByArticle = articleTagMapper.selectList(
                        new LambdaQueryWrapper<ArticleTag>().in(ArticleTag::getArticleId, articleIds))
                .stream()
                .collect(Collectors.groupingBy(ArticleTag::getArticleId,
                        Collectors.mapping(ArticleTag::getTagId, Collectors.toList())));
        Set<Long> allTagIds = tagIdByArticle.values().stream()
                .flatMap(List::stream).collect(Collectors.toSet());
        Map<Long, Tag> tagMap = allTagIds.isEmpty() ? Map.of()
                : tagMapper.selectBatchIds(allTagIds).stream()
                        .collect(Collectors.toMap(Tag::getId, Function.identity()));

        // 作者
        Set<Long> authorIds = articles.stream()
                .map(Article::getAuthorId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, User> userMap = authorIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(authorIds).stream()
                        .collect(Collectors.toMap(User::getId, Function.identity()));

        // 点赞状态（未登录返回空集，字段恒为 false）
        Set<Long> likedIds = likeService.likedIdsOf(articleIds);

        // 收藏状态（同上；一次批量查询，避免列表页 N+1）
        Set<Long> collectedIds = collectService.collectedIdsOf(articleIds);

        return articles.stream().map(article -> {
            ArticleVO vo = new ArticleVO();
            vo.setId(article.getId());
            vo.setTitle(article.getTitle());
            vo.setSummary(article.getSummary());
            if (withContent) {
                vo.setContent(article.getContent());
            }
            vo.setCover(article.getCover());
            vo.setCategoryId(article.getCategoryId());
            Category category = categoryMap.get(article.getCategoryId());
            if (category != null) {
                vo.setCategory(CategoryVO.builder()
                        .id(category.getId()).name(category.getName()).slug(category.getSlug())
                        .description(category.getDescription()).icon(category.getIcon())
                        .sortOrder(category.getSortOrder()).articleCount(0L)
                        .build());
            }
            List<TagVO> tagVos = tagIdByArticle.getOrDefault(article.getId(), List.of()).stream()
                    .map(tagMap::get).filter(Objects::nonNull)
                    .map(t -> TagVO.builder()
                            .id(t.getId()).name(t.getName()).slug(t.getSlug())
                            .color(t.getColor()).articleCount(0L)
                            .build())
                    .toList();
            vo.setTags(tagVos);
            User user = userMap.get(article.getAuthorId());
            if (user != null) {
                vo.setAuthor(UserBriefVO.builder()
                        .id(user.getId()).nickname(user.getNickname()).avatar(user.getAvatar())
                        .build());
            }
            vo.setStatus(article.getStatus());
            vo.setIsTop(Boolean.TRUE.equals(article.getIsTop()));
            vo.setIsRecommend(Boolean.TRUE.equals(article.getIsRecommend()));
            vo.setViewCount(article.getViewCount());
            vo.setLikeCount(article.getLikeCount());
            vo.setCollectCount(article.getCollectCount());
            vo.setLiked(likedIds.contains(article.getId()));
            vo.setCollected(collectedIds.contains(article.getId()));
            vo.setWordCount(article.getWordCount());
            vo.setReadMinutes(article.getReadMinutes());
            vo.setPublishTime(article.getPublishTime());
            vo.setCreateTime(article.getCreateTime());
            return vo;
        }).toList();
    }
}
