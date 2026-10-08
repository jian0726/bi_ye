package com.jianyou.blog.controller;

import com.jianyou.blog.common.Result;
import com.jianyou.blog.service.CategoryService;
import com.jianyou.blog.service.TagService;
import com.jianyou.blog.vo.CategoryVO;
import com.jianyou.blog.vo.TagVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 分类与标签接口
 */
@RestController
@RequestMapping("/portal")
@RequiredArgsConstructor
public class TaxonomyController {

    private final CategoryService categoryService;
    private final TagService tagService;

    @GetMapping("/categories")
    public Result<List<CategoryVO>> categories() {
        return Result.ok(categoryService.listAll());
    }

    @GetMapping("/tags")
    public Result<List<TagVO>> tags() {
        return Result.ok(tagService.listAll());
    }
}
