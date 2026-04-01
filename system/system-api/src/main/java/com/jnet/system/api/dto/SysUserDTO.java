package com.jnet.system.api.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

/**
 * 用户 DTO
 */
@Data
public class SysUserDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String userName;

    /**
     * 密码
     */
    private String password;

    /**
     * 昵称
     */
    private String nickName;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 手机号
     */
    private String mobile;

    /**
     * 性别 (0:男 1:女)
     */
    private Integer sex;

    /**
     * 头像地址
     */
    private String headImgUrl;

    /**
     * 状态 (0:禁用 1:启用)
     */
    private Boolean enabled;

    /**
     * 部门 ID
     */
    private Long deptId;

    /**
     * 角色 ID 列表
     */
    private List<Long> roleIds;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}
