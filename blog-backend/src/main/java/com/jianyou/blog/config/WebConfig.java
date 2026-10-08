package com.jianyou.blog.config;

import com.jianyou.blog.interceptor.AuthInterceptor;
import com.jianyou.blog.interceptor.VisitLogInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 配置：跨域（开发调试用，生产走 nginx 同源反代）+ 鉴权拦截器 + 访问埋点拦截器
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final VisitLogInterceptor visitLogInterceptor;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/portal/**", "/admin/**", "/auth/**");
        // 访问埋点：仅门户请求（后台与接口调用不计入）
        registry.addInterceptor(visitLogInterceptor)
                .addPathPatterns("/portal/**");
    }
}
