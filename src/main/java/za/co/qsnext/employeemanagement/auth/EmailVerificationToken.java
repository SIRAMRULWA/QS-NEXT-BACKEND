package za.co.qsnext.employeemanagement.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A pending email-address confirmation for a self-signup account. As with
 * {@link PasswordResetToken}, only a SHA-256 hash of the token is stored.
 */
@Entity
@Table(name = "email_verification_tokens")
public class EmailVerificationToken {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "token_hash", nullable = false, unique = true, updatable = false, length = 255)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "used_at")
    private OffsetDateTime usedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected EmailVerificationToken() {
        // Required by JPA
    }

    public EmailVerificationToken(UUID userId, String tokenHash, OffsetDateTime expiresAt) {
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }

    public UUID getUserId() {
        return userId;
    }

    public boolean isUsable(OffsetDateTime now) {
        return usedAt == null && expiresAt.isAfter(now);
    }

    public void markUsed(OffsetDateTime now) {
        this.usedAt = now;
    }
}
