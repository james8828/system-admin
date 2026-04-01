package com.jnet.common.result;

import com.jnet.common.constant.ResultCode;
import lombok.Data;

import java.io.Serializable;

/**
 * 通用 API 响应结果封装类
 * 
 * <p>用于统一后端接口的返回格式，包含状态码、消息和数据</p>
 * 
 * <h3>数据结构：</h3>
 * <ul>
 *     <li>code - 状态码（使用 ResultCode 枚举）</li>
 *     <li>message - 响应消息</li>
 *     <li>data - 响应数据（泛型）</li>
 * </ul>
 * 
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 返回成功（无数据）
 * return Result.success();
 * 
 * // 返回成功（带数据）
 * return Result.success(data);
 * 
 * // 返回成功（自定义消息）
 * return Result.success("操作成功", data);
 * 
 * // 返回错误
 * return Result.error(ResultCode.USER_NOT_EXIST);
 * }</pre>
 * 
 * @param <T> 响应数据的类型
 * @author mu
 * @version 1.0
 * @since 2026/4/1
 */

@Data
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer code;

    private String message;

    private T data;

    public Result() {
    }

    public Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public Boolean isSuccess() {
        return code == ResultCode.SUCCESS.getCode();
    }

    public static <T> Result<T> success() {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), null);
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    public static <T> Result<T> success(ResultCode resultCode, T data) {
        return new Result<>(resultCode.getCode(), resultCode.getMessage(), data);
    }

    public static <T> Result<T> success(String message, T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), message, data);
    }

    public static <T> Result<T> error() {
        return new Result<>(ResultCode.FAIL.getCode(), ResultCode.FAIL.getMessage(), null);
    }

    public static <T> Result<T> error(String message) {
        return new Result<>(ResultCode.FAIL.getCode(), message, null);
    }

    public static <T> Result<T> error(ResultCode resultCode) {
        return new Result<>(resultCode.getCode(), resultCode.getMessage(), null);
    }

    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null);
    }

    public static <T> Result<T> error(ResultCode resultCode, String message) {
        return new Result<>(resultCode.getCode(), message, null);
    }
}
