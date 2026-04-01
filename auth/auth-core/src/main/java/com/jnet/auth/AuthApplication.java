package com.jnet.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * @author mu
 * @version 1.0
 * @since 2026/4/1
 */
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
@EnableDiscoveryClient
@EnableFeignClients(basePackages = {"com.jnet.system.api.client"})
public class AuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
        System.out.println("========================================");
        System.out.println("    JNet 认证服务启动成功！");
        System.out.println("    OAuth2 授权端点：http://localhost:8081/oauth2/authorize");
        System.out.println("    Token 端点：http://localhost:8081/oauth2/token");
        System.out.println("========================================");
    }

}
