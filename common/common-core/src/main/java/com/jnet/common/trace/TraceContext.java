package com.jnet.common.trace;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.UUID;

/**
 * 链路追踪上下文
 * 
 * <p>用于在线程中存储和获取 TraceId，适用于 Servlet 环境</p>
 * 
 * <h3>主要功能：</h3>
 * <ul>
 *     <li>生成全局唯一的 TraceId（16 位 UUID）</li>
 *     <li>在线程间传递 TraceId（使用 InheritableThreadLocal）</li>
 *     <li>子线程自动继承父线程的 TraceId</li>
 *     <li>提供清理方法防止内存泄漏</li>
 * </ul>
 * 
 * <h3>线程支持：</h3>
 * <ul>
 *     <li>✅ 支持父子线程间的 TraceId 传递（使用 InheritableThreadLocal）</li>
 *     <li>✅ 子线程会自动继承父线程的 TraceId</li>
 *     <li>⚠️ 注意：在线程池中使用时需要手动传递（因为线程复用）</li>
 * </ul>
 * 
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 设置 TraceId
 * TraceContext.setTraceId("abc123");
 * 
 * // 获取当前线程的 TraceId
 * String traceId = TraceContext.getTraceId();
 * 
 * // 获取或生成 TraceId
 * String traceId = TraceContext.getOrGenerateTraceId();
 * 
 * // 清除 TraceId（请求结束后必须调用）
 * TraceContext.clear();
 * }</pre>
 * 
 * @author mu
 * @version 4.0 (支持子线程继承)
 * @since 2026/4/1
 */
@Slf4j
public class TraceContext {

    /**
     * ThreadLocal 存储（用于 Servlet 环境）
     * 
     * <p>⚠️ 使用 InheritableThreadLocal 支持父子线程传递</p>
     */
    private static final InheritableThreadLocal<String> TRACE_ID_HOLDER = new InheritableThreadLocal<>();

    /**
     * 生成新的 TraceId
     *
     * @return 格式化的 TraceId（16 位 UUID，去除了连字符）
     */
    public static String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    /**
     * 设置 TraceId
     * 
     * <p>将 TraceId 存储到 ThreadLocal 中，供当前线程使用</p>
     *
     * @param traceId 链路追踪 ID
     */
    public static void setTraceId(String traceId) {
        if (StringUtils.hasText(traceId)) {
            TRACE_ID_HOLDER.set(traceId);
            log.debug("TraceId 已设置到 ThreadLocal: {}", traceId);
        }
    }

    /**
     * 获取当前线程的 TraceId
     * 
     * <p>从 ThreadLocal 中获取 TraceId</p>
     *
     * @return TraceId，如果没有则返回 null
     */
    public static String getTraceId() {
        return TRACE_ID_HOLDER.get();
    }

    /**
     * 获取当前线程的 TraceId，如果没有则生成一个新的
     * 
     * <p>优先从 ThreadLocal 获取，不存在则生成新的 TraceId</p>
     *
     * @return TraceId
     */
    public static String getOrGenerateTraceId() {
        String traceId = TRACE_ID_HOLDER.get();
        if (!StringUtils.hasText(traceId)) {
            traceId = generateTraceId();
            TRACE_ID_HOLDER.set(traceId);
            log.debug("生成新的 TraceId: {}", traceId);
        }
        return traceId;
    }

    /**
     * 清除当前线程的 TraceId
     * 
     * <p>清理 ThreadLocal，防止内存泄漏</p>
     * 
     * <h4>⚠️ 注意事项：</h4>
     * <ul>
     *   <li>在请求结束后必须调用此方法</li>
     *   <li>特别是使用线程池时，必须手动清理</li>
     *   <li>否则会导致内存泄漏或 TraceId 污染</li>
     * </ul>
     */
    public static void clear() {
        TRACE_ID_HOLDER.remove();
        log.debug("ThreadLocal TraceId 已清理");
    }
}
