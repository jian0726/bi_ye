package com.jianyou.blog.common;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 鉴权上下文：拦截器解析 token 后写入，请求内任意位置可取当前登录用户。
 * 游客访问时值为 null（/portal/** 允许匿名）。
 */
public class AuthContext {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    @Data
    @AllArgsConstructor
    public static class LoginUser {
        private Long userId;
        private String role;
    }

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    /** 当前登录用户，未登录返回 null */
    public static LoginUser get() {
        return HOLDER.get();
    }

    /** 当前登录用户 id，未登录返回 null */
    public static Long getUserId() {
        LoginUser user = HOLDER.get();
        return user != null ? user.getUserId() : null;
    }

    /** 当前登录用户是否管理员 */
    public static boolean isAdmin() {
        LoginUser user = HOLDER.get();
        return user != null && "ADMIN".equals(user.getRole());
    }

    /** 请求结束时清理，防止线程池复用串号 */
    public static void clear() {
        HOLDER.remove();
    }
}
