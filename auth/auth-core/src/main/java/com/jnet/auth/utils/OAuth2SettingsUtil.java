package com.jnet.auth.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

/**
 * OAuth2 设置工具类
 * 
 * <p>核心功能：解析和转换 OAuth2 客户端设置和 Token 设置</p>
 * 
 * <p>主要职责：</p>
 * <ul>
 *     <li>将 Object 类型的 clientSettings 转换为 ClientSettings 对象</li>
 *     <li>将 Object 类型的 tokenSettings 转换为 TokenSettings 对象</li>
 *     <li>支持多种数据格式（Map、JSON String、Java Bean）</li>
 *     <li>支持 kebab-case 和 camelCase 两种命名风格</li>
 *     <li>解析布尔值、持续时间等复杂类型</li>
 * </ul>
 * 
 * <p>使用场景：</p>
 * <ul>
 *     <li>从数据库读取 OAuth2 客户端配置</li>
 *     <li>反序列化 JSONB 格式的设置数据</li>
 *     <li>构建 Spring Security OAuth2 的配置对象</li>
 * </ul>
 * 
 * @author mu
 * @version 1.0
 * @since 2026/4/1
 */
@Slf4j
@Component
public class OAuth2SettingsUtil {

    private static ObjectMapper objectMapper;

    public OAuth2SettingsUtil(ObjectMapper objectMapper) {
        OAuth2SettingsUtil.objectMapper = objectMapper;
    }

    /**
     * 将 Object 类型的 clientSettings 转换为 ClientSettings
     * 
     * <p>支持的配置项：</p>
     * <ul>
     *     <li>require-authorization-consent - 是否需要授权同意</li>
     *     <li>require-proof-key - PKCE 是否需要 proof key</li>
     *     <li>jwk-set-url - JWK Set URL</li>
     *     <li>token-endpoint-authentication-signing-algorithm - Token 端点认证签名算法</li>
     * </ul>
     * 
     * @param clientSettingsObject Object 类型的客户端设置（可能是 Map、String 或其他类型）
     * @return ClientSettings 对象
     */
    public static ClientSettings parseClientSettingsFromObject(Object clientSettingsObject) {
        ClientSettings.Builder builder = ClientSettings.builder();

        if (clientSettingsObject == null) {
            // 默认设置
            return builder.requireProofKey(true).build();
        }

        try {
            // 如果已经是 Map 类型，直接使用
            Map<String, Object> settings = convertToMap(clientSettingsObject);

            // 解析 require-authorization-consent
            if (settings.containsKey("require-authorization-consent")) {
                Object value = settings.get("require-authorization-consent");
                builder.requireAuthorizationConsent(parseBoolean(value));
            }

            // 解析 require-proof-key
            if (settings.containsKey("require-proof-key")) {
                Object value = settings.get("require-proof-key");
                builder.requireProofKey(parseBoolean(value));
            }

            // 解析 jwk-set-url
            if (settings.containsKey("jwk-set-url")) {
                builder.jwkSetUrl(settings.get("jwk-set-url").toString());
            }

            // 解析 token-endpoint-authentication-signing-algorithm
            if (settings.containsKey("token-endpoint-authentication-signing-algorithm")) {
                String algorithmValue = settings.get("token-endpoint-authentication-signing-algorithm").toString();
                try {
                    builder.tokenEndpointAuthenticationSigningAlgorithm(SignatureAlgorithm.from(algorithmValue));
                } catch (Exception e) {
                    log.warn("无效的 Token 端点认证签名算法：{}", algorithmValue);
                }
            }

        } catch (Exception e) {
            log.warn("解析客户端设置失败：{}", e.getMessage());
        }

        return builder.build();
    }

    /**
     * 将 Object 类型的 tokenSettings 转换为 TokenSettings
     * 
     * <p>支持的配置项：</p>
     * <ul>
     *     <li>access-token-time-to-live - Access Token 有效期（秒）</li>
     *     <li>refresh-token-time-to-live - Refresh Token 有效期（秒）</li>
     *     <li>access-token-format - Access Token 格式（如 JWT）</li>
     *     <li>reuse-refresh-tokens - 是否重用 Refresh Token</li>
     *     <li>authorization-code-time-to-live - Authorization Code 有效期（秒）</li>
     *     <li>device-code-time-to-live - Device Code 有效期（秒）</li>
     *     <li>id-token-signature-algorithm - ID Token 签名算法</li>
     * </ul>
     * 
     * <p>命名风格支持：</p>
     * <ul>
     *     <li>kebab-case: access-token-time-to-live (JSONB 原始格式)</li>
     *     <li>camelCase: accessTokenTimeToLive (Java 序列化格式)</li>
     * </ul>
     * 
     * @param tokenSettingsObject Object 类型的令牌设置（可能是 Map、String 或其他类型）
     * @return TokenSettings 对象
     */
    public static TokenSettings parseTokenSettingsFromObject(Object tokenSettingsObject) {
        TokenSettings.Builder builder = TokenSettings.builder();

        if (tokenSettingsObject == null) {
            log.info("Token 设置为空，使用默认设置");
            // 默认设置
            return builder.build();
        }

        try {
            // 如果已经是 Map 类型，直接使用
            Map<String, Object> settings = convertToMap(tokenSettingsObject);
            log.info("正在解析 Token 设置：{}", settings);

            // 支持两种命名风格：kebab-case 和 camelCase
            // kebab-case: access-token-time-to-live (JSONB 原始格式)
            // camelCase: accessTokenTimeToLive (Java 序列化格式)
            
            // 解析 access-token-time-to-live
            Object accessTokenTTL = settings.get("access-token-time-to-live");
            if (accessTokenTTL == null) {
                accessTokenTTL = settings.get("accessTokenTimeToLive");
            }
            if (accessTokenTTL != null) {
                log.info("找到 access-token-time-to-live: {} (类型：{})", accessTokenTTL, accessTokenTTL.getClass().getName());
                Long seconds = parseDurationSeconds(accessTokenTTL);
                if (seconds != null) {
                    builder.accessTokenTimeToLive(java.time.Duration.ofSeconds(seconds));
                    log.info("已设置 Access Token 有效期：{} 秒", seconds);
                } else {
                    log.warn("解析 access-token-time-to-live 失败：{}", accessTokenTTL);
                }
            }

            // 解析 refresh-token-time-to-live
            Object refreshTokenTTL = settings.get("refresh-token-time-to-live");
            if (refreshTokenTTL == null) {
                refreshTokenTTL = settings.get("refreshTokenTimeToLive");
            }
            if (refreshTokenTTL != null) {
                log.info("找到 refresh-token-time-to-live: {} (类型：{})", refreshTokenTTL, refreshTokenTTL.getClass().getName());
                Long seconds = parseDurationSeconds(refreshTokenTTL);
                if (seconds != null) {
                    builder.refreshTokenTimeToLive(java.time.Duration.ofSeconds(seconds));
                    log.info("已设置 Refresh Token 有效期：{} 秒", seconds);
                } else {
                    log.warn("解析 refresh-token-time-to-live 失败：{}", refreshTokenTTL);
                }
            }

            // 解析 access-token-format
            Object accessTokenFormat = settings.get("access-token-format");
            if (accessTokenFormat == null) {
                accessTokenFormat = settings.get("accessTokenFormat");
            }
            if (accessTokenFormat != null) {
                String formatValue = accessTokenFormat.toString();
                try {
                    builder.accessTokenFormat(new org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat(formatValue));
                    log.info("已设置 Access Token 格式：{}", formatValue);
                } catch (Exception e) {
                    log.warn("无效的 Access Token 格式：{}", formatValue);
                }
            }

            // 解析 reuse-refresh-tokens
            Object reuseRefreshTokens = settings.get("reuse-refresh-tokens");
            if (reuseRefreshTokens == null) {
                reuseRefreshTokens = settings.get("reuseRefreshTokens");
            }
            if (reuseRefreshTokens != null) {
                boolean reuse = parseBoolean(reuseRefreshTokens);
                builder.reuseRefreshTokens(reuse);
                log.info("已设置是否重用 Refresh Token: {}", reuse);
            }

            // 解析 authorization-code-time-to-live
            Object authCodeTTL = settings.get("authorization-code-time-to-live");
            if (authCodeTTL == null) {
                authCodeTTL = settings.get("authorizationCodeTimeToLive");
            }
            if (authCodeTTL != null) {
                Long seconds = parseDurationSeconds(authCodeTTL);
                if (seconds != null) {
                    builder.authorizationCodeTimeToLive(java.time.Duration.ofSeconds(seconds));
                    log.info("已设置 Authorization Code 有效期：{} 秒", seconds);
                }
            }

            // 解析 device-code-time-to-live
            Object deviceCodeTTL = settings.get("device-code-time-to-live");
            if (deviceCodeTTL == null) {
                deviceCodeTTL = settings.get("deviceCodeTimeToLive");
            }
            if (deviceCodeTTL != null) {
                Long seconds = parseDurationSeconds(deviceCodeTTL);
                if (seconds != null) {
                    builder.deviceCodeTimeToLive(java.time.Duration.ofSeconds(seconds));
                    log.info("已设置 Device Code 有效期：{} 秒", seconds);
                }
            }

            // 解析 id-token-signature-algorithm
            Object idTokenAlgorithm = settings.get("id-token-signature-algorithm");
            if (idTokenAlgorithm == null) {
                idTokenAlgorithm = settings.get("idTokenSignatureAlgorithm");
            }
            if (idTokenAlgorithm != null) {
                String algorithmValue = idTokenAlgorithm.toString();
                try {
                    builder.idTokenSignatureAlgorithm(SignatureAlgorithm.from(algorithmValue));
                    log.info("已设置 ID Token 签名算法：{}", algorithmValue);
                } catch (Exception e) {
                    log.warn("无效的 ID Token 签名算法：{}", algorithmValue);
                }
            }

            log.info("Token 设置构建成功");

        } catch (Exception e) {
            log.error("解析 Token 设置失败：{}", e.getMessage(), e);
        }

        return builder.build();
    }

    /**
     * 解析布尔值（支持 Boolean、Integer、String 等类型）
     * 
     * <p>支持的输入格式：</p>
     * <ul>
     *         <li>Boolean - 直接返回</li>
     *         <li>Integer - 非 0 为 true，0 为 false</li>
     *         <li>String - "true"、"1" 或布尔字符串</li>
     * </ul>
     * 
     * @param value 待解析的值
     * @return 布尔值
     */
    public static boolean parseBoolean(Object value) {
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        if (value instanceof Integer intValue) {
            return intValue != 0;
        }
        if (value instanceof String stringValue) {
            return Boolean.parseBoolean(stringValue) || "1".equals(stringValue) || "true".equalsIgnoreCase(stringValue);
        }
        return false;
    }

    /**
     * 解析持续时间（秒数）
     * 
     * <p>支持的输入格式：</p>
     * <ul>
     *         <li>Long/Integer - 直接返回秒数</li>
     *         <li>String - 数字字符串或 ISO-8601 格式（如 "PT5M", "PT1H"）</li>
     *         <li>Map - Duration 对象的序列化格式</li>
     * </ul>
     * 
     * @param value 待解析的值
     * @return 秒数，解析失败返回 null
     */
    public static Long parseDurationSeconds(Object value) {
        if (value == null) {
            return null;
        }

        // 数字类型直接返回
        if (value instanceof Number number) {
            return number.longValue();
        }

        if (value instanceof String stringValue) {
            // 尝试解析 ISO-8601 格式 (如 "PT5M", "PT1H", "PT7200S")
            if (stringValue.startsWith("PT")) {
                try {
                    java.time.Duration duration = java.time.Duration.parse(stringValue);
                    return duration.getSeconds();
                } catch (Exception e) {
                    log.warn("解析 ISO-8601 持续时间失败：{}", stringValue);
                }
            }
            
            // 尝试解析普通数字字符串
            try {
                return Long.parseLong(stringValue);
            } catch (NumberFormatException e) {
                log.warn("解析持续时间字符串失败：{}", value);
            }
        }

        if (value instanceof Map durationMap) {
            // 处理 Duration 对象的 Map 表示形式
            if (durationMap.containsKey("seconds")) {
                Object secondsObj = durationMap.get("seconds");
                if (secondsObj instanceof Number number) {
                    return number.longValue();
                }
            }
            
            // 处理 Java Time API 的序列化格式
            if (durationMap.containsKey("nano")) {
                long seconds = durationMap.getOrDefault("seconds", 0L) instanceof Number 
                    ? ((Number) durationMap.get("seconds")).longValue() 
                    : 0L;
                int nanos = durationMap.getOrDefault("nano", 0) instanceof Number 
                    ? ((Number) durationMap.get("nano")).intValue() 
                    : 0;
                return seconds + (nanos > 0 ? 1 : 0); // 近似值
            }
        }

        return null;
    }

    /**
     * 将 Object 转换为 Map
     * 
     * <p>转换策略：</p>
     * <ul>
     *         <li>Map - 直接返回</li>
     *         <li>String - 解析为 JSON</li>
     *         <li>其他对象 - 先序列化为 JSON，再反序列化为 Map</li>
     * </ul>
     * 
     * @param object 待转换的对象
     * @return Map 对象
     * @throws Exception 转换失败时抛出异常
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> convertToMap(Object object) throws Exception {
        if (object instanceof Map map) {
            return map;
        } else if (object instanceof String stringValue) {
            return objectMapper.readValue(stringValue, Map.class);
        } else {
            String json = objectMapper.writeValueAsString(object);
            return objectMapper.readValue(json, Map.class);
        }
    }

    /**
     * LocalDateTime 转 Instant
     * 
     * <p>使用时区转换将 LocalDateTime 转换为 Instant</p>
     * 
     * @param localDateTime LocalDateTime 对象
     * @return Instant 对象，如果为 null 则返回 null
     */
    public static Instant instantFromLocalDateTime(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return localDateTime.atZone(ZoneId.systemDefault()).toInstant();
    }

    /**
     * Instant 转 LocalDateTime
     * 
     * <p>使用时区转换将 Instant 转换为 LocalDateTime</p>
     * 
     * @param instant Instant 对象
     * @return LocalDateTime 对象，如果为 null 则返回 null
     */
    public static LocalDateTime localDateTimeFromInstant(Instant instant) {
        if (instant == null) {
            return null;
        }
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }
}
