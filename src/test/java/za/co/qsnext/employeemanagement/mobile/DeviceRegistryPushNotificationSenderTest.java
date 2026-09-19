package za.co.qsnext.employeemanagement.mobile;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import za.co.qsnext.employeemanagement.audit.AuditService;
import za.co.qsnext.employeemanagement.notification.NotificationEvent;
import za.co.qsnext.employeemanagement.notification.NotificationType;

import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceRegistryPushNotificationSenderTest {

    @Mock
    private MobileDeviceRepository mobileDeviceRepository;
    @Mock
    private AuditService auditService;

    private DeviceRegistryPushNotificationSender sender;

    @BeforeEach
    void setUp() {
        sender = new DeviceRegistryPushNotificationSender(mobileDeviceRepository, auditService);
    }

    @Test
    void send_isANoOp_whenTheUserHasNoRegisteredDevices() {
        UUID userId = UUID.randomUUID();
        when(mobileDeviceRepository.findByUserId(userId)).thenReturn(List.of());

        sender.send(new NotificationEvent(userId, NotificationType.LEAVE_REQUEST_APPROVED, "Title", "Message"));

        verify(auditService, never()).log(any(), any(), any(), any(), any());
    }

    @Test
    void send_auditsOnePushPerRegisteredDevice() {
        UUID userId = UUID.randomUUID();

        MobileDevice deviceOne = new MobileDevice(userId, "token-1", "IOS");
        setId(deviceOne, UUID.randomUUID());
        MobileDevice deviceTwo = new MobileDevice(userId, "token-2", "ANDROID");
        setId(deviceTwo, UUID.randomUUID());

        when(mobileDeviceRepository.findByUserId(userId)).thenReturn(List.of(deviceOne, deviceTwo));

        sender.send(new NotificationEvent(userId, NotificationType.LEAVE_REQUEST_APPROVED, "Title", "Message"));

        verify(auditService, times(2)).log(
                org.mockito.ArgumentMatchers.eq(userId),
                org.mockito.ArgumentMatchers.eq("PUSH_NOTIFICATION_QUEUED"),
                org.mockito.ArgumentMatchers.eq("MobileDevice"),
                any(),
                org.mockito.ArgumentMatchers.eq(AuditService.RESULT_SUCCESS));
    }

    private static void setId(Object entity, UUID id) {
        try {
            Field field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
