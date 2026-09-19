package za.co.qsnext.employeemanagement.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET =
            "unit-test-jwt-signing-secret-do-not-use-in-production-0123456789";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, 900_000L, 604_800_000L);
    }

    @Test
    void accessToken_isValidAndIdentifiedAsAnAccessToken() {
        UUID userId = UUID.randomUUID();
        String token = jwtService.generateAccessToken(userId, "jane.doe");

        assertThat(jwtService.isAccessToken(token)).isTrue();
        assertThat(jwtService.isRefreshToken(token)).isFalse();
        assertThat(jwtService.extractUsername(token)).isEqualTo("jane.doe");
        assertThat(jwtService.extractUserId(token)).isEqualTo(userId);
        assertThat(jwtService.isTokenValid(token, "jane.doe")).isTrue();
    }

    @Test
    void refreshToken_carriesAUniqueTokenId() {
        UUID userId = UUID.randomUUID();

        JwtService.IssuedRefreshToken first =
                jwtService.generateRefreshToken(userId, "jane.doe");
        JwtService.IssuedRefreshToken second =
                jwtService.generateRefreshToken(userId, "jane.doe");

        assertThat(jwtService.isRefreshToken(first.token())).isTrue();
        assertThat(jwtService.isAccessToken(first.token())).isFalse();
        assertThat(jwtService.extractTokenId(first.token())).isEqualTo(first.tokenId());
        assertThat(first.tokenId()).isNotEqualTo(second.tokenId());
        assertThat(first.expiresAt()).isNotNull();
    }

    @Test
    void isTokenValid_isFalse_whenUsernameDoesNotMatch() {
        String token = jwtService.generateAccessToken(UUID.randomUUID(), "jane.doe");

        assertThat(jwtService.isTokenValid(token, "someone.else")).isFalse();
    }

    @Test
    void extractTokenId_throwsIllegalArgument_forAnAccessTokenWithNoJti() {
        String accessToken = jwtService.generateAccessToken(UUID.randomUUID(), "jane.doe");

        assertThatThrownBy(() -> jwtService.extractTokenId(accessToken))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void extractUsername_throws_forATamperedToken() {
        String token = jwtService.generateAccessToken(UUID.randomUUID(), "jane.doe");
        String tampered = token.substring(0, token.length() - 2) + "xx";

        assertThatThrownBy(() -> jwtService.extractUsername(tampered))
                .isInstanceOf(RuntimeException.class);
    }
}
