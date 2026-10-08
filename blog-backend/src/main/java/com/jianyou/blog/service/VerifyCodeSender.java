package com.jianyou.blog.service;

/**
 * 验证码发送通道（短信 / 邮件）
 *
 * 当前只有占位实现 {@link ConsoleCodeSender}：把验证码写进日志，
 * 并在 dev-mode 下随接口响应回传前端，不产生任何真实外发。
 *
 * 后续接入真实通道时新增实现类即可，调用方无需改动：
 * - 短信：阿里云 / 腾讯云短信 SDK
 * - 邮件：spring-boot-starter-mail + SMTP 授权码
 */
public interface VerifyCodeSender {

    /** 通道标识：phone / email */
    String channel();

    /**
     * 发送验证码
     *
     * @param target 接收目标（手机号或邮箱原文，由实现自行决定是否需要脱敏记录）
     * @param code   验证码明文
     */
    void send(String target, String code);
}
