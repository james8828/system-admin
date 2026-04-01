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
 * 自定义认证成功处理器
 * 
 * <p>核心功能：处理用户登录成功后的 OAuth2 授权流程</p>
 * 
 * <p>处理流程：</p>
 * <ol>
 *     <li>从请求中提取 state 参数</li>
 *     <li>更新 Redis 中的认证状态为已认证</li>
 *     <li>从 Redis 获取 OAuth2 授权参数</li>
 *     <li>构建重定向 URL（原始授权端点）</li>
 *     <li>重定向用户到 OAuth2 授权端点，继续授权流程</li>
 * </ol>
 * 
 * <p>异常处理：如果处理失败，会清理 Redis 数据并使用默认处理器作为降级方案</p>
 * 
 * @author mu
 * @version 1.0
 * @since 2026/4/1
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

        // 如果没有 state 参数，使用默认处理（跳转到默认目标 URL）
        if (state == null || state.isEmpty()) {
            log.warn("未找到 state 参数，使用默认认证成功处理器");
            super.onAuthenticationSuccess(request, response, authentication);
            return;
        }

        try {
            // 提取用户名（支持多种 UserDetails 实现）
            String username = extractUsername(authentication);
            if (username == null) {
                log.error("无法从认证对象中提取用户名，类型：{}", authentication.getClass().getName());
                super.onAuthenticationSuccess(request, response, authentication);
                return;
            }

            log.info("用户 '{}' 登录成功，state: {}", username, state);

            // 第一步：更新 Redis 中的认证状态为已认证
            boolean updateSuccess = redisRequestCache.updateAuthenticationStatusByState(state, username, authentication.isAuthenticated());
            log.debug("更新 state [{}] 的认证状态：{}", state, updateSuccess);

            // 第二步：从 Redis 获取 OAuth2 授权参数
            var savedRequest = redisRequestCache.getAuthorizationParamsFromRedis(state);

            if (savedRequest == null) {
                log.error("在 Redis 中未找到 state [{}] 的授权参数", state);
                super.onAuthenticationSuccess(request, response, authentication);
                return;
            }

            // 第三步：构建重定向 URL（原始 OAuth2 授权端点）
            String targetUrl = savedRequest.getOriginalUrl();

            if (targetUrl == null || targetUrl.isEmpty()) {
                log.error("无法为重定向构建授权 URL，state: {}", state);
                super.onAuthenticationSuccess(request, response, authentication);
                return;
            }

            // 第四步：执行重定向到 OAuth2 授权端点
            log.info("正在重定向用户 '{}' 到 OAuth2 授权端点：{}", username, targetUrl);
            getRedirectStrategy().sendRedirect(request, response, targetUrl);

        } catch (Exception e) {
            try {
                // 清理 Redis 数据
                redisRequestCache.removeRequest(request, response);
                log.warn("认证失败，已清理 Redis 数据，state: {}", state);
            } catch (Exception cleanupEx) {
                log.warn("清理 Redis 数据失败，state: {}", state, cleanupEx);
            }
            // 使用默认处理器作为降级方案
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