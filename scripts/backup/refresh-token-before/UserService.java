package com.jianyou.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.jianyou.blog.common.AuthContext;
import com.jianyou.blog.common.BusinessException;
import com.jianyou.blog.dto.LoginDTO;
import com.jianyou.blog.dto.PasswordChangeDTO;
import com.jianyou.blog.dto.ProfileUpdateDTO;
import com.jianyou.blog.dto.RegisterDTO;
import com.jianyou.blog.entity.User;
import com.jianyou.blog.mapper.UserMapper;
import com.jianyou.blog.util.JwtUtil;
import com.jianyou.blog.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

/**
 * 用户服务：注册 / 登录 / 当前用户
 * 登录标识只有手机号与邮箱（全站唯一），没有独立的用户名字段
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder encoder;
    private final StorageService storageService;

    /** 注册：手机号/邮箱至少一项 + 唯一校验 + BCrypt 密文入库，角色固定 USER */
    public LoginVO register(RegisterDTO dto) {
        // 空串归一为 null；DTO 已保证两者至少有一个
        String phone = StringUtils.hasText(dto.getPhone()) ? dto.getPhone().trim() : null;
        String email = StringUtils.hasText(dto.getEmail()) ? dto.getEmail().trim() : null;
        if (phone == null && email == null) {
            throw new BusinessException(400, "请填写手机号或邮箱");
        }
        if (phone != null && userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getPhone, phone)) > 0) {
            throw new BusinessException(400, "该手机号已被注册");
        }
        if (email != null && userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, email)) > 0) {
            throw new BusinessException(400, "该邮箱已被注册");
        }

        User user = new User();
        user.setPhone(phone);
        user.setEmail(email);
        user.setNickname(dto.getNickname());
        user.setPassword(encoder.encode(dto.getPassword()));
        user.setGender(0);
        user.setRole("USER");
        user.setStatus(1);
        user.setCreateTime(LocalDateTime.now());
        userMapper.insert(user);

        return buildVO(user);
    }

    /** 登录：手机号 / 邮箱 任一匹配 + BCrypt 比对 + 签发 JWT */
    public LoginVO login(LoginDTO dto) {
        String contact = dto.getAccount().trim();
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .and(w -> w.eq(User::getPhone, contact).or().eq(User::getEmail, contact))
                .last("LIMIT 1"));
        if (user == null || !StringUtils.hasText(user.getPassword())
                || !encoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(400, "手机号/邮箱或密码错误");
        }
        if (user.getStatus() == 0) {
            throw new BusinessException(403, "账号已被禁用");
        }
        return buildVO(user);
    }

    /** 当前登录用户信息（供 /auth/me） */
    public LoginVO me() {
        Long userId = AuthContext.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(401, "账号不存在或已注销");
        }
        return buildVO(user);
    }

    /** 修改个人资料（昵称 / 简介），返回更新后的用户信息 */
    public LoginVO updateProfile(ProfileUpdateDTO dto) {
        Long userId = AuthContext.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(401, "账号不存在或已注销");
        }
        user.setNickname(dto.getNickname().trim());
        user.setBio(dto.getBio() != null ? dto.getBio().trim() : null);
        userMapper.updateById(user);
        return buildVO(user);
    }

    /** 修改密码：校验旧密码 + BCrypt 加密新密码 */
    public void changePassword(PasswordChangeDTO dto) {
        Long userId = AuthContext.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(401, "账号不存在或已注销");
        }
        if (!StringUtils.hasText(user.getPassword())
                || !encoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new BusinessException(400, "当前密码不正确");
        }
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getPassword, encoder.encode(dto.getNewPassword())));
    }

    /** 上传/更换头像：存 MinIO 并更新 user.avatar，返回更新后的用户信息 */
    public LoginVO updateAvatar(MultipartFile file) {
        Long userId = AuthContext.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }
        String url = storageService.storeAvatar(file);
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(401, "账号不存在或已注销");
        }
        user.setAvatar(url);
        userMapper.updateById(user);
        return buildVO(user);
    }

    /** 上传/更换留言页自定义背景：存 MinIO（登记 t_file）并更新 user.messageBg */
    public LoginVO updateMessageBg(MultipartFile file) {
        Long userId = AuthContext.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }
        String url = storageService.storeImage(file);
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(401, "账号不存在或已注销");
        }
        user.setMessageBg(url);
        userMapper.updateById(user);
        return buildVO(user);
    }

    /** 清除留言页自定义背景，恢复默认夜空 */
    public LoginVO clearMessageBg() {
        Long userId = AuthContext.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(401, "账号不存在或已注销");
        }
        // updateById 默认忽略 null 字段，须用 UpdateWrapper 显式置空
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getMessageBg, null));
        user.setMessageBg(null);
        return buildVO(user);
    }

    private LoginVO buildVO(User user) {
        return LoginVO.builder()
                .token(jwtUtil.issue(user.getId(), user.getRole()))
                .userId(user.getId())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .messageBg(user.getMessageBg())
                .role(user.getRole())
                .build();
    }
}
