package com.jnet.common.security.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 内部服务认证配置属性
 * 
 * <p>用于配置微服务架构中内部服务间的认证机制</p>
 * 
 * <h3>配置项说明：</h3>
 * <ul>
 *     <li>jnet.internal-auth.enabled - 是否启用内部服务认证（默认 true）</li>
 *     <li>jnet.internal-auth.secret - 内部服务共享密钥（默认：internal-service-secret-2024）</li>
 *     <li>jnet.internal-auth.allowed-services - 允许的服务名称列表（逗号分隔）</li>
 * </ul>
 * 
 * <h3>使用示例：</h3>
 * <pre>{@code
 * jnet:
 *   internal-auth:
 *     enabled: true
 *     secret: your-custom-secret-key
 *     allowed-services: jnet-auth,jnet-gateway,jnet-system
 * }</pre>
 * 
 * @author mu
 * @version 1.0
 * @since 2026/4/1
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "jnet.internal-auth")
public class InternalAuthProperties {

    /**
     * 是否启用内部服务认证
     * <p>默认为 true，禁用后所有内部服务调用将跳过认证</p>
     */
    private boolean enabled = true;

    /**
     * 内部服务密钥
     * <p>用于验证内部服务请求的合法性，所有服务必须配置相同的密钥</p>
     */
    private String secret = "internal-service-secret-2024";

    /**
     * 允许的内部服务名称列表（逗号分隔）
     * <p>只有在此列表中的服务才能通过内部服务认证</p>
     */
    private String allowedServices = "jnet-auth,jnet-gateway,jnet-resource";
}
