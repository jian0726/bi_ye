package com.jianyou.blog.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 生产环境启动安全校验（仅在 prod profile 下生效）。
 *
 * <p>动机：{@code application.yml} 里的 {@code ${VAR:默认值}} 是为了让本机裸跑无需配置，
 * 但生产环境一旦漏配环境变量，就会静默使用仓库里公开的默认值 —— 等于没加密。
 * 因此 prod profile 启动时必须把配置和默认值逐项比对，命中任一弱配置就直接拒绝服务。</p>
 *
 * <p>为什么在 {@link ApplicationReadyEvent} 而不是 {@code @PostConstruct}：
 * 前者在容器完全就绪后触发，抛异常会中止启动并输出完整原因，且不影响 Bean 注入顺序。</p>
 */
@Component
@Profile("prod")
public class SecurityGuard {

    private static final Logger log = LoggerFactory.getLogger(SecurityGuard.class);

    /** 与 application.yml 中的默认值保持一致；生产出现这些值即为未覆盖 */
    private static final String DEFAULT_JWT_SECRET =
            "***REMOVED***";
    private static final String DEFAULT_DB_PASSWORD = "***REMOVED***";
    private static final String DEFAULT_MINIO_SECRET = "***REMOVED***";

    /** HMAC-SHA256 要求密钥至少 256 bit */
    private static final int MIN_JWT_SECRET_LENGTH = 32;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    @Value("${minio.secret-key}")
    private String minioSecret;

    @Value("${verify.dev-mode:false}")
    private boolean verifyDevMode;

    @EventListener(ApplicationReadyEvent.class)
    public void check() {
        List<String> problems = new ArrayList<>();

        if (DEFAULT_JWT_SECRET.equals(jwtSecret)) {
            problems.add("JWT_SECRET 仍是仓库中的默认值，请设置环境变量 JWT_SECRET");
        } else if (jwtSecret == null || jwtSecret.length() < MIN_JWT_SECRET_LENGTH) {
            problems.add("JWT_SECRET 长度不足 " + MIN_JWT_SECRET_LENGTH + " 字符（HMAC-SHA256 要求至少 256 bit）");
        }

        if (DEFAULT_DB_PASSWORD.equals(dbPassword)) {
            problems.add("MYSQL_PASSWORD 仍是仓库中的默认值，请设置环境变量 MYSQL_PASSWORD");
        }

        if (DEFAULT_MINIO_SECRET.equals(minioSecret)) {
            problems.add("MINIO_SECRET_KEY 仍是仓库中的默认值，请设置环境变量 MINIO_SECRET_KEY");
        }

        if (verifyDevMode) {
            problems.add("VERIFY_DEV_MODE 仍为 true，生产环境验证码会随接口返回，请设为 false");
        }

        if (!problems.isEmpty()) {
            log.error("========================================================");
            log.error("生产环境安全校验未通过，拒绝启动。共 {} 项问题：", problems.size());
            for (String p : problems) {
                log.error("  [{}] {}", problems.indexOf(p) + 1, p);
            }
            log.error("参考 .env.example 配置后重新启动");
            log.error("========================================================");
            throw new IllegalStateException("生产环境存在 " + problems.size() + " 项弱配置，已中止启动");
        }

        log.info("生产环境安全校验通过");
    }
}
