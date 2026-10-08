package com.jianyou.blog.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.jianyou.blog.entity.User;
import com.jianyou.blog.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 首次启动初始化博主账号：
 * 若库中尚无手机号/邮箱对应的账号，则为 id=1（种子数据里的博主）补齐联系方式与 BCrypt 密码。
 * 密码由代码生成，明文不落库、不进脚本。
 * 登录只认手机号/邮箱（全站唯一），没有独立的用户名字段。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataLoader implements ApplicationRunner {

    static final String OWNER_INIT_PASSWORD = "jianyou2026";

    /** 站长登录用的手机号与邮箱（可在后台用户管理里改） */
    public static final String OWNER_PHONE = "13800000001";
    public static final String OWNER_EMAIL = "jianyou@local.dev";

    private final UserMapper userMapper;
    private final PasswordEncoder encoder;

    @Override
    public void run(ApplicationArguments args) {
        // 站长账号以手机号为锚点：已存在且已有密码则只补缺，跳过
        User existing = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getPhone, OWNER_PHONE)
                .or()
                .eq(User::getEmail, OWNER_EMAIL)
                .last("LIMIT 1"));
        if (existing != null && StringUtils.hasText(existing.getPassword())) {
            if (!StringUtils.hasText(existing.getPhone()) || !StringUtils.hasText(existing.getEmail())) {
                existing.setPhone(OWNER_PHONE);
                existing.setEmail(OWNER_EMAIL);
                userMapper.update(existing,
                        new LambdaUpdateWrapper<User>().eq(User::getId, existing.getId()));
                log.info("已为博主账号补齐登录手机号 {} / 邮箱 {}", OWNER_PHONE, OWNER_EMAIL);
            } else {
                log.info("博主账号已初始化（{} / {}），跳过", OWNER_PHONE, OWNER_EMAIL);
            }
            return;
        }

        // 以已存在账号为准；否则落到种子博主（id=1）
        User owner = existing != null ? existing : userMapper.selectById(1L);
        if (owner == null) {
            owner = new User();
            owner.setNickname("简之航");
            owner.setBio("软件技术专业在读。不太会说话，所以写下来。");
            owner.setGender(0);
            owner.setRole("ADMIN");
            owner.setStatus(1);
            owner.setCreateTime(LocalDateTime.now());
        }
        owner.setPhone(OWNER_PHONE);
        owner.setEmail(OWNER_EMAIL);
        owner.setPassword(encoder.encode(OWNER_INIT_PASSWORD));
        if (owner.getId() == null) {
            userMapper.insert(owner);
        } else {
            userMapper.update(owner, new LambdaUpdateWrapper<User>().eq(User::getId, owner.getId()));
        }
        log.info("已初始化博主账号（登录手机号 {} / 邮箱 {}，密码为部署时约定值，BCrypt 存储）",
                OWNER_PHONE, OWNER_EMAIL);
    }
}
