package com.jnet.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.cors.reactive.CorsUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * 跨域配置（CORS）
 * 
 * <p>由于 Gateway 基于 WebFlux，需要使用响应式的 CORS 配置</p>
 * 
 * <h3>配置说明：</h3>
 * <ul>
 *     <li>允许所有来源访问（生产环境应该限制为特定域名）</li>
 *     <li>允许所有 HTTP 方法（GET, POST, PUT, DELETE, OPTIONS）</li>
 *     <li>允许所有请求头</li>
 *     <li>允许携带凭证（Cookie、Authorization headers 等）</li>
 *     <li>预检请求缓存时间：3600 秒</li>
 * </ul>
 * 
 * <h3>安全提示：</h3>
 * <ul>
 *     <li>生产环境应该设置 allowedOrigins 为具体域名</li>
 *     <li>不要使用 "*" 同时允许携带凭证（会冲突）</li>
 * </ul>
 * 
 * @author JNet Team
 * @version 1.0
 * @since 2024-01-01
 */
@Configuration
public class CorsConfig {

    /**
     * 全局 CORS 过滤器
     * 
     * @return WebFilter 实例
     */
    @Bean
    public WebFilter corsFilter() {
        return (ServerWebExchange ctx, WebFilterChain chain) -> {
            ServerHttpRequest request = ctx.getRequest();

            // ========== 步骤 1: 判断是否为 CORS 请求 ==========
            if (CorsUtils.isCorsRequest(request)) {
                ServerHttpResponse response = ctx.getResponse();
                HttpHeaders headers = response.getHeaders();

                // ========== 步骤 2: 设置允许的源 ==========
                // 🔥 生产环境应该设置为具体的前端域名，如："https://example.com"
                headers.add("Access-Control-Allow-Origin", "*");

                // ========== 步骤 3: 设置允许的 HTTP 方法 ==========
                headers.add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");

                // ========== 步骤 4: 设置允许的请求头 ==========
                headers.add("Access-Control-Allow-Headers", "*");

                // ========== 步骤 5: 设置暴露的响应头 ==========
                headers.add("Access-Control-Expose-Headers", "*");

                // ========== 步骤 6: 设置是否允许携带凭证 ==========
                // 🔥 注意：如果设置为 true，Access-Control-Allow-Origin 不能为 "*"
                headers.add("Access-Control-Allow-Credentials", "true");

                // ========== 步骤 7: 设置预检请求缓存时间（秒） ==========
                headers.add("Access-Control-Max-Age", "3600");

                // ========== 步骤 8: 处理预检请求（OPTIONS） ==========
                if (request.getMethod() == HttpMethod.OPTIONS) {
                    response.setStatusCode(HttpStatus.OK);
                    return Mono.empty();
                }
            }

            // ========== 步骤 9: 继续执行过滤器链 ==========
            return chain.filter(ctx);
        };
    }
}
