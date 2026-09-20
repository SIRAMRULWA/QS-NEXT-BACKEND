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
 * A non-sensitive key/value setting for one {@link IntegrationConfig}
 * (e.g. a default sender name, a base URL). See {@link IntegrationConfig}'s
 * javadoc for why secrets don't belong here.
 */
@Entity
@Table(name = "integration_settings")
public class IntegrationSetting {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "integration_config_id", nullable = false, updatable = false)
    private UUID integrationConfigId;

    @Column(name = "setting_key", nullable = false, length = 100, updatable = false)
    private String settingKey;

    @Column(name = "setting_value", length = 500)
    private String settingValue;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected IntegrationSetting() {
        // Required by JPA
    }

    public IntegrationSetting(UUID integrationConfigId, String settingKey, String settingValue) {
        this.integrationConfigId = integrationConfigId;
        this.settingKey = settingKey;
        this.settingValue = settingValue;
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

    public UUID getIntegrationConfigId() {
        return integrationConfigId;
    }

    public String getSettingKey() {
        return settingKey;
    }

    public String getSettingValue() {
        return settingValue;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void updateValue(String settingValue) {
        this.settingValue = settingValue;
    }
}
