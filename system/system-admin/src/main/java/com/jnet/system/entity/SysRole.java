package com.jnet.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@TableName("sys_role")
public class SysRole {

    private Long roleId;

    private String roleName;

    private String roleKey;

    private Integer roleSort;

    private Boolean enabled;

    private String dataScope;

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

    private String remark;

    @TableField(exist = false)
    private List<Long> menuIds = new ArrayList<>();

    @TableField(exist = false)
    private List<Long> userIds = new ArrayList<>();

}
