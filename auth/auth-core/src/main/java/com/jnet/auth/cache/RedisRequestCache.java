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
 * 基于 Redis 的 OAuth2 授权请求缓存
 * 
 * <p>核心功能：</p>
 * <ul>
 *     <li>使用 OAuth2 标准 state 参数作为唯一标识，实现无状态授权</li>
 *     <li>将 OAuth2 授权参数存储到 Redis，设置 10 分钟过期时间</li>
 *     <li>登录成功后，通过 state 恢复授权参数，完成 OAuth2 流程</li>
 *     <li>支持 PKCE 模式，存储 code_challenge、code_verifier 等参数</li>
 * </ul>
 * 
 * <p>工作流程：</p>
 * <ol>
 *     <li>用户访问 OAuth2 授权端点，携带 state 等参数</li>
 *     <li>saveRequest() 方法将参数保存到 Redis</li>
 *     <li>用户登录后，updateAuthenticationStatusByState() 标记为已认证</li>
 *     <li>getRequest() 方法恢复授权参数，重定向回授权端点</li>
 *     <li>授权完成后，removeRequest() 清理 Redis 数据</li>
 * </ol>
 * 
 * @author mu
 * @version 1.0
 * @since 2026/4/1
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
     * 保存 OAuth2 授权请求参数到 Redis
     * 
     * <p>调用时机：用户首次访问 OAuth2 授权端点时</p>
     * <p>保存内容：response_type, client_id, redirect_uri, scope, state, code_challenge 等</p>
     * <p>过期时间：10 分钟</p>
     * 
     * @param request HTTP 请求对象
     * @param response HTTP 响应对象
     */
    @Override
    public void saveRequest(HttpServletRequest request, HttpServletResponse response) {
        String state = extractStateFromRequest(request);

        if (state == null || state.isEmpty()) {
            log.warn("缺少 state 参数，无法保存 OAuth2 授权请求");
            return;
        }

        // 检查 Redis 中是否已存在该 state 的数据
        AuthorizationParams existingParams = getAuthorizationParamsFromRedis(state);
        if (existingParams != null) {
            log.info("State [{}] 的授权参数已存在，认证状态：{}", state, existingParams.getAuthenticated());
            // 如果已存在且已认证，直接返回，不要覆盖
            if (Boolean.TRUE.equals(existingParams.getAuthenticated())) {
                log.info("用户已认证，跳过保存请求");
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

        // 存储到 Redis，使用 state 作为 key，设置 10 分钟过期时间
        commonRedisTemplate.opsForValue().set(buildRedisKey(state), params, EXPIRE_MINUTES, TimeUnit.MINUTES);

        log.info("已将 OAuth2 授权参数保存到 Redis，state: {}", state);
        log.debug("保存的参数详情：response_type={}, client_id={}, redirect_uri={}, state={}, code_challenge={}",
                params.getResponseType(), params.getClientId(), params.getRedirectUri(), params.getState(), params.getCodeChallenge());
    }

    /**
     * 构建完整的请求 URL（包含查询参数）
     * 
     * @param request HTTP 请求对象
     * @return 完整的 URL 字符串
     */
    private String buildFullUrl(HttpServletRequest request) {
        StringBuffer requestURL = request.getRequestURL();
        if (requestURL == null) {
            log.warn("请求 URL 为 null");
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
     * 从 Redis 获取授权参数（通过 state 自动构建 Redis Key）
     * 
     * @param state OAuth2 state 参数
     * @return 授权参数对象，不存在或类型不匹配时返回 null
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
            log.warn("Redis 中键 [{}] 的数据类型异常：{}，期望类型：AuthorizationParams",
                    redisKey, obj.getClass().getSimpleName());
        }
        return null;
    }

    /**
     * 根据 state 构建 Redis Key
     * 
     * @param state OAuth2 state 参数
     * @return Redis 键名
     */
    private String buildRedisKey(String state) {
        return REDIS_KEY_PREFIX + state;
    }

    /**
     * 从 HTTP 请求中提取 state 参数
     * 
     * @param request HTTP 请求对象
     * @return state 参数值，不存在返回 null
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
     * 从 Redis 恢复 OAuth2 授权请求参数
     * 
     * <p>调用时机：用户登录成功后，需要重定向回 OAuth2 授权端点时</p>
     * <p>返回值：封装了授权参数的 SavedRequest 对象，用于构建重定向 URL</p>
     * 
     * @param request HTTP 请求对象
     * @param response HTTP 响应对象
     * @return SavedRequest 对象，如果找不到参数返回 null
     */
    @Override
    public SavedRequest getRequest(HttpServletRequest request, HttpServletResponse response) {
        log.debug("尝试恢复 OAuth2 授权请求，请求 URI: {}", request.getRequestURI());

        // 尝试从请求参数中获取 state（POST /login 时从隐藏字段获取）
        String state = extractStateFromRequest(request);

        if (state == null || state.isEmpty()) {
            log.warn("在请求参数或 Cookie 中未找到 state 参数，无法恢复授权请求");
            return null;
        }

        // 从 Redis 读取授权参数（直接传入 state，方法内部会自动构建 Redis Key）
        AuthorizationParams params = getAuthorizationParamsFromRedis(state);

        if (params == null) {
            log.warn("在 Redis 中未找到 state [{}] 的授权参数", state);
            return null;
        }

        log.info("已从 Redis 恢复 OAuth2 授权参数，state: {}，标记为已认证", state);
        log.debug("恢复的参数详情：response_type={}, client_id={}, redirect_uri={}, authenticated={}",
                params.getResponseType(), params.getClientId(), params.getRedirectUri(), params.getAuthenticated());

        // 创建 SavedRequest 对象，用于后续重定向
        return new RedisSavedRequest(params);
    }

    /**
     * 根据 state 更新用户认证状态
     * 
     * <p>调用时机：用户登录成功后</p>
     * <p>作用：标记 Redis 中的授权参数为已认证，并保存用户名</p>
     * 
     * @param state OAuth2 state 参数
     * @param principal 用户名（principal）
     * @param authenticated 认证状态（true=已认证）
     * @return 是否更新成功
     */
    public boolean updateAuthenticationStatusByState(String state, String principal, boolean authenticated) {
        if (state == null || state.isEmpty()) {
            log.warn("无效的 state 参数");
            return false;
        }
        AuthorizationParams params = getAuthorizationParamsFromRedis(state);
        if (params == null) {
            log.warn("未找到 state [{}] 对应的授权参数", state);
            return false;
        }
        params.setAuthenticated(authenticated);
        params.setPrincipal(principal);
        String redisKey = buildRedisKey(state);
        commonRedisTemplate.opsForValue().set(redisKey, params, EXPIRE_MINUTES, TimeUnit.MINUTES);

        log.info("已更新 state [{}] 的认证状态为：{}，用户：{}", state, authenticated, principal);
        return true;
    }


    @Override
    public HttpServletRequest getMatchingRequest(HttpServletRequest request, HttpServletResponse response) {
        return null;
    }

    /**
     * 移除已使用的 OAuth2 授权参数
     * 
     * <p>调用时机：OAuth2 授权流程完成后，清理临时数据</p>
     * 
     * @param request HTTP 请求对象
     * @param response HTTP 响应对象
     */
    @Override
    public void removeRequest(HttpServletRequest request, HttpServletResponse response) {
        String state = extractStateFromRequest(request);
        if (state != null && !state.isEmpty()) {
            String redisKey = buildRedisKey(state);
            commonRedisTemplate.delete(redisKey);
            log.info("已从 Redis 移除 OAuth2 授权参数，state: {}", state);
        }
    }


    /**
     * OAuth2 授权参数封装类
     * 
     * <p>用于存储 OAuth2 授权请求的所有参数，包括：</p>
     * <ul>
     *     <li>基础参数：response_type, client_id, redirect_uri, scope</li>
     *     <li>PKCE 参数：code_challenge, code_challenge_method, code_verifier</li>
     *     <li>状态参数：state, authenticated, principal</li>
     *     <li>辅助信息：originalUrl, registeredClient</li>
     * </ul>
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
     * SavedRequest 实现类，用于恢复 OAuth2 授权请求
     * 
     * <p>封装了从 Redis 中恢复的授权参数，提供 Spring Security 标准接口</p>
     * <p>主要用于构建重定向 URL 和恢复请求参数</p>
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
