package com.jnet.system.api.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.Duration;

/**
 * OAuth2 Token 设置 DTO
 * 
 * 参照 Spring Security TokenSettings 设计
 * 
 * @author JNet
 * @since 1.0.0
 */
@Data
public class TokenSettingsDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 授权码有效期
     * 默认 5 分钟
     */
    private Duration authorizationCodeTimeToLive;

    /**
     * 访问令牌有效期
     * 默认 5 分钟
     */
    private Duration accessTokenTimeToLive;

    /**
     * 访问令牌格式
     * 默认 SELF_CONTAINED（自包含）
     */
    private String accessTokenFormat;

    /**
     * 设备码有效期
     * 默认 5 分钟
     */
    private Duration deviceCodeTimeToLive;

    /**
     * 是否重用刷新令牌
     * 默认 true
     */
    private Boolean reuseRefreshTokens;

    /**
     * 刷新令牌有效期
     * 默认 60 分钟
     */
    private Duration refreshTokenTimeToLive;

    /**
     * ID 令牌签名算法
     * 默认 RS256
     */
    private String idTokenSignatureAlgorithm;
}
