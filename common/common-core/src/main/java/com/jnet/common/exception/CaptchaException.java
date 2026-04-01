package com.jnet.common.exception;

import com.jnet.common.constant.ResultCode;

import java.io.Serial;

/**
 * 验证码异常类
 * 
 * <p>当验证码校验失败时抛出的业务异常</p>
 * 
 * <h3>触发场景：</h3>
 * <ul>
 *     <li>用户输入的验证码错误</li>
 *     <li>验证码已过期</li>
 *     <li>验证码不存在</li>
 * </ul>
 * 
 * <h3>状态码：</h3>
 * <p>使用 ResultCode.CAPTCHA_ERROR (1001)</p>
 * 
 * @author mu
 * @version 1.0
 * @since 2026/4/1
 */

public class CaptchaException extends BaseException {

    @Serial
    private static final long serialVersionUID = 1L;

    public CaptchaException() {
        super("验证码错误", ResultCode.CAPTCHA_ERROR);
    }

}
