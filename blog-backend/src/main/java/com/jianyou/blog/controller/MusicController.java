package com.jianyou.blog.controller;

import com.jianyou.blog.common.Result;
import com.jianyou.blog.dto.AdminMusicSaveDTO;
import com.jianyou.blog.entity.Music;
import com.jianyou.blog.service.MusicService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 背景音乐门户接口：歌单公开；上传 / 直链加入歌单需登录
 */
@RestController
@RequestMapping("/portal/music")
@RequiredArgsConstructor
public class MusicController {

    private final MusicService musicService;

    @GetMapping
    public Result<List<Music>> playlist() {
        return Result.ok(musicService.portalEnabled());
    }

    /** 用户上传音频并直接加入全站歌单（需登录；曲名缺省用文件名） */
    @PostMapping("/uploads")
    public Result<Music> upload(@RequestParam("file") MultipartFile file,
                                @RequestParam(required = false) String title) {
        return Result.ok(musicService.uploadAndAdd(file, title));
    }

    /** 用户用音乐直链加入全站歌单（需登录） */
    @PostMapping
    public Result<Long> add(@RequestBody AdminMusicSaveDTO dto) {
        return Result.ok(musicService.addByUser(dto));
    }
}
