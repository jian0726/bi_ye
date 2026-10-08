package com.jianyou.blog.common;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 统一分页响应体（与前端 types.ts 的 PageResult 接口对齐）
 */
@Data
@AllArgsConstructor
public class PageResult<T> {

    private List<T> records;
    private long total;
    private long page;
    private long size;
    private long pages;
}
