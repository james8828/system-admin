package com.jnet.common.result;

import lombok.Getter;

@Getter
public enum ResultCode {

    SUCCESS("0", "成功"),
    FAIL("1", "失败"),
    UNAUTHORIZED("401", "未授权"),
    FORBIDDEN("403", "禁止访问"),
    NOT_FOUND("404", "资源不存在"),
    INTERNAL_SERVER_ERROR("500", "服务器内部错误"),
    CAPTCHA_ERROR("1001", "验证码错误"),
    USER_NOT_EXIST("1002", "用户不存在"),
    PASSWORD_ERROR("1003", "密码错误"),
    USER_DISABLED("1004", "用户已禁用"),
    TOKEN_EXPIRED("1005", "令牌已过期"),
    TOKEN_INVALID("1006", "令牌无效"),
    PERMISSION_DENIED("1007", "权限不足"),
    DATA_SCOPE_ERROR("1008", "数据权限不足");

    private final String code;
    private final String message;

    ResultCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

}
