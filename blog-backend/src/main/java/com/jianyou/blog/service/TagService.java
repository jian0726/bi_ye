package com.jianyou.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jianyou.blog.entity.Article;
import com.jianyou.blog.entity.ArticleTag;
import com.jianyou.blog.entity.Tag;
import com.jianyou.blog.mapper.ArticleMapper;
import com.jianyou.blog.mapper.ArticleTagMapper;
import com.jianyou.blog.mapper.TagMapper;
import com.jianyou.blog.vo.TagVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 标签服务
 */
@Service
@RequiredArgsConstructor
public class TagService {

    private final TagMapper tagMapper;
    private final ArticleTagMapper articleTagMapper;
    private final ArticleMapper articleMapper;

    /** 全部标签（含已发布文章数），按文章数降序 */
    public List<TagVO> listAll() {
        List<Tag> tags = tagMapper.selectList(new LambdaQueryWrapper<Tag>().orderByAsc(Tag::getId));

        // 已发布文章 id 集合
        Set<Long> publishedIds = articleMapper.selectList(new LambdaQueryWrapper<Article>()
                        .eq(Article::getStatus, Article.STATUS_PUBLISHED)
                        .select(Article::getId)).stream()
                .map(Article::getId).collect(Collectors.toSet());

        // 关联计数（只统计已发布文章；无已发布文章时直接返回零计数）
        Map<Long, Long> countMap = publishedIds.isEmpty() ? Map.of()
                : articleTagMapper.selectList(
                        new LambdaQueryWrapper<ArticleTag>().in(ArticleTag::getArticleId, publishedIds)).stream()
                .collect(Collectors.groupingBy(ArticleTag::getTagId, Collectors.counting()));

        return tags.stream()
                .map(t -> TagVO.builder()
                        .id(t.getId()).name(t.getName()).slug(t.getSlug()).color(t.getColor())
                        .articleCount(countMap.getOrDefault(t.getId(), 0L))
                        .build())
                .toList();
    }
}
