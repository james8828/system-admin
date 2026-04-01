package com.jnet.gateway.filter;

import com.jnet.common.trace.TraceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static com.jnet.common.constant.Constants.TRACE_ID_HEADER;

/**
 * 网关链路追踪全局过滤器
 * <p>
 * 在网关层统一处理链路追踪，为每个请求生成或提取 Trace ID，
 * 并通过请求头传递给下游微服务，实现全链路追踪。
 * </p>
 *
 * <h3>工作流程：</h3>
 * <ol>
 *   <li>从请求头中提取 Trace ID（如果存在）</li>
 *   <li>如果没有则生成新的 Trace ID</li>
 *   <li>将 Trace ID 添加到请求头，传递给下游服务</li>
 *   <li>将 Trace ID 添加到响应头，返回给客户端</li>
 * </ol>
 *
 * <h4>⚠️ 注意事项：</h4>
 * <ul>
 *   <li>本过滤器不使用 ThreadLocal（WebFlux 响应式环境不适用）</li>
 *   <li>完全通过 HTTP 请求头传递 Trace ID</li>
 *   <li>下游 Servlet 服务会从请求头获取并设置到 ThreadLocal</li>
 * </ul>
 *
 * @author mu
 * @version 1.0
 * @since 2026/4/1
 */
@Slf4j
@Component
public class GatewayTraceFilter implements GlobalFilter, Ordered {

    /**
     * 过滤器执行顺序
     * <p>
     * 设置为最高优先级 +1，确保在其他过滤器之前执行
     * 这样可以保证所有请求都带有 Trace ID
     * </p>
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }

    /**
     * 核心过滤方法
     * <p>
     * 为每个请求提供链路追踪支持：
     * </p>
     * <ul>
     *   <li>从请求头或上游获取 Trace ID</li>
     *   <li>生成新的 Trace ID（如果需要）</li>
     *   <li>传递到下游服务（通过请求头）</li>
     *   <li>返回给客户端（通过响应头）</li>
     * </ul>
     *
     * <h4>📝 日志格式：</h4>
     * <ul>
     *   <li>统一使用 [Trace] [Gateway] 前缀</li>
     *   <li>便于日志收集和分析</li>
     * </ul>
     *
     * @param exchange 请求上下文
     * @param chain 过滤器链
     * @return Mono<Void> 异步处理结果
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // ========== 步骤 1: 获取或生成 Trace ID ==========
        String traceId = getOrGenerateTraceId(exchange);
        
        log.debug("[Trace] [Gateway] [{}] 开始处理请求", traceId);
        
        // ========== 步骤 2: 添加到请求头和响应头 ==========
        ServerWebExchange mutatedExchange = addTraceIdToRequest(exchange, traceId);
        
        // ========== 步骤 3: 继续执行过滤器链 ==========
        return chain.filter(mutatedExchange)
                .doOnSuccess(aVoid -> {
                    log.trace("[Trace] [Gateway] [{}] 请求处理成功", traceId);
                })
                .doOnError(throwable -> {
                    log.error("[Trace] [Gateway] [{}] 发生错误：{}", traceId, throwable.getMessage(), throwable);
                });
    }

    /**
     * 生成或获取 Trace ID
     * 
     * <p>优先从请求头中获取（可能来自客户端或上游服务），如果没有则生成新的 Trace ID</p>
     *
     * @param exchange 请求上下文
     * @return Trace ID
     */
    private String getOrGenerateTraceId(ServerWebExchange exchange) {
        HttpHeaders headers = exchange.getRequest().getHeaders();
        
        // 尝试从请求头获取 Trace ID
        String traceId = headers.getFirst(TRACE_ID_HEADER);
        
        if (traceId != null && !traceId.isEmpty()) {
            log.debug("[Trace] [Gateway] [{}] 从请求头中提取", traceId);
            return traceId;
        }
        
        // 生成新的 Trace ID（16 位 UUID）
        traceId = TraceContext.generateTraceId();
        log.debug("[Trace] [Gateway] [{}] 生成新的 Trace ID", traceId);
        return traceId;
    }

    /**
     * 将 Trace ID 添加到请求头
     * 
     * <p>同时设置到请求头和响应头，确保：</p>
     * <ul>
     *   <li>下游服务可以接收到 Trace ID</li>
     *   <li>客户端可以在响应中看到 Trace ID</li>
     * </ul>
     *
     * @param exchange 原始请求上下文
     * @param traceId Trace ID
     * @return 修改后的请求上下文
     */
    private ServerWebExchange addTraceIdToRequest(ServerWebExchange exchange, String traceId) {
        // 添加到响应头（返回给客户端）
        exchange.getResponse().getHeaders().add(TRACE_ID_HEADER, traceId);
        
        // 添加到请求头（传递给下游服务）并返回新的 exchange
        return exchange.mutate()
                .request(exchange.getRequest().mutate()
                        .header(TRACE_ID_HEADER, traceId)
                        .build())
                .build();
    }
}
