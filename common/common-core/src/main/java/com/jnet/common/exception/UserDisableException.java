package com.jnet.common.exception;

import com.jnet.common.result.ResultCode;

public class UserDisableException extends BaseException {

    private static final long serialVersionUID = 1L;

    public UserDisableException() {
        super("用户已禁用", ResultCode.USER_DISABLED);
    }

}
