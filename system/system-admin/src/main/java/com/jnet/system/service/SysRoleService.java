package com.jnet.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jnet.common.result.PageQuery;
import com.jnet.common.result.PageResult;
import com.jnet.system.entity.SysRole;

import java.util.List;

public interface SysRoleService extends IService<SysRole> {

    PageResult<SysRole> pageRole(PageQuery pageQuery, SysRole role);

    SysRole getRoleById(Long roleId);

    List<SysRole> getRolesByUserId(Long userId);

    Boolean addRole(SysRole role);

    Boolean updateRole(SysRole role);

    Boolean deleteRole(Long roleId);

    Boolean assignMenuToRole(Long roleId, List<Long> menuIds);

    Boolean assignUserToRole(Long roleId, List<Long> userIds);

}
