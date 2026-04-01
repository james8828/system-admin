package com.jnet.system;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;


/**
 * 系统统一管理应用
 * 统一的系统管理 API 入口，整合所有 system 子模块功能
 */
@SpringBootApplication
@MapperScan(basePackages = {"com.jnet.system.mapper"})
@EnableFeignClients(basePackages = {"com.jnet.system.api.client"})
public class SystemAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(SystemAdminApplication.class, args);
        System.out.println("========================================");
        System.out.println("JNet System Admin Service Started Successfully");
        System.out.println("========================================");
    }
}
