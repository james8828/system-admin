package com.jnet.auth.service;

import com.jnet.auth.utils.OAuth2SettingsUtils;
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
 * 基于 Feign Client 的 RegisteredClientRepository 实现
 * 从 system-admin 服务获取 OAuth2 客户端配置
 */
@Slf4j
@Repository
public class FeignRegisteredClientService implements RegisteredClientRepository {

    @Resource
    private OAuth2ClientFeignClient oauth2ClientFeignClient;

    @Override
    public void save(RegisteredClient registeredClient) {
        log.warn("Save operation is not supported in read-only mode. Client ID: {}", registeredClient.getClientId());
        // 注意：这里是只读模式，不支持保存操作
        // 如需保存，需要通过 Feign Client 调用 system-admin 的创建/更新接口
    }

    @Override
    public RegisteredClient findById(String id) throws RuntimeException {
        try {
            log.debug("Finding registered client by ID: {}", id);
            Result<OAuth2ClientDTO> result = oauth2ClientFeignClient.getClientById(id);
            
            if (result == null || !result.isSuccess() || result.getData() == null) {
                log.debug("No client found for ID: {}", id);
                return null;
            }
            
            return convertToRegisteredClient(result.getData());
        } catch (FeignException e) {
            if (e.status() == 401 || e.status() == 403 || e.status() == 404) {
                log.debug("Client not found for ID: {} (HTTP {})", id, e.status());
                return null;
            }
            log.error("Error finding registered client by ID: {}", id, e);
            throw new RuntimeException("Failed to find registered client", e);
        } catch (Exception e) {
            log.error("Error finding registered client by ID: {}", id, e);
            throw new RuntimeException("Failed to find registered client", e);
        }
    }

    @Override
    public RegisteredClient findByClientId(String clientId) throws RuntimeException{
        try {
            log.debug("Finding registered client by Client ID: {}", clientId);
            Result<OAuth2ClientDTO> result = oauth2ClientFeignClient.getClientByClientId(clientId);
            
            if (result == null || !result.isSuccess() || result.getData() == null) {
                log.debug("No client found for Client ID: {}", clientId);
                return null;
            }
            
            return convertToRegisteredClient(result.getData());
        } catch (FeignException e) {
            if (e.status() == 401 || e.status() == 403 || e.status() == 404) {
                log.debug("Client not found for Client ID: {} (HTTP {})", clientId, e.status());
                return null;
            }
            log.error("Error finding registered client by Client ID: {}", clientId, e);
            throw new RuntimeException("Failed to find registered client", e);
        } catch (Exception e) {
            log.error("Error finding registered client by Client ID: {}", clientId, e);
            throw new RuntimeException("Failed to find registered client", e);
        }
    }

    /**
     * 将 OAuth2ClientDTO 转换为 RegisteredClient
     */
    private RegisteredClient convertToRegisteredClient(OAuth2ClientDTO dto) {
        log.debug("Converting OAuth2ClientDTO to RegisteredClient: {}", dto.getClientId());
        
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
                    log.warn("Invalid client authentication method: {}", method);
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
                    log.warn("Invalid authorization grant type: {}", grantType);
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
        TokenSettings tokenSettings = OAuth2SettingsUtils.parseTokenSettingsFromObject(dto.getTokenSettings());
        log.info("Token Settings: {}", tokenSettings);
        builder.tokenSettings(tokenSettings);

        
        // 解析 Client Settings（Object 类型）
        ClientSettings clientSettings = OAuth2SettingsUtils.parseClientSettingsFromObject(dto.getClientSettings());
        builder.clientSettings(clientSettings);
        
        return builder.build();
    }
}
