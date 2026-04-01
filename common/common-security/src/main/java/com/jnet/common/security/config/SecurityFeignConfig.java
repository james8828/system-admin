package com.jnet.common.security.config;

import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;


/**
 * Common-Security Feign 配置类（Servlet 专用）
 * <p>
 * 为所有 Feign 请求自动添加 JWT Token，实现认证信息的传递
 * </p>
 *
 * <h3>核心功能：</h3>
 * <ul>
 *   <li>从 HttpServletRequest 提取 Authorization 头</li>
 *   <li>支持 Bearer Token 格式</li>
 *   <li>同时传递 TraceId 等链路追踪头</li>
 * </ul>
 *
 * @author JNet Team
 * @version 3.0 (Servlet Only)
 * @since 2024-01-01
 */
@Slf4j
@Configuration
public class SecurityFeignConfig {

    /**
     * Servlet 环境的 JWT Token 传递拦截器
     * <p>
     * 从当前 HttpServletRequest 中提取 Authorization 头并传递给 Feign 请求
     * </p>
     */
    @Bean
    public RequestInterceptor jwtRequestInterceptor() {
        log.info("[SecurityFeignConfig] Creating JWT request interceptor bean (SERVLET mode)");
        return requestTemplate -> {
            log.debug("Adding JWT token to Feign request: {}", requestTemplate.url());

            try {
                // 从 RequestContextHolder 获取当前请求
                ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

                if (attributes != null) {
                    HttpServletRequest request = attributes.getRequest();

                    // 提取 Authorization 头
                    String authorization = request.getHeader("Authorization");
                    if (authorization != null && !authorization.isEmpty()) {
                        requestTemplate.header("Authorization", authorization);
                        log.debug("Authorization header added: {}...",
                                authorization.substring(0, Math.min(20, authorization.length())));
                    } else {
                        log.debug("No Authorization header found in current request");
                    }
                } else {
                    log.warn("[SecurityFeignConfig] No ServletRequestAttributes found, cannot propagate auth headers");
                }
            } catch (Exception e) {
                log.error("Failed to add JWT token to Feign request: {}", e.getMessage(), e);
            }
        };
    }

}
