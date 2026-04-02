package com.jnet.auth.service;

import com.jnet.auth.utils.OAuth2SettingsUtil;
import com.jnet.common.redis.TokenManager;
import com.jnet.common.result.Result;
import com.jnet.system.api.client.OAuth2AuthorizationFeignClient;
import com.jnet.system.api.dto.OAuth2AuthorizationDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;


/**
 * 基于 Feign 的 OAuth2 授权服务实现
 * 
 * <p>核心功能：通过 Feign Client 远程调用 system-admin 服务，实现 OAuth2 授权的持久化存储</p>
 * 
 * <p>主要职责：</p>
 * <ul>
 *     <li>保存 OAuth2 授权信息（Authorization Code、Access Token、Refresh Token）</li>
 *     <li>删除/撤销 OAuth2 授权</li>
 *     <li>根据 ID 查询授权信息</li>
 *     <li>根据 Token 值查询授权信息（支持 access_token、refresh_token、code 等类型）</li>
 * </ul>
 * 
 * <p>数据转换：</p>
 * <ul>
 *     <li>save() 时将 OAuth2Authorization 转换为 OAuth2AuthorizationDTO</li>
 *     <li>findById()/findByToken() 时将 DTO 转换回 OAuth2Authorization</li>
 *     <li>处理复杂的属性序列化（如 OAuth2AuthorizationRequest、Principal 等）</li>
 * </ul>
 * 
 * @author mu
 * @version 1.0
 * @since 2026/4/1
 */
@Slf4j
@Service
public class FeignOAuth2AuthorizationService implements OAuth2AuthorizationService {

    @Resource
    private OAuth2AuthorizationFeignClient oauth2AuthorizationFeignClient;

    @Resource
    private FeignRegisteredClientService feignRegisteredClientService;

    @Resource
    private TokenManager tokenManager;


    /**
     * 保存 OAuth2 授权信息到远程数据库
     * 
     * <p>调用时机：生成 Authorization Code、Access Token 或 Refresh Token 时</p>
     * <p>存储内容：完整的 OAuth2Authorization 对象，包括所有 Token 和属性</p>
     * 
     * @param authorization Spring Security OAuth2Authorization 对象
     */
    @Override
    public void save(OAuth2Authorization authorization) {
        try {
            log.info("正在保存 OAuth2 授权信息，id={}, principal={}",
                    authorization.getId(), authorization.getPrincipalName());

            OAuth2AuthorizationDTO dto = convertToDTO(authorization);
            Result<Void> result = oauth2AuthorizationFeignClient.saveAuthorization(dto);

            if (result != null && result.isSuccess()) {
                // 只在 Access Token 存在时才存储到 Redis（第二次调用）
                if (dto.getAccessTokenValue() != null && !dto.getAccessTokenValue().isEmpty()) {
                    // 计算过期时间（秒）
                    long expireSeconds = Duration.between(
                            Instant.now(),
                            dto.getAccessTokenExpiresAt().atZone(java.time.ZoneId.systemDefault()).toInstant()
                    ).getSeconds();

                    tokenManager.storeToken(dto.getAccessTokenValue(), dto.getPrincipalName(), expireSeconds);
                    log.info("✓ Token 已存储到 Redis: token={}, user={}, expire={}s",
                            dto.getAccessTokenValue(), dto.getPrincipalName(), expireSeconds);
                } else {
                    log.debug("第一次调用：仅保存授权码，Access Token 尚未生成");
                }
            }
        } catch (Exception e) {
            log.error("保存 OAuth2 授权信息失败", e);
            throw new RuntimeException("保存 OAuth2 授权信息失败", e);
        }
    }


    /**
     * 删除/撤销 OAuth2 授权信息
     * 
     * <p>调用时机：用户登出、Token 撤销或授权过期时</p>
     * 
     * @param authorization 要删除的授权对象
     */
    @Override
    public void remove(OAuth2Authorization authorization) {
        try {
            log.info("正在删除 OAuth2 授权信息，id={}", authorization.getId());

            // 调用 Feign Client 撤销授权
            Result<Void> result = oauth2AuthorizationFeignClient.revokeAuthorization(authorization.getId());

            if (result != null && result.isSuccess()) {
                log.info("成功删除授权：{}", authorization.getId());
            } else {
                log.warn("删除授权失败：{}, 返回结果={}", authorization.getId(), result);
            }

        } catch (Exception e) {
            log.error("删除 OAuth2 授权信息失败", e);
            throw new RuntimeException("删除 OAuth2 授权信息失败", e);
        }
    }

    /**
     * 根据 ID 查找 OAuth2 授权信息
     * 
     * <p>调用时机：恢复会话、验证 Token 时</p>
     * 
     * @param id 授权 ID
     * @return OAuth2Authorization 对象，不存在返回 null
     */
    @Override
    public OAuth2Authorization findById(String id) {
        try {
            log.debug("正在通过 ID 查询 OAuth2 授权信息：{}", id);

            Result<OAuth2AuthorizationDTO> result = oauth2AuthorizationFeignClient.getAuthorizationById(id);

            if (result == null || !result.isSuccess() || result.getData() == null) {
                log.debug("未找到 ID 为 {} 的授权信息", id);
                return null;
            }

            OAuth2AuthorizationDTO dto = result.getData();
            OAuth2Authorization authorization = convertToAuthorization(dto);

            log.debug("成功找到授权信息，id={}, principal={}",
                    dto.getId(), dto.getPrincipalName());

            return authorization;

        } catch (Exception e) {
            log.error("通过 ID 查询 OAuth2 授权信息失败，id={}", id, e);
            return null;
        }
    }

    /**
     * 根据 Token 值查找 OAuth2 授权信息
     * 
     * <p>调用时机：Resource Server 验证 Token、刷新 Token 时</p>
     * <p>支持的 Token 类型：access_token、refresh_token、authorization_code 等</p>
     * 
     * @param tokenValue Token 值
     * @param tokenType  Token 类型（access_token, refresh_token, code 等）
     * @return OAuth2Authorization 对象，不存在返回 null
     */
    @Override
    public OAuth2Authorization findByToken(String tokenValue, OAuth2TokenType tokenType) {
        try {
            if (tokenValue == null || tokenType == null) {
                log.debug("Token 值或类型为 null");
                return null;
            }

            String tokenTypeValue = tokenType.getValue();
            log.debug("正在通过 Token 查询授权信息，value={}, type={}",
                    tokenValue, tokenTypeValue);

            // 调用 Feign Client 查询
            Result<OAuth2AuthorizationDTO> result = oauth2AuthorizationFeignClient.getAuthorizationByToken(tokenValue, tokenTypeValue);

            if (result == null || !result.isSuccess() || result.getData() == null) {
                log.debug("未找到 Token 类型为 {} 的授权信息", tokenTypeValue);
                return null;
            }

            OAuth2AuthorizationDTO dto = result.getData();
            OAuth2Authorization authorization = convertToAuthorization(dto);

            log.debug("成功找到 Token 对应的授权，type={}, principal={}",
                    tokenTypeValue, dto.getPrincipalName());

            return authorization;

        } catch (Exception e) {
            log.error("通过 Token 查询 OAuth2 授权信息失败", e);
            return null;
        }
    }

    /**
     * 将 OAuth2Authorization 转换为 OAuth2AuthorizationDTO
     *
     * @param authorization Spring Security 授权对象
     * @return 数据传输对象
     */
    private OAuth2AuthorizationDTO convertToDTO(OAuth2Authorization authorization) {
        OAuth2AuthorizationDTO dto = new OAuth2AuthorizationDTO();

        // 基础信息
        dto.setId(authorization.getId());
        dto.setRegisteredClientId(authorization.getRegisteredClientId());
        dto.setPrincipalName(authorization.getPrincipalName());

        // 安全处理：添加 null 检查
        org.springframework.security.oauth2.core.AuthorizationGrantType grantType = authorization.getAuthorizationGrantType();
        if (grantType != null) {
            dto.setAuthorizationGrantType(grantType.getValue());
        }

        // 授权范围 - 直接存储 Set
        Set<String> scopes = authorization.getAuthorizedScopes();
        if (scopes != null && !scopes.isEmpty()) {
            dto.setAuthorizedScopes(scopes);
        }

        // 属性 - 手动处理序列化，添加类型标识
        Map<String, Object> attributes = authorization.getAttributes();
        dto.setAttributes(attributes);
        dto.setState(authorization.getAttribute(OAuth2ParameterNames.STATE));

        // 优化：提取 Token 处理逻辑
        processAuthorizationCode(authorization, dto);
        processAccessToken(authorization, dto);
        processRefreshToken(authorization, dto);

        return dto;
    }

    /**
     * 处理授权码 Token 信息
     */
    private void processAuthorizationCode(OAuth2Authorization authorization, OAuth2AuthorizationDTO dto) {
        var authorizationCodeToken = authorization.getToken(OAuth2AuthorizationCode.class);
        if (authorizationCodeToken != null) {
            var code = authorizationCodeToken.getToken();
            if (code != null) {
                dto.setAuthorizationCodeValue(code.getTokenValue());
                dto.setAuthorizationCodeIssuedAt(OAuth2SettingsUtil.localDateTimeFromInstant(code.getIssuedAt()));
                dto.setAuthorizationCodeExpiresAt(OAuth2SettingsUtil.localDateTimeFromInstant(code.getExpiresAt()));
            }
            dto.setAuthorizationCodeMetadata(authorizationCodeToken.getMetadata());
        }
    }

    /**
     * 处理 Access Token 信息
     */
    private void processAccessToken(OAuth2Authorization authorization, OAuth2AuthorizationDTO dto) {
        var accessTokenToken = authorization.getToken(OAuth2AccessToken.class);
        if (accessTokenToken != null) {
            var accessToken = accessTokenToken.getToken();
            if (accessToken != null) {
                dto.setAccessTokenValue(accessToken.getTokenValue());
                dto.setAccessTokenIssuedAt(OAuth2SettingsUtil.localDateTimeFromInstant(accessToken.getIssuedAt()));
                dto.setAccessTokenExpiresAt(OAuth2SettingsUtil.localDateTimeFromInstant(accessToken.getExpiresAt()));
                dto.setAccessTokenScopes(accessToken.getScopes());
            }
            dto.setAccessTokenMetadata(accessTokenToken.getMetadata());
        }
    }

    /**
     * 处理 Refresh Token 信息
     */
    private void processRefreshToken(OAuth2Authorization authorization, OAuth2AuthorizationDTO dto) {
        var refreshTokenToken = authorization.getToken(OAuth2RefreshToken.class);
        if (refreshTokenToken != null) {
            var refreshToken = refreshTokenToken.getToken();
            if (refreshToken != null) {
                dto.setRefreshTokenValue(refreshToken.getTokenValue());
                dto.setRefreshTokenIssuedAt(OAuth2SettingsUtil.localDateTimeFromInstant(refreshToken.getIssuedAt()));
                dto.setRefreshTokenExpiresAt(OAuth2SettingsUtil.localDateTimeFromInstant(refreshToken.getExpiresAt()));
            }
            dto.setRefreshTokenMetadata(refreshTokenToken.getMetadata());
        }
    }


    /**
     * 将 OAuth2AuthorizationDTO 转换为 OAuth2Authorization
     *
     * @param dto 数据传输对象
     * @return Spring Security 授权对象
     */
    private OAuth2Authorization convertToAuthorization(OAuth2AuthorizationDTO dto) {
        // 第一步：先获取 RegisteredClient
        RegisteredClient registeredClient = getRegisteredClientById(dto.getRegisteredClientId());

        // 安全检查：registeredClient 不能为 null
        if (registeredClient == null) {
            log.warn("RegisteredClient 未找到，id: {}，将使用最小化客户端", dto.getRegisteredClientId());
        }

        // 第二步：使用 RegisteredClient 构建 OAuth2Authorization
        assert registeredClient != null;
        OAuth2Authorization.Builder builder = OAuth2Authorization.withRegisteredClient(registeredClient);

        // 设置基础信息
        builder.id(dto.getId())
                .principalName(dto.getPrincipalName());

        // 安全处理：添加 null 检查
        if (dto.getAuthorizationGrantType() != null) {
            builder.authorizationGrantType(new AuthorizationGrantType(dto.getAuthorizationGrantType()));
        }

        // 添加授权范围
        if (dto.getAuthorizedScopes() != null) {
            builder.authorizedScopes(dto.getAuthorizedScopes());
        }

        if (dto.getAttributes() != null && !dto.getAttributes().isEmpty()) {
            try {
                for (Map.Entry<String, Object> entry : dto.getAttributes().entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();

                    if (value instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> valueMap = (Map<String, Object>) value;

                        // 检查是否是 OAuth2AuthorizationRequest
                        if ("org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest".equals(key)) {
                            try {
                                OAuth2AuthorizationRequest.Builder oAuth2AuthorizationRequestBuilder = OAuth2AuthorizationRequest.authorizationCode();

                                // 从 Map 中读取所有字段（使用标准 Java Bean 属性名）
                                String authorizationUri = getStringValue(valueMap, "authorizationUri");
                                String clientId = getStringValue(valueMap, "clientId");
                                String redirectUri = getStringValue(valueMap, "redirectUri");
                                String state = getStringValue(valueMap, OAuth2ParameterNames.STATE);
                                String authorizationRequestUri = getStringValue(valueMap, "authorizationRequestUri");

                                // 处理 scopes - 转换为 Set
                                Set<String> scopes = null;
                                Object scopesObj = valueMap.get("scopes");
                                if (scopesObj instanceof Collection scopesCollection) {
                                    scopes = new HashSet<>(scopesCollection);
                                }

                                // 处理 additionalParameters
                                Map<String, Object> additionalParameters = getMapValue(valueMap, "additionalParameters");

                                // 设置所有字段
                                if (authorizationUri != null) {
                                    oAuth2AuthorizationRequestBuilder.authorizationUri(authorizationUri);
                                }
                                if (clientId != null) {
                                    oAuth2AuthorizationRequestBuilder.clientId(clientId);
                                }
                                if (redirectUri != null) {
                                    oAuth2AuthorizationRequestBuilder.redirectUri(redirectUri);
                                }
                                if (scopes != null) {
                                    oAuth2AuthorizationRequestBuilder.scopes(scopes);
                                }
                                if (state != null) {
                                    oAuth2AuthorizationRequestBuilder.state(state);
                                }
                                if (authorizationRequestUri != null) {
                                    oAuth2AuthorizationRequestBuilder.authorizationRequestUri(authorizationRequestUri);
                                }
                                if (additionalParameters != null) {
                                    oAuth2AuthorizationRequestBuilder.additionalParameters(additionalParameters);
                                }

                                builder.attribute(key, oAuth2AuthorizationRequestBuilder.build());
                                continue;
                            } catch (Exception e) {
                                log.warn("重建 OAuth2AuthorizationRequest 失败，key={}: {}", key, e.getMessage(), e);
                            }
                        }
                        // 检查是否是 Principal (UsernamePasswordAuthenticationToken)
                        else if ("java.security.Principal".equals(key)) {
                            try {
                                Object principal = valueMap.get("principal");
                                Object credentials = valueMap.get("credentials");
                                // 获取 authorities（如果有）
                                List<GrantedAuthority> authorities = new ArrayList<>();
                                Object authoritiesObj = valueMap.get("authorities");
                                if (authoritiesObj instanceof Collection<?> authorityCollection) {
                                    for (Object auth : authorityCollection) {
                                        if (auth instanceof Map) {
                                            @SuppressWarnings("unchecked")
                                            Map<String, Object> authMap = (Map<String, Object>) auth;
                                            String authority = (String) authMap.get("authority");
                                            if (authority != null) {
                                                authorities.add(() -> authority);
                                            }
                                        } else if (auth instanceof String) {
                                            authorities.add(auth::toString);
                                        }
                                    }
                                }

                                UsernamePasswordAuthenticationToken authentication =
                                        new UsernamePasswordAuthenticationToken(principal, credentials, authorities);
                                builder.attribute(key, authentication);
                                continue;
                            } catch (Exception e) {
                                log.warn("重建 Principal 失败，key={}: {}", key, e.getMessage(), e);
                            }
                        }
                    }

                    // 其他情况直接添加
                    builder.attribute(key, value);
                }
            } catch (Exception e) {
                log.warn("反序列化 OAuth2 授权属性失败，id={}: {}", dto.getId(), e.getMessage(), e);
            }
        }


        // 添加 State
        if (dto.getState() != null) {
            builder.attribute(OAuth2ParameterNames.STATE, dto.getState());
        }

        // 优化：提取 Token 处理逻辑
        processAuthorizationCode(dto, builder);
        processAccessToken(dto, builder);
        processRefreshToken(dto, builder);

        return builder.build();
    }

    /**
     * 处理授权码 Token 信息
     */
    private void processAuthorizationCode(OAuth2AuthorizationDTO dto, OAuth2Authorization.Builder builder) {
        if (dto.getAuthorizationCodeValue() != null && !dto.getAuthorizationCodeValue().trim().isEmpty()) {
            Instant issuedAt = OAuth2SettingsUtil.instantFromLocalDateTime(dto.getAuthorizationCodeIssuedAt());
            Instant expiresAt = OAuth2SettingsUtil.instantFromLocalDateTime(dto.getAuthorizationCodeExpiresAt());

            if (issuedAt != null && expiresAt != null) {
                OAuth2AuthorizationCode authorizationCode = new OAuth2AuthorizationCode(
                        dto.getAuthorizationCodeValue(),
                        issuedAt,
                        expiresAt
                );

                builder.token(authorizationCode, m -> {
                    Map<String, Object> metadata = dto.getAuthorizationCodeMetadata();
                    if (metadata != null) {
                        m.putAll(metadata);
                    }
                });
            }
        }
    }

    /**
     * 处理 Access Token 信息
     */
    private void processAccessToken(OAuth2AuthorizationDTO dto, OAuth2Authorization.Builder builder) {
        if (dto.getAccessTokenValue() != null && !dto.getAccessTokenValue().trim().isEmpty()) {
            Instant issuedAt = OAuth2SettingsUtil.instantFromLocalDateTime(dto.getAccessTokenIssuedAt());
            Instant expiresAt = OAuth2SettingsUtil.instantFromLocalDateTime(dto.getAccessTokenExpiresAt());

            if (issuedAt != null && expiresAt != null) {
                // 优化：移除冗余的条件判断，默认就是 BEARER
                OAuth2AccessToken accessToken = new OAuth2AccessToken(
                        OAuth2AccessToken.TokenType.BEARER,
                        dto.getAccessTokenValue(),
                        issuedAt,
                        expiresAt,
                        dto.getAccessTokenScopes()
                );

                builder.token(accessToken, m -> {
                    Map<String, Object> metadata = dto.getAccessTokenMetadata();
                    if (metadata != null) {
                        m.putAll(metadata);
                    }
                });
            }
        }
    }

    /**
     * 处理 Refresh Token 信息
     */
    private void processRefreshToken(OAuth2AuthorizationDTO dto, OAuth2Authorization.Builder builder) {
        if (dto.getRefreshTokenValue() != null && !dto.getRefreshTokenValue().trim().isEmpty()) {
            Instant issuedAt = OAuth2SettingsUtil.instantFromLocalDateTime(dto.getRefreshTokenIssuedAt());
            Instant expiresAt = OAuth2SettingsUtil.instantFromLocalDateTime(dto.getRefreshTokenExpiresAt());

            if (issuedAt != null && expiresAt != null) {
                OAuth2RefreshToken refreshToken = new OAuth2RefreshToken(
                        dto.getRefreshTokenValue(),
                        issuedAt,
                        expiresAt
                );

                builder.token(refreshToken, m -> {
                    Map<String, Object> metadata = dto.getRefreshTokenMetadata();
                    if (metadata != null) {
                        m.putAll(metadata);
                    }
                });
            }
        }
    }

    /**
     * 安全地从 Map 中获取 String 值
     */
    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof String str) {
            return str;
        }
        return null;
    }

    /**
     * 安全地从 Map 中获取 Map 值
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> getMapValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Map) {
            return new HashMap<>((Map<String, Object>) value);
        }
        return Collections.emptyMap();
    }

    /**
     * 获取 RegisteredClient
     * 
     * <p>通过 FeignRegisteredClientService 获取已注册的客户端信息</p>
     * 
     * @param id 客户端 ID
     * @return RegisteredClient 对象，获取失败返回 null
     */
    private RegisteredClient getRegisteredClientById(String id) {
        if (id == null || id.trim().isEmpty()) {
            return null;
        }

        try {
            log.debug("正在通过 ID 获取已注册客户端：{}", id);
            return feignRegisteredClientService.findById(id);
        } catch (Exception e) {
            log.warn("获取已注册客户端失败，ID: {}，将使用最小化客户端", id, e);
            return null;
        }
    }
}
