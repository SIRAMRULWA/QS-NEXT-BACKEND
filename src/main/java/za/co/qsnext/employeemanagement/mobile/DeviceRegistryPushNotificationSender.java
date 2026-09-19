package za.co.qsnext.employeemanagement.mobile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import za.co.qsnext.employeemanagement.audit.AuditService;
import za.co.qsnext.employeemanagement.notification.NotificationEvent;
import za.co.qsnext.employeemanagement.notification.PushNotificationSender;

import java.util.List;

/**
 * The first real implementation of {@link PushNotificationSender}, now
 * that {@link MobileDevice} gives it the device-token registry its
 * javadoc has been waiting on. Like {@code InternalPayrollIntegrationProvider}
 * in the Integrations module, this deliberately does not make a live
 * HTTP call to FCM/APNs/web-push - no vendor credentials exist in this
 * environment, and this codebase's convention is to wire the real
 * interface and log+audit what would be sent, leaving the actual
 * vendor HTTP call as a follow-up once credentials are available. A
 * user with zero registered devices is a normal, silent no-op.
 */
@Component
public class DeviceRegistryPushNotificationSender implements PushNotificationSender {

    private static final Logger log = LoggerFactory.getLogger(DeviceRegistryPushNotificationSender.class);

    private final MobileDeviceRepository mobileDeviceRepository;
    private final AuditService auditService;

    public DeviceRegistryPushNotificationSender(
            MobileDeviceRepository mobileDeviceRepository,
            AuditService auditService
    ) {
        this.mobileDeviceRepository = mobileDeviceRepository;
        this.auditService = auditService;
    }

    @Override
    public void send(NotificationEvent event) {
        List<MobileDevice> devices = mobileDeviceRepository.findByUserId(event.recipientUserId());

        for (MobileDevice device : devices) {
            log.info(
                    "PUSH {}: would be sent to {} device {} for user {}: {}",
                    event.type(), device.getPlatform(), device.getId(), event.recipientUserId(), event.title());

            auditService.log(
                    event.recipientUserId(), "PUSH_NOTIFICATION_QUEUED", "MobileDevice", device.getId(),
                    AuditService.RESULT_SUCCESS);
        }
    }
}
