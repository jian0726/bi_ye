package com.jianyou.blog.controller;

import com.jianyou.blog.common.Result;
import com.jianyou.blog.dto.LinkApplyDTO;
import com.jianyou.blog.service.FriendLinkService;
import com.jianyou.blog.vo.FriendLinkVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 友链接口
 */
@RestController
@RequestMapping("/portal/links")
@RequiredArgsConstructor
public class FriendLinkController {

    private final FriendLinkService friendLinkService;

    /** 已上架友链列表 */
    @GetMapping
    public Result<List<FriendLinkVO>> list() {
        return Result.ok(friendLinkService.listApproved());
    }

    /** 申请友链（落库待审） */
    @PostMapping("/apply")
    public Result<Void> apply(@Valid @RequestBody LinkApplyDTO dto) {
        friendLinkService.apply(dto);
        return Result.ok();
    }
}
