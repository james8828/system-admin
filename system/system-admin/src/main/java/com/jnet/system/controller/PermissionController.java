package com.jnet.system.controller;

import com.jnet.common.result.PageQuery;
import com.jnet.common.result.PageResult;
import com.jnet.common.result.Result;
import com.jnet.common.result.ResultCode;
import com.jnet.system.api.dto.UserPermissionDTO;
import com.jnet.system.dto.PermissionCheckResult;
import com.jnet.system.dto.PermissionDTO;
import com.jnet.system.service.PermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 权限管理统一 API
 */
@Tag(name = "权限管理")
@RestController
@RequestMapping("/api/system/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @Operation(summary = "获取用户权限标识集合")
    @PreAuthorize("@ss.hasPermi('system:permission:query')")
    @GetMapping("/user/{userId}")
    public Result<List<String>> getUserPermissions(@PathVariable Long userId) {
        return Result.success(permissionService.getUserPermissions(userId).stream().toList());
    }

    @Operation(summary = "检查用户权限")
    @PreAuthorize("@ss.hasPermi('system:permission:check')")
    @GetMapping("/user/{userId}/check")
    public Result<Boolean> checkPermission(@PathVariable Long userId, @RequestParam String perms) {
        return Result.success(permissionService.checkUserPermission(userId, perms));
    }

    @Operation(summary = "批量检查用户权限")
    @PreAuthorize("@ss.hasPermi('system:permission:check')")
    @PostMapping("/user/{userId}/batch-check")
    public Result<List<PermissionCheckResult>> batchCheckPermissions(
            @PathVariable Long userId,
            @RequestBody List<String> perms) {
        return Result.success(permissionService.batchCheckUserPermissions(userId, perms));
    }

    @Operation(summary = "获取用户菜单权限树")
    @PreAuthorize("@ss.hasPermi('system:permission:query')")
    @GetMapping("/user/{userId}/menu-tree")
    public Result<List<PermissionDTO>> getUserMenuTree(@PathVariable Long userId) {
        return Result.success(permissionService.getUserMenuTree(userId));
    }

    @Operation(summary = "分页查询权限列表")
    @PreAuthorize("@ss.hasPermi('system:permission:list')")
    @GetMapping("/page")
    public Result<PageResult<PermissionDTO>> pagePermissions(PageQuery pageQuery, PermissionDTO permission) {
        return Result.success(permissionService.pagePermissions(pageQuery, permission));
    }

    @Operation(summary = "获取权限详情")
    @PreAuthorize("@ss.hasPermi('system:permission:query')")
    @GetMapping("/{menuId}")
    public Result<PermissionDTO> getPermissionById(@PathVariable Long menuId) {
        return Result.success(permissionService.getPermissionById(menuId));
    }

    @Operation(summary = "创建权限")
    @PreAuthorize("@ss.hasPermi('system:permission:add')")
    @PostMapping
    public Result<Void> addPermission(@RequestBody PermissionDTO permission) {
        permissionService.addPermission(permission);
        return Result.success();
    }

    @Operation(summary = "更新权限")
    @PreAuthorize("@ss.hasPermi('system:permission:update')")
    @PutMapping("/{menuId}")
    public Result<Void> updatePermission(@PathVariable Long menuId, @RequestBody PermissionDTO permission) {
        permission.setMenuId(menuId);
        permissionService.updatePermission(permission);
        return Result.success();
    }

    @Operation(summary = "删除权限")
    @PreAuthorize("@ss.hasPermi('system:permission:remove')")
    @DeleteMapping("/{menuId}")
    public Result<Void> deletePermission(@PathVariable Long menuId) {
        permissionService.deletePermission(menuId);
        return Result.success();
    }

    @Operation(summary = "批量删除权限")
    @PreAuthorize("@ss.hasPermi('system:permission:batchDelete')")
    @DeleteMapping("/batch")
    public Result<Void> batchDeletePermissions(@RequestBody List<Long> menuIds) {
        permissionService.batchDeletePermissions(menuIds);
        return Result.success();
    }

    @Operation(summary = "为角色分配菜单权限")
    @PreAuthorize("@ss.hasPermi('system:permission:assign')")
    @PostMapping("/role/{roleId}/menus")
    public Result<Void> assignMenusToRole(
            @PathVariable Long roleId,
            @RequestBody List<Long> menuIds) {
        permissionService.assignMenusToRole(roleId, menuIds);
        return Result.success();
    }

    @Operation(summary = "获取角色的权限标识列表")
    @PreAuthorize("@ss.hasPermi('system:permission:query')")
    @GetMapping("/role/{roleId}/perms")
    public Result<List<String>> getRolePermissions(@PathVariable Long roleId) {
        return Result.success(permissionService.getRolePermsList(roleId));
    }

    @Operation(summary = "刷新用户权限缓存")
    @PreAuthorize("@ss.hasPermi('system:permission:refresh')")
    @PostMapping("/refresh/user/{userId}")
    public Result<Void> refreshUserPermissions(@PathVariable Long userId) {
        permissionService.refreshUserPermissions(userId);
        return Result.success();
    }

    @Operation(summary = "清除角色权限缓存")
    @PreAuthorize("@ss.hasPermi('system:permission:clear')")
    @PostMapping("/clear/role/{roleId}")
    public Result<Void> clearRolePermissionsCache(@PathVariable Long roleId) {
        permissionService.clearRolePermissionsCache(roleId);
        return Result.success();
    }

    @Operation(summary = "根据用户名获取用户权限标识集合")
    @GetMapping
    public Result<UserPermissionDTO> getPermissionsByCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        // 直接调用服务层方法获取用户权限信息
        UserPermissionDTO userPermissionDTO = permissionService.getUserPermissionInfo(username);
        if (userPermissionDTO == null) {
            return Result.error(400, "用户不存在");
        }
        
        return Result.success(userPermissionDTO);
    }
}
