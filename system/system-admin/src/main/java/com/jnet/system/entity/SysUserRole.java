package com.jnet.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

/**
 * 用户角色关联实体
 */
@Data
@TableName("sys_user_role")
public class SysUserRole {

    /**
     * 主键 ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 角色 ID
     */
    private Long roleId;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
}
