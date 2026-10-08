package com.jianyou.blog.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 验证码配置（application.yml 中 verify.* 前缀）
 *
 * dev-mode 是**上线前必须关掉的开关**：
 * 打开时验证码不经任何真实通道下发，而是随接口响应回传给前端页面展示，
 * 仅用于本机联调。接入真实短信 / 邮件服务后，把它置为 false，
 * 届时验证码只走短信或邮件，接口不再回传。
 */
@Data
@Component
@ConfigurationProperties(prefix = "verify")
public class VerifyProperties {

    /**
     * 开发模式开关（true = 验证码回传前端便于联调）
     * TODO 接入真实短信 / 邮件通道后改为 false
     */
    private boolean devMode = false;

    /** 验证码位数 */
    private int codeLength = 6;

    /** 验证码有效期（分钟） */
    private int expireMinutes = 5;

    /** 同一账号发送冷却（秒） */
    private int resendSeconds = 60;

    /** 连续校验失败多少次后作废验证码 */
    private int maxFailCount = 5;
}
