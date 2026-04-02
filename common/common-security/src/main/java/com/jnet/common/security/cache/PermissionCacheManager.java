package com.jnet.common.security.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 通用缓存管理器（Servlet 专用）
 * 
 * <p>提供基于 Caffeine 的本地缓存支持，支持自动过期和统计信息</p>
 * 
 * <h3>核心功能：</h3>
 * <ul>
 *     <li>本地缓存管理 - 基于 Caffeine 的高性能缓存</li>
 *     <li>自动过期策略 - 写入后 5 分钟自动过期</li>
 *     <li>容量限制 - 最大 1000 条记录，LRU 淘汰</li>
 *     <li>统计监控 - 命中率、命中次数、未命中次数</li>
 *     <li>定时清理 - 每 10 分钟清理过期缓存</li>
 * </ul>
 * 
 * <h3>使用示例：</h3>
 * <pre>{@code
 * @Autowired
 * private PermissionCacheManager cacheManager;
 * 
 * // 存入缓存
 * cacheManager.put("key", value);
 * 
 * // 获取缓存
 * Object value = cacheManager.get("key");
 * }</pre>
 * 
 * @author mu
 * @version 2.0 (Servlet Only)
 * @since 2026-03-30
 */
@Slf4j
@Component
public class PermissionCacheManager {

    /**
     * 默认缓存（5 分钟过期，最大 1000 条）
     */
    private Cache<String, Object> defaultCache;

    /**
     * 定时任务线程池
     */
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    /**
     * 缓存命中率统计
     */
    private long cacheHitCount = 0;
    private long cacheMissCount = 0;

    public PermissionCacheManager() {
        log.info("已创建 PermissionCacheManager 实例");
    }

    /**
     * 初始化方法
     * <p>
     * 启动时自动：
     * </p>
     * <ol>
     *   <li>创建 Caffeine 缓存（5 分钟过期，最大 1000 条）</li>
     *   <li>启动定时清理任务</li>
     *   <li>启动定时打印统计信息</li>
     * </ol>
     */
    @PostConstruct
    public void init() {
        log.info("正在初始化 PermissionCacheManager v2.0...");

        // 创建 Caffeine 缓存
        defaultCache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(5))
                .maximumSize(1000)
                .recordStats()
                .build();

        // 启动定时清理任务（每 10 分钟清理一次无效缓存）
        scheduler.scheduleAtFixedRate(
            this::cleanupCache,
            10,
            10,
            TimeUnit.MINUTES
        );

        // 启动定时打印统计信息（每 30 分钟）
        scheduler.scheduleAtFixedRate(
            this::printCacheStats,
            30,
            30,
            TimeUnit.MINUTES
        );

        log.info("PermissionCacheManager 初始化完成");
    }

    /**
     * 销毁方法，关闭线程池
     */
    @PreDestroy
    public void destroy() {
        if (!scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        if (defaultCache != null) {
            defaultCache.invalidateAll();
        }

        log.info("PermissionCacheManager 已销毁");
    }

    /**
     * 从缓存获取数据
     *
     * @param key 缓存键
     * @param <T> 数据类型
     * @return 缓存值，不存在返回 null
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        T value = (T) defaultCache.getIfPresent(key);
        if (value != null) {
            cacheHitCount++;
            log.debug("缓存命中 - 键：{}, 累计命中次数：{}", key, cacheHitCount);
        } else {
            cacheMissCount++;
            log.debug("缓存未命中 - 键：{}, 累计未命中次数：{}", key, cacheMissCount);
        }
        return value;
    }

    /**
     * 向缓存中放入数据
     *
     * @param key 缓存键
     * @param value 缓存值
     */
    public void put(String key, Object value) {
        defaultCache.put(key, value);
        log.debug("已缓存数据 - 键：{}", key);
    }

    /**
     * 从缓存中移除数据
     *
     * @param key 缓存键
     */
    public void invalidate(String key) {
        defaultCache.invalidate(key);
        log.debug("已移除缓存 - 键：{}", key);
    }

    /**
     * 清空所有缓存
     */
    public void invalidateAll() {
        defaultCache.invalidateAll();
        log.info("已清空所有 {} 个缓存条目", size());
    }

    /**
     * 获取缓存大小
     *
     * @return 缓存条目数
     */
    public long size() {
        return defaultCache.estimatedSize();
    }

    /**
     * 清理缓存
     * 
     * <p>定期清理过期的缓存条目，释放内存空间</p>
     */
    private void cleanupCache() {
        if (defaultCache != null) {
            long size = defaultCache.estimatedSize();
            log.debug("正在清理过期缓存，当前大小：{}", size);
            defaultCache.cleanUp();
        }
    }

    /**
     * 打印缓存统计信息
     * 
     * <p>定期输出缓存命中率等统计信息，用于性能监控</p>
     */
    private void printCacheStats() {
        if (defaultCache != null) {
            var stats = defaultCache.stats();
            double hitRate = stats.hitRate() * 100;
            log.info("缓存统计 - 大小：{}, 命中次数：{}, 未命中次数：{}, 命中率：{}%",
                    defaultCache.estimatedSize(),
                    stats.hitCount(),
                    stats.missCount(),
                    String.format("%.2f", hitRate));
        }

        log.info("总缓存统计 - 请求数：{}, 命中：{}, 未命中：{}, 命中率：{}%",
                cacheHitCount + cacheMissCount,
                cacheHitCount,
                cacheMissCount,
                String.format("%.2f", cacheHitCount + cacheMissCount > 0 ? (cacheHitCount * 100.0 / (cacheHitCount + cacheMissCount)) : 0));
    }


    /**
     * 获取缓存命中率
     *
     * @return 命中率（0-1 之间）
     */
    public double getHitRate() {
        long total = cacheHitCount + cacheMissCount;
        return total > 0 ? (double) cacheHitCount / total : 0.0;
    }

    /**
     * 获取命中次数
     *
     * @return 命中次数
     */
    public long getHitCount() {
        return cacheHitCount;
    }

    /**
     * 获取未命中次数
     *
     * @return 未命中次数
     */
    public long getMissCount() {
        return cacheMissCount;
    }
}
