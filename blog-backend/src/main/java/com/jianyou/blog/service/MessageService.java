package com.jianyou.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jianyou.blog.common.PageResult;
import com.jianyou.blog.dto.MessagePostDTO;
import com.jianyou.blog.entity.Message;
import com.jianyou.blog.entity.User;
import com.jianyou.blog.mapper.MessageMapper;
import com.jianyou.blog.mapper.UserMapper;
import com.jianyou.blog.util.ProvinceResolver;
import com.jianyou.blog.vo.MessageVO;
import com.jianyou.blog.vo.UserBriefVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 留言板服务
 */
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageMapper messageMapper;
    private final UserMapper userMapper;
    private final ProvinceResolver provinceResolver;

    /** 留言分页，按时间倒序 */
    public PageResult<MessageVO> page(long page, long size) {
        page = Math.max(page, 1);
        size = Math.min(Math.max(size, 1), 50);

        Page<Message> result = messageMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Message>().orderByDesc(Message::getCreateTime));

        // 关联登录用户
        Set<Long> userIds = result.getRecords().stream()
                .map(Message::getUserId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, User> userMap = userIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(userIds).stream()
                        .collect(Collectors.toMap(User::getId, Function.identity()));

        List<MessageVO> vos = result.getRecords().stream().map(m -> {
            MessageVO vo = new MessageVO();
            vo.setId(m.getId());
            User user = userMap.get(m.getUserId());
            if (user != null) {
                vo.setUser(UserBriefVO.builder()
                        .id(user.getId()).nickname(user.getNickname()).avatar(user.getAvatar())
                        .build());
            }
            vo.setNickname(m.getNickname());
            vo.setContent(m.getContent());
            vo.setIpLocation(m.getIpLocation());
            vo.setCreateTime(m.getCreateTime());
            return vo;
        }).toList();

        return new PageResult<>(vos, result.getTotal(), page, size, result.getPages());
    }

    /** 游客留言（登录用户体系接入后可扩展） */
    public void post(MessagePostDTO dto, String clientIp) {
        Message message = new Message();
        message.setUserId(null);
        message.setNickname(StringUtils.hasText(dto.getNickname()) ? dto.getNickname() : "访客");
        message.setEmail(dto.getEmail());
        message.setContent(dto.getContent());
        message.setIpLocation(provinceResolver.resolve(clientIp));
        message.setCreateTime(LocalDateTime.now());
        messageMapper.insert(message);
    }
}
