package com.jnet.system.controller;

import com.jnet.common.result.PageQuery;
import com.jnet.common.result.PageResult;
import com.jnet.common.result.Result;
import com.jnet.system.entity.SysUser;
import com.jnet.system.service.SysUserService;
import com.jnet.system.api.dto.SysUserDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/system/user")
@RequiredArgsConstructor
public class SysUserController {

    private final SysUserService sysUserService;

    @GetMapping("/page")
    public Result<PageResult<SysUserDTO>> pageUser(PageQuery pageQuery, SysUserDTO user) {
        SysUser queryUser = new SysUser();
        if (user != null) {
            BeanUtils.copyProperties(user, queryUser);
        }
        PageResult<SysUser> result = sysUserService.pageUser(pageQuery, queryUser);
        return Result.success(convertPageResult(result));
    }

    @GetMapping("/{userId}")
    public Result<SysUserDTO> getUserById(@PathVariable("userId") Long userId) {
        SysUser user = sysUserService.getUserById(userId);
        return user != null ? Result.success(convertToDTO(user)) : Result.error("用户不存在");
    }

    @GetMapping("/username/{username}")
    public Result<SysUserDTO> getUserByUsername(@PathVariable("username") String username) {
        SysUser user = sysUserService.getUserByUsername(username);
        return user != null ? Result.success(convertToDTO(user)) : Result.error("用户不存在");
    }

    @PostMapping
    public Result<Boolean> addUser(@RequestBody SysUserDTO user) {
        SysUser sysUser = new SysUser();
        BeanUtils.copyProperties(user, sysUser);
        return Result.success(sysUserService.addUser(sysUser));
    }

    @PutMapping
    public Result<Boolean> updateUser(@RequestBody SysUserDTO user) {
        SysUser sysUser = new SysUser();
        BeanUtils.copyProperties(user, sysUser);
        return Result.success(sysUserService.updateUser(sysUser));
    }

    @DeleteMapping("/{userId}")
    public Result<Boolean> deleteUser(@PathVariable("userId") Long userId) {
        return Result.success(sysUserService.deleteUser(userId));
    }

    @PostMapping("/{userId}/resetPwd")
    public Result<Boolean> resetPassword(@PathVariable("userId") Long userId, @RequestBody String newPassword) {
        return Result.success(sysUserService.resetPassword(userId, newPassword));
    }

    @PutMapping("/{userId}/enable")
    public Result<Boolean> enableUser(@PathVariable("userId") Long userId, @RequestBody Boolean enabled) {
        return Result.success(sysUserService.enableUser(userId, enabled));
    }

    @DeleteMapping("/batch")
    public Result<Boolean> batchDeleteUsers(@RequestBody java.util.List<Long> userIds) {
        return Result.success(sysUserService.batchDeleteUsers(userIds));
    }

    private SysUserDTO convertToDTO(SysUser user) {
        SysUserDTO dto = new SysUserDTO();
        BeanUtils.copyProperties(user, dto);
        return dto;
    }

    private PageResult<SysUserDTO> convertPageResult(PageResult<SysUser> pageResult) {
        return new PageResult<>(
                pageResult.getRecords().stream().map(this::convertToDTO).collect(Collectors.toList()), pageResult.getTotal(),
                pageResult.getSize(), pageResult.getCurrent()
        );
    }
}
