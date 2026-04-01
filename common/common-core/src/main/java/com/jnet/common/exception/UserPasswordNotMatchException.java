package com.jnet.common.exception;

import com.jnet.common.constant.ResultCode;

/**
 * 用户密码错误异常类
 * 
 * <p>当用户输入的密码不正确时抛出的业务异常</p>
 * 
 * <h3>触发场景：</h3>
 * <ul>
 *     <li>登录时密码输入错误</li>
 *     <li>修改密码时原密码验证失败</li>
 *     <li>密码重置时验证失败</li>
 * </ul>
 * 
 * <h3>状态码：</h3>
 * <p>使用 ResultCode.PASSWORD_ERROR (1003)</p>
 * 
 * @author mu
 * @version 1.0
 * @since 2026/4/1
 */

public class UserPasswordNotMatchException extends BaseException {

    private static final long serialVersionUID = 1L;

    public UserPasswordNotMatchException() {
        super("密码错误", ResultCode.PASSWORD_ERROR);
    }

}
