package com.jnet.system.api.client;

import com.jnet.common.result.Result;
import com.jnet.system.api.dto.UserPermissionDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 用户权限服务 Feign 客户端
 * <p>
 * 用于查询用户的角色和权限信息
 * </p>
 * 
 * @author JNet Team
 * @version 1.0
 * @since 2024-01-01
 */
@FeignClient(name = "jnet-system-admin", contextId = "userPermissionClient")
public interface UserPermissionFeignClient {

    /**
     * 根据用户名查询用户权限信息
     * <p>
     * 返回用户的角色列表和权限标识列表，用于动态权限控制
     * </p>
     * 
     * @return 用户权限信息
     */
    @GetMapping("/api/system/permissions/")
    Result<UserPermissionDTO> getPermissionsByCurrentUser();



    /**
     * 根据用户 ID 查询用户权限信息
     * <p>
     * 返回用户的角色列表和权限标识列表，用于动态权限控制
     * </p>
     * 
     * @param userId 用户 ID
     * @return 用户权限信息
     */
    @GetMapping("/api/system/permissions/user/{userId}")
    Result<UserPermissionDTO> getUserPermissionsByUserId(@PathVariable("userId") Long userId);
}
