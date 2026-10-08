package com.jianyou.blog.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 归档条目（年份分组内）
 */
@Data
@Builder
public class ArchiveItemVO {

    private Long id;
    private String title;
    private LocalDateTime createTime;
    private LocalDateTime publishTime;
}
