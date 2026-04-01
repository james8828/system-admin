package com.jnet.system.api.client;

import com.jnet.common.result.PageQuery;
import com.jnet.common.result.PageResult;
import com.jnet.common.result.Result;
import com.jnet.system.api.dto.SysRoleDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色服务 Feign 客户端
 */
@FeignClient(name = "jnet-system-admin", contextId = "sysRoleClient")
public interface SysRoleFeignClient {

    /**
     * 分页查询角色
     */
    @GetMapping("/api/system/role/page")
    Result<PageResult<SysRoleDTO>> pageRole(@SpringQueryMap PageQuery pageQuery, @SpringQueryMap SysRoleDTO role);

    /**
     * 根据 ID 查询角色
     */
    @GetMapping("/api/system/role/{roleId}")
    Result<SysRoleDTO> getRoleById(@PathVariable("roleId") Long roleId);

    /**
     * 查询所有角色
     */
    @GetMapping("/api/system/role/list")
    Result<List<SysRoleDTO>> listRoles();

    /**
     * 新增角色
     */
    @PostMapping("/api/system/role")
    Result<Boolean> addRole(@RequestBody SysRoleDTO role);

    /**
     * 修改角色
     */
    @PutMapping("/api/system/role")
    Result<Boolean> updateRole(@RequestBody SysRoleDTO role);

    /**
     * 删除角色
     */
    @DeleteMapping("/api/system/role/{roleId}")
    Result<Boolean> deleteRole(@PathVariable("roleId") Long roleId);

    /**
     * 分配菜单权限
     */
    @PostMapping("/api/system/role/menu/{roleId}")
    Result<Boolean> assignMenus(@PathVariable("roleId") Long roleId, @RequestBody List<Long> menuIds);

    /**
     * 分配用户
     */
    @PostMapping("/api/system/role/user/{roleId}")
    Result<Boolean> assignUsers(@PathVariable("roleId") Long roleId, @RequestBody List<Long> userIds);
}
