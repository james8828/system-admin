package com.jnet.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jnet.common.exception.ServiceException;
import com.jnet.common.result.PageQuery;
import com.jnet.common.result.PageResult;
import com.jnet.system.api.dto.SysUserDTO;
import com.jnet.system.api.dto.UserPermissionDTO;
import com.jnet.system.dto.PermissionCheckResult;
import com.jnet.system.dto.PermissionDTO;
import com.jnet.system.entity.SysMenu;
import com.jnet.system.mapper.SysMenuMapper;
import com.jnet.system.service.PermissionService;
import com.jnet.system.service.SysUserService;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 权限管理服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionServiceImpl extends ServiceImpl<SysMenuMapper, SysMenu> implements PermissionService {

    @Resource
    private SysMenuMapper sysMenuMapper;

    @Resource
    private SysUserService sysUserService;

    @Override
    public Set<String> getUserPermissions(Long userId) {
        // 从 Mapper 获取用户的权限标识列表
        List<String> perms = sysMenuMapper.selectPermsByUserId(userId);
        return new HashSet<>(perms);
    }

    @Override
    public Set<String> getRolePermissions(Long roleId) {
        List<SysMenu> menus = sysMenuMapper.selectMenusByRoleId(roleId);
        return menus.stream()
                .map(SysMenu::getPerms)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    @Override
    public Boolean checkUserPermission(Long userId, String perms) {
        Set<String> userPermissions = getUserPermissions(userId);
        // 检查是否包含超级管理员权限
        if (userPermissions.contains("*:*:*")) {
            return true;
        }
        return userPermissions.contains(perms);
    }

    @Override
    public List<PermissionCheckResult> batchCheckUserPermissions(Long userId, List<String> perms) {
        Set<String> userPermissions = getUserPermissions(userId);
        boolean isAdmin = userPermissions.contains("*:*:*");

        return perms.stream()
                .map(p -> {
                    boolean hasPermission = isAdmin || userPermissions.contains(p);
                    PermissionCheckResult result = new PermissionCheckResult();
                    result.setPerms(p);
                    result.setHasPermission(hasPermission);
                    if (!hasPermission) {
                        result.setDenyReason("用户没有权限：" + p);
                    }
                    return result;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<PermissionDTO> getUserMenuTree(Long userId) {
        // 获取用户菜单列表
        List<SysMenu> menus = sysMenuMapper.selectMenusByUserId(userId);
        return buildMenuTree(menus);
    }

    @Override
    public List<SysMenu> getRoleMenus(Long roleId) {
        return sysMenuMapper.selectMenusByRoleId(roleId);
    }

    @Override
    public List<String> getRolePermsList(Long roleId) {
        List<SysMenu> menus = getRoleMenus(roleId);
        return menus.stream()
                .map(SysMenu::getPerms)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public PageResult<PermissionDTO> pagePermissions(PageQuery pageQuery, PermissionDTO permission) {
        Page<SysMenu> page = new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize());
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
        
        // 构建查询条件
        wrapper.like(permission.getMenuName() != null, SysMenu::getMenuName, permission.getMenuName());
        wrapper.eq(permission.getType() != null, SysMenu::getType, permission.getType());
        wrapper.eq(permission.getParentId() != null, SysMenu::getParentId, permission.getParentId());
        wrapper.like(permission.getPerms() != null, SysMenu::getPerms, permission.getPerms());
        wrapper.orderByAsc(SysMenu::getOrderNum);
        
        IPage<SysMenu> result = page(page, wrapper);
        
        // 转换为 DTO
        List<PermissionDTO> dtoList = result.getRecords().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        return PageResult.of(dtoList, result.getTotal(), pageQuery.getPageSize(), pageQuery.getPageNum());
    }

    @Override
    public PermissionDTO getPermissionById(Long menuId) {
        SysMenu menu = getById(menuId);
        if (menu == null) {
            throw new ServiceException("权限不存在");
        }
        return convertToDTO(menu);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean addPermission(PermissionDTO permission) {
        // 检查权限标识是否已存在
        if (permission.getPerms() != null) {
            LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysMenu::getPerms, permission.getPerms());
            Long count = count(wrapper);
            if (count > 0) {
                throw new ServiceException("权限标识已存在");
            }
        }

        SysMenu menu = convertToEntity(permission);
        menu.setDelFlag(false);
        return save(menu);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean updatePermission(PermissionDTO permission) {
        if (permission.getMenuId() == null) {
            throw new ServiceException("权限 ID 不能为空");
        }

        // 检查权限标识是否与其他记录重复
        if (permission.getPerms() != null) {
            LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysMenu::getPerms, permission.getPerms())
                   .ne(SysMenu::getMenuId, permission.getMenuId());
            Long count = count(wrapper);
            if (count > 0) {
                throw new ServiceException("权限标识已存在");
            }
        }

        SysMenu menu = convertToEntity(permission);
        return updateById(menu);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean deletePermission(Long menuId) {
        // 检查是否有子菜单
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysMenu::getParentId, menuId);
        Long count = count(wrapper);
        if (count > 0) {
            throw new ServiceException("存在子菜单，无法删除");
        }

        return removeById(menuId);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean batchDeletePermissions(List<Long> menuIds) {
        for (Long menuId : menuIds) {
            deletePermission(menuId);
        }
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean assignMenusToRole(Long roleId, List<Long> menuIds) {
        // TODO: 需要实现角色菜单关联表的保存逻辑
        // 这里需要先删除原有的角色菜单关联，再插入新的关联
        log.info("为角色 {} 分配菜单：{}", roleId, menuIds);
        return true;
    }

    @Override
    public Set<String> getAllPermissions() {
        List<SysMenu> menus = list();
        return menus.stream()
                .map(SysMenu::getPerms)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    @Override
    public void refreshUserPermissions(Long userId) {
        // TODO: 刷新 Redis 缓存中的用户权限
        log.info("刷新用户 {} 的权限缓存", userId);
    }

    @Override
    public void clearRolePermissionsCache(Long roleId) {
        // TODO: 清除 Redis 缓存中的角色权限
        log.info("清除角色 {} 的权限缓存", roleId);
    }

    @Override
    public SysUserDTO getUserByUsername(String username) {
        if (sysUserService == null) {
            log.warn("SysUserService not available, returning null for username: {}", username);
            return null;
        }
        try {
            // 调用 SysUserService 查询用户实体
            com.jnet.system.entity.SysUser user = sysUserService.getUserByUsername(username);
            if (user == null) {
                return null;
            }
            
            // 将实体转换为 DTO
            com.jnet.system.api.dto.SysUserDTO dto = new com.jnet.system.api.dto.SysUserDTO();
            dto.setUserId(user.getUserId());
            dto.setUserName(user.getUserName());
            dto.setNickName(user.getNickName());
            dto.setEmail(user.getEmail());
            dto.setMobile(user.getMobile());
            dto.setSex(user.getSex());
            dto.setEnabled(user.getEnabled());
            dto.setDeptId(user.getDeptId());
            dto.setCreateTime(user.getCreateTime());
            dto.setUpdateTime(user.getUpdateTime());
            
            return dto;
        } catch (Exception e) {
            log.error("Error getting user by username: {}", username, e);
            return null;
        }
    }

    @Override
    public List<String> getUserRoles(Long userId) {
        // TODO: 需要从用户 - 角色关联表中查询用户的角色列表
        // 暂时返回空列表，后续需要实现
        log.debug("Getting roles for user: {}", userId);
        return new ArrayList<>();
    }

    /**
     * 获取用户的权限路径集合（用于网关 URL 匹配）
     * <p>
     * 从用户的菜单权限中提取所有接口类型的 path 路径
     * </p>
     *
     * @param userId 用户 ID
     * @return 权限路径集合
     */
    private Set<String> getUserPermissionPaths(Long userId) {
        // 获取用户的所有菜单权限
        List<SysMenu> menus = sysMenuMapper.selectMenusByUserId(userId);
        
        // 提取所有接口类型（type=3）的 path 路径
        return menus.stream()
                .filter(menu -> menu.getType() == 3) // 只过滤接口类型的权限
                .map(SysMenu::getPath)
                .filter(Objects::nonNull)
                .filter(path -> !path.isEmpty())
                .collect(Collectors.toSet());
    }

    /**
     * 根据用户名获取用户权限信息（包含角色和权限）
     *
     * @param username 用户名
     * @return 用户权限 DTO
     */
    @Override
    public UserPermissionDTO getUserPermissionInfo(String username) {
        // 先根据用户名查询用户 ID
        SysUserDTO user = getUserByUsername(username);
        if (user == null || user.getUserId() == null) {
            log.warn("User not found by username: {}", username);
            return null;
        }
        
        // 构建用户权限 DTO
        UserPermissionDTO userPermissionDTO = new UserPermissionDTO();
        userPermissionDTO.setUserId(user.getUserId());
        userPermissionDTO.setUsername(user.getUserName());
        userPermissionDTO.setNickname(user.getNickName());
        
        // 获取用户权限标识
        Set<String> permissions = getUserPermissions(user.getUserId());
        userPermissionDTO.setPermissions(permissions);
        
        // 获取用户角色
        List<String> roles = getUserRoles(user.getUserId());
        userPermissionDTO.setRoles(new HashSet<>(roles));
        
        // 获取用户权限路径（用于网关 URL 匹配）
        Set<String> paths = getUserPermissionPaths(user.getUserId());
        userPermissionDTO.setPaths(paths);
        
        // 判断是否超级管理员
        userPermissionDTO.setAdmin(permissions.contains("*:*:*"));
        
        return userPermissionDTO;
    }

    /**
     * 构建菜单树
     */
    private List<PermissionDTO> buildMenuTree(List<SysMenu> menus) {
        Map<Long, SysMenu> menuMap = menus.stream()
                .collect(Collectors.toMap(SysMenu::getMenuId, m -> m));

        List<SysMenu> tree = new ArrayList<>();
        for (SysMenu menu : menus) {
            if (menu.getParentId() == 0) {
                tree.add(menu);
            } else {
                SysMenu parent = menuMap.get(menu.getParentId());
                if (parent != null) {
                    if (parent.getChildren() == null) {
                        parent.setChildren(new ArrayList<>());
                    }
                    parent.getChildren().add(menu);
                }
            }
        }

        return tree.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Entity 转 DTO
     */
    private PermissionDTO convertToDTO(SysMenu menu) {
        PermissionDTO dto = new PermissionDTO();
        dto.setMenuId(menu.getMenuId());
        dto.setPerms(menu.getPerms());
        dto.setPermsName(menu.getMenuName());
        dto.setType(menu.getType());
        dto.setParentId(menu.getParentId());
        dto.setPath(menu.getPath());
        dto.setComponent(menu.getComponent());
        dto.setVisible(menu.getVisible());
        dto.setEnabled(menu.getEnabled());
        dto.setIcon(menu.getIcon());
        dto.setOrderNum(menu.getOrderNum());
        
        if (menu.getChildren() != null && !menu.getChildren().isEmpty()) {
            dto.setChildren(menu.getChildren().stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList()));
        }
        
        return dto;
    }

    /**
     * DTO 转 Entity
     */
    private SysMenu convertToEntity(PermissionDTO dto) {
        SysMenu menu = new SysMenu();
        menu.setMenuId(dto.getMenuId());
        menu.setMenuName(dto.getPermsName());
        menu.setPerms(dto.getPerms());
        menu.setType(dto.getType());
        menu.setParentId(dto.getParentId());
        menu.setPath(dto.getPath());
        menu.setComponent(dto.getComponent());
        menu.setVisible(dto.getVisible());
        menu.setEnabled(dto.getEnabled());
        menu.setIcon(dto.getIcon());
        menu.setOrderNum(dto.getOrderNum());
        return menu;
    }
}
