package com.jnet.system.api.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * OAuth2 客户端 DTO
 */
@Data
public class OAuth2ClientDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID
     */
    private String id;

    /**
     * 客户端 ID
     */
    private String clientId;

    /**
     * 客户端 ID 签发时间
     */
    private LocalDateTime clientIdIssuedAt;

    /**
     * 客户端密钥
     */
    private String clientSecret;

    /**
     * 客户端密钥过期时间
     */
    private LocalDateTime clientSecretExpiresAt;

    /**
     * 客户端名称
     */
    private String clientName;

    /**
     * 客户端认证方式（JSONB Set）
     */
    private Set<String> clientAuthenticationMethods;

    /**
     * 授权类型（JSONB Set）
     */
    private Set<String> authorizationGrantTypes;

    /**
     * 重定向 URI（JSONB Set）
     */
    private Set<String> redirectUris;

    /**
     * 登出重定向 URI（JSONB Set）
     */
    private Set<String> postLogoutRedirectUris;

    /**
     * 授权范围（JSONB Set）
     */
    private Set<String> scopes;

    /**
     * 客户端设置（JSONB 对象）
     */
    private ClientSettingsDTO clientSettings;

    /**
     * 令牌设置（JSONB 对象）
     */
    private TokenSettingsDTO tokenSettings;

    /**
     * 租户 ID
     */
    private Long tenantId;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
