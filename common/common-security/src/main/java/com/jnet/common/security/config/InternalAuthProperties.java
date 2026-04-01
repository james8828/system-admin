package com.jnet.common.security.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 内部服务认证配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "jnet.internal-auth")
public class InternalAuthProperties {

    /**
     * 是否启用内部服务认证
     */
    private boolean enabled = true;

    /**
     * 内部服务密钥
     */
    private String secret = "internal-service-secret-2024";

    /**
     * 允许的内部服务名称列表（逗号分隔）
     */
    private String allowedServices = "jnet-auth,jnet-gateway,jnet-resource";
}
