package com.jnet.common.security.service;

import com.jnet.common.result.Result;
import com.jnet.common.security.cache.PermissionCacheManager;
import com.jnet.common.trace.TraceContext;
import com.jnet.system.api.client.UserPermissionFeignClient;
import com.jnet.system.api.dto.UserPermissionDTO;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 权限元数据服务（Servlet 专用版）
 * <p>
 * 核心功能：
 * </p>
 * <ul>
 *   <li>基于 JWT Token 动态获取用户权限</li>
 *   <li>本地缓存 + Feign 远程调用结合</li>
 *   <li>URL 权限匹配与验证</li>
 *   <li>匿名 URL 配置</li>
 * </ul>
 *
 * @author JNet Team
 * @version 6.0 (Using PermissionCacheManager)
 * @since 2024-01-01
 */
@Component
@Slf4j
public class PermissionMetadataService {

    /**
     * URL 权限映射表（Key: URL 模式，Value: 需要的权限标识集合）
     */
    private final Map<String, Set<String>> urlPermissionMap = new ConcurrentHashMap<>();

    /**
     * 免认证 URL 列表
     */
    private final Set<String> anonymousUrls = ConcurrentHashMap.newKeySet();

    /**
     * 路径匹配器（使用 Spring 官方实现）
     */
    private final PathMatcher pathMatcher = new AntPathMatcher();

    /**
     * 缓存管理器（统一管理缓存）
     */
    @Resource
    private PermissionCacheManager cacheManager;

    /**
     * 用户权限 Feign 客户端
     */
    @Resource
    private UserPermissionFeignClient userPermissionFeignClient;

    /**
     * 初始化方法
     */
    @PostConstruct
    public void init() {
        // 加载默认匿名 URL
        loadDefaultAnonymousUrls();

        log.info("PermissionMetadataService 初始化完成");
    }

    /**
     * 销毁方法
     */
    @PreDestroy
    public void destroy() {
        if (cacheManager != null) {
            cacheManager.invalidateAll();
        }
        log.info("PermissionMetadataService 已销毁");
    }

    /**
     * 获取当前用户对指定 URL 的权限集合
     *
     * @return 用户权限集合（可能为空）
     */
    public Set<String> getPermissions() {
        // 步骤 1: 从缓存管理器获取或加载用户权限（使用 JWT Token 作为缓存键）
        UserPermissionDTO userPermissionDTO = getOrLoadUserPermission();
        return buildCompletePermissions(userPermissionDTO);
    }

    /**
     * 构建完整的权限集合
     *
     * @param dto 用户权限 DTO
     * @return 完整权限集合
     */
    private Set<String> buildCompletePermissions(UserPermissionDTO dto) {
        Set<String> permissions = new HashSet<>();

        // 添加角色权限
        if (dto.getRoles() != null) {
            dto.getRoles().forEach(role -> permissions.add("ROLE_" + role));
        }

        // 添加操作权限标识
        if (dto.getPermissions() != null) {
            permissions.addAll(dto.getPermissions());
        }

        // 添加 URL 路径权限（用于网关匹配）
        if (dto.getPaths() != null) {
            permissions.addAll(dto.getPaths());
        }

        // 如果是超级管理员，添加通配符权限
        if (dto.isAdmin()) {
            permissions.add("*:*:*");
        }

        return permissions;
    }
    

    /**
     * 添加 URL 权限映射
     */
    public void addUrlPermission(String urlPattern, String permission) {
        urlPermissionMap.computeIfAbsent(urlPattern, k -> ConcurrentHashMap.newKeySet()).add(permission);
        log.debug("已添加 URL 权限：{} -> {}", urlPattern, permission);
    }

    /**
     * 批量添加 URL 权限映射
     */
    public void addUrlPermissions(String urlPattern, Collection<String> permissions) {
        urlPermissionMap.computeIfAbsent(urlPattern, k -> ConcurrentHashMap.newKeySet()).addAll(permissions);
        log.debug("已批量添加 URL 权限：{} -> {}", urlPattern, permissions);
    }

    /**
     * 移除 URL 权限映射
     */
    public void removeUrlPermission(String urlPattern) {
        urlPermissionMap.remove(urlPattern);
        log.debug("已移除 URL 权限映射：{}", urlPattern);
    }

    /**
     * 添加匿名访问 URL
     */
    public void addAnonymousUrl(String urlPattern) {
        anonymousUrls.add(urlPattern);
        log.debug("已添加匿名 URL: {}", urlPattern);
    }

    /**
     * 检查 URL 是否允许匿名访问
     */
    public boolean isAnonymousUrl(String url) {
        for (String pattern : anonymousUrls) {
            if (pathMatcher.match(pattern, url)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 清空所有配置和缓存
     */
    public void clear() {
        urlPermissionMap.clear();
        anonymousUrls.clear();
        if (cacheManager != null) {
            cacheManager.invalidateAll();
        }
        log.info("已清空所有权限配置和缓存");
    }

    /**
     * 获取所有 URL 权限映射
     */
    public Map<String, Set<String>> getAllUrlPermissions() {
        return new HashMap<>(urlPermissionMap);
    }

    /**
     * 获取或加载用户权限（使用缓存管理器）
     * <p>
     * 优先从 JWT Token claims 中获取 userId
     * 如果找不到，再使用用户名作为降级方案
     * </p>
     *
     * @return 用户权限 DTO
     */
    public UserPermissionDTO getOrLoadUserPermission() {
        // 步骤 0: 获取 TraceId（用于链路追踪）
        String traceId = TraceContext.getTraceId();
        if (traceId == null) {
            traceId = "unknown";
        }
        
        // 步骤 1: 尝试从 JWT Token claims 中获取 userId
        Long userId = extractUserIdFromToken();
        String username = null;
        
        if (userId != null) {
            log.debug("[Trace] [Security] [{}] 从 Token claims 中获取到 userId={}", traceId, userId);
            // 使用 userId 作为缓存键
            String cacheKey = "user:id:" + userId;
            
            // 尝试从缓存获取
            UserPermissionDTO cached = cacheManager.get(cacheKey);
            if (cached != null) {
                log.debug("[Trace] [Security] [{}] 缓存命中 - userId={}, 角色数={}, 权限数={}, 路径数={}",
                        traceId, userId, cached.getRoles().size(), cached.getPermissions().size(), cached.getPaths().size());
                return cached;
            }
            
            // 通过 Feign 按 userId 查询
            try {
                Result<UserPermissionDTO> result = userPermissionFeignClient.getPermissionsByCurrentUser();
                if (result != null && result.isSuccess() && result.getData() != null) {
                    UserPermissionDTO dto = result.getData();
                    cacheManager.put(cacheKey, dto);
                    log.info("[Trace] [Security] [{}] 已缓存用户权限 - userId={}, 角色数={}, 权限数={}, 路径数={}",
                            traceId, userId, dto.getRoles().size(), dto.getPermissions().size(), dto.getPaths().size());
                    return dto;
                } else {
                    log.warn("[Trace] [Security] [{}] Feign 调用失败 - userId={}, 代码={}, 消息={}",
                            traceId, userId, result != null ? result.getCode() : "null",
                            result != null ? result.getMessage() : "null");
                }
            } catch (Exception e) {
                log.error("[Trace] [Security] [{}] 按 userId 获取用户权限出错 - userId={}", traceId, userId, e);
            }
        }
        
        // 步骤 2: 如果没有 userId，使用用户名作为降级方案
        username = extractUsernameFromSecurityContext();
        if (username == null || username.isEmpty()) {
            log.debug("[Trace] [Security] [{}] 未找到认证用户，无法获取权限", traceId);
            return null;
        }
        
        log.info("[Trace] [Security] [{}] 降级使用用户名查询 - username={}", traceId, username);
        String cacheKey = "user:" + username;
        
        // 尝试从缓存获取
        UserPermissionDTO cached = cacheManager.get(cacheKey);
        if (cached != null) {
            log.debug("[Trace] [Security] [{}] 缓存命中 - 用户={}, 角色数={}, 权限数={}, 路径数={}",
                    traceId, username, cached.getRoles().size(), cached.getPermissions().size(), cached.getPaths().size());
            return cached;
        }
        
        // 通过 Feign 按用户名查询
        try {
            Result<UserPermissionDTO> result = userPermissionFeignClient.getPermissionsByCurrentUser();
            if (result != null && result.isSuccess() && result.getData() != null) {
                UserPermissionDTO dto = result.getData();
                cacheManager.put(cacheKey, dto);
                log.info("[Trace] [Security] [{}] 已缓存用户权限 - 用户={}, 角色数={}, 权限数={}, 路径数={}",
                        traceId, username, dto.getRoles().size(), dto.getPermissions().size(), dto.getPaths().size());
                return dto;
            } else {
                log.warn("[Trace] [Security] [{}] Feign 调用失败 - 用户={}, 代码={}, 消息={}",
                        traceId, username, result != null ? result.getCode() : "null",
                        result != null ? result.getMessage() : "null");
            }
        } catch (Exception e) {
            log.error("[Trace] [Security] [{}] 获取用户权限出错 - 用户={}", traceId, username, e);
        }
        
        return null;
    }
    
    /**
     * 从 JWT Token claims 中提取 userId
     * 优先使用此方法，因为 Token 中已包含 userId
     *
     * @return userId，如果不存在返回 null
     */
    private Long extractUserIdFromToken() {
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
    private String extractUsernameFromSecurityContext() {
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
     * 加载默认匿名 URL
     */
    private void loadDefaultAnonymousUrls() {
        List<String> defaultUrls = Arrays.asList(
            "/api/public/**",
            "/api/actuator/**",
            "/api-docs/**",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/error",
            "/login/**",
            "/oauth2/**",
            "/oauth2/token",
            "/oauth2/authorize"
        );

        defaultUrls.forEach(this::addAnonymousUrl);
        log.debug("已加载 {} 个默认匿名 URL", defaultUrls.size());
    }
}
