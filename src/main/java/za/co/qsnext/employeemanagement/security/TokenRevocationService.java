package za.co.qsnext.employeemanagement.security;

import io.jsonwebtoken.JwtException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Immediate revocation for access tokens. Access tokens are otherwise
 * stateless signed JWTs, valid purely by signature and expiry, so a
 * logout, password change or detected refresh-token reuse would
 * otherwise leave the access token in use at that moment valid for up to
 * its remaining lifetime. Revoked token ids are held in Redis with a TTL
 * matching the token's own remaining lifetime, so entries clean themselves
 * up and are never kept longer than the token would have been valid anyway.
 */
@Service
public class TokenRevocationService {

    private static final Logger log = LoggerFactory.getLogger(TokenRevocationService.class);

    private static final String KEY_PREFIX = "revoked:token:";

    private final StringRedisTemplate redisTemplate;
    private final JwtService jwtService;

    public TokenRevocationService(
            StringRedisTemplate redisTemplate,
            JwtService jwtService
    ) {
        this.redisTemplate = redisTemplate;
        this.jwtService = jwtService;
    }

    /**
     * Revokes the given token immediately, if it is well-formed. Silently
     * does nothing for a malformed/already-expired token: there is nothing
     * meaningful to revoke either way.
     */
    public void revoke(String token) {

        if (token == null || token.isBlank()) {
            return;
        }

        try {

            UUID tokenId = jwtService.extractTokenId(token);
            OffsetDateTime expiresAt = jwtService.extractExpiration(token);

            Duration ttl = Duration.between(OffsetDateTime.now(), expiresAt);

            if (ttl.isNegative() || ttl.isZero()) {
                return;
            }

            redisTemplate.opsForValue().set(
                    KEY_PREFIX + tokenId,
                    "revoked",
                    ttl
            );

        } catch (JwtException | IllegalArgumentException ex) {
            // Malformed token: nothing to revoke.
        }
    }

    public boolean isRevoked(UUID tokenId) {
        return Boolean.TRUE.equals(
                redisTemplate.hasKey(KEY_PREFIX + tokenId)
        );
    }
}
