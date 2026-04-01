package com.jnet.common.trace;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import static com.jnet.common.constant.Constants.TRACE_ID_HEADER;

/**
 * 链路追踪过滤器
 * <p>
 * 为每个请求生成或提取 TraceId，并在响应完成后清理
 * </p>
 *
 * <h4>📝 日志格式：</h4>
 * <ul>
 *   <li>统一使用 [Trace] [Servlet] 前缀</li>
 *   <li>TraceId 紧跟在组件标识后面：[Trace] [Servlet] [{traceId}]</li>
 *   <li>便于日志收集和分析</li>
 * </ul>
 */
@Slf4j
public class TraceFilter extends OncePerRequestFilter implements Ordered {


    @Override
    public int getOrder() {
        // 高优先级，确保在其他过滤器之前执行
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            // 从请求头中获取 TraceId（可能来自网关或上游服务）
            String traceId = request.getHeader(TRACE_ID_HEADER);

            if (traceId != null && !traceId.isEmpty()) {
                TraceContext.setTraceId(traceId);
                log.debug("[Trace] [Servlet] [{}] Extracted from request header", traceId);
            } else {
                // 如果没有则生成新的
                traceId = TraceContext.getOrGenerateTraceId();
                log.debug("[Trace] [Servlet] [{}] Generated new trace ID", traceId);
                
                // 🔥 关键修复：将新生成的 TraceId 添加到请求属性中，供下游获取
                request.setAttribute(TRACE_ID_HEADER, traceId);
            }

            // 继续执行过滤链
            filterChain.doFilter(request, response);

        } finally {
            // 清理 ThreadLocal，防止内存泄漏
            TraceContext.clear();
            log.trace("[Trace] [Servlet] Context cleared");
        }

    }
}
