package za.co.qsnext.employeemanagement.security;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String VALID_SECRET =
            "unit-test-jwt-signing-secret-at-least-32-bytes-long";

    private final JwtService jwtService = new JwtService(
            VALID_SECRET,
            900_000L,
            604_800_000L
    );

    @Test
    void constructorRejectsSecretShorterThan32Bytes() {

        assertThatThrownBy(() ->
                new JwtService("too-short-secret", 900_000L, 604_800_000L)
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32");
    }

    @Test
    void accessTokenRoundTripsUsernameAndUserId() {

        UUID userId = UUID.randomUUID();
        String token = jwtService.generateAccessToken(userId, "jdoe");

        assertThat(jwtService.extractUsername(token)).isEqualTo("jdoe");
        assertThat(jwtService.extractUserId(token)).isEqualTo(userId);
        assertThat(jwtService.isAccessToken(token)).isTrue();
        assertThat(jwtService.isRefreshToken(token)).isFalse();
        assertThat(jwtService.isTokenValid(token, "jdoe")).isTrue();
    }

    @Test
    void accessTokenHasNoTokenId() {

        String token = jwtService.generateAccessToken(
                UUID.randomUUID(),
                "jdoe"
        );

        assertThat(jwtService.extractTokenId(token)).isNull();
    }

    @Test
    void refreshTokenCarriesTheSuppliedTokenId() {

        UUID userId = UUID.randomUUID();
        String tokenId = UUID.randomUUID().toString();

        String token = jwtService.generateRefreshToken(
                userId,
                "jdoe",
                tokenId
        );

        assertThat(jwtService.extractTokenId(token)).isEqualTo(tokenId);
        assertThat(jwtService.isRefreshToken(token)).isTrue();
        assertThat(jwtService.isAccessToken(token)).isFalse();
    }

    @Test
    void tokenIsInvalidForADifferentUsername() {

        String token = jwtService.generateAccessToken(
                UUID.randomUUID(),
                "jdoe"
        );

        assertThat(jwtService.isTokenValid(token, "someone-else")).isFalse();
    }

    @Test
    void tokenSignedWithADifferentSecretIsRejected() {

        JwtService otherIssuer = new JwtService(
                "a-completely-different-unit-test-signing-secret",
                900_000L,
                604_800_000L
        );

        String token = otherIssuer.generateAccessToken(
                UUID.randomUUID(),
                "jdoe"
        );

        assertThatThrownBy(() -> jwtService.extractUsername(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void expiredTokenIsReportedAsInvalid() {

        JwtService shortLivedIssuer = new JwtService(
                VALID_SECRET,
                -1_000L,
                604_800_000L
        );

        String token = shortLivedIssuer.generateAccessToken(
                UUID.randomUUID(),
                "jdoe"
        );

        assertThatThrownBy(() -> jwtService.isTokenValid(token, "jdoe"))
                .isInstanceOf(JwtException.class);
    }
}
