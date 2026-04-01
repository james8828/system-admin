package com.jnet.common.exception;

import com.jnet.common.result.ResultCode;

public class BaseException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private ResultCode resultCode;

    public BaseException() {
        this(ResultCode.FAIL);
    }

    public BaseException(ResultCode resultCode) {
        this(resultCode.getMessage());
        this.resultCode = resultCode;
    }

    public BaseException(String message) {
        super(message);
        this.resultCode = ResultCode.FAIL;
    }

    public BaseException(String message, ResultCode resultCode) {
        super(message);
        this.resultCode = resultCode;
    }

    public BaseException(String message, Throwable cause, ResultCode resultCode) {
        super(message, cause);
        this.resultCode = resultCode;
    }

    public ResultCode getResultCode() {
        return resultCode;
    }

}
