package com.jianyou.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jianyou.blog.common.BusinessException;
import com.jianyou.blog.dto.AdminPhotoSaveDTO;
import com.jianyou.blog.entity.Photo;
import com.jianyou.blog.mapper.PhotoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * 相册服务：门户照片墙 + 后台管理
 */
@Service
@RequiredArgsConstructor
public class AlbumService {

    private final PhotoMapper photoMapper;

    /* ============================ 门户 ============================ */

    /** 已显示照片，按排序权重升序 */
    public List<Photo> portalList() {
        return photoMapper.selectList(new LambdaQueryWrapper<Photo>()
                .eq(Photo::getStatus, Photo.STATUS_SHOW)
                .orderByAsc(Photo::getSortOrder)
                .orderByAsc(Photo::getId));
    }

    /* ============================ 管理 ============================ */

    /** 全部照片（含隐藏） */
    public List<Photo> adminList() {
        return photoMapper.selectList(new LambdaQueryWrapper<Photo>()
                .orderByAsc(Photo::getSortOrder)
                .orderByAsc(Photo::getId));
    }

    public Long save(AdminPhotoSaveDTO dto) {
        Photo photo = new Photo();
        apply(photo, dto);
        photo.setStatus(Photo.STATUS_SHOW);
        photo.setCreateTime(java.time.LocalDateTime.now());
        photoMapper.insert(photo);
        return photo.getId();
    }

    public void update(Long id, AdminPhotoSaveDTO dto) {
        Photo photo = photoMapper.selectById(id);
        if (photo == null) {
            throw new BusinessException(404, "照片不存在");
        }
        apply(photo, dto);
        photoMapper.updateById(photo);
    }

    public void delete(Long id) {
        if (photoMapper.selectById(id) == null) {
            throw new BusinessException(404, "照片不存在");
        }
        photoMapper.deleteById(id);
    }

    /** 显示 / 隐藏（status 1 或 2） */
    public void setStatus(Long id, int status) {
        if (status != Photo.STATUS_SHOW && status != Photo.STATUS_HIDDEN) {
            throw new BusinessException(400, "状态不合法");
        }
        if (photoMapper.selectById(id) == null) {
            throw new BusinessException(404, "照片不存在");
        }
        photoMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Photo>()
                        .eq(Photo::getId, id).set(Photo::getStatus, status));
    }

    /* ============================ 私有工具 ============================ */

    private void apply(Photo photo, AdminPhotoSaveDTO dto) {
        if (!StringUtils.hasText(dto.getTitle())) {
            throw new BusinessException(400, "请填写标题");
        }
        photo.setTitle(dto.getTitle());
        photo.setUrl(dto.getUrl());
        photo.setLocation(dto.getLocation());
        photo.setTakenDate(parseDate(dto.getTakenDate()));
        photo.setRatio(StringUtils.hasText(dto.getRatio()) ? dto.getRatio() : "1/1");
        photo.setTone(dto.getTone());
        photo.setEmoji(dto.getEmoji());
        photo.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 99);
    }

    private LocalDate parseDate(String date) {
        if (!StringUtils.hasText(date)) {
            return null;
        }
        try {
            return LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (DateTimeParseException e) {
            throw new BusinessException(400, "日期格式应为 yyyy-MM-dd");
        }
    }
}
