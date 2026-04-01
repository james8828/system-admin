package com.jnet.system.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 权限 DTO
 * 用于权限管理和校验的数据传输对象
 */
@Data
public class PermissionDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 菜单 ID
     */
    private Long menuId;

    /**
     * 权限标识
     */
    private String perms;

    /**
     * 权限名称
     */
    private String permsName;

    /**
     * 菜单名称
     */
    private String menuName;

    /**
     * 权限类型（0=目录 1=菜单 2=按钮 3=接口）
     */
    private Integer type;

    /**
     * 父级权限 ID
     */
    private Long parentId;

    /**
     * 路由地址
     */
    private String path;

    /**
     * 组件路径
     */
    private String component;

    /**
     * 是否显示（true=显示 false=隐藏）
     */
    private Boolean visible;

    /**
     * 是否启用（true=启用 false=禁用）
     */
    private Boolean enabled;

    /**
     * 图标
     */
    private String icon;

    /**
     * 排序号
     */
    private Integer orderNum;

    /**
     * 权限标识列表（用于批量操作）
     */
    private List<String> permsList;

    /**
     * 子权限（用于树形结构）
     */
    private List<PermissionDTO> children;
}
