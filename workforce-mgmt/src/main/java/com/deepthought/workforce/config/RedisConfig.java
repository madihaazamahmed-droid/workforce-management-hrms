package com.deepthought.workforce.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.SimpleCacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * LF-202 FIX:
 * 1. RedisTemplate is configured but Spring won't crash if Redis is unreachable at startup
 *    because we set connect-timeout in application.yml (fast-fail, not infinite wait).
 * 2. CacheErrorHandler logs and swallows Redis errors at runtime — app degrades to DB-only.
 * 3. When Redis comes back, caching resumes automatically (connection factory reconnects).
 */
@Configuration
@EnableCaching
@Slf4j
public class RedisConfig {

    @Bean
    public ObjectMapper redisObjectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer(redisObjectMapper()));
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer(redisObjectMapper()));
        return template;
    }

    /**
     * Custom error handler: when Redis is unavailable, log and continue — never throw.
     * Spring @Cacheable will call the actual method (DB). @CacheEvict failures are also swallowed.
     */
    @Bean
    public CacheErrorHandler cacheErrorHandler() {
        return new SimpleCacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException e, Cache cache, Object key) {
                log.warn("Redis GET failed for cache={} key={}. Falling back to DB. Error: {}",
                        cache.getName(), key, e.getMessage());
            }

            @Override
            public void handleCachePutError(RuntimeException e, Cache cache, Object key, Object value) {
                log.warn("Redis PUT failed for cache={} key={}. DB write still succeeded. Error: {}",
                        cache.getName(), key, e.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException e, Cache cache, Object key) {
                log.warn("Redis EVICT failed for cache={} key={}. Error: {}",
                        cache.getName(), key, e.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException e, Cache cache) {
                log.warn("Redis CLEAR failed for cache={}. Error: {}", cache.getName(), e.getMessage());
            }
        };
    }
}
