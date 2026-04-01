package com.jnet.system.api.client;

import com.jnet.common.result.Result;
import com.jnet.system.api.dto.SysMenuDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 菜单服务 Feign 客户端
 */
@FeignClient(name = "jnet-system-admin", contextId = "sysMenuClient")
public interface SysMenuFeignClient {

    /**
     * 获取菜单树
     */
    @GetMapping("/api/system/menu/tree")
    Result<List<SysMenuDTO>> getMenuTree();

    /**
     * 根据 ID 查询菜单
     */
    @GetMapping("/api/system/menu/{menuId}")
    Result<SysMenuDTO> getMenuById(@PathVariable("menuId") Long menuId);

    /**
     * 新增菜单
     */
    @PostMapping("/api/system/menu")
    Result<Boolean> addMenu(@RequestBody SysMenuDTO menu);

    /**
     * 修改菜单
     */
    @PutMapping("/api/system/menu")
    Result<Boolean> updateMenu(@RequestBody SysMenuDTO menu);

    /**
     * 删除菜单
     */
    @DeleteMapping("/api/system/menu/{menuId}")
    Result<Boolean> deleteMenu(@PathVariable("menuId") Long menuId);

    /**
     * 获取用户权限菜单
     */
    @GetMapping("/api/system/menu/user/{userId}")
    Result<List<SysMenuDTO>> getUserMenus(@PathVariable("userId") Long userId);
}
