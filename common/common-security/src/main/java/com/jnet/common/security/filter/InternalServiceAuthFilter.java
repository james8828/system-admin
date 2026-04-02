package com.jnet.common.security.filter;

import com.jnet.common.security.config.InternalAuthProperties;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.jnet.common.constant.Constants.*;

/**
 * 内部服务认证过滤器
 * 
 * <p>验证来自其他内部服务的请求，允许通过共享密钥进行快速认证</p>
 * 
 * <h3>工作原理：</h3>
 * <ol>
 *     <li>检查请求头中是否包含内部服务认证信息（X-Service-Token, X-Service-Name）</li>
 *     <li>验证服务名称是否在允许列表中</li>
 *     <li>验证服务密钥是否匹配</li>
 *     <li>认证成功后设置 SecurityContext，允许跳过 JWT 验证</li>
 * </ol>
 * 
 * <h3>使用场景：</h3>
 * <ul>
 *     <li>Gateway → Auth Service（绕过 JWT 验证）</li>
 *     <li>Gateway → System Service（绕过 JWT 验证）</li>
 *     <li>微服务间的 Feign 调用</li>
 * </ul>
 * 
 * @author mu
 * @version 1.0
 * @since 2026/4/1
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class InternalServiceAuthFilter implements Filter {
    
    private final InternalAuthProperties authProperties;
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String serviceToken = httpRequest.getHeader(INTERNAL_SERVICE_AUTH_SERVICE_TOKEN);
        String serviceName = httpRequest.getHeader(INTERNAL_SERVICE_AUTH_SERVICE_NAME);
        
        String requestURI = httpRequest.getRequestURI();
        
        // 获取 TraceId（用于链路追踪）
        String traceId = httpRequest.getHeader(TRACE_ID_HEADER);
        if (traceId == null || traceId.isEmpty()) {
            traceId = "unknown";
        }
        
        log.debug("[Trace] [Security] [{}] 内部服务认证检查 - URI: {}, 服务：{}", traceId, requestURI, serviceName);
        
        // 检查是否是内部服务请求（通过检查是否存在内部服务认证头）
        boolean hasInternalServiceHeader = httpRequest.getHeader(INTERNAL_SERVICE_FLAG) != null;
        
        if (hasInternalServiceHeader || StringUtils.hasText(serviceToken)) {
            // 如果有内部服务认证头，进行验证
            if (isValidInternalService(serviceToken, serviceName)) {
                log.debug("[Trace] [Security] [{}] 内部服务认证成功 - 服务：{}", traceId, serviceName);
                
                // 设置内部服务认证
                List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(
                        "internal-service-" + serviceName, 
                        null, 
                        authorities
                    );
                SecurityContextHolder.getContext().setAuthentication(authentication);
                request.setAttribute(INTERNAL_SERVICE_AUTHENTICATED, true);
                // 认证成功，继续执行过滤器链
                chain.doFilter(request, response);
                return;
            } else {
                // 认证失败，记录日志并返回 401
                log.warn("[Trace] [Security] [{}] 内部服务认证失败 - 服务：{}, Token 是否存在：{}", traceId, serviceName, serviceToken != null);
                ((jakarta.servlet.http.HttpServletResponse) response).sendError(401, "Internal service authentication failed");
                return;
            }
        }
        
        // 没有内部服务认证头，继续执行后续过滤器（如 JWT 验证）
        log.debug("[Trace] [Security] [{}] 无内部服务认证头，继续执行 JWT 验证流程", traceId);
        chain.doFilter(request, response);
    }
    
    /**
     * 验证内部服务请求的合法性
     * 
     * @param token 服务密钥 Token
     * @param serviceName 服务名称
     * @return true-验证通过，false-验证失败
     */
    private boolean isValidInternalService(String token, String serviceName) {
        if (!StringUtils.hasText(token) || !StringUtils.hasText(serviceName)) {
            return false;
        }
        
        // 验证服务名称是否被允许
        List<String> allowedServicesList = Arrays.asList(authProperties.getAllowedServices().split(","));
        boolean isAllowed = allowedServicesList.contains(serviceName);
        
        // 验证令牌（这里简化为简单的密钥验证）
        return isAllowed && authProperties.getSecret().equals(token);
    }
}
