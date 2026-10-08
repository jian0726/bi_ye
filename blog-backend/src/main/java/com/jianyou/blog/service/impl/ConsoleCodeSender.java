package com.jianyou.blog.service.impl;

import com.jianyou.blog.service.VerifyCodeSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 验证码发送占位实现（phone / email 共用）
 *
 * 不接任何真实通道，只把验证码打到应用日志：
 *   [验证码-占位] channel=email target=ji***@qq.com code=482913
 *
 * dev-mode 打开时，接口还会把 code 一并回传给前端页面展示，方便本机联调。
 * 接入真实短信 / 邮件后：
 * 1. 新增 PhoneCodeSender / MailCodeSender 实现本接口；
 * 2. 把 VerifyService 里按 channel 取 sender 的逻辑指向真实实现；
 * 3. 将 verify.dev-mode 置为 false。
 */
@Slf4j
@Component
public class ConsoleCodeSender implements VerifyCodeSender {

    /** 两个通道都由此占位实现兜底，真实实现就绪后按 channel 覆盖 */
    @Override
    public String channel() {
        return "*";
    }

    @Override
    public void send(String target, String code) {
        log.info("[验证码-占位] 未接入真实通道，仅打印。target={} code={}", mask(target), code);
    }

    /** 日志里只留头尾，避免完整手机号 / 邮箱落到日志文件 */
    private String mask(String target) {
        if (target == null || target.length() <= 4) {
            return "***";
        }
        int at = target.indexOf('@');
        if (at > 0) {
            return target.substring(0, Math.min(2, at)) + "***" + target.substring(at);
        }
        return target.substring(0, 3) + "****" + target.substring(target.length() - 2);
    }
}
