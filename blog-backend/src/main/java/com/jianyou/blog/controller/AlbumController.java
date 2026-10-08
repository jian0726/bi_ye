package com.jianyou.blog.controller;

import com.jianyou.blog.common.Result;
import com.jianyou.blog.entity.Photo;
import com.jianyou.blog.service.AlbumService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 相册门户接口
 */
@RestController
@RequestMapping("/portal/album")
@RequiredArgsConstructor
public class AlbumController {

    private final AlbumService albumService;

    @GetMapping
    public Result<List<Photo>> photos() {
        return Result.ok(albumService.portalList());
    }
}
