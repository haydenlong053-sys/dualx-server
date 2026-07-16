package com.app.common.redis;

import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;

import java.time.Duration;

/**
 * 自定义缓存配置
 */
public class MyCacheManager extends RedisCacheManager {

    //缓存key与过期时间分隔符
    private static final String SEPARATOR = "#";

    public MyCacheManager(RedisCacheWriter cacheWriter, RedisCacheConfiguration defaultCacheConfiguration) {
        super(cacheWriter, defaultCacheConfiguration);
    }

    @Override
    protected RedisCache createRedisCache(String name, RedisCacheConfiguration cacheConfig) {
        String[] values = name.split(SEPARATOR);
        if (values.length > 1) {
            long second = Long.parseLong(values[1]);
            cacheConfig = cacheConfig.entryTtl(Duration.ofSeconds(second));
        }

        return super.createRedisCache(name, cacheConfig);
    }
}
