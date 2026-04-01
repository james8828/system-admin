package com.jnet.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jnet.common.result.PageQuery;
import com.jnet.common.result.PageResult;
import com.jnet.system.api.dto.ClientSettingsDTO;
import com.jnet.system.api.dto.OAuth2ClientDTO;
import com.jnet.system.api.dto.TokenSettingsDTO;
import com.jnet.system.mapper.OAuth2ClientMapper;
import com.jnet.system.service.OAuth2ClientService;
import com.jnet.system.entity.OAuth2Client;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * OAuth2 客户端 Service 实现类
 */
@Slf4j
@Service
public class OAuth2ClientServiceImpl extends ServiceImpl<OAuth2ClientMapper, OAuth2Client> implements OAuth2ClientService {

    @Resource
    private ObjectMapper objectMapper;
    @Resource
    private PasswordEncoder passwordEncoder;

    @Override
    public PageResult<OAuth2ClientDTO> pageClient(PageQuery pageQuery, OAuth2ClientDTO client) {
        Page<OAuth2Client> page = new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize());
        
        LambdaQueryWrapper<OAuth2Client> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(client.getTenantId() != null, OAuth2Client::getTenantId, client.getTenantId())
                .eq(StrUtil.isNotBlank(client.getClientId()), OAuth2Client::getClientId, client.getClientId())
                .like(StrUtil.isNotBlank(client.getClientName()), OAuth2Client::getClientName, client.getClientName())
                .orderByDesc(OAuth2Client::getCreateTime);
        
        Page<OAuth2Client> resultPage = baseMapper.selectPage(page, wrapper);

        // 将 Entity 转换为 DTO
        return PageResult.of(convertToDTOList(resultPage.getRecords()), resultPage.getTotal(), pageQuery.getPageSize(), pageQuery.getPageNum());
    }

    @Override
    public OAuth2ClientDTO getClientById(String id) {
        OAuth2Client client = baseMapper.selectById(id);
        return convertToDTO(client);
    }

    @Override
    public OAuth2ClientDTO getClientByClientId(String clientId) {
        LambdaQueryWrapper<OAuth2Client> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OAuth2Client::getClientId, clientId);
        OAuth2Client client = baseMapper.selectOne(wrapper);
        return convertToDTO(client);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean createClient(OAuth2ClientDTO dto) {
        // 检查 clientId 是否已存在
        OAuth2ClientDTO existing = getClientByClientId(dto.getClientId());
        if (existing != null) {
            throw new RuntimeException("客户端 ID 已存在");
        }

        OAuth2Client client = convertToEntity(dto);
        client.setId(UUID.randomUUID().toString());
        client.setClientSecret("{bcrypt}" + passwordEncoder.encode(dto.getClientSecret()));
        client.setClientIdIssuedAt(LocalDateTime.now());
        client.setCreateTime(LocalDateTime.now());
        
        return save(client);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateClient(OAuth2ClientDTO dto) {
        if (StrUtil.isBlank(dto.getId())) {
            throw new RuntimeException("ID 不能为空");
        }

        OAuth2ClientDTO existing = getClientById(dto.getId());
        if (existing == null) {
            throw new RuntimeException("客户端不存在");
        }

        OAuth2Client client = convertToEntity(dto);
        client.setId(existing.getId());
        client.setCreateTime(existing.getCreateTime());
        client.setClientIdIssuedAt(existing.getClientIdIssuedAt());
        
        // 如果提供了新的密钥，则更新
        if (StrUtil.isNotBlank(dto.getClientSecret())) {
            client.setClientSecret("{bcrypt}" + passwordEncoder.encode(dto.getClientSecret()));
        } else {
            client.setClientSecret(existing.getClientSecret());
        }
        
        client.setUpdateTime(LocalDateTime.now());
        
        return updateById(client);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteClient(String id) {
        return removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String refreshSecret(String clientId) {
        // 直接查询 Entity
        OAuth2Client client = baseMapper.selectOne(new LambdaQueryWrapper<OAuth2Client>().eq(OAuth2Client::getClientId, clientId));
        if (client == null) {
            throw new RuntimeException("客户端不存在");
        }

        // 生成新的密钥
        String newSecret = UUID.randomUUID().toString().substring(0, 16);
        client.setClientSecret("{bcrypt}" + passwordEncoder.encode(newSecret));
        client.setUpdateTime(LocalDateTime.now());
        
        updateById(client);
        
        return newSecret;
    }

    /**
     * 构建客户端设置
     */
    private ClientSettingsDTO buildClientSettings(OAuth2ClientDTO dto) {
        // 如果 DTO 中已有 clientSettings，直接使用
        if (dto.getClientSettings() != null) {
            return dto.getClientSettings();
        }
        
        // 否则使用默认值
        ClientSettingsDTO settings = new ClientSettingsDTO();
        settings.setRequireAuthorizationConsent(true);
        settings.setRequireProofKey(true);
        
        return settings;
    }

    /**
     * 构建令牌设置
     */
    private TokenSettingsDTO buildTokenSettings(OAuth2ClientDTO dto) {
        // 如果 DTO 中已有 tokenSettings，直接使用
        if (dto.getTokenSettings() != null) {
            return dto.getTokenSettings();
        }
        
        // 否则使用默认值
        TokenSettingsDTO settings = new TokenSettingsDTO();
        settings.setAccessTokenTimeToLive(Duration.ofHours(1));
        settings.setRefreshTokenTimeToLive(Duration.ofDays(7));
        settings.setReuseRefreshTokens(true);
        settings.setIdTokenSignatureAlgorithm("RS256");
        
        return settings;
    }

    /**
     * 将 Entity 转换为 DTO
     */
    private OAuth2ClientDTO convertToDTO(OAuth2Client client) {
        if (client == null) {
            return null;
        }
        
        OAuth2ClientDTO dto = new OAuth2ClientDTO();
        dto.setId(client.getId());
        dto.setClientId(client.getClientId());
        dto.setClientIdIssuedAt(client.getClientIdIssuedAt());
        dto.setClientSecret(client.getClientSecret());
        dto.setClientSecretExpiresAt(client.getClientSecretExpiresAt());
        dto.setClientName(client.getClientName());
        dto.setClientAuthenticationMethods(client.getClientAuthenticationMethods());
        dto.setAuthorizationGrantTypes(client.getAuthorizationGrantTypes());
        dto.setRedirectUris(client.getRedirectUris());
        dto.setPostLogoutRedirectUris(client.getPostLogoutRedirectUris());
        dto.setScopes(client.getScopes());
        dto.setClientSettings(convertToClientSettingsDTO(client.getClientSettings()));
        dto.setTokenSettings(convertToTokenSettingsDTO(client.getTokenSettings()));
        dto.setTenantId(client.getTenantId());
        dto.setCreateTime(client.getCreateTime());
        dto.setUpdateTime(client.getUpdateTime());
        
        return dto;
    }

    /**
     * 将 Entity List 转换为 DTO List
     */
    private java.util.List<OAuth2ClientDTO> convertToDTOList(java.util.List<OAuth2Client> clients) {
        return clients.stream()
                .map(this::convertToDTO)
                .toList();
    }

    /**
     * 将 Object 类型的 clientSettings 转换为 ClientSettingsDTO
     */
    private ClientSettingsDTO convertToClientSettingsDTO(Object settings) {
        if (settings == null) {
            return null;
        }
        
        // 如果已经是 DTO 类型，直接返回
        if (settings instanceof ClientSettingsDTO dto) {
            return dto;
        }
        
        // 如果是 Map 类型（从数据库读取），转换为 DTO
        if (settings instanceof Map map) {
            try {
                ClientSettingsDTO dto = new ClientSettingsDTO();
                
                // 支持两种命名风格：kebab-case 和 camelCase
                dto.setRequireAuthorizationConsent(getBooleanValue(map, "require-authorization-consent", "requireAuthorizationConsent"));
                dto.setRequireProofKey(getBooleanValue(map, "require-proof-key", "requireProofKey"));
                dto.setJwkSetUrl(getStringValue(map, "jwk-set-url", "jwkSetUrl"));
                dto.setTokenEndpointAuthenticationSigningAlgorithm(getStringValue(map, "token-endpoint-authentication-signing-algorithm", "tokenEndpointAuthenticationSigningAlgorithm"));
                
                log.debug("Converted client settings: {}", dto);
                return dto;
            } catch (Exception e) {
                log.warn("Failed to convert client settings to DTO: {}", e.getMessage());
                return null;
            }
        }
        
        return null;
    }

    /**
     * 从 Map 中获取布尔值（支持两种键名）
     */
    private Boolean getBooleanValue(Map map, String kebabKey, String camelKey) {
        Object value = map.get(kebabKey);
        if (value == null) {
            value = map.get(camelKey);
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Integer intVal) {
            return intVal != 0;
        }
        if (value instanceof String strVal) {
            return Boolean.parseBoolean(strVal) || "1".equals(strVal) || "true".equalsIgnoreCase(strVal);
        }
        return null;
    }

    /**
     * 从 Map 中获取字符串值（支持两种键名）
     */
    private String getStringValue(Map map, String kebabKey, String camelKey) {
        Object value = map.get(kebabKey);
        if (value == null) {
            value = map.get(camelKey);
        }
        return value != null ? value.toString() : null;
    }

    /**
     * 将 Object 类型的 tokenSettings 转换为 TokenSettingsDTO
     */
    private TokenSettingsDTO convertToTokenSettingsDTO(Object settings) {
        if (settings == null) {
            return null;
        }
        
        // 如果已经是 DTO 类型，直接返回
        if (settings instanceof TokenSettingsDTO dto) {
            return dto;
        }
        
        // 如果是 Map 类型（从数据库读取），转换为 DTO
        if (settings instanceof Map map) {
            try {
                TokenSettingsDTO dto = new TokenSettingsDTO();
                
                // 支持两种命名风格：kebab-case 和 camelCase
                // 数据库中存储的是秒数（Integer/Long），需要转换为 Duration
                dto.setAuthorizationCodeTimeToLive(getDurationValue(map, "authorization-code-time-to-live", "authorizationCodeTimeToLive"));
                dto.setAccessTokenTimeToLive(getDurationValue(map, "access-token-time-to-live", "accessTokenTimeToLive"));
                dto.setRefreshTokenTimeToLive(getDurationValue(map, "refresh-token-time-to-live", "refreshTokenTimeToLive"));
                dto.setDeviceCodeTimeToLive(getDurationValue(map, "device-code-time-to-live", "deviceCodeTimeToLive"));
                
                // 其他字段
                dto.setAccessTokenFormat(getStringValue(map, "access-token-format", "accessTokenFormat"));
                dto.setReuseRefreshTokens(getBooleanValue(map, "reuse-refresh-tokens", "reuseRefreshTokens"));
                dto.setIdTokenSignatureAlgorithm(getStringValue(map, "id-token-signature-algorithm", "idTokenSignatureAlgorithm"));
                
                log.debug("Converted token settings: {}", dto);
                return dto;
            } catch (Exception e) {
                log.warn("Failed to convert token settings to DTO: {}", e.getMessage());
                return null;
            }
        }
        
        return null;
    }

    /**
     * 从 Map 中获取 Duration 值（支持两种键名）
     * 数据库中的值是秒数（Integer/Long）或 ISO-8601 字符串（如 "PT5M"）
     */
    private Duration getDurationValue(Map map, String kebabKey, String camelKey) {
        Object value = map.get(kebabKey);
        if (value == null) {
            value = map.get(camelKey);
        }
        
        if (value == null) {
            return null;
        }
        
        // 如果是 Duration 类型，直接返回
        if (value instanceof Duration duration) {
            return duration;
        }
        
        // 如果是数字类型（秒数），转换为 Duration
        if (value instanceof Number number) {
            return Duration.ofSeconds(number.longValue());
        }
        
        // 如果是字符串，尝试解析为 ISO-8601 格式或数字
        if (value instanceof String str) {
            if (str.startsWith("PT")) {
                // ISO-8601 格式（如 "PT5M", "PT1H"）
                try {
                    return Duration.parse(str);
                } catch (Exception e) {
                    log.warn("Failed to parse ISO-8601 duration: {}", str);
                }
            } else {
                // 数字字符串
                try {
                    return Duration.ofSeconds(Long.parseLong(str));
                } catch (NumberFormatException e) {
                    log.warn("Failed to parse duration string: {}", str);
                }
            }
        }
        
        return null;
    }

    /**
     * 将 DTO 转换为 Entity
     */
    private OAuth2Client convertToEntity(OAuth2ClientDTO dto) {
        if (dto == null) {
            return null;
        }
        
        OAuth2Client client = new OAuth2Client();
        client.setClientId(dto.getClientId());
        client.setClientName(dto.getClientName());
        client.setTenantId(dto.getTenantId());
        
        // 直接设置 Set（类型一致）
        client.setClientAuthenticationMethods(dto.getClientAuthenticationMethods());
        client.setAuthorizationGrantTypes(dto.getAuthorizationGrantTypes());
        client.setRedirectUris(dto.getRedirectUris());
        client.setPostLogoutRedirectUris(dto.getPostLogoutRedirectUris());
        client.setScopes(dto.getScopes());
        
        client.setClientSettings(buildClientSettings(dto));
        client.setTokenSettings(buildTokenSettings(dto));
        
        return client;
    }

}
