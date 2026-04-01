package com.jnet.common.exception;

import com.jnet.common.result.ResultCode;

public class UserPasswordNotMatchException extends BaseException {

    private static final long serialVersionUID = 1L;

    public UserPasswordNotMatchException() {
        super("密码错误", ResultCode.PASSWORD_ERROR);
    }

}
