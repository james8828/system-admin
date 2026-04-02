package com.jnet.common.security.utils;

import com.jnet.common.trace.TraceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * SecurityContext 工具类
 *
 * <p>提供从 Spring Security 上下文中提取用户信息、权限、Token 等功能的工具类</p>
 *
 * <h3>主要功能：</h3>
 * <ul>
 *     <li>获取用户权限列表（getAuthorities）</li>
 *     <li>权限校验（hasPermission, hasRole）</li>
 *     <li>提取用户 ID（extractUserIdFromToken）</li>
 *     <li>提取用户名（extractUsernameFromSecurityContext）</li>
 *     <li>提取 Token（extractTokenFromSecurityContext）</li>
 * </ul>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 获取当前用户权限
 * List<String> authorities = SecurityContextUtils.getAuthorities();
 *
 * // 检查权限
 * boolean hasPerm = SecurityContextUtils.hasPermission("user:add");
 *
 * // 提取用户信息
 * Long userId = SecurityContextUtils.extractUserIdFromToken();
 * String username = SecurityContextUtils.extractUsernameFromSecurityContext();
 * String token = SecurityContextUtils.extractTokenFromSecurityContext();
 * }</pre>
 *
 * @author mu
 * @version 1.0
 * @since 2026/4/1
 */
@Slf4j
public class SecurityContextUtils {

    public static List<String> getAuthorities() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return new ArrayList<>();
        }
        Collection<? extends org.springframework.security.core.GrantedAuthority> authorities = authentication.getAuthorities();
        List<String> authorityList = new ArrayList<>();
        for (org.springframework.security.core.GrantedAuthority authority : authorities) {
            authorityList.add(authority.getAuthority());
        }
        return authorityList;
    }

    public static List<String> getPermissions() {
        return getAuthorities();
    }

    public static boolean hasPermission(String permission) {
        List<String> authorities = getAuthorities();
        return authorities.contains(permission);
    }


    public static boolean hasRole(String role) {
        List<String> authorities = getAuthorities();
        return authorities.contains("ROLE_" + role) || authorities.contains(role);
    }


    /**
     * 从 JWT Token claims 中提取 userId
     * 优先使用此方法，因为 Token 中已包含 userId
     *
     * @return userId，如果不存在返回 null
     */
    public static Long extractUserIdFromToken() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return null;
            }

            // 情况 1: JwtAuthenticationToken（Resource Server 标准情况）
            if (authentication instanceof org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken jwtAuth) {
                Object userIdObj = jwtAuth.getToken().getClaim("userId");
                if (userIdObj instanceof Number) {
                    Long userId = ((Number) userIdObj).longValue();
                    log.debug("从 JwtAuthenticationToken 中提取到 userId={}", userId);
                    return userId;
                }
                log.debug("JwtAuthenticationToken 中未找到 userId claim");
                return null;
            }

            // 情况 2: 尝试通过反射从任意 Authentication 的 attributes 中获取
            // 使用反射避免依赖问题
            try {
                var getAttributesMethod = authentication.getClass().getMethod("getAttributes");
                @SuppressWarnings("unchecked")
                Map<String, Object> attributes = (Map<String, Object>) getAttributesMethod.invoke(authentication);
                Object userIdObj = attributes.get("userId");
                if (userIdObj instanceof Number) {
                    Long userId = ((Number) userIdObj).longValue();
                    log.debug("从 Authentication.attributes 中提取到 userId={}", userId);
                    return userId;
                }
            } catch (NoSuchMethodException e) {
                // 不支持 getAttributes 方法，忽略
            }

            // 情况 3: 从 authentication details 中获取
            if (authentication.getDetails() instanceof java.util.Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> details = (Map<String, Object>) authentication.getDetails();
                Object userIdObj = details.get("userId");
                if (userIdObj instanceof Number) {
                    Long userId = ((Number) userIdObj).longValue();
                    log.debug("从 Authentication.details 中提取到 userId={}", userId);
                    return userId;
                }
            }

            log.debug("无法从 Authentication 中提取 userId，类型：{}", authentication.getClass().getSimpleName());
            return null;
        } catch (Exception e) {
            log.error("Failed to extract userId from token", e);
            return null;
        }
    }

    /**
     * 从 SecurityContextHolder 中提取用户名
     *
     * @return 用户名，如果不存在返回 null
     */
    public static String extractUsernameFromSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            String traceId = TraceContext.getTraceId();
            if (traceId == null) {
                traceId = "unknown";
            }
            log.debug("[Trace] [Security] [{}] SecurityContextHolder 中未找到认证信息", traceId);
            return null;
        }

        Object principal = authentication.getPrincipal();

        // 情况 1: UserDetails 类型
        if (principal instanceof UserDetails userDetails) {
            String traceId = TraceContext.getTraceId();
            if (traceId == null) {
                traceId = "unknown";
            }
            log.debug("[Trace] [Security] [{}] 从 UserDetails 中提取用户名：{}", traceId, userDetails.getUsername());
            return userDetails.getUsername();
        }

        // 情况 2: String 类型
        if (principal instanceof String str && !str.isEmpty()) {
            String traceId = TraceContext.getTraceId();
            if (traceId == null) {
                traceId = "unknown";
            }
            log.debug("[Trace] [Security] [{}] 从 String 中提取用户名：{}", traceId, str);
            return str;
        }

        // 情况 3: 其他类型
        String traceId = TraceContext.getTraceId();
        if (traceId == null) {
            traceId = "unknown";
        }
        log.debug("[Trace] [Security] [{}] 无法从 Principal 中提取用户名 - 类型：{}", traceId, principal.getClass().getSimpleName());
        return null;
    }

    /**
     * 从 SecurityContextHolder 中提取 Token
     *
     * <p>支持多种 Authentication 类型的 Token 提取：</p>
     * <ul>
     *     <li>JwtAuthenticationToken - 从 OAuth2 JWT Token 中提取</li>
     *     <li>UsernamePasswordAuthenticationToken - 从 credentials 中提取</li>
     *     <li>其他类型 - 尝试从 attributes 或 details 中提取</li>
     * </ul>
     *
     * @return Token 字符串，如果不存在返回 null
     */
    public static String extractTokenFromSecurityContext() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                String traceId = TraceContext.getTraceId();
                if (traceId == null) {
                    traceId = "unknown";
                }
                log.debug("[Trace] [Security] [{}] SecurityContextHolder 中未找到认证信息", traceId);
                return null;
            }

            // 情况 1: JwtAuthenticationToken（OAuth2 Resource Server 标准情况）
            if (authentication instanceof org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken jwtAuth) {
                String token = jwtAuth.getToken().getTokenValue();
                String traceId = TraceContext.getTraceId();
                if (traceId == null) {
                    traceId = "unknown";
                }
                log.debug("[Trace] [Security] [{}] 从 JwtAuthenticationToken 中提取 Token", traceId);
                return token;
            }

            // 情况 2: 尝试通过反射从任意 Authentication 的 attributes 中获取
            try {
                var getAttributesMethod = authentication.getClass().getMethod("getAttributes");
                @SuppressWarnings("unchecked")
                Map<String, Object> attributes = (Map<String, Object>) getAttributesMethod.invoke(authentication);
                Object tokenObj = attributes.get("token");
                if (tokenObj instanceof String) {
                    String token = (String) tokenObj;
                    String traceId = TraceContext.getTraceId();
                    if (traceId == null) {
                        traceId = "unknown";
                    }
                    log.debug("[Trace] [Security] [{}] 从 Authentication.attributes 中提取 Token", traceId);
                    return token;
                }
            } catch (NoSuchMethodException e) {
                // 不支持 getAttributes 方法，忽略
            }

            // 情况 3: 从 credentials 中获取（传统方式）
            Object credentials = authentication.getCredentials();
            if (credentials instanceof String) {
                String token = (String) credentials;
                String traceId = TraceContext.getTraceId();
                if (traceId == null) {
                    traceId = "unknown";
                }
                log.debug("[Trace] [Security] [{}] 从 credentials 中提取 Token", traceId);
                return token;
            }

            // 情况 4: 从 details 中获取
            if (authentication.getDetails() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> details = (Map<String, Object>) authentication.getDetails();
                Object tokenObj = details.get("token");
                if (tokenObj instanceof String) {
                    String token = (String) tokenObj;
                    String traceId = TraceContext.getTraceId();
                    if (traceId == null) {
                        traceId = "unknown";
                    }
                    log.debug("[Trace] [Security] [{}] 从 Authentication.details 中提取 Token", traceId);
                    return token;
                }
            }

            // 无法提取
            String traceId = TraceContext.getTraceId();
            if (traceId == null) {
                traceId = "unknown";
            }
            log.debug("[Trace] [Security] [{}] 无法从 Authentication 中提取 Token，类型：{}", traceId, authentication.getClass().getSimpleName());
            return null;
        } catch (Exception e) {
            String traceId = TraceContext.getTraceId();
            if (traceId == null) {
                traceId = "unknown";
            }
            log.error("[Trace] [Security] [{}] 提取 Token 时发生错误：{}", traceId, e.getMessage(), e);
            return null;
        }
    }

}
