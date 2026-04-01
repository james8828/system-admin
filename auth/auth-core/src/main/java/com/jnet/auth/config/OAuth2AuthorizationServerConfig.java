package com.jnet.auth.config;

import com.jnet.auth.cache.RedisRequestCache;
import com.jnet.auth.filter.OAuth2StateAuthenticationFilter;
import com.jnet.auth.handler.CustomAuthenticationSuccessHandler;
import com.jnet.auth.service.CustomUserDetailsService;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextPersistenceFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;

/**
 * OAuth2 授权服务器配置类
 *
 * 负责配置 Spring Authorization Server 的核心功能：
 * 1. 提供 OAuth2 授权端点（/oauth2/authorize）
 * 2. 提供 Token 端点（/oauth2/token）
 * 3. 支持 PKCE 模式
 * 4. 支持 OIDC (OpenID Connect)
 * 5. 使用 Redis 保存请求上下文和 OAuth2 参数
 */
@Slf4j
@Configuration
@EnableWebSecurity
public class OAuth2AuthorizationServerConfig {

    /**
     * 网关基础 URL 配置
     * 用于构建完整的 OAuth2 重定向地址
     * 默认值：http://localhost:8080
     */
    @Value("${gateway.base-url:http://localhost:8080}")
    private String gatewayBaseUrl;

    /**
     * 密码编码器 Bean
     *
     * 功能：
     * - 使用 BCrypt 强哈希算法加密用户密码
     * - BCrypt 是自适应的一-way 哈希函数，对暴力破解有很好的防护
     * - 自动处理盐值（salt），无需手动管理
     *
     * @return PasswordEncoder 实例
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 登录安全过滤器链（优先级：1）
     *
     * 职责：
     * - 处理 /login 和 /error 路径的安全认证
     * - 配置表单登录（Form Login）
     * - 集成 RedisRequestCache 保存 OAuth2 授权参数
     * - 配置自定义认证成功处理器
     *
     * 关键组件：
     * 1. DaoAuthenticationProvider: 基于 UserDetailsService 的认证提供者
     * 2. RedisRequestCache: 将 OAuth2 参数保存到 Redis，避免会话丢失
     * 3. CustomAuthenticationSuccessHandler: 认证成功后跳转到授权端点
     *
     * @param http HttpSecurity 配置对象
     * @param userDetailsService 自定义用户详情服务
     * @param redisRequestCache Redis 请求缓存
     * @param successHandler 认证成功处理器
     * @return SecurityFilterChain 安全过滤器链
     * @throws Exception 配置异常
     */
    @Bean
    @Order(1)
    public SecurityFilterChain loginSecurityFilterChain(
            HttpSecurity http,
            CustomUserDetailsService userDetailsService,
            RedisRequestCache redisRequestCache,
            CustomAuthenticationSuccessHandler successHandler) throws Exception {

        // 1. 配置 AuthenticationManager
        // 使用 DaoAuthenticationProvider 进行基于数据库的用户认证
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        http.authenticationProvider(authProvider);

        // 2. 设置安全匹配器
        // 只拦截 /login 和 /error 路径，其他路径由其他 FilterChain 处理
        http.securityMatcher("/login", "/error");

        // 3. 配置表单登录
        // loginPage: 登录页面路径
        // loginProcessingUrl: 处理登录 POST 请求的 URL
        // failureUrl: 登录失败后的重定向地址
        // successHandler: 认证成功后的处理器（会跳转到 OAuth2 授权端点）
        http.formLogin(form -> form
            .loginPage("/login")
            .loginProcessingUrl("/login")
            .failureUrl("/login?error=true")
            .successHandler(successHandler)
        );

        // 5. 配置 RequestCache
        // 使用 RedisRequestCache 保存 OAuth2 授权参数（如 client_id, redirect_uri, state 等）
        // 这样即使用户关闭浏览器，登录后仍能恢复 OAuth2 流程
        http.requestCache(cache -> cache
            .requestCache(redisRequestCache)
        );

        // 6. 禁用 CSRF
        // OAuth2 授权服务器通常不需要 CSRF 保护（使用 state 参数防重放攻击）
        http.csrf(AbstractHttpConfigurer::disable);

        return http.build();
    }

    /**
     * OAuth2 授权服务器安全过滤器链（优先级：2）
     *
     * 职责：
     * - 保护 OAuth2 端点（/oauth2/authorize, /oauth2/token 等）
     * - 应用 Spring Authorization Server 的默认安全配置
     * - 添加自定义的 OAuth2StateAuthenticationFilter
     * - 配置未认证时的重定向行为
     * 关键特性：
     * 1. 使用 OAuth2AuthorizationServerConfiguration.applyDefaultSecurity() 应用标准配置
     * 2. 通过 RedisRequestCache 保持 OAuth2 参数持久化
     * 3. 未认证时重定向到 /login 并保留所有 OAuth2 参数
     * 4. 支持 OIDC (OpenID Connect)
     *
     * @param redisRequestCache Redis 请求缓存
     * @param http HttpSecurity 配置对象
     * @return SecurityFilterChain 安全过滤器链
     * @throws Exception 配置异常
     */
    @Bean
    @Order(2)
    public SecurityFilterChain oauth2AuthorizationServerSecurityFilterChain(RedisRequestCache redisRequestCache,HttpSecurity http) throws Exception {

        // 2. 应用 OAuth2 Authorization Server 默认安全配置
        // 这会配置标准的 OAuth2 端点安全策略
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);

        // 3. 获取 OAuth2 端点匹配器
        // 用于确定哪些请求应该由这个 FilterChain 处理
        OAuth2AuthorizationServerConfigurer authorizationServerConfigurer =
                http.getConfigurer(OAuth2AuthorizationServerConfigurer.class);
        RequestMatcher endpointsMatcher = authorizationServerConfigurer.getEndpointsMatcher();

        // 配置 RequestCache，使用 Redis 保存 OAuth2 参数
        http.requestCache(cache -> cache
                .requestCache(redisRequestCache)
        );

        // 4. 设置安全匹配器
        // 只匹配 OAuth2 端点（如 /oauth2/authorize, /oauth2/token）
        http.securityMatcher(endpointsMatcher);

        // 5. 添加自定义过滤器
        // OAuth2StateAuthenticationFilter 用于在 OAuth2 回调时恢复认证状态
        // 放置在 SecurityContextPersistenceFilter 之前，确保能正确处理认证上下文
        http.addFilterBefore(new OAuth2StateAuthenticationFilter(redisRequestCache),
                SecurityContextPersistenceFilter.class);

        // 7. 配置认证入口点
        // 当用户未认证时，会自动重定向到登录页面
        // 关键点：保留所有 OAuth2 参数（client_id, redirect_uri, state 等）
        // 这样登录后可以正确完成 OAuth2 流程
        http.exceptionHandling(exceptions -> exceptions
            .authenticationEntryPoint((request, response, authException) -> {
                // 获取原始查询参数
                String queryString = request.getQueryString();
                // 构建登录页面重定向 URL
                String redirectUrl = gatewayBaseUrl + "/login";
                if (queryString != null && !queryString.isEmpty()) {
                    // 附加所有 OAuth2 参数
                    redirectUrl += "?" + queryString;
                }

                log.info("Redirecting to login with params: {}", redirectUrl);
                response.sendRedirect(redirectUrl);
            })
        );

        // 8. 启用 OIDC 支持
        // OpenID Connect 是基于 OAuth2 的身份层协议
        // 提供用户身份信息（id_token, userinfo endpoint 等）
        authorizationServerConfigurer.oidc(Customizer.withDefaults());

        // 9. 禁用 CSRF
        // OAuth2 使用 state 参数和 PKCE 来防止 CSRF 攻击
        http.csrf(AbstractHttpConfigurer::disable);

        return http.build();
    }

    /**
     * OAuth2 授权服务器设置 Bean
     *
     * 配置授权服务器的端点路径和发行者标识
     *
     * 关键配置：
     * - issuer: OAuth2 Token 的发行者标识
     *   格式：{gateway-base-url}/oauth2
     *   例如：http://localhost:8080/oauth2
     *
     * @return AuthorizationServerSettings 配置对象
     */
    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
            .issuer(gatewayBaseUrl + "/oauth2")
            .build();
    }

    /**
     * JWK (JSON Web Key) 源配置 Bean
     *
     * 功能：
     * - 生成和管理 RSA 密钥对
     * - 提供公钥给客户端用于验证 JWT Token 签名
     * - 端点：/oauth2/jwks
     *
     * 技术细节：
     * - 使用 RSA-2048 位密钥
     * - 通过 JWKSet 格式暴露公钥
     * - 符合 JWS (JSON Web Signature) 规范
     *
     * @return JWKSource<SecurityContext> JWK 源
     */
    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        // todo 从数据库或文件加载已存在的密钥对
        RSAKey rsaKey = generateRsaKey();
        JWKSet jwkSet = new JWKSet(rsaKey);
        return new ImmutableJWKSet<>(jwkSet);
    }

    /**
     * 生成 RSA 密钥对
     *
     * 过程：
     * 1. 调用 generateKeyPair() 生成 RSA 密钥对
     * 2. 提取公钥和私钥
     * 3. 使用 RSAKey.Builder 构建 JWK 格式的密钥
     * 4. 设置唯一的 keyID（用于密钥轮换）
     *
     * @return RSAKey JWK 格式的 RSA 密钥
     */
    private RSAKey generateRsaKey() {
        KeyPair keyPair = generateKeyPair();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        return new RSAKey.Builder(publicKey)
            .privateKey(privateKey)
            .keyID(UUID.randomUUID().toString())
            .build();
    }

    /**
     * 生成 RSA 密钥对
     *
     * 使用 Java Cryptography Architecture (JCA) 生成 2048 位 RSA 密钥对
     *
     * 安全性说明：
     * - 2048 位是当前推荐的最小密钥长度
     * - 对于更高安全需求，可考虑 3072 或 4096 位
     *
     * @return KeyPair RSA 公私钥对
     * @throws IllegalStateException 密钥生成失败时抛出
     */
    private KeyPair generateKeyPair() {
        try {
            // 获取 RSA 算法的 KeyPairGenerator 实例
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            // 初始化密钥长度为 2048 位
            keyPairGenerator.initialize(2048);
            // 生成密钥对
            return keyPairGenerator.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate RSA key pair", e);
        }
    }
}
