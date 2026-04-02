package com.jnet.common.constant;

import lombok.Getter;


/**
 * 通用响应状态码枚举
 * 
 * <p>定义了系统中所有业务操作的状态码，用于统一返回格式</p>
 * 
 * <h3>状态码分类：</h3>
 * <ul>
 *     <li>0-99：基础状态码（成功、失败等）</li>
 *     <li>400-599：HTTP 标准状态码（未授权、禁止访问等）</li>
 *     <li>1000-1999：业务状态码（验证码、用户相关等）</li>
 * </ul>
 * 
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 返回成功
 * Result.success(ResultCode.SUCCESS, data);
 * 
 * // 返回错误
 * Result.error(ResultCode.USER_NOT_EXIST);
 * }</pre>
 * 
 * @author mu
 * @version 1.0
 * @since 2026/4/1
 */
@Getter
public enum ResultCode {

    SUCCESS(200, "成功"),
    FAIL(500, "失败"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),
    INTERNAL_SERVER_ERROR(500, "服务器内部错误"),
    CAPTCHA_ERROR(1001, "验证码错误"),
    USER_NOT_EXIST(1002, "用户不存在"),
    PASSWORD_ERROR(1003, "密码错误"),
    USER_DISABLED(1004, "用户已禁用"),
    TOKEN_EXPIRED(1005, "令牌已过期"),
    TOKEN_INVALID(1006, "令牌无效"),
    PERMISSION_DENIED(1007, "权限不足"),
    DATA_SCOPE_ERROR(1008, "数据权限不足");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

}
