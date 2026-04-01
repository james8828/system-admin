package com.jnet.system.api.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 菜单 DTO
 */
@Data
public class SysMenuDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 菜单 ID
     */
    private Long menuId;

    /**
     * 父菜单 ID
     */
    private Long parentId;

    /**
     * 菜单名称
     */
    private String menuName;

    /**
     * 路由地址
     */
    private String path;

    /**
     * 组件路径
     */
    private String component;

    /**
     * 菜单类型 (0:目录 1:菜单 2:按钮)
     */
    private Integer type;

    /**
     * 权限标识
     */
    private String perms;

    /**
     * 图标
     */
    private String icon;

    /**
     * 显示顺序
     */
    private Integer sort;

    /**
     * 是否可见 (0:隐藏 1:显示)
     */
    private Integer visible;

    /**
     * 子菜单
     */
    private List<SysMenuDTO> children;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
