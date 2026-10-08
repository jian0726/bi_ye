package com.jianyou.blog.controller;

import com.jianyou.blog.common.PageResult;
import com.jianyou.blog.common.Result;
import com.jianyou.blog.dto.MessagePostDTO;
import com.jianyou.blog.service.MessageService;
import com.jianyou.blog.util.IpUtils;
import com.jianyou.blog.vo.MessageVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 留言板接口
 */
@RestController
@RequestMapping("/portal/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @GetMapping
    public Result<PageResult<MessageVO>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        return Result.ok(messageService.page(page, size));
    }

    @PostMapping
    public Result<Void> post(@Valid @RequestBody MessagePostDTO dto, HttpServletRequest request) {
        messageService.post(dto, IpUtils.getClientIp(request));
        return Result.ok();
    }
}
