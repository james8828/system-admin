package com.jnet.gateway.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 健康检查控制器
 * 
 * <p>提供网关的健康检查和状态查询接口</p>
 * 
 * <h3>接口列表：</h3>
 * <ul>
 *     <li>GET /health - 健康检查</li>
 *     <li>GET /info - 服务信息</li>
 * </ul>
 * 
 * @author JNet Team
 * @version 1.0
 * @since 2024-01-01
 */
@RestController
@RequestMapping
public class HealthController {

    @Value("${spring.application.name:unknown}")
    private String applicationName;

    @Value("${server.port:0}")
    private String serverPort;

    /**
     * 健康检查接口
     * 
     * <p>返回网关的当前状态，用于负载均衡器或监控系统检测服务是否可用</p>
     * 
     * @return 健康状态信息
     */
    @GetMapping("/health")
    public Mono<Map<String, Object>> health() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "UP");
        result.put("timestamp", LocalDateTime.now().toString());
        result.put("application", applicationName);
        return Mono.just(result);
    }

    /**
     * 服务信息接口
     * 
     * <p>返回网关的详细信息，包括应用名称、端口、启动时间等</p>
     * 
     * @return 服务信息
     */
    @GetMapping("/info")
    public Mono<Map<String, Object>> info() {
        Map<String, Object> result = new HashMap<>();
        result.put("application", applicationName);
        result.put("port", serverPort);
        result.put("startTime", LocalDateTime.now().toString());
        result.put("description", "JNet API Gateway");
        result.put("version", "1.0.0");
        return Mono.just(result);
    }
}
