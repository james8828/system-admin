package com.jnet.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 网关认证配置属性
 * 
 * <p>用于集中管理网关的自定义认证配置项，支持从 application.yml 自动绑定</p>
 * 
 * <h3>配置项说明：</h3>
 * <ul>
 *     <li>enable-token-check - 是否启用 Token 验证（默认 false）</li>
 *     <li>anonymous-urls - 匿名 URL 列表，跳过验证的路径</li>
 *     <li>token-expiry-check - 是否启用 Token 过期检查（默认 true）</li>
 *     <li><b>enable-blacklist-check - 是否启用黑名单检查（默认 true）</b></li>
 *     <li>token-prefix - Token 在 Redis 中的前缀（用于黑名单/白名单功能）</li>
 * </ul>
 * 
 * <h3>使用示例：</h3>
 * <pre>{@code
 * jnet:
 *   gateway:
 *     auth:
 *       enable-token-check: true
 *       anonymous-urls:
 *         - /auth/**
 *         - /public/**
 *       token-expiry-check: true
 *       enable-blacklist-check: true
 *       token-prefix: "token:"}
 * </pre>
 *
 * @author mu
 * @version 1.1 (增加黑名单检查)
 * @since 2026/4/1
 */
@Data
@Component
@ConfigurationProperties(prefix = "jnet.gateway.auth")
public class GatewayAuthProperties {

    /**
     * 是否启用 Token 验证
     * 
     * <p>默认不启用，开发环境方便调试；生产环境建议启用</p>
     */
    private boolean enableTokenCheck = false;

    /**
     * 匿名 URL 列表
     * 
     * <p>跳过验证的路径配置，支持 Ant 风格的路径模式</p>
     * 
     * <h4>支持的路径模式：</h4>
     * <ul>
     *     <li>? - 匹配单个字符</li>
     *     <li>* - 匹配零个或多个字符</li>
     *     <li>** - 匹配零个或多个目录</li>
     * </ul>
     * 
     * <h4>示例：</h4>
     * <ul>
     *     <li>/auth/** - 匹配 /auth/ 开头的所有路径</li>
     *     <li>/*.js - 匹配所有 JS 文件</li>
     *     <li>/api/v?/users - 匹配 /api/v1/users, /api/v2/users 等</li>
     * </ul>
     */
    private List<String> anonymousUrls = new ArrayList<>();

    /**
     * 是否启用 Token 过期检查
     * 
     * <p>启用后会解析 JWT payload 中的 exp 字段并检查是否过期</p>
     * <p>注意：这只是基本检查，完整的 Token 验证需要在资源服务器中进行</p>
     */
    private boolean tokenExpiryCheck = true;

    /**
     * 是否启用黑名单检查
     * 
     * <p>启用后会检查 Redis 中的 Token 黑名单，已撤销/注销的 Token 会被拒绝访问</p>
     * <p>依赖 Redis 服务，需要确保 Redis 可用</p>
     * <p>生产环境强烈建议启用，提高安全性</p>
     */
    private boolean enableBlacklistCheck = true;

    /**
     * Token 在 Redis 中的前缀
     * 
     * <p>用于后续可能的 Redis Token 黑名单/白名单功能</p>
     * <p>示例：token:abc123def456</p>
     */
    private String tokenPrefix = "token:";
}
