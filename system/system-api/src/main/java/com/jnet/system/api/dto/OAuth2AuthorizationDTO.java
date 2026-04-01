package com.jnet.system.api.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

/**
 * OAuth2 授权 DTO
 */
@Data
public class OAuth2AuthorizationDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 授权 ID（主键）
     */
    private String id;

    /**
     * 客户端 ID
     */
    private String registeredClientId;

    /**
     * 用户标识
     */
    private String principalName;

    /**
     * 授权类型
     */
    private String authorizationGrantType;

    /**
     * 授权范围
     */
    private Set<String> authorizedScopes;

    /**
     * 属性（JSONB 对象）
     */
    private Map<String, Object> attributes;

    /**
     * 状态
     */
    private String state;

    /**
     * 授权码值
     */
    private String authorizationCodeValue;

    /**
     * 授权码签发时间
     */
    private LocalDateTime authorizationCodeIssuedAt;

    /**
     * 授权码过期时间
     */
    private LocalDateTime authorizationCodeExpiresAt;

    /**
     * 授权码元数据（JSONB 对象）
     */
    private Map<String, Object> authorizationCodeMetadata;

    /**
     * 访问令牌值
     */
    private String accessTokenValue;

    /**
     * 访问令牌签发时间
     */
    private LocalDateTime accessTokenIssuedAt;

    /**
     * 访问令牌过期时间
     */
    private LocalDateTime accessTokenExpiresAt;

    /**
     * 访问令牌元数据（JSONB 对象）
     */
    private Map<String, Object> accessTokenMetadata;

    /**
     * 访问令牌类型
     */
    private String accessTokenType;

    /**
     * 访问令牌范围
     */
    private Set<String> accessTokenScopes;

    /**
     * OIDC ID 令牌值
     */
    private String oidcIdTokenValue;

    /**
     * OIDC ID 令牌签发时间
     */
    private LocalDateTime oidcIdTokenIssuedAt;

    /**
     * OIDC ID 令牌过期时间
     */
    private LocalDateTime oidcIdTokenExpiresAt;

    /**
     * OIDC ID 令牌元数据（JSONB 对象）
     */
    private Map<String, Object> oidcIdTokenMetadata;

    /**
     * 刷新令牌值
     */
    private String refreshTokenValue;

    /**
     * 刷新令牌签发时间
     */
    private LocalDateTime refreshTokenIssuedAt;

    /**
     * 刷新令牌过期时间
     */
    private LocalDateTime refreshTokenExpiresAt;

    /**
     * 刷新令牌元数据（JSONB 对象）
     */
    private Map<String, Object> refreshTokenMetadata;

    /**
     * 用户代码值
     */
    private String userCodeValue;

    /**
     * 用户代码签发时间
     */
    private LocalDateTime userCodeIssuedAt;

    /**
     * 用户代码过期时间
     */
    private LocalDateTime userCodeExpiresAt;

    /**
     * 用户代码元数据（JSONB 对象）
     */
    private Map<String, Object> userCodeMetadata;

    /**
     * 设备代码值
     */
    private String deviceCodeValue;

    /**
     * 设备代码签发时间
     */
    private LocalDateTime deviceCodeIssuedAt;

    /**
     * 设备代码过期时间
     */
    private LocalDateTime deviceCodeExpiresAt;

    /**
     * 设备代码元数据（JSONB 对象）
     */
    private Map<String, Object> deviceCodeMetadata;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
