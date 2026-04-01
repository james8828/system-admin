package com.jnet.common.exception;

import com.jnet.common.constant.ResultCode;

/**
 * 用户不存在异常类
 * 
 * <p>当查询的用户不存在时抛出的业务异常</p>
 * 
 * <h3>触发场景：</h3>
 * <ul>
 *     <li>用户名输入错误</li>
 *     <li>用户已被删除</li>
 *     <li>查询条件不匹配任何用户</li>
 * </ul>
 * 
 * <h3>状态码：</h3>
 * <p>使用 ResultCode.USER_NOT_EXIST (1002)</p>
 * 
 * @author mu
 * @version 1.0
 * @since 2026/4/1
 */

public class UserNotExistsException extends BaseException {

    private static final long serialVersionUID = 1L;

    public UserNotExistsException() {
        super("用户不存在", ResultCode.USER_NOT_EXIST);
    }

}
