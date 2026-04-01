package com.jnet.auth.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 登录页面控制器 - 处理 GET /login 请求
 */
@Controller
public class LoginController {

    /**
     * 显示登录页面
     */
    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }
}
