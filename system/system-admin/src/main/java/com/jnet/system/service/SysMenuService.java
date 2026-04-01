package com.jnet.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jnet.system.entity.SysMenu;

import java.util.List;

public interface SysMenuService extends IService<SysMenu> {

    List<SysMenu> getMenuTree();

    List<SysMenu> getMenuList();

    SysMenu getMenuById(Long menuId);

    List<SysMenu> getMenusByRoleId(Long roleId);

    List<String> getPermsByUserId(Long userId);

    Boolean addMenu(SysMenu menu);

    Boolean updateMenu(SysMenu menu);

    Boolean deleteMenu(Long menuId);

    Boolean getMenuIdsByRoleId(Long roleId);

}
