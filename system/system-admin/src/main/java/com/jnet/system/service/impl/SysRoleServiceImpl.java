package com.jnet.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jnet.common.exception.ServiceException;
import com.jnet.common.result.PageQuery;
import com.jnet.common.result.PageResult;
import com.jnet.system.entity.SysRole;
import com.jnet.system.mapper.SysRoleMapper;
import com.jnet.system.service.SysRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements SysRoleService {

    private final SysRoleMapper sysRoleMapper;

    @Override
    public PageResult<SysRole> pageRole(PageQuery pageQuery, SysRole role) {
        Page<SysRole> page = new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize());
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(role.getRoleName() != null, SysRole::getRoleName, role.getRoleName());
        wrapper.eq(role.getEnabled() != null, SysRole::getEnabled, role.getEnabled());
        wrapper.orderByAsc(SysRole::getRoleSort);
        
        IPage<SysRole> result = page(page, wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), pageQuery.getPageSize(), pageQuery.getPageNum());
    }

    @Override
    public SysRole getRoleById(Long roleId) {
        return sysRoleMapper.selectById(roleId);
    }

    @Override
    public List<SysRole> getRolesByUserId(Long userId) {
        return sysRoleMapper.selectRolesByUserId(userId);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean addRole(SysRole role) {
        if (getRoleByRoleKey(role.getRoleKey()) != null) {
            throw new ServiceException("角色权限字符串已存在");
        }
        return save(role);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean updateRole(SysRole role) {
        SysRole existingRole = getRoleByRoleKey(role.getRoleKey());
        if (existingRole != null && !existingRole.getRoleId().equals(role.getRoleId())) {
            throw new ServiceException("角色权限字符串已存在");
        }
        return updateById(role);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean deleteRole(Long roleId) {
        return removeById(roleId);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean assignMenuToRole(Long roleId, List<Long> menuIds) {
        // TODO: 实现角色菜单分配逻辑
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean assignUserToRole(Long roleId, List<Long> userIds) {
        // TODO: 实现角色用户分配逻辑
        return true;
    }

    private SysRole getRoleByRoleKey(String roleKey) {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRole::getRoleKey, roleKey);
        return sysRoleMapper.selectOne(wrapper);
    }

}
