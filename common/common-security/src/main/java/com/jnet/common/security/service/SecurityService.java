// 在 common-security 中扩展 AuthorityUtils
package com.jnet.common.security.service;

import com.jnet.common.security.utils.SecurityContextUtils;
import com.jnet.common.security.utils.DynamicPermissionChecker;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
@Import({DynamicPermissionChecker.class})
@Component("ss")  // 这样就可以在@PreAuthorize 中使用@ss
public class SecurityService {
    
    @Resource
    private DynamicPermissionChecker dynamicPermissionChecker;
    
    @Resource
    private PermissionMetadataService permissionMetadataService;
    

    
    public boolean hasPermi(String permission) {
        return SecurityContextUtils.hasPermission(permission) || isInternalService();
    }
    
    /**
     * 动态 URL 权限检查
     * @param requiredPermissions 需要的权限标识（多个用逗号分隔）
     * @return true-有权限，false-无权限
     */
    public boolean hasUrlPermission(String requiredPermissions) {
        if (isInternalService()) {
            return true;
        }
        
        ServletRequestAttributes attributes = 
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return false;
        }
        
        HttpServletRequest request = attributes.getRequest();
        String url = request.getRequestURI();
        String method = request.getMethod();
        
        // 解析需要的权限
        Set<String> requiredPermSet = parsePermissions(requiredPermissions);
        
        // 获取用户权限（从 PermissionMetadataService）
        Set<String> userPermSet = null;
        if (permissionMetadataService != null) {
            userPermSet = permissionMetadataService.getPermissions();
        }
        
        // 降级处理：使用认证机构中的权限
        if (userPermSet == null || userPermSet.isEmpty()) {
            userPermSet = new HashSet<>(SecurityContextUtils.getAuthorities());
        }
        
        // 使用动态权限检查器
        if (dynamicPermissionChecker != null) {
            // 检查是否有通配符权限
            if (userPermSet.contains("*:*:*")) {
                return true;
            }
            
            // 逐个检查所需权限
            for (String requiredPerm : requiredPermSet) {
                if (userPermSet.contains(requiredPerm)) {
                    return true;
                }
            }
            
            return false;
        }
        
        // 降级处理：简单匹配
        for (String perm : requiredPermSet) {
            if (userPermSet.contains(perm)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 根据 URL 自动判断权限
     * 从 PermissionMetadataService 中获取该 URL 需要的权限
     */
    public boolean checkUrlPermission() {
        if (permissionMetadataService == null) {
            return true;
        }
        
        ServletRequestAttributes attributes = 
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return true;
        }
        
        HttpServletRequest request = attributes.getRequest();
        String url = request.getRequestURI();
        
        // 检查是否匿名 URL
        if (permissionMetadataService.isAnonymousUrl(url)) {
            return true;
        }
        
        // 获取用户权限（已从 PermissionMetadataService 获取所有权限）
        Set<String> userPermissions = permissionMetadataService.getPermissions();
        
        // 如果没有权限，直接拒绝
        if (userPermissions == null || userPermissions.isEmpty()) {
            return false;
        }
        
        // 使用动态权限检查器检查 URL 是否在权限列表中
        if (dynamicPermissionChecker != null) {
            return dynamicPermissionChecker.hasPermission(url, userPermissions);
        }
        
        // 降级处理：检查 URL 是否直接存在于权限列表中
        return userPermissions.contains(url);
    }
    
    public boolean hasRole(String role) {
        List<String> authorities = SecurityContextUtils.getAuthorities();
        return authorities.contains("ROLE_" + role) || 
               authorities.contains(role) || 
               isInternalService();
    }
    
    private boolean isInternalService() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            // 检查是否是内部服务账户（兼容多种命名格式）
            String authName = authentication.getName();
            return authName != null && (
                authName.startsWith("internal-service-") ||
                authentication.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().startsWith("INTERNAL_"))
            );
        }
        return false;
    }
    
    /**
     * 解析权限字符串为集合
     */
    private Set<String> parsePermissions(String permissions) {
        Set<String> permSet = new HashSet<>();
        if (permissions != null && !permissions.isEmpty()) {
            String[] perms = permissions.split(",");
            for (String perm : perms) {
                permSet.add(perm.trim());
            }
        }
        return permSet;
    }
}
