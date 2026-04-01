package com.jnet.auth.config;

import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import static com.jnet.common.constant.Constants.*;

/**
 * Auth-Core Feign配置类
 * 配置内部服务认证和错误处理
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
     * 为所有Feign请求添加内部服务认证头
     */
    @Bean
    public RequestInterceptor internalServiceAuthRequestInterceptor() {
        return requestTemplate -> {
            log.debug("Adding internal service auth headers to Feign request: {}", requestTemplate.url());

            // 添加内部服务认证头
            requestTemplate.header(INTERNAL_SERVICE_AUTH_SERVICE_TOKEN, internalSecret);
            requestTemplate.header(INTERNAL_SERVICE_AUTH_SERVICE_NAME, serviceName);
            requestTemplate.header(INTERNAL_SERVICE_FLAG, "true");

            log.debug("Internal service auth headers added for service: {}", serviceName);
        };
    }

    /**
     * Feign错误解码器
     * 处理内部服务调用失败的情况
     */
    @Bean
    public ErrorDecoder feignErrorDecoder() {
        return (methodKey, response) -> {
            log.error("Feign request failed - Method: {}, Status: {}, URL: {}",
                    methodKey, response.status(), response.request().url());

            if (response.status() == 401) {
                log.error("Internal service authentication failed - check service token configuration");
                return new RuntimeException("Internal service authentication failed");
            } else if (response.status() == 403) {
                log.error("Internal service authorization failed - insufficient permissions");
                return new RuntimeException("Internal service authorization failed");
            }

            return new RuntimeException("Feign request failed with status: " + response.status());
        };
    }
}

