package com.jianyou.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jianyou.blog.entity.Article;
import com.jianyou.blog.entity.Category;
import com.jianyou.blog.mapper.ArticleMapper;
import com.jianyou.blog.mapper.CategoryMapper;
import com.jianyou.blog.vo.CategoryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 分类服务
 */
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryMapper categoryMapper;
    private final ArticleMapper articleMapper;

    /** 全部分类（含已发布文章数），按 sortOrder 升序 */
    public List<CategoryVO> listAll() {
        List<Category> categories = categoryMapper.selectList(
                new LambdaQueryWrapper<Category>().orderByAsc(Category::getSortOrder));

        // 已发布文章按分类计数
        Map<Long, Long> countMap = articleMapper.selectList(new LambdaQueryWrapper<Article>()
                        .eq(Article::getStatus, Article.STATUS_PUBLISHED)
                        .select(Article::getCategoryId)).stream()
                .map(Article::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(id -> id, Collectors.counting()));

        return categories.stream()
                .map(c -> CategoryVO.builder()
                        .id(c.getId()).name(c.getName()).slug(c.getSlug())
                        .description(c.getDescription()).icon(c.getIcon())
                        .sortOrder(c.getSortOrder())
                        .articleCount(countMap.getOrDefault(c.getId(), 0L))
                        .build())
                .toList();
    }
}
