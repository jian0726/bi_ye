package com.jianyou.blog.interceptor;

import com.jianyou.blog.common.AuthContext;
import com.jianyou.blog.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 鉴权拦截器
 * - /admin/**：强制登录且必须 ADMIN，否则写回 401/403 JSON
 * - /portal/**：可选解析（有合法 token 就注入上下文，游客直接放行）
 * - 其余路径（/auth/**）：不处理
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        // 预检请求直接放行（CORS 由全局配置处理）
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        String path = request.getRequestURI();
        // 去掉 context-path（/api）
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }

        // 解析 token（请求头优先）；仅 access 有效 —— refresh 不能当作请求凭证
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        Claims claims = token != null ? jwtUtil.parseAccess(token) : null;
        if (claims != null) {
            AuthContext.set(new AuthContext.LoginUser(
                    Long.valueOf(claims.getSubject()),
                    claims.get("role", String.class)));
        }

        boolean adminArea = path.startsWith("/admin");
        if (!adminArea) {
            return true; // 门户区：游客可访问，上下文有则用
        }

        if (claims == null) {
            writeJson(response, 401, "请先登录");
            return false;
        }
        if (!AuthContext.isAdmin()) {
            writeJson(response, 403, "无权限访问管理后台");
            return false;
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        AuthContext.clear();
    }

    /** 与全局 Result 结构一致的 JSON 错误响应（HTTP 200 + 业务码，前端统一按 code 判断） */
    private void writeJson(HttpServletResponse response, int code, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json;charset=UTF-8");
        long ts = System.currentTimeMillis();
        response.getWriter().write(
                "{\"code\":" + code + ",\"message\":\"" + message + "\",\"data\":null,\"timestamp\":" + ts + "}");
    }
}
