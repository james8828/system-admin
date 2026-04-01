package com.jnet.system.api.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 部门 DTO
 */
@Data
public class SysDeptDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 部门 ID
     */
    private Long deptId;

    /**
     * 父部门 ID
     */
    private Long parentId;

    /**
     * 部门名称
     */
    private String deptName;

    /**
     * 显示顺序
     */
    private Integer sort;

    /**
     * 负责人
     */
    private String leader;

    /**
     * 联系电话
     */
    private String phone;

    /**
     * 状态 (0:禁用 1:启用)
     */
    private Integer status;

    /**
     * 子部门
     */
    private List<SysDeptDTO> children;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
