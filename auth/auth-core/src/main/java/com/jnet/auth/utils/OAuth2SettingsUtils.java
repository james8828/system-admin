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
 * 提供 ClientSettings 和 TokenSettings 的解析和转换方法
 * 
 * @author JNet
 * @since 1.0.0
 */
@Slf4j
@Component
public class OAuth2SettingsUtils {

    private static ObjectMapper objectMapper;

    public OAuth2SettingsUtils(ObjectMapper objectMapper) {
        OAuth2SettingsUtils.objectMapper = objectMapper;
    }

    /**
     * 将 Object 类型的 clientSettings 转换为 ClientSettings
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
                    log.warn("Invalid token endpoint authentication signing algorithm: {}", algorithmValue);
                }
            }

        } catch (Exception e) {
            log.warn("Failed to parse client settings: {}", e.getMessage());
        }

        return builder.build();
    }

    /**
     * 将 Object 类型的 tokenSettings 转换为 TokenSettings
     * 
     * @param tokenSettingsObject Object 类型的令牌设置（可能是 Map、String 或其他类型）
     * @return TokenSettings 对象
     */
    public static TokenSettings parseTokenSettingsFromObject(Object tokenSettingsObject) {
        TokenSettings.Builder builder = TokenSettings.builder();

        if (tokenSettingsObject == null) {
            log.info("Token settings is null, using default settings");
            // 默认设置
            return builder.build();
        }

        try {
            // 如果已经是 Map 类型，直接使用
            Map<String, Object> settings = convertToMap(tokenSettingsObject);
            log.info("Parsing token settings from map: {}", settings);

            // 支持两种命名风格：kebab-case 和 camelCase
            // kebab-case: access-token-time-to-live (JSONB 原始格式)
            // camelCase: accessTokenTimeToLive (Java 序列化格式)
            
            // 解析 access-token-time-to-live
            Object accessTokenTTL = settings.get("access-token-time-to-live");
            if (accessTokenTTL == null) {
                accessTokenTTL = settings.get("accessTokenTimeToLive");
            }
            if (accessTokenTTL != null) {
                log.info("Found access-token-time-to-live: {} (type: {})", accessTokenTTL, accessTokenTTL.getClass().getName());
                Long seconds = parseDurationSeconds(accessTokenTTL);
                if (seconds != null) {
                    builder.accessTokenTimeToLive(java.time.Duration.ofSeconds(seconds));
                    log.info("Set access token TTL: {} seconds", seconds);
                } else {
                    log.warn("Failed to parse access-token-time-to-live: {}", accessTokenTTL);
                }
            }

            // 解析 refresh-token-time-to-live
            Object refreshTokenTTL = settings.get("refresh-token-time-to-live");
            if (refreshTokenTTL == null) {
                refreshTokenTTL = settings.get("refreshTokenTimeToLive");
            }
            if (refreshTokenTTL != null) {
                log.info("Found refresh-token-time-to-live: {} (type: {})", refreshTokenTTL, refreshTokenTTL.getClass().getName());
                Long seconds = parseDurationSeconds(refreshTokenTTL);
                if (seconds != null) {
                    builder.refreshTokenTimeToLive(java.time.Duration.ofSeconds(seconds));
                    log.info("Set refresh token TTL: {} seconds", seconds);
                } else {
                    log.warn("Failed to parse refresh-token-time-to-live: {}", refreshTokenTTL);
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
                    log.info("Set access token format: {}", formatValue);
                } catch (Exception e) {
                    log.warn("Invalid access token format: {}", formatValue);
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
                log.info("Set reuse refresh tokens: {}", reuse);
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
                    log.info("Set authorization code TTL: {} seconds", seconds);
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
                    log.info("Set device code TTL: {} seconds", seconds);
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
                    log.info("Set ID token signature algorithm: {}", algorithmValue);
                } catch (Exception e) {
                    log.warn("Invalid ID token signature algorithm: {}", algorithmValue);
                }
            }

            log.info("Token settings built successfully");

        } catch (Exception e) {
            log.error("Failed to parse token settings: {}", e.getMessage(), e);
        }

        return builder.build();
    }

    /**
     * 解析布尔值（支持 Boolean、Integer、String 等类型）
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
     * 支持 Long、Integer、String、Map（Duration 对象）、ISO-8601 格式等
     * 
     * @param value 待解析的值
     * @return 秒数
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
                    log.warn("Failed to parse ISO-8601 duration: {}", stringValue);
                }
            }
            
            // 尝试解析普通数字字符串
            try {
                return Long.parseLong(stringValue);
            } catch (NumberFormatException e) {
                log.warn("Failed to parse duration string: {}", value);
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
