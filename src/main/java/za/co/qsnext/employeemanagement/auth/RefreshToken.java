package za.co.qsnext.employeemanagement.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Server-side record of an issued refresh token, keyed by the JWT's
 * {@code jti} claim. The JWT itself carries the user identity and
 * signature; this table is what makes rotation and revocation possible,
 * since a bare signed JWT cannot otherwise be invalidated before it
 * naturally expires.
 */
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "created_by_ip", length = 64, updatable = false)
    private String createdByIp;

    protected RefreshToken() {
        // Required by JPA
    }

    public RefreshToken(
            UUID id,
            UUID userId,
            OffsetDateTime expiresAt,
            String createdByIp
    ) {
        this.id = id;
        this.userId = userId;
        this.expiresAt = expiresAt;
        this.createdByIp = createdByIp;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public OffsetDateTime getRevokedAt() {
        return revokedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public String getCreatedByIp() {
        return createdByIp;
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isExpired() {
        return OffsetDateTime.now().isAfter(expiresAt);
    }

    public boolean isActive() {
        return !isRevoked() && !isExpired();
    }

    public void revoke() {
        if (revokedAt == null) {
            revokedAt = OffsetDateTime.now();
        }
    }
}
