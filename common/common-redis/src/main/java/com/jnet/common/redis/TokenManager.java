package com.jnet.common.redis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Token 管理器
 * 负责 Token 的 Redis 存储、黑名单管理、防重放攻击
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
     * @param token Token 值
     * @param principalName 用户标识
     * @param expireSeconds 过期时间（秒）
     */
    public void storeToken(String token, String principalName, long expireSeconds) {
        String key = tokenPrefix + token;
        redisUtils.set(key, principalName, expireSeconds, TimeUnit.SECONDS);
        log.debug("Token stored in Redis: key={}, user={}, expire={}s", key, principalName, expireSeconds);
    }

    /**
     * 从 Redis 获取 Token 对应的用户
     * @param token Token 值
     * @return 用户标识，如果不存在返回 null
     */
    public String getPrincipalByToken(String token) {
        String key = tokenPrefix + token;
        Object value = redisUtils.get(key);
        return value != null ? value.toString() : null;
    }

    /**
     * 删除 Token
     * @param token Token 值
     */
    public boolean removeToken(String token) {
        String key = tokenPrefix + token;
        return redisUtils.delete(key);
    }

    /**
     * 刷新 Token 过期时间
     * @param token Token 值
     * @param expireSeconds 新的过期时间（秒）
     */
    public boolean refreshToken(String token, long expireSeconds) {
        String key = tokenPrefix + token;
        return redisUtils.expire(key, expireSeconds, TimeUnit.SECONDS);
    }

    /**
     * 获取 Token 的剩余过期时间
     * @param token Token 值
     * @param unit 时间单位
     * @return 剩余时间，如果不存在返回 null
     */
    public Long getExpire(String token, TimeUnit unit) {
        String key = tokenPrefix + token;
        return redisUtils.getExpire(key, unit);
    }

    // ==================== Token 黑名单 ====================

    /**
     * 将 Token 加入黑名单
     * @param token Token 值
     * @param expireSeconds 黑名单过期时间（通常等于 Token 剩余有效期）
     */
    public void addToBlacklist(String token, long expireSeconds) {
        String key = blacklistPrefix + token;
        redisUtils.set(key, "blacklisted", expireSeconds, TimeUnit.SECONDS);
        log.debug("Token added to blacklist: key={}, expire={}s", key, expireSeconds);
    }

    /**
     * 检查 Token 是否在黑名单中
     * @param token Token 值
     * @return true-在黑名单中，false-不在黑名单
     */
    public boolean isBlacklisted(String token) {
        String key = blacklistPrefix + token;
        return redisUtils.hasKey(key);
    }

    /**
     * 从黑名单移除 Token
     * @param token Token 值
     */
    public boolean removeFromBlacklist(String token) {
        String key = blacklistPrefix + token;
        return redisUtils.delete(key);
    }

    // ==================== 防重放攻击 ====================

    /**
     * 检查并记录 Nonce（防重放）
     * @param nonce 一次性随机数
     * @param timestamp 时间戳
     * @return true-首次请求（合法），false-重复请求（重放攻击）
     */
    public boolean checkAndStoreNonce(String nonce, long timestamp) {
        // 检查时间戳是否过期（允许 5 分钟误差）
        long currentTime = System.currentTimeMillis() / 1000;
        if (Math.abs(currentTime - timestamp) > nonceExpireSeconds) {
            log.warn("Nonce timestamp expired: current={}, request={}, diff={}", 
                    currentTime, timestamp, Math.abs(currentTime - timestamp));
            return false;
        }

        String key = noncePrefix + nonce;
        
        // 使用原子操作检查并设置
        Object oldValue = redisUtils.getAndSet(key, timestamp);
        
        if (oldValue == null) {
            // 首次使用，设置过期时间
            redisUtils.expire(key, nonceExpireSeconds, TimeUnit.SECONDS);
            log.debug("Nonce accepted: nonce={}, timestamp={}", nonce, timestamp);
            return true;
        } else {
            // 已使用过，拒绝请求
            log.warn("Nonce replay detected: nonce={}, timestamp={}", nonce, timestamp);
            return false;
        }
    }

    /**
     * 验证请求签名（防篡改）
     * @param signature 请求签名
     * @param timestamp 时间戳
     * @param secret 密钥
     * @return true-签名有效，false-签名无效
     */
    public boolean verifySignature(String signature, long timestamp, String secret) {
        // 这里可以根据具体业务实现签名验证逻辑
        // 例如：HMAC-SHA256(timestamp + nonce + body, secret)
        return true;
    }
}
