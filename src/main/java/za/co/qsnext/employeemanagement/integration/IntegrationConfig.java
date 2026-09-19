package za.co.qsnext.employeemanagement.integration;

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
 * One row per known integration point. {@code type} is a fixed catalog
 * (see the {@code TYPE_*} constants), seeded once by migration - this
 * table is updated, never created through, at runtime. Several of these
 * types (EMAIL, STORAGE, ESIGNATURE) are already real, wired-up internal
 * integrations built in earlier phases (see {@code EmailSender},
 * {@code DocumentStorageService}, {@code ESignatureProvider}); others
 * (CALENDAR, IDENTITY_PROVIDER, EXTERNAL_HR_SYSTEM) are registered here
 * as configurable but not yet backed by a concrete provider - future
 * work, same as {@code AI} and {@code PushNotificationSender}.
 * <p>
 * Do not store live secrets (API keys, tokens) in {@link IntegrationSetting}
 * - per the project's secrets policy those belong in environment
 * variables/secret management, not the database.
 */
@Entity
@Table(name = "integration_configs")
public class IntegrationConfig {

    public static final String TYPE_PAYROLL = "PAYROLL";
    public static final String TYPE_EMAIL = "EMAIL";
    public static final String TYPE_STORAGE = "STORAGE";
    public static final String TYPE_ESIGNATURE = "ESIGNATURE";
    public static final String TYPE_CALENDAR = "CALENDAR";
    public static final String TYPE_IDENTITY_PROVIDER = "IDENTITY_PROVIDER";
    public static final String TYPE_EXTERNAL_HR_SYSTEM = "EXTERNAL_HR_SYSTEM";

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "type", nullable = false, unique = true, length = 30, updatable = false)
    private String type;

    @Column(name = "provider_name", nullable = false, length = 100)
    private String providerName;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected IntegrationConfig() {
        // Required by JPA
    }

    public IntegrationConfig(String type, String providerName, boolean enabled) {
        this.type = type;
        this.providerName = providerName;
        this.enabled = enabled;
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

    public String getType() {
        return type;
    }

    public String getProviderName() {
        return providerName;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void update(String providerName, boolean enabled) {
        this.providerName = providerName;
        this.enabled = enabled;
    }
}
