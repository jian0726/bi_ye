package com.jianyou.blog.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * refresh token 登记服务：jti 存 Redis，删除即撤销。
 *
 * <p>Redis key 约定（与 blog:verify:* 前缀风格一致）：
 * - blog:auth:refresh:{jti}  value=userId，TTL 与 refresh token 有效期一致</p>
 *
 * <p>撤销语义：
 * - 登出：删除 jti，对应 refresh 立即失效（幂等）
 * - 轮换：每次刷新成功删旧 jti、写新 jti，泄漏的旧 token 最多再换发一次
 * 存量 access token（默认 2h）不建黑名单，靠短有效期自然过期。</p>
 */
@Service
public class TokenService {

    private static final String KEY_REFRESH = "blog:auth:refresh:%s";

    private final StringRedisTemplate redisTemplate;
    private final long refreshDays;

    public TokenService(StringRedisTemplate redisTemplate,
                        @Value("${jwt.refresh-days:7}") long refreshDays) {
        this.redisTemplate = redisTemplate;
        this.refreshDays = refreshDays;
    }

    /** 登记新签发的 refresh token */
    public void register(String jti, Long userId) {
        redisTemplate.opsForValue().set(key(jti), String.valueOf(userId),
                Duration.ofDays(refreshDays));
    }

    /** jti 是否仍在册（已撤销/已轮换/过期清理后返回 false） */
    public boolean isRegistered(String jti) {
        return jti != null && Boolean.TRUE.equals(redisTemplate.hasKey(key(jti)));
    }

    /** 撤销（幂等） */
    public void revoke(String jti) {
        if (jti != null) {
            redisTemplate.delete(key(jti));
        }
    }

    private String key(String jti) {
        return String.format(KEY_REFRESH, jti);
    }
}
