package com.jianyou.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 相册照片
 */
@Data
@TableName("t_photo")
public class Photo {

    public static final int STATUS_SHOW = 1;
    public static final int STATUS_HIDDEN = 2;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    /** 图片地址（MinIO）；空 = 前台用 tone 渐变 + emoji 占位 */
    private String url;

    private String location;

    private LocalDate takenDate;

    /** 宽高比（如 3/4），瀑布流占位用 */
    private String ratio;

    /** 占位渐变色（url 为空时显示） */
    private String tone;

    /** 占位 emoji */
    private String emoji;

    private Integer sortOrder;

    /** 1显示 2隐藏 */
    private Integer status;

    private LocalDateTime createTime;
}
