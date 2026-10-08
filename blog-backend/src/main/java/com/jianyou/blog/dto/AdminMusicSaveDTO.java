package com.jianyou.blog.dto;

import lombok.Data;

/**
 * 背景音乐保存（后台新增 / 编辑共用）
 */
@Data
public class AdminMusicSaveDTO {

    private String title;

    private String artist;

    /** 音频直链或 MinIO 地址 */
    private String url;

    private Integer sortOrder;
}
