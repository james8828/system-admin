package com.jnet.common.exception;

import com.jnet.common.result.ResultCode;

public class CaptchaException extends BaseException {

    private static final long serialVersionUID = 1L;

    public CaptchaException() {
        super("验证码错误", ResultCode.CAPTCHA_ERROR);
    }

}
