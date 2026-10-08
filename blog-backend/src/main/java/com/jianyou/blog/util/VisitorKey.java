package com.jianyou.blog.util;

import com.jianyou.blog.common.AuthContext;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 浏览量去重用的访客标识（长度控制在 t_view_record.visitor_key 的 64 以内）
 *
 * 已登录 → u:{userId}：换设备、换网络都算同一人
 * 未登录 → ip:{客户端IP}：同一网络出口下的多个访客会被合并计一次，
 *          这是无登录态博客的常规近似，取舍见选题方案「未落地清单」
 */
public final class VisitorKey {

    private VisitorKey() {
    }

    public static String of(HttpServletRequest request) {
        Long userId = AuthContext.getUserId();
        if (userId != null) {
            return "u:" + userId;
        }
        return "ip:" + normalize(request == null ? null : IpUtils.getClientIp(request));
    }

    /**
     * 回环地址归一化：浏览器访问 localhost 时可能解析为 IPv4 的 127.0.0.1，
     * 也可能解析为 IPv6 的 ::1 / 0:0:0:0:0:0:0:1。若不归一化，同一台机器会被
     * 当成两个访客，本地调试时浏览量会凭空翻倍。
     */
    private static String normalize(String ip) {
        if (ip == null || ip.isBlank()) {
            return "unknown";
        }
        if (ip.equals("::1") || ip.equals("0:0:0:0:0:0:0:1")) {
            return "127.0.0.1";
        }
        return ip;
    }
}
