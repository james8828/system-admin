package com.jnet.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import org.apache.ibatis.type.JdbcType;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * OAuth2 客户端实体类
 */
@Data
@TableName(value = "oauth2_registered_client", autoResultMap = true)
public class OAuth2Client {

    /**
     * 主键 ID
     */
    @TableId(type = IdType.ASSIGN_UUID)
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
     * 客户端认证方式（JSONB 数组）
     */
    @TableField(jdbcType = JdbcType.OTHER,typeHandler = JacksonTypeHandler.class)
    private Set<String> clientAuthenticationMethods;

    /**
     * 授权类型（JSONB 数组）
     */
    @TableField(jdbcType = JdbcType.OTHER,typeHandler = JacksonTypeHandler.class)
    private Set<String> authorizationGrantTypes;

    /**
     * 重定向 URI（JSONB 数组）
     */
    @TableField(jdbcType = JdbcType.OTHER,typeHandler = JacksonTypeHandler.class)
    private Set<String> redirectUris;

    /**
     * 登出重定向 URI（JSONB 数组）
     */
    @TableField(jdbcType = JdbcType.OTHER,typeHandler = JacksonTypeHandler.class)
    private Set<String> postLogoutRedirectUris;

    /**
     * 授权范围（JSONB 数组）
     */
    @TableField(jdbcType = JdbcType.OTHER,typeHandler = JacksonTypeHandler.class)
    private Set<String> scopes;

    /**
     * 客户端设置（JSONB 对象）
     */
    @TableField(jdbcType = JdbcType.OTHER,typeHandler = JacksonTypeHandler.class)
    private Object clientSettings;

    /**
     * 令牌设置（JSONB 对象）
     */
    @TableField(jdbcType = JdbcType.OTHER,typeHandler = JacksonTypeHandler.class)
    private Object tokenSettings;

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
