package com.jnet.auth.service;

import com.jnet.auth.utils.OAuth2SettingsUtil;
import com.jnet.common.result.Result;
import com.jnet.system.api.client.OAuth2ClientFeignClient;
import com.jnet.system.api.dto.OAuth2ClientDTO;
import feign.FeignException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Repository;
import java.util.Set;

/**
 * 基于 Feign 的已注册客户端服务实现
 * 
 * <p>核心功能：通过 Feign Client 远程调用 system-admin 服务，实现 OAuth2 客户端信息的读取</p>
 * 
 * <p>主要职责：</p>
 * <ul>
 *     <li>根据 ID 查询已注册的 OAuth2 客户端</li>
 *     <li>根据 Client ID 查询已注册的 OAuth2 客户端</li>
 *     <li>将 OAuth2ClientDTO 转换为 RegisteredClient 对象</li>
 * </ul>
 * 
 * <p>工作模式：只读模式（不支持保存操作）</p>
 * 
 * @author mu
 * @version 1.0
 * @since 2026/4/1
 */
@Slf4j
@Repository
public class FeignRegisteredClientService implements RegisteredClientRepository {

    @Resource
    private OAuth2ClientFeignClient oauth2ClientFeignClient;

    @Override
    public void save(RegisteredClient registeredClient) {
        log.warn("只读模式不支持保存操作。客户端 ID: {}", registeredClient.getClientId());
        // 注意：这里是只读模式，不支持保存操作
        // 如需保存，需要通过 Feign Client 调用 system-admin 的创建/更新接口
    }

    @Override
    public RegisteredClient findById(String id) throws RuntimeException {
        try {
            log.debug("正在通过 ID 查找已注册客户端：{}", id);
            Result<OAuth2ClientDTO> result = oauth2ClientFeignClient.getClientById(id);
            
            if (result == null || !result.isSuccess() || result.getData() == null) {
                log.debug("未找到 ID 为 {} 的客户端", id);
                return null;
            }
            
            return convertToRegisteredClient(result.getData());
        } catch (FeignException e) {
            if (e.status() == 401 || e.status() == 403 || e.status() == 404) {
                log.debug("未找到 ID 为 {} 的客户端（HTTP {}）", id, e.status());
                return null;
            }
            log.error("查找已注册客户端失败，ID: {}", id, e);
            throw new RuntimeException("查找已注册客户端失败", e);
        } catch (Exception e) {
            log.error("查找已注册客户端失败，ID: {}", id, e);
            throw new RuntimeException("查找已注册客户端失败", e);
        }
    }

    @Override
    public RegisteredClient findByClientId(String clientId) throws RuntimeException{
        try {
            log.debug("正在通过 Client ID 查找已注册客户端：{}", clientId);
            Result<OAuth2ClientDTO> result = oauth2ClientFeignClient.getClientByClientId(clientId);
            
            if (result == null || !result.isSuccess() || result.getData() == null) {
                log.debug("未找到 Client ID 为 {} 的客户端", clientId);
                return null;
            }
            
            return convertToRegisteredClient(result.getData());
        } catch (FeignException e) {
            if (e.status() == 401 || e.status() == 403 || e.status() == 404) {
                log.debug("未找到 Client ID 为 {} 的客户端（HTTP {}）", clientId, e.status());
                return null;
            }
            log.error("查找已注册客户端失败，Client ID: {}", clientId, e);
            throw new RuntimeException("查找已注册客户端失败", e);
        } catch (Exception e) {
            log.error("查找已注册客户端失败，Client ID: {}", clientId, e);
            throw new RuntimeException("查找已注册客户端失败", e);
        }
    }

    /**
     * 将 OAuth2ClientDTO 转换为 RegisteredClient
     * 
     * <p>转换内容包括：</p>
     * <ul>
     *     <li>基础信息：ID、Client ID、Client Secret</li>
     *     <li>时间戳：ClientIdIssuedAt、ClientSecretExpiresAt</li>
     *     <li>认证方法：clientAuthenticationMethods</li>
     *     <li>授权类型：authorizationGrantTypes</li>
     *     <li>重定向 URI：redirectUris、postLogoutRedirectUris</li>
     *     <li>权限范围：scopes</li>
     *     <li>Token 设置和客户端设置</li>
     * </ul>
     * 
     * @param dto OAuth2 客户端数据传输对象
     * @return RegisteredClient 已注册的客户端对象
     */
    private RegisteredClient convertToRegisteredClient(OAuth2ClientDTO dto) {
        log.debug("正在将 OAuth2ClientDTO 转换为 RegisteredClient: {}", dto.getClientId());
        
        RegisteredClient.Builder builder = RegisteredClient.withId(dto.getId())
            .clientId(dto.getClientId())
            .clientSecret(dto.getClientSecret());
        
        // 转换时间戳（LocalDateTime -> Instant）
        if (dto.getClientIdIssuedAt() != null) {
            builder.clientIdIssuedAt(dto.getClientIdIssuedAt().atZone(java.time.ZoneId.systemDefault()).toInstant());
        }
        if (dto.getClientSecretExpiresAt() != null) {
            builder.clientSecretExpiresAt(dto.getClientSecretExpiresAt().atZone(java.time.ZoneId.systemDefault()).toInstant());
        }
        
        // 直接使用 Set（DTO 与 Entity 类型一致）
        Set<String> clientAuthenticationMethods = dto.getClientAuthenticationMethods();
        if (CollectionUtils.isNotEmpty(clientAuthenticationMethods)) {
            clientAuthenticationMethods.forEach(method -> {
                try {
                    builder.clientAuthenticationMethod(new ClientAuthenticationMethod(method.trim()));
                } catch (Exception e) {
                    log.warn("无效的客户端认证方法：{}", method);
                }
            });
        }
        
        // 直接使用 Set（DTO 与 Entity 类型一致）
        Set<String> authorizationGrantTypes = dto.getAuthorizationGrantTypes();
        if (CollectionUtils.isNotEmpty(authorizationGrantTypes)) {
            authorizationGrantTypes.forEach(grantType -> {
                try {
                    builder.authorizationGrantType(new AuthorizationGrantType(grantType.trim()));
                } catch (Exception e) {
                    log.warn("无效的授权类型：{}", grantType);
                }
            });
        }
        
        // 直接使用 Set（DTO 与 Entity 类型一致）
        Set<String> redirectUris = dto.getRedirectUris();
        if (CollectionUtils.isNotEmpty(redirectUris)) {
            redirectUris.forEach(uri -> builder.redirectUri(uri.trim()));
        }
        
        // 直接使用 Set（DTO 与 Entity 类型一致）
        Set<String> postLogoutRedirectUris = dto.getPostLogoutRedirectUris();
        if (CollectionUtils.isNotEmpty(postLogoutRedirectUris)) {
            postLogoutRedirectUris.forEach(uri -> builder.postLogoutRedirectUri(uri.trim()));
        }
        // 直接使用 Set（DTO 与 Entity 类型一致）
        Set<String> scopes = dto.getScopes();
        if (CollectionUtils.isNotEmpty(scopes)) {
            scopes.forEach(scope -> builder.scope(scope.trim()));
        }
        
        // 解析 Token Settings（Object 类型）
        TokenSettings tokenSettings = OAuth2SettingsUtil.parseTokenSettingsFromObject(dto.getTokenSettings());
        log.debug("Token 设置：{}", tokenSettings);
        builder.tokenSettings(tokenSettings);

        
        // 解析 Client Settings（Object 类型）
        ClientSettings clientSettings = OAuth2SettingsUtil.parseClientSettingsFromObject(dto.getClientSettings());
        builder.clientSettings(clientSettings);
        
        return builder.build();
    }
}
