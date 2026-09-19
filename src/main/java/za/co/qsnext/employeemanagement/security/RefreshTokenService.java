package za.co.qsnext.employeemanagement.security;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

/**
 * Tracks issued refresh tokens in Redis so a token can be revoked
 * (logout, rotation on refresh, password change) before its JWT expiry.
 * Only the token's {@code jti} is stored, never the raw token, and each
 * entry self-expires via TTL so a crash between issuing and storing a
 * token fails safe (the token is simply treated as not-yet-active).
 */
@Service
public class RefreshTokenService {

    private static final String KEY_PREFIX = "refresh-token:";
    private static final String ACTIVE_MARKER = "1";

    private final StringRedisTemplate redisTemplate;

    public RefreshTokenService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void store(
            UUID userId,
            String tokenId,
            Duration timeToLive
    ) {
        redisTemplate.opsForValue()
                .set(key(userId, tokenId), ACTIVE_MARKER, timeToLive);
    }

    public boolean isActive(
            UUID userId,
            String tokenId
    ) {
        return Boolean.TRUE.equals(
                redisTemplate.hasKey(key(userId, tokenId))
        );
    }

    public void revoke(
            UUID userId,
            String tokenId
    ) {
        redisTemplate.delete(key(userId, tokenId));
    }

    /**
     * Revokes every refresh token issued to a user. Used when a password
     * change means older sessions should no longer be trustable.
     */
    public void revokeAll(UUID userId) {

        Set<String> keys =
                redisTemplate.keys(KEY_PREFIX + userId + ":*");

        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    private String key(
            UUID userId,
            String tokenId
    ) {
        return KEY_PREFIX + userId + ":" + tokenId;
    }
}
