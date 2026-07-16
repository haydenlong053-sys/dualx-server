package com.app.common.redis;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

@EnableCaching
@Configuration
public class RedisConfig {

    //默认过期时间，10s
    private static final int EXPIRATION_SECOND = 10;
    public static final String KEY_GENERATOR_NAME = "MyKeyGenerator";
    public static final String CACHE_KEY = "Cache";

    @Bean
    public MyCacheManager redisCacheManager(RedisConnectionFactory redisConnectionFactory) {
        RedisSerializationContext.SerializationPair<Object> valuePair =
            RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer());

        RedisCacheWriter redisCacheWriter = RedisCacheWriter.nonLockingRedisCacheWriter(redisConnectionFactory);
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofSeconds(EXPIRATION_SECOND))
            .serializeValuesWith(valuePair)
            .disableCachingNullValues();
        return new MyCacheManager(redisCacheWriter, config);
    }

    @Bean(value = KEY_GENERATOR_NAME)
    public KeyGenerator keyGenerator() {
        return new MyCacheKeyGenerator();
    }
}
