package com.jnet.common.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 服务层业务异常类
 * 
 * <p>用于服务层（Service Layer）抛出的业务异常，包含状态码和错误消息</p>
 * 
 * <h3>主要特点：</h3>
 * <ul>
 *     <li>继承 RuntimeException，支持 unchecked 异常处理</li>
 *     <li>包含 code 和 message 字段，便于前端展示</li>
 *     <li>默认状态码为 500（服务器内部错误）</li>
 *     <li>支持自定义状态码和消息</li>
 * </ul>
 * 
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 抛出默认异常（code=500）
 * throw new ServiceException("服务异常");
 * 
 * // 抛出自定义状态码异常
 * throw new ServiceException("参数错误", 400);
 * }</pre>
 * 
 * @author mu
 * @version 1.0
 * @since 2026/4/1
 */

@Data
@EqualsAndHashCode(callSuper = true)
public class ServiceException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private Integer code;

    private String message;

    public ServiceException(String message) {
        this(message, 500);
    }

    public ServiceException(String message, Integer code) {
        super(message);
        this.code = code;
        this.message = message;
    }

}
