package com.jnet.common.exception;

import com.jnet.common.result.ResultCode;

public class UserNotExistsException extends BaseException {

    private static final long serialVersionUID = 1L;

    public UserNotExistsException() {
        super("用户不存在", ResultCode.USER_NOT_EXIST);
    }

}
