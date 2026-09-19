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

import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;

import java.time.Duration;
import java.util.Map;

/**
 * Redis-backed caching. Only a deliberate, small set of hot/expensive
 * lookups are cached (see the per-cache config below) - this is not a
 * blanket cache-everything setup.
 *
 * Values are serialized via GenericJacksonJsonRedisSerializer's own
 * builder (the first-party recipe for this, rather than hand-configuring
 * an ObjectMapper) with a dedicated Jackson mapper isolated from the
 * application's main HTTP ObjectMapper: field-visibility ANY, since the
 * cached types (JPA entities and the CustomUserDetails record) have no
 * setters, and default typing scoped to this application's own package
 * so the cache manager can tell a stored value's concrete class apart
 * from the generic Object it's handed as (without that, every read
 * would deserialize to a plain LinkedHashMap instead of the real type -
 * caught by the Testcontainers-backed caching tests, not guessable from
 * a signature alone). The validator only trusts our own classes, not
 * arbitrary types, to avoid the classic polymorphic-deserialization
 * gadget-chain risk.
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

        PolymorphicTypeValidator typeValidator = BasicPolymorphicTypeValidator
                .builder()
                .allowIfSubType("za.co.qsnext.employeemanagement.")
                .build();

        GenericJacksonJsonRedisSerializer serializer =
                GenericJacksonJsonRedisSerializer.builder()
                        .enableDefaultTyping(typeValidator)
                        .customize(mapperBuilder -> mapperBuilder
                                .changeDefaultVisibility(visibility -> visibility
                                        .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                                )
                        )
                        .build();

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
