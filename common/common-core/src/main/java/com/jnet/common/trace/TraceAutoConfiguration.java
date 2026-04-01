package com.jnet.common.trace;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 链路追踪自动配置
 * <p>
 * 为 Servlet 环境提供链路追踪组件支持
 * </p>
 *
 * <h3>适用场景：</h3>
 * <ul>
 *   <li>✅ 业务服务（Spring MVC / Servlet 环境）</li>
 *   <li>❌ 网关服务（Spring WebFlux / Reactive 环境）- 使用 GatewayTraceFilter</li>
 * </ul>
 *
 * @author JNet Team
 * @version 4.0 (明确区分应用场景)
 * @since 2024-01-01
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "jnet.trace", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TraceAutoConfiguration {

    /**
     * Feign 客户端的链路追踪拦截器
     * <p>
     * 在微服务间调用时自动传递 Trace ID
     * </p>
     *
     * @return 链路追踪拦截器
     */
    @Bean
    public TraceFeignInterceptor traceFeignInterceptor() {
        return new TraceFeignInterceptor();
    }

    /**
     * AOP 切面，用于方法级别的链路追踪
     * <p>
     * 可以在 Service 层等方法上记录链路信息
     * </p>
     *
     * @return 链路追踪切面
     */
    @Bean
    public TraceAspect traceAspect() {
        return new TraceAspect();
    }

    /**
     * Servlet 环境的过滤器
     * <p>
     * 为每个请求生成或提取 TraceId，并在响应完成后清理
     * </p>
     *
     * @return 链路追踪过滤器
     */
    @Bean
    public TraceFilter traceFilter() {
        return new TraceFilter();
    }
}

