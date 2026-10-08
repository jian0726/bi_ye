package com.jianyou.blog.controller;

import com.jianyou.blog.common.Result;
import com.jianyou.blog.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 文件上传（/admin/uploads，管理端专用）
 * 支持：图片 jpg/png/gif/webp（≤5MB）、音频 mp3/flac（≤1028MB）
 * 上传与校验逻辑统一收敛在 StorageService（头像上传也复用它）
 */
@RestController
@RequestMapping("/admin/uploads")
@RequiredArgsConstructor
public class UploadController {

    private final StorageService storageService;

    @PostMapping
    public Result<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        return Result.ok(Map.of("url", storageService.store(file)));
    }
}
