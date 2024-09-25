package net.my.config;

import net.my.cache.MyCaffeineCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Author tai
 * @create 2024-09-25 12:54
 */
@Configuration
public class CaffeineCacheConfig {
    @Bean
    public MyCaffeineCache myCaffeineCache() {
        return new MyCaffeineCache();
    }
}
