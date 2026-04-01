package com.jnet.common.exception;

import com.jnet.common.constant.ResultCode;
import lombok.Getter;

import java.io.Serial;

/**
 * 基础业务异常类
 * 
 * <p>所有业务异常的基类，提供统一的状态码和消息处理</p>
 * 
 * <h3>主要功能：</h3>
 * <ul>
 *     <li>继承 RuntimeException，支持 unchecked 异常处理</li>
 *     <li>封装 ResultCode 枚举，统一管理状态码</li>
 *     <li>支持多种构造方法，灵活创建异常实例</li>
 *     <li>可携带 cause，保留原始异常信息</li>
 * </ul>
 * 
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 抛出默认异常
 * throw new BaseException();
 * 
 * // 抛出自定义消息异常
 * throw new BaseException("操作失败");
 * 
 * // 抛出带状态码的异常
 * throw new BaseException(ResultCode.PERMISSION_DENIED);
 * }</pre>
 * 
 * @author mu
 * @version 1.0
 * @since 2026/4/1
 */

@Getter
public class BaseException extends RuntimeException {

    @Serial
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

}
