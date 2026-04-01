package com.jnet.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

@Data
@TableName("sys_oper_log")
public class SysOperLog {

    private Long operId;

    private String title;

    private String businessType;

    private String method;

    private String requestMethod;

    private String operatorType;

    private String operName;

    private String deptName;

    private String operUrl;

    private String operIp;

    private String operLocation;

    private String operParam;

    private String jsonResult;

    private Boolean status;

    private String errorMsg;

    private Date operTime;

    private Long costTime;

    private Long tenantId;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

}
