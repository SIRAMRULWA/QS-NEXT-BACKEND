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
    void extractTokenId_returnsAUniqueId_forAnAccessTokenToo() {
        String firstToken = jwtService.generateAccessToken(UUID.randomUUID(), "jane.doe");
        String secondToken = jwtService.generateAccessToken(UUID.randomUUID(), "jane.doe");

        assertThat(jwtService.extractTokenId(firstToken)).isNotNull();
        assertThat(jwtService.extractTokenId(firstToken))
                .isNotEqualTo(jwtService.extractTokenId(secondToken));
    }

    @Test
    void extractUsername_throws_forATamperedToken() {
        String token = jwtService.generateAccessToken(UUID.randomUUID(), "jane.doe");
        // Change the signature's first character: it encodes a full 6 bits of
        // the signature, whereas the last base64url character carries padding
        // bits, so rewriting the tail could leave the decoded signature (and
        // the token's validity) unchanged.
        int signatureStart = token.lastIndexOf('.') + 1;
        char replacement = token.charAt(signatureStart) == 'A' ? 'B' : 'A';
        String tampered = token.substring(0, signatureStart) + replacement + token.substring(signatureStart + 1);

        assertThatThrownBy(() -> jwtService.extractUsername(tampered))
                .isInstanceOf(RuntimeException.class);
    }
}
