package za.co.qsnext.employeemanagement.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import org.hibernate.annotations.UuidGenerator;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Per-user channel opt-in/out for notifications. Deliberately one row per
 * user rather than per (user, notification type): a per-type matrix would
 * multiply rows for little real benefit at this stage, and can be added
 * later without breaking this table's shape if a specific type ever needs
 * finer control.
 */
@Entity
@Table(name = "notification_preferences")
public class NotificationPreference {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true, updatable = false)
    private UUID userId;

    @Column(name = "in_app_enabled", nullable = false)
    private boolean inAppEnabled = true;

    @Column(name = "email_enabled", nullable = false)
    private boolean emailEnabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected NotificationPreference() {
        // Required by JPA
    }

    public NotificationPreference(UUID userId) {
        this.userId = userId;
    }

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public boolean isInAppEnabled() {
        return inAppEnabled;
    }

    public boolean isEmailEnabled() {
        return emailEnabled;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void update(boolean inAppEnabled, boolean emailEnabled) {
        this.inAppEnabled = inAppEnabled;
        this.emailEnabled = emailEnabled;
    }
}
