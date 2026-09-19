package za.co.qsnext.employeemanagement.mobile;

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
 * One registered mobile/web push-notification device token for a user.
 * This is the device-token registry {@code PushNotificationSender}'s
 * javadoc has been anticipating since it was first stubbed out - see
 * {@link DeviceRegistryPushNotificationSender}, the first real
 * implementation of that interface.
 */
@Entity
@Table(name = "mobile_devices")
public class MobileDevice {

    public static final String PLATFORM_IOS = "IOS";
    public static final String PLATFORM_ANDROID = "ANDROID";
    public static final String PLATFORM_WEB = "WEB";

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "device_token", nullable = false, unique = true, length = 500)
    private String deviceToken;

    @Column(name = "platform", nullable = false, length = 20)
    private String platform;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected MobileDevice() {
        // Required by JPA
    }

    public MobileDevice(UUID userId, String deviceToken, String platform) {
        this.userId = userId;
        this.deviceToken = deviceToken;
        this.platform = platform;
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

    public String getDeviceToken() {
        return deviceToken;
    }

    public String getPlatform() {
        return platform;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * A device token can outlive the app being reinstalled by a
     * different user on the same physical device - re-registering an
     * existing token re-homes it rather than erroring.
     */
    public void reassign(UUID userId, String platform) {
        this.userId = userId;
        this.platform = platform;
    }
}
