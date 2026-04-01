package com.jnet.auth.service;

import com.jnet.auth.utils.OAuth2SettingsUtils;
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
import java.time.Instant;
import java.util.*;


/**
 * 基于 Feign Client 的 OAuth2AuthorizationService 实现
 * 从 system-admin 服务获取 OAuth2 授权数据
 * <p>
 * 功能说明：
 * 1. 通过 Feign 调用远程 system-admin 服务实现授权数据的持久化
 * 2. 支持授权码模式、密码模式等多种授权类型
 * 3. 实现 Token 的存储、查询和撤销功能
 */
@Slf4j
@Service
public class FeignOAuth2AuthorizationService implements OAuth2AuthorizationService {

    @Resource
    private OAuth2AuthorizationFeignClient oauth2AuthorizationFeignClient;

    @Resource
    private FeignRegisteredClientService feignRegisteredClientService;


    /**
     * 保存 OAuth2 授权信息
     *
     * @param authorization Spring Security OAuth2Authorization 对象
     */
    @Override
    public void save(OAuth2Authorization authorization) {
        try {
            log.info("Saving OAuth2 authorization: id={}, principal={}",
                    authorization.getId(), authorization.getPrincipalName());

            OAuth2AuthorizationDTO dto = convertToDTO(authorization);
            oauth2AuthorizationFeignClient.saveAuthorization(dto);

        } catch (Exception e) {
            log.error("Failed to save OAuth2 authorization", e);
            throw new RuntimeException("Failed to save OAuth2 authorization", e);
        }
    }

    /**
     * 删除 OAuth2 授权信息
     *
     * @param authorization 要删除的授权对象
     */
    @Override
    public void remove(OAuth2Authorization authorization) {
        try {
            log.info("Removing OAuth2 authorization: id={}", authorization.getId());

            // 调用 Feign Client 撤销授权
            Result<Void> result = oauth2AuthorizationFeignClient.revokeAuthorization(authorization.getId());

            if (result != null && result.isSuccess()) {
                log.info("Successfully removed authorization: {}", authorization.getId());
            } else {
                log.warn("Failed to remove authorization: {}, result={}", authorization.getId(), result);
            }

        } catch (Exception e) {
            log.error("Failed to remove OAuth2 authorization", e);
            throw new RuntimeException("Failed to remove OAuth2 authorization", e);
        }
    }

    /**
     * 根据 ID 查找 OAuth2 授权信息
     *
     * @param id 授权 ID
     * @return OAuth2Authorization 对象，不存在返回 null
     */
    @Override
    public OAuth2Authorization findById(String id) {
        try {
            log.debug("Finding OAuth2 authorization by ID: {}", id);

            Result<OAuth2AuthorizationDTO> result = oauth2AuthorizationFeignClient.getAuthorizationById(id);

            if (result == null || !result.isSuccess() || result.getData() == null) {
                log.debug("No authorization found for ID: {}", id);
                return null;
            }

            OAuth2AuthorizationDTO dto = result.getData();
            OAuth2Authorization authorization = convertToAuthorization(dto);

            log.debug("Successfully found authorization: id={}, principal={}",
                    dto.getId(), dto.getPrincipalName());

            return authorization;

        } catch (Exception e) {
            log.error("Error finding OAuth2 authorization by ID: {}", id, e);
            return null;
        }
    }

    /**
     * 根据 Token 值查找 OAuth2 授权信息
     *
     * @param tokenValue Token 值
     * @param tokenType  Token 类型（access_token, refresh_token, code 等）
     * @return OAuth2Authorization 对象，不存在返回 null
     */
    @Override
    public OAuth2Authorization findByToken(String tokenValue, OAuth2TokenType tokenType) {
        try {
            if (tokenValue == null || tokenType == null) {
                log.debug("Token value or type is null");
                return null;
            }

            String tokenTypeValue = tokenType.getValue();
            log.debug("Finding OAuth2 authorization by token: value={}, type={}",
                    tokenValue, tokenTypeValue);

            // 调用 Feign Client 查询
            Result<OAuth2AuthorizationDTO> result = oauth2AuthorizationFeignClient.getAuthorizationByToken(tokenValue, tokenTypeValue);

            if (result == null || !result.isSuccess() || result.getData() == null) {
                log.debug("No authorization found for token type: {}", tokenTypeValue);
                return null;
            }

            OAuth2AuthorizationDTO dto = result.getData();
            OAuth2Authorization authorization = convertToAuthorization(dto);

            log.debug("Successfully found authorization for token: type={}, principal={}",
                    tokenTypeValue, dto.getPrincipalName());

            return authorization;

        } catch (Exception e) {
            log.error("Error finding OAuth2 authorization by token", e);
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
                dto.setAuthorizationCodeIssuedAt(OAuth2SettingsUtils.localDateTimeFromInstant(code.getIssuedAt()));
                dto.setAuthorizationCodeExpiresAt(OAuth2SettingsUtils.localDateTimeFromInstant(code.getExpiresAt()));
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
                dto.setAccessTokenIssuedAt(OAuth2SettingsUtils.localDateTimeFromInstant(accessToken.getIssuedAt()));
                dto.setAccessTokenExpiresAt(OAuth2SettingsUtils.localDateTimeFromInstant(accessToken.getExpiresAt()));
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
                dto.setRefreshTokenIssuedAt(OAuth2SettingsUtils.localDateTimeFromInstant(refreshToken.getIssuedAt()));
                dto.setRefreshTokenExpiresAt(OAuth2SettingsUtils.localDateTimeFromInstant(refreshToken.getExpiresAt()));
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
            log.warn("RegisteredClient not found for id: {}, using minimal client", dto.getRegisteredClientId());
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
                        if (OAuth2AuthorizationRequest.class.getName().equals(key)) {
                            try {
                                OAuth2AuthorizationRequest.Builder oAuth2AuthorizationRequestBuilder = OAuth2AuthorizationRequest.authorizationCode();

                                // 从 Map 中读取所有字段
                                String authorizationUri = getStringValue(valueMap, "authorizationUri");
                                String clientId = getStringValue(valueMap, "clientId");
                                String redirectUri = getStringValue(valueMap, "redirectUri");
                                String state = getStringValue(valueMap, "state");
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
                                log.warn("Failed to rebuild OAuth2AuthorizationRequest for key={}: {}", key, e.getMessage(), e);
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
                                log.warn("Failed to rebuild Principal for key={}: {}", key, e.getMessage(), e);
                            }
                        }
                    }

                    // 其他情况直接添加
                    builder.attribute(key, value);
                }
            } catch (Exception e) {
                log.warn("Failed to deserialize OAuth2 authorization attributes for id={}: {}", dto.getId(), e.getMessage(), e);
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
            Instant issuedAt = OAuth2SettingsUtils.instantFromLocalDateTime(dto.getAuthorizationCodeIssuedAt());
            Instant expiresAt = OAuth2SettingsUtils.instantFromLocalDateTime(dto.getAuthorizationCodeExpiresAt());

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
            Instant issuedAt = OAuth2SettingsUtils.instantFromLocalDateTime(dto.getAccessTokenIssuedAt());
            Instant expiresAt = OAuth2SettingsUtils.instantFromLocalDateTime(dto.getAccessTokenExpiresAt());

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
            Instant issuedAt = OAuth2SettingsUtils.instantFromLocalDateTime(dto.getRefreshTokenIssuedAt());
            Instant expiresAt = OAuth2SettingsUtils.instantFromLocalDateTime(dto.getRefreshTokenExpiresAt());

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
     * 通过 FeignRegisteredClientService 获取
     */
    private RegisteredClient getRegisteredClientById(String id) {
        if (id == null || id.trim().isEmpty()) {
            return null;
        }

        try {
            log.debug("Fetching registered client by ID: {}", id);
            return feignRegisteredClientService.findById(id);
        } catch (Exception e) {
            log.warn("Failed to fetch registered client for ID: {}, will use minimal client", id, e);
            return null;
        }
    }
}
