package com.jianyou.blog.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

/**
 * JWT 工具：签发 / 解析（HMAC-SHA256），双 token 体系
 *
 * <p>- access：短效（默认 2h），每次请求携带，载荷带角色；claim type=access</p>
 * <p>- refresh：长效（默认 7 天），仅用于换发 access，不带业务角色（角色在刷新时回库读取，
 * 降权/禁用立即生效）；claim type=refresh + jti（UUID，由调用方生成并登记 Redis，
 * key 形如 blog:auth:refresh:{jti}，删除即撤销）</p>
 *
 * <p>两类 token 不可混用：refresh 过不了 {@link #parseAccess}，access 也过不了 {@link #parseRefresh}。
 * 注意：引入 type 校验后，旧版（无 type claim）的存量 token 会全部失效，用户需重新登录。</p>
 */
@Component
public class JwtUtil {

    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    private final SecretKey key;
    private final long accessMillis;
    private final long refreshMillis;

    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.access-hours:2}") long accessHours,
                   @Value("${jwt.refresh-days:7}") long refreshDays) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessMillis = Duration.ofHours(accessHours).toMillis();
        this.refreshMillis = Duration.ofDays(refreshDays).toMillis();
    }

    /** 签发 access token：subject 为用户 id，载荷带角色 */
    public String issueAccess(Long userId, String role) {
        return build(userId, TYPE_ACCESS, null, role, accessMillis);
    }

    /**
     * 签发 refresh token：jti 由调用方生成并保管（登记 Redis 用），
     * token 内仅含 subject 与随机 jti，不携带角色
     */
    public String issueRefresh(Long userId, String jti) {
        return build(userId, TYPE_REFRESH, jti, null, refreshMillis);
    }

    private String build(Long userId, String type, String jti, String role, long ttlMillis) {
        Date now = new Date();
        JwtBuilder builder = Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("type", type)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ttlMillis))
                .signWith(key);
        if (jti != null) {
            builder.id(jti);
        }
        if (role != null) {
            builder.claim("role", role);
        }
        return builder.compact();
    }

    /** 解析 access token：合法且类型正确返回 Claims；过期/篡改/refresh 冒充返回 null */
    public Claims parseAccess(String token) {
        return parseType(token, TYPE_ACCESS);
    }

    /** 解析 refresh token：仅 refresh 类型有效，access 冒充返回 null */
    public Claims parseRefresh(String token) {
        return parseType(token, TYPE_REFRESH);
    }

    private Claims parseType(String token, String expectedType) {
        Claims claims = parse(token);
        if (claims == null || !expectedType.equals(claims.get("type", String.class))) {
            return null;
        }
        return claims;
    }

    /** 底层解析：过期或被篡改返回 null（调用方按未登录处理） */
    public Claims parse(String token) {
        try {
            return Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token).getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }
}
