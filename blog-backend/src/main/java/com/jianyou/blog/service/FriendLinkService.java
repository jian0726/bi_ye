package com.jianyou.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jianyou.blog.dto.LinkApplyDTO;
import com.jianyou.blog.entity.FriendLink;
import com.jianyou.blog.mapper.FriendLinkMapper;
import com.jianyou.blog.vo.FriendLinkVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 友链服务
 */
@Service
@RequiredArgsConstructor
public class FriendLinkService {

    private final FriendLinkMapper friendLinkMapper;

    /** 已上架友链 */
    public List<FriendLinkVO> listApproved() {
        return friendLinkMapper.selectList(new LambdaQueryWrapper<FriendLink>()
                        .eq(FriendLink::getStatus, 1)
                        .orderByAsc(FriendLink::getSortOrder))
                .stream()
                .map(l -> FriendLinkVO.builder()
                        .id(l.getId()).name(l.getName()).url(l.getUrl())
                        .logo(l.getLogo()).description(l.getDescription())
                        .build())
                .toList();
    }

    /** 申请友链：落库为待审核，博主在后台审核后才展示 */
    public void apply(LinkApplyDTO dto) {
        FriendLink link = new FriendLink();
        link.setName(dto.getName());
        link.setUrl(dto.getUrl());
        link.setLogo(dto.getLogo());
        link.setDescription(dto.getDescription());
        link.setStatus(0);
        link.setSortOrder(99);
        link.setCreateTime(LocalDateTime.now());
        friendLinkMapper.insert(link);
    }
}
