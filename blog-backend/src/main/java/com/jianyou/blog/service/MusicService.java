package com.jianyou.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.jianyou.blog.common.AuthContext;
import com.jianyou.blog.common.BusinessException;
import com.jianyou.blog.dto.AdminMusicSaveDTO;
import com.jianyou.blog.entity.Music;
import com.jianyou.blog.mapper.MusicMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 背景音乐服务：门户歌单 + 后台管理 + 用户投稿
 */
@Service
@RequiredArgsConstructor
public class MusicService {

    private final MusicMapper musicMapper;
    private final StorageService storageService;

    /* ============================ 门户 ============================ */

    /** 已启用曲目，按排序权重升序 */
    public List<Music> portalEnabled() {
        return musicMapper.selectList(new LambdaQueryWrapper<Music>()
                .eq(Music::getStatus, Music.STATUS_ON)
                .orderByAsc(Music::getSortOrder)
                .orderByAsc(Music::getId));
    }

    /* ============================ 用户上传（登录即可） ============================ */

    /** 用户上传一首音频并直接加入全站歌单（启用状态），曲名缺省用文件名去扩展名 */
    public Music uploadAndAdd(MultipartFile file, String title) {
        requireLogin();
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "请选择要上传的音频");
        }
        String url = storageService.store(file);
        Music music = new Music();
        music.setTitle(StringUtils.hasText(title) ? title.trim() : defaultTitle(file.getOriginalFilename()));
        music.setUrl(url);
        music.setSortOrder(99);
        music.setStatus(Music.STATUS_ON);
        music.setCreateTime(LocalDateTime.now());
        musicMapper.insert(music);
        return music;
    }

    /** 用户用音乐直链加入全站歌单（与后台保存同校验，启用状态） */
    public Long addByUser(AdminMusicSaveDTO dto) {
        requireLogin();
        Music music = new Music();
        apply(music, dto);
        music.setStatus(Music.STATUS_ON);
        music.setCreateTime(LocalDateTime.now());
        musicMapper.insert(music);
        return music.getId();
    }

    private void requireLogin() {
        if (AuthContext.getUserId() == null) {
            throw new BusinessException(401, "请先登录");
        }
    }

    private String defaultTitle(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "未命名曲目";
        }
        return filename.replaceAll("\\.(mp3|flac|wav|ogg|m4a|aac)$", "");
    }

    /* ============================ 管理 ============================ */

    /** 全部曲目（含停用） */
    public List<Music> adminList() {
        return musicMapper.selectList(new LambdaQueryWrapper<Music>()
                .orderByAsc(Music::getSortOrder)
                .orderByAsc(Music::getId));
    }

    public Long save(AdminMusicSaveDTO dto) {
        Music music = new Music();
        apply(music, dto);
        music.setStatus(Music.STATUS_ON);
        music.setCreateTime(LocalDateTime.now());
        musicMapper.insert(music);
        return music.getId();
    }

    public void update(Long id, AdminMusicSaveDTO dto) {
        Music music = musicMapper.selectById(id);
        if (music == null) {
            throw new BusinessException(404, "曲目不存在");
        }
        apply(music, dto);
        musicMapper.updateById(music);
    }

    public void delete(Long id) {
        if (musicMapper.selectById(id) == null) {
            throw new BusinessException(404, "曲目不存在");
        }
        musicMapper.deleteById(id);
    }

    /** 启用 / 停用（status 1 或 2） */
    public void setStatus(Long id, int status) {
        if (status != Music.STATUS_ON && status != Music.STATUS_OFF) {
            throw new BusinessException(400, "状态不合法");
        }
        if (musicMapper.selectById(id) == null) {
            throw new BusinessException(404, "曲目不存在");
        }
        musicMapper.update(null, new LambdaUpdateWrapper<Music>()
                .eq(Music::getId, id).set(Music::getStatus, status));
    }

    /* ============================ 私有工具 ============================ */

    private void apply(Music music, AdminMusicSaveDTO dto) {
        if (!StringUtils.hasText(dto.getTitle())) {
            throw new BusinessException(400, "请填写曲名");
        }
        if (!StringUtils.hasText(dto.getUrl())) {
            throw new BusinessException(400, "请上传音频或填写音频地址");
        }
        music.setTitle(dto.getTitle());
        music.setArtist(dto.getArtist());
        music.setUrl(dto.getUrl());
        music.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 99);
    }
}
