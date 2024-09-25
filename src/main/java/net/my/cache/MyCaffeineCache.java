package net.my.cache;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

import java.util.concurrent.TimeUnit;

/**
 * @Author tai
 * @create 2024-09-25 12:48
 */
public class MyCaffeineCache extends AbstractCaffeineCache {
    @Override
    LoadingCache createLoadingCache() {
        loadingCache = Caffeine.newBuilder()
                .expireAfterWrite(30L, TimeUnit.MINUTES) // 最后一次写入后，经过固定时间过期
                .initialCapacity(10) // 初始的缓存空间大小
                .maximumSize(100) // 缓存的最大条数
                .recordStats() // 开发统计功能
                .build((CacheLoader<String, String>) key -> null);
        return loadingCache;
    }
}
