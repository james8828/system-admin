package com.jnet.common.security.filter;

import com.jnet.common.security.utils.DynamicPermissionChecker;
import com.jnet.common.security.service.PermissionMetadataService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static com.jnet.common.constant.Constants.INTERNAL_SERVICE_AUTHENTICATED;
import static com.jnet.common.constant.Constants.TRACE_ID_HEADER;

/**
 * 动态 URL 权限过滤器
 * <p>
 * 在运行时根据 URL 和用户权限进行动态权限判断，实现灵活的访问控制。
 * 该过滤器会检查请求的 URL 是否需要特定权限，并验证当前用户是否拥有这些权限。
 * </p>
 *
 * <h3>工作流程：</h3>
 * <ol>
 *   <li>检查是否为匿名 URL（免认证）</li>
 *   <li>从 SecurityContext 获取用户认证信息</li>
 *   <li>提取用户的权限标识集合</li>
 *   <li>从 PermissionMetadataService 获取该 URL 需要的权限</li>
 *   <li>使用 DynamicPermissionChecker 进行权限匹配</li>
 *   <li>根据匹配结果放行或返回 403</li>
 * </ol>
 *
 * <h3>特性：</h3>
 * <ul>
 *   <li>支持 Ant 风格 URL 模式匹配（*, **, ?）</li>
 *   <li>支持通配符权限（如 system:*:list, *:*:*）</li>
 *   <li>智能过滤：只拦截配置了权限的 URL</li>
 *   <li>降级处理：组件缺失时默认放行</li>
 *   <li>完整的审计日志</li>
 * </ul>
 *
 * @author JNet Team
 * @version 1.0
 * @since 2024-01-01
 */
@Slf4j
@Component
public class DynamicUrlPermissionFilter extends OncePerRequestFilter {

    /**
     * 动态权限检查器（可选依赖）
     * 用于执行具体的权限匹配逻辑
     */
    private final DynamicPermissionChecker permissionChecker;

    /**
     * 权限元数据服务（可选依赖）
     * 用于管理 URL 与权限标识的映射关系
     */
    private final PermissionMetadataService metadataService;

    DynamicUrlPermissionFilter(
            DynamicPermissionChecker permissionChecker,
            PermissionMetadataService metadataService) {
        this.permissionChecker = permissionChecker;
        this.metadataService = metadataService;
    }

    /**
     * 核心过滤方法
     * <p>
     * 对每个请求执行动态 URL 权限检查：
     * 1. 检查是否为匿名 URL
     * 2. 获取用户认证信息和权限
     * 3. 获取 URL 需要的权限
     * 4. 执行权限匹配
     * 5. 根据结果放行或拒绝
     * </p>
     *
     * @param request     HTTP 请求对象
     * @param response    HTTP 响应对象
     * @param filterChain 过滤器链
     * @throws ServletException Servlet 异常
     * @throws IOException      IO 异常
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String url = request.getRequestURI();
        String method = request.getMethod();

        // 获取 TraceId（用于链路追踪）
        String traceId = request.getHeader(TRACE_ID_HEADER);
        if (traceId == null || traceId.isEmpty()) {
            traceId = "unknown";
        }

        log.debug("[Trace] [Security] [{}] 开始检查动态 URL 权限：{} {}", traceId, method, url);

        // ========== 步骤 1: 检查匿名 URL ==========
        if (metadataService != null && metadataService.isAnonymousUrl(url)) {
            log.debug("[Trace] [Security] [{}] URL 为匿名访问，直接放行：{}", traceId, url);
            filterChain.doFilter(request, response);
            return;
        }

        // ========== 步骤 2: 获取用户认证信息 ==========
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 如果没有认证信息，跳过检查（由其他过滤器处理）
        if (authentication == null || !authentication.isAuthenticated()) {
            log.debug("[Trace] [Security] [{}] 未找到用户认证信息，跳过权限检查", traceId);
            filterChain.doFilter(request, response);
            return;
        }

        // 🔥 关键修复：检查是否是内部服务认证，如果是则跳过 URL 权限验证
        Boolean isInternalServiceAuthenticated = (Boolean) request.getAttribute(INTERNAL_SERVICE_AUTHENTICATED);
        if (Boolean.TRUE.equals(isInternalServiceAuthenticated)) {
            log.debug("[Trace] [Security] [{}] 内部服务已认证，跳过 URL 权限验证：{}", traceId, url);
            filterChain.doFilter(request, response);
            return;
        }

        // ========== 步骤 3: 获取用户权限列表 ==========
        Set<String> userPermissions = null;
        if (metadataService != null) {
            userPermissions = metadataService.getPermissions();
            log.debug("[Trace] [Security] [{}] 用户权限集合：{}", traceId, userPermissions);
        }

        // 如果无法获取用户权限，使用认证信息中的权限
        if (userPermissions == null || userPermissions.isEmpty()) {
            userPermissions = new HashSet<>();
            for (GrantedAuthority authority : authentication.getAuthorities()) {
                userPermissions.add(authority.getAuthority());
            }
            log.debug("[Trace] [Security] [{}] 无法从服务获取权限，降级使用认证机构中的权限：{}", traceId, userPermissions);
        }

        // ========== 步骤 4: 执行权限匹配 ==========
        if (permissionChecker != null) {
            // 🔥 关键修复：如果用户权限为空，直接拒绝
            if (userPermissions.isEmpty()) {
                log.warn("[Trace] [Security] [{}] 用户无任何权限，拒绝访问：{}", traceId, url);
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":403,\"message\":\"无权限访问\"}");
                return;
            }

            // 检查 URL 是否在用户权限列表中
            boolean hasPermission = permissionChecker.hasPermission(url, userPermissions);

            if (hasPermission) {
                log.debug("[Trace] [Security] [{}] 权限检查通过，允许访问：{} - 用户权限：{}", traceId, url, userPermissions);
                filterChain.doFilter(request, response);
            } else {
                // 权限不足，返回 403
                log.warn("[Trace] [Security] [{}] 权限不足，拒绝访问：{} - 用户权限：{}", traceId, url, userPermissions);
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":403,\"message\":\"无权限访问\"}");
            }
        } else {
            // 没有权限检查器，放行（降级处理）
            log.warn("[Trace] [Security] [{}] 动态权限检查器不可用，降级处理默认放行", traceId);
            filterChain.doFilter(request, response);
        }
    }
}
