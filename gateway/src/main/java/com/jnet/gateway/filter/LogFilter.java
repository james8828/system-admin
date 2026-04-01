package com.jnet.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static com.jnet.common.constant.Constants.TRACE_ID_HEADER;

/**
 * 日志追踪过滤器
 * 
 * <p>为每个请求生成唯一的追踪 ID，并记录请求和响应信息</p>
 * 
 * <h3>主要功能：</h3>
 * <ul>
 *     <li>生成全局唯一的 Trace ID（与 GatewayTraceFilter 配合）</li>
 *     <li>记录请求方法、路径等信息</li>
 *     <li>记录响应状态码和耗时</li>
 *     <li>将 Trace ID 传递到下游服务</li>
 *     <li>便于问题排查和链路追踪</li>
 * </ul>
 * 
 * <h3>Trace ID 传递流程：</h3>
 * <ol>
 *     <li>客户端请求到达网关</li>
 *     <li>GatewayTraceFilter 生成 Trace ID 并添加到请求头 X-Trace-ID</li>
 *     <li>本过滤器从请求头获取 Trace ID 并记录日志</li>
 *     <li>将请求转发到下游服务（自动携带 Trace ID）</li>
 *     <li>下游服务在日志中输出相同的 Trace ID</li>
 *     <li>通过 Trace ID 可以在日志系统中追踪完整的调用链</li>
 * </ol>
 *
 * @author mu
 * @version 1.0
 * @since 2026/4/1
 */
@Slf4j
@Component
public class LogFilter implements GlobalFilter, Ordered {

    /**
     * 注意：不再使用独立的 REQUEST_ID_HEADER，统一使用 Constants.TRACE_ID_HEADER
     */

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // ========== 步骤 1: 从请求头获取 Trace ID（由 GatewayTraceFilter 设置） ==========
        String traceId = request.getHeaders().getFirst(TRACE_ID_HEADER);
        if (traceId == null || traceId.isEmpty()) {
            // 如果没有则生成新的（正常情况下应该由 GatewayTraceFilter 设置）
            traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            log.warn("[Trace] [Gateway] [{}] 请求中未找到 TraceId，已生成新的", traceId);
        }
        final String finalTraceId = traceId;

        // ========== 步骤 2: 记录请求开始时间 ==========
        final long startTime = System.currentTimeMillis();

        // ========== 步骤 3: 继续执行过滤器链（不需要再添加 Trace ID 到请求头） ==========
        // GatewayTraceFilter 已经添加了 Trace ID 到请求头

        // ========== 步骤 4: 记录请求日志（包含 Trace ID） ==========
        log.info("[Trace] [Gateway] [{}] 收到请求：{} {}",
                finalTraceId,
                request.getMethod(),
                request.getPath());

        // ========== 步骤 5: 记录响应日志（包含耗时和 Trace ID） ==========
        return chain.filter(exchange)
                .then(Mono.fromRunnable(() -> {
                    long endTime = System.currentTimeMillis();
                    long duration = endTime - startTime;
                    
                    log.info("[Trace] [Gateway] [{}] 响应完成：{} - 耗时 {}ms",
                            finalTraceId,
                            exchange.getResponse().getStatusCode(),
                            duration);
                }));
    }

    @Override
    public int getOrder() {
        // 优先级：低于 AuthFilter，高于其他业务过滤器
        return Ordered.HIGHEST_PRECEDENCE + 200;
    }
}
