package com.jnet.system.api.client;

import com.jnet.common.result.PageQuery;
import com.jnet.common.result.PageResult;
import com.jnet.common.result.Result;
import com.jnet.system.api.dto.SysUserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户服务 Feign 客户端
 */
@FeignClient(name = "jnet-system-admin", contextId = "sysUserClient")
public interface SysUserFeignClient {

    /**
     * 分页查询用户
     */
    @GetMapping("/api/system/user/page")
    Result<PageResult<SysUserDTO>> pageUser(@SpringQueryMap PageQuery pageQuery, @SpringQueryMap SysUserDTO user);

    /**
     * 根据 ID 查询用户
     */
    @GetMapping("/api/system/user/{userId}")
    Result<SysUserDTO> getUserById(@PathVariable("userId") Long userId);

    /**
     * 根据用户名查询用户
     */
    @GetMapping("/api/system/user/username/{username}")
    Result<SysUserDTO> getUserByUsername(@PathVariable("username") String username);

    /**
     * 新增用户
     */
    @PostMapping("/api/system/user")
    Result<Boolean> addUser(@RequestBody SysUserDTO user);

    /**
     * 修改用户
     */
    @PutMapping("/api/system/user")
    Result<Boolean> updateUser(@RequestBody SysUserDTO user);

    /**
     * 删除用户
     */
    @DeleteMapping("/api/system/user/{userId}")
    Result<Boolean> deleteUser(@PathVariable("userId") Long userId);

    /**
     * 重置密码
     */
    @PostMapping("/api/system/user/{userId}/resetPwd")
    Result<Boolean> resetPassword(@PathVariable("userId") Long userId, @RequestBody String newPassword);

    /**
     * 启用/禁用用户
     */
    @PutMapping("/api/system/user/{userId}/enable")
    Result<Boolean> enableUser(@PathVariable("userId") Long userId, @RequestBody Boolean enabled);

    /**
     * 批量删除用户
     */
    @DeleteMapping("/api/system/user/batch")
    Result<Boolean> batchDeleteUsers(@RequestBody List<Long> userIds);
}
