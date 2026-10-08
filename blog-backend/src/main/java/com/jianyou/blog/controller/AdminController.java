package com.jianyou.blog.controller;

import com.jianyou.blog.common.PageResult;
import com.jianyou.blog.common.Result;
import com.jianyou.blog.dto.AdminArticleSaveDTO;
import com.jianyou.blog.dto.AdminCategoryDTO;
import com.jianyou.blog.dto.AdminMusicSaveDTO;
import com.jianyou.blog.dto.AdminPhotoSaveDTO;
import com.jianyou.blog.dto.AdminTagDTO;
import com.jianyou.blog.dto.SiteConfigUpdateDTO;
import com.jianyou.blog.entity.FriendLink;
import com.jianyou.blog.entity.Music;
import com.jianyou.blog.entity.Photo;
import com.jianyou.blog.entity.UploadFile;
import com.jianyou.blog.service.AdminService;
import com.jianyou.blog.service.AlbumService;
import com.jianyou.blog.service.MusicService;
import com.jianyou.blog.service.SiteService;
import com.jianyou.blog.vo.AdminUserVO;
import com.jianyou.blog.vo.ArticleVO;
import com.jianyou.blog.vo.CategoryVO;
import com.jianyou.blog.vo.MessageVO;
import com.jianyou.blog.vo.SiteConfigVO;
import com.jianyou.blog.vo.TagVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 管理后台接口（/admin/**，由 AuthInterceptor 强制 ADMIN 鉴权）
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final SiteService siteService;
    private final AlbumService albumService;
    private final MusicService musicService;

    /* ============================ 仪表盘 ============================ */

    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard() {
        return Result.ok(adminService.dashboard());
    }

    /* ============================ 网站设置 ============================ */

    @GetMapping("/site/config")
    public Result<SiteConfigVO> siteConfig() {
        return Result.ok(siteService.get());
    }

    @PutMapping("/site/config")
    public Result<Void> updateSiteConfig(@RequestBody SiteConfigUpdateDTO dto) {
        siteService.update(dto);
        return Result.ok();
    }

    /* ============================ 相册管理 ============================ */

    @GetMapping("/photos")
    public Result<List<Photo>> photos() {
        return Result.ok(albumService.adminList());
    }

    @PostMapping("/photos")
    public Result<Long> savePhoto(@RequestBody AdminPhotoSaveDTO dto) {
        return Result.ok(albumService.save(dto));
    }

    @PutMapping("/photos/{id}")
    public Result<Void> updatePhoto(@PathVariable Long id, @RequestBody AdminPhotoSaveDTO dto) {
        albumService.update(id, dto);
        return Result.ok();
    }

    /** 显示 / 隐藏（status 1 或 2） */
    @PutMapping("/photos/{id}/status")
    public Result<Void> setPhotoStatus(@PathVariable Long id, @RequestParam int status) {
        albumService.setStatus(id, status);
        return Result.ok();
    }

    @DeleteMapping("/photos/{id}")
    public Result<Void> deletePhoto(@PathVariable Long id) {
        albumService.delete(id);
        return Result.ok();
    }

    /* ============================ 音乐管理 ============================ */

    @GetMapping("/musics")
    public Result<List<Music>> musics() {
        return Result.ok(musicService.adminList());
    }

    @PostMapping("/musics")
    public Result<Long> saveMusic(@RequestBody AdminMusicSaveDTO dto) {
        return Result.ok(musicService.save(dto));
    }

    @PutMapping("/musics/{id}")
    public Result<Void> updateMusic(@PathVariable Long id, @RequestBody AdminMusicSaveDTO dto) {
        musicService.update(id, dto);
        return Result.ok();
    }

    /** 启用 / 停用（status 1 或 2） */
    @PutMapping("/musics/{id}/status")
    public Result<Void> setMusicStatus(@PathVariable Long id, @RequestParam int status) {
        musicService.setStatus(id, status);
        return Result.ok();
    }

    @DeleteMapping("/musics/{id}")
    public Result<Void> deleteMusic(@PathVariable Long id) {
        musicService.delete(id);
        return Result.ok();
    }

    /* ============================ 文章管理 ============================ */

    @GetMapping("/articles")
    public Result<PageResult<ArticleVO>> articles(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long categoryId) {
        return Result.ok(adminService.articlePage(page, size, keyword, status, categoryId));
    }

    @GetMapping("/articles/{id}")
    public Result<ArticleVO> articleDetail(@PathVariable Long id) {
        return Result.ok(adminService.articleDetail(id));
    }

    @PostMapping("/articles")
    public Result<Long> saveArticle(@Valid @RequestBody AdminArticleSaveDTO dto) {
        return Result.ok(adminService.saveArticle(dto));
    }

    @PutMapping("/articles/{id}")
    public Result<Void> updateArticle(@PathVariable Long id, @Valid @RequestBody AdminArticleSaveDTO dto) {
        adminService.updateArticle(id, dto);
        return Result.ok();
    }

    @DeleteMapping("/articles/{id}")
    public Result<Void> deleteArticle(@PathVariable Long id) {
        adminService.deleteArticle(id);
        return Result.ok();
    }

    /** 发布 / 隐藏 */
    @PutMapping("/articles/{id}/status")
    public Result<Void> setArticleStatus(@PathVariable Long id, @RequestParam int status) {
        adminService.setArticleStatus(id, status);
        return Result.ok();
    }

    /** 置顶切换 */
    @PutMapping("/articles/{id}/top")
    public Result<Void> toggleArticleTop(@PathVariable Long id) {
        adminService.toggleArticleTop(id);
        return Result.ok();
    }

    /* ============================ 分类管理 ============================ */

    @GetMapping("/categories")
    public Result<List<CategoryVO>> categories() {
        return Result.ok(adminService.categoryList());
    }

    @PostMapping("/categories")
    public Result<Long> saveCategory(@Valid @RequestBody AdminCategoryDTO dto) {
        return Result.ok(adminService.saveCategory(dto));
    }

    @PutMapping("/categories/{id}")
    public Result<Void> updateCategory(@PathVariable Long id, @Valid @RequestBody AdminCategoryDTO dto) {
        adminService.updateCategory(id, dto);
        return Result.ok();
    }

    @DeleteMapping("/categories/{id}")
    public Result<Void> deleteCategory(@PathVariable Long id) {
        adminService.deleteCategory(id);
        return Result.ok();
    }

    /* ============================ 标签管理 ============================ */

    @GetMapping("/tags")
    public Result<List<TagVO>> tags() {
        return Result.ok(adminService.tagList());
    }

    @PostMapping("/tags")
    public Result<Long> saveTag(@Valid @RequestBody AdminTagDTO dto) {
        return Result.ok(adminService.saveTag(dto));
    }

    @PutMapping("/tags/{id}")
    public Result<Void> updateTag(@PathVariable Long id, @Valid @RequestBody AdminTagDTO dto) {
        adminService.updateTag(id, dto);
        return Result.ok();
    }

    @DeleteMapping("/tags/{id}")
    public Result<Void> deleteTag(@PathVariable Long id) {
        adminService.deleteTag(id);
        return Result.ok();
    }

    /* ============================ 留言管理 ============================ */

    @GetMapping("/messages")
    public Result<PageResult<MessageVO>> messages(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String ipLocation) {
        return Result.ok(adminService.messagePage(page, size, ipLocation));
    }

    @DeleteMapping("/messages/{id}")
    public Result<Void> deleteMessage(@PathVariable Long id) {
        adminService.deleteMessage(id);
        return Result.ok();
    }

    /* ============================ 友链管理 ============================ */

    @GetMapping("/links")
    public Result<List<FriendLink>> links() {
        return Result.ok(adminService.linkList());
    }

    @PostMapping("/links")
    public Result<Long> saveLink(@RequestBody FriendLink link) {
        return Result.ok(adminService.saveLink(link));
    }

    @PutMapping("/links/{id}")
    public Result<Void> updateLink(@PathVariable Long id, @RequestBody FriendLink link) {
        adminService.updateLink(id, link);
        return Result.ok();
    }

    @PutMapping("/links/{id}/status")
    public Result<Void> setLinkStatus(@PathVariable Long id, @RequestParam int status) {
        adminService.setLinkStatus(id, status);
        return Result.ok();
    }

    @DeleteMapping("/links/{id}")
    public Result<Void> deleteLink(@PathVariable Long id) {
        adminService.deleteLink(id);
        return Result.ok();
    }

    /* ============================ 用户管理 ============================ */

    @GetMapping("/users")
    public Result<PageResult<AdminUserVO>> users(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword) {
        return Result.ok(adminService.userPage(page, size, keyword));
    }

    /** 启用 / 禁用用户（status 0 或 1） */
    @PutMapping("/users/{id}/status")
    public Result<Void> setUserStatus(@PathVariable Long id, @RequestParam int status) {
        adminService.setUserStatus(id, status);
        return Result.ok();
    }

    /* ============================ 资源管理 ============================ */

    @GetMapping("/files")
    public Result<PageResult<UploadFile>> files(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type) {
        return Result.ok(adminService.filePage(page, size, keyword, type));
    }

    @DeleteMapping("/files/{id}")
    public Result<Void> deleteFile(@PathVariable Long id) {
        adminService.deleteFile(id);
        return Result.ok();
    }
}
