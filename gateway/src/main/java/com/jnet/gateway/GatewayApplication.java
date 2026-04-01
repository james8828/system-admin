package com.jnet.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 网关应用启动类
 * 
 * <p>基于 Spring Cloud Gateway 实现的简单网关</p>
 * 
 * <h3>主要功能：</h3>
 * <ul>
 *     <li>路由转发：将请求路由到对应的微服务</li>
 *     <li>负载均衡：通过 LoadBalancer 实现客户端负载均衡</li>
 *     <li>跨域支持：配置 CORS 允许跨域访问</li>
 *     <li>简单鉴权：验证 Token 有效性（可选）</li>
 * </ul>
 * 
 * @author JNet Team
 * @version 1.0
 * @since 2024-01-01
 */
@SpringBootApplication
@EnableDiscoveryClient
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
        System.out.println("========================================");
        System.out.println("    JNet Gateway Started Successfully!");
        System.out.println("    Access: http://localhost:8080");
        System.out.println("========================================");
    }
}
