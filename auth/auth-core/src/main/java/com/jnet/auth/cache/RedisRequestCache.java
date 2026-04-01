package com.jnet.auth.cache;

import jakarta.annotation.Resource;
import jakarta.servlet.http.Cookie;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.Serial;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 基于 Redis 的请求缓存，实现无状态的 OAuth2 PKCE 授权
 * <p>
 * 核心思路：
 * 1. 使用 OAuth2 标准 state 参数作为唯一标识，替代传统的 Session
 * 2. 将 OAuth2 授权参数存储到 Redis，设置合理的过期时间
 * 3. 登录成功后，通过 state 恢复授权参数
 * </p>
 */
@Slf4j
@Component
public class RedisRequestCache implements RequestCache {

    private static final String REDIS_KEY_PREFIX = "oauth2:authorization:";
    private static final long EXPIRE_MINUTES = 10; // OAuth2 授权参数过期时间
    private static final String STATE = "state";

    @Resource
    private RedisTemplate<String, Object> commonRedisTemplate;

    /**
     * 保存授权请求参数到 Redis
     */
    @Override
    public void saveRequest(HttpServletRequest request, HttpServletResponse response) {
        String state = extractStateFromRequest(request);

        if (state == null || state.isEmpty()) {
            log.warn("Missing state parameter, cannot save request");
            return;
        }

        // 检查 Redis 中是否已存在该 state 的数据
        AuthorizationParams existingParams = getAuthorizationParamsFromRedis(state);
        if (existingParams != null) {
            log.info("Authorization params already exist for state: {}, authenticated: {}",
                    state, existingParams.getAuthenticated());
            // 如果已存在且已认证，直接返回，不要覆盖
            if (Boolean.TRUE.equals(existingParams.getAuthenticated())) {
                log.info("User already authenticated, skipping saveRequest");
                return;
            }
        }

        // 提取 OAuth2 授权参数
        AuthorizationParams params = new AuthorizationParams();
        params.setResponseType(request.getParameter("response_type"));

        String clientId = request.getParameter("client_id");
        params.setClientId(clientId);
        params.setRedirectUri(request.getParameter("redirect_uri"));
        params.setScope(request.getParameter("scope"));
        params.setState(state);
        params.setCodeChallenge(request.getParameter("code_challenge"));
        params.setCodeChallengeMethod(request.getParameter("code_challenge_method"));
        params.setCodeVerifier(request.getParameter("code_verifier"));

        // 保存完整的请求 URL，包含所有参数
        String fullUrl = buildFullUrl(request);
        params.setOriginalUrl(fullUrl);

        // 存储到 Redis，使用 state 作为 key，设置过期时间
        commonRedisTemplate.opsForValue().set(buildRedisKey(state), params, EXPIRE_MINUTES, TimeUnit.MINUTES);

        log.info("Saved OAuth2 authorization params to Redis with state: {}", state);
        log.debug("Authorization params saved: response_type={}, client_id={}, redirect_uri={}, state={}, code_challenge={}",
                params.getResponseType(), params.getClientId(), params.getRedirectUri(), params.getState(), params.getCodeChallenge());
    }

    /**
     * 构建完整的请求 URL
     */
    private String buildFullUrl(HttpServletRequest request) {
        StringBuffer requestURL = request.getRequestURL();
        if (requestURL == null) {
            log.warn("Request URL is null");
            return "";
        }

        String fullUrl = requestURL.toString();
        String queryString = request.getQueryString();

        if (queryString != null && !queryString.isEmpty()) {
            fullUrl += "?" + queryString;
        }

        return fullUrl;
    }

    /**
     * 从 Redis 获取 AuthorizationParams（通过 state 自动构建 Redis Key）
     */
    public AuthorizationParams getAuthorizationParamsFromRedis(String state) {
        String redisKey = buildRedisKey(state);
        Object obj = commonRedisTemplate.opsForValue().get(redisKey);
        // 使用 instanceof 模式匹配，避免 ClassCastException
        if (obj instanceof AuthorizationParams params) {
            return params;
        }
        // 如果是其他类型，记录警告
        if (obj != null) {
            log.warn("Unexpected type in Redis for key {}: {}. Expected AuthorizationParams.",
                    redisKey, obj.getClass().getSimpleName());
        }
        return null;
    }

    /**
     * 构建 Redis Key
     */
    private String buildRedisKey(String state) {
        return REDIS_KEY_PREFIX + state;
    }

    /**
     * 从请求中提取 state 参数
     */
    private String extractStateFromRequest(HttpServletRequest request) {
        // 优先从请求参数获取
        String state = request.getParameter(STATE);
        if (state != null && !state.isEmpty()) {
            return state;
        }
        return null;

    }

    /**
     * 从 Redis 恢复授权请求参数，并标记为已认证
     */
    @Override
    public SavedRequest getRequest(HttpServletRequest request, HttpServletResponse response) {
        log.debug("Attempting to restore OAuth2 request. Request URI: {}", request.getRequestURI());

        // 尝试从请求参数中获取 state（POST /login 时从隐藏字段获取）
        String state = extractStateFromRequest(request);

        if (state == null || state.isEmpty()) {
            log.warn("No state parameter found in request parameters or cookies, cannot restore request");
            return null;
        }

        // 从 Redis 读取授权参数（直接传入 state，方法内部会自动构建 Redis Key）
        AuthorizationParams params = getAuthorizationParamsFromRedis(state);

        if (params == null) {
            log.warn("Authorization params not found in Redis for state: {}", state);
            return null;
        }

        log.info("Restored OAuth2 authorization params from Redis with state: {} and marked as authenticated", state);
        log.debug("Authorization params after update: response_type={}, client_id={}, redirect_uri={}, authenticated={}",
                params.getResponseType(), params.getClientId(), params.getRedirectUri(), params.getAuthenticated());

        // 创建 SavedRequest 对象
        return new RedisSavedRequest(params);
    }

    /**
     * 根据 state 直接更新认证状态
     *
     * @param state         OAuth2 state 参数
     * @param authenticated 认证状态
     * @return 是否更新成功
     */
    public boolean updateAuthenticationStatusByState(String state, String principal, boolean authenticated) {
        if (state == null || state.isEmpty()) {
            log.warn("Invalid state parameter");
            return false;
        }
        AuthorizationParams params = getAuthorizationParamsFromRedis(state);
        if (params == null) {
            log.warn("AuthorizationParams not found for state: {}", state);
            return false;
        }
        params.setAuthenticated(authenticated);
        params.setPrincipal(principal);
        String redisKey = buildRedisKey(state);
        commonRedisTemplate.opsForValue().set(redisKey, params, EXPIRE_MINUTES, TimeUnit.MINUTES);

        log.info("Updated authentication status to {} for state: {}", authenticated, state);
        return true;
    }


    @Override
    public HttpServletRequest getMatchingRequest(HttpServletRequest request, HttpServletResponse response) {
        return null;
    }

    /**
     * 移除已使用的授权参数
     */
    @Override
    public void removeRequest(HttpServletRequest request, HttpServletResponse response) {
        String state = extractStateFromRequest(request);
        if (state != null && !state.isEmpty()) {
            String redisKey = buildRedisKey(state);
            commonRedisTemplate.delete(redisKey);
            log.info("Removed OAuth2 authorization params from Redis with state: {}", state);
        }
    }


    /**
     * OAuth2 授权参数封装类
     */
    @Data
    public static class AuthorizationParams implements java.io.Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private String responseType;
        private String clientId;
        private String redirectUri;
        private String principal;
        private String scope;
        private String state;
        private String codeChallenge;
        private String codeChallengeMethod;
        private String codeVerifier; // PKCE code verifier
        private String originalUrl;
        private Boolean authenticated; // 标记用户是否已认证
        private org.springframework.security.oauth2.server.authorization.client.RegisteredClient registeredClient; // 客户端信息
    }

    /**
     * SavedRequest 实现，用于恢复授权请求
     */
    public static class RedisSavedRequest implements SavedRequest {
        private final AuthorizationParams params;

        public RedisSavedRequest(AuthorizationParams params) {
            this.params = params;
        }

        @Override
        public String getRedirectUrl() {
            return params.getOriginalUrl();
        }

        @Override
        public List<Cookie> getCookies() {
            return List.of();
        }

        @Override
        public String[] getParameterValues(String name) {
            String value = switch (name) {
                case "response_type" -> params.getResponseType();
                case "client_id" -> params.getClientId();
                case "redirect_uri" -> params.getRedirectUri();
                case "scope" -> params.getScope();
                case "state" -> params.getState();
                case "code_challenge" -> params.getCodeChallenge();
                case "code_challenge_method" -> params.getCodeChallengeMethod();
                case "code_verifier" -> params.getCodeVerifier();
                default -> null;
            };

            return value != null ? new String[]{value} : null;
        }

        @Override
        public Map<String, String[]> getParameterMap() {
            // 返回所有参数的 Map
            return Collections.emptyMap();
        }

        @Override
        public String getMethod() {
            return "GET";
        }

        @Override
        public List<String> getHeaderValues(String name) {
            return List.of();
        }

        @Override
        public Collection<String> getHeaderNames() {
            return List.of();
        }

        @Override
        public List<Locale> getLocales() {
            return List.of();
        }
    }
}
