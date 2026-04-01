package com.jnet.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jnet.common.exception.ServiceException;
import com.jnet.common.result.PageQuery;
import com.jnet.common.result.PageResult;
import com.jnet.system.entity.SysUser;
import com.jnet.system.mapper.SysUserMapper;
import com.jnet.system.service.SysUserService;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    @Resource
    private PasswordEncoder passwordEncoder;

    @Override
    public PageResult<SysUser> pageUser(PageQuery pageQuery, SysUser user) {
        Page<SysUser> page = new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize());
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(user.getUserName() != null, SysUser::getUserName, user.getUserName());
        wrapper.eq(user.getEnabled() != null, SysUser::getEnabled, user.getEnabled());
        wrapper.orderByDesc(SysUser::getCreateTime);
        
        IPage<SysUser> result = page(page, wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), pageQuery.getPageSize(), pageQuery.getPageNum());
    }

    @Override
    public SysUser getUserById(Long userId) {
        return getById(userId);
    }

    @Override
    public SysUser getUserByUsername(String username) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUserName, username);
        return getOne( wrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean addUser(SysUser user) {
        if (getUserByUsername(user.getUserName()) != null) {
            throw new ServiceException("用户名已存在");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setDelFlag(false);
        return save(user);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean updateUser(SysUser user) {
        if (getUserByUsername(user.getUserName()) != null && 
                    !getUserByUsername(user.getUserName()).getUserId().equals(user.getUserId())) {
            throw new ServiceException("用户名已存在");
        }
        return updateById(user);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean deleteUser(Long userId) {
        return removeById(userId);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean resetPassword(Long userId, String newPassword) {
        SysUser user = getUserById(userId);
        if (user == null) {
            return false;
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        return updateById(user);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean enableUser(Long userId, Boolean enabled) {
        SysUser user = getUserById(userId);
        if (user == null) {
            return false;
        }
        user.setEnabled(enabled);
        return updateById(user);
    }

    @Override
    public Boolean batchDeleteUsers(List<Long> userIds) {
        return null;
    }

}
