package com.jnet.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@TableName("sys_menu")
public class SysMenu {

    private Long menuId;

    private String menuName;

    private String path;

    private String component;

    private Boolean visible;

    private Boolean enabled;

    private String perms;

    private String icon;

    private Integer type;

    private Long parentId;

    private Integer orderNum;

    private String queryParams;

    private Boolean isCache;

    private Boolean isFrame;

    private Long tenantId;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.UPDATE)
    private Long updateBy;

    @TableField(fill = FieldFill.UPDATE)
    private Date updateTime;

    @TableLogic
    private Boolean delFlag;

    @TableField(exist = false)
    private List<SysMenu> children = new ArrayList<>();

}
