package com.jianyou.blog.controller;

import com.jianyou.blog.common.BusinessException;
import com.jianyou.blog.common.Result;
import com.jianyou.blog.dto.FindAccountDTO;
import com.jianyou.blog.dto.LoginDTO;
import com.jianyou.blog.dto.LogoutDTO;
import com.jianyou.blog.dto.PasswordChangeDTO;
import com.jianyou.blog.dto.ProfileUpdateDTO;
import com.jianyou.blog.dto.RefreshTokenDTO;
import com.jianyou.blog.dto.RegisterDTO;
import com.jianyou.blog.dto.ResetPasswordDTO;
import com.jianyou.blog.dto.SendCodeDTO;
import com.jianyou.blog.service.PasswordResetService;
import com.jianyou.blog.service.UserService;
import com.jianyou.blog.vo.FindAccountVO;
import com.jianyou.blog.vo.LoginVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证接口
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final PasswordResetService passwordResetService;

    /** 登录 */
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.ok(userService.login(dto));
    }

    /**
     * 刷新令牌：用长效 refresh token 换发新双 token（轮换制，旧的立即失效）。
     * 角色与禁用态在刷新时回库读取，降权/禁用立即生效
     */
    @PostMapping("/refresh")
    public Result<LoginVO> refresh(@Valid @RequestBody RefreshTokenDTO dto) {
        return Result.ok(userService.refresh(dto.getRefreshToken().trim()));
    }

    /** 登出：撤销 refresh token（幂等，重复调用无害） */
    @PostMapping("/logout")
    public Result<Void> logout(@RequestBody(required = false) LogoutDTO dto) {
        userService.logout(dto == null ? null : dto.getRefreshToken());
        return Result.ok();
    }

    /** 注册 */
    @PostMapping("/register")
    public Result<LoginVO> register(@Valid @RequestBody RegisterDTO dto) {
        return Result.ok(userService.register(dto));
    }

    /** 当前登录用户（需携带 token；拦截器可选解析） */
    @GetMapping("/me")
    public Result<LoginVO> me() {
        return Result.ok(userService.me());
    }

    /** 修改个人资料（昵称 / 简介），返回更新后的用户信息 */
    @PutMapping("/profile")
    public Result<LoginVO> updateProfile(@Valid @RequestBody ProfileUpdateDTO dto) {
        return Result.ok(userService.updateProfile(dto));
    }

    /** 修改密码 */
    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody PasswordChangeDTO dto) {
        userService.changePassword(dto);
        return Result.ok();
    }

    /** 上传/更换头像（图片 ≤2MB），返回更新后的用户信息 */
    @PostMapping("/avatar")
    public Result<LoginVO> avatar(@RequestParam("file") MultipartFile file) {
        return Result.ok(userService.updateAvatar(file));
    }

    /** 上传/更换留言页自定义背景（图片 ≤5MB，登记资源管理），返回更新后的用户信息 */
    @PostMapping("/message-bg")
    public Result<LoginVO> messageBg(@RequestParam("file") MultipartFile file) {
        return Result.ok(userService.updateMessageBg(file));
    }

    /** 清除留言页自定义背景，恢复默认夜空 */
    @DeleteMapping("/message-bg")
    public Result<LoginVO> clearMessageBg() {
        return Result.ok(userService.clearMessageBg());
    }

    // ---------- 找回密码（无需登录） ----------

    /**
     * 找回密码第一步：查该账号可用的验证方式
     * 返回脱敏后的手机号 / 邮箱，前端据此展示「发到手机」或「发到邮箱」
     */
    @PostMapping("/find-account")
    public Result<FindAccountVO> findAccount(@Valid @RequestBody FindAccountDTO dto) {
        return Result.ok(passwordResetService.findAccount(dto));
    }

    /**
     * 找回密码第二步：发送验证码
     * channel 必须是该账号实际绑定的方式（phone / email）
     * dev-mode 下返回值里带验证码明文，接入真实通道后为 null
     */
    @PostMapping("/send-code")
    public Result<Map<String, String>> sendCode(@RequestBody SendCodeDTO dto) {
        String account = dto.getAccount() == null ? "" : dto.getAccount().trim();
        if (account.isEmpty()) {
            throw new BusinessException(400, "请输入手机号或邮箱");
        }
        String code = passwordResetService.sendCode(account, dto.getChannel());
        Map<String, String> data = new HashMap<>(2);
        if (code != null) {
            // 仅开发模式回传，便于本机联调；正式环境为 null
            data.put("devCode", code);
        }
        return Result.ok(data);
    }

    /** 找回密码第三步：校验验证码并重置密码 */
    @PostMapping("/reset-password")
    public Result<Void> resetPassword(@Valid @RequestBody ResetPasswordDTO dto) {
        passwordResetService.resetPassword(dto);
        return Result.ok();
    }
}
