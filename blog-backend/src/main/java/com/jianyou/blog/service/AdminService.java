package com.jianyou.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jianyou.blog.common.BusinessException;
import com.jianyou.blog.common.PageResult;
import com.jianyou.blog.dto.AdminArticleSaveDTO;
import com.jianyou.blog.dto.AdminCategoryDTO;
import com.jianyou.blog.dto.AdminTagDTO;
import com.jianyou.blog.entity.Article;
import com.jianyou.blog.entity.ArticleTag;
import com.jianyou.blog.entity.Category;
import com.jianyou.blog.entity.Collect;
import com.jianyou.blog.entity.FriendLink;
import com.jianyou.blog.entity.LikeRecord;
import com.jianyou.blog.entity.Message;
import com.jianyou.blog.entity.Tag;
import com.jianyou.blog.entity.UploadFile;
import com.jianyou.blog.entity.User;
import com.jianyou.blog.mapper.ArticleMapper;
import com.jianyou.blog.mapper.ArticleTagMapper;
import com.jianyou.blog.mapper.CategoryMapper;
import com.jianyou.blog.mapper.CollectMapper;
import com.jianyou.blog.mapper.FriendLinkMapper;
import com.jianyou.blog.mapper.LikeRecordMapper;
import com.jianyou.blog.mapper.MessageMapper;
import com.jianyou.blog.mapper.TagMapper;
import com.jianyou.blog.mapper.UploadFileMapper;
import com.jianyou.blog.mapper.UserMapper;
import com.jianyou.blog.config.MinioProperties;
import com.jianyou.blog.vo.ArticleVO;
import com.jianyou.blog.vo.CategoryVO;
import com.jianyou.blog.vo.TagVO;
import com.jianyou.blog.vo.UserBriefVO;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 管理后台服务：仪表盘统计 + 文章/分类/标签/留言/友链/资源/用户管理
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final ArticleMapper articleMapper;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    private final ArticleTagMapper articleTagMapper;
    private final LikeRecordMapper likeRecordMapper;
    private final CollectMapper collectMapper;
    private final MessageMapper messageMapper;
    private final FriendLinkMapper friendLinkMapper;
    private final UserMapper userMapper;
    private final UploadFileMapper uploadFileMapper;
    private final VisitService visitService;
    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    /* ============================ 仪表盘 ============================ */

    public Map<String, Object> dashboard() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("articleTotal", articleMapper.selectCount(null));
        data.put("publishedTotal", articleMapper.selectCount(
                new LambdaQueryWrapper<Article>().eq(Article::getStatus, Article.STATUS_PUBLISHED)));
        data.put("draftTotal", articleMapper.selectCount(
                new LambdaQueryWrapper<Article>().eq(Article::getStatus, Article.STATUS_DRAFT)));
        data.put("messageTotal", messageMapper.selectCount(null));
        data.put("linkPendingTotal", friendLinkMapper.selectCount(
                new LambdaQueryWrapper<FriendLink>().eq(FriendLink::getStatus, 0)));
        data.put("userTotal", userMapper.selectCount(null));

        // 全站浏览量（view_count 求和）
        List<Object> sum = articleMapper.selectObjs(
                new QueryWrapper<Article>().select("IFNULL(SUM(view_count), 0)"));
        data.put("viewTotal", sum.isEmpty() || sum.get(0) == null ? 0L : Long.parseLong(sum.get(0).toString()));

        // 今日访问埋点统计：今日访问总数 + 按省份分组
        data.putAll(visitService.todayStats());
        return data;
    }

    /* ============================ 文章管理 ============================ */

    /** 后台文章分页：含草稿/隐藏，支持关键词、状态、分类过滤 */
    public PageResult<ArticleVO> articlePage(long page, long size, String keyword, Integer status, Long categoryId) {
        page = Math.max(page, 1);
        size = Math.min(Math.max(size, 1), 50);

        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<Article>()
                .like(StringUtils.hasText(keyword), Article::getTitle, keyword)
                .eq(status != null, Article::getStatus, status)
                .eq(categoryId != null, Article::getCategoryId, categoryId)
                .orderByDesc(Article::getIsTop)
                .orderByDesc(Article::getCreateTime);

        Page<Article> result = articleMapper.selectPage(new Page<>(page, size), wrapper);
        List<ArticleVO> vos = fillArticleVOs(result.getRecords(), false);
        return new PageResult<>(vos, result.getTotal(), page, size, result.getPages());
    }

    /** 后台文章详情（含正文与标签） */
    public ArticleVO articleDetail(Long id) {
        Article article = articleMapper.selectById(id);
        if (article == null) {
            throw new BusinessException(404, "文章不存在");
        }
        return fillArticleVOs(List.of(article), true).get(0);
    }

    /** 新建文章（作者取当前登录管理员，即博主） */
    @Transactional(rollbackFor = Exception.class)
    public Long saveArticle(AdminArticleSaveDTO dto) {
        Article article = new Article();
        applyArticle(article, dto);
        article.setAuthorId(1L); // 博主
        article.setViewCount(0L);
        article.setLikeCount(0L);
        article.setCollectCount(0L);
        article.setCreateTime(LocalDateTime.now());
        article.setUpdateTime(LocalDateTime.now());
        articleMapper.insert(article);
        saveTags(article.getId(), dto.getTagIds());
        return article.getId();
    }

    /** 更新文章（含标签重挂） */
    @Transactional(rollbackFor = Exception.class)
    public void updateArticle(Long id, AdminArticleSaveDTO dto) {
        Article article = articleMapper.selectById(id);
        if (article == null) {
            throw new BusinessException(404, "文章不存在");
        }
        applyArticle(article, dto);
        article.setUpdateTime(LocalDateTime.now());
        articleMapper.updateById(article);
        articleTagMapper.delete(new LambdaQueryWrapper<ArticleTag>().eq(ArticleTag::getArticleId, id));
        saveTags(id, dto.getTagIds());
    }

    /** 删除文章：连带清理标签关联、点赞记录与收藏记录 */
    @Transactional(rollbackFor = Exception.class)
    public void deleteArticle(Long id) {
        Article article = articleMapper.selectById(id);
        if (article == null) {
            throw new BusinessException(404, "文章不存在");
        }
        articleTagMapper.delete(new LambdaQueryWrapper<ArticleTag>().eq(ArticleTag::getArticleId, id));
        likeRecordMapper.delete(new LambdaQueryWrapper<LikeRecord>().eq(LikeRecord::getTargetId, id));
        collectMapper.delete(new LambdaQueryWrapper<Collect>().eq(Collect::getArticleId, id));
        articleMapper.deleteById(id);
    }

    /** 发布 / 隐藏（status 1 或 2）；首次发布补 publishTime */
    public void setArticleStatus(Long id, int status) {
        if (status != Article.STATUS_PUBLISHED && status != Article.STATUS_HIDDEN) {
            throw new BusinessException(400, "状态不合法");
        }
        Article article = articleMapper.selectById(id);
        if (article == null) {
            throw new BusinessException(404, "文章不存在");
        }
        LambdaUpdateWrapper<Article> update = new LambdaUpdateWrapper<Article>()
                .eq(Article::getId, id)
                .set(Article::getStatus, status)
                .set(Article::getUpdateTime, LocalDateTime.now());
        if (status == Article.STATUS_PUBLISHED && article.getPublishTime() == null) {
            update.set(Article::getPublishTime, LocalDateTime.now());
        }
        articleMapper.update(null, update);
    }

    /** 置顶切换 */
    public void toggleArticleTop(Long id) {
        Article article = articleMapper.selectById(id);
        if (article == null) {
            throw new BusinessException(404, "文章不存在");
        }
        articleMapper.update(null, new LambdaUpdateWrapper<Article>()
                .eq(Article::getId, id)
                .set(Article::getIsTop, !Boolean.TRUE.equals(article.getIsTop()))
                .set(Article::getUpdateTime, LocalDateTime.now()));
    }

    /* ============================ 分类管理 ============================ */

    /** 全部分类（含发布文章数），复用门户统计口径 */
    public List<CategoryVO> categoryList() {
        List<Category> categories = categoryMapper.selectList(
                new LambdaQueryWrapper<Category>().orderByAsc(Category::getSortOrder));

        Map<Long, Long> countMap = articleMapper.selectList(new LambdaQueryWrapper<Article>()
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

    public Long saveCategory(AdminCategoryDTO dto) {
        Category category = new Category();
        applyCategory(category, dto);
        category.setCreateTime(LocalDateTime.now());
        categoryMapper.insert(category);
        return category.getId();
    }

    public void updateCategory(Long id, AdminCategoryDTO dto) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException(404, "分类不存在");
        }
        applyCategory(category, dto);
        categoryMapper.updateById(category);
    }

    /** 删除分类：分类下仍有文章时拒绝 */
    @Transactional(rollbackFor = Exception.class)
    public void deleteCategory(Long id) {
        Long used = articleMapper.selectCount(new LambdaQueryWrapper<Article>()
                .eq(Article::getCategoryId, id));
        if (used > 0) {
            throw new BusinessException(400, "该分类下仍有 " + used + " 篇文章，无法删除");
        }
        categoryMapper.deleteById(id);
    }

    /* ============================ 标签管理 ============================ */

    /** 全部标签（按文章数口径同后台：含草稿） */
    public List<TagVO> tagList() {
        List<Tag> tags = tagMapper.selectList(new LambdaQueryWrapper<Tag>().orderByAsc(Tag::getId));
        Map<Long, Long> countMap = articleTagMapper.selectList(null).stream()
                .collect(Collectors.groupingBy(ArticleTag::getTagId, Collectors.counting()));
        return tags.stream()
                .map(t -> TagVO.builder()
                        .id(t.getId()).name(t.getName()).slug(t.getSlug()).color(t.getColor())
                        .articleCount(countMap.getOrDefault(t.getId(), 0L))
                        .build())
                .toList();
    }

    public Long saveTag(AdminTagDTO dto) {
        Tag tag = new Tag();
        applyTag(tag, dto);
        tag.setCreateTime(LocalDateTime.now());
        tagMapper.insert(tag);
        return tag.getId();
    }

    public void updateTag(Long id, AdminTagDTO dto) {
        Tag tag = tagMapper.selectById(id);
        if (tag == null) {
            throw new BusinessException(404, "标签不存在");
        }
        applyTag(tag, dto);
        tagMapper.updateById(tag);
    }

    /** 删除标签：连带清理文章关联 */
    @Transactional(rollbackFor = Exception.class)
    public void deleteTag(Long id) {
        articleTagMapper.delete(new LambdaQueryWrapper<ArticleTag>().eq(ArticleTag::getTagId, id));
        tagMapper.deleteById(id);
    }

    /* ============================ 留言管理 ============================ */

    /** 留言分页：按 IP 属地模糊筛选，新留言在前 */
    public PageResult<com.jianyou.blog.vo.MessageVO> messagePage(long page, long size, String ipLocation) {
        page = Math.max(page, 1);
        size = Math.min(Math.max(size, 1), 50);

        Page<Message> result = messageMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Message>()
                        .like(StringUtils.hasText(ipLocation), Message::getIpLocation, ipLocation)
                        .orderByDesc(Message::getCreateTime));

        Set<Long> userIds = result.getRecords().stream()
                .map(Message::getUserId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, User> userMap = userIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(userIds).stream()
                        .collect(Collectors.toMap(User::getId, Function.identity()));

        List<com.jianyou.blog.vo.MessageVO> vos = result.getRecords().stream().map(m -> {
            com.jianyou.blog.vo.MessageVO vo = new com.jianyou.blog.vo.MessageVO();
            vo.setId(m.getId());
            User user = userMap.get(m.getUserId());
            if (user != null) {
                vo.setUser(UserBriefVO.builder()
                        .id(user.getId()).nickname(user.getNickname()).avatar(user.getAvatar())
                        .build());
            }
            vo.setNickname(m.getNickname());
            vo.setContent(m.getContent());
            vo.setIpLocation(m.getIpLocation());
            vo.setCreateTime(m.getCreateTime());
            return vo;
        }).toList();

        return new PageResult<>(vos, result.getTotal(), page, size, result.getPages());
    }

    public void deleteMessage(Long id) {
        if (messageMapper.selectById(id) == null) {
            throw new BusinessException(404, "留言不存在");
        }
        messageMapper.deleteById(id);
    }

    /* ============================ 用户管理 ============================ */

    /** 用户分页：手机号/邮箱/昵称关键词过滤，按注册先后排序 */
    public PageResult<com.jianyou.blog.vo.AdminUserVO> userPage(long page, long size, String keyword) {
        page = Math.max(page, 1);
        size = Math.min(Math.max(size, 1), 50);

        Page<User> result = userMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<User>()
                        .and(StringUtils.hasText(keyword), w -> w
                                .like(User::getPhone, keyword)
                                .or().like(User::getEmail, keyword)
                                .or().like(User::getNickname, keyword))
                        .orderByAsc(User::getId));

        List<com.jianyou.blog.vo.AdminUserVO> vos = result.getRecords().stream()
                .map(u -> com.jianyou.blog.vo.AdminUserVO.builder()
                        .id(u.getId()).phone(u.getPhone()).email(u.getEmail()).nickname(u.getNickname())
                        .avatar(u.getAvatar()).role(u.getRole()).status(u.getStatus())
                        .createTime(u.getCreateTime())
                        .build())
                .toList();
        return new PageResult<>(vos, result.getTotal(), page, size, result.getPages());
    }

    /** 启用 / 禁用用户（0-禁用 1-正常）；管理员账号受保护 */
    public void setUserStatus(Long id, int status) {
        if (status != 0 && status != 1) {
            throw new BusinessException(400, "状态不合法");
        }
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        if ("ADMIN".equals(user.getRole())) {
            throw new BusinessException(400, "管理员账号不允许禁用");
        }
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, id).set(User::getStatus, status));
    }

    /* ============================ 资源管理 ============================ */

    /** 上传资源分页：按原文件名与类型过滤（type 传 image / audio，按 MIME 前缀匹配），新资源在前 */
    public PageResult<UploadFile> filePage(long page, long size, String keyword, String type) {
        page = Math.max(page, 1);
        size = Math.min(Math.max(size, 1), 50);

        Page<UploadFile> result = uploadFileMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<UploadFile>()
                        .like(StringUtils.hasText(keyword), UploadFile::getName, keyword)
                        .likeRight(StringUtils.hasText(type), UploadFile::getType, type + "/")
                        .orderByDesc(UploadFile::getId));
        return new PageResult<>(result.getRecords(), result.getTotal(), page, size, result.getPages());
    }

    /** 删除资源：先删 MinIO 对象（失败仅告警），再删记录 */
    public void deleteFile(Long id) {
        UploadFile record = uploadFileMapper.selectById(id);
        if (record == null) {
            throw new BusinessException(404, "资源不存在");
        }
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(minioProperties.getBucket())
                    .object(record.getObjectKey())
                    .build());
        } catch (Exception e) {
            log.warn("MinIO 对象删除失败（记录仍将删除）: {}", record.getObjectKey(), e);
        }
        uploadFileMapper.deleteById(id);
    }

    /* ============================ 友链管理 ============================ */

    /** 全部友链（含待审核/下架），待审在前 */
    public List<FriendLink> linkList() {
        return friendLinkMapper.selectList(new LambdaQueryWrapper<FriendLink>()
                .orderByAsc(FriendLink::getStatus)
                .orderByAsc(FriendLink::getSortOrder)
                .orderByDesc(FriendLink::getCreateTime));
    }

    /** 新增友链（后台新增默认直接上架） */
    public Long saveLink(FriendLink link) {
        link.setId(null);
        if (link.getStatus() == null) {
            link.setStatus(1);
        }
        if (link.getSortOrder() == null) {
            link.setSortOrder(99);
        }
        link.setCreateTime(LocalDateTime.now());
        friendLinkMapper.insert(link);
        return link.getId();
    }

    public void updateLink(Long id, FriendLink link) {
        FriendLink exists = friendLinkMapper.selectById(id);
        if (exists == null) {
            throw new BusinessException(404, "友链不存在");
        }
        link.setId(id);
        friendLinkMapper.updateById(link);
    }

    /** 审核上架 / 下架（status 1 或 2） */
    public void setLinkStatus(Long id, int status) {
        if (status != 1 && status != 2) {
            throw new BusinessException(400, "状态不合法");
        }
        if (friendLinkMapper.selectById(id) == null) {
            throw new BusinessException(404, "友链不存在");
        }
        friendLinkMapper.update(null, new LambdaUpdateWrapper<FriendLink>()
                .eq(FriendLink::getId, id).set(FriendLink::getStatus, status));
    }

    public void deleteLink(Long id) {
        if (friendLinkMapper.selectById(id) == null) {
            throw new BusinessException(404, "友链不存在");
        }
        friendLinkMapper.deleteById(id);
    }

    /* ============================ 私有工具 ============================ */

    /** DTO → 实体（新建与更新共用）；字数与阅读分钟自动统计 */
    private void applyArticle(Article article, AdminArticleSaveDTO dto) {
        article.setTitle(dto.getTitle());
        article.setSummary(StringUtils.hasText(dto.getSummary()) ? dto.getSummary() : "");
        article.setContent(dto.getContent());
        article.setCover(dto.getCover());
        article.setCategoryId(dto.getCategoryId());
        if (dto.getStatus() != null) {
            article.setStatus(dto.getStatus());
            if (dto.getStatus() == Article.STATUS_PUBLISHED && article.getPublishTime() == null) {
                article.setPublishTime(LocalDateTime.now());
            }
        }
        article.setIsTop(Boolean.TRUE.equals(dto.getIsTop()));
        article.setIsRecommend(Boolean.TRUE.equals(dto.getIsRecommend()));

        // 字数（去空白）与预计阅读分钟（400 字/分钟）
        int words = dto.getContent() == null ? 0
                : dto.getContent().replaceAll("\\s", "").length();
        article.setWordCount(words);
        article.setReadMinutes(Math.max(1, (int) Math.ceil(words / 400.0)));
    }

    /** 保存文章-标签关联 */
    private void saveTags(Long articleId, List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }
        for (Long tagId : tagIds) {
            ArticleTag at = new ArticleTag();
            at.setArticleId(articleId);
            at.setTagId(tagId);
            articleTagMapper.insert(at);
        }
    }

    private void applyCategory(Category category, AdminCategoryDTO dto) {
        category.setName(dto.getName());
        category.setSlug(dto.getSlug());
        category.setDescription(dto.getDescription());
        category.setIcon(dto.getIcon());
        category.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 99);
    }

    private void applyTag(Tag tag, AdminTagDTO dto) {
        tag.setName(dto.getName());
        tag.setSlug(dto.getSlug());
        tag.setColor(dto.getColor());
    }

    /** 实体列表 → ArticleVO 列表（批量查分类/标签/作者，避免 N+1） */
    private List<ArticleVO> fillArticleVOs(List<Article> articles, boolean withContent) {
        if (articles.isEmpty()) {
            return List.of();
        }

        Set<Long> categoryIds = articles.stream().map(Article::getCategoryId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Category> categoryMap = categoryIds.isEmpty() ? Map.of()
                : categoryMapper.selectBatchIds(categoryIds).stream()
                        .collect(Collectors.toMap(Category::getId, Function.identity()));

        List<Long> articleIds = articles.stream().map(Article::getId).toList();
        Map<Long, List<Long>> tagIdMap = articleTagMapper.selectList(
                        new LambdaQueryWrapper<ArticleTag>().in(ArticleTag::getArticleId, articleIds)).stream()
                .collect(Collectors.groupingBy(ArticleTag::getArticleId,
                        Collectors.mapping(ArticleTag::getTagId, Collectors.toList())));
        Set<Long> tagIds = tagIdMap.values().stream().flatMap(List::stream).collect(Collectors.toSet());
        Map<Long, Tag> tagMap = tagIds.isEmpty() ? Map.of()
                : tagMapper.selectBatchIds(tagIds).stream()
                        .collect(Collectors.toMap(Tag::getId, Function.identity()));

        Set<Long> authorIds = articles.stream().map(Article::getAuthorId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, User> userMap = authorIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(authorIds).stream()
                        .collect(Collectors.toMap(User::getId, Function.identity()));

        List<ArticleVO> vos = new ArrayList<>();
        for (Article a : articles) {
            ArticleVO vo = new ArticleVO();
            vo.setId(a.getId());
            vo.setTitle(a.getTitle());
            vo.setSummary(a.getSummary());
            if (withContent) {
                vo.setContent(a.getContent());
            }
            vo.setCover(a.getCover());
            vo.setCategoryId(a.getCategoryId());
            Category category = categoryMap.get(a.getCategoryId());
            if (category != null) {
                vo.setCategory(CategoryVO.builder()
                        .id(category.getId()).name(category.getName()).slug(category.getSlug())
                        .description(category.getDescription()).icon(category.getIcon())
                        .sortOrder(category.getSortOrder()).articleCount(0L)
                        .build());
            }
            vo.setTags(tagIdMap.getOrDefault(a.getId(), List.of()).stream()
                    .map(tid -> {
                        Tag t = tagMap.get(tid);
                        return t == null ? null : TagVO.builder()
                                .id(t.getId()).name(t.getName()).slug(t.getSlug()).color(t.getColor())
                                .articleCount(0L).build();
                    })
                    .filter(Objects::nonNull).toList());
            User author = userMap.get(a.getAuthorId());
            if (author != null) {
                vo.setAuthor(UserBriefVO.builder()
                        .id(author.getId()).nickname(author.getNickname()).avatar(author.getAvatar())
                        .build());
            }
            vo.setStatus(a.getStatus());
            vo.setIsTop(Boolean.TRUE.equals(a.getIsTop()));
            vo.setIsRecommend(Boolean.TRUE.equals(a.getIsRecommend()));
            vo.setViewCount(a.getViewCount());
            vo.setLikeCount(a.getLikeCount());
            vo.setCollectCount(a.getCollectCount());
            vo.setWordCount(a.getWordCount());
            vo.setReadMinutes(a.getReadMinutes());
            vo.setPublishTime(a.getPublishTime());
            vo.setCreateTime(a.getCreateTime());
            vos.add(vo);
        }
        return vos;
    }
}
