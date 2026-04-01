package com.jnet.auth.handler;

import com.jnet.auth.cache.RedisRequestCache;
import jakarta.annotation.Resource;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;

/**
 * 自定义登录成功处理器
 * 处理登录成功后的重定向逻辑，根据 state 参数从 Redis 中获取授权参数并构建重定向 URL
 */
@Component
@Slf4j
public class CustomAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    @Resource
    private RedisRequestCache redisRequestCache;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws ServletException, IOException {

        String state = request.getParameter("state");

        // 如果没有 state 参数，使用默认处理
        if (state == null || state.isEmpty()) {
            log.warn("No state parameter found, calling super.onAuthenticationSuccess");
            super.onAuthenticationSuccess(request, response, authentication);
            return;
        }

        try {
            // 提取用户名（支持多种 UserDetails 实现）
            String username = extractUsername(authentication);
            if (username == null) {
                log.error("Failed to extract username from authentication: {}", authentication.getClass().getName());
                super.onAuthenticationSuccess(request, response, authentication);
                return;
            }

            log.info("User '{}' authenticated successfully with state: {}", username, state);

            // 第一步：更新 Redis 中的认证状态
            boolean updateSuccess = redisRequestCache.updateAuthenticationStatusByState(state, username, authentication.isAuthenticated());
            log.debug("Update login authentication status by state: {}, success: {}", state, updateSuccess);

            // 第二步：从 Redis 获取授权参数
            var savedRequest = redisRequestCache.getAuthorizationParamsFromRedis(state);

            if (savedRequest == null) {
                log.error("Saved request is null after successful update for state: {}", state);
                super.onAuthenticationSuccess(request, response, authentication);
                return;
            }

            // 第三步：构建重定向 URL
            String targetUrl = savedRequest.getOriginalUrl();

            if (targetUrl == null || targetUrl.isEmpty()) {
                log.error("Failed to build authorization URL for state: {}", state);
                super.onAuthenticationSuccess(request, response, authentication);
                return;
            }

            // 第四步：执行重定向
            log.info("Redirecting user '{}' to OAuth2 authorization endpoint: {}", username, targetUrl);
            getRedirectStrategy().sendRedirect(request, response, targetUrl);

        } catch (Exception e) {
            try {
                redisRequestCache.removeRequest(request, response);
                log.warn("Cleaned up Redis data for failed authentication with state: {}", state);
            } catch (Exception cleanupEx) {
                log.warn("Failed to cleanup Redis data after error for state: {}", state, cleanupEx);
            }
            // 使用默认处理作为降级方案
            super.onAuthenticationSuccess(request, response, authentication);
        }
    }

    /**
     * 从 Authentication 对象中提取用户名
     * 支持 Spring Security User 和其他 UserDetails 实现
     *
     * @param authentication 认证对象
     * @return 用户名，提取失败返回 null
     */
    private String extractUsername(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return null;
        }

        Object principal = authentication.getPrincipal();

        // 情况 1：Principal 是 UserDetails 类型（包括 Spring Security User）
        if (principal instanceof User userDetails) {
            return userDetails.getUsername();
        }

        // 情况 2：Principal 是字符串（某些自定义认证场景）
        if (principal instanceof String str) {
            return str;
        }

        // 情况 3：其他类型，尝试调用 getUsername 方法
        try {
            var method = principal.getClass().getMethod("getUsername");
            if (method != null) {
                return (String) method.invoke(principal);
            }
        } catch (Exception e) {
            log.debug("Failed to invoke getUsername method on principal", e);
        }

        // 无法提取，返回 null
        log.warn("Unknown principal type: {}, falling back to toString()", principal.getClass().getName());
        return principal.toString();
    }


}