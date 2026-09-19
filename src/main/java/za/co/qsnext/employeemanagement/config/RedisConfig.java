package za.co.qsnext.employeemanagement.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Map;

/**
 * Redis-backed caching. A JSON serializer is used for cache values so
 * cached entries are human-readable in Redis and are not tied to Java
 * serialization compatibility across deployments.
 */
@Configuration
@EnableCaching
public class RedisConfig {

    private final long defaultCacheTtlMinutes;
    private final long userPrincipalCacheTtlMinutes;

    public RedisConfig(
            @Value("${security.cache.default-ttl-minutes}") long defaultCacheTtlMinutes,
            @Value("${security.cache.user-principal-ttl-minutes}") long userPrincipalCacheTtlMinutes
    ) {
        this.defaultCacheTtlMinutes = defaultCacheTtlMinutes;
        this.userPrincipalCacheTtlMinutes = userPrincipalCacheTtlMinutes;
    }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(defaultCacheTtlMinutes))
                .disableCachingNullValues()
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(new StringRedisSerializer())
                )
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(new GenericJackson2JsonRedisSerializer())
                );

        Map<String, RedisCacheConfiguration> perCacheConfig = Map.of(
                RedisCacheNames.USER_PRINCIPALS,
                defaultConfig.entryTtl(Duration.ofMinutes(userPrincipalCacheTtlMinutes))
        );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(perCacheConfig)
                .build();
    }

    /*
     * Simple key/value use (rate-limit counters, the access-token
     * revocation denylist) is served by Spring Boot's auto-configured
     * StringRedisTemplate bean - no need to declare another one.
     */
}
