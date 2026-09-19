package za.co.qsnext.employeemanagement.mobile;

import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import za.co.qsnext.employeemanagement.attendance.AttendanceService;
import za.co.qsnext.employeemanagement.attendance.dto.AttendanceResponse;
import za.co.qsnext.employeemanagement.audit.AuditService;
import za.co.qsnext.employeemanagement.calendar.CalendarService;
import za.co.qsnext.employeemanagement.exception.MobileDeviceNotFoundException;
import za.co.qsnext.employeemanagement.leave.LeaveService;
import za.co.qsnext.employeemanagement.mobile.dto.MobileDashboardResponse;
import za.co.qsnext.employeemanagement.mobile.dto.MobileDeviceResponse;
import za.co.qsnext.employeemanagement.notification.NotificationService;
import za.co.qsnext.employeemanagement.selfservice.SelfServiceService;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * The Mobile API's job, per its charter, is to compose already-existing
 * domain services into mobile-friendly shapes - never to duplicate
 * their business logic. Every read here delegates to {@code
 * SelfServiceService}, {@code AttendanceService}, {@code LeaveService},
 * {@code CalendarService} or {@code NotificationService}; the only
 * genuinely new capability this module adds is the mobile device
 * registry for push notifications (see {@link MobileDevice} and {@code
 * DeviceRegistryPushNotificationSender}).
 */
@Service
@Transactional(readOnly = true)
public class MobileService {

    private static final int UPCOMING_EVENTS_WINDOW_DAYS = 14;

    private final SelfServiceService selfServiceService;
    private final AttendanceService attendanceService;
    private final LeaveService leaveService;
    private final CalendarService calendarService;
    private final NotificationService notificationService;
    private final MobileDeviceRepository mobileDeviceRepository;
    private final AuditService auditService;

    public MobileService(
            SelfServiceService selfServiceService,
            AttendanceService attendanceService,
            LeaveService leaveService,
            CalendarService calendarService,
            NotificationService notificationService,
            MobileDeviceRepository mobileDeviceRepository,
            AuditService auditService
    ) {
        this.selfServiceService = selfServiceService;
        this.attendanceService = attendanceService;
        this.leaveService = leaveService;
        this.calendarService = calendarService;
        this.notificationService = notificationService;
        this.mobileDeviceRepository = mobileDeviceRepository;
        this.auditService = auditService;
    }

    public MobileDashboardResponse getDashboard(UUID userId) {

        AttendanceResponse todayAttendance = attendanceService
                .getOwnAttendanceForDate(userId, LocalDate.now())
                .map(AttendanceResponse::from)
                .orElse(null);

        OffsetDateTime now = OffsetDateTime.now();

        long unreadCount = notificationService
                .getUnread(userId, PageRequest.of(0, 1))
                .getTotalElements();

        return new MobileDashboardResponse(
                selfServiceService.getOwnProfile(userId),
                todayAttendance,
                leaveService.getOwnLeaveBalances(userId, LocalDate.now().getYear()),
                calendarService.listEvents(userId, now, now.plusDays(UPCOMING_EVENTS_WINDOW_DAYS)),
                unreadCount
        );
    }

    @Transactional
    public MobileDeviceResponse registerDevice(UUID userId, String deviceToken, String platform) {

        MobileDevice device = mobileDeviceRepository.findByDeviceToken(deviceToken)
                .map(existing -> {
                    existing.reassign(userId, platform);
                    return existing;
                })
                .orElseGet(() -> new MobileDevice(userId, deviceToken, platform));

        MobileDevice saved = mobileDeviceRepository.save(device);

        auditService.log(userId, "MOBILE_DEVICE_REGISTERED", "MobileDevice", saved.getId(),
                AuditService.RESULT_SUCCESS);

        return MobileDeviceResponse.from(saved);
    }

    public List<MobileDeviceResponse> getMyDevices(UUID userId) {
        return mobileDeviceRepository.findByUserId(userId).stream()
                .map(MobileDeviceResponse::from)
                .toList();
    }

    @Transactional
    public void unregisterDevice(UUID userId, UUID deviceId) {

        MobileDevice device = mobileDeviceRepository.findById(deviceId)
                .orElseThrow(() -> new MobileDeviceNotFoundException("Device not found: " + deviceId));

        if (!device.getUserId().equals(userId)) {
            throw new AccessDeniedException("You do not have permission to remove this device");
        }

        mobileDeviceRepository.delete(device);

        auditService.log(userId, "MOBILE_DEVICE_UNREGISTERED", "MobileDevice", deviceId,
                AuditService.RESULT_SUCCESS);
    }
}
