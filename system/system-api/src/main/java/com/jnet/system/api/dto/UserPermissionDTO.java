package com.jnet.system.api.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Set;

/**
 * 用户权限信息 DTO
 * <p>
 * 用于在微服务间传递用户的权限信息
 * </p>
 * 
 * @author JNet Team
 * @version 1.0
 * @since 2024-01-01
 */
@Data
public class UserPermissionDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * 角色标识集合
     */
    private Set<String> roles;

    /**
     * 权限标识集合（如 system:user:list, system:role:add 等）
     */
    private Set<String> permissions;

    /**
     * 权限路径集合（用于网关 URL 权限匹配）
     */
    private Set<String> paths;

    /**
     * 是否超级管理员
     */
    private boolean admin;
}
