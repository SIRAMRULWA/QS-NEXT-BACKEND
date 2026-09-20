package za.co.qsnext.employeemanagement.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenRevocationServiceTest {

    private static final String SECRET =
            "unit-test-jwt-signing-secret-do-not-use-in-production-0123456789";

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private JwtService jwtService;
    private TokenRevocationService tokenRevocationService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, 900_000L, 604_800_000L);
        tokenRevocationService = new TokenRevocationService(redisTemplate, jwtService);
    }

    @Test
    void revoke_storesTheTokenIdWithATtlMatchingItsRemainingLifetime() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        String accessToken = jwtService.generateAccessToken(UUID.randomUUID(), "jane.doe");
        UUID tokenId = jwtService.extractTokenId(accessToken);

        tokenRevocationService.revoke(accessToken);

        verify(valueOperations).set(
                eq("revoked:token:" + tokenId),
                anyString(),
                any(Duration.class)
        );
    }

    @Test
    void revoke_doesNothing_forABlankToken() {
        tokenRevocationService.revoke("");
        tokenRevocationService.revoke(null);

        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    void revoke_doesNothing_forAMalformedToken() {
        tokenRevocationService.revoke("not-a-real-token");

        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    void isRevoked_reflectsWhetherTheKeyExists() {
        UUID tokenId = UUID.randomUUID();
        when(redisTemplate.hasKey("revoked:token:" + tokenId)).thenReturn(true);

        assertThat(tokenRevocationService.isRevoked(tokenId)).isTrue();
    }

    @Test
    void isRevoked_isFalse_whenTheKeyIsAbsent() {
        UUID tokenId = UUID.randomUUID();
        when(redisTemplate.hasKey("revoked:token:" + tokenId)).thenReturn(false);

        assertThat(tokenRevocationService.isRevoked(tokenId)).isFalse();
    }
}
