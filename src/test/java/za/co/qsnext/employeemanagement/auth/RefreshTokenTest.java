package za.co.qsnext.employeemanagement.auth;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenTest {

    @Test
    void isActive_isTrue_forAFreshUnrevokedToken() {
        RefreshToken token = new RefreshToken(
                UUID.randomUUID(),
                UUID.randomUUID(),
                OffsetDateTime.now().plusDays(1),
                "127.0.0.1"
        );

        assertThat(token.isActive()).isTrue();
        assertThat(token.isRevoked()).isFalse();
        assertThat(token.isExpired()).isFalse();
    }

    @Test
    void isActive_isFalse_onceRevoked() {
        RefreshToken token = new RefreshToken(
                UUID.randomUUID(),
                UUID.randomUUID(),
                OffsetDateTime.now().plusDays(1),
                "127.0.0.1"
        );

        token.revoke();

        assertThat(token.isRevoked()).isTrue();
        assertThat(token.isActive()).isFalse();
    }

    @Test
    void isActive_isFalse_onceExpired() {
        RefreshToken token = new RefreshToken(
                UUID.randomUUID(),
                UUID.randomUUID(),
                OffsetDateTime.now().minusSeconds(1),
                "127.0.0.1"
        );

        assertThat(token.isExpired()).isTrue();
        assertThat(token.isActive()).isFalse();
    }

    @Test
    void revoke_isIdempotent_keepingTheFirstRevocationTimestamp() {
        RefreshToken token = new RefreshToken(
                UUID.randomUUID(),
                UUID.randomUUID(),
                OffsetDateTime.now().plusDays(1),
                "127.0.0.1"
        );

        token.revoke();
        OffsetDateTime firstRevokedAt = token.getRevokedAt();

        token.revoke();

        assertThat(token.getRevokedAt()).isEqualTo(firstRevokedAt);
    }
}
