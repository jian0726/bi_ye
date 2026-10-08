package com.jianyou.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 背景音乐（全站 BGM 歌单）
 */
@Data
@TableName("t_music")
public class Music {

    public static final int STATUS_ON = 1;
    public static final int STATUS_OFF = 2;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String artist;

    /** 音频直链或 MinIO 地址 */
    private String url;

    private Integer sortOrder;

    /** 1启用 2停用 */
    private Integer status;

    private LocalDateTime createTime;
}
