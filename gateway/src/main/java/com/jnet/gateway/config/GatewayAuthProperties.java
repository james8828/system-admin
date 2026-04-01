package com.jnet.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 网关认证配置属性
 * <p>
 * 用于集中管理网关的自定义认证配置项，支持从 application.yml 自动绑定
 * </p>
 *
 * @author JNet Team
 * @version 1.0
 * @since 2024-01-01
 */
@Data
@Component
@ConfigurationProperties(prefix = "jnet.gateway.auth")
public class GatewayAuthProperties {

    /**
     * 是否启用 Token 验证
     * <p>
     * 默认不启用，生产环境建议启用
     * </p>
     */
    private boolean enableTokenCheck = false;

    /**
     * 匿名 URL
     * 跳过验证的路径列表
     * <p>
     * 支持 Ant 风格的路径模式，如 /api/**, /*.js 等
     * </p>
     */
    private List<String> anonymousUrls = new ArrayList<>();

    /**
     * 是否启用 Token 过期检查
     * <p>
     * 启用后会解析 JWT payload 中的 exp 字段并检查是否过期
     * </p>
     */
    private boolean tokenExpiryCheck = true;

    /**
     * Token 在 Redis 中的前缀
     * <p>
     * 用于后续可能的 Redis Token 黑名单/白名单功能
     * </p>
     */
    private String tokenPrefix = "token:";
}
