package com.jnet.common.trace;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;

import static com.jnet.common.constant.Constants.TRACE_ID_HEADER;

/**
 * Feign 链路追踪拦截器
 * <p>
 * 在 Feign 请求中传递 TraceId，实现微服务间的链路追踪
 * </p>
 *
 * <h4>📝 日志格式：</h4>
 * <ul>
 *   <li>统一使用 [Trace] [Feign] 前缀</li>
 *   <li>TraceId 紧跟在组件标识后面：[Trace] [Feign] [{traceId}]</li>
 *   <li>便于日志收集和分析</li>
 * </ul>
 */
@Slf4j
public class TraceFeignInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        // 从当前线程获取 TraceId
        String traceId = TraceContext.getTraceId();

        if (traceId != null && !traceId.isEmpty()) {
            // 添加到 Feign 请求头中
            template.header(TRACE_ID_HEADER, traceId);
            log.debug("[Trace] [Feign] [{}] Adding TraceId to request: {}", traceId, template.url());
        } else {
            // 如果没有则生成新的
            traceId = TraceContext.generateTraceId();
            TraceContext.setTraceId(traceId);
            template.header(TRACE_ID_HEADER, traceId);
            log.debug("[Trace] [Feign] [{}] Generated new TraceId for request: {}", traceId, template.url());
        }
    }
}
