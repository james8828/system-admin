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
 * 
 * <p>为所有 Feign 请求自动添加 JWT Token，实现认证信息的传递</p>
 * 
 * <h3>核心功能：</h3>
 * <ul>
 *     <li>从 HttpServletRequest 提取 Authorization 头</li>
 *     <li>支持 Bearer Token 格式</li>
 *     <li>同时传递 TraceId 等链路追踪头</li>
 *     <li>保证微服务间调用的认证连续性</li>
 * </ul>
 * 
 * <h3>使用场景：</h3>
 * <ul>
 *     <li>Gateway → Auth Service（传递用户 Token）</li>
 *     <li>Gateway → System Service（传递用户 Token）</li>
 *     <li>Service → Service（内部服务调用）</li>
 * </ul>
 * 
 * @author mu
 * @version 3.0 (Servlet Only)
 * @since 2026/4/1
 */
@Slf4j
@Configuration
public class SecurityFeignConfig {

    /**
     * Servlet 环境的 JWT Token 传递拦截器
     * 
     * <p>从当前 HttpServletRequest 中提取 Authorization 头并传递给 Feign 请求</p>
     */
    @Bean
    public RequestInterceptor jwtRequestInterceptor() {
        log.info("[SecurityFeignConfig] 正在创建 JWT 请求拦截器（SERVLET 模式）");
        return requestTemplate -> {
            log.debug("正在为 Feign 请求添加 JWT Token: {}", requestTemplate.url());

            try {
                // 从 RequestContextHolder 获取当前请求
                ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

                if (attributes != null) {
                    HttpServletRequest request = attributes.getRequest();

                    // 提取 Authorization 头
                    String authorization = request.getHeader("Authorization");
                    if (authorization != null && !authorization.isEmpty()) {
                        requestTemplate.header("Authorization", authorization);
                        log.debug("已添加 Authorization 头：{}...",
                                authorization.substring(0, Math.min(20, authorization.length())));
                    } else {
                        log.debug("当前请求中没有 Authorization 头");
                    }
                } else {
                    log.warn("[SecurityFeignConfig] 未找到 ServletRequestAttributes，无法传递认证头");
                }
            } catch (Exception e) {
                log.error("为 Feign 请求添加 JWT Token 失败：{}", e.getMessage(), e);
            }
        };
    }

}
