package com.jnet.gateway.filter;

import com.jnet.common.redis.TokenManager;
import com.jnet.gateway.config.GatewayAuthProperties;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.regex.Pattern;

import static com.jnet.common.constant.Constants.TRACE_ID_HEADER;

/**
 * 全局认证过滤器
 *
 * <p>负责验证请求中的 Token，确保只有合法请求才能访问后端服务</p>
 *
 * <h3>过滤逻辑：</h3>
 * <ol>
 *     <li>检查路径是否在跳过列表中，如果是则直接放行</li>
 *     <li>从请求头中提取 Authorization 信息</li>
 *     <li>验证 Bearer Token 格式是否正确</li>
 *     <li>验证 JWT Token 结构（Header.Payload.Signature）</li>
 *     <li>检查 Token 是否过期</li>
 *     <li>简单验证 Token 非空（完整的 JWT 验证需要在资源服务器中进行）</li>
 *     <li>将请求转发到下游服务</li>
 * </ol>
 *
 * <h3>注意事项：</h3>
 * <ul>
 *     <li>本过滤器只做基本的 Token 格式和过期验证，不做签名验证</li>
 *     <li>完整的 Token 验证应该在后端资源服务器中进行</li>
 *     <li>可以通过配置 enable-token-check 来控制是否启用验证</li>
 * </ul>
 *
 * @author mu
 * @version 1.0
 * @since 2026/4/1
 */
@Slf4j
@Component
public class AuthFilter implements GlobalFilter, Ordered {

    @Resource
    private TokenManager tokenManager;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    
    /**
     * 网关认证配置属性（从 application.yml 自动绑定）
     */
    private final GatewayAuthProperties gatewayAuthProperties;

    /**
     * JWT 格式正则表达式：header.payload.signature
     */
    private static final Pattern JWT_PATTERN = Pattern.compile("^[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+$");

    /**
     * 构造器注入网关认证配置属性
     *
     * @param gatewayAuthProperties 网关认证配置属性
     */
    public AuthFilter(GatewayAuthProperties gatewayAuthProperties) {
        this.gatewayAuthProperties = gatewayAuthProperties;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();
        
        // ========== 步骤 0: 获取 Trace ID（用于日志追踪） ==========
        String traceId = request.getHeaders().getFirst(TRACE_ID_HEADER);
        if (traceId == null || traceId.isEmpty()) {
            traceId = "unknown";
        }

        // ========== 步骤 1: 检查是否需要跳过验证 ==========
        if (shouldSkip(path)) {
            log.debug("[Trace] [Gateway] [{}] 跳过路径 {} 的认证", traceId, path);
            return chain.filter(exchange);
        }

        // ========== 步骤 2: 如果不启用 Token 验证，直接放行 ==========
        if (!gatewayAuthProperties.isEnableTokenCheck()) {
            log.debug("[Trace] [Gateway] [{}] Token 验证已禁用，放行路径 {}", traceId, path);
            return chain.filter(exchange);
        }

        // ========== 步骤 3: 获取 Authorization Header ==========
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        // ========== 步骤 4: 验证 Token 基础格式 ==========
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("[Trace] [Gateway] [{}] 路径 {} 缺少或无效的 Authorization header", traceId, path);
            return onError(exchange, HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7); // 移除 "Bearer " 前缀

        // ========== 步骤 5: 验证 Token 非空 ==========
        if (token.trim().isEmpty()) {
            log.warn("[Trace] [Gateway] [{}] 路径 {} 的 Token 为空", traceId, path);
            return onError(exchange, HttpStatus.UNAUTHORIZED, "Empty token");
        }

        // ========== 步骤 6: 验证 JWT 格式 ==========
        if (!JWT_PATTERN.matcher(token).matches()) {
            log.warn("[Trace] [Gateway] [{}] 路径 {} 的 JWT 格式无效", traceId, path);
            return onError(exchange, HttpStatus.UNAUTHORIZED, "Invalid JWT format");
        }

        // ========== 步骤 7: 解析并检查 Token 过期时间 ==========
        if (gatewayAuthProperties.isTokenExpiryCheck()) {
            try {
                Instant expiryTime = parseTokenExpiry(token);
                if (expiryTime != null && Instant.now().isAfter(expiryTime)) {
                    // 转换为北京时间（+8 时区）格式
                    String expiryTimeStr = expiryTime.atZone(ZoneId.of("Asia/Shanghai"))
                            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    log.warn("[Trace] [Gateway] [{}] 路径 {} 的 Token 已过期，过期时间：{}", traceId, path, expiryTimeStr);
                    return onError(exchange, HttpStatus.UNAUTHORIZED, "Token has expired");
                }
                log.debug("[Trace] [Gateway] [{}] Token 未过期，过期时间：{}", traceId, expiryTime);
            } catch (Exception e) {
                log.warn("[Trace] [Gateway] [{}] 路径 {} 解析 Token 过期时间失败：{}", traceId, path, e.getMessage());
                // 解析失败不阻断，继续后续流程
            }
        }

        // ========== 步骤 8: 检查 Token 是否在黑名单中（已撤销/已注销）==========
        if (gatewayAuthProperties.isEnableBlacklistCheck()) {
            try {
                if (tokenManager.isBlacklisted(token)) {
                    log.warn("[Trace] [Gateway] [{}] 路径 {} 的 Token 已在黑名单中（已撤销），拒绝访问", traceId, path);
                    return onError(exchange, HttpStatus.UNAUTHORIZED, "Token has been revoked");
                }
                log.debug("[Trace] [Gateway] [{}] Token 不在黑名单中，路径 {} 允许通过", traceId, path);
            } catch (Exception e) {
                log.warn("[Trace] [Gateway] [{}] 检查 Token 黑名单失败：{}", traceId, e.getMessage());
            }
        }

        // ========== 步骤 9: 验证通过，继续过滤器链 ==========
        log.debug("[Trace] [Gateway] [{}] 路径 {} 的 Token 验证通过", traceId, path);
        return chain.filter(exchange);
    }

    /**
     * 从 JWT Token 中解析过期时间
     *
     * <p>JWT 结构：header.payload.signature，本方法解码 payload 部分并提取 exp 字段</p>
     *
     * @param token JWT Token
     * @return 过期时间戳（秒），如果无法解析返回 null
     */
    private Instant parseTokenExpiry(String token) {
        try {
            // JWT 结构：header.payload.signature
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return null;
            }

            // 解码 payload 部分
            String payload = parts[1];

            // 添加 Base64 填充（Base64 URL 编码可能省略末尾的 =）
            String paddedPayload = addBase64Padding(payload);

            // 解码 payload
            byte[] decodedBytes = Base64.getUrlDecoder().decode(paddedPayload);
            String payloadJson = new String(decodedBytes, StandardCharsets.UTF_8);

            // 简单解析 JSON，查找 exp 字段（过期时间）
            // 示例：{"sub":"1234567890","name":"John Doe","exp":1516239022}
            int expIndex = payloadJson.indexOf("\"exp\":");
            if (expIndex == -1) {
                return null; // 没有 exp 字段
            }

            // 提取 exp 值
            int startIdx = expIndex + 6; // "exp": 的长度
            int endIdx = payloadJson.indexOf(",", startIdx);
            if (endIdx == -1) {
                endIdx = payloadJson.indexOf("}", startIdx);
            }

            if (endIdx == -1) {
                return null;
            }

            String expStr = payloadJson.substring(startIdx, endIdx).trim();
            long expTimestamp = Long.parseLong(expStr);

            return Instant.ofEpochSecond(expTimestamp);
        } catch (Exception e) {
            log.debug("解析 Token 过期时间失败：{}", e.getMessage());
            return null;
        }
    }

    /**
     * 为 Base64 字符串添加填充（=）
     *
     * @param base64 Base64 字符串
     * @return 添加填充后的 Base64 字符串
     */
    private String addBase64Padding(String base64) {
        int paddingNeeded = 4 - (base64.length() % 4);
        if (paddingNeeded < 4) {
            return base64 + "=".repeat(paddingNeeded);
        }
        return base64;
    }

    /**
     * 检查路径是否应该跳过验证
     * 
     * <p>使用 Ant 风格的路径匹配器进行匹配</p>
     * 
     * @param path 请求路径
     * @return true-跳过验证，false-需要验证
     */
    private boolean shouldSkip(String path) {
        var skipPaths = gatewayAuthProperties.getAnonymousUrls();
        if (skipPaths == null || skipPaths.isEmpty()) {
            return false;
        }
    
        for (String skipPath : skipPaths) {
            if (pathMatcher.match(skipPath, path)) {
                return true;
            }
        }
    
        return false;
    }
    
    /**
     * 错误响应处理
     * 
     * <p>返回统一的 JSON 格式错误响应</p>
     * 
     * @param exchange 请求上下文
     * @param status HTTP 状态码
     * @param message 错误消息
     * @return Mono<Void>
     */
    private Mono<Void> onError(ServerWebExchange exchange, HttpStatus status, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().add("Content-Type", "application/json;charset=UTF-8");
            
        String body = String.format("{\"code\":%d,\"message\":\"%s\"}", status.value(), message);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body.getBytes())));
    }

    @Override
    public int getOrder() {
        // 优先级：最高（数字越小优先级越高）
        // 在 CORS 过滤器之后执行，在其他业务过滤器之前执行
        return Ordered.HIGHEST_PRECEDENCE + 100;
    }
}
