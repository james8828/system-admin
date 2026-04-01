package com.jnet.auth.config;

import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import static com.jnet.common.constant.Constants.*;

/**
 * Feign 客户端配置类
 * 
 * <p>核心功能：为 auth 服务的 Feign 客户端提供统一配置</p>
 * 
 * <p>主要职责：</p>
 * <ul>
 *     <li>添加内部服务认证头，实现服务间安全调用</li>
 *     <li>配置错误解码器，统一处理 Feign 请求异常</li>
 *     <li>支持服务名标识，便于链路追踪和权限控制</li>
 * </ul>
 * 
 * @author mu
 * @version 1.0
 * @since 2026/4/1
 */
@Slf4j
@Configuration
public class FeignConfig {

    @Value("${jnet.internal-auth.secret:internal-service-secret-2024}")
    private String internalSecret;

    @Value("${spring.application.name:jnet-auth}")
    private String serviceName;

    /**
     * 内部服务认证请求拦截器
     * 
     * <p>为所有 Feign 请求添加内部服务认证头，实现服务间安全调用</p>
     * <p>添加的请求头：</p>
     * <ul>
     *     <li>INTERNAL_SERVICE_AUTH_SERVICE_TOKEN - 服务密钥（用于身份验证）</li>
     *     <li>INTERNAL_SERVICE_AUTH_SERVICE_NAME - 服务名称（用于标识调用方）</li>
     *     <li>INTERNAL_SERVICE_FLAG - 内部服务标识（true）</li>
     * </ul>
     * 
     * @return RequestInterceptor 请求拦截器实例
     */
    @Bean
    public RequestInterceptor internalServiceAuthRequestInterceptor() {
        return requestTemplate -> {
            log.debug("正在为 Feign 请求添加内部服务认证头：{}", requestTemplate.url());
    
            // 添加内部服务认证头
            requestTemplate.header(INTERNAL_SERVICE_AUTH_SERVICE_TOKEN, internalSecret);
            requestTemplate.header(INTERNAL_SERVICE_AUTH_SERVICE_NAME, serviceName);
            requestTemplate.header(INTERNAL_SERVICE_FLAG, "true");
    
            log.debug("已为服务 [{}] 添加内部服务认证头", serviceName);
        };
    }

    /**
     * Feign 错误解码器
     * 
     * <p>统一处理 Feign 请求失败的情况，根据 HTTP 状态码提供详细的错误信息</p>
     * <p>错误处理策略：</p>
     * <ul>
     *     <li>401 - 内部服务认证失败（检查服务密钥配置）</li>
     *     <li>403 - 内部服务授权失败（权限不足）</li>
     *     <li>其他状态码 - 通用请求失败异常</li>
     * </ul>
     * 
     * @return ErrorDecoder 错误解码器实例
     */
    @Bean
    public ErrorDecoder feignErrorDecoder() {
        return (methodKey, response) -> {
            log.error("Feign 请求失败 - 方法：{}, 状态码：{}, URL: {}",
                    methodKey, response.status(), response.request().url());
    
            if (response.status() == 401) {
                log.error("内部服务认证失败 - 请检查服务密钥配置");
                return new RuntimeException("内部服务认证失败");
            } else if (response.status() == 403) {
                log.error("内部服务授权失败 - 权限不足");
                return new RuntimeException("内部服务授权失败");
            }
    
            return new RuntimeException("Feign 请求失败，状态码：" + response.status());
        };
    }
}

