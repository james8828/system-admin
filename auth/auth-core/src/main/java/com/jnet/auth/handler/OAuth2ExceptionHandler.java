package com.jnet.auth.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * OAuth2 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class OAuth2ExceptionHandler {

    /**
     * 处理 OAuth2 授权码请求异常
     */
    @ExceptionHandler(OAuth2AuthorizationCodeRequestAuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthorizationCodeRequestException(
            OAuth2AuthorizationCodeRequestAuthenticationException ex) {
        
        log.error("OAuth2 授权码请求失败", ex);
        log.error("错误详情 - Error: {}, ErrorDescription: {}", 
            ex.getError(), ex.getError().getDescription());
        
        // 获取请求参数
        var request = ((org.springframework.web.context.request.ServletRequestAttributes) 
            org.springframework.web.context.request.RequestContextHolder.getRequestAttributes()).getRequest();
        
        log.error("请求参数 - response_type: {}, client_id: {}, redirect_uri: {}, scope: {}, state: {}, code_challenge: {}",
            request.getParameter("response_type"),
            request.getParameter("client_id"),
            request.getParameter("redirect_uri"),
            request.getParameter("scope"),
            request.getParameter("state"),
            request.getParameter("code_challenge")
        );
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", ex.getError().getDescription());
        errorResponse.put("error_description", ex.getError().getDescription());
        errorResponse.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * 处理其他 OAuth2 认证异常
     */
    @ExceptionHandler(OAuth2AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleOAuth2AuthenticationException(
            OAuth2AuthenticationException ex) {
        
        log.error("OAuth2 认证失败", ex);
        log.error("错误详情 - Error: {}, ErrorDescription: {}", 
            ex.getError(), ex.getError().getDescription());
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", ex.getError().getDescription());
        errorResponse.put("error_description", ex.getError().getDescription());
        errorResponse.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }
}
