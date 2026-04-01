package com.jnet.system.api.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * OAuth2 Client 设置 DTO
 * 
 * 参照 Spring Security ClientSettings 设计
 * 
 * @author JNet
 * @since 1.0.0
 */
@Data
public class ClientSettingsDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 是否需要授权同意
     * 默认 true
     */
    private Boolean requireAuthorizationConsent;

    /**
     * 是否需要 Proof Key (PKCE)
     * 默认 true
     */
    private Boolean requireProofKey;

    /**
     * JWK Set URL
     * 用于获取客户端的公钥
     */
    private String jwkSetUrl;

    /**
     * Token 端点认证签名算法
     */
    private String tokenEndpointAuthenticationSigningAlgorithm;
}
