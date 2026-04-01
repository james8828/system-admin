package com.jnet.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 网关应用启动类
 * 
 * <p>基于 Spring Cloud Gateway 构建的微服务网关</p>
 * 
 * <h3>主要功能：</h3>
 * <ul>
 *     <li>路由转发：将请求路由到对应的微服务</li>
 *     <li>负载均衡：通过 LoadBalancer 实现客户端负载均衡</li>
 *     <li>跨域支持：配置 CORS 允许跨域访问</li>
 *     <li>统一鉴权：验证 Token 有效性，保护后端服务</li>
 *     <li>链路追踪：为每个请求生成 Trace ID，实现全链路追踪</li>
 *     <li>日志记录：统一记录请求和响应信息</li>
 * </ul>
 * 
 * <h3>架构说明：</h3>
 * <ul>
 *     <li>基于 WebFlux 响应式编程模型</li>
 *     <li>使用 GlobalFilter 实现全局过滤器链</li>
 *     <li>支持 OAuth2 JWT Token 认证</li>
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
        System.out.println("    JNet 网关服务启动成功！");
        System.out.println("    访问地址：http://localhost:8080");
        System.out.println("========================================");
    }
}
