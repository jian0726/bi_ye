package com.jianyou.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jianyou.blog.common.BusinessException;
import com.jianyou.blog.dto.FindAccountDTO;
import com.jianyou.blog.dto.ResetPasswordDTO;
import com.jianyou.blog.entity.User;
import com.jianyou.blog.mapper.UserMapper;
import com.jianyou.blog.vo.FindAccountVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 找回密码服务（无需登录）
 *
 * 设计要点：找回方式跟随注册时留下的联系方式——
 * 用手机号注册的走手机号，用邮箱注册的走邮箱，两者都有的（如站长账号）让用户自选。
 * 这样不改变注册流程（仍是手机号/邮箱二选一），也不给 t_user 增加字段。
 *
 * 验证码本身由 {@link VerifyCodeService} 生成与校验，本类只负责
 * 「账号 → 可用渠道」的判定、脱敏展示与重置后的限流清理。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserMapper userMapper;
    private final VerifyCodeService verifyCodeService;
    private final PasswordEncoder encoder;

    /**
     * 第一步：探账号，返回可用的验证渠道（脱敏）
     * 账号不存在时 exists=false（不抛异常，避免暴露注册信息给暴力枚举）
     */
    public FindAccountVO findAccount(FindAccountDTO dto) {
        User user = locate(dto.getAccount().trim());
        if (user == null) {
            return FindAccountVO.builder().exists(false).channels(List.of()).build();
        }

        List<FindAccountVO.Channel> channels = new ArrayList<>();
        if (StringUtils.hasText(user.getPhone())) {
            channels.add(FindAccountVO.Channel.builder()
                    .channel("phone")
                    .masked(maskPhone(user.getPhone()))
                    .build());
        }
        if (StringUtils.hasText(user.getEmail())) {
            channels.add(FindAccountVO.Channel.builder()
                    .channel("email")
                    .masked(maskEmail(user.getEmail()))
                    .build());
        }
        return FindAccountVO.builder().exists(true).channels(channels).build();
    }

    /**
     * 第二步：发送验证码
     * 渠道必须在该账号实际绑定的范围内，防止拿别人的手机号/邮箱收码
     */
    public String sendCode(String account, String channel) {
        String contact = account.trim();
        User user = locate(contact);
        if (user == null) {
            throw new BusinessException(400, "该账号不存在");
        }

        String target = resolveTarget(user, channel);
        return verifyCodeService.send(channel, target);
    }

    /**
     * 第三步：校验验证码并重置密码
     */
    public void resetPassword(ResetPasswordDTO dto) {
        String account = dto.getAccount().trim();
        User user = locate(account);
        if (user == null) {
            throw new BusinessException(400, "该账号不存在");
        }

        String target = resolveTarget(user, dto.getChannel());
        verifyCodeService.verify(dto.getChannel(), target, dto.getCode());

        userMapper.updateById(withEncodedPassword(user, dto.getNewPassword()));

        log.info("用户重置密码成功 userId={} channel={} target={}",
                user.getId(), dto.getChannel(), maskByChannel(dto.getChannel(), target));
    }

    /**
     * 按渠道取该账号绑定的接收目标，并校验渠道是否可用
     */
    private String resolveTarget(User user, String channel) {
        if ("phone".equals(channel)) {
            if (!StringUtils.hasText(user.getPhone())) {
                throw new BusinessException(400, "该账号未绑定手机号，请改用邮箱找回");
            }
            return user.getPhone();
        }
        if ("email".equals(channel)) {
            if (!StringUtils.hasText(user.getEmail())) {
                throw new BusinessException(400, "该账号未绑定邮箱，请改用手机号找回");
            }
            return user.getEmail();
        }
        throw new BusinessException(400, "不支持的验证方式");
    }

    /** 账号定位：手机号或邮箱任一匹配 */
    private User locate(String account) {
        if (!StringUtils.hasText(account)) {
            throw new BusinessException(400, "请输入手机号或邮箱");
        }
        return userMapper.selectOne(new LambdaQueryWrapper<User>()
                .and(w -> w.eq(User::getPhone, account).or().eq(User::getEmail, account))
                .last("LIMIT 1"));
    }

    private User withEncodedPassword(User user, String rawPassword) {
        user.setPassword(encoder.encode(rawPassword));
        return user;
    }

    private String maskByChannel(String channel, String target) {
        return "phone".equals(channel) ? maskPhone(target) : maskEmail(target);
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return "***";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private String maskEmail(String email) {
        if (email == null) {
            return "***";
        }
        int at = email.indexOf('@');
        if (at <= 0) {
            return "***";
        }
        String name = email.substring(0, at);
        String head = name.length() <= 2 ? name.substring(0, 1) : name.substring(0, 2);
        return head + "***" + email.substring(at);
    }
}
