package com.jnet.common.security.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.jnet.common.trace.TraceContext;

/**
 * 动态 URL 权限判断工具类
 * <p>
 * 支持 Ant 风格路径匹配和正则表达式
 * </p>
 *
 * <h3>支持的权限类型：</h3>
 * <ul>
 *   <li>角色权限（ROLE_前缀）</li>
 *   <li>操作权限标识（system:user:list 等）</li>
 *   <li>URL 路径权限（/api/system/user/** 等）</li>
 *   <li>通配符权限（*:*:*）</li>
 * </ul>
 *
 * @author JNet Team
 * @version 2.0
 * @since 2024-01-01
 */
@Slf4j
@Component
public class DynamicPermissionChecker {

    /**
     * Spring 提供的路径匹配器（支持 Ant 风格）
     */
    private final PathMatcher pathMatcher = new AntPathMatcher();

    /**
     * 检查 URL 是否在用户权限列表中
     * <p>
     * 核心逻辑：
     * </p>
     * <ol>
     *   <li>检查是否有通配符权限（*:*:*）</li>
     *   <li>遍历用户权限列表，检查是否包含当前 URL 或匹配 URL 的路径</li>
     *   <li>支持 Ant 风格路径匹配（*, **, ?）</li>
     * </ol>
     *
     * @param requestUrl 请求的 URL
     * @param userPermissions 用户拥有的权限标识集合（包含角色、操作权限、URL 路径等）
     * @return true-有权限，false-无权限
     */
    public boolean hasPermission(String requestUrl, Set<String> userPermissions) {
        // 获取 TraceId（用于链路追踪）
        String traceId = TraceContext.getTraceId();
        if (traceId == null) {
            traceId = "unknown";
        }
        
        if (userPermissions == null || userPermissions.isEmpty()) {
            log.debug("[Trace] [Security] [{}] 用户无任何权限", traceId);
            return false;
        }

        // 检查是否有通配符权限（*:*:*）
        if (userPermissions.contains("*:*:*")) {
            log.debug("[Trace] [Security] [{}] 用户拥有通配符权限（*:*:*）", traceId);
            return true;
        }

        // 遍历用户权限列表，检查是否匹配
        for (String permission : userPermissions) {
            if (matchUrlWithPermission(requestUrl, permission, traceId)) {
                log.debug("[Trace] [Security] [{}] ✅ URL 权限匹配成功 - URL: {}, 权限：{}", traceId, requestUrl, permission);
                return true;
            }
        }

        log.debug("[Trace] [Security] [{}] ❌ 未找到匹配的权限 - URL: {}", traceId, requestUrl);
        return false;
    }

    /**
     * 将 URL 与单个权限进行匹配
     * <p>
     * 匹配规则：
     * </p>
     * <ol>
     *   <li>精确匹配：权限等于 URL</li>
     *   <li>Ant 风格匹配：权限是 Ant 模式（如 /api/**, /api/user/*）</li>
     *   <li>前缀匹配：权限以 /** 结尾</li>
     * </ol>
     *
     * @param url 请求 URL
     * @param permission 权限标识（可能是 URL 路径或权限标识）
     * @param traceId 链路追踪 ID
     * @return true-匹配成功，false-匹配失败
     */
    private boolean matchUrlWithPermission(String url, String permission, String traceId) {
        // 忽略空值和非 URL 权限（如 ROLE_ADMIN, system:user:list 等）
        if (permission == null || !permission.startsWith("/")) {
            return false;
        }

        log.debug("[Trace] [Security] [{}] 正在匹配 - URL: {}, 权限：{}", traceId, url, permission);

        // 1. 精确匹配
        if (permission.equals(url)) {
            log.debug("[Trace] [Security] [{}] ✅ 精确匹配", traceId);
            return true;
        }

        // 2. Ant 风格通配符匹配（使用 Spring 的 PathMatcher）
        if (pathMatcher.match(permission, url)) {
            log.debug("[Trace] [Security] [{}] ✅ Ant 模式匹配：{} -> {}", traceId, permission, url);
            return true;
        }

        // 3. ** 双星号前缀匹配
        if (permission.endsWith("/**")) {
            String prefix = permission.substring(0, permission.length() - 3);
            if (url.startsWith(prefix)) {
                log.debug("[Trace] [Security] [{}] ✅ 前缀匹配：{} -> {}", traceId, prefix, url);
                return true;
            }
        }

        log.debug("[Trace] [Security] [{}] ❌ 权限不匹配：{}", traceId, permission);
        return false;
    }

    /**
     * 解析 URL 对应的权限标识
     * <p>
     * 从 URL 权限映射表中解析出匹配的权限标识
     * </p>
     *
     * @param httpMethod HTTP 方法
     * @param url 请求 URL
     * @param urlPermissionMap URL 权限映射表
     * @return 权限标识集合
     */
    public Set<String> parsePermissionsFromUrl(String httpMethod, String url, 
                                                Map<String, Set<String>> urlPermissionMap) {
        Set<String> permissions = new HashSet<>();

        if (urlPermissionMap == null || urlPermissionMap.isEmpty()) {
            return permissions;
        }

        // 遍历所有 URL 模式，找到匹配的权限
        for (Map.Entry<String, Set<String>> entry : urlPermissionMap.entrySet()) {
            String urlPattern = entry.getKey();
            Set<String> perms = entry.getValue();

            if (pathMatcher.match(urlPattern, url)) {
                permissions.addAll(perms);
            }
        }

        log.debug("已解析 URL [{} {}] 的权限：{}", httpMethod, url, permissions);
        return permissions;
    }
}
