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

    /**
     * 未配置时的占位值。
     * application.yml 里所有敏感项写作 {@code ${VAR:__UNSET__}}，
     * 仓库中因此不含任何真实凭据；本地开发需自行设置环境变量或使用 IDE 的运行配置。
     */
    private static final String UNSET = "__UNSET__";

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

        if (UNSET.equals(jwtSecret) || isBlank(jwtSecret)) {
            problems.add("JWT_SECRET 未设置，请通过环境变量注入");
        } else if (jwtSecret.length() < MIN_JWT_SECRET_LENGTH) {
            problems.add("JWT_SECRET 长度不足 " + MIN_JWT_SECRET_LENGTH + " 字符（HMAC-SHA256 要求至少 256 bit）");
        }

        if (UNSET.equals(dbPassword) || isBlank(dbPassword)) {
            problems.add("MYSQL_PASSWORD 未设置，请通过环境变量注入");
        }

        if (UNSET.equals(minioSecret) || isBlank(minioSecret)) {
            problems.add("MINIO_SECRET_KEY 未设置，请通过环境变量注入");
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

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
