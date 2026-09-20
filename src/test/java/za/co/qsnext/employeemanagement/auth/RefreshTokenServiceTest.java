package za.co.qsnext.employeemanagement.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import za.co.qsnext.employeemanagement.security.JwtService;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private JwtService jwtService;

    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        refreshTokenService = new RefreshTokenService(refreshTokenRepository, jwtService);
    }

    @Test
    void issue_savesARecordForTheIssuedTokenAndReturnsIt() {
        UUID userId = UUID.randomUUID();
        UUID tokenId = UUID.randomUUID();
        OffsetDateTime expiresAt = OffsetDateTime.now().plusDays(7);

        JwtService.IssuedRefreshToken issued =
                new JwtService.IssuedRefreshToken("signed-refresh-token", tokenId, expiresAt);

        when(jwtService.generateRefreshToken(userId, "jane.doe")).thenReturn(issued);

        String token = refreshTokenService.issue(userId, "jane.doe", "127.0.0.1");

        assertThat(token).isEqualTo("signed-refresh-token");

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());

        RefreshToken saved = captor.getValue();
        assertThat(saved.getId()).isEqualTo(tokenId);
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(saved.getCreatedByIp()).isEqualTo("127.0.0.1");
    }

    @Test
    void find_returnsTheStoredToken_whenItExists() {
        UUID tokenId = UUID.randomUUID();
        RefreshToken storedToken = new RefreshToken(
                tokenId, UUID.randomUUID(), OffsetDateTime.now().plusDays(7), "127.0.0.1");

        when(refreshTokenRepository.findById(tokenId)).thenReturn(Optional.of(storedToken));

        Optional<RefreshToken> result = refreshTokenService.find(tokenId);

        assertThat(result).contains(storedToken);
    }

    @Test
    void find_returnsEmpty_whenTheTokenIsUnknown() {
        UUID tokenId = UUID.randomUUID();

        when(refreshTokenRepository.findById(tokenId)).thenReturn(Optional.empty());

        Optional<RefreshToken> result = refreshTokenService.find(tokenId);

        assertThat(result).isEmpty();
    }

    @Test
    void revoke_marksTheTokenAsRevoked() {
        RefreshToken storedToken = new RefreshToken(
                UUID.randomUUID(), UUID.randomUUID(), OffsetDateTime.now().plusDays(7), "127.0.0.1");

        assertThat(storedToken.isRevoked()).isFalse();

        refreshTokenService.revoke(storedToken);

        assertThat(storedToken.isRevoked()).isTrue();
        assertThat(storedToken.getRevokedAt()).isNotNull();
    }

    @Test
    void revoke_isIdempotent_whenTheTokenIsAlreadyRevoked() {
        RefreshToken storedToken = new RefreshToken(
                UUID.randomUUID(), UUID.randomUUID(), OffsetDateTime.now().plusDays(7), "127.0.0.1");
        storedToken.revoke();
        OffsetDateTime firstRevokedAt = storedToken.getRevokedAt();

        refreshTokenService.revoke(storedToken);

        assertThat(storedToken.getRevokedAt()).isEqualTo(firstRevokedAt);
    }

    @Test
    void revokeAllActiveForUser_delegatesToTheRepository() {
        UUID userId = UUID.randomUUID();

        refreshTokenService.revokeAllActiveForUser(userId);

        verify(refreshTokenRepository).revokeAllActiveForUser(eq(userId), any(OffsetDateTime.class));
    }
}
