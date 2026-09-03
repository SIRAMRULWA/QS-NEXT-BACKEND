package za.co.qsnext.employeemanagement.security;

import io.jsonwebtoken.Claims;
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

    private final SecretKey secretKey;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public JwtService(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.access-token-expiration}") long accessTokenExpiration,
            @Value("${security.jwt.refresh-token-expiration}") long refreshTokenExpiration
    ) {

        this.secretKey = Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
        );

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
                accessTokenExpiration
        );
    }

    public String generateRefreshToken(
            UUID userId,
            String username
    ) {

        return generateToken(
                userId,
                username,
                REFRESH_TOKEN,
                refreshTokenExpiration
        );
    }

    public String extractUsername(
            String token
    ) {

        return extractClaims(token)
                .getSubject();
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
            long expirationMillis
    ) {

        Date now = new Date();

        Date expiration =
                new Date(
                        now.getTime()
                                + expirationMillis
                );

        return Jwts.builder()
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
                .expiration(expiration)
                .signWith(secretKey)
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