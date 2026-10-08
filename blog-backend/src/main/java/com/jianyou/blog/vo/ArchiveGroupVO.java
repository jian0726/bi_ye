package com.jianyou.blog.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 归档分组（按年份）
 */
@Data
@AllArgsConstructor
public class ArchiveGroupVO {

    private Integer year;

    private Long count;

    private List<ArchiveItemVO> articles;
}
