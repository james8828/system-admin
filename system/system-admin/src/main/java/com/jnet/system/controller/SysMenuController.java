package com.jnet.system.controller;

import com.jnet.system.entity.SysMenu;
import com.jnet.system.service.SysMenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/system/menu")
@RequiredArgsConstructor
public class SysMenuController {

    private final SysMenuService sysMenuService;

    @GetMapping("/tree")
    public List<SysMenu> getMenuTree() {
        return sysMenuService.getMenuTree();
    }

    @GetMapping("/list")
    public List<SysMenu> getMenuList() {
        return sysMenuService.getMenuList();
    }

    @GetMapping("/{menuId}")
    public SysMenu getMenuById(@PathVariable Long menuId) {
        return sysMenuService.getMenuById(menuId);
    }

    @PostMapping
    public Boolean addMenu(@RequestBody SysMenu menu) {
        return sysMenuService.addMenu(menu);
    }

    @PutMapping
    public Boolean updateMenu(@RequestBody SysMenu menu) {
        return sysMenuService.updateMenu(menu);
    }

    @DeleteMapping("/{menuId}")
    public Boolean deleteMenu(@PathVariable Long menuId) {
        return sysMenuService.deleteMenu(menuId);
    }

}
