package com.jnet.system.controller;

import com.jnet.common.result.PageQuery;
import com.jnet.common.result.PageResult;
import com.jnet.system.entity.SysRole;
import com.jnet.system.service.SysRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/system/role")
@RequiredArgsConstructor
public class SysRoleController {

    private final SysRoleService sysRoleService;

    @GetMapping("/page")
    public PageResult<SysRole> pageRole(PageQuery pageQuery, SysRole role) {
        return sysRoleService.pageRole(pageQuery, role);
    }

    @GetMapping("/{roleId}")
    public SysRole getRoleById(@PathVariable Long roleId) {
        return sysRoleService.getRoleById(roleId);
    }

    @GetMapping("/user/{userId}")
    public List<SysRole> getRolesByUserId(@PathVariable Long userId) {
        return sysRoleService.getRolesByUserId(userId);
    }

    @PostMapping
    public Boolean addRole(@RequestBody SysRole role) {
        return sysRoleService.addRole(role);
    }

    @PutMapping
    public Boolean updateRole(@RequestBody SysRole role) {
        return sysRoleService.updateRole(role);
    }

    @DeleteMapping("/{roleId}")
    public Boolean deleteRole(@PathVariable Long roleId) {
        return sysRoleService.deleteRole(roleId);
    }

    @PostMapping("/{roleId}/menu")
    public Boolean assignMenuToRole(@PathVariable Long roleId, @RequestBody List<Long> menuIds) {
        return sysRoleService.assignMenuToRole(roleId, menuIds);
    }

    @PostMapping("/{roleId}/user")
    public Boolean assignUserToRole(@PathVariable Long roleId, @RequestBody List<Long> userIds) {
        return sysRoleService.assignUserToRole(roleId, userIds);
    }

}
