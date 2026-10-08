package com.jianyou.blog.config;

import com.jianyou.blog.util.IpUtils;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 客户端 IP 信任配置：把 {@code app.trusted-proxies} 注入 {@link IpUtils}。
 *
 * <p>仅当请求的直接来源 IP 在此列表中时才采信 {@code X-Forwarded-For}，
 * 其余来源的 XFF 一律忽略（防伪造）。本机开发默认含回环地址以便联调模拟多地区访客；
 * 生产部署时应改为反向代理的内网地址（如 nginx 所在主机的内网 IP）。</p>
 */
@Configuration
public class ClientIpConfig {

    @Value("${app.trusted-proxies:}")
    private String trustedProxies;

    @PostConstruct
    public void init() {
        Set<String> proxies = Arrays.stream(trustedProxies.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(ip -> "0:0:0:0:0:0:0:1".equals(ip) ? "::1" : ip)
                .collect(Collectors.toSet());
        IpUtils.configure(proxies);
    }
}
