package za.co.qsnext.employeemanagement.auth;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import za.co.qsnext.employeemanagement.security.JwtService;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence and lifecycle for server-tracked refresh tokens: issuing new
 * ones, finding an existing record, and revocation (single token or all of
 * a user's active tokens, e.g. on logout, password reset or detected
 * token reuse).
 */
@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            JwtService jwtService
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
    }

    @Transactional
    public String issue(
            UUID userId,
            String username,
            String createdByIp
    ) {
        JwtService.IssuedRefreshToken issued =
                jwtService.generateRefreshToken(userId, username);

        refreshTokenRepository.save(
                new RefreshToken(
                        issued.tokenId(),
                        userId,
                        issued.expiresAt(),
                        createdByIp
                )
        );

        return issued.token();
    }

    @Transactional(readOnly = true)
    public Optional<RefreshToken> find(UUID tokenId) {
        return refreshTokenRepository.findById(tokenId);
    }

    @Transactional
    public void revoke(RefreshToken refreshToken) {
        refreshToken.revoke();
    }

    /**
     * Revokes every active refresh token for a user. Runs in its own
     * transaction so the revocation is persisted even when it is triggered
     * from a flow (e.g. detected token reuse) whose own transaction is
     * about to roll back after throwing.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void revokeAllActiveForUser(UUID userId) {
        refreshTokenRepository.revokeAllActiveForUser(
                userId,
                OffsetDateTime.now()
        );
    }
}
