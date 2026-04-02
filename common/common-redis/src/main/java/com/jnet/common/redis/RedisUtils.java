package com.jnet.common.redis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Redis 工具类
 * 
 * <p>封装 Redis 常用操作，提供便捷的 API 接口</p>
 * 
 * <h3>主要功能：</h3>
 * <ul>
 *     <li>字符串操作 - set, get, delete</li>
 *     <li>过期时间管理 - expire, getExpire</li>
 *     <li>集合操作 - addToSet, removeFromSet, isInSet</li>
 *     <li>原子操作 - increment（计数器）</li>
 *     <li>防重放 - getAndSet（原子性获取并设置）</li>
 * </ul>
 * 
 * <h3>使用示例：</h3>
 * <pre>{@code
 * @Autowired
 * private RedisUtils redisUtils;
 * 
 * // 存储数据（带过期时间）
 * redisUtils.set("key", "value", 300, TimeUnit.SECONDS);
 * 
 * // 获取数据
 * Object value = redisUtils.get("key");
 * 
 * // 检查 key 是否存在
 * boolean exists = redisUtils.hasKey("key");
 * }</pre>
 * 
 * @author mu
 * @version 1.0
 * @since 2026/4/1
 */
@ConditionalOnBean(RedisTemplate.class)
@Slf4j
@Component
public class RedisUtils {

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisUtils(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 设置字符串值
     * @param key 键
     * @param value 值
     */
    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 设置带过期时间的值
     * @param key 键
     * @param value 值
     * @param timeout 过期时间
     * @param unit 时间单位
     */
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    /**
     * 获取值
     * @param key 键
     * @return 值，如果不存在返回 null
     */
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 删除键
     * @param key 键
     * @return true-删除成功，false-删除失败或 key 不存在
     */
    public boolean delete(String key) {
        return Boolean.TRUE.equals(redisTemplate.delete(key));
    }

    /**
     * 检查键是否存在
     * @param key 键
     * @return true-存在，false-不存在
     */
    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 设置过期时间
     * @param key 键
     * @param timeout 过期时间
     * @param unit 时间单位
     * @return true-设置成功，false-设置失败
     */
    public boolean expire(String key, long timeout, TimeUnit unit) {
        return Boolean.TRUE.equals(redisTemplate.expire(key, timeout, unit));
    }

    /**
     * 获取剩余过期时间
     * @param key 键
     * @param unit 时间单位
     * @return 剩余时间，如果 key 不存在返回 null
     */
    public Long getExpire(String key, TimeUnit unit) {
        return redisTemplate.getExpire(key, unit);
    }

    /**
     * 添加到集合
     * @param key 键
     * @param value 值
     */
    public void addToSet(String key, Object value) {
        redisTemplate.opsForSet().add(key, value);
    }

    /**
     * 从集合移除
     * @param key 键
     * @param value 值
     */
    public void removeFromSet(String key, Object value) {
        redisTemplate.opsForSet().remove(key, value);
    }

    /**
     * 检查是否在集合中
     * @param key 键
     * @param value 值
     * @return true-在集合中，false-不在集合中
     */
    public boolean isInSet(String key, Object value) {
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(key, value));
    }

    /**
     * 原子递增（用于计数器）
     * @param key 键
     * @return 递增后的值
     */
    public Long increment(String key) {
        return redisTemplate.opsForValue().increment(key);
    }

    /**
     * 获取并设置（用于防重放）
     * 
     * <p>原子性操作：先获取旧值，再设置新值</p>
     * 
     * @param key 键
     * @param value 新值
     * @return 旧值，如果 key 不存在返回 null
     */
    public Object getAndSet(String key, Object value) {
        return redisTemplate.opsForValue().getAndSet(key, value);
    }
}
