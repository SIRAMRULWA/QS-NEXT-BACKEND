package za.co.qsnext.employeemanagement.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private static final String TOKEN_TYPE_CLAIM = "tokenType";
    private static final String USER_ID_CLAIM = "userId";

    private static final String ACCESS_TOKEN = "access";
    private static final String REFRESH_TOKEN = "refresh";

    /**
     * HS256 requires a key of at least 256 bits. Enforced at startup so a
     * weak secret fails fast with a clear message instead of surfacing as
     * an obscure WeakKeyException on the first token signed at runtime.
     */
    private static final int MIN_SECRET_BYTES = 32;

    private final SecretKey secretKey;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public JwtService(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.access-token-expiration}") long accessTokenExpiration,
            @Value("${security.jwt.refresh-token-expiration}") long refreshTokenExpiration
    ) {

        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);

        if (secretBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "security.jwt.secret must be at least "
                            + MIN_SECRET_BYTES
                            + " bytes (256 bits) for HS256 signing; "
                            + "configured secret is only "
                            + secretBytes.length
                            + " bytes"
            );
        }

        this.secretKey = Keys.hmacShaKeyFor(secretBytes);

        this.accessTokenExpiration =
                accessTokenExpiration;

        this.refreshTokenExpiration =
                refreshTokenExpiration;
    }

    public String generateAccessToken(
            UUID userId,
            String username
    ) {

        return generateToken(
                userId,
                username,
                ACCESS_TOKEN,
                accessTokenExpiration,
                null
        );
    }

    /**
     * @param tokenId unique identifier (jti) the caller tracks in Redis
     *                so the refresh token can be revoked before its
     *                natural expiry (logout, password change, reuse
     *                detection on rotation).
     */
    public String generateRefreshToken(
            UUID userId,
            String username,
            String tokenId
    ) {

        return generateToken(
                userId,
                username,
                REFRESH_TOKEN,
                refreshTokenExpiration,
                tokenId
        );
    }

    public String extractUsername(
            String token
    ) {

        return extractClaims(token)
                .getSubject();
    }

    /**
     * @return the token's {@code jti} claim, or {@code null} for tokens
     *         that were not issued with one (access tokens).
     */
    public String extractTokenId(
            String token
    ) {

        return extractClaims(token)
                .getId();
    }

    public UUID extractUserId(
            String token
    ) {

        String userId =
                extractClaims(token)
                        .get(
                                USER_ID_CLAIM,
                                String.class
                        );

        return UUID.fromString(userId);
    }

    public boolean isTokenValid(
            String token,
            String username
    ) {

        Claims claims =
                extractClaims(token);

        return claims.getSubject()
                .equals(username)
                && !isExpired(claims);
    }

    public boolean isAccessToken(
            String token
    ) {

        return hasTokenType(
                token,
                ACCESS_TOKEN
        );
    }

    public boolean isRefreshToken(
            String token
    ) {

        return hasTokenType(
                token,
                REFRESH_TOKEN
        );
    }

    public long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }

    public long getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }

    private String generateToken(
            UUID userId,
            String username,
            String tokenType,
            long expirationMillis,
            String tokenId
    ) {

        Date now = new Date();

        Date expiration =
                new Date(
                        now.getTime()
                                + expirationMillis
                );

        JwtBuilder builder = Jwts.builder()
                .subject(username)
                .claim(
                        USER_ID_CLAIM,
                        userId.toString()
                )
                .claim(
                        TOKEN_TYPE_CLAIM,
                        tokenType
                )
                .issuedAt(now)
                .expiration(expiration);

        if (tokenId != null) {
            builder.id(tokenId);
        }

        return builder
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    private boolean hasTokenType(
            String token,
            String expectedType
    ) {

        Claims claims =
                extractClaims(token);

        return expectedType.equals(
                claims.get(
                        TOKEN_TYPE_CLAIM,
                        String.class
                )
        );
    }

    private Claims extractClaims(
            String token
    ) {

        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private boolean isExpired(
            Claims claims
    ) {

        return claims
                .getExpiration()
                .before(new Date());
    }
}