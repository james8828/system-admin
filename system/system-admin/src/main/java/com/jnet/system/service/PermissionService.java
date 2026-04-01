package com.jnet.system.service;

import com.jnet.common.result.PageQuery;
import com.jnet.common.result.PageResult;
import com.jnet.system.dto.PermissionDTO;
import com.jnet.system.dto.PermissionCheckResult;
import com.jnet.system.entity.SysMenu;

import java.util.List;
import java.util.Set;

/**
 * 权限管理服务接口
 */
public interface PermissionService {

    /**
     * 获取用户的权限标识集合
     *
     * @param userId 用户 ID
     * @return 权限标识集合
     */
    Set<String> getUserPermissions(Long userId);

    /**
     * 获取角色的权限标识集合
     *
     * @param roleId 角色 ID
     * @return 权限标识集合
     */
    Set<String> getRolePermissions(Long roleId);

    /**
     * 检查用户是否有指定权限
     *
     * @param userId 用户 ID
     * @param perms  权限标识
     * @return true=有权限 false=无权限
     */
    Boolean checkUserPermission(Long userId, String perms);

    /**
     * 批量检查用户权限
     *
     * @param userId 用户 ID
     * @param perms  权限标识列表
     * @return 权限校验结果列表
     */
    List<PermissionCheckResult> batchCheckUserPermissions(Long userId, List<String> perms);

    /**
     * 获取用户的菜单权限树
     *
     * @param userId 用户 ID
     * @return 菜单权限树
     */
    List<PermissionDTO> getUserMenuTree(Long userId);

    /**
     * 获取角色的菜单权限列表
     *
     * @param roleId 角色 ID
     * @return 菜单权限列表
     */
    List<SysMenu> getRoleMenus(Long roleId);

    /**
     * 获取角色的权限标识列表
     *
     * @param roleId 角色 ID
     * @return 权限标识列表
     */
    List<String> getRolePermsList(Long roleId);

    /**
     * 分页查询权限列表
     *
     * @param pageQuery 分页参数
     * @param permission  权限查询条件
     * @return 权限分页数据
     */
    PageResult<PermissionDTO> pagePermissions(PageQuery pageQuery, PermissionDTO permission);

    /**
     * 根据 ID 获取权限详情
     *
     * @param menuId 菜单 ID
     * @return 权限详情
     */
    PermissionDTO getPermissionById(Long menuId);

    /**
     * 创建权限
     *
     * @param permission 权限信息
     * @return 是否成功
     */
    Boolean addPermission(PermissionDTO permission);

    /**
     * 更新权限
     *
     * @param permission 权限信息
     * @return 是否成功
     */
    Boolean updatePermission(PermissionDTO permission);

    /**
     * 删除权限
     *
     * @param menuId 菜单 ID
     * @return 是否成功
     */
    Boolean deletePermission(Long menuId);

    /**
     * 批量删除权限
     *
     * @param menuIds 菜单 ID 列表
     * @return 是否成功
     */
    Boolean batchDeletePermissions(List<Long> menuIds);

    /**
     * 为角色分配菜单权限
     *
     * @param roleId  角色 ID
     * @param menuIds 菜单 ID 列表
     * @return 是否成功
     */
    Boolean assignMenusToRole(Long roleId, List<Long> menuIds);

    /**
     * 获取所有权限标识（用于缓存刷新）
     *
     * @return 权限标识集合
     */
    Set<String> getAllPermissions();

    /**
     * 刷新用户权限缓存
     *
     * @param userId 用户 ID
     */
    void refreshUserPermissions(Long userId);

    /**
     * 清除角色权限缓存
     *
     * @param roleId 角色 ID
     */
    void clearRolePermissionsCache(Long roleId);

    /**
     * 根据用户名查询用户信息
     *
     * @param username 用户名
     * @return 用户 DTO
     */
    com.jnet.system.api.dto.SysUserDTO getUserByUsername(String username);

    /**
     * 获取用户的角色列表
     *
     * @param userId 用户 ID
     * @return 角色标识列表
     */
    java.util.List<String> getUserRoles(Long userId);

    /**
     * 根据用户名获取用户权限信息（包含角色和权限）
     *
     * @param username 用户名
     * @return 用户权限 DTO
     */
    com.jnet.system.api.dto.UserPermissionDTO getUserPermissionInfo(String username);
}
