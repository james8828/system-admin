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
 * OAuth2 State 认证过滤器
 * 
 * <p>核心功能：在 OAuth2 回调流程中，通过 Redis 中的认证标记恢复用户认证状态</p>
 * 
 * <p>工作原理：</p>
 * <ol>
 *     <li>用户登录成功后，Redis 中会标记为已认证（authenticated=true）</li>
 *     <li>当请求携带 state 参数时，从 Redis 读取认证状态</li>
 *     <li>如果已认证，创建 Authentication 对象并设置到 SecurityContext</li>
 *     <li>这样无需重新登录即可完成 OAuth2 授权流程</li>
 * </ol>
 * 
 * @author mu
 * @version 1.0
 * @since 2026/4/1
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
            // 没有 state 参数，跳过处理
            filterChain.doFilter(request, response);
            return;
        }

        // 检查 Redis 中是否有认证标记
        var params = redisRequestCache.getAuthorizationParamsFromRedis(state);

        if (params != null && Boolean.TRUE.equals(params.getAuthenticated())) {
            log.info("通过 Redis State 发现用户已认证，state: {}，创建认证令牌", state);

            // 创建临时 Authentication，使用真实的用户名
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    params.getPrincipal(), // 使用标识用户名
                    null,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
            );

            // 设置额外的详细信息（客户端 ID）
            auth.setDetails(params.getClientId());
            SecurityContextHolder.getContext().setAuthentication(auth);
            log.info("已为 state [{}] 设置安全上下文，用户：{}", state, params.getPrincipal());
        } else {
            log.debug("在 Redis 中未找到 state [{}] 的认证信息。参数存在：{}, 认证状态：{}",
                state, params != null, params != null ? params.getAuthenticated() : "N/A");
        }
        
        filterChain.doFilter(request, response);
    }
}
