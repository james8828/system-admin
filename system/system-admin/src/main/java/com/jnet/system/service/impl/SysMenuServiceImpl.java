package com.jnet.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jnet.system.entity.SysMenu;
import com.jnet.system.mapper.SysMenuMapper;
import com.jnet.system.service.SysMenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysMenuServiceImpl extends ServiceImpl<SysMenuMapper, SysMenu> implements SysMenuService {

    private final SysMenuMapper sysMenuMapper;

    @Override
    public List<SysMenu> getMenuTree() {
        List<SysMenu> menus = list();
        Map<Long, SysMenu> menuMap = menus.stream().collect(Collectors.toMap(SysMenu::getMenuId, m -> m));
        
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
        return tree;
    }

    @Override
    public List<SysMenu> getMenuList() {
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(SysMenu::getOrderNum);
        return list(wrapper);
    }

    @Override
    public SysMenu getMenuById(Long menuId) {
        return getById(menuId);
    }

    @Override
    public List<SysMenu> getMenusByRoleId(Long roleId) {
        return sysMenuMapper.selectMenusByRoleId(roleId);
    }

    @Override
    public List<String> getPermsByUserId(Long userId) {
        return sysMenuMapper.selectPermsByUserId(userId);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean addMenu(SysMenu menu) {
        return save(menu);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean updateMenu(SysMenu menu) {
        return updateById(menu);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean deleteMenu(Long menuId) {
        return removeById(menuId);
    }

    @Override
    public Boolean getMenuIdsByRoleId(Long roleId) {
        // TODO: 实现获取角色菜单ID列表
        return true;
    }

}
