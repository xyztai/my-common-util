package net.my.cache;

import com.github.benmanes.caffeine.cache.LoadingCache;
import sun.rmi.server.LoaderHandler;

/**
 * @Author tai
 * @create 2024-09-25 12:43
 */
public abstract class AbstractCaffeineCache<T> {
    protected LoadingCache<String, T> loadingCache;
    abstract LoadingCache<String, T> createLoadingCache();
    public boolean put(String key, T value) {
        if(loadingCache == null) {
            loadingCache = createLoadingCache();
        }
        loadingCache.put(key, value);
        return Boolean.TRUE;
    }

    public T get(String key) {
        if(loadingCache == null) {
            loadingCache = createLoadingCache();
        }
        try {
            return loadingCache.get(key);
        } catch (Exception e) {
            return null;
        }
    }

    public boolean clear(String key) {
        if(loadingCache == null) {
            loadingCache = createLoadingCache();
        }
        loadingCache.invalidate(key);
        return Boolean.TRUE;
    }
}
