package com.jianyou.blog.dto;

import lombok.Data;

/**
 * 相册照片保存（后台新增 / 编辑共用）
 */
@Data
public class AdminPhotoSaveDTO {

    private String title;

    /** 图片地址（MinIO）；空 = 渐变占位 */
    private String url;

    private String location;

    /** 拍摄日期 yyyy-MM-dd */
    private String takenDate;

    /** 宽高比（如 3/4） */
    private String ratio;

    /** 占位渐变色（可选） */
    private String tone;

    /** 占位 emoji（可选） */
    private String emoji;

    private Integer sortOrder;
}
