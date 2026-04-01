package com.jnet.common.exception;

import com.jnet.common.constant.ResultCode;

/**
 * 用户已禁用异常类
 * 
 * <p>当尝试登录被禁用的用户时抛出的业务异常</p>
 * 
 * <h3>触发场景：</h3>
 * <ul>
 *     <li>用户账号被管理员禁用</li>
 *     <li>用户因违规被封号</li>
 *     <li>用户主动注销账号</li>
 * </ul>
 * 
 * <h3>状态码：</h3>
 * <p>使用 ResultCode.USER_DISABLED (1004)</p>
 * 
 * @author mu
 * @version 1.0
 * @since 2026/4/1
 */

public class UserDisableException extends BaseException {

    private static final long serialVersionUID = 1L;

    public UserDisableException() {
        super("用户已禁用", ResultCode.USER_DISABLED);
    }

}
