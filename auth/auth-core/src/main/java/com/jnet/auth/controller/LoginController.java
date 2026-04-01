package com.jnet.auth.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 登录页面控制器
 * 
 * <p>负责显示登录表单页面</p>
 * <p>路径映射：GET /login -> login.html</p>
 * 
 * @author mu
 * @version 1.0
 * @since 2026/4/1
 */
@Controller
public class LoginController {

    /**
     * 显示登录页面
     * 
     * <p>映射路径：GET /login</p>
     * <p>返回视图：login.html（位于 resources/templates/）</p>
     * 
     * @return 登录页面视图名
     */
    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }
}
