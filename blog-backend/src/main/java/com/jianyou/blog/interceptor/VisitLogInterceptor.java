package com.jianyou.blog.interceptor;

import com.jianyou.blog.service.VisitService;
import com.jianyou.blog.util.IpUtils;
import com.jianyou.blog.util.VisitorKey;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 访问埋点拦截器：记录门户 GET 请求（IP / 访客标识 / 路径 / UA），异步落库。
 *
 * <p>visitorKey 复用浏览量的访客标识体系（登录 u:{userId} / 游客 ip:{IP}），
 * 供仪表盘按独立访客统计「今日访问」；AuthInterceptor 先于本拦截器注册，
 * 因此此处 AuthContext 已填充登录态。</p>
 */
@Component
@RequiredArgsConstructor
public class VisitLogInterceptor implements HandlerInterceptor {

    private final VisitService visitService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("GET".equalsIgnoreCase(request.getMethod())) {
            // requestURI 含 context-path(/api)，剥掉后与前端路由一致
            String uri = request.getRequestURI();
            String contextPath = request.getContextPath();
            if (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)) {
                uri = uri.substring(contextPath.length());
            }
            visitService.recordAsync(
                    VisitorKey.of(request),
                    IpUtils.getClientIp(request),
                    uri,
                    request.getHeader("User-Agent"));
        }
        return true;
    }
}
