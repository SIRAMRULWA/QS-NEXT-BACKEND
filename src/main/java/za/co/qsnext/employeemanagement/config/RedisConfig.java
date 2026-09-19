package za.co.qsnext.employeemanagement.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.Map;

/**
 * Redis-backed caching. Only a deliberate, small set of hot/expensive
 * lookups are cached (see the per-cache config below) - this is not a
 * blanket cache-everything setup.
 *
 * Values are serialized with a dedicated Jackson ObjectMapper (isolated
 * from the application's main HTTP ObjectMapper) configured for direct
 * field access, since the cached types (JPA entities and the
 * CustomUserDetails record) have no setters. Cached types are limited to
 * ones built entirely from concrete JDK types (UUID/String/boolean/etc.)
 * with no lazy Hibernate collections in the serialized graph, so no
 * global default-typing is needed - GenericJacksonJsonRedisSerializer
 * already embeds the concrete top-level type itself.
 */
@Configuration
@EnableCaching
public class RedisConfig {

    private static final Duration DEPARTMENT_TTL = Duration.ofMinutes(30);

    /**
     * Deliberately short: this cache backs every authenticated request's
     * principal lookup, and every place that can invalidate it evicts
     * explicitly (see CustomUserDetailsService/UserService). The TTL is
     * only a safety net bounding staleness if an eviction is ever missed.
     */
    private static final Duration USER_DETAILS_TTL = Duration.ofSeconds(60);

    @Bean
    public RedisCacheManager cacheManager(
            RedisConnectionFactory connectionFactory
    ) {

        JsonMapper cacheObjectMapper = JsonMapper.builder()
                .changeDefaultVisibility(visibility -> visibility
                        .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                )
                .build();

        GenericJacksonJsonRedisSerializer serializer =
                new GenericJacksonJsonRedisSerializer(cacheObjectMapper);

        RedisSerializationContext.SerializationPair<Object> valueSerialization =
                RedisSerializationContext.SerializationPair.fromSerializer(serializer);

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration
                .defaultCacheConfig()
                .disableCachingNullValues()
                .serializeValuesWith(valueSerialization)
                .entryTtl(DEPARTMENT_TTL);

        Map<String, RedisCacheConfiguration> perCacheConfig = Map.of(
                "departments", defaultConfig.entryTtl(DEPARTMENT_TTL),
                "departmentsByName", defaultConfig.entryTtl(DEPARTMENT_TTL),
                "userDetails", defaultConfig.entryTtl(USER_DETAILS_TTL)
        );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(perCacheConfig)
                .build();
    }
}
