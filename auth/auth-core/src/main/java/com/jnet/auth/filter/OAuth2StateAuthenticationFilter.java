package com.jnet.auth.filter;

import com.jnet.auth.cache.RedisRequestCache;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Collections;

/**
 * OAuth2 状态认证过滤器
 * 通过 Redis 中的 state 标记判断用户是否已认证，实现无状态的跨域授权流程
 */
@Slf4j
@RequiredArgsConstructor
public class OAuth2StateAuthenticationFilter extends OncePerRequestFilter {

    private final RedisRequestCache redisRequestCache;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String state = request.getParameter("state");
        if (state == null || state.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        // 检查 Redis 中是否有认证标记
        var params = redisRequestCache.getAuthorizationParamsFromRedis(state);

        if (params != null && Boolean.TRUE.equals(params.getAuthenticated())) {
            log.info("User authenticated via Redis state for state: {}, creating authentication token", state);

            // 创建临时 Authentication，使用真实的用户名（如果需要，可以从 params 中获取）
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    params.getPrincipal(), // 使用标识用户名
                    null,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
            );

            // 设置额外的详细信息
            auth.setDetails(params.getClientId());
            SecurityContextHolder.getContext().setAuthentication(auth);
            log.info("SecurityContext set with authenticated user for state: {}", state);
        } else {
            log.debug("No authentication found in Redis for state: {}. Params exists: {}, authenticated value: {}",
                state, params != null, params != null ? params.getAuthenticated() : "N/A");
        }
        
        filterChain.doFilter(request, response);
    }
}
