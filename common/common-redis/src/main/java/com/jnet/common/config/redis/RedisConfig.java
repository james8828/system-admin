package com.jnet.common.config.redis;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 配置类
 * 
 * <p>配置 RedisTemplate 的序列化方式，用于统一 Redis 数据的存储格式</p>
 * 
 * <h3>序列化策略：</h3>
 * <ul>
 *     <li>Key 序列化 - StringRedisSerializer（字符串）</li>
 *     <li>Value 序列化 - GenericJackson2JsonRedisSerializer（JSON）</li>
 *     <li>Hash Key 序列化 - StringRedisSerializer（字符串）</li>
 *     <li>Hash Value 序列化 - GenericJackson2JsonRedisSerializer（JSON）</li>
 * </ul>
 * 
 * <h3>优势：</h3>
 * <ul>
 *     <li>✅ JSON 序列化便于跨语言读取和调试</li>
 *     <li>✅ 支持复杂对象类型（自动转换）</li>
 *     <li>✅ 字符串序列化保证 key 的可读性</li>
 * </ul>
 * 
 * <h3>使用示例：</h3>
 * <pre>{@code
 * @Autowired
 * private RedisTemplate<String, Object> redisTemplate;
 * 
 * // 存储对象
 * redisTemplate.opsForValue().set("user:1", userObject);
 * 
 * // 获取对象
 * User user = (User) redisTemplate.opsForValue().get("user:1");
 * }</pre>
 * 
 * @author mu
 * @version 1.0
 * @since 2026/4/1
 */

@Configuration
public class RedisConfig {

    /**
     * 配置 RedisTemplate（主 Bean）
     * 
     * <p>使用 String 序列化 key，JSON 序列化 value</p>
     * 
     * @param redisConnectionFactory Redis 连接工厂
     * @return RedisTemplate 实例
     */
    @Primary
    @Bean("commonRedisTemplate")
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);

        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer jsonRedisSerializer = new GenericJackson2JsonRedisSerializer();

        // Key 使用字符串序列化
        template.setKeySerializer(stringRedisSerializer);
        template.setHashKeySerializer(stringRedisSerializer);
        
        // Value 使用 JSON 序列化
        template.setValueSerializer(jsonRedisSerializer);
        template.setHashValueSerializer(jsonRedisSerializer);

        template.afterPropertiesSet();
        return template;
    }

}
