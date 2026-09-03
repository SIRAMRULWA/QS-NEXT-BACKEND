package za.co.qsnext.employeemanagement.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

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

        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    public String generateAccessToken(
            UUID userId,
            String username
    ) {

        Date now = new Date();

        Date expiration = new Date(
                now.getTime() + accessTokenExpiration
        );

        return Jwts.builder()
                .subject(username)
                .claim("userId", userId.toString())
                .claim("tokenType", "access")
                .issuedAt(now)
                .expiration(expiration)
                .signWith(secretKey)
                .compact();
    }

    public String generateRefreshToken(
            UUID userId,
            String username
    ) {

        Date now = new Date();

        Date expiration = new Date(
                now.getTime() + refreshTokenExpiration
        );

        return Jwts.builder()
                .subject(username)
                .claim("userId", userId.toString())
                .claim("tokenType", "refresh")
                .issuedAt(now)
                .expiration(expiration)
                .signWith(secretKey)
                .compact();
    }

    public String extractUsername(String token) {

        return extractClaims(token)
                .getSubject();
    }

    public UUID extractUserId(String token) {

        String userId = extractClaims(token)
                .get("userId", String.class);

        return UUID.fromString(userId);
    }

    public boolean isTokenValid(
            String token,
            String username
    ) {

        Claims claims = extractClaims(token);

        return claims.getSubject().equals(username)
                && !isExpired(claims);
    }

    public boolean isAccessToken(String token) {

        Claims claims = extractClaims(token);

        return "access".equals(
                claims.get("tokenType", String.class)
        );
    }

    public boolean isRefreshToken(String token) {

        Claims claims = extractClaims(token);

        return "refresh".equals(
                claims.get("tokenType", String.class)
        );
    }

    private Claims extractClaims(String token) {

        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private boolean isExpired(Claims claims) {

        return claims.getExpiration()
                .before(new Date());
    }

    public long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }

    public long getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }
}
