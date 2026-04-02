package com.jnet.common.mybatis;

/**
 * 租户上下文持有者
 * 
 * <p>使用 ThreadLocal 存储当前请求的租户 ID，实现线程隔离的多租户上下文</p>
 * 
 * <h3>主要功能：</h3>
 * <ul>
 *     <li>setTenantId - 设置当前线程的租户 ID</li>
 *     <li>getTenantId - 获取当前线程的租户 ID</li>
 *     <li>clear - 清理当前线程的租户 ID（防止内存泄漏）</li>
 * </ul>
 * 
 * <h3>使用场景：</h3>
 * <ul>
 *     <li>在过滤器/拦截器中从请求头提取租户 ID 并设置</li>
 *     <li>在 MyBatis TenantLineHandler 中获取租户 ID 用于 SQL 拼接</li>
 *     <li>在请求结束后必须调用 clear() 清理 ThreadLocal</li>
 * </ul>
 * 
 * <h3>注意事项：</h3>
 * <ul>
 *     <li>⚠️ 必须在请求结束后调用 clear()，否则会导致内存泄漏</li>
 *     <li>⚠️ 在线程池环境中需要手动传递租户 ID</li>
 * </ul>
 * 
 * @author mu
 * @version 1.0
 * @since 2026/4/1
 */

public class TenantContextHolder {

    /**
     * ThreadLocal 存储当前线程的租户 ID
     */
    private static final ThreadLocal<String> CONTEXT_HOLDER = new ThreadLocal<>();

    /**
     * 设置当前线程的租户 ID
     * @param tenantId 租户 ID
     */
    public static void setTenantId(String tenantId) {
        CONTEXT_HOLDER.set(tenantId);
    }

    /**
     * 获取当前线程的租户 ID
     * @return 租户 ID，如果未设置返回 null
     */
    public static String getTenantId() {
        return CONTEXT_HOLDER.get();
    }

    /**
     * 清理当前线程的租户 ID
     * ⚠️ 必须在请求结束后调用，防止内存泄漏
     */
    public static void clear() {
        CONTEXT_HOLDER.remove();
    }

}
