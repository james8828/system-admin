package com.jnet.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@TableName("sys_user")
public class SysUser {

    private Long userId;

    private String userName;

    private String password;

    private String nickName;

    private String headImgUrl;

    private String mobile;

    private Integer sex;

    private String email;

    private Boolean enabled;

    private String type;

    private String company;

    private String openId;

    private Long tenantId;

    private Long deptId;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.UPDATE)
    private Long updateBy;

    @TableField(fill = FieldFill.UPDATE)
    private Date updateTime;

    private Boolean delFlag;

    private String remark;

    @TableField(exist = false)
    private List<Long> roleIds = new ArrayList<>();

    @TableField(exist = false)
    private String deptName;

}
