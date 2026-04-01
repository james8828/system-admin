package com.jnet.system.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 鏉冮檺鏍￠獙缁撴灉 DTO
 */
@Data
public class PermissionCheckResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 鏉冮檺鏍囪瘑
     */
    private String perms;

    /**
     * 鏄�惁鏈夋潈闄愶紙true=鏈夋潈闄?false=鏃犳潈闄愶級
     */
    private Boolean hasPermission;

    /**
     * 鏉冮檺鍚嶇О
     */
    private String permsName;

    /**
     * 鎷掔粷鍘熷洜锛堟棤鏉冮檺鏃剁殑璇︾粏璇存槑锛?     */
    private String denyReason;

    public PermissionCheckResult() {
    }

    public PermissionCheckResult(String perms, Boolean hasPermission) {
        this.perms = perms;
        this.hasPermission = hasPermission;
    }

    public PermissionCheckResult(String perms, Boolean hasPermission, String denyReason) {
        this.perms = perms;
        this.hasPermission = hasPermission;
        this.denyReason = denyReason;
    }
}
