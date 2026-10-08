package com.jianyou.blog.service;

import com.jianyou.blog.common.BusinessException;
import com.jianyou.blog.config.VerifyProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.List;

/**
 * 验证码服务：生成 / 下发 / 校验
 *
 * 验证码本身存 Redis，不落库；t_user 表无需为找回密码增加任何字段。
 *
 * Redis key 约定（冒号分隔，与项目既有 blog:* 前缀风格一致）：
 * - blog:verify:code:{channel}:{account}   验证码明文，TTL = expireMinutes
 * - blog:verify:limit:{channel}:{account}  发送冷却标记，TTL = resendSeconds
 * - blog:verify:fail:{channel}:{account}   连续失败计数，TTL = expireMinutes
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VerifyCodeService {

    private static final String KEY_CODE = "blog:verify:code:%s:%s";
    private static final String KEY_LIMIT = "blog:verify:limit:%s:%s";
    private static final String KEY_FAIL = "blog:verify:fail:%s:%s";

    private static final SecureRandom RANDOM = new SecureRandom();

    private final StringRedisTemplate redisTemplate;
    private final VerifyProperties props;
    private final List<VerifyCodeSender> senders;

    /**
     * 发送验证码
     *
     * @param channel phone / email
     * @param account 手机号或邮箱原文
     * @return dev-mode 下的验证码明文（供前端展示）；非 dev-mode 返回 null
     */
    public String send(String channel, String account) {
        String limitKey = key(KEY_LIMIT, channel, account);
        if (Boolean.TRUE.equals(redisTemplate.hasKey(limitKey))) {
            Long ttl = redisTemplate.getExpire(limitKey);
            long wait = ttl != null && ttl > 0 ? ttl : props.getResendSeconds();
            throw new BusinessException(400, "发送太频繁了，请 " + wait + " 秒后再试");
        }

        String code = randomCode();
        String codeKey = key(KEY_CODE, channel, account);
        redisTemplate.opsForValue().set(codeKey, code, Duration.ofMinutes(props.getExpireMinutes()));
        // 冷却标记与验证码分开存：验证码到期前允许用户重新获取，但两次发送间隔不小于 60 秒
        redisTemplate.opsForValue().set(limitKey, "1", Duration.ofSeconds(props.getResendSeconds()));
        // 重置失败计数，避免上一次的失败次数影响新验证码
        redisTemplate.delete(key(KEY_FAIL, channel, account));

        dispatch(channel, account, code);
        log.info("验证码已生成 channel={} account={} 有效期={}分钟",
                channel, mask(account), props.getExpireMinutes());

        return props.isDevMode() ? code : null;
    }

    /**
     * 校验验证码；校验通过后立即销毁，防止同一码重复使用
     */
    public void verify(String channel, String account, String input) {
        if (input == null || input.isBlank()) {
            throw new BusinessException(400, "请输入验证码");
        }

        String codeKey = key(KEY_CODE, channel, account);
        String cached = redisTemplate.opsForValue().get(codeKey);
        if (cached == null) {
            throw new BusinessException(400, "验证码已过期，请重新获取");
        }

        if (!cached.equals(input.trim())) {
            String failKey = key(KEY_FAIL, channel, account);
            Long fails = redisTemplate.opsForValue().increment(failKey);
            if (fails != null && fails == 1L) {
                // 首次失败时给计数设置与验证码一致的生命周期
                redisTemplate.expire(failKey, Duration.ofMinutes(props.getExpireMinutes()));
            }
            if (fails != null && fails >= props.getMaxFailCount()) {
                redisTemplate.delete(codeKey);
                redisTemplate.delete(failKey);
                throw new BusinessException(400, "错误次数过多，验证码已作废，请重新获取");
            }
            long left = props.getMaxFailCount() - (fails == null ? 0 : fails);
            throw new BusinessException(400, "验证码不正确，还可尝试 " + left + " 次");
        }

        // 一次性使用：校验通过即销毁
        redisTemplate.delete(codeKey);
        redisTemplate.delete(key(KEY_FAIL, channel, account));
    }

    /** 按 channel 找发送实现；暂无专用实现时回落到占位实现 */
    private void dispatch(String channel, String account, String code) {
        VerifyCodeSender sender = senders.stream()
                .filter(s -> channel.equals(s.channel()))
                .findFirst()
                .orElseGet(() -> senders.stream()
                        .filter(s -> "*".equals(s.channel()))
                        .findFirst()
                        .orElseThrow(() -> new BusinessException(500, "验证码通道未配置")));
        sender.send(account, code);
    }

    private String randomCode() {
        StringBuilder sb = new StringBuilder(props.getCodeLength());
        for (int i = 0; i < props.getCodeLength(); i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }

    private String key(String template, String channel, String account) {
        return String.format(template, channel, account);
    }

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
