package com.jianyou.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 上传资源记录（MinIO 对象）
 */
@Data
@TableName("t_file")
public class UploadFile {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 原文件名 */
    private String name;

    /** 对象键 */
    private String objectKey;

    /** 访问地址 */
    private String url;

    /** MIME 类型 */
    private String type;

    /** 文件大小（字节） */
    private Long size;

    private LocalDateTime createTime;
}
