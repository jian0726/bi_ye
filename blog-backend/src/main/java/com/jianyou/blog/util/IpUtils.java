package com.jianyou.blog.util;

import jakarta.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 客户端 IP 提取（兼容 nginx 反代场景）
 *
 * <p>安全约束：只有当请求的<strong>直接来源</strong>（{@code request.getRemoteAddr()}）
 * 位于可信代理白名单（{@code app.trusted-proxies}，由 {@code ClientIpConfig} 启动时注入）时，
 * 才采信 {@code X-Forwarded-For} 头；否则一律使用 TCP 层真实来源地址，
 * 防止任意客户端伪造 XFF 污染归属地统计。</p>
 *
 * <p>白名单支持两种写法：单个精确 IP（{@code 127.0.0.1}）与 IPv4 CIDR 网段
 * （{@code 172.20.0.0/16}）。Docker 容器网络 IP 每次重建可能变化，
 * 用网段可避免容器重建后白名单失配导致访客归属地全部记成内网 IP。</p>
 */
public final class IpUtils {

    /** 可信反向代理精确 IP 集合（IPv6 回环已归一） */
    private static volatile Set<String> trustedExact = Set.of();

    /** 可信反向代理 CIDR 网段（IPv4，解析为 [网络地址(long), 掩码长度]） */
    private static volatile List<Cidr> trustedCidrs = List.of();

    private IpUtils() {
    }

    /**
     * 启动时由 ClientIpConfig 注入配置值。
     * 单项含 {@code /} 视为 CIDR，否则视为精确 IP。
     */
    public static void configure(Set<String> proxies) {
        Set<String> src = proxies == null ? Set.of() : Set.copyOf(proxies);
        Set<String> exact = new java.util.HashSet<>();
        List<Cidr> cidrs = new ArrayList<>();
        for (String item : src) {
            if (item == null || item.isBlank()) {
                continue;
            }
            String v = item.trim();
            if (v.indexOf('/') > 0) {
                Cidr c = Cidr.parse(v);
                if (c != null) {
                    cidrs.add(c);
                }
            } else {
                exact.add(normalize(v));
            }
        }
        trustedExact = Set.copyOf(exact);
        trustedCidrs = List.copyOf(cidrs);
    }

    public static String getClientIp(HttpServletRequest request) {
        String remote = request.getRemoteAddr();
        String xff = request.getHeader("X-Forwarded-For");
        if (xff == null || xff.isBlank()) {
            return remote;
        }
        // 直接来源不是可信代理 → XFF 是客户端可伪造的头，忽略
        if (!isTrusted(remote)) {
            return remote;
        }
        // 多级代理时第一个为客户端真实 IP
        int comma = xff.indexOf(',');
        String candidate = (comma > 0 ? xff.substring(0, comma) : xff).trim();
        // 非法/超长值兜底回落（varchar(45) 为 IPv6 上限）
        if (candidate.isEmpty() || candidate.length() > 45) {
            return remote;
        }
        return candidate;
    }

    /** 来源 IP 是否命中可信代理白名单（精确 IP 或 CIDR 网段） */
    private static boolean isTrusted(String remote) {
        String ip = normalize(remote);
        if (trustedExact.contains(ip)) {
            return true;
        }
        if (trustedCidrs.isEmpty()) {
            return false;
        }
        long value = ipv4ToLong(ip);
        if (value < 0) {
            return false;
        }
        for (Cidr c : trustedCidrs) {
            if (c.contains(value)) {
                return true;
            }
        }
        return false;
    }

    /** IPv6 回环两种写法归一，避免配置写 ::1 而请求来源是 0:0:0:0:0:0:0:1 时白名单失配 */
    private static String normalize(String ip) {
        if (ip == null) {
            return "";
        }
        return "0:0:0:0:0:0:0:1".equals(ip) ? "::1" : ip;
    }

    /** IPv4 点分十进制转 long；非法或非 IPv4 返回 -1 */
    private static long ipv4ToLong(String ip) {
        if (ip == null || ip.isEmpty()) {
            return -1;
        }
        String[] parts = ip.split("\\.");
        if (parts.length != 4) {
            return -1;
        }
        long result = 0;
        for (String p : parts) {
            if (p.isEmpty() || p.length() > 3) {
                return -1;
            }
            int seg;
            try {
                seg = Integer.parseInt(p);
            } catch (NumberFormatException e) {
                return -1;
            }
            if (seg < 0 || seg > 255) {
                return -1;
            }
            result = (result << 8) | seg;
        }
        return result;
    }

    /** 单个 IPv4 CIDR 网段 */
    private record Cidr(long network, int maskBits) {

        static Cidr parse(String text) {
            int slash = text.indexOf('/');
            String ipPart = text.substring(0, slash);
            String maskPart = text.substring(slash + 1);
            long ip = ipv4ToLong(ipPart);
            if (ip < 0) {
                return null;
            }
            int bits;
            try {
                bits = Integer.parseInt(maskPart);
            } catch (NumberFormatException e) {
                return null;
            }
            if (bits < 0 || bits > 32) {
                return null;
            }
            long mask = bits == 0 ? 0L : (0xFFFFFFFFL << (32 - bits)) & 0xFFFFFFFFL;
            return new Cidr(ip & mask, bits);
        }

        boolean contains(long value) {
            long mask = maskBits == 0 ? 0L : (0xFFFFFFFFL << (32 - maskBits)) & 0xFFFFFFFFL;
            return (value & mask) == network;
        }
    }
}
