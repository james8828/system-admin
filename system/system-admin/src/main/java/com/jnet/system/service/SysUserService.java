package com.jnet.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jnet.common.result.PageQuery;
import com.jnet.common.result.PageResult;
import com.jnet.system.entity.SysUser;

import java.util.List;

public interface SysUserService extends IService<SysUser> {

    PageResult<SysUser> pageUser(PageQuery pageQuery, SysUser user);

    SysUser getUserById(Long userId);

    SysUser getUserByUsername(String username);

    Boolean addUser(SysUser user);

    Boolean updateUser(SysUser user);

    Boolean deleteUser(Long userId);

    Boolean resetPassword(Long userId, String newPassword);

    Boolean enableUser(Long userId, Boolean enabled);

    Boolean batchDeleteUsers(List<Long> userIds);

}
