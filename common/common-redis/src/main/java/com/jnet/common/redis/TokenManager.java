package com.jnet.common.redis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;

/**
 * Token 管理器
 *
 * <p>负责 Token 的 Redis 存储、黑名单管理、防重放攻击等安全功能</p>
 *
 * <h3>核心功能：</h3>
 * <h4>1. Token 存储</h4>
 * <ul>
 *     <li>storeToken - 存储 Token 到 Redis（带过期时间）</li>
 *     <li>getPrincipalByToken - 根据 Token 获取用户标识</li>
 *     <li>removeToken - 删除 Token（注销时使用）</li>
 *     <li>refreshToken - 刷新 Token 过期时间</li>
 * </ul>
 *
 * <h4>2. Token 黑名单</h4>
 * <ul>
 *     <li>addToBlacklist - 将 Token 加入黑名单（注销后立即失效）</li>
 *     <li>isBlacklisted - 检查 Token 是否在黑名单中</li>
 *     <li>removeFromBlacklist - 从黑名单移除</li>
 * </ul>
 *
 * <h4>3. 防重放攻击</h4>
 * <ul>
 *     <li>checkAndStoreNonce - 检查并记录 Nonce（一次性随机数）</li>
 *     <li>verifySignature - 验证请求签名（防篡改）</li>
 * </ul>
 *
 * <h3>配置项：</h3>
 * <ul>
 *     <li>jnet.token.redis.prefix - Token 前缀（默认：token:）</li>
 *     <li>jnet.token.blacklist.prefix - 黑名单前缀（默认：blacklist:）</li>
 *     <li>jnet.token.nonce.prefix - Nonce 前缀（默认：nonce:）</li>
 *     <li>jnet.token.nonce.expire - Nonce 过期时间（秒，默认：300）</li>
 * </ul>
 *
 * @author mu
 * @version 1.0
 * @since 2026/4/1
 */
@ConditionalOnBean(RedisUtils.class)
@Slf4j
@Component
public class TokenManager {

    private final RedisUtils redisUtils;

    @Value("${jnet.token.redis.prefix:token:}")
    private String tokenPrefix;

    @Value("${jnet.token.blacklist.prefix:blacklist:}")
    private String blacklistPrefix;

    @Value("${jnet.token.nonce.prefix:nonce:}")
    private String noncePrefix;

    @Value("${jnet.token.nonce.expire:300}")
    private long nonceExpireSeconds;

    public TokenManager(RedisUtils redisUtils) {
        this.redisUtils = redisUtils;
    }

    // ==================== Token 存储 ====================

    /**
     * 存储 Token 到 Redis
     *
     * <p>使用示例：</p>
     * <pre>{@code
     * tokenManager.storeToken("abc123", "user123", 7200);
     * }</pre>
     *
     * @param token Token 值（不能为空）
     * @param principalName 用户标识（不能为空）
     * @param expireSeconds 过期时间（秒，必须大于 0）
     * @throws IllegalArgumentException 如果参数无效
     */
    public void storeToken(String token, String principalName, long expireSeconds) {
        if (!StringUtils.hasText(token) || !StringUtils.hasText(principalName)) {
            throw new IllegalArgumentException("Token and principalName must not be empty");
        }
        if (expireSeconds <= 0) {
            throw new IllegalArgumentException("Expire seconds must be positive");
        }

        try {
            String key = buildTokenKey(token);
            redisUtils.set(key, principalName, expireSeconds, TimeUnit.SECONDS);
            log.info("✓ Token 已存储到 Redis: key={}, user={}, expire={}s", key, principalName, expireSeconds);
        } catch (Exception e) {
            log.error("✗ 存储 Token 失败：token={}, error={}", token, e.getMessage(), e);
            throw new RuntimeException("Failed to store token", e);
        }
    }

    /**
     * 从 Redis 获取 Token 对应的用户
     * @param token Token 值
     * @return 用户标识，如果不存在返回 null
     */
    public String getPrincipalByToken(String token) {
        if (!StringUtils.hasText(token)) {
            return null;
        }

        try {
            String key = buildTokenKey(token);
            Object value = redisUtils.get(key);
            return value != null ? value.toString() : null;
        } catch (Exception e) {
            log.error("获取 Token 用户失败：token={}, error={}", token, e.getMessage());
            return null;
        }
    }

    /**
     * 删除 Token
     * @param token Token 值
     * @return true-删除成功，false-删除失败
     */
    public boolean removeToken(String token) {
        if (!StringUtils.hasText(token)) {
            return false;
        }

        try {
            String key = buildTokenKey(token);
            return redisUtils.delete(key);
        } catch (Exception e) {
            log.error("删除 Token 失败：token={}, error={}", token, e.getMessage());
            return false;
        }
    }

    /**
     * 刷新 Token 过期时间
     * @param token Token 值
     * @param expireSeconds 新的过期时间（秒）
     * @return true-刷新成功，false-刷新失败
     */
    public boolean refreshToken(String token, long expireSeconds) {
        if (!StringUtils.hasText(token) || expireSeconds <= 0) {
            return false;
        }

        try {
            String key = buildTokenKey(token);
            return redisUtils.expire(key, expireSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("刷新 Token 过期时间失败：token={}, error={}", token, e.getMessage());
            return false;
        }
    }

    /**
     * 获取 Token 的剩余过期时间
     * @param token Token 值
     * @param unit 时间单位
     * @return 剩余时间，如果不存在返回 null
     */
    public Long getExpire(String token, TimeUnit unit) {
        if (!StringUtils.hasText(token) || unit == null) {
            return null;
        }

        try {
            String key = buildTokenKey(token);
            return redisUtils.getExpire(key, unit);
        } catch (Exception e) {
            log.error("获取 Token 过期时间失败：token={}, error={}", token, e.getMessage());
            return null;
        }
    }

    // ==================== Token 黑名单 ====================

    /**
     * 将 Token 加入黑名单
     *
     * <p>使用场景：用户退出登录时，使 Token 立即失效</p>
     *
     * @param token Token 值（不能为空）
     * @param expireSeconds 黑名单过期时间（通常等于 Token 剩余有效期，必须大于 0）
     * @throws IllegalArgumentException 如果参数无效
     */
    public void addToBlacklist(String token, long expireSeconds) {
        if (!StringUtils.hasText(token)) {
            throw new IllegalArgumentException("Token must not be empty");
        }
        if (expireSeconds <= 0) {
            throw new IllegalArgumentException("Expire seconds must be positive");
        }

        try {
            String key = buildBlacklistKey(token);
            redisUtils.set(key, "blacklisted", expireSeconds, TimeUnit.SECONDS);
            log.info("⚠ Token 已加入黑名单：key={}, expire={}s", key, expireSeconds);
        } catch (Exception e) {
            log.error("✗ 将 Token 加入黑名单失败：token={}, error={}", token, e.getMessage(), e);
            throw new RuntimeException("Failed to add token to blacklist", e);
        }
    }

    /**
     * 检查 Token 是否在黑名单中
     * @param token Token 值
     * @return true-在黑名单中，false-不在黑名单
     */
    public boolean isBlacklisted(String token) {
        if (!StringUtils.hasText(token)) {
            return false;
        }

        try {
            String key = buildBlacklistKey(token);
            return redisUtils.hasKey(key);
        } catch (Exception e) {
            log.error("检查 Token 黑名单状态失败：token={}, error={}", token, e.getMessage());
            return false;
        }
    }

    /**
     * 从黑名单移除 Token
     * @param token Token 值
     * @return true-移除成功，false-移除失败
     */
    public boolean removeFromBlacklist(String token) {
        if (!StringUtils.hasText(token)) {
            return false;
        }

        try {
            String key = buildBlacklistKey(token);
            return redisUtils.delete(key);
        } catch (Exception e) {
            log.error("从黑名单移除 Token 失败：token={}, error={}", token, e.getMessage());
            return false;
        }
    }

    // ==================== 防重放攻击 ====================

    /**
     * 检查并记录 Nonce（防重放）
     *
     * <p>使用 Redis 原子操作实现幂等性检查，防止请求被重复提交</p>
     *
     * @param nonce 一次性随机数（不能为空）
     * @param timestamp 时间戳（秒）
     * @return true-首次请求（合法），false-重复请求（重放攻击）
     */
    public boolean checkAndStoreNonce(String nonce, long timestamp) {
        if (!StringUtils.hasText(nonce)) {
            log.warn("Nonce 为空，拒绝请求");
            return false;
        }

        // 检查时间戳是否过期（允许 5 分钟误差）
        long currentTime = System.currentTimeMillis() / 1000;
        long timeDiff = Math.abs(currentTime - timestamp);

        if (timeDiff > nonceExpireSeconds) {
            log.warn("⚠ Nonce 时间戳已过期：current={}, request={}, diff={}s, max={}s",
                    currentTime, timestamp, timeDiff, nonceExpireSeconds);
            return false;
        }

        try {
            String key = buildNonceKey(nonce);

            // 使用原子操作检查并设置
            Object oldValue = redisUtils.getAndSet(key, timestamp);

            if (oldValue == null) {
                // 首次使用，设置过期时间
                redisUtils.expire(key, nonceExpireSeconds, TimeUnit.SECONDS);
                log.debug("✓ Nonce 验证通过：nonce={}, timestamp={}", nonce, timestamp);
                return true;
            } else {
                // 已使用过，拒绝请求
                log.warn("⚠ 检测到重放攻击：nonce={}, timestamp={}", nonce, timestamp);
                return false;
            }
        } catch (Exception e) {
            log.error("✗ Nonce 验证失败：nonce={}, error={}", nonce, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 验证请求签名（防篡改）
     *
     * <p>根据具体业务实现签名验证逻辑，例如：HMAC-SHA256(timestamp + nonce + body, secret)</p>
     *
     * <p><b>注意：</b>当前方法需要实现具体的签名验证逻辑，目前仅返回 true</p>
     *
     * @param signature 请求签名
     * @param timestamp 时间戳
     * @param secret 密钥
     * @return true-签名有效，false-签名无效
     *
     * @deprecated 待实现具体的签名验证逻辑
     */
    @Deprecated
    public boolean verifySignature(String signature, long timestamp, String secret) {
        // TODO: 实现具体的签名验证逻辑
        // 示例：HMAC-SHA256(timestamp + nonce + body, secret)
        log.warn("verifySignature 方法尚未实现，当前始终返回 true");
        return true;
    }

    // ==================== 辅助方法 ====================

    /**
     * 构建 Token 的 Redis Key
     */
    private String buildTokenKey(String token) {
        return tokenPrefix + token;
    }

    /**
     * 构建黑名单的 Redis Key
     */
    private String buildBlacklistKey(String token) {
        return blacklistPrefix + token;
    }

    /**
     * 构建 Nonce 的 Redis Key
     */
    private String buildNonceKey(String nonce) {
        return noncePrefix + nonce;
    }
}
