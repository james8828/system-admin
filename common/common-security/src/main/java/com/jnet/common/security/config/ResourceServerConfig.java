package com.jnet.common.security.config;

import com.jnet.common.security.filter.DynamicUrlPermissionFilter;
import com.jnet.common.security.filter.InternalServiceAuthFilter;
import com.jnet.common.security.service.PermissionMetadataService;
import jakarta.annotation.Resource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * OAuth2 资源服务器配置（Servlet 专用）
 * <p>
 * 配置 Spring Security 作为资源服务器，负责：
 * </p>
 * <ul>
 *   <li>JWT Token 验证（通过 OAuth2 Resource Server）</li>
 *   <li>内部服务认证（通过 InternalServiceAuthFilter）</li>
 *   <li>动态 URL 权限控制（通过 DynamicUrlPermissionFilter）</li>
 *   <li>方法级权限注解支持（@PreAuthorize, @PostAuthorize）</li>
 * </ul>
 * 
 * <h3>安全过滤器链顺序：</h3>
 * <ol>
 *   <li><b>InternalServiceAuthFilter</b> - 首先验证内部服务调用（允许绕过 JWT 验证）</li>
 *   <li><b>DynamicUrlPermissionFilter</b> - 动态 URL 权限检查（基于配置的权限映射）</li>
 *   <li><b>UsernamePasswordAuthenticationFilter</b> - 标准表单认证过滤器</li>
 *   <li><b>OAuth2 Resource Server</b> - JWT 令牌验证</li>
 * </ol>
 * 
 * <h3>配置说明：</h3>
 * <ul>
 *   <li>禁用 CSRF 保护（无状态会话）</li>
 *   <li>配置公开端点（actuator, swagger, error 等）</li>
 *   <li>其他所有请求需要认证</li>
 *   <li>JWKS 端点用于验证 JWT 签名</li>
 * </ul>
 * 
 * @author JNet Team
 * @version 2.0
 * @since 2024-01-01
 */
@Configuration
@EnableMethodSecurity(prePostEnabled = true)
@Import({InternalServiceAuthFilter.class, DynamicUrlPermissionFilter.class, PermissionMetadataService.class})
public class ResourceServerConfig {

    /**
     * 内部服务认证过滤器
     * 用于验证来自其他微服务的 Feign 调用
     */
    @Resource
    private InternalServiceAuthFilter internalServiceAuthFilter;

    /**
     * 动态 URL 权限过滤器
     * 用于根据配置动态检查 URL 权限
     */
    @Resource
    private DynamicUrlPermissionFilter dynamicUrlPermissionFilter;

        /**
         * 配置安全过滤器链
         * <p>
         * 这是 Spring Security 的核心配置，定义了完整的安全处理流程。
         * </p>
         *
         * <h3>配置内容：</h3>
         * <ol>
         *   <li><b>CSRF 配置</b>: 禁用 CSRF（因为是无状态的 REST API）</li>
         *   <li><b>会话管理</b>: STATELESS 模式，不使用 Session</li>
         *   <li><b>URL 授权</b>: 配置公开端点和受保护端点</li>
         *   <li><b>OAuth2 Resource Server</b>: 配置 JWT 验证的 JWKS 端点</li>
         *   <li><b>自定义过滤器</b>: 添加内部服务认证和动态 URL 权限过滤器</li>
         * </ol>
         *
         * @param http HttpSecurity 配置对象
         * @return SecurityFilterChain 安全过滤器链
         * @throws Exception 配置过程中可能出现的异常
         */
        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            http
                    // ========== 步骤 1: CSRF 配置 ==========
                    .csrf(csrf -> csrf.disable())

                    // ========== 步骤 2: 会话管理 ==========
                    .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                    // ========== 步骤 3: URL 授权配置 ==========
                    .authorizeHttpRequests(authz -> authz
                            // 公开端点 - 不需要认证
                            .requestMatchers("/api/actuator/**").permitAll()
                            .requestMatchers("/api-docs/**").permitAll()
                            .requestMatchers("/swagger-ui/**").permitAll()
                            .requestMatchers("/v3/api-docs/**").permitAll()
                            .requestMatchers("/error").permitAll()
                            .requestMatchers("/login/**", "/oauth2/**").permitAll()
                            // 其他所有请求都需要认证
                            .anyRequest().authenticated()
                    )

                    // ========== 步骤 4: OAuth2 资源服务器配置 ==========
                    // 配置 JWT 验证，指定 JWK Set URI 用于验证令牌签名
                    .oauth2ResourceServer(oauth2 -> oauth2
                            .jwt(jwt -> jwt
                                    .jwkSetUri("http://localhost:8080/oauth2/jwks")  // 认证服务器的JWKS端点
                            )
                    );

            // ========== 步骤 5: 添加自定义过滤器 ==========

            // 🔥 关键配置：将内部服务认证过滤器添加到安全过滤器链中
            // 位置：在 UsernamePasswordAuthenticationFilter 之前执行
            // 作用：内部服务请求可以绕过标准的 JWT 验证流程
            http.addFilterBefore(internalServiceAuthFilter, UsernamePasswordAuthenticationFilter.class);
            http.addFilterAfter(dynamicUrlPermissionFilter, BearerTokenAuthenticationFilter.class);
            return http.build();
        }

    /**
     * 密码编码器 Bean
     * <p>
     * 使用 BCrypt 强哈希算法，支持自动升级和安全存储用户密码。
     * </p>
     * 
     * @return PasswordEncoder BCrypt 密码编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}
